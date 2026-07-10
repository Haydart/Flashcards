import * as admin from "firebase-admin";
import { onCall, onRequest, HttpsError, Request } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import type { Response } from "express";
import { requireAuthenticatedUid } from "./lib/auth";
import { requirePremiumEntitlement, isPremiumUser } from "./lib/entitlement";
import { parseMultipart } from "./lib/multipart";
import { transcribeWithElevenLabsScribe } from "./lib/elevenlabs";
import { sanitizeTranscript, gradeSanitizedTranscript } from "./lib/grading";
import { HttpError } from "./lib/httpError";

admin.initializeApp();

const elevenLabsApiKey = defineSecret("ELEVENLABS_API_KEY");

const RUNTIME_OPTIONS = {
  region: "us-central1",
  timeoutSeconds: 120,
  memory: "512MiB" as const,
};

/** Shared entry wrapper: only ever sends the {sanitized_transcript, grade, feedback} /
 * {transcript} / {is_premium} JSON contract the app already parses, plus a uniform error
 * shape for anything that throws HttpError. Never logs or persists audio bytes. */
function handle(fn: (req: Request, res: Response) => Promise<void>) {
  return async (req: Request, res: Response) => {
    try {
      await fn(req, res);
    } catch (error) {
      if (error instanceof HttpError) {
        res.status(error.statusCode).json({ error: error.message });
      } else {
        console.error(error);
        res.status(500).json({ error: "Internal error" });
      }
    }
  };
}

export const entitlement = onRequest(RUNTIME_OPTIONS, handle(async (req, res) => {
  const uid = await requireAuthenticatedUid(req);
  const isPremium = await isPremiumUser(uid);
  res.status(200).json({ is_premium: isPremium });
}));

export const transcribe = onRequest(
  { ...RUNTIME_OPTIONS, secrets: [elevenLabsApiKey] },
  handle(async (req, res) => {
    const uid = await requireAuthenticatedUid(req);
    await requirePremiumEntitlement(uid);

    const { audio } = await parseMultipart(req);
    if (!audio) throw new HttpError(400, "Missing audio part");

    const transcript = await transcribeWithElevenLabsScribe(audio, elevenLabsApiKey.value());
    res.status(200).json({ transcript });
  }),
);

export const sanitizeAndGrade = onRequest(RUNTIME_OPTIONS, handle(async (req, res) => {
  const uid = await requireAuthenticatedUid(req);
  await requirePremiumEntitlement(uid);

  const { question, expected_answer: expectedAnswer, transcript } = req.body ?? {};
  if (typeof question !== "string" || typeof expectedAnswer !== "string" || typeof transcript !== "string") {
    throw new HttpError(400, "question, expected_answer, and transcript must be non-empty strings");
  }
  if (!question || !expectedAnswer || !transcript) {
    throw new HttpError(400, "Missing question/expected_answer/transcript");
  }

  const sanitizedTranscript = await sanitizeTranscript(transcript);
  const grade = await gradeSanitizedTranscript(question, expectedAnswer, sanitizedTranscript);
  res.status(200).json({
    sanitized_transcript: sanitizedTranscript,
    grade: grade.gradePercent,
    feedback: grade.feedback,
  });
}));

interface GradeVoiceAnswerRequest {
  card_id?: string;
  question?: string;
  expected_answer?: string;
  audio_base64?: string;
}

interface GradeVoiceAnswerChunk {
  sanitized_transcript: string;
}

interface GradeVoiceAnswerResult {
  grade: number;
  feedback: string;
}

/**
 * Streaming callable (ADR-0028): one client request, two ordered results over the same
 * connection — `response.sendChunk({ sanitized_transcript })` as soon as STT + sanitize finish,
 * then `{ grade, feedback }` as the function's normal return value once grading finishes. Must
 * be `onCall`, not `onRequest` — that's what gives streaming (`request.acceptsStreaming`,
 * `response.sendChunk`) and automatic Firebase Auth ID-token verification (`request.auth`).
 * A non-streaming caller still gets the full `{ sanitized_transcript, grade, feedback }`-shaped
 * data buffered into the single final result (Firebase's own streaming-callable backward
 * compatibility) — sendChunk silently no-ops when `request.acceptsStreaming` is false.
 */
export const gradeVoiceAnswer = onCall<GradeVoiceAnswerRequest, Promise<GradeVoiceAnswerResult>, GradeVoiceAnswerChunk>(
  { ...RUNTIME_OPTIONS, secrets: [elevenLabsApiKey] },
  async (request, response) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Missing Firebase ID token");
    if (!(await isPremiumUser(uid))) {
      throw new HttpsError("permission-denied", "No active premium entitlement");
    }

    const { question, expected_answer: expectedAnswer, audio_base64: audioBase64 } = request.data;
    if (!question || !expectedAnswer || !audioBase64) {
      throw new HttpsError("invalid-argument", "Missing question/expected_answer/audio_base64");
    }

    const wavBuffer = Buffer.from(audioBase64, "base64");
    const rawTranscript = await transcribeWithElevenLabsScribe(wavBuffer, elevenLabsApiKey.value());
    const sanitizedTranscript = await sanitizeTranscript(rawTranscript);
    await response?.sendChunk({ sanitized_transcript: sanitizedTranscript });

    const grade = await gradeSanitizedTranscript(question, expectedAnswer, sanitizedTranscript);
    return { grade: grade.gradePercent, feedback: grade.feedback };
  },
);
