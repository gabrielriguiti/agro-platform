import { createApiClient } from "@agro/api-client";
import { networkError, problemToError } from "./errors";

/** Cliente tipado da API (mesmo host; em desenvolvimento o proxy do Vite repassa `/api`). */
export const api = createApiClient();

/** Resposta do cliente `openapi-fetch`, reduzida ao que o tratamento precisa. */
interface FetchResult<T> {
  data?: T;
  error?: unknown;
  response: Response;
}

/**
 * Resolve a chamada devolvendo o corpo de sucesso ou lançando {@link ApiError}.
 *
 * Respostas de erro viram `ApiError` com o ProblemDetail; falhas de rede viram `ApiError` com `status` 0.
 * Para respostas sem corpo (204) use {@link unwrapEmpty}.
 *
 * @param request chamada ao cliente tipado (ex.: `api.GET("/api/producers")`)
 */
export async function unwrap<T>(request: Promise<FetchResult<T>>): Promise<NonNullable<T>> {
  return (await settle(request)) as NonNullable<T>;
}

/**
 * Como {@link unwrap}, para chamadas sem corpo de resposta (ex.: exclusão com 204).
 *
 * @param request chamada ao cliente tipado
 */
export async function unwrapEmpty(request: Promise<FetchResult<unknown>>): Promise<void> {
  await settle(request);
}

async function settle<T>(request: Promise<FetchResult<T>>): Promise<T | undefined> {
  let result: FetchResult<T>;
  try {
    result = await request;
  } catch {
    throw networkError();
  }
  if (!result.response.ok) {
    throw problemToError(result.response.status, result.error);
  }
  return result.data;
}
