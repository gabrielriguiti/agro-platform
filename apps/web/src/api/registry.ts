import type { IdentityRequest, ProducerRequest } from "@agro/api-client";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, unwrap, unwrapEmpty } from "./client";

/** Chaves do cache de React Query para o cadastro. */
export const registryKeys = {
  producers: ["producers"] as const,
  producerList: (page: number, size: number) => ["producers", "list", page, size] as const,
  producer: (id: number) => ["producers", "detail", id] as const,
  laboratories: ["laboratories"] as const,
  laboratory: (id: number) => ["laboratories", "detail", id] as const,
  properties: (producerId?: number) => ["properties", "available", producerId ?? null] as const,
  allProperties: ["properties", "all"] as const,
  property: (id: number) => ["properties", "detail", id] as const,
};

/**
 * Página de produtores.
 *
 * @param page página a partir de 0
 * @param size itens por página
 */
export function useProducers(page: number, size: number) {
  return useQuery({
    queryKey: registryKeys.producerList(page, size),
    queryFn: () => unwrap(api.GET("/api/producers", { params: { query: { page, size } } })),
    placeholderData: keepPreviousData,
  });
}

/** Um produtor, com laboratório e propriedades (para preencher o formulário de edição). */
export function useProducer(id: number | undefined) {
  return useQuery({
    queryKey: registryKeys.producer(id ?? 0),
    queryFn: () => unwrap(api.GET("/api/producers/{id}", { params: { path: { id: id as number } } })),
    enabled: id !== undefined,
  });
}

/** Laboratórios disponíveis para a escolha única do formulário. */
export function useLaboratories() {
  return useQuery({
    queryKey: registryKeys.laboratories,
    queryFn: () => unwrap(api.GET("/api/laboratories")),
  });
}

/**
 * Propriedades que podem ser vinculadas: as livres e, na edição, as do produtor.
 *
 * @param producerId produtor em edição (omitido na criação)
 */
export function useAvailableProperties(producerId?: number) {
  return useQuery({
    queryKey: registryKeys.properties(producerId),
    queryFn: () =>
      unwrap(api.GET("/api/properties", { params: { query: { disponiveis: true, ...(producerId !== undefined ? { produtorId: producerId } : {}) } } })),
  });
}

/** Exclui um produtor e atualiza as listas. */
export function useDeleteProducer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => unwrapEmpty(api.DELETE("/api/producers/{id}", { params: { path: { id } } })),
    onSuccess: () => Promise.all([queryClient.invalidateQueries({ queryKey: registryKeys.producers }), queryClient.invalidateQueries({ queryKey: ["properties"] })]),
  });
}

/**
 * Cria (sem `id`) ou atualiza (com `id`) um produtor e atualiza as listas.
 *
 * @param id produtor em edição, quando houver
 */
export function useSaveProducer(id?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: ProducerRequest) =>
      id === undefined ? unwrap(api.POST("/api/producers", { body })) : unwrap(api.PUT("/api/producers/{id}", { params: { path: { id } }, body })),
    onSuccess: () => Promise.all([queryClient.invalidateQueries({ queryKey: registryKeys.producers }), queryClient.invalidateQueries({ queryKey: ["properties"] })]),
  });
}

/** Todas as propriedades (livres e vinculadas), para a tela de cadastro de propriedades. */
export function useAllProperties() {
  return useQuery({
    queryKey: registryKeys.allProperties,
    queryFn: () => unwrap(api.GET("/api/properties")),
  });
}

/** Um laboratório (para preencher o formulário de edição). */
export function useLaboratory(id: number | undefined) {
  return useQuery({
    queryKey: registryKeys.laboratory(id ?? 0),
    queryFn: () => unwrap(api.GET("/api/laboratories/{id}", { params: { path: { id: id as number } } })),
    enabled: id !== undefined,
  });
}

/** Uma propriedade (para preencher o formulário de edição). */
export function useProperty(id: number | undefined) {
  return useQuery({
    queryKey: registryKeys.property(id ?? 0),
    queryFn: () => unwrap(api.GET("/api/properties/{id}", { params: { path: { id: id as number } } })),
    enabled: id !== undefined,
  });
}

/** Cria (sem `id`) ou atualiza (com `id`) um laboratório; atualiza laboratórios e produtores (que exibem o laboratório). */
export function useSaveLaboratory(id?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: IdentityRequest) =>
      id === undefined ? unwrap(api.POST("/api/laboratories", { body })) : unwrap(api.PUT("/api/laboratories/{id}", { params: { path: { id } }, body })),
    onSuccess: () => Promise.all([queryClient.invalidateQueries({ queryKey: registryKeys.laboratories }), queryClient.invalidateQueries({ queryKey: registryKeys.producers })]),
  });
}

/** Exclui um laboratório (409 se houver produtores vinculados). */
export function useDeleteLaboratory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => unwrapEmpty(api.DELETE("/api/laboratories/{id}", { params: { path: { id } } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: registryKeys.laboratories }),
  });
}

/** Cria (sem `id`) ou atualiza (com `id`) uma propriedade; atualiza propriedades e produtores. */
export function useSaveProperty(id?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: IdentityRequest) =>
      id === undefined ? unwrap(api.POST("/api/properties", { body })) : unwrap(api.PUT("/api/properties/{id}", { params: { path: { id } }, body })),
    onSuccess: () => Promise.all([queryClient.invalidateQueries({ queryKey: ["properties"] }), queryClient.invalidateQueries({ queryKey: registryKeys.producers })]),
  });
}

/** Exclui uma propriedade (desvincula do produtor, que continua existindo). */
export function useDeleteProperty() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => unwrapEmpty(api.DELETE("/api/properties/{id}", { params: { path: { id } } })),
    onSuccess: () => Promise.all([queryClient.invalidateQueries({ queryKey: ["properties"] }), queryClient.invalidateQueries({ queryKey: registryKeys.producers })]),
  });
}
