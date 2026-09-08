// Firestore Security Rules tests (spec 04 ticket 01). Small and standalone on purpose — this is a
// guard against a specific class of production-only failure (there is no other way to verify a
// rule without deploying it), not a second test framework for the project. Run via `npm test` in
// this directory, which starts the Firestore emulator (see ../firebase.json) and runs this file
// under Node's built-in test runner.
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { after, afterEach, before, describe, it } from 'node:test';
import { fileURLToPath } from 'node:url';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, setDoc } from 'firebase/firestore';

const __dirname = dirname(fileURLToPath(import.meta.url));
const PROJECT_ID = 'flashcards-rules-test';
const OWNER_UID = 'owner-uid';
const OTHER_UID = 'other-uid';

/** A minimal, syntactically valid session document (ADR-0014) — rules don't inspect its shape. */
const sessionDoc = { sessionId: 'session-1', studyMode: 'Rated' };

/** A minimal, syntactically valid progress document (ADR-0016) — rules don't inspect its shape. */
const progressDoc = { categoryId: 'cat-1', cards: {} };

/** A minimal, syntactically valid progress-summary document (ADR-0016) — rules don't inspect its shape. */
const progressSummaryDoc = { subcategories: {} };

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
  it('the owning user can create and read their own session', async () => {
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    const ownRef = doc(ownerDb, `users/${OWNER_UID}/sessions/session-1`);

    await assertSucceeds(setDoc(ownRef, sessionDoc));
    await assertSucceeds(getDoc(ownRef));
  });

  it('the owning user cannot update or delete their own session once created (CR-69 #3)', async () => {
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    const ownRef = doc(ownerDb, `users/${OWNER_UID}/sessions/session-1`);
    await setDoc(ownRef, sessionDoc);

    await assertFails(setDoc(ownRef, { ...sessionDoc, studyMode: 'Fast' }));
    await assertFails(deleteDoc(ownRef));
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

describe('users/{uid}/progress/details/subcategories/{subcategoryId}', () => {
  it('the owning user can read and write their own progress document', async () => {
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    const ownRef = doc(ownerDb, `users/${OWNER_UID}/progress/details/subcategories/sub-1`);

    await assertSucceeds(setDoc(ownRef, progressDoc));
    await assertSucceeds(getDoc(ownRef));
  });

  it('a different authenticated user cannot read or write it', async () => {
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    await setDoc(doc(ownerDb, `users/${OWNER_UID}/progress/details/subcategories/sub-1`), progressDoc);

    const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
    const foreignRef = doc(otherDb, `users/${OWNER_UID}/progress/details/subcategories/sub-1`);

    await assertFails(getDoc(foreignRef));
    await assertFails(setDoc(foreignRef, progressDoc));
  });

  it('an unauthenticated request cannot read or write it', async () => {
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    await setDoc(doc(ownerDb, `users/${OWNER_UID}/progress/details/subcategories/sub-1`), progressDoc);

    const anonDb = testEnv.unauthenticatedContext().firestore();
    const anonRef = doc(anonDb, `users/${OWNER_UID}/progress/details/subcategories/sub-1`);

    await assertFails(getDoc(anonRef));
    await assertFails(setDoc(anonRef, progressDoc));
  });
});

describe('users/{uid}/progress/{docId}', () => {
  it('the owning user can read and write their own progress-summary document', async () => {
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    const ownRef = doc(ownerDb, `users/${OWNER_UID}/progress/summary`);

    await assertSucceeds(setDoc(ownRef, progressSummaryDoc));
    await assertSucceeds(getDoc(ownRef));
  });

  it('a different authenticated user cannot read or write it', async () => {
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    await setDoc(doc(ownerDb, `users/${OWNER_UID}/progress/summary`), progressSummaryDoc);

    const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
    const foreignRef = doc(otherDb, `users/${OWNER_UID}/progress/summary`);

    await assertFails(getDoc(foreignRef));
    await assertFails(setDoc(foreignRef, progressSummaryDoc));
  });

  it('an unauthenticated request cannot read or write it', async () => {
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    await setDoc(doc(ownerDb, `users/${OWNER_UID}/progress/summary`), progressSummaryDoc);

    const anonDb = testEnv.unauthenticatedContext().firestore();
    const anonRef = doc(anonDb, `users/${OWNER_UID}/progress/summary`);

    await assertFails(getDoc(anonRef));
    await assertFails(setDoc(anonRef, progressSummaryDoc));
  });
});

describe('users/{uid}/progress/details/subcategories/{subcategoryId} nested-key merge (ADR-0016)', () => {
  it('a merge write touching one card leaves an existing untouched card byte-for-byte intact', async () => {
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    const progressRef = doc(ownerDb, `users/${OWNER_UID}/progress/details/subcategories/sub-1`);
    await setDoc(progressRef, {
      categoryId: 'cat-1',
      cards: { 'card-1': { state: 'Mastered', firstStudiedAt: new Date(0), masteredAt: new Date(0) } },
    });

    // The same shape a session commit writes for a second card studied in this Subcategory: only
    // card-2's key, via set(merge) — never a wholesale rewrite of the `cards` map.
    await setDoc(
      progressRef,
      { categoryId: 'cat-1', cards: { 'card-2': { state: 'Seen', firstStudiedAt: new Date(1) } } },
      { merge: true },
    );

    const cards = (await getDoc(progressRef)).data().cards;
    assert.equal(cards['card-1'].state, 'Mastered');
    assert.ok(cards['card-1'].masteredAt, 'card-1 must keep its masteredAt untouched');
    assert.equal(cards['card-1'].firstStudiedAt.toMillis(), 0, 'card-1 must keep its original firstStudiedAt untouched');
    assert.equal(cards['card-2'].state, 'Seen');
  });
});
