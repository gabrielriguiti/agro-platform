import type { Laboratory, Producer, Property } from "@agro/api-client";

/** Laboratórios de exemplo. */
export const labs: Laboratory[] = [
  { id: 1, nome: "LabSolo Cerrado", inscricaoFiscal: "11.222.333/0001-81", endereco: "Rio Verde – GO" },
  { id: 2, nome: "Laboratório Terra Viva", inscricaoFiscal: "07.618.233/0001-05", endereco: "Uberlândia – MG" },
];

/** Propriedades de exemplo (a 2 pertence ao produtor 7). */
export const properties: Property[] = [
  { id: 10, nome: "Fazenda Santa Rita", inscricaoFiscal: "12.345.678/0001-95", endereco: "Rio Verde – GO", produtorId: null },
  { id: 11, nome: "Fazenda Boa Esperança", inscricaoFiscal: "98.765.432/0001-98", endereco: "Montividiu – GO", produtorId: 7 },
  { id: 12, nome: "Sítio São José", inscricaoFiscal: "529.982.247-25", endereco: "Uberlândia – MG", produtorId: null },
];

/**
 * Produtor de exemplo.
 *
 * @param id identificador
 * @param nome nome do produtor
 */
export function producer(id: number, nome: string, overrides: Partial<Producer> = {}): Producer {
  return {
    id,
    nome,
    inscricaoFiscal: "529.982.247-25",
    endereco: "Rodovia GO-174, km 12",
    dataInicial: new Date(2026, 2, 1, 8, 0).toISOString(),
    dataFinal: new Date(2026, 8, 30, 18, 0).toISOString(),
    observacoes: null,
    laboratorio: labs[0],
    propriedades: [{ id: 11, nome: "Fazenda Boa Esperança" }],
    ...overrides,
  };
}

/** Página de produtores no formato da API. */
export function producerPage(content: Producer[], page = 0, size = 10, totalElements = content.length) {
  return { content, page, size, totalElements, totalPages: Math.max(1, Math.ceil(totalElements / size)) };
}
