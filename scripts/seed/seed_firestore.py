#!/usr/bin/env python3
"""Stage 2 of Firestore seeding: temp fixture JSON -> Firestore.

Globs the gitignored temp dir (default `scripts/seed/.tmp/*.json`) for fixtures
produced by `build_fixture.py` and upserts them via the Firebase Admin SDK.

Schema written (see ADR-0007, ADR-0037):
  categories/{categoryId}                               → { name, order, subcategoryCount, iconSvg, color, featuredSubcategoryNames[] }
  subcategories/{categoryId-subSlug}                    → { name, nameLower, categoryId, categoryName, order, cardCount }
  subcategories/{categoryId-subSlug}/shards/{n}         → { flashcards: { "<cardId>": { id, question, answer, tags[], createdAt, ... }, ... } }

The `subcategoryId` field carried in the fixture is used to route each shard into the correct
subcollection path — it is NOT written as a Firestore document field.

`categories` gets its own write path (see `upsert_categories`), separate from the generic
`upsert()` used by `subcategories`: `iconSvg`/`color` are optional, hand-curated fields
(see docs/design/category-icon-color.md) that this pipeline must never clobber — once Firestore
holds a non-empty value for either (including one set by a manual console edit), that value wins
over whatever the checked-in fixture has, and the write uses `set(merge=True)` so any field
omitted from the payload is left untouched rather than deleted.

`shards` also gets its own write path (see `upsert_shards`), independent of --skip-existing/
--overwrite: shard membership is pure derived data (ADR-0037) with nothing hand-curated inside
it, so every run overwrites every shard doc in full, and any shard left over from a previous run
whose index no longer exists in the freshly-packed set is deleted — otherwise a subcategory that
shrinks or repacks differently would leave stale card content sitting in an orphaned shard doc.

Idempotency:
  --skip-existing (DEFAULT)  subcategories: write a doc only if its id is absent, skip if
                             present. categories, shards: not applicable — always written (see
                             `upsert_categories`/`upsert_shards` above).
  --overwrite                subcategories: set() every doc unconditionally (clobbers console
                             edits). categories: also clobbers `iconSvg`/`color` sticky fields,
                             bypassing the preserve-existing-value check — though the write is
                             still `set(merge=True)`, so a field the fixture omits (an uncurated
                             icon) is left alone rather than deleted. shards: no effect, already
                             always-overwrite.

                             Required when a NEW denormalized field is added to the
                             --skip-existing `subcategories` collection: --skip-existing writes
                             only absent doc ids, so a field like `subcategories.nameLower` never
                             reaches docs that already exist, and search silently matches
                             nothing. Dry-run first. Does NOT apply to `categories`/`shards` —
                             both are always fully written regardless of this flag.
  --dry-run                  report planned writes/skips without touching Firestore. Still reads
                             existing docs (categories: to evaluate sticky fields; shards: to
                             find orphans to delete; subcategories: to determine skip/write), it
                             just skips the write call itself.

Credentials: Application Default Credentials. Point GOOGLE_APPLICATION_CREDENTIALS
at a service-account JSON, or pass --cred <path>. The project id is taken from the
credential. The service-account JSON must never be committed.
"""
from __future__ import annotations
import argparse, glob, json, os, sys
from collections import defaultdict

DEFAULT_FIXTURES = os.path.join(os.path.dirname(__file__), ".tmp")
BATCH_LIMIT = 400  # Firestore batched-write cap is 500; stay under it.

# Firestore's commit RPC also caps total request/transaction size; shard docs run far larger
# than the tiny per-card docs BATCH_LIMIT was sized for, so upsert_shards() flushes on this byte
# budget too. A batched write is accounted as a transaction internally (index entries etc. count
# against the limit, not just raw payload bytes), which errors ("Transaction too big") at a
# noticeably smaller effective size than the ~10MiB request-size cap alone would suggest — kept
# well under that observed threshold.
SHARD_BATCH_BYTE_LIMIT = 1_000_000


