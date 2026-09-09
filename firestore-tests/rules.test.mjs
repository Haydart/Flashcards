// Firestore Security Rules tests (spec 04 ticket 01, locked down further by spec 08 ticket 04). Small
// and standalone on purpose — this is a guard against a specific class of production-only failure
// (there is no other way to verify a rule without deploying it), not a second test framework for the
// project. Run via `npm test` in this directory, which starts the Firestore emulator (see
// ../firebase.json) and runs this file under Node's built-in test runner.
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

/** A minimal, syntactically valid scoring-state document (spec 05 ticket 02) — rules don't inspect its shape. */
const scoringStateDoc = { xp: 0, level: 1, xpIntoCurrentLevel: 0, currentStreak: 0, bestStreak: 0, lastStudyDate: '', goalMetDate: '' };

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

/**
 * Seeds a document straight past security rules, the way `submitStudySession`'s Admin SDK context
 * would — spec 08 ticket 04 made every document below client-read-only, so a client `setDoc` can no
 * longer be used to arrange fixtures for the read/foreign-user/unauthenticated assertions.
 */
async function seedAsAdmin(path, data) {
  await testEnv.withSecurityRulesDisabled(async (adminContext) => {
    await setDoc(doc(adminContext.firestore(), path), data);
  });
}

describe('users/{uid}/sessions/{sessionId} (spec 08 ticket 04: client-read-only)', () => {
  it('the owning user can read their own session', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/sessions/session-1`, sessionDoc);
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();

    await assertSucceeds(getDoc(doc(ownerDb, `users/${OWNER_UID}/sessions/session-1`)));
  });

  it('the owning user cannot create, update or delete their own session', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/sessions/session-1`, sessionDoc);
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    const ownRef = doc(ownerDb, `users/${OWNER_UID}/sessions/session-1`);

    await assertFails(setDoc(doc(ownerDb, `users/${OWNER_UID}/sessions/session-2`), sessionDoc));
    await assertFails(setDoc(ownRef, { ...sessionDoc, studyMode: 'Fast' }));
    await assertFails(deleteDoc(ownRef));
  });

  it('a different authenticated user cannot read or write it', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/sessions/session-1`, sessionDoc);
    const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
    const foreignRef = doc(otherDb, `users/${OWNER_UID}/sessions/session-1`);

    await assertFails(getDoc(foreignRef));
    await assertFails(setDoc(foreignRef, sessionDoc));
  });

  it('an unauthenticated request cannot read or write it', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/sessions/session-1`, sessionDoc);
    const anonDb = testEnv.unauthenticatedContext().firestore();
    const anonRef = doc(anonDb, `users/${OWNER_UID}/sessions/session-1`);

    await assertFails(getDoc(anonRef));
    await assertFails(setDoc(anonRef, sessionDoc));
  });
});

describe('users/{uid}/progress/details/subcategories/{subcategoryId} (spec 08 ticket 04: client-read-only)', () => {
  it('the owning user can read their own progress document', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/progress/details/subcategories/sub-1`, progressDoc);
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();

    await assertSucceeds(getDoc(doc(ownerDb, `users/${OWNER_UID}/progress/details/subcategories/sub-1`)));
  });

  it('the owning user cannot write their own progress document', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/progress/details/subcategories/sub-1`, progressDoc);
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    const ownRef = doc(ownerDb, `users/${OWNER_UID}/progress/details/subcategories/sub-1`);

    await assertFails(setDoc(ownRef, progressDoc));
  });

  it('a different authenticated user cannot read or write it', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/progress/details/subcategories/sub-1`, progressDoc);
    const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
    const foreignRef = doc(otherDb, `users/${OWNER_UID}/progress/details/subcategories/sub-1`);

    await assertFails(getDoc(foreignRef));
    await assertFails(setDoc(foreignRef, progressDoc));
  });

  it('an unauthenticated request cannot read or write it', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/progress/details/subcategories/sub-1`, progressDoc);
    const anonDb = testEnv.unauthenticatedContext().firestore();
    const anonRef = doc(anonDb, `users/${OWNER_UID}/progress/details/subcategories/sub-1`);

    await assertFails(getDoc(anonRef));
    await assertFails(setDoc(anonRef, progressDoc));
  });
});

describe('users/{uid}/progress/{docId} (spec 08 ticket 04: client-read-only)', () => {
  it('the owning user can read their own progress-summary document', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/progress/summary`, progressSummaryDoc);
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();

    await assertSucceeds(getDoc(doc(ownerDb, `users/${OWNER_UID}/progress/summary`)));
  });

  it('the owning user cannot write their own progress-summary document', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/progress/summary`, progressSummaryDoc);
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    const ownRef = doc(ownerDb, `users/${OWNER_UID}/progress/summary`);

    await assertFails(setDoc(ownRef, progressSummaryDoc));
  });

  it('a different authenticated user cannot read or write it', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/progress/summary`, progressSummaryDoc);
    const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
    const foreignRef = doc(otherDb, `users/${OWNER_UID}/progress/summary`);

    await assertFails(getDoc(foreignRef));
    await assertFails(setDoc(foreignRef, progressSummaryDoc));
  });

  it('an unauthenticated request cannot read or write it', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/progress/summary`, progressSummaryDoc);
    const anonDb = testEnv.unauthenticatedContext().firestore();
    const anonRef = doc(anonDb, `users/${OWNER_UID}/progress/summary`);

    await assertFails(getDoc(anonRef));
    await assertFails(setDoc(anonRef, progressSummaryDoc));
  });
});

describe('users/{uid}/progress/user-stats (spec 05 ticket 02, spec 08 ticket 04: client-read-only)', () => {
  it('the owning user can read their own scoring-state document', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/progress/user-stats`, scoringStateDoc);
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();

    await assertSucceeds(getDoc(doc(ownerDb, `users/${OWNER_UID}/progress/user-stats`)));
  });

  it('the owning user cannot write their own scoring-state document', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/progress/user-stats`, scoringStateDoc);
    const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
    const ownRef = doc(ownerDb, `users/${OWNER_UID}/progress/user-stats`);

    await assertFails(setDoc(ownRef, scoringStateDoc));
  });

  it('a different authenticated user cannot read or write it', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/progress/user-stats`, scoringStateDoc);
    const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
    const foreignRef = doc(otherDb, `users/${OWNER_UID}/progress/user-stats`);

    await assertFails(getDoc(foreignRef));
    await assertFails(setDoc(foreignRef, scoringStateDoc));
  });

  it('an unauthenticated request cannot read or write it', async () => {
    await seedAsAdmin(`users/${OWNER_UID}/progress/user-stats`, scoringStateDoc);
    const anonDb = testEnv.unauthenticatedContext().firestore();
    const anonRef = doc(anonDb, `users/${OWNER_UID}/progress/user-stats`);

    await assertFails(getDoc(anonRef));
    await assertFails(setDoc(anonRef, scoringStateDoc));
  });
});
