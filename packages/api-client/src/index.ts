import createClient from "openapi-fetch";
import type { components, paths } from "./schema";

export type { components, paths };

/** Resposta de `GET /api/info`, derivada do contrato OpenAPI. */
export type InfoResponse = components["schemas"]["InfoResponse"];

/**
 * Cria um cliente HTTP tipado pelo contrato OpenAPI da API.
 *
 * @param baseUrl URL base da API (vazia para usar o mesmo host, com proxy do Vite)
 */
export function createApiClient(baseUrl = "") {
  return createClient<paths>({ baseUrl });
}
