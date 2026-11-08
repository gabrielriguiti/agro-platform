import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { errorMessage } from "../../api/errors";
import { useProducer } from "../../api/registry";
import { Badge, Button, Dash, Modal, Pagination, StateMessage, Table, TableSkeleton, useToast } from "../../components";
import type { RegistryConfig } from "./registryConfig";

/**
 * Listagem de laboratórios ou propriedades, com editar e excluir (com confirmação).
 * Propriedades mostram a situação (disponível ou vinculada a produtor).
 */
export function RegistryListPage({ config }: { config: RegistryConfig }) {
  const navigate = useNavigate();
  const { notify } = useToast();
  const query = config.useList();
  const remove = config.useDelete();
  const [toDelete, setToDelete] = useState<{ id: number; nome: string; produtorId?: number | null } | null>(null);
  const [page, setPage] = useState(0);
  const withStatus = config.kind === "property";
  const columns = ["Nome", config.taxIdLabel, "Endereço", ...(withStatus ? ["Situação"] : []), "Ações"];
  const items = query.data ?? [];
  // A API devolve a lista inteira; quando a tela pagina, o corte é feito aqui.
  const size = config.pageSize;
  const lastPage = size > 0 ? Math.max(0, Math.ceil(items.length / size) - 1) : 0;
  const current = Math.min(page, lastPage);
  const visible = size > 0 ? items.slice(current * size, (current + 1) * size) : items;

  const confirmDelete = () => {
    if (!toDelete) return;
    const target = toDelete;
    remove.mutate(target.id, {
      onSuccess: () => {
        setToDelete(null);
        notify({ title: config.deletedTitle, message: `${target.nome} foi removido.` });
      },
      onError: (error) => {
        setToDelete(null);
        notify({ tone: "error", title: config.deleteFailTitle, message: errorMessage(error) });
      },
    });
  };

  return (
    <>
      <div className="page-head">
        <div>
          <h1 className="page-title">{config.listTitle}</h1>
          {query.data ? <p className="page-sub">{config.count(items.length)}</p> : <p className="page-sub">{config.listSubtitle}</p>}
        </div>
        <Link to={`${config.base}/novo`} className="btn btn--primary">
          {config.newLabel}
        </Link>
      </div>

      <div className="card">
        {query.isPending ? (
          <TableSkeleton columns={columns} label={`Carregando ${config.listTitle.toLowerCase()}`} />
        ) : query.isError ? (
          <StateMessage kind="error" title={config.loadError} actionLabel="Tentar de novo" onAction={() => void query.refetch()}>
            {errorMessage(query.error)}
          </StateMessage>
        ) : items.length === 0 ? (
          <StateMessage kind="empty" title={config.emptyTitle} actionLabel={config.newLabel} onAction={() => navigate(`${config.base}/novo`)}>
            {config.emptyText}
          </StateMessage>
        ) : (
          <Table aria-label={config.listTitle}>
            <thead>
              <tr>
                <th>Nome</th>
                <th>{config.taxIdLabel}</th>
                <th>Endereço</th>
                {withStatus ? <th>Situação</th> : null}
                <th>
                  <span className="sr-only">Ações</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {visible.map((item) => (
                <tr key={item.id}>
                  <td>{item.nome}</td>
                  <td className="table__id">{item.inscricaoFiscal}</td>
                  <td>{item.endereco || <Dash />}</td>
                  {withStatus ? (
                    <td>{item.produtorId != null ? <Badge tone="info">vinculada</Badge> : <Badge tone="ok">disponível</Badge>}</td>
                  ) : null}
                  <td>
                    <div className="actions">
                      <Button variant="ghost" size="sm" aria-label={`Editar ${item.nome}`} onClick={() => navigate(`${config.base}/${item.id}/editar`)}>
                        Editar
                      </Button>
                      <Button variant="ghost-danger" size="sm" aria-label={`Excluir ${item.nome}`} onClick={() => setToDelete({ id: item.id as number, nome: item.nome ?? "", produtorId: item.produtorId })}>
                        Excluir
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
        {size > 0 && items.length > 0 ? <Pagination page={current} size={size} totalElements={items.length} totalPages={lastPage + 1} onPageChange={setPage} /> : null}
      </div>

      <Modal
        open={toDelete !== null}
        title={config.deleteTitle}
        description={config.deleteText(toDelete?.nome ?? "")}
        onClose={() => (remove.isPending ? undefined : setToDelete(null))}
        actions={
          <>
            <Button onClick={() => setToDelete(null)} disabled={remove.isPending}>
              Cancelar
            </Button>
            <Button variant="danger" loading={remove.isPending} onClick={confirmDelete}>
              {remove.isPending ? "Excluindo…" : "Excluir"}
            </Button>
          </>
        }
      >
        {config.kind === "property" && toDelete ? <LinkedProducer producerId={toDelete.produtorId} /> : null}
      </Modal>
    </>
  );
}

/** Situação da propriedade e o produtor vinculado, mostrados na confirmação de exclusão. */
function LinkedProducer({ producerId }: { producerId?: number | null }) {
  const linked = producerId != null;
  const producer = useProducer(linked ? producerId : undefined);
  return (
    <dl className="summary" style={{ margin: 0 }}>
      <dt>Situação</dt>
      <dd>{linked ? <Badge tone="info">vinculada</Badge> : <Badge tone="ok">disponível</Badge>}</dd>
      {linked ? (
        <>
          <dt>Produtor vinculado</dt>
          <dd>{producer.data?.nome ?? (producer.isError ? `Produtor #${producerId}` : "Carregando…")}</dd>
        </>
      ) : null}
    </dl>
  );
}
