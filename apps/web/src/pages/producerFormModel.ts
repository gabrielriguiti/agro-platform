import type { Producer, ProducerRequest } from "@agro/api-client";
import { fromDateTimeLocal, toDateTimeLocal } from "../lib/format";
import { validateTaxId } from "../lib/taxId";

/** Valores do formulário de produtor, como texto (campos de entrada). */
export interface ProducerFormValues {
  nome: string;
  inscricaoFiscal: string;
  endereco: string;
  /** Valor de `datetime-local` (aaaa-mm-ddThh:mm). */
  dataInicial: string;
  dataFinal: string;
  observacoes: string;
  /** Id do laboratório como texto ("" quando não escolhido). */
  laboratorioId: string;
  propriedadeIds: number[];
}

/** Campos validáveis do formulário. */
export type ProducerField = "nome" | "inscricaoFiscal" | "endereco" | "dataInicial" | "dataFinal" | "laboratorioId" | "propriedadeIds";

/** Mensagem de erro por campo. */
export type ProducerErrors = Partial<Record<ProducerField, string>>;

/** Todos os campos que têm mensagem junto ao controle. */
export const PRODUCER_FIELDS: ProducerField[] = ["nome", "inscricaoFiscal", "endereco", "dataInicial", "dataFinal", "laboratorioId", "propriedadeIds"];

/** Formulário em branco. */
export const emptyProducerForm: ProducerFormValues = {
  nome: "",
  inscricaoFiscal: "",
  endereco: "",
  dataInicial: "",
  dataFinal: "",
  observacoes: "",
  laboratorioId: "",
  propriedadeIds: [],
};

/**
 * Valida obrigatoriedade, dígitos verificadores da inscrição fiscal e período (final não anterior ao inicial).
 *
 * @param values valores atuais do formulário
 * @returns mensagens por campo; vazio quando válido
 */
export function validateProducer(values: ProducerFormValues): ProducerErrors {
  const errors: ProducerErrors = {};
  const nome = values.nome.trim();
  if (!nome) errors.nome = "Informe o nome.";
  else if (nome.length > 200) errors.nome = "Máximo de 200 caracteres.";

  const taxId = validateTaxId(values.inscricaoFiscal);
  if (taxId) errors.inscricaoFiscal = taxId;

  const endereco = values.endereco.trim();
  if (!endereco) errors.endereco = "Informe o endereço.";
  else if (endereco.length > 500) errors.endereco = "Máximo de 500 caracteres.";

  if (!values.dataInicial) errors.dataInicial = "Informe a data e hora inicial.";
  if (!values.dataFinal) errors.dataFinal = "Informe a data e hora final.";
  if (values.dataInicial && values.dataFinal && new Date(values.dataFinal) < new Date(values.dataInicial)) {
    errors.dataFinal = "A data final não pode ser anterior à inicial.";
  }

  if (!values.laboratorioId) errors.laboratorioId = "Selecione um laboratório.";
  return errors;
}

/** Valores iniciais do formulário a partir de um produtor existente (edição). */
export function toFormValues(producer: Producer): ProducerFormValues {
  return {
    nome: producer.nome ?? "",
    inscricaoFiscal: producer.inscricaoFiscal ?? "",
    endereco: producer.endereco ?? "",
    dataInicial: producer.dataInicial ? toDateTimeLocal(producer.dataInicial) : "",
    dataFinal: producer.dataFinal ? toDateTimeLocal(producer.dataFinal) : "",
    observacoes: producer.observacoes ?? "",
    laboratorioId: producer.laboratorio?.id !== undefined ? String(producer.laboratorio.id) : "",
    propriedadeIds: (producer.propriedades ?? []).flatMap((property) => (property.id !== undefined ? [property.id] : [])),
  };
}

/** Corpo da requisição a partir de valores já validados. */
export function toRequest(values: ProducerFormValues): ProducerRequest {
  return {
    nome: values.nome.trim(),
    inscricaoFiscal: values.inscricaoFiscal,
    endereco: values.endereco.trim(),
    dataInicial: fromDateTimeLocal(values.dataInicial),
    dataFinal: fromDateTimeLocal(values.dataFinal),
    observacoes: values.observacoes.trim() || null,
    laboratorioId: Number(values.laboratorioId),
    propriedadeIds: values.propriedadeIds,
  };
}

/**
 * Duração do período em dias, arredondada para cima (mínimo 1), ou `null` se o período for inválido.
 */
export function periodDays(values: Pick<ProducerFormValues, "dataInicial" | "dataFinal">): number | null {
  if (!values.dataInicial || !values.dataFinal) return null;
  const ms = new Date(values.dataFinal).getTime() - new Date(values.dataInicial).getTime();
  if (!Number.isFinite(ms) || ms < 0) return null;
  return Math.max(1, Math.ceil(ms / 86_400_000));
}
