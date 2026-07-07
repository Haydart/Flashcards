import { HttpError } from "./httpError";

const SCRIBE_ENDPOINT = "https://api.elevenlabs.io/v1/speech-to-text";
const SCRIBE_MODEL_ID = "scribe_v1";

interface ScribeResponse {
  text: string;
}

/**
 * Forwards the obfuscated WAV to ElevenLabs Scribe and returns the raw transcript.
 * The audio buffer is never written to disk and is discarded once this call returns
 * (design doc's "Data retention" section) — the caller must not persist `wavBuffer`.
 */
export async function transcribeWithElevenLabsScribe(
  wavBuffer: Buffer,
  apiKey: string,
): Promise<string> {
  const formData = new FormData();
  formData.append("model_id", SCRIBE_MODEL_ID);
  formData.append("file", new Blob([wavBuffer], { type: "audio/wav" }), "answer.wav");

  const response = await fetch(SCRIBE_ENDPOINT, {
    method: "POST",
    headers: { "xi-api-key": apiKey },
    body: formData,
  });

  if (!response.ok) {
    const body = await response.text();
    throw new HttpError(502, `ElevenLabs Scribe error (${response.status}): ${body}`);
  }

  const parsed = (await response.json()) as ScribeResponse;
  return parsed.text;
}