def load_fixtures(fixtures_dir: str):
    paths = sorted(glob.glob(os.path.join(fixtures_dir, "*.json")))
    if not paths:
        sys.exit(f"no *.json fixtures in {fixtures_dir} — run build_fixture.py first")
    categories, subcategories, shards = [], [], []
    # Shard doc ids ("0", "1", ...) are only unique within their subcategory, so the de-dupe key
    # is the (subcategoryId, id) pair, not the bare shard id.
    seen_shard_keys: set[tuple[str, str]] = set()
    for p in paths:
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        categories += data.get("categories", [])
        subcategories += data.get("subcategories", [])
        for s in data.get("shards", []):
            key = (s["subcategoryId"], s["id"])
            if key in seen_shard_keys:
                continue  # de-dupe across multiple fixture files
            seen_shard_keys.add(key)
            shards.append(s)
    return paths, categories, subcategories, shards


def init_db(cred_path: str | None):
    try:
        import firebase_admin
        from firebase_admin import credentials, firestore
    except ImportError:
        sys.exit("firebase-admin not installed — `pip install -r requirements.txt`")
    if cred_path:
        os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = cred_path
    if not os.environ.get("GOOGLE_APPLICATION_CREDENTIALS") and not cred_path:
        sys.exit("no credentials: set GOOGLE_APPLICATION_CREDENTIALS or pass --cred")
    firebase_admin.initialize_app(credentials.ApplicationDefault())
    return firestore.client()


def existing_ids(db, collection: str) -> set[str]:
    return {ref.id for ref in db.collection(collection).list_documents()}


def existing_shard_ids(db, sub_ids: set[str]) -> dict[str, set[str]]:
    """Existing `shards/{n}` doc ids per subcategoryId, used to find orphans to delete."""
    return {
        sid: {
            ref.id
            for ref in (db.collection("subcategories")
                          .document(sid)
                          .collection("shards")
                          .list_documents())
        }
        for sid in sub_ids
    }


def upsert(db, collection: str, docs: list[dict], *, overwrite: bool, dry_run: bool):
    """Upsert flat-collection docs keyed on their 'id' field. Returns (written, skipped)."""
    present: set[str] = set()
    if not overwrite or dry_run:
        present = existing_ids(db, collection)

    to_write = [d for d in docs if overwrite or d["id"] not in present]
    skipped = len(docs) - len(to_write)

    if dry_run:
        return len(to_write), skipped

    written = 0
    batch = db.batch()
    n = 0
    for d in to_write:
        payload = {k: v for k, v in d.items() if k != "id"}
        batch.set(db.collection(collection).document(d["id"]), payload)
        n += 1
        written += 1
        if n >= BATCH_LIMIT:
            batch.commit()
            batch = db.batch()
            n = 0
    if n:
        batch.commit()
    return written, skipped


def upsert_categories(db, categories: list[dict], *, overwrite: bool, dry_run: bool):
    """Bespoke category write path — see module docstring for why this isn't the generic
    upsert(). Every category is always written (merge=True), but `iconSvg`/`color` are dropped
    from the payload whenever Firestore already holds a non-empty value for that field, so a
    manually-curated or previously-seeded value is never clobbered. Returns (written, skipped).
    """
    written = 0
    for category in categories:
        doc_ref = db.collection("categories").document(category["id"])
        # get() then set() below are not atomic: a console edit to iconSvg/color landing in this
        # window would get clobbered. Accepted risk — this is a solo, manually-run pipeline, not
        # a concurrent/scheduled one; revisit with db.transaction() if that ever changes.
        existing = {} if overwrite else (doc_ref.get().to_dict() or {})
        payload = {k: v for k, v in category.items() if k != "id"}
        for sticky_field in ("iconSvg", "color"):
            if not overwrite and existing.get(sticky_field) and sticky_field in payload:
                del payload[sticky_field]
        if not dry_run:
            doc_ref.set(payload, merge=True)
        written += 1
    return written, 0


