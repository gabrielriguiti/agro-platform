import createClient from "openapi-fetch";
import type { components, paths } from "./schema";

export type { components, paths };

type Schemas = components["schemas"];

/** Resposta de `GET /api/info`, derivada do contrato OpenAPI. */
export type InfoResponse = Schemas["InfoResponse"];

/** Produtor com laboratório e propriedades embutidos. */
export type Producer = Schemas["ProducerResponse"];
/** Corpo de criação e atualização de produtor. */
export type ProducerRequest = Schemas["ProducerRequest"];
/** Corpo de criação e atualização de laboratório e de propriedade (nome, inscrição fiscal e endereço). */
export type IdentityRequest = Schemas["IdentityRequest"];
/** Propriedade rural; `produtorId` é nulo quando está disponível. */
export type Property = Schemas["PropertyResponse"];
/** Laboratório. */
export type Laboratory = Schemas["LaboratoryResponse"];
/** Laboratório com total de produtores filtrados e valor financeiro consolidado. */
export type LabAnalytics = Schemas["LabAnalyticsResponse"];
/** Resumo de um lote de integração, com contagens por status e estado derivado. */
export type Batch = Schemas["BatchResponse"];
/** Mensagem da outbox; o `payload` só vem no detalhe. */
export type IntegrationMessage = Schemas["MessageResponse"];
/** Corpo do envio de um lote de integração. */
export type BatchRequest = Schemas["BatchRequest"];
/** Resultado da ingestão de um lote. */
export type BatchAccepted = Schemas["BatchAcceptedResponse"];
/** Resultado do reprocessamento de um lote. */
export type ReprocessResult = Schemas["ReprocessResponse"];
/** Status possíveis de uma mensagem. */
export type MessageStatus = NonNullable<paths["/api/integration/messages"]["get"]["parameters"]["query"]>["status"] &
  string;

/** Cliente HTTP tipado pelo contrato OpenAPI. */
export type ApiClient = ReturnType<typeof createApiClient>;

/**
 * Cria um cliente HTTP tipado pelo contrato OpenAPI da API.
 *
 * @param baseUrl URL base da API (vazia para usar o mesmo host, com proxy do Vite)
 */
export function createApiClient(baseUrl = "") {
  // `Request` exige URL absoluta fora do navegador (ex.: jsdom); no navegador `location.origin` é o mesmo host.
  const base = baseUrl || globalThis.location?.origin || "";
  // O fetch é resolvido a cada chamada (não na criação), para poder ser substituído nos testes.
  return createClient<paths>({ baseUrl: base, fetch: (request) => globalThis.fetch(request) });
}
