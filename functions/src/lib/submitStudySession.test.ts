// Transaction integration tests (spec 08, ticket 01) — run against a real Firestore emulator, not a
// mock, since the whole point of this function is atomic, idempotent multi-document writes that a
// mocked Firestore could not meaningfully exercise. `npm test` (see package.json) starts the
// Firestore emulator via `firebase emulators:exec` before this file runs.
//
// Deliberately does not go through a running Functions emulator or an `onCall` HTTP round trip:
// `submitStudySession`/`validateSubmitStudySessionRequest` are called directly, the same seam
// `index.ts`'s thin `onCall` wrapper delegates to. This exercises every line this ticket is
// responsible for — the auth check `index.ts` itself performs is a single `if (!uid) throw` guard,
// trivial enough that a direct call with/without a uid covers it without needing a live Auth
// emulator and token round trip for zero extra coverage.
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { after, before, describe, it } from "node:test";
import * as admin from "firebase-admin";
import { submitStudySession, validateSubmitStudySessionRequest } from "./submitStudySession";

const TEST_PROJECT_ID = "flashcards-functions-test";

before(() => {
  admin.initializeApp({ projectId: TEST_PROJECT_ID });
});

after(async () => {
  await Promise.all(admin.apps.map((app) => app?.delete()));
});

function rawRatedRequest(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    sessionId: randomUUID(),
    studyMode: "Rated",
    startedAtEpochMillis: Date.now(),
    durationSeconds: 60,
    abandoned: false,
    categoryId: "cat-1",
    categoryName: "Category One",
    subcategoryIds: ["sub-1"],
    subcategoryNames: ["Subcategory One"],
    cardResults: [{ cardId: "card-1", subcategoryId: "sub-1", state: "Mastered", attemptsUsed: 1, wasPreviouslyMastered: false }],
    ...overrides,
  };
}

describe("validateSubmitStudySessionRequest", () => {
  it("rejects a payload missing sessionId", () => {
    const { sessionId, ...withoutSessionId } = rawRatedRequest();
    assert.throws(() => validateSubmitStudySessionRequest(withoutSessionId), /sessionId/);
  });

  it("rejects a payload missing categoryId", () => {
    const { categoryId, ...withoutCategoryId } = rawRatedRequest();
    assert.throws(() => validateSubmitStudySessionRequest(withoutCategoryId), /categoryId/);
  });

  it("rejects a non-finite durationSeconds", () => {
    assert.throws(() => validateSubmitStudySessionRequest(rawRatedRequest({ durationSeconds: Number.NaN })), /durationSeconds/);
    assert.throws(() => validateSubmitStudySessionRequest(rawRatedRequest({ durationSeconds: Number.POSITIVE_INFINITY })), /durationSeconds/);
  });

  it("rejects mismatched subcategoryIds/subcategoryNames lengths", () => {
    const request = rawRatedRequest({ subcategoryIds: ["sub-1", "sub-2"], subcategoryNames: ["Subcategory One"] });
    assert.throws(() => validateSubmitStudySessionRequest(request), /same length/);
  });

  it("rejects an unknown studyMode", () => {
    assert.throws(() => validateSubmitStudySessionRequest(rawRatedRequest({ studyMode: "Bogus" })), /studyMode/);
  });

  it("rejects a Rated cardResults entry with state Seen", () => {
    const request = rawRatedRequest({ cardResults: [{ cardId: "card-1", subcategoryId: "sub-1", state: "Seen" }] });
    assert.throws(() => validateSubmitStudySessionRequest(request), /can never be Seen/);
  });

  it("rejects a Fast cardResults entry with a non-Seen state", () => {
    const request = rawRatedRequest({ studyMode: "Fast", cardResults: [{ cardId: "card-1", subcategoryId: "sub-1", state: "Mastered" }] });
    assert.throws(() => validateSubmitStudySessionRequest(request), /must be Seen/);
  });

  it("rejects an empty cardResults array", () => {
    assert.throws(() => validateSubmitStudySessionRequest(rawRatedRequest({ cardResults: [] })), /non-empty array/);
  });

  it("rejects a duplicate cardId within cardResults", () => {
    const duplicateCardId = "card-1";
    const request = rawRatedRequest({
      cardResults: [
        { cardId: duplicateCardId, subcategoryId: "sub-1", state: "Mastered", attemptsUsed: 1, wasPreviouslyMastered: false },
        { cardId: duplicateCardId, subcategoryId: "sub-1", state: "Mastered", attemptsUsed: 1, wasPreviouslyMastered: false },
      ],
    });
    assert.throws(() => validateSubmitStudySessionRequest(request), /duplicate cardId/);
  });

  it("rejects a cardResults entry whose subcategoryId isn't declared in subcategoryIds", () => {
    const request = rawRatedRequest({ cardResults: [{ cardId: "card-1", subcategoryId: "undeclared-sub", state: "Mastered", attemptsUsed: 1, wasPreviouslyMastered: false }] });
    assert.throws(() => validateSubmitStudySessionRequest(request), /not present in subcategoryIds/);
  });

  it("rejects a cardId matching Firestore's reserved __name__ pattern", () => {
    // Firestore rejects `__.*__` outright (both as a document id and as a nested map field-path
    // segment) — reject it here, cleanly, rather than let it surface as a raw internal error from
    // deep inside the transaction.
    const request = rawRatedRequest({ cardResults: [{ cardId: "__proto__", subcategoryId: "sub-1", state: "Mastered", attemptsUsed: 1, wasPreviouslyMastered: false }] });
    assert.throws(() => validateSubmitStudySessionRequest(request), /not a valid Firestore id/);
  });

  it("rejects a subcategoryId matching Firestore's reserved __name__ pattern", () => {
    const request = rawRatedRequest({ subcategoryIds: ["__proto__"], cardResults: [{ cardId: "card-1", subcategoryId: "__proto__", state: "Mastered", attemptsUsed: 1, wasPreviouslyMastered: false }] });
    assert.throws(() => validateSubmitStudySessionRequest(request), /not a valid Firestore id/);
  });

  it("accepts a structurally valid Rated payload", () => {
    const validated = validateSubmitStudySessionRequest(rawRatedRequest());
    assert.equal(validated.studyMode, "Rated");
    assert.equal(validated.cardResults.length, 1);
  });
});

