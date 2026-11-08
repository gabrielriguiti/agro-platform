import type { IntegrationMessage, MessageStatus } from "@agro/api-client";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { errorMessage } from "../../api/errors";
import { useBatch, useBatchMessages, useMessage, useReprocessBatch, useReprocessMessage } from "../../api/integration";
import { Badge, Button, Modal, Notice, Pagination, StateMessage, Table, TableSkeleton, useToast } from "../../components";
import { formatDateTime, formatTime } from "../../lib/format";
import { BatchStateBadge } from "./BatchStateBadge";
import { messageTypeLabel, prettyPayload } from "./integrationModel";

const PAGE_SIZE = 10;
const COLUMNS = ["Tipo", "Id externo", "Status", "Tentativas", "Próxima tentativa", "Último erro", "Ações"];
const STATUSES: MessageStatus[] = ["PENDING", "PROCESSING", "DONE", "DEAD"];

function StatusBadge({ status }: { status: string | undefined }) {
  const tone = status === "DEAD" ? "solid-err" : status === "DONE" ? "ok" : status === "PROCESSING" ? "info" : "neutral";
  return (
    <Badge tone={tone} mono>
      {status ?? "—"}
    </Badge>
  );
}

/** Lote aberto: resumo, mensagens com filtro por status, destaque da DLQ, detalhe e reprocessamento. */
export function BatchDetail() {
  const { notify } = useToast();
  const batchId = Number(useParams().batchId);
  const [status, setStatus] = useState<MessageStatus | undefined>();
  const [page, setPage] = useState(0);
  const [openId, setOpenId] = useState<number | null>(null);
  const [confirmDead, setConfirmDead] = useState(false);

  const batch = useBatch(batchId);
  const live = batch.data?.state === "EM_ANDAMENTO";
  const messages = useBatchMessages(batchId, status, page, PAGE_SIZE, live);
  const reprocessBatch = useReprocessBatch();
  const reprocessMessage = useReprocessMessage();

  const counts: Record<MessageStatus, number> = {
    PENDING: batch.data?.pending ?? 0,
    PROCESSING: batch.data?.processing ?? 0,
    DONE: batch.data?.done ?? 0,
    DEAD: batch.data?.dead ?? 0,
  };
  const dead = counts.DEAD;

  const chooseStatus = (next: MessageStatus | undefined) => {
    setStatus(next);
    setPage(0);
  };

  const doReprocessBatch = () =>
    reprocessBatch.mutate(batchId, {
      onSuccess: (result) => {
        setConfirmDead(false);
        const n = result.requeued ?? 0;
        notify({ title: "Reprocessamento iniciado", message: n === 1 ? "1 mensagem voltou a PENDING." : `${n} mensagens voltaram a PENDING.` });
      },
      onError: (error) => {
        setConfirmDead(false);
        notify({ tone: "error", title: "Falha ao reprocessar", message: errorMessage(error) });
      },
    });

  const doReprocessMessage = (message: IntegrationMessage) =>
    reprocessMessage.mutate(message.id as number, {
      onSuccess: () => {
        setOpenId(null);
        notify({ title: "Mensagem reprocessada", message: `${message.externalId} voltou a PENDING.` });
      },
      onError: (error) => notify({ tone: "error", title: "Falha ao reprocessar", message: errorMessage(error) }),
    });

  if (!Number.isFinite(batchId)) return <StateMessage kind="error" title="Lote inválido" />;

  return (
    <>
      <div className="page-head">
        <div>
          <p className="breadcrumb">
            <Link to="/integracao">Integração</Link> / Lotes
          </p>
          <h1 className="page-title">Lote #{batchId}</h1>
        </div>
      </div>

      {batch.isPending ? (
        <div className="card card__pad" aria-busy="true" aria-label="Carregando lote">
          <span className="skeleton" style={{ width: "50%" }} />
        </div>
      ) : batch.isError && !batch.data ? (
        <div className="card">
          <StateMessage kind="error" title="Não foi possível carregar o lote" actionLabel="Tentar de novo" onAction={() => void batch.refetch()}>
            {errorMessage(batch.error)}
          </StateMessage>
        </div>
      ) : (
        <div className="stack">
          <section className="card card__pad" aria-label="Resumo do lote">
            <dl className="row" style={{ gap: "var(--s-8)", margin: 0 }}>
              <div>
                <dt className="field__help">Origem</dt>
                <dd style={{ margin: 0, fontWeight: 500 }}>{batch.data?.source}</dd>
              </div>
              <div>
                <dt className="field__help">Recebido em</dt>
                <dd style={{ margin: 0, fontWeight: 500 }}>{batch.data?.createdAt ? formatDateTime(batch.data.createdAt) : "—"}</dd>
              </div>
              <div>
                <dt className="field__help">Total de mensagens</dt>
                <dd style={{ margin: 0, fontWeight: 500 }}>{batch.data?.total ?? 0}</dd>
              </div>
              <div>
                <dt className="field__help">Estado</dt>
                <dd style={{ margin: 0 }}>
                  <BatchStateBadge state={batch.data?.state} />
                </dd>
              </div>
            </dl>
            {live ? <p className="page-sub">Atualização automática a cada 5 s · última às {formatTime(new Date(batch.dataUpdatedAt))}</p> : null}
          </section>

          <section className="card" aria-labelledby="msgs-title">
            <div className="page-head card__pad" style={{ marginBottom: 0 }}>
              <h2 className="card__title" id="msgs-title">
                Mensagens
              </h2>
              <Button variant="secondary" disabled={dead === 0} onClick={() => setConfirmDead(true)}>
                Reprocessar mortas ({dead})
              </Button>
            </div>
            <div className="card__pad" style={{ paddingTop: 0 }}>
              <div className="tabs" role="group" aria-label="Filtrar por status">
                <button type="button" className="tab" aria-pressed={status === undefined} onClick={() => chooseStatus(undefined)}>
                  Todos <b>{batch.data?.total ?? 0}</b>
                </button>
                {STATUSES.map((s) => (
                  <button key={s} type="button" className="tab" aria-pressed={status === s} onClick={() => chooseStatus(s)}>
                    {s} <b>{counts[s]}</b>
                  </button>
                ))}
              </div>
            </div>

            {messages.isPending ? (
              <TableSkeleton columns={COLUMNS} label="Carregando mensagens" />
            ) : messages.isError && !messages.data ? (
              <StateMessage kind="error" title="Não foi possível carregar as mensagens" actionLabel="Tentar de novo" onAction={() => void messages.refetch()}>
                {errorMessage(messages.error)}
              </StateMessage>
            ) : (messages.data?.content ?? []).length === 0 ? (
              <StateMessage kind="empty" title="Nenhuma mensagem" >
                {status ? `Não há mensagens com status ${status} neste lote.` : "Este lote não tem mensagens."}
              </StateMessage>
            ) : (
              <>
                <Table aria-labelledby="msgs-title">
                  <thead>
                    <tr>
                      <th>Tipo</th>
                      <th>Id externo</th>
                      <th>Status</th>
                      <th className="num">Tentativas</th>
                      <th>Próxima tentativa</th>
                      <th>Último erro</th>
                      <th>
                        <span className="sr-only">Ações</span>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {(messages.data?.content ?? []).map((message) => {
                      const isDead = message.status === "DEAD";
                      const waiting = message.status === "PENDING" || message.status === "PROCESSING";
                      return (
                        <tr key={message.id} className={isDead ? "row--dead" : undefined}>
                          <td className="mono">{messageTypeLabel(message.type)}</td>
                          <td>
                            <button type="button" className="link-btn table__id" onClick={() => setOpenId(message.id as number)} aria-label={`Ver detalhes de ${message.externalId}`}>
                              {message.externalId}
                            </button>
                          </td>
                          <td>
                            <StatusBadge status={message.status} />
                          </td>
                          <td className="num">{message.attempts ?? 0}</td>
                          <td>{waiting && message.nextAttemptAt ? formatDateTime(message.nextAttemptAt) : "—"}</td>
                          <td className="cell-error">{message.lastError || "—"}</td>
                          <td>
                            <div className="actions">
                              {isDead ? (
                                <Button size="sm" variant="ghost" loading={reprocessMessage.isPending && reprocessMessage.variables === message.id} onClick={() => doReprocessMessage(message)} aria-label={`Reprocessar ${message.externalId}`}>
                                  Reprocessar
                                </Button>
                              ) : null}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </Table>
                <Pagination page={messages.data?.page ?? page} size={messages.data?.size ?? PAGE_SIZE} totalElements={messages.data?.totalElements ?? 0} totalPages={messages.data?.totalPages ?? 1} onPageChange={setPage} />
              </>
            )}
          </section>
        </div>
      )}

      <Modal
        open={confirmDead}
        title={dead === 1 ? "Reprocessar 1 mensagem morta?" : `Reprocessar ${dead} mensagens mortas?`}
        description="As mensagens voltam para PENDING com as tentativas zeradas."
        onClose={() => (reprocessBatch.isPending ? undefined : setConfirmDead(false))}
        actions={
          <>
            <Button disabled={reprocessBatch.isPending} onClick={() => setConfirmDead(false)}>
              Cancelar
            </Button>
            <Button variant="primary" loading={reprocessBatch.isPending} onClick={doReprocessBatch}>
              Reprocessar mortas
            </Button>
          </>
        }
      />

      <MessageDetail id={openId} onClose={() => setOpenId(null)} onReprocess={doReprocessMessage} reprocessing={reprocessMessage.isPending} />
    </>
  );
}

interface MessageDetailProps {
  id: number | null;
  onClose: () => void;
  onReprocess: (message: IntegrationMessage) => void;
  reprocessing: boolean;
}

/** Painel lateral com o detalhe de uma mensagem: status, tentativas, motivo da falha e payload original. */
function MessageDetail({ id, onClose, onReprocess, reprocessing }: MessageDetailProps) {
  const query = useMessage(id);
  const message = query.data;
  return (
    <Modal
      open={id !== null}
      side
      title={message ? `Mensagem ${message.externalId}` : "Mensagem"}
      onClose={onClose}
      actions={
        <>
          <Button onClick={onClose}>Fechar</Button>
          {message?.status === "DEAD" ? (
            <Button variant="primary" loading={reprocessing} onClick={() => onReprocess(message)}>
              Reprocessar
            </Button>
          ) : null}
        </>
      }
    >
      {query.isPending ? (
        <span className="skeleton" style={{ width: "60%" }} aria-label="Carregando mensagem" />
      ) : query.isError ? (
        <Notice tone="error" title="Não foi possível carregar a mensagem">
          {errorMessage(query.error)}
        </Notice>
      ) : (
        <div className="stack">
          <dl className="row" style={{ gap: "var(--s-8)", margin: 0 }}>
            <div>
              <dt className="field__help">Tipo</dt>
              <dd style={{ margin: 0 }} className="mono">
                {messageTypeLabel(message?.type)}
              </dd>
            </div>
            <div>
              <dt className="field__help">Status</dt>
              <dd style={{ margin: 0 }}>
                <StatusBadge status={message?.status} />
              </dd>
            </div>
            <div>
              <dt className="field__help">Tentativas</dt>
              <dd style={{ margin: 0, fontWeight: 500 }}>{message?.attempts ?? 0}</dd>
            </div>
          </dl>
          {message?.lastError ? (
            <Notice tone="error" title="Motivo da falha">
              {message.lastError}
            </Notice>
          ) : null}
          <div>
            <p className="field__label" id="payload-label">
              Payload original
            </p>
            <pre className="code" aria-labelledby="payload-label">
              {prettyPayload(message?.payload) || "—"}
            </pre>
          </div>
        </div>
      )}
    </Modal>
  );
}
