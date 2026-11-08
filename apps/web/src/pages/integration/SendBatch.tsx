import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, errorMessage } from "../../api/errors";
import { useSendBatch } from "../../api/integration";
import { Button, Field, Notice, Textarea, useToast } from "../../components";
import { parseBatch, sampleBatchJson } from "./integrationModel";

/** Formulário para colar ou editar o JSON de um lote e enviá-lo, com um exemplo pronto que demonstra a DLQ. */
export function SendBatch() {
  const navigate = useNavigate();
  const { notify } = useToast();
  const send = useSendBatch();
  const [text, setText] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    setServerError(null);
    const parsed = parseBatch(text);
    if (!parsed.ok) {
      setError(parsed.error);
      return;
    }
    setError(null);
    send.mutate(parsed.batch, {
      onSuccess: (result) => {
        const accepted = result.accepted ?? 0;
        const duplicates = result.duplicates ?? 0;
        notify({
          title: "Lote recebido",
          message: `Lote #${result.batchId} entrou na fila: ${accepted} ${accepted === 1 ? "item aceito" : "itens aceitos"}${duplicates > 0 ? `, ${duplicates} duplicado${duplicates === 1 ? "" : "s"}` : ""}.`,
        });
        navigate(`/integracao/lotes/${result.batchId}`);
      },
      onError: (e) => {
        const fields = e instanceof ApiError ? e.fieldErrors.map((f) => f.message) : [];
        setServerError(fields.length > 0 ? fields.join(" ") : errorMessage(e));
      },
    });
  };

  return (
    <>
      <div className="page-head">
        <div>
          <p className="breadcrumb">
            <Link to="/integracao">Integração</Link> / Enviar lote
          </p>
          <h1 className="page-title">Enviar lote de exemplo</h1>
          <p className="page-sub">O exemplo inclui um produtor com CPF inválido, que termina na DLQ para demonstrar o reprocessamento.</p>
        </div>
      </div>
      <form className="card card__pad stack" noValidate onSubmit={submit} aria-label="Enviar lote">
        {serverError ? (
          <Notice tone="error" title="O servidor recusou o lote">
            {serverError}
          </Notice>
        ) : null}
        <Field label="JSON do lote" required error={error}>
          {(control) => (
            <Textarea
              {...control}
              className="json-editor"
              spellCheck={false}
              placeholder='{ "source": "erp-demo", "mergeStrategy": "MERGE", "items": [ … ] }'
              value={text}
              onChange={(e) => {
                setText(e.target.value);
                setError(null);
              }}
            />
          )}
        </Field>
        <div className="row row--end">
          <Button
            onClick={() => {
              setText(sampleBatchJson());
              setError(null);
              setServerError(null);
            }}
          >
            Carregar exemplo
          </Button>
          <Button type="submit" variant="primary" loading={send.isPending}>
            {send.isPending ? "Enviando…" : "Enviar lote"}
          </Button>
        </div>
      </form>
    </>
  );
}
