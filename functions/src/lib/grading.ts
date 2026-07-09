import { VertexAI, SchemaType } from "@google-cloud/vertexai";
import { HttpError } from "./httpError";

const VERTEX_LOCATION = "us-central1";
const GEMINI_MODEL = "gemini-2.5-flash";

export interface GradeResult {
  sanitizedTranscript: string;
  gradePercent: number;
  feedback: string;
}

interface GeminiGradeJson {
  sanitized_transcript: string;
  grade: number;
  feedback: string;
}

const RESPONSE_SCHEMA = {
  type: SchemaType.OBJECT,
  properties: {
    sanitized_transcript: { type: SchemaType.STRING },
    grade: { type: SchemaType.INTEGER },
    feedback: { type: SchemaType.STRING },
  },
  required: ["sanitized_transcript", "grade", "feedback"],
};

function buildPrompt(question: string, expectedAnswer: string, rawTranscript: string): string {
  return `You grade a spoken flashcard answer for a study app.

Flashcard question: ${question}
Expected answer: ${expectedAnswer}
Raw spoken transcript (may contain filler words and disfluencies): ${rawTranscript}

Do two things and return them as the single JSON object described by the response schema:
1. "sanitized_transcript": the transcript with filler/disfluencies (um, uh, repeated words)
   normalized, and any personal information the speaker incidentally said (names, emails,
   phone numbers, addresses) removed/redacted. Keep the substantive answer content intact.
2. "grade": an integer 0-100 for how completely and correctly the transcript answers the
   question relative to the expected answer. Give partial credit for partially correct or
   incomplete answers; do not default to round numbers just because they look tidy.
3. "feedback": one short spoken-aloud sentence, tone depends on the grade:
   - grade < 40 (failed) or grade 40-79 (partial): state briefly what was missed, then include
     the full expected answer verbatim so the user hears the correct answer.
   - grade >= 80 (correct): a short affirmation only — do not repeat the expected answer.`;
}

let vertexAi: VertexAI | null = null;

function getVertexAi(): VertexAI {
  if (!vertexAi) {
    vertexAi = new VertexAI({
      project: process.env.GCLOUD_PROJECT,
      location: VERTEX_LOCATION,
    });
  }
  return vertexAi;
}

/**
 * Single combined sanitize+grade LLM call (design doc: "Sanitize + grade: one combined
 * LLM call"). Runs on Vertex AI Gemini — same GCP project as Firestore/Cloud Functions,
 * so no separate API key/Secret Manager entry is needed, just the Vertex AI API enabled
 * and the function's service account granted `roles/aiplatform.user`.
 */
export async function gradeWithGemini(
  question: string,
  expectedAnswer: string,
  rawTranscript: string,
): Promise<GradeResult> {
  const model = getVertexAi().getGenerativeModel({
    model: GEMINI_MODEL,
    generationConfig: {
      responseMimeType: "application/json",
      responseSchema: RESPONSE_SCHEMA,
    },
  });

  const result = await model.generateContent(buildPrompt(question, expectedAnswer, rawTranscript));
  const text = result.response.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text) {
    throw new HttpError(502, "Grading LLM returned no content");
  }

  let parsed: GeminiGradeJson;
  try {
    parsed = JSON.parse(text) as GeminiGradeJson;
  } catch {
    throw new HttpError(502, "Grading LLM returned malformed JSON");
  }

  if (typeof parsed.grade !== "number" || !Number.isFinite(parsed.grade)) {
    throw new HttpError(502, "Grading LLM returned an invalid grade");
  }

  return {
    sanitizedTranscript: parsed.sanitized_transcript,
    gradePercent: Math.max(0, Math.min(100, Math.round(parsed.grade))),
    feedback: parsed.feedback,
  };
}
