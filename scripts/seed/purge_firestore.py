#!/usr/bin/env python3
"""Purge all Firestore data written by the seed pipeline.

Deletes every document in:
  subcategories/{id}/flashcards/*   (subcollections first)
  subcategories/*
  categories/*

Credentials: Application Default Credentials. Point GOOGLE_APPLICATION_CREDENTIALS
at a service-account JSON, or pass --cred <path>.
"""
from __future__ import annotations
import argparse, os, sys

BATCH_LIMIT = 400


def init_db(cred_path: str | None):
    try:
        import firebase_admin
        from firebase_admin import credentials, firestore
    except ImportError:
        sys.exit("firebase-admin not installed — `pip install -r requirements.txt`")
    if cred_path:
        os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = cred_path
    if not os.environ.get("GOOGLE_APPLICATION_CREDENTIALS"):
        sys.exit("no credentials: set GOOGLE_APPLICATION_CREDENTIALS or pass --cred")
    firebase_admin.initialize_app(credentials.ApplicationDefault())
    return firestore.client()


def delete_collection(db, col_ref, batch_size: int = BATCH_LIMIT) -> int:
    deleted = 0
    while True:
        docs = list(col_ref.limit(batch_size).stream())
        if not docs:
            break
        batch = db.batch()
        for doc in docs:
            batch.delete(doc.reference)
        batch.commit()
        deleted += len(docs)
    return deleted


def main():
    ap = argparse.ArgumentParser(description="Purge all seed data from Firestore.")
    ap.add_argument("--cred", help="path to service-account JSON (else GOOGLE_APPLICATION_CREDENTIALS)")
    ap.add_argument("--dry-run", action="store_true", help="report counts without deleting")
    args = ap.parse_args()

    db = init_db(args.cred)

    if args.dry_run:
        sub_docs = list(db.collection("subcategories").stream())
        total_cards = sum(
            len(list(s.reference.collection("flashcards").stream()))
            for s in sub_docs
        )
        print(f"[dry-run] would delete:")
        print(f"  {total_cards} flashcard docs")
        print(f"  {len(sub_docs)} subcategory docs")
        cats = len(list(db.collection("categories").stream()))
        print(f"  {cats} category docs")
        return

    print("Purging subcategories/*/flashcards ...")
    sub_docs = list(db.collection("subcategories").stream())
    total_cards = 0
    for sub in sub_docs:
        total_cards += delete_collection(db, sub.reference.collection("flashcards"))
    print(f"  deleted {total_cards}")

    print("Purging subcategories/ ...")
    n = delete_collection(db, db.collection("subcategories"))
    print(f"  deleted {n}")

    print("Purging categories/ ...")
    n = delete_collection(db, db.collection("categories"))
    print(f"  deleted {n}")

    print("Done.")


if __name__ == "__main__":
    main()
