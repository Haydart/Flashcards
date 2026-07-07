import * as admin from "firebase-admin";
import { onRequest, Request } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import type { Response } from "express";
import { requireAuthenticatedUid } from "./lib/auth";
import { requirePremiumEntitlement, isPremiumUser } from "./lib/entitlement";
import { parseMultipart } from "./lib/multipart";
import { transcribeWithElevenLabsScribe } from "./lib/elevenlabs";
import { gradeWithGemini } from "./lib/grading";
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
  if (!question || !expectedAnswer || !transcript) {
    throw new HttpError(400, "Missing question/expected_answer/transcript");
  }

  const grade = await gradeWithGemini(question, expectedAnswer, transcript);
  res.status(200).json({
    sanitized_transcript: grade.sanitizedTranscript,
    grade: grade.gradePercent,
    feedback: grade.feedback,
  });
}));

export const gradeVoiceAnswer = onRequest(
  { ...RUNTIME_OPTIONS, secrets: [elevenLabsApiKey] },
  handle(async (req, res) => {
    const uid = await requireAuthenticatedUid(req);
    await requirePremiumEntitlement(uid);

    const { fields, audio } = await parseMultipart(req);
    if (!audio) throw new HttpError(400, "Missing audio part");
    const { question, expected_answer: expectedAnswer } = fields;
    if (!question || !expectedAnswer) {
      throw new HttpError(400, "Missing question/expected_answer part");
    }

    const rawTranscript = await transcribeWithElevenLabsScribe(audio, elevenLabsApiKey.value());
    const grade = await gradeWithGemini(question, expectedAnswer, rawTranscript);
    res.status(200).json({
      sanitized_transcript: grade.sanitizedTranscript,
      grade: grade.gradePercent,
      feedback: grade.feedback,
    });
  }),
);