describe("submitStudySession", () => {
  it("a first submission writes the session, progress, summary and scoring-state documents and returns the correct breakdown", async () => {
    const uid = randomUUID();
    const request = validateSubmitStudySessionRequest(rawRatedRequest());

    const result = await submitStudySession(uid, request);

    assert.equal(result.breakdown.newCards, 10, "card-1 has no prior progress entry, so it counts as newly studied");
    assert.equal(result.breakdown.mastered, 100);
    assert.equal(result.breakdown.timeStudied, 10);
    assert.equal(result.breakdown.sessionCompletionBonus, 500);
    assert.equal(result.breakdown.xpTotal, 10 + 100 + 10 + 500);
    assert.equal(result.level, 1);
    assert.equal(result.xpIntoCurrentLevel, result.breakdown.xpTotal);
    assert.deepEqual(result.levelsCrossed, []);

    const db = admin.firestore();
    const sessionDoc = await db.doc(`users/${uid}/sessions/${request.sessionId}`).get();
    assert.ok(sessionDoc.exists);
    assert.equal(sessionDoc.data()?.xpTotal, result.breakdown.xpTotal);
    assert.equal(sessionDoc.data()?.cardsMastered, 1);

    const progressDoc = await db.doc(`users/${uid}/progress/details/subcategories/sub-1`).get();
    const cards = progressDoc.data()?.cards ?? {};
    assert.equal(cards["card-1"].state, "Mastered");
    assert.ok(cards["card-1"].masteredAt, "a newly mastered card must stamp masteredAt");
    assert.ok(cards["card-1"].firstStudiedAt, "a card with no prior entry must stamp firstStudiedAt");

    const summaryDoc = await db.doc(`users/${uid}/progress/summary`).get();
    assert.equal(summaryDoc.data()?.subcategories?.["sub-1"]?.masteredCount, 1);
    assert.equal(summaryDoc.data()?.subcategories?.["sub-1"]?.studiedCount, 1);

    const scoringDoc = await db.doc(`users/${uid}/progress/user-stats`).get();
    assert.equal(scoringDoc.data()?.xp, result.breakdown.xpTotal);
    assert.equal(scoringDoc.data()?.level, result.level);
  });

  it("a retried submission of an already-processed session is a no-op: same result, no double award", async () => {
    const uid = randomUUID();
    const request = validateSubmitStudySessionRequest(rawRatedRequest());

    const first = await submitStudySession(uid, request);
    const second = await submitStudySession(uid, request);

    assert.deepEqual(second, first);

    const scoringDoc = await admin.firestore().doc(`users/${uid}/progress/user-stats`).get();
    assert.equal(scoringDoc.data()?.xp, first.breakdown.xpTotal, "xp must not be awarded twice");
  });

  it("two concurrent submissions of the same not-yet-processed session award exactly once (the actual race the idempotency check exists for)", async () => {
    const uid = randomUUID();
    const request = validateSubmitStudySessionRequest(rawRatedRequest());

    // Sequential calls (the test above) never race the "does sessionId already exist" check itself —
    // both transactions here start from a state where the session doc genuinely does not exist yet.
    const [first, second] = await Promise.all([submitStudySession(uid, request), submitStudySession(uid, request)]);

    assert.deepEqual(second, first, "one of the two concurrent calls must retry and observe the other's committed write");

    const scoringDoc = await admin.firestore().doc(`users/${uid}/progress/user-stats`).get();
    assert.equal(scoringDoc.data()?.xp, first.breakdown.xpTotal, "xp must not be awarded twice");

    const summaryDoc = await admin.firestore().doc(`users/${uid}/progress/summary`).get();
    assert.equal(summaryDoc.data()?.subcategories?.["sub-1"]?.studiedCount, 1, "the card must not be counted studied twice");
  });

  it("two different sessions submitted in close succession both apply, neither lost", async () => {
    const uid = randomUUID();
    const subcategoryId = "sub-1";
    const requestA = validateSubmitStudySessionRequest(
      rawRatedRequest({ cardResults: [{ cardId: "card-a", subcategoryId, state: "Mastered", attemptsUsed: 1, wasPreviouslyMastered: false }] }),
    );
    const requestB = validateSubmitStudySessionRequest(
      rawRatedRequest({ cardResults: [{ cardId: "card-b", subcategoryId, state: "Mastered", attemptsUsed: 1, wasPreviouslyMastered: false }] }),
    );

    const [resultA, resultB] = await Promise.all([submitStudySession(uid, requestA), submitStudySession(uid, requestB)]);

    // Whichever transaction Firestore serializes first sees level 1 -> 1 (or crosses into a level
    // the other then starts from); either way, applied together they must sum, never clobber.
    assert.equal(resultA.breakdown.xpTotal, resultB.breakdown.xpTotal, "both sessions earn the identical award shape");

    const scoringDoc = await admin.firestore().doc(`users/${uid}/progress/user-stats`).get();
    assert.equal(scoringDoc.data()?.xp, resultA.breakdown.xpTotal + resultB.breakdown.xpTotal);

    const summaryDoc = await admin.firestore().doc(`users/${uid}/progress/summary`).get();
    assert.equal(summaryDoc.data()?.subcategories?.[subcategoryId]?.masteredCount, 2);
    assert.equal(summaryDoc.data()?.subcategories?.[subcategoryId]?.studiedCount, 2);
  });

  it("a defended card (already Mastered) produces no progress write and earns the defense bonus, not a fresh mastery", async () => {
    const uid = randomUUID();
    const subcategoryId = "sub-1";
    const cardId = "card-1";
    const first = validateSubmitStudySessionRequest(
      rawRatedRequest({ cardResults: [{ cardId, subcategoryId, state: "Mastered", attemptsUsed: 1, wasPreviouslyMastered: false }] }),
    );
    await submitStudySession(uid, first);

    const second = validateSubmitStudySessionRequest(
      rawRatedRequest({ cardResults: [{ cardId, subcategoryId, state: "Mastered", attemptsUsed: 1, wasPreviouslyMastered: true }] }),
    );
    const result = await submitStudySession(uid, second);

    assert.equal(result.breakdown.mastered, 0);
    assert.equal(result.breakdown.masteryDefenseBonus, 50);

    const summaryDoc = await admin.firestore().doc(`users/${uid}/progress/summary`).get();
    // Only the first session's mastery counted; the defended session's masteredDelta is 0.
    assert.equal(summaryDoc.data()?.subcategories?.[subcategoryId]?.masteredCount, 1);
  });

  it("a card Failed on its first exposure still counts as studied, exactly like a Fast session's Seen card", async () => {
    const uid = randomUUID();
    const request = validateSubmitStudySessionRequest(
      rawRatedRequest({ cardResults: [{ cardId: "card-1", subcategoryId: "sub-1", state: "Failed", attemptsUsed: 3, wasPreviouslyMastered: false }] }),
    );

    const result = await submitStudySession(uid, request);

    // No mastery/demastery XP for a card that was never mastered to begin with — but the flat
    // per-card newCards award still applies: the card was studied this session either way.
    assert.equal(result.breakdown.mastered, 0);
    assert.equal(result.breakdown.demastered, 0);
    assert.equal(result.breakdown.newCards, 10);

    const progressDoc = await admin.firestore().doc(`users/${uid}/progress/details/subcategories/sub-1`).get();
    const card = progressDoc.data()?.cards?.["card-1"];
    assert.equal(card.state, "Failed", "the terminal state itself is preserved, not collapsed into Seen");
    assert.ok(card.firstStudiedAt, "first exposure to a card must stamp firstStudiedAt regardless of outcome");
    assert.equal(card.masteredAt, undefined, "a Failed card must never stamp masteredAt");

    const summaryDoc = await admin.firestore().doc(`users/${uid}/progress/summary`).get();
    assert.equal(
      summaryDoc.data()?.subcategories?.["sub-1"]?.studiedCount,
      1,
      "the subcategory's studied-card progress must increase even though the terminal state was Failed — " +
        "the user still saw and studied that card",
    );
    assert.equal(summaryDoc.data()?.subcategories?.["sub-1"]?.masteredCount, 0);
  });
});
