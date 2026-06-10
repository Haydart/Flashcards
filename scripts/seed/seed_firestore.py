#!/usr/bin/env python3
"""Stage 2 of Firestore seeding: temp fixture JSON -> Firestore.

Globs the gitignored temp dir (default `scripts/seed/.tmp/*.json`) for fixtures
produced by `build_fixture.py` and upserts them via the Firebase Admin SDK.

Schema written (see ADR-0008):
  categories/{categoryId}                               → { name, order, subcategoryCount, iconUrl }
  subcategories/{categoryId-subSlug}                    → { name, categoryId, order, cardCount }
  subcategories/{categoryId-subSlug}/flashcards/{cardId} → { question, answer, tags[], createdAt, ... }

The `subcategoryId` field carried in the fixture is used to route each card into the correct
subcollection path — it is NOT written as a Firestore document field.

Idempotency:
  --skip-existing (DEFAULT)  write a doc only if its id is absent; skip if present.
  --overwrite                set() every doc unconditionally (clobbers console edits).
  --dry-run                  report planned writes/skips without touching Firestore.

Credentials: Application Default Credentials. Point GOOGLE_APPLICATION_CREDENTIALS
at a service-account JSON, or pass --cred <path>. The project id is taken from the
credential. The service-account JSON must never be committed.
"""
from __future__ import annotations
import argparse, glob, json, os, sys
from collections import defaultdict

DEFAULT_FIXTURES = os.path.join(os.path.dirname(__file__), ".tmp")
BATCH_LIMIT = 400  # Firestore batched-write cap is 500; stay under it.


def load_fixtures(fixtures_dir: str):
    paths = sorted(glob.glob(os.path.join(fixtures_dir, "*.json")))
    if not paths:
        sys.exit(f"no *.json fixtures in {fixtures_dir} — run build_fixture.py first")
    categories, subcategories, cards = [], [], []
    seen_card_ids: set[str] = set()
    for p in paths:
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        categories += data.get("categories", [])
        subcategories += data.get("subcategories", [])
        for c in data.get("cards", []):
            if c["id"] in seen_card_ids:
                continue  # de-dupe across multiple fixture files
            seen_card_ids.add(c["id"])
            cards.append(c)
    return paths, categories, subcategories, cards


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


def existing_card_ids(db, sub_ids: set[str]) -> set[str]:
    existing: set[str] = set()
    for sid in sub_ids:
        for ref in (db.collection("subcategories")
                      .document(sid)
                      .collection("flashcards")
                      .list_documents()):
            existing.add(ref.id)
    return existing


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


def upsert_cards(db, cards: list[dict], *, overwrite: bool, dry_run: bool):
    """Upsert cards into subcategories/{subcategoryId}/flashcards/{cardId} subcollections.

    subcategoryId is a fixture routing field — stripped from the Firestore payload.
    Returns (written, skipped).
    """
    sub_ids = {c["subcategoryId"] for c in cards}
    present: set[str] = set()
    if not overwrite or dry_run:
        present = existing_card_ids(db, sub_ids)

    to_write = [c for c in cards if overwrite or c["id"] not in present]
    skipped = len(cards) - len(to_write)

    if dry_run:
        return len(to_write), skipped

    written = 0
    batch = db.batch()
    n = 0
    for c in to_write:
        payload = {k: v for k, v in c.items() if k not in ("id", "subcategoryId")}
        ref = (db.collection("subcategories")
               .document(c["subcategoryId"])
               .collection("flashcards")
               .document(c["id"]))
        batch.set(ref, payload)
        n += 1
        written += 1
        if n >= BATCH_LIMIT:
            batch.commit()
            batch = db.batch()
            n = 0
    if n:
        batch.commit()
    return written, skipped


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

    paths, categories, subcategories, cards = load_fixtures(args.fixtures_dir)
    print(f"fixtures: {', '.join(os.path.relpath(p) for p in paths)}")
    print(f"  categories={len(categories)} subcategories={len(subcategories)} cards={len(cards)}")
    print(f"mode: {'OVERWRITE' if overwrite else 'skip-existing'}"
          f"{' (DRY-RUN)' if args.dry_run else ''}\n")

    db = init_db(args.cred)

    cw, cs = upsert(db, "categories", categories, overwrite=overwrite, dry_run=args.dry_run)
    sw, ss = upsert(db, "subcategories", subcategories, overwrite=overwrite, dry_run=args.dry_run)
    kw, ks = upsert_cards(db, cards, overwrite=overwrite, dry_run=args.dry_run)

    verb = "would write" if args.dry_run else "wrote"
    print(f"categories/                    {verb} {cw}, skipped {cs}")
    print(f"subcategories/                 {verb} {sw}, skipped {ss}")
    print(f"subcategories/*/flashcards/    {verb} {kw}, skipped {ks}")
    if args.dry_run:
        print("\n(dry-run — nothing written)")


if __name__ == "__main__":
    main()
