// Pure calculation tests (spec 08, ticket 01) — mirrors the cases already covered by
// `CalculateSessionXpUseCaseTest.kt` on the Kotlin side, using the same fixture values, so the two
// independent implementations (spec 08's Further Notes) can be checked against the same expectations.
import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { ScoringState, ScoredSession, XpConfig, computeSessionXp, levelThreshold } from "./xpScoring";

// Matches CalculateSessionXpUseCaseTest.kt's own CONFIG companion object exactly, including its
// levelCurveExponent = 2.0 override (chosen there so level thresholds land on round numbers).
const CONFIG: XpConfig = {
  newCardStudied: 10,
  cardMastered: 100,
  cardPartial: 25,
  masteryDefended: 50,
  cardDemastered: -80,
  sessionCompleted: 500,
  dailyGoalMet: 1000,
  streakPerDay: 250,
  streakMaxPerDay: 2500,
  minuteStudied: 10,
  levelCurveBase: 1000.0,
  levelCurveExponent: 2.0,
};

const STARTING_STATE: ScoringState = { xp: 0, level: 1, xpIntoCurrentLevel: 0, currentStreak: 0, bestStreak: 0, lastStudyDate: "", goalMetDate: "" };

function ratedSession(overrides: Partial<ScoredSession> = {}): ScoredSession {
  return { studyMode: "Rated", durationSeconds: 0, abandoned: false, cardResults: [], ...overrides };
}

function fastSession(overrides: Partial<ScoredSession> = {}): ScoredSession {
  return { studyMode: "Fast", durationSeconds: 0, abandoned: false, cardResults: [], ...overrides };
}

describe("computeSessionXp — per-card awards", () => {
  it("a card studied for the first time earns newCardStudied", () => {
    const { breakdown } = computeSessionXp(fastSession(), 1, STARTING_STATE, CONFIG);
    assert.equal(breakdown.newCards, CONFIG.newCardStudied);
  });

  it("a card ending Mastered with no prior mastery earns cardMastered", () => {
    const session = ratedSession({ cardResults: [{ state: "Mastered", wasPreviouslyMastered: false }] });
    const { breakdown } = computeSessionXp(session, 0, STARTING_STATE, CONFIG);
    assert.equal(breakdown.mastered, CONFIG.cardMastered);
    assert.equal(breakdown.masteryDefenseBonus, 0);
  });

  it("a card ending Mastered that was already Mastered earns masteryDefended instead of cardMastered", () => {
    const session = ratedSession({ cardResults: [{ state: "Mastered", wasPreviouslyMastered: true }] });
    const { breakdown } = computeSessionXp(session, 0, STARTING_STATE, CONFIG);
    assert.equal(breakdown.mastered, 0);
    assert.equal(breakdown.masteryDefenseBonus, CONFIG.masteryDefended);
  });

  it("a card ending Partial earns cardPartial regardless of prior mastery", () => {
    const session = ratedSession({ cardResults: [{ state: "Partial" }] });
    const { breakdown } = computeSessionXp(session, 0, STARTING_STATE, CONFIG);
    assert.equal(breakdown.partial, CONFIG.cardPartial);
  });

  it("a previously mastered card ending Partial is mastery-neutral: no defense bonus, no demastery penalty", () => {
    const session = ratedSession({ cardResults: [{ state: "Partial", wasPreviouslyMastered: true }] });
    const { breakdown } = computeSessionXp(session, 0, STARTING_STATE, CONFIG);
    assert.equal(breakdown.masteryDefenseBonus, 0);
    assert.equal(breakdown.demastered, 0);
    assert.equal(breakdown.partial, CONFIG.cardPartial);
  });

  it("a previously mastered card ending Failed earns cardDemastered (negative)", () => {
    const session = ratedSession({ cardResults: [{ state: "Failed", wasPreviouslyMastered: true }] });
    const { breakdown } = computeSessionXp(session, 0, STARTING_STATE, CONFIG);
    assert.equal(breakdown.demastered, CONFIG.cardDemastered);
  });

  it("a card ending Failed that was never mastered earns nothing and costs nothing", () => {
    const session = ratedSession({ abandoned: true, cardResults: [{ state: "Failed", wasPreviouslyMastered: false }] });
    const { breakdown } = computeSessionXp(session, 0, STARTING_STATE, CONFIG);
    assert.equal(breakdown.xpTotal, 0);
  });

  it("a Fast session earns no mastery-related awards, only newCards and timeStudied", () => {
    const session = fastSession({ durationSeconds: 120, cardResults: [{ state: "Seen" }] });
    const { breakdown } = computeSessionXp(session, 1, STARTING_STATE, CONFIG);
    assert.equal(breakdown.mastered, 0);
    assert.equal(breakdown.partial, 0);
    assert.equal(breakdown.masteryDefenseBonus, 0);
    assert.equal(breakdown.demastered, 0);
    assert.equal(breakdown.newCards, CONFIG.newCardStudied);
    assert.equal(breakdown.timeStudied, 2 * CONFIG.minuteStudied);
  });
});

