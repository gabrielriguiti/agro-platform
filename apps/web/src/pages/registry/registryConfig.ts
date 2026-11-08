import type { IdentityRequest } from "@agro/api-client";
import { useAllProperties, useDeleteLaboratory, useDeleteProperty, useLaboratories, useLaboratory, useProperty, useSaveLaboratory, useSaveProperty } from "../../api/registry";
import type { RegistryKind } from "./registryModel";

/** Item de lista comum a laboratório e propriedade (propriedade traz também `produtorId`). */
export interface RegistryItem {
  id?: number;
  nome?: string;
  inscricaoFiscal?: string;
  endereco?: string;
  produtorId?: number | null;
}

/** Consulta de lista, como usada pelas telas. */
export interface ListQuery {
  data?: RegistryItem[];
  isPending: boolean;
  isError: boolean;
  error: unknown;
  refetch: () => unknown;
}

/** Textos, rotas e acesso aos dados de cada tipo de cadastro. */
export interface RegistryConfig {
  kind: RegistryKind;
  /** Rota-base (`/laboratorios` ou `/propriedades`). */
  base: string;
  listTitle: string;
  listSubtitle: string;
  newLabel: string;
  editTitle: string;
  breadcrumbNew: string;
  emptyTitle: string;
  emptyText: string;
  loadError: string;
  loadOneError: string;
  /** Título da seção do formulário ("Dados do laboratório"). */
  sectionTitle: string;
  namePlaceholder: string;
  /** Itens por página na lista (0 = sem paginação). */
  pageSize: number;
  taxIdLabel: string;
  taxIdHelp: string;
  taxIdPlaceholder: string;
  savedTitle: string;
  savedMessage: (name: string) => string;
  updatedTitle: string;
  deleteTitle: string;
  deleteText: (name: string) => string;
  deletedTitle: string;
  deleteFailTitle: string;
  count: (n: number) => string;
  useList: () => ListQuery;
  useOne: (id: number | undefined) => { data?: RegistryItem; isPending: boolean; isError: boolean; error: unknown; refetch: () => unknown };
  useSave: (id?: number) => { mutate: (body: IdentityRequest, options: { onSuccess: (saved: RegistryItem) => void; onError: (error: unknown) => void }) => void; isPending: boolean };
  useDelete: () => { mutate: (id: number, options: { onSuccess: () => void; onError: (error: unknown) => void }) => void; isPending: boolean };
}

/** Cadastro de laboratórios. */
export const laboratoryConfig: RegistryConfig = {
  kind: "laboratory",
  base: "/laboratorios",
  listTitle: "Laboratórios",
  listSubtitle: "Laboratórios que atendem os produtores. O CNPJ é obrigatório.",
  newLabel: "Novo laboratório",
  editTitle: "Editar laboratório",
  breadcrumbNew: "Novo laboratório",
  emptyTitle: "Nenhum laboratório cadastrado",
  emptyText: "Cadastre o primeiro laboratório para poder vinculá-lo aos produtores.",
  loadError: "Não foi possível carregar os laboratórios",
  loadOneError: "Não foi possível carregar o laboratório",
  sectionTitle: "Dados do laboratório",
  namePlaceholder: "Razão social ou nome do laboratório",
  pageSize: 0,
  taxIdLabel: "CNPJ",
  taxIdHelp: "Laboratório exige CNPJ. A máscara é aplicada ao digitar.",
  taxIdPlaceholder: "00.000.000/0000-00",
  savedTitle: "Laboratório salvo",
  savedMessage: (name) => `${name} foi cadastrado com sucesso.`,
  updatedTitle: "Alterações salvas",
  deleteTitle: "Excluir laboratório?",
  deleteText: (name) => `O laboratório ${name} será excluído. Só é possível excluir laboratórios sem produtores vinculados. Esta ação não pode ser desfeita.`,
  deletedTitle: "Laboratório excluído",
  deleteFailTitle: "Não foi possível excluir",
  count: (n) => (n === 1 ? "1 laboratório cadastrado" : `${n} laboratórios cadastrados`),
  useList: () => useLaboratories(),
  useOne: (id) => useLaboratory(id),
  useSave: (id) => useSaveLaboratory(id) as ReturnType<RegistryConfig["useSave"]>,
  useDelete: () => useDeleteLaboratory() as ReturnType<RegistryConfig["useDelete"]>,
};

/** Cadastro de propriedades. */
export const propertyConfig: RegistryConfig = {
  kind: "property",
  base: "/propriedades",
  listTitle: "Propriedades",
  listSubtitle: "Propriedades rurais. O vínculo com o produtor é feito no formulário do produtor.",
  newLabel: "Nova propriedade",
  editTitle: "Editar propriedade",
  breadcrumbNew: "Nova propriedade",
  emptyTitle: "Nenhuma propriedade cadastrada",
  emptyText: "Cadastre a primeira propriedade para poder vinculá-la a um produtor.",
  loadError: "Não foi possível carregar as propriedades",
  loadOneError: "Não foi possível carregar a propriedade",
  sectionTitle: "Dados da propriedade",
  namePlaceholder: "Nome da propriedade",
  pageSize: 10,
  taxIdLabel: "Inscrição fiscal",
  taxIdHelp: "CPF ou CNPJ. A máscara é aplicada ao digitar.",
  taxIdPlaceholder: "000.000.000-00 ou 00.000.000/0000-00",
  savedTitle: "Propriedade salva",
  savedMessage: (name) => `${name} foi cadastrada com sucesso. Ela já está disponível para vincular a um produtor.`,
  updatedTitle: "Alterações salvas",
  deleteTitle: "Excluir propriedade?",
  deleteText: (name) => `A propriedade ${name} será excluída. Se estiver vinculada a um produtor, ela é desvinculada e o produtor continua existindo. Esta ação não pode ser desfeita.`,
  deletedTitle: "Propriedade excluída",
  deleteFailTitle: "Não foi possível excluir",
  count: (n) => (n === 1 ? "1 propriedade cadastrada" : `${n} propriedades cadastradas`),
  useList: () => useAllProperties(),
  useOne: (id) => useProperty(id),
  useSave: (id) => useSaveProperty(id) as ReturnType<RegistryConfig["useSave"]>,
  useDelete: () => useDeleteProperty() as ReturnType<RegistryConfig["useDelete"]>,
};
