# Firestore Security Rules tests

Guards a specific class of production-only failure: this repo has no other way to verify a
Firestore rule than deploying it (spec 04 ticket 01). Not a second test framework for the project —
kept small and standalone, one file, Node's built-in test runner.

## Running

```shell
npm install
npm test
```

`npm test` starts the Firestore emulator (config: `../firebase.json`) via `firebase emulators:exec`
and runs `rules.test.mjs` against it, reading rules straight from `../firestore.rules`. Requires
Node 22 and the `firebase-tools` CLI (installed locally as a devDependency here — no global install
needed).

## Coverage

`users/{uid}/sessions/{sessionId}` (ADR-0014): owner can read/write their own session; a different
authenticated user is denied; an unauthenticated request is denied. Ticket 02 extends this file with
the same three cases for `users/{uid}/progress/details/subcategories/{subcategoryId}` (ADR-0016),
plus one data test — not a rules case — proving Firestore's real nested-key `merge` write leaves an
existing card's untouched entry intact, the one failure mode ticket 02 calls out as invisible to
mocks. Ticket 03 extends it again with the user's `progress/{docId}` singletons (`summary`, and
`user-stats` once spec 05 lands).
