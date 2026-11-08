import type { BatchRequest } from "@agro/api-client";

/** Estado de um lote, como devolvido pela API. */
export type BatchState = "EM_ANDAMENTO" | "CONCLUIDO" | "CONCLUIDO_COM_FALHAS";

/** Rótulo do estado do lote na interface. */
export function batchStateLabel(state: string | undefined): string {
  switch (state) {
    case "EM_ANDAMENTO":
      return "em andamento";
    case "CONCLUIDO":
      return "concluído";
    case "CONCLUIDO_COM_FALHAS":
      return "concluído com falhas";
    default:
      return state ?? "—";
  }
}

/** Rótulo do tipo de mensagem. */
export function messageTypeLabel(type: string | undefined): string {
  switch (type) {
    case "PRODUCER":
      return "PRODUTOR";
    case "PROPERTY":
      return "PROPRIEDADE";
    case "LABORATORY":
      return "LABORATÓRIO";
    default:
      return type ?? "—";
  }
}

/**
 * Exibe o payload com indentação quando for JSON; devolve o texto original caso contrário.
 *
 * @param payload conteúdo recebido da API
 */
export function prettyPayload(payload: string | null | undefined): string {
  if (!payload) return "";
  try {
    return JSON.stringify(JSON.parse(payload), null, 2);
  } catch {
    return payload;
  }
}

/**
 * Lote como texto livre do editor. O contrato descreve `Item.data` como objeto sem propriedades, então o
 * exemplo usa uma estrutura própria e só vira `BatchRequest` depois de lido do JSON digitado.
 */
export interface SampleBatch {
  source: string;
  mergeStrategy: string;
  items: Array<{ type: string; externalId: string; data: Record<string, unknown> }>;
}

/**
 * Lote de exemplo: itens válidos e um produtor com inscrição fiscal inválida, que termina na DLQ.
 *
 * A API é idempotente por (origem, tipo, id externo, payload): reenviar os mesmos ids vira "duplicado" e o lote
 * fica vazio. Por isso os ids externos levam um sufixo, e cada "Carregar exemplo" gera um lote novo.
 *
 * @param suffix texto que torna os ids externos únicos (ex.: derivado do horário)
 */
export function buildSampleBatch(suffix: string): SampleBatch {
  const lab = { nome: "Laboratório Demo Solo Vivo", inscricaoFiscal: "11.222.333/0001-81", endereco: "Av. das Análises, 100, Cuiabá/MT" };
  const property = { nome: "Fazenda Demo Santa Aurora", inscricaoFiscal: "11.444.777/0001-61", endereco: "Rodovia BR-163, km 12, Sorriso/MT" };
  const labId = `LAB-${suffix}`;
  const propertyId = `PROP-${suffix}`;
  return {
    source: "erp-demo",
    mergeStrategy: "MERGE",
    items: [
      { type: "LABORATORY", externalId: labId, data: lab },
      { type: "PROPERTY", externalId: propertyId, data: property },
      {
        type: "PRODUCER",
        externalId: `PROD-${suffix}`,
        data: {
          nome: "Maria Demo da Silva",
          inscricaoFiscal: "529.982.247-25",
          endereco: "Linha 5, lote 12, Lucas do Rio Verde/MT",
          dataInicial: "2026-01-01T00:00:00Z",
          dataFinal: "2026-12-31T23:59:59Z",
          laboratorio: { id: labId, ...lab },
          propriedades: [{ id: propertyId, ...property }],
        },
      },
      {
        type: "PRODUCER",
        externalId: `PROD-INVALIDO-${suffix}`,
        data: {
          nome: "João Demo com CPF inválido",
          inscricaoFiscal: "111.222.333-44",
          endereco: "Estrada Rural, s/n, Sinop/MT",
          dataInicial: "2026-01-01T00:00:00Z",
          dataFinal: "2026-12-31T23:59:59Z",
          laboratorio: { id: labId, ...lab },
          propriedades: [],
        },
      },
    ],
  };
}

/**
 * Texto do lote de exemplo para o editor, com ids únicos a cada chamada.
 *
 * @param now instante usado no sufixo (injetável para testes)
 */
export function sampleBatchJson(now: Date = new Date()): string {
  return JSON.stringify(buildSampleBatch(now.getTime().toString(36).toUpperCase()), null, 2);
}

/** Resultado da leitura do JSON digitado: o lote, ou a mensagem de erro. */
export type ParsedBatch = { ok: true; batch: BatchRequest } | { ok: false; error: string };

/**
 * Lê e valida o JSON do lote (envelope com `source`, `mergeStrategy` e `items`).
 *
 * @param text conteúdo do editor
 */
export function parseBatch(text: string): ParsedBatch {
  if (text.trim() === "") return { ok: false, error: "Cole ou carregue um JSON de lote." };
  let value: unknown;
  try {
    value = JSON.parse(text);
  } catch (error) {
    return { ok: false, error: `JSON inválido: ${error instanceof Error ? error.message : "erro de sintaxe"}.` };
  }
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    return { ok: false, error: "O lote deve ser um objeto JSON com source, mergeStrategy e items." };
  }
  const record = value as Record<string, unknown>;
  if (typeof record.source !== "string" || record.source.trim() === "") return { ok: false, error: 'Informe "source" (sistema de origem).' };
  if (typeof record.mergeStrategy !== "string") return { ok: false, error: 'Informe "mergeStrategy" (MERGE, OVERWRITE ou KEEP_EXISTING).' };
  if (!Array.isArray(record.items)) return { ok: false, error: 'Informe "items" como uma lista.' };
  return { ok: true, batch: value as BatchRequest };
}