def upsert_shards(db, shards: list[dict], *, dry_run: bool):
    """Upsert subcategories/{subcategoryId}/shards/{n} docs.

    subcategoryId is a fixture routing field — stripped from the Firestore payload. Unlike
    upsert()/upsert_categories(), there is no --skip-existing/--overwrite distinction here: shard
    membership is pure derived data (ADR-0037), so every run writes every shard doc in full. Any
    existing shard doc whose index falls outside the freshly-packed set for its subcategory is
    deleted — otherwise a subcategory that shrinks, or repacks its cards across a different
    number of shards, would leave stale card content sitting in an orphaned doc.
    Returns (written, deleted).
    """
    sub_ids = {s["subcategoryId"] for s in shards}
    existing = existing_shard_ids(db, sub_ids) if sub_ids else {}

    new_ids_by_sub: dict[str, set[str]] = defaultdict(set)
    for s in shards:
        new_ids_by_sub[s["subcategoryId"]].add(s["id"])

    to_delete = [
        (sid, shard_id)
        for sid, existing_ids in existing.items()
        for shard_id in existing_ids - new_ids_by_sub.get(sid, set())
    ]

    if dry_run:
        return len(shards), len(to_delete)

    written = 0
    batch = db.batch()
    n = 0
    batch_bytes = 0
    for s in shards:
        payload = {k: v for k, v in s.items() if k not in ("id", "subcategoryId")}
        # Shard docs run up to SHARD_BYTE_BUDGET (~700KB) each, unlike the tiny per-doc payloads
        # BATCH_LIMIT was sized for — a handful of them can already blow past Firestore's ~10MiB
        # commit-request cap, so flush on byte budget too, not just doc count.
        size = len(json.dumps(payload, ensure_ascii=False).encode("utf-8"))
        if n and batch_bytes + size > SHARD_BATCH_BYTE_LIMIT:
            batch.commit()
            batch = db.batch()
            n = 0
            batch_bytes = 0
        ref = (db.collection("subcategories")
               .document(s["subcategoryId"])
               .collection("shards")
               .document(s["id"]))
        batch.set(ref, payload)
        n += 1
        batch_bytes += size
        written += 1
        if n >= BATCH_LIMIT:
            batch.commit()
            batch = db.batch()
            n = 0
            batch_bytes = 0
    for sid, shard_id in to_delete:
        ref = (db.collection("subcategories")
               .document(sid)
               .collection("shards")
               .document(shard_id))
        batch.delete(ref)
        n += 1
        if n >= BATCH_LIMIT:
            batch.commit()
            batch = db.batch()
            n = 0
            batch_bytes = 0
    if n:
        batch.commit()
    return written, len(to_delete)


def main():
    ap = argparse.ArgumentParser(description="Upsert temp fixtures into Firestore.")
    ap.add_argument("--fixtures-dir", default=DEFAULT_FIXTURES,
                    help=f"dir of *.json fixtures (default {DEFAULT_FIXTURES})")
    ap.add_argument("--cred", help="path to service-account JSON (else GOOGLE_APPLICATION_CREDENTIALS)")
    mode = ap.add_mutually_exclusive_group()
    mode.add_argument("--skip-existing", action="store_true", default=True,
                      help="write only absent docs (default)")
    mode.add_argument("--overwrite", action="store_true",
                      help="set() every doc unconditionally")
    ap.add_argument("--dry-run", action="store_true", help="report only, no writes")
    args = ap.parse_args()
    overwrite = bool(args.overwrite)

    paths, categories, subcategories, shards = load_fixtures(args.fixtures_dir)
    total_cards = sum(len(s["flashcards"]) for s in shards)
    print(f"fixtures: {', '.join(os.path.relpath(p) for p in paths)}")
    print(f"  categories={len(categories)} subcategories={len(subcategories)} "
          f"shards={len(shards)} cards={total_cards}")
    print(f"mode: {'OVERWRITE' if overwrite else 'skip-existing'} (shards always overwrite)"
          f"{' (DRY-RUN)' if args.dry_run else ''}\n")

    db = init_db(args.cred)

    cw, cs = upsert_categories(db, categories, overwrite=overwrite, dry_run=args.dry_run)
    sw, ss = upsert(db, "subcategories", subcategories, overwrite=overwrite, dry_run=args.dry_run)
    hw, hd = upsert_shards(db, shards, dry_run=args.dry_run)

    verb = "would write" if args.dry_run else "wrote"
    del_verb = "would delete" if args.dry_run else "deleted"
    print(f"categories/                    {verb} {cw}, skipped {cs}")
    print(f"subcategories/                 {verb} {sw}, skipped {ss}")
    print(f"subcategories/*/shards/        {verb} {hw}, {del_verb} {hd} orphaned")
    if args.dry_run:
        print("\n(dry-run — nothing written)")


if __name__ == "__main__":
    main()
