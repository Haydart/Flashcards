# Flashcard Difficulty — Anchor Creation

## Purpose

Every flashcard subcategory needs a `difficulty_anchors.json` file — three calibration
questions spanning the low, middle, and high end of the 1–10 difficulty scale for that
domain. These anchors are the reference ruler that all future scoring (both live capture
and retroactive backfill) is measured against.

Difficulty is **domain-relative**: a score of 3 in `android/compose` and a score of 3
in `kotlin/coroutines` both mean "a beginner in that area gets this right." Never score
globally across all of programming.

Anchor creation must be completed before retroactive backfill can run.
See `docs/DIFFICULTY_BACKFILL.md` for the backfill procedure.

---

## Invocation

```
@DIFFICULTY_ANCHOR.md <category/subcategory>
```

Example:
```
@DIFFICULTY_ANCHOR.md android/compose
```

---

## Key paths

| Resource | Path |
|---|---|
| Inbox file | `~/.claude/flashcards/<category>/<subcategory>/inbox.jsonl` |
| Anchors file | `~/.claude/flashcards/<category>/<subcategory>/difficulty_anchors.json` |
| Progress tracker | `docs/DIFFICULTY_PROGRESS.md` (in this repo) |

---

## Difficulty scoring signals

| Signal | Low (1–3) | Mid (4–6) | High (7–10) |
|---|---|---|---|
| Prerequisite count | 0–1 concepts assumed | 2–3 | 4+ or deep ones |
| Question type | Definition / "what is X" | Mechanism / tradeoff / when-to-use | Edge case / pitfall / cross-concept synthesis |
| Obscurity | Core API surface | Common idiom | Niche / rarely encountered |
| Surprise factor | Competent beginner gets it right | Practitioner might get it wrong | Expert might get it wrong |

Weight **prerequisite count** and **question type** more heavily than obscurity alone.
Final score = weighted average of all four signals, rounded to nearest integer.

---

## Procedure

### Step 1 — Check for existing anchors file

```bash
cat ~/.claude/flashcards/<category>/<subcategory>/difficulty_anchors.json 2>/dev/null
```

- **File exists** → print its contents and ask the user whether to keep or replace it.
  If kept, mark Section 1 checkbox in the progress tracker and stop.
- **File missing** → continue to Step 2.

### Step 2 — Sample the inbox

Read 50 random lines from `inbox.jsonl` to understand the topic range and vocabulary
in this subcategory. Random sampling ensures coverage of cards added at different times.

```bash
shuf -n 50 ~/.claude/flashcards/<category>/<subcategory>/inbox.jsonl
```

Parse each line as JSON. Read the `question` field of each card.

### Step 3 — Draft 5–10 anchor questions

Internally draft between 5 and 10 hypothetical questions (not necessarily from the
inbox) that together span the full 1–10 scale for this subcategory's domain. Aim for
good distribution — don't cluster anchors in the middle. At minimum, cover each band:

| Band | Score range | Character |
|---|---|---|
| Beginner | 1–2 | Definition, basic syntax, first-hour material |
| Foundational | 3–4 | Common usage, simple mechanism, practitioner starting point |
| Intermediate | 5–6 | Tradeoffs, idioms, needs hands-on experience |
| Advanced | 7–8 | Edge cases, cross-concept synthesis, niche pitfalls |
| Expert | 9–10 | Obscure internals, rarely encountered, expert might get wrong |

For each draft, apply the signals table to confirm the score:
- **Prerequisite count** and **question type** carry the most weight.
- **Obscurity** and **surprise factor** are secondary.

Aim for scores that are genuinely well-separated — e.g. 1, 3, 5, 7, 9 covers the
scale evenly with 5 anchors; 1, 2, 4, 6, 7, 9, 10 with 7; etc. Avoid clustering
(e.g. four anchors all scoring 4–6 is useless as a ruler).

These are calibration anchors — they do NOT become flashcards.

### Step 4 — Confirm with user

Print all drafted anchors in a single message (numbered list, score and question per
line). Then call `AskUserQuestion` with a fast-approve prompt:

