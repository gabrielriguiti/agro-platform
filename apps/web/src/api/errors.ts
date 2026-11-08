/** Campo inválido devolvido pela API em `errors` (ProblemDetail, RFC 9457). */
export interface FieldProblem {
  field: string;
  message: string;
}

/** Corpo `application/problem+json` da API. */
export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  errors?: FieldProblem[];
}

/**
 * Erro de uma chamada à API: resposta HTTP de erro (ProblemDetail) ou falha de rede (`status` 0).
 */
export class ApiError extends Error {
  /** Código HTTP; 0 quando o servidor não respondeu. */
  readonly status: number;
  /** Título do ProblemDetail, quando houver. */
  readonly title?: string;
  /** Campos inválidos informados pela API. */
  readonly fieldErrors: FieldProblem[];

  constructor(status: number, message: string, options: { title?: string; fieldErrors?: FieldProblem[] } = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.title = options.title;
    this.fieldErrors = options.fieldErrors ?? [];
  }

  /** @returns verdadeiro quando o servidor não respondeu (rede, tempo esgotado, CORS). */
  get isNetwork(): boolean {
    return this.status === 0;
  }
}

const NETWORK_MESSAGE = "A conexão com o servidor falhou. Verifique sua rede e tente de novo.";

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function parseFieldErrors(value: unknown): FieldProblem[] {
  if (!Array.isArray(value)) return [];
  return value.flatMap((item) =>
    isRecord(item) && typeof item.field === "string" && typeof item.message === "string"
      ? [{ field: item.field, message: item.message }]
      : [],
  );
}

/**
 * Converte o corpo de uma resposta de erro em {@link ApiError}.
 *
 * @param status código HTTP da resposta
 * @param body corpo já decodificado (objeto ProblemDetail ou texto)
 */
export function problemToError(status: number, body: unknown): ApiError {
  if (isRecord(body)) {
    const title = typeof body.title === "string" ? body.title : undefined;
    const detail = typeof body.detail === "string" ? body.detail : undefined;
    return new ApiError(status, detail ?? title ?? `Erro ${status}`, {
      title,
      fieldErrors: parseFieldErrors(body.errors),
    });
  }
  if (typeof body === "string" && body.trim() !== "") {
    return new ApiError(status, body.trim());
  }
  return new ApiError(status, status >= 500 ? "O servidor encontrou um erro. Tente de novo." : `Erro ${status}`);
}

/** Erro para falha de comunicação com o servidor. */
export function networkError(): ApiError {
  return new ApiError(0, NETWORK_MESSAGE);
}

/**
 * Mensagem para exibir ao usuário a partir de qualquer erro capturado.
 *
 * @param error valor capturado em um `catch` ou vindo do React Query
 */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error && error.message) return error.message;
  return "Algo deu errado. Tente de novo.";
}
