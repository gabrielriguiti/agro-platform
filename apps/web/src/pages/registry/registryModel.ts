import { onlyDigits, validateCnpj, validateTaxId } from "../../lib/taxId";

/** Tipo de cadastro tratado pelas telas genéricas. */
export type RegistryKind = "laboratory" | "property";

/** Valores do formulário de laboratório ou propriedade, como texto. */
export interface RegistryFormValues {
  nome: string;
  inscricaoFiscal: string;
  endereco: string;
}

/** Campos do formulário. */
export type RegistryField = keyof RegistryFormValues;

/** Mensagem de erro por campo. */
export type RegistryErrors = Partial<Record<RegistryField, string>>;

/** Campos que têm mensagem junto ao controle. */
export const REGISTRY_FIELDS: RegistryField[] = ["nome", "inscricaoFiscal", "endereco"];

/** Formulário em branco. */
export const emptyRegistryForm: RegistryFormValues = { nome: "", inscricaoFiscal: "", endereco: "" };

/** Rótulos dos campos nas mensagens de resumo. */
export function fieldLabels(kind: RegistryKind): Record<RegistryField, string> {
  return { nome: "Nome", inscricaoFiscal: kind === "laboratory" ? "CNPJ" : "Inscrição fiscal", endereco: "Endereço" };
}

/**
 * Valida nome, inscrição fiscal e endereço. Laboratório exige CNPJ; propriedade aceita CPF ou CNPJ.
 *
 * @param kind tipo de cadastro
 * @param values valores atuais do formulário
 * @returns mensagens por campo; vazio quando válido
 */
export function validateRegistry(kind: RegistryKind, values: RegistryFormValues): RegistryErrors {
  const errors: RegistryErrors = {};
  const nome = values.nome.trim();
  if (!nome) errors.nome = kind === "laboratory" ? "Informe o nome do laboratório." : "Informe o nome da propriedade.";
  else if (nome.length > 200) errors.nome = "Máximo de 200 caracteres.";

  if (onlyDigits(values.inscricaoFiscal) === "") {
    errors.inscricaoFiscal = kind === "laboratory" ? "Informe o CNPJ." : "Informe o CPF ou CNPJ.";
  } else {
    const taxId = kind === "laboratory" ? validateCnpj(values.inscricaoFiscal) : validateTaxId(values.inscricaoFiscal);
    if (taxId) errors.inscricaoFiscal = taxId;
  }

  const endereco = values.endereco.trim();
  if (!endereco) errors.endereco = "Informe o endereço.";
  else if (endereco.length > 500) errors.endereco = "Máximo de 500 caracteres.";
  return errors;
}

/**
 * Resumo para o topo do formulário quando há erros: título "Corrija N campo(s) para salvar" e a lista deles.
 *
 * @param kind tipo de cadastro
 * @param errors erros atuais
 */
export function errorSummary(kind: RegistryKind, errors: RegistryErrors): { title: string; message: string } | null {
  const fields = REGISTRY_FIELDS.filter((field) => errors[field]);
  if (fields.length === 0) return null;
  const labels = fieldLabels(kind);
  const names = fields.map((field) => labels[field]);
  const list = names.length === 1 ? names[0] : `${names.slice(0, -1).join(", ")} e ${names[names.length - 1]}`;
  const allEmpty = fields.every((field) => /^Informe /.test(errors[field] ?? ""));
  return {
    title: fields.length === 1 ? "Corrija 1 campo para salvar" : `Corrija ${fields.length} campos para salvar`,
    message: allEmpty ? `${list} ${fields.length === 1 ? "é obrigatório" : "são obrigatórios"}.` : `Revise: ${list}.`,
  };
}
