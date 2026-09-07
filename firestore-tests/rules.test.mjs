// Firestore Security Rules tests (spec 04 ticket 01). Small and standalone on purpose — this is a
// guard against a specific class of production-only failure (there is no other way to verify a
// rule without deploying it), not a second test framework for the project. Run via `npm test` in
// this directory, which starts the Firestore emulator (see ../firebase.json) and runs this file
// under Node's built-in test runner.
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { after, afterEach, before, describe, it } from 'node:test';
import { fileURLToPath } from 'node:url';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc } from 'firebase/firestore';

const __dirname = dirname(fileURLToPath(import.meta.url));
const PROJECT_ID = 'flashcards-rules-test';
const OWNER_UID = 'owner-uid';
const OTHER_UID = 'other-uid';

/** A minimal, syntactically valid session document (ADR-0014) — rules don't inspect its shape. */
const sessionDoc = { sessionId: 'session-1', studyMode: 'Rated' };

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: readFileSync(join(__dirname, '..', 'firestore.rules'), 'utf8'),
      host: '127.0.0.1',
      port: 8080,
    },
  });
});

after(async () => {
  await testEnv.cleanup();
});

afterEach(async () => {
  await testEnv.clearFirestore();
});

describe('users/{uid}/sessions/{sessionId}', () => {
  it('the owning user can read and write their own session', async () => {
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    const ownRef = doc(ownerDb, `users/${OWNER_UID}/sessions/session-1`);

    await assertSucceeds(setDoc(ownRef, sessionDoc));
    await assertSucceeds(getDoc(ownRef));
  });

  it('a different authenticated user cannot read or write it', async () => {
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    await setDoc(doc(ownerDb, `users/${OWNER_UID}/sessions/session-1`), sessionDoc);

    const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
    const foreignRef = doc(otherDb, `users/${OWNER_UID}/sessions/session-1`);

    await assertFails(getDoc(foreignRef));
    await assertFails(setDoc(foreignRef, sessionDoc));
  });

  it('an unauthenticated request cannot read or write it', async () => {
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    await setDoc(doc(ownerDb, `users/${OWNER_UID}/sessions/session-1`), sessionDoc);

    const anonDb = testEnv.unauthenticatedContext().firestore();
    const anonRef = doc(anonDb, `users/${OWNER_UID}/sessions/session-1`);

    await assertFails(getDoc(anonRef));
    await assertFails(setDoc(anonRef, sessionDoc));
  });
});
