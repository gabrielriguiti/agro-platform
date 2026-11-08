import { Link, useNavigate } from "react-router-dom";
import { errorMessage } from "../../api/errors";
import { useBatches } from "../../api/integration";
import { StateMessage, Table, TableSkeleton } from "../../components";
import { formatDateTime, formatTime } from "../../lib/format";
import { BatchStateBadge } from "./BatchStateBadge";

const COLUMNS = ["Lote", "Origem", "Recebido em", "Estado", "PENDING", "PROCESSING", "DONE", "DEAD", "Progresso"];

/** Lista de lotes recebidos, atualizada sozinha a cada 5 s. */
export function BatchList() {
  const navigate = useNavigate();
  const query = useBatches();
  const batches = query.data ?? [];

  return (
    <>
      <div className="page-head">
        <div>
          <h1 className="page-title">Monitor de integração</h1>
          <p className="page-sub">Acompanhe a sincronização, inspecione falhas e reprocesse a DLQ.</p>
        </div>
        <Link to="/integracao/enviar" className="btn btn--secondary">
          Enviar lote de exemplo
        </Link>
      </div>

      <section className="card" aria-labelledby="batches-title">
        <div className="page-head card__pad" style={{ marginBottom: 0 }}>
          <div>
            <h2 className="card__title" id="batches-title">
              Lotes
            </h2>
            {query.isSuccess ? <p className="page-sub">{batches.length === 1 ? "1 lote recebido" : `${batches.length} lotes recebidos`}</p> : null}
          </div>
          {query.isSuccess ? <p className="page-sub">Atualizado às {formatTime(new Date(query.dataUpdatedAt))}</p> : null}
        </div>
        {query.isPending ? (
          <TableSkeleton columns={COLUMNS} label="Carregando lotes" />
        ) : query.isError && !query.data ? (
          <StateMessage kind="error" title="Não foi possível carregar os lotes" actionLabel="Tentar de novo" onAction={() => void query.refetch()}>
            {errorMessage(query.error)}
          </StateMessage>
        ) : batches.length === 0 ? (
          <StateMessage kind="empty" title="Nenhum lote recebido" actionLabel="Enviar lote de exemplo" onAction={() => navigate("/integracao/enviar")}>
            Os lotes enviados pelas integrações aparecem aqui. Para testar o fluxo, envie um lote de exemplo.
          </StateMessage>
        ) : (
          <Table aria-labelledby="batches-title">
            <thead>
              <tr>
                <th>Lote</th>
                <th>Origem</th>
                <th>Recebido em</th>
                <th>Estado</th>
                <th className="num">PENDING</th>
                <th className="num">PROCESSING</th>
                <th className="num">DONE</th>
                <th className="num">DEAD</th>
                <th>Progresso</th>
              </tr>
            </thead>
            <tbody>
              {batches.map((batch) => {
                const total = batch.total ?? 0;
                const pct = (value: number | undefined) => (total > 0 ? ((value ?? 0) / total) * 100 : 0);
                return (
                  <tr key={batch.id}>
                    <td>
                      <Link to={`/integracao/lotes/${batch.id}`} className="table__id">
                        Lote #{batch.id}
                      </Link>
                    </td>
                    <td>{batch.source}</td>
                    <td>{batch.createdAt ? formatDateTime(batch.createdAt) : "—"}</td>
                    <td>
                      <BatchStateBadge state={batch.state} />
                    </td>
                    <td className="num">{batch.pending ?? 0}</td>
                    <td className="num">{batch.processing ?? 0}</td>
                    <td className="num">{batch.done ?? 0}</td>
                    <td className="num" style={(batch.dead ?? 0) > 0 ? { color: "var(--err)", fontWeight: 600 } : undefined}>
                      {batch.dead ?? 0}
                    </td>
                    <td>
                      <div className="meter" role="img" aria-label={`${batch.done ?? 0} de ${total} concluídas`}>
                        <span className="meter__done" style={{ width: `${pct(batch.done)}%` }} />
                        <span className="meter__dead" style={{ width: `${pct(batch.dead)}%` }} />
                        <span className="meter__pending" style={{ width: `${pct((batch.pending ?? 0) + (batch.processing ?? 0))}%` }} />
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </Table>
        )}
      </section>
    </>
  );
}
