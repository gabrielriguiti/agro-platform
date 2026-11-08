import type { BatchRequest, MessageStatus } from "@agro/api-client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, unwrap } from "./client";

/** Intervalo da atualização automática do monitor, em milissegundos. */
export const POLL_MS = 5000;

/** Chaves do cache de React Query para o monitor de integração. */
export const integrationKeys = {
  all: ["integration"] as const,
  batches: ["integration", "batches"] as const,
  batch: (id: number) => ["integration", "batch", id] as const,
  messages: (batchId: number, status: MessageStatus | undefined, page: number, size: number) => ["integration", "messages", batchId, status ?? "ALL", page, size] as const,
  message: (id: number) => ["integration", "message", id] as const,
};

/** Lotes mais recentes, atualizados sozinhos a cada 5 s. */
export function useBatches() {
  return useQuery({
    queryKey: integrationKeys.batches,
    queryFn: () => unwrap(api.GET("/api/integration/batches", { params: { query: { limit: 20 } } })),
    refetchInterval: POLL_MS,
  });
}

/** Um lote com contagens; atualiza a cada 5 s enquanto estiver em andamento. */
export function useBatch(id: number) {
  return useQuery({
    queryKey: integrationKeys.batch(id),
    queryFn: () => unwrap(api.GET("/api/integration/batches/{id}", { params: { path: { id } } })),
    refetchInterval: (query) => (query.state.data?.state === "EM_ANDAMENTO" ? POLL_MS : false),
  });
}

/**
 * Mensagens de um lote, com filtro por status e paginação.
 *
 * @param live atualizar a cada 5 s (lote em andamento)
 */
export function useBatchMessages(batchId: number, status: MessageStatus | undefined, page: number, size: number, live: boolean) {
  return useQuery({
    queryKey: integrationKeys.messages(batchId, status, page, size),
    queryFn: () => unwrap(api.GET("/api/integration/messages", { params: { query: { batchId, page, size, ...(status ? { status } : {}) } } })),
    refetchInterval: live ? POLL_MS : false,
  });
}

/** Detalhe de uma mensagem, com o payload original. */
export function useMessage(id: number | null) {
  return useQuery({
    queryKey: integrationKeys.message(id ?? 0),
    queryFn: () => unwrap(api.GET("/api/integration/messages/{id}", { params: { path: { id: id as number } } })),
    enabled: id !== null,
  });
}

/** Reprocessa uma mensagem morta e atualiza lotes, mensagens e detalhe. */
export function useReprocessMessage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => unwrap(api.POST("/api/integration/messages/{id}/reprocess", { params: { path: { id } } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: integrationKeys.all }),
  });
}

/** Reprocessa as mensagens mortas de um lote. */
export function useReprocessBatch() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => unwrap(api.POST("/api/integration/batches/{id}/reprocess", { params: { path: { id } } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: integrationKeys.all }),
  });
}

/** Envia um lote de integração (resposta 202: o processamento é assíncrono). */
export function useSendBatch() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: BatchRequest) => unwrap(api.POST("/api/integration/batches", { body })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: integrationKeys.all }),
  });
}
