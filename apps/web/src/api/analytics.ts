import { useQuery } from "@tanstack/react-query";
import { api, unwrap } from "./client";

/** Filtros da consulta analítica, já no formato da API (datas em ISO-8601 UTC). */
export interface AnalyticsParams {
  qtdMinimaProdutores: number;
  dataInicialInicio?: string;
  dataInicialFim?: string;
  dataFinalInicio?: string;
  dataFinalFim?: string;
  buscaTexto?: string;
}

/**
 * Consulta analítica de laboratórios; só executa quando há filtros enviados.
 *
 * @param params filtros enviados com "Consultar" (`null` antes da primeira consulta)
 */
export function useLabAnalytics(params: AnalyticsParams | null) {
  return useQuery({
    queryKey: ["analytics", params],
    queryFn: () => unwrap(api.GET("/api/laboratories/analytics", { params: { query: params as AnalyticsParams } })),
    enabled: params !== null,
  });
}
