import { endOfDayIso, startOfDayIso } from "../lib/format";
import type { AnalyticsParams } from "../api/analytics";

/** Valores dos filtros do dashboard, como texto (campos de entrada). */
export interface AnalyticsFilters {
  /** Intervalo da data inicial do produtor (aaaa-mm-dd). */
  dataInicialDe: string;
  dataInicialAte: string;
  /** Intervalo da data final do produtor (aaaa-mm-dd). */
  dataFinalDe: string;
  dataFinalAte: string;
  busca: string;
  /** Quantidade mínima de produtores, obrigatória. */
  qtdMinima: string;
}

/** Filtros padrão: sem datas nem busca e quantidade mínima 1. */
export const defaultFilters: AnalyticsFilters = { dataInicialDe: "", dataInicialAte: "", dataFinalDe: "", dataFinalAte: "", busca: "", qtdMinima: "1" };

/** Erros dos filtros, por campo. */
export interface AnalyticsFilterErrors {
  qtdMinima?: string;
  dataInicial?: string;
  dataFinal?: string;
}

/**
 * Valida os filtros: quantidade mínima obrigatória (inteiro ≥ 1) e intervalos de data não invertidos.
 *
 * @param filters valores atuais dos campos
 */
export function validateFilters(filters: AnalyticsFilters): AnalyticsFilterErrors {
  const errors: AnalyticsFilterErrors = {};
  const raw = filters.qtdMinima.trim();
  if (raw === "") errors.qtdMinima = "Informe a quantidade mínima de produtores.";
  else if (!/^\d+$/.test(raw) || Number(raw) < 1) errors.qtdMinima = "Use um número inteiro de 1 ou mais.";
  if (filters.dataInicialDe && filters.dataInicialAte && filters.dataInicialAte < filters.dataInicialDe) {
    errors.dataInicial = "O fim do intervalo não pode ser anterior ao início.";
  }
  if (filters.dataFinalDe && filters.dataFinalAte && filters.dataFinalAte < filters.dataFinalDe) {
    errors.dataFinal = "O fim do intervalo não pode ser anterior ao início.";
  }
  return errors;
}

/**
 * Converte filtros válidos nos parâmetros da API (início do dia e fim do dia em UTC).
 *
 * @param filters filtros já validados
 */
export function toParams(filters: AnalyticsFilters): AnalyticsParams {
  const params: AnalyticsParams = { qtdMinimaProdutores: Number(filters.qtdMinima.trim()) };
  if (filters.dataInicialDe) params.dataInicialInicio = startOfDayIso(filters.dataInicialDe);
  if (filters.dataInicialAte) params.dataInicialFim = endOfDayIso(filters.dataInicialAte);
  if (filters.dataFinalDe) params.dataFinalInicio = startOfDayIso(filters.dataFinalDe);
  if (filters.dataFinalAte) params.dataFinalFim = endOfDayIso(filters.dataFinalAte);
  if (filters.busca.trim()) params.buscaTexto = filters.busca.trim();
  return params;
}

/** Soma valores monetários sem acumular erro de ponto flutuante (centavos inteiros). */
export function sumCurrency(values: number[]): number {
  return values.reduce((total, value) => total + Math.round(value * 100), 0) / 100;
}
