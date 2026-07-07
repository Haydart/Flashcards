import type { Request } from "firebase-functions/v2/https";
import Busboy from "busboy";
import { Readable } from "node:stream";
import { HttpError } from "./httpError";

export interface ParsedMultipart {
  fields: Record<string, string>;
  audio: Buffer | null;
}

const AUDIO_FIELD_NAME = "audio";

/**
 * Parses the multipart bodies `gradeVoiceAnswer`/`transcribe` send (see
 * `RetrofitVoiceGradingApi.toAudioPart()` / `VoiceGradingRetrofitService` on the client).
 * Cloud Functions v2 exposes the unparsed body as `req.rawBody`; Busboy needs a stream,
 * so it's replayed from that buffer rather than reading `req` directly.
 */
export function parseMultipart(req: Request): Promise<ParsedMultipart> {
  return new Promise((resolve, reject) => {
    const contentType = req.header("content-type");
    if (!contentType) {
      reject(new HttpError(400, "Missing Content-Type header"));
      return;
    }

    const busboy = Busboy({ headers: { "content-type": contentType } });
    const fields: Record<string, string> = {};
    const audioChunks: Buffer[] = [];
    let audio: Buffer | null = null;

    busboy.on("field", (name, value) => {
      fields[name] = value;
    });

    busboy.on("file", (name, file) => {
      if (name !== AUDIO_FIELD_NAME) {
        file.resume();
        return;
      }
      file.on("data", (chunk: Buffer) => audioChunks.push(chunk));
      file.on("end", () => {
        audio = Buffer.concat(audioChunks);
      });
    });

    busboy.on("error", (error) => reject(error));
    busboy.on("finish", () => resolve({ fields, audio }));

    Readable.from(req.rawBody).pipe(busboy);
  });
}
