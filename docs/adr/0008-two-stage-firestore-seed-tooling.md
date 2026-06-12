# Two-stage Firestore seed tooling: local fixture intermediate, no direct inbox-to-Firestore write

## Decision

Seeding Firestore from the capture inboxes is split into two discrete stages:

1. **Stage 1 — `build_fixture.py`**: reads `~/.claude/flashcards/<cat>/<sub>/inbox.jsonl`, projects each card into the Firestore document shape, derives the taxonomy from path slugs, and writes a single intermediate JSON fixture to a gitignored temp dir (`.tmp/fixture.json`).
2. **Stage 2 — `seed_firestore.py`**: reads the temp fixture and upserts it into Firestore via the Firebase Admin SDK.

The intermediate fixture is never committed. Credentials required for Firestore writes are never required during Stage 1.

## Context

The capture inboxes live on the local machine under `~/.claude/flashcards/` and are populated by the flashcard-capture skill. They use a flat JSONL format keyed to the skill's output schema, which differs from the Firestore document shape (field names, optional additive fields, routing fields).

Two constraints shaped the tooling design:

- **Credential isolation**: Stage 2 requires a Firebase service-account JSON. Keeping the stages separate means the transformation logic (Stage 1) can be run, inspected, and debugged without any Firestore credentials present.
- **Inspectability before write**: The intermediate fixture is plain JSON, diffable and human-readable. It can be reviewed or modified before any write touches Firestore.

## Alternatives considered

**Single-script direct write** — rejected. Combines transformation and I/O in one pass; errors mid-upload leave Firestore in a partially-written state with no local artifact to inspect. Requires credentials to be present even for dry validation of the projection logic.

**Commit the fixture to git** — rejected. Fixture content mirrors the capture inboxes, which may contain PII (source field) or draft cards not yet curated for production. The gitignored `.tmp/` dir enforces that fixtures are always regenerated from the current inbox state.

**Write directly from the capture skill** — rejected. The capture skill writes to local inboxes for human review and curation. Bypassing that review step risks writing uncurated or malformed cards to production Firestore.

## Consequences

- Re-running Stage 1 is safe and idempotent — the fixture is deterministic given the same inbox state.
- Stage 2 is idempotent by default (`--skip-existing`): each card doc is keyed on its capture `id`, so re-running does not duplicate cards. `--overwrite` is available for explicit clobber.
- `--dry-run` on Stage 2 reports planned writes/skips with no Firestore mutations.
- The `subcategoryId` field is carried in the fixture as a routing key to construct the Firestore path (`subcategories/{subcategoryId}/flashcards/{cardId}`); it is stripped before the document is written so it does not appear as a Firestore field (consistent with ADR-0007).
- Multiple fixture files in `.tmp/` are merged by Stage 2; duplicate card ids across files are de-duped by Stage 2.