describe("computeSessionXp — time and completion", () => {
  it("time studied is a flat rate per whole minute", () => {
    const { breakdown } = computeSessionXp(ratedSession({ durationSeconds: 179 }), 0, STARTING_STATE, CONFIG);
    assert.equal(breakdown.timeStudied, 2 * CONFIG.minuteStudied);
  });

  it("a finished session earns the completion bonus", () => {
    const { breakdown } = computeSessionXp(ratedSession({ abandoned: false }), 0, STARTING_STATE, CONFIG);
    assert.equal(breakdown.sessionCompletionBonus, CONFIG.sessionCompleted);
  });

  it("an abandoned session omits the completion bonus but keeps every other applicable award", () => {
    const session = ratedSession({ abandoned: true, durationSeconds: 60, cardResults: [{ state: "Mastered", wasPreviouslyMastered: false }] });
    const { breakdown } = computeSessionXp(session, 1, STARTING_STATE, CONFIG);
    assert.equal(breakdown.sessionCompletionBonus, 0);
    assert.equal(breakdown.newCards, CONFIG.newCardStudied);
    assert.equal(breakdown.mastered, CONFIG.cardMastered);
    assert.equal(breakdown.timeStudied, CONFIG.minuteStudied);
  });
});

describe("computeSessionXp — level curve", () => {
  it("levelThreshold matches ceil(base * level^exponent / 1000) * 1000", () => {
    assert.equal(levelThreshold(CONFIG, 1), 1000);
    assert.equal(levelThreshold(CONFIG, 2), 4000);
    assert.equal(levelThreshold(CONFIG, 3), 9000);
  });

  it("a delta smaller than the level threshold does not cross a level", () => {
    const { newScoringState, levelsCrossed } = computeSessionXp(ratedSession({ abandoned: true }), 0, { ...STARTING_STATE, xpIntoCurrentLevel: 500 }, CONFIG);
    // no cards, no time, abandoned: xpTotal is 0, so nothing moves.
    assert.equal(newScoringState.level, 1);
    assert.equal(newScoringState.xpIntoCurrentLevel, 500);
    assert.deepEqual(levelsCrossed, []);
  });

  it("crossing exactly one level reports it once, with no burst awarded for reaching it", () => {
    const state: ScoringState = { ...STARTING_STATE, xpIntoCurrentLevel: 900 };
    const session = ratedSession({ abandoned: true, cardResults: [{ state: "Mastered", wasPreviouslyMastered: false }] }); // +100
    const { newScoringState, levelsCrossed } = computeSessionXp(session, 0, state, CONFIG);
    assert.deepEqual(levelsCrossed, [2]);
    assert.equal(newScoringState.level, 2);
    assert.equal(newScoringState.xpIntoCurrentLevel, 0);
  });

  it("crossing multiple levels in one session reports each in ascending order", () => {
    const state: ScoringState = { ...STARTING_STATE, xpIntoCurrentLevel: 0 };
    // +100*100 = 10000 xp: threshold(1)=1000, threshold(2)=4000, threshold(3)=9000 -> crosses to level 4 with 10000-1000-4000-9000=-4000? recompute below.
    const cardResults = Array.from({ length: 100 }, () => ({ state: "Mastered" as const, wasPreviouslyMastered: false }));
    const session = ratedSession({ abandoned: true, cardResults });
    const { newScoringState, levelsCrossed } = computeSessionXp(session, 0, state, CONFIG);
    // total xp = 10000 (100 mastered cards * 100). thresholds: 1000, 4000, 9000 -> cumulative 1000, 5000, 14000.
    // 10000 >= 1000 -> level 2, remainder 9000. 9000 >= 4000 -> level 3, remainder 5000. 5000 < 9000 -> stop.
    assert.deepEqual(levelsCrossed, [2, 3]);
    assert.equal(newScoringState.level, 3);
    assert.equal(newScoringState.xpIntoCurrentLevel, 5000);
  });

  it("a loss is clamped so xpIntoCurrentLevel never goes negative and the level never drops", () => {
    const priorXpIntoCurrentLevel = 50;
    const state: ScoringState = { ...STARTING_STATE, level: 2, xpIntoCurrentLevel: priorXpIntoCurrentLevel };
    const session = ratedSession({ abandoned: true, cardResults: [{ state: "Failed", wasPreviouslyMastered: true }] }); // -80
    const { newScoringState } = computeSessionXp(session, 0, state, CONFIG);
    assert.equal(newScoringState.level, 2);
    assert.equal(newScoringState.xpIntoCurrentLevel, 0);
    assert.equal(newScoringState.xp, state.xp - priorXpIntoCurrentLevel);
  });

  it("a loss smaller than the points already in the level is applied unclamped", () => {
    const state: ScoringState = { ...STARTING_STATE, level: 2, xpIntoCurrentLevel: 500 };
    const session = ratedSession({ abandoned: true, cardResults: [{ state: "Failed", wasPreviouslyMastered: true }] }); // -80
    const { newScoringState } = computeSessionXp(session, 0, state, CONFIG);
    assert.equal(newScoringState.xpIntoCurrentLevel, 420);
  });
});

