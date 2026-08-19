#!/usr/bin/env python3
"""Stage 1 of Firestore seeding: capture inboxes -> local temp fixture JSON.

Reads the migrated capture inboxes (`~/.claude/flashcards/<cat>/<sub>/inbox.jsonl`),
projects each card into the Firestore shape, derives the taxonomy from the slugs,
and writes a single fixture file to a gitignored temp dir (default `scripts/seed/.tmp/fixture.json`).

The fixture is intentionally NOT committed — it is a throwaway hand-off to
`seed_firestore.py`. Re-run any time; output is deterministic.

Schema (see ADR-0007):
  categories/{categoryId}                        → { name, order, subcategoryCount, iconSvg, color }
  subcategories/{categoryId-subSlug}             → { name, categoryId, categoryName, order, cardCount }
  subcategories/{categoryId-subSlug}/flashcards/{cardId} → { question, answer, tags[], createdAt, ... }

Subcategory IDs are namespaced "{categoryId}-{subSlug}" (e.g. "android-compose") to guarantee
uniqueness across parent categories in the flat subcategories/ collection.

Projection (inbox field -> Firestore field):
  question/answer                -> question/answer
  difficulty                     -> difficulty                   (mandatory — hard-fail if absent)
  question_code/answer_code      -> questionCode/answerCode      (omitted if absent)
  question_spoken/answer_spoken  -> questionSpoken/answerSpoken  (omitted if absent)
  extended_context               -> extendedContext              (omitted if absent)
  tags[]                         -> tags[]
  created_at                     -> createdAt
  id                             -> doc id (idempotency key)
Routing: subcategoryId is carried in the fixture as a path-routing field only —
         it is NOT written as a Firestore document field (encoded in the collection path).
Dropped: subcategoryId field on card, source (PII), status (capture lifecycle).
"""
from __future__ import annotations
import argparse, glob, json, os, random, sys
from collections import Counter, defaultdict

DEFAULT_SRC = os.path.expanduser("~/.claude/flashcards")
DEFAULT_OUT = os.path.join(os.path.dirname(__file__), ".tmp", "fixture.json")
DEFAULT_ICON_ASSETS = os.path.join(os.path.dirname(__file__), "assets", "category-icons")

# Display-name overrides for slugs that do not titlecase nicely. Admin can extend.
NAME_OVERRIDES = {
    "ui": "UI",
    "aaos": "AAOS",
    "claude-code": "Claude Code",
    "android-features": "Android Features",
    "dependency-injection": "Dependency Injection",
    "app-distribution": "App Distribution",
    "build-system": "Build System",
}

# Icon/color cannot be derived from a slug like NAME_OVERRIDES can. Unlike NAME_OVERRIDES, these
# are NOT hand-maintained Python dict literals — they're real, versioned files under
# scripts/seed/assets/category-icons/{slug}.svg (plain SVG, rendered via androidsvg on-device) and
# {slug}.color (plain-text hex string), one pair per category slug. Both fields are optional on
# Category (nullable, see
# docs/design/category-icon-color.md), so a missing file is a warning, not a build failure — the
# seed pipeline can legitimately auto-create a category before its icon/color art exists.


def read_icon_asset(slug: str, ext: str, icon_assets_dir: str) -> str | None:
    path = os.path.join(icon_assets_dir, f"{slug}.{ext}")
    if not os.path.isfile(path):
        print(f"warning: no {path} — category '{slug}' will omit its {ext} field", file=sys.stderr)
        return None
    with open(path, encoding="utf-8") as f:
        content = f.read().strip()
    if not content:
        print(f"warning: {path} is empty — category '{slug}' will omit its {ext} field", file=sys.stderr)
        return None
    return content


def display_name(slug: str) -> str:
    if slug in NAME_OVERRIDES:
        return NAME_OVERRIDES[slug]
    return slug.replace("-", " ").replace("_", " ").title()


def make_sub_id(cat_slug: str, sub_slug: str) -> str:
    return f"{cat_slug}-{sub_slug}"


def project_card(d: dict, subcategory_id: str) -> dict:
    if "difficulty" not in d:
        sys.exit(f"card {d.get('id')} is missing mandatory field 'difficulty'")
    if not isinstance(d["difficulty"], int) or not (1 <= d["difficulty"] <= 10):
        sys.exit(f"card {d.get('id')} has invalid difficulty {d['difficulty']!r} (must be int 1–10)")
    out = {
        "id": d["id"],
        "subcategoryId": subcategory_id,  # fixture routing only — stripped before Firestore write
        "question": d["question"],
        "answer": d["answer"],
        "tags": d.get("tags") or [],
        "createdAt": d.get("created_at"),
        "difficulty": d["difficulty"],
    }
    # optional additive fields — omit entirely when absent (never write null)
    for src, dst in (
        ("question_code", "questionCode"),
        ("answer_code", "answerCode"),
        ("question_spoken", "questionSpoken"),
        ("answer_spoken", "answerSpoken"),
        ("extended_context", "extendedContext"),
    ):
        if d.get(src):
            out[dst] = d[src]
    return out


