import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useDeleteProducer, useProducers } from "../api/registry";
import { errorMessage } from "../api/errors";
import { Button, Dash, Modal, Pagination, StateMessage, Table, TableSkeleton, useToast } from "../components";
import { formatPeriod } from "../lib/format";

/** Itens por página do grid. */
const PAGE_SIZE = 10;

const COLUMNS = ["Nome", "Inscrição fiscal", "Laboratório", "Nº de propriedades", "Período", "Ações"];

/** Grid operacional de produtores: listagem paginada, edição e exclusão com confirmação. */
export function ProducersPage() {
  const navigate = useNavigate();
  const { notify } = useToast();
  const [params, setParams] = useSearchParams();
  const requested = Number.parseInt(params.get("pagina") ?? "1", 10);
  const page = Number.isFinite(requested) && requested > 1 ? requested - 1 : 0;
  const query = useProducers(page, PAGE_SIZE);
  const remove = useDeleteProducer();
  const [toDelete, setToDelete] = useState<{ id: number; nome: string } | null>(null);

  const goToPage = (next: number) => setParams(next > 0 ? { pagina: String(next + 1) } : {});
  const data = query.data;

  const confirmDelete = () => {
    if (!toDelete) return;
    const target = toDelete;
    remove.mutate(target.id, {
      onSuccess: () => {
        setToDelete(null);
        notify({ title: "Produtor excluído", message: `${target.nome} foi removido.` });
        if ((data?.content?.length ?? 0) === 1 && page > 0) goToPage(page - 1);
      },
      onError: (error) => {
        setToDelete(null);
        notify({ tone: "error", title: "Falha ao excluir", message: errorMessage(error) });
      },
    });
  };

  return (
    <>
      <div className="page-head">
        <div>
          <h1 className="page-title">Produtores</h1>
          {data ? (
            <p className="page-sub">{data.totalElements === 1 ? "1 produtor cadastrado" : `${data.totalElements} produtores cadastrados`}</p>
          ) : null}
        </div>
        <Link to="/produtores/novo" className="btn btn--primary">
          Novo produtor
        </Link>
      </div>

      <div className="card">
        {query.isPending ? (
          <TableSkeleton columns={COLUMNS} label="Carregando produtores" />
        ) : query.isError ? (
          <StateMessage kind="error" title="Não foi possível carregar os produtores" actionLabel="Tentar de novo" onAction={() => void query.refetch()}>
            {errorMessage(query.error)}
          </StateMessage>
        ) : (query.data.totalElements ?? 0) === 0 ? (
          <StateMessage kind="empty" title="Nenhum produtor cadastrado" actionLabel="Novo produtor" onAction={() => navigate("/produtores/novo")}>
            Cadastre o primeiro produtor para vinculá-lo a um laboratório e às suas propriedades.
          </StateMessage>
        ) : (
          <>
            <Table aria-label="Produtores">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Inscrição fiscal</th>
                  <th>Laboratório</th>
                  <th className="num">Nº de propriedades</th>
                  <th>Período</th>
                  <th>
                    <span className="sr-only">Ações</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {(query.data.content ?? []).map((producer) => (
                  <tr key={producer.id}>
                    <td>{producer.nome}</td>
                    <td className="table__id">{producer.inscricaoFiscal}</td>
                    <td>{producer.laboratorio?.nome ?? <Dash />}</td>
                    <td className="num">{producer.propriedades?.length ?? 0}</td>
                    <td>{producer.dataInicial && producer.dataFinal ? formatPeriod(producer.dataInicial, producer.dataFinal) : <Dash />}</td>
                    <td>
                      <div className="actions">
                        <Button variant="ghost" size="sm" onClick={() => navigate(`/produtores/${producer.id}/editar`)} aria-label={`Editar ${producer.nome}`}>
                          Editar
                        </Button>
                        <Button variant="ghost-danger" size="sm" onClick={() => setToDelete({ id: producer.id as number, nome: producer.nome ?? "" })} aria-label={`Excluir ${producer.nome}`}>
                          Excluir
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
            <Pagination page={query.data.page ?? page} size={query.data.size ?? PAGE_SIZE} totalElements={query.data.totalElements ?? 0} totalPages={query.data.totalPages ?? 1} onPageChange={goToPage} />
          </>
        )}
      </div>

      <Modal
        open={toDelete !== null}
        title="Excluir produtor?"
        description={
          <>
            O cadastro de <b>{toDelete?.nome}</b> será excluído e as propriedades vinculadas ficarão sem produtor. Esta ação não pode ser desfeita.
          </>
        }
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
      />
    </>
  );
}
