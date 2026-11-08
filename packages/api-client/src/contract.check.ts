/**
 * Verificação de contrato em tempo de compilação: se a API mudar algo de que o web depende (campo, tipo ou
 * resposta de sucesso), o `typecheck` deste pacote quebra depois de regenerar `schema.d.ts`. Não tem efeito em
 * tempo de execução e não faz parte do bundle.
 */
import type { Batch, IntegrationMessage, LabAnalytics, paths, Producer, ProducerRequest, Property } from "./index";

type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2 ? true : false;
function assertType<T extends true>(): T | undefined {
  return undefined;
}

type JsonOf<R> = R extends { content: { "*/*": infer B } } ? B : R extends { content: { "application/json": infer B } } ? B : never;

// Corpo de criação/atualização do produtor.
assertType<Equal<ProducerRequest["laboratorioId"], number>>();
assertType<Equal<ProducerRequest["propriedadeIds"], number[] | undefined>>();
assertType<Equal<ProducerRequest["observacoes"], string | null | undefined>>();

// Propriedade disponível: produtorId nulo.
assertType<Equal<Property["produtorId"], number | null | undefined>>();

// Resposta de sucesso das operações usadas pelo web (precisam estar declaradas no contrato).
assertType<Equal<JsonOf<paths["/api/producers/{id}"]["get"]["responses"][200]>, Producer>>();
assertType<Equal<JsonOf<paths["/api/producers/{id}"]["put"]["responses"][200]>, Producer>>();
assertType<Equal<JsonOf<paths["/api/laboratories/analytics"]["get"]["responses"][200]>, LabAnalytics[]>>();
assertType<Equal<JsonOf<paths["/api/integration/batches/{id}"]["get"]["responses"][200]>, Batch>>();
assertType<Equal<JsonOf<paths["/api/integration/messages/{id}"]["get"]["responses"][200]>, IntegrationMessage>>();
assertType<Equal<JsonOf<paths["/api/integration/messages/{id}/reprocess"]["post"]["responses"][200]>, IntegrationMessage>>();