def build(
    src_root: str,
    sample: int | None = None,
    seed: int | None = None,
    icon_assets_dir: str = DEFAULT_ICON_ASSETS,
):
    files = sorted(glob.glob(os.path.join(src_root, "*", "*", "inbox.jsonl")))
    if not files:
        sys.exit(f"no inbox files under {src_root}")

    cards: list[dict] = []
    cat_counts: Counter = Counter()
    sub_counts: Counter = Counter()
    sub_parent: dict[str, str] = {}     # namespaced sub_id -> cat_slug
    sub_display: dict[str, str] = {}    # namespaced sub_id -> bare sub_slug (for display_name)
    seen_ids: set[str] = set()

    for path in files:
        parts = path.split(os.sep)
        cat_slug, sub_slug = parts[-3], parts[-2]
        sid = make_sub_id(cat_slug, sub_slug)
        with open(path, encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                d = json.loads(line)
                # integrity: card's own fields must agree with its directory
                if d.get("category") != cat_slug or d.get("subcategory") != sub_slug:
                    sys.exit(f"card {d.get('id')} fields ({d.get('category')}/"
                             f"{d.get('subcategory')}) disagree with path {cat_slug}/{sub_slug}")
                cid = d["id"]
                if cid in seen_ids:
                    sys.exit(f"duplicate card id across corpus: {cid}")
                seen_ids.add(cid)
                sub_parent[sid] = cat_slug
                sub_display[sid] = sub_slug
                cat_counts[cat_slug] += 1
                sub_counts[sid] += 1
                cards.append(project_card(d, sid))

    if sample is not None:
        rng = random.Random(seed)
        by_sub: dict[str, list[dict]] = defaultdict(list)
        for card in cards:
            by_sub[card["subcategoryId"]].append(card)
        sampled: list[dict] = []
        for sub_cards in by_sub.values():
            sampled.extend(sub_cards if len(sub_cards) <= sample else rng.sample(sub_cards, sample))
        cards = sampled
        cat_counts = Counter()
        sub_counts = Counter()
        for card in cards:
            sid = card["subcategoryId"]
            sub_counts[sid] += 1
            cat_counts[sub_parent[sid]] += 1

    # subcategory count per category (for subcategoryCount field)
    cat_sub_counts: Counter = Counter(sub_parent.values())

    # categories: ordered by card volume desc, then slug
    categories = []
    for order, slug in enumerate(sorted(cat_counts, key=lambda s: (-cat_counts[s], s))):
        category = {
            "id": slug,
            "name": display_name(slug),
            "order": order,
            "subcategoryCount": cat_sub_counts[slug],
        }
        icon_svg = read_icon_asset(slug, "svg", icon_assets_dir)
        if icon_svg is not None:
            category["iconSvg"] = icon_svg
        color = read_icon_asset(slug, "color", icon_assets_dir)
        if color is not None:
            category["color"] = color
        categories.append(category)

    # subcategories: flat collection, namespaced id, ordered per-parent by card volume
    by_parent: dict[str, list[str]] = defaultdict(list)
    for sid, parent in sub_parent.items():
        by_parent[parent].append(sid)
    subcategories = []
    for parent in sorted(by_parent):
        for order, sid in enumerate(sorted(by_parent[parent],
                                           key=lambda s: (-sub_counts[s], s))):
            subcategories.append({
                "id": sid,
                "name": display_name(sub_display[sid]),
                "categoryId": parent,
                "categoryName": display_name(parent),
                "order": order,
                "cardCount": sub_counts[sid],
            })

    return {"categories": categories, "subcategories": subcategories, "cards": cards}, \
           cat_counts, sub_counts


def main():
    ap = argparse.ArgumentParser(description="Build a temp Firestore seed fixture from capture inboxes.")
    ap.add_argument("--src", default=DEFAULT_SRC, help=f"inbox root (default {DEFAULT_SRC})")
    ap.add_argument("--out", default=DEFAULT_OUT, help=f"fixture output path (default {DEFAULT_OUT})")
    ap.add_argument("--sample", type=int, default=None, metavar="N",
                    help="keep at most N cards per subcategory (random sample)")
    ap.add_argument("--seed", type=int, default=None,
                    help="random seed for reproducible sampling (only with --sample)")
    ap.add_argument("--icon-assets", default=DEFAULT_ICON_ASSETS,
                    help=f"dir of {{slug}}.svg/{{slug}}.color files (default {DEFAULT_ICON_ASSETS})")
    args = ap.parse_args()

    fixture, cat_counts, sub_counts = build(
        args.src, sample=args.sample, seed=args.seed, icon_assets_dir=args.icon_assets
    )
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(fixture, f, ensure_ascii=False, indent=2)
        f.write("\n")

    print(f"wrote {args.out}")
    print(f"  categories:    {len(fixture['categories'])}")
    print(f"  subcategories: {len(fixture['subcategories'])}")
    print(f"  cards:         {len(fixture['cards'])}")
    print("  cards per category: " +
          ", ".join(f"{c}={cat_counts[c]}" for c in sorted(cat_counts)))
    if args.sample is not None:
        suffix = f", seed={args.seed}" if args.seed is not None else ", seed=random"
        print(f"  (sampled: up to {args.sample} per subcategory{suffix})")


if __name__ == "__main__":
    main()