```
question:    "Approve all <N> anchors as drafted, or review each one?"
header:      "Approve anchors"
multiSelect: false
options:
  - label: "Approve all"
    description: "Accept every proposed anchor and score as-is."
  - label: "Review each"
    description: "Go through anchors one by one to adjust scores or replace questions."
```

**"Approve all"** → skip per-anchor review; proceed directly to Step 5 with all drafted
anchors as confirmed.

**"Review each"** → confirm in batches using `AskUserQuestion` — up to 4 anchors per
call (tool maximum). Each question in the call covers one anchor:

```
question: "Anchor <N> — score <score>: keep or replace?"
header:   "Anchor <N> (score <score>)"
multiSelect: false
options:
  - "Keep it"       → description: <drafted question text>
  - "Adjust score"  → description: "Type the corrected score in the Other field."
  - "Replace it"    → description: "Type a better question in the Other field."
```

Issue as many calls as needed (e.g. 8 anchors = 2 calls of 4). If the user adjusts a
score, update the integer. If they replace a question, re-score it with the signals
table. Collect all confirmed (question, difficulty) pairs.

### Step 5 — Write the anchors file

Sort confirmed anchors by `difficulty` ascending before writing.

```json
{
  "category": "<category>",
  "subcategory": "<subcategory>",
  "anchors": [
    { "difficulty": <N>, "question": "<question>" },
    { "difficulty": <N>, "question": "<question>" },
    { "difficulty": <N>, "question": "<question>" },
    { "difficulty": <N>, "question": "<question>" },
    { "difficulty": <N>, "question": "<question>" }
  ]
}
```

Write to `~/.claude/flashcards/<category>/<subcategory>/difficulty_anchors.json` with
2-space indentation. This file is human-editable; it is not JSONL.

### Step 6 — Update progress tracker

In `docs/DIFFICULTY_PROGRESS.md`, check the Section 1 box for this subcategory:

```
- [x] <category>/<subcategory>
```

### Step 7 — Report

```
Anchors written for <category>/<subcategory>. N anchors.
Score <N>: <question>
Score <N>: <question>
...

Ready for: @DIFFICULTY_BACKFILL.md <category>/<subcategory>
```

---

## Anchor file schema (reference)

```json
{
  "category": "android",
  "subcategory": "compose",
  "anchors": [
    { "difficulty": 1, "question": "What does the @Composable annotation do?" },
    { "difficulty": 3, "question": "What is the difference between `remember` and `rememberSaveable`?" },
    { "difficulty": 5, "question": "Why does reading a `State` object inside a lambda delay recomposition to that lambda's scope?" },
    { "difficulty": 6, "question": "Why does `remember` not survive configuration changes?" },
    { "difficulty": 8, "question": "Why can a composable that reads a `StateFlow` skip recomposition even when the flow emits a new value?" },
    { "difficulty": 9, "question": "How does `derivedStateOf` interact with recomposition when its lambda captures a state object that changes outside its scope?" }
  ]
}
```

Scores are whatever integers 1–10 were confirmed by the user. Sort by `difficulty`
ascending. Aim for at least 5 entries spread across the full scale.

---

## Constraints

- **Do not commit**: anchor files live in `~/.claude/flashcards/`, outside this repo.
  Only `docs/DIFFICULTY_PROGRESS.md` changes inside the repo. The user commits separately.
- **Anchors do not become flashcards**: they are calibration references only.
- **5–10 anchors, well-distributed**: cover all five difficulty bands (beginner /
  foundational / intermediate / advanced / expert). Do not write a file with fewer than
  5 anchors or with anchors clustered in a narrow score range — a clustered set is
  useless as a ruler for the full 1–10 scale.

---

## References

- Backfill procedure: `docs/DIFFICULTY_BACKFILL.md`
- Progress tracker: `docs/DIFFICULTY_PROGRESS.md`
- Flashcard capture skill: `~/.claude/skills/flashcard-capture/SKILL.md`
