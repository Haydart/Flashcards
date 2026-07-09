/** Thrown by pipeline steps to short-circuit a request with a specific HTTP status. */
export class HttpError extends Error {
  constructor(
    public readonly statusCode: number,
    message: string,
  ) {
    super(message);
    this.name = "HttpError";
  }
}