describe("computeSessionXp — no rate is a literal", () => {
  // Mirrors CalculateSessionXpUseCaseTest.kt's own "unusual config" case: off-beat values that don't
  // match any documented default, so a regression that hardcodes a literal instead of reading it from
  // `config` would go undetected by every other test above (they'd still pass against the real defaults).
  const UNUSUAL_CONFIG: XpConfig = {
    newCardStudied: 3,
    cardMastered: 7,
    cardPartial: 11,
    masteryDefended: 13,
    cardDemastered: -17,
    sessionCompleted: 19,
    dailyGoalMet: 29,
    streakPerDay: 31,
    streakMaxPerDay: 37,
    minuteStudied: 23,
    levelCurveBase: 1000.0,
    levelCurveExponent: 2.0,
  };

  it("every award reads its rate from the given config, not a hardcoded default", () => {
    const session = ratedSession({
      durationSeconds: 60,
      cardResults: [
        { state: "Mastered", wasPreviouslyMastered: false },
        { state: "Mastered", wasPreviouslyMastered: true },
        { state: "Partial" },
        { state: "Failed", wasPreviouslyMastered: true },
      ],
    });
    const { breakdown } = computeSessionXp(session, 1, STARTING_STATE, UNUSUAL_CONFIG);
    assert.equal(breakdown.newCards, UNUSUAL_CONFIG.newCardStudied);
    assert.equal(breakdown.mastered, UNUSUAL_CONFIG.cardMastered);
    assert.equal(breakdown.masteryDefenseBonus, UNUSUAL_CONFIG.masteryDefended);
    assert.equal(breakdown.partial, UNUSUAL_CONFIG.cardPartial);
    assert.equal(breakdown.demastered, UNUSUAL_CONFIG.cardDemastered);
    assert.equal(breakdown.timeStudied, UNUSUAL_CONFIG.minuteStudied);
    assert.equal(breakdown.sessionCompletionBonus, UNUSUAL_CONFIG.sessionCompleted);
  });

  it("levelThreshold reads its curve parameters from the given config, not a hardcoded default", () => {
    const oddCurveConfig: XpConfig = { ...UNUSUAL_CONFIG, levelCurveBase: 500.0, levelCurveExponent: 1.0 };
    // ceil(500 * 3^1 / 1000) * 1000 = ceil(1.5) * 1000 = 2000 — matches neither the production default
    // (1000 * 3^2.5 / 1000, rounded) nor the shared test CONFIG's (1000 * 3^2 / 1000 = 9000).
    assert.equal(levelThreshold(oddCurveConfig, 3), 2000);
  });
});
