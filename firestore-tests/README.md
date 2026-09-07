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
authenticated user is denied; an unauthenticated request is denied. Tickets 02 and 03 extend this
file with the same three cases for the packed progress collection and the user `state` collection.
