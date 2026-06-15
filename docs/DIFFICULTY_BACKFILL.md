# Flashcard Difficulty — Retroactive Backfill

## Purpose

Existing `inbox.jsonl` cards were written before the `difficulty` field existed. This
procedure scores each card 1–10 and writes the field back, in batches of 50, using the
subcategory's `difficulty_anchors.json` as the calibration reference.

Difficulty is **domain-relative**: a score of 3 in `android/compose` and a score of 3
in `kotlin/coroutines` both mean "a beginner in that area gets this right." Never score
globally across all of programming.

**Anchor creation must be completed before backfill can run.** If the anchors file is
missing, stop and direct the user to `docs/DIFFICULTY_ANCHOR.md` first.

---

## Invocation

```
@DIFFICULTY_BACKFILL.md <category/subcategory>
```

Example:
```
@DIFFICULTY_BACKFILL.md android/compose
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

### Step 1 — Load anchors

```bash
cat ~/.claude/flashcards/<category>/<subcategory>/difficulty_anchors.json
```

If missing, stop and print:
```
No anchors file found for <category>/<subcategory>.
Run: @DIFFICULTY_ANCHOR.md <category>/<subcategory>
```

Load the anchors into working memory. They define three reference points on the 1–10
scale — a ruler against which every card in this batch is measured.

### Step 2 — Identify the next unchecked batch

Read `docs/DIFFICULTY_PROGRESS.md`. Find the Section 2 block for this subcategory.
Locate the **first unchecked** batch checkbox and note its card range (e.g. "cards 51–100").

If all batches are already checked: print "All batches complete for <category>/<subcategory>."
and stop.

### Step 3 — Read the batch

Read `~/.claude/flashcards/<category>/<subcategory>/inbox.jsonl` in full. Extract the
lines corresponding to the batch range (line numbers, 1-indexed). Parse each as JSON.

Skip any card that already has a `difficulty` field — it was scored in a prior run.
If the entire batch is already scored, mark it checked and report.

### Step 4 — Score each card

For each card in the batch:

1. Read the `question` field (and `question_spoken` if present).
2. Apply the four signals table. Reason through each signal explicitly.
3. Compare against the loaded anchors as a relative ruler:
   - Clearly easier than the lowest anchor → score 1 or 2.
   - Matches the lowest anchor in difficulty → score near its value.
   - Falls between two anchors → interpolate proportionally.
   - Matches the highest anchor → score near its value.
   - Harder than the highest anchor → score 9 or 10.
4. Assign an integer. Add `"difficulty": <score>` to the card object.

Do not change any other field. Insert `difficulty` immediately after `tags` for
readability — JSON is unordered so any position is valid.

### Step 5 — Write back

`inbox.jsonl` is normally append-only per the flashcard-capture skill. Backfilling is
an explicit exception — a missing field is being added, not content changed.

1. Read all lines of the inbox file. Record the line count.
2. Replace only the batch lines with their updated versions.
3. Write the entire file back: one JSON object per line, no trailing comma, no array
   wrapper, no pretty-printing. Preserve every line outside the batch byte-for-byte.
4. Verify the line count after writing — must be identical to before.

### Step 6 — Update progress tracker

In `docs/DIFFICULTY_PROGRESS.md`, check the batch box:

```
- [x] Batch N (cards X–Y)
```

### Step 7 — Report and stop

```
Batch N complete: <category>/<subcategory> cards X–Y scored.
N cards updated, M already had difficulty (skipped).

Next: @DIFFICULTY_BACKFILL.md <category>/<subcategory>
(or run /compact first if context is large)
```

**Do not proceed to the next batch automatically.** The user may want to review,
compact context, or stop. Wait for re-invocation.

---

## Constraints

- **Anchor-first**: never score a batch without a confirmed `difficulty_anchors.json`.
  Scores without a shared anchor scale are inconsistent across sessions.
- **One batch per invocation**: always stop after one batch of 50. Context grows with
  each card scored; compacting between batches prevents degraded reasoning quality.
- **Score every card, skip none** (except already-scored cards): partial batches corrupt
  curriculum ordering downstream.
- **Do not change card content**: only `difficulty` is added. `question`, `answer`,
  `extended_context`, `tags`, `id`, `created_at`, `status` — all unchanged.
- **Line count invariant**: verify line count before and after write. Mismatch means
  data was lost — do not proceed, report the error.
- **Do not commit**: cards live in `~/.claude/flashcards/`, outside this repo. Only
  `docs/DIFFICULTY_PROGRESS.md` changes inside the repo. The user commits separately.

---

## References

- Anchor creation: `docs/DIFFICULTY_ANCHOR.md`
- Progress tracker: `docs/DIFFICULTY_PROGRESS.md`
- Flashcard capture skill: `~/.claude/skills/flashcard-capture/SKILL.md`
