import type { Request } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { HttpError } from "./httpError";

const BEARER_PREFIX = "Bearer ";

/**
 * Verifies the Firebase ID token the app attaches via `FirebaseAuthTokenInterceptor`.
 * Throws 401 if missing/invalid — every endpoint calls this before touching
 * entitlement, ElevenLabs, or the grading LLM.
 */
export async function requireAuthenticatedUid(req: Request): Promise<string> {
  const header = req.header("Authorization") ?? "";
  if (!header.startsWith(BEARER_PREFIX)) {
    throw new HttpError(401, "Missing bearer token");
  }
  const idToken = header.slice(BEARER_PREFIX.length);
  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    return decoded.uid;
  } catch (error) {
    console.error("ID token verification failed:", error);
    throw new HttpError(401, "Invalid or expired authentication token");
  }
}
