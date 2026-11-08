import { useMemo, useRef, useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError, errorMessage } from "../../api/errors";
import { useProducer } from "../../api/registry";
import { Button, Field, Input, Notice, StateMessage, useToast } from "../../components";
import { maskTaxId } from "../../lib/taxId";
import type { RegistryConfig } from "./registryConfig";
import { emptyRegistryForm, errorSummary, REGISTRY_FIELDS, validateRegistry } from "./registryModel";
import type { RegistryErrors, RegistryField, RegistryFormValues } from "./registryModel";

function isField(field: string): field is RegistryField {
  return (REGISTRY_FIELDS as string[]).includes(field);
}

/** Rota de criação (`<base>/novo`) e de edição (`<base>/:id/editar`) de laboratório ou propriedade. */
export function RegistryFormPage({ config }: { config: RegistryConfig }) {
  const params = useParams();
  const id = params.id !== undefined ? Number(params.id) : undefined;
  const existing = config.useOne(Number.isFinite(id) ? id : undefined);

  if (id === undefined) return <RegistryForm key="new" config={config} />;
  return (
    <>
      <Header config={config} editing name={existing.data?.nome} />
      {existing.isPending ? (
        <div className="card card__pad stack" style={{ maxWidth: 720 }} aria-busy="true" aria-label="Carregando cadastro">
          <h2 className="section-title">{config.sectionTitle}</h2>
          {["30%", "45%", "70%"].map((width) => (
            <div key={width} className="stack" style={{ gap: "var(--s-2)" }}>
              <span className="skeleton" style={{ width: "12%" }} />
              <span className="skeleton" style={{ width, height: 40 }} />
            </div>
          ))}
        </div>
      ) : existing.isError ? (
        <div className="card">
          <StateMessage kind="error" title={config.loadOneError} actionLabel="Tentar de novo" onAction={() => void existing.refetch()}>
            {errorMessage(existing.error)}
          </StateMessage>
        </div>
      ) : (
        <RegistryForm
          key={id}
          config={config}
          itemId={id}
          showHeader={false}
          linkedProducerId={existing.data?.produtorId}
          initial={{ nome: existing.data?.nome ?? "", inscricaoFiscal: existing.data?.inscricaoFiscal ?? "", endereco: existing.data?.endereco ?? "" }}
        />
      )}
    </>
  );
}

function Header({ config, editing, name }: { config: RegistryConfig; editing: boolean; name?: string }) {
  return (
    <div className="page-head">
      <div>
        <p className="breadcrumb">
          <Link to={config.base}>{config.listTitle}</Link> / {editing ? "Editar" : config.breadcrumbNew}
        </p>
        <h1 className="page-title">
          {editing ? config.editTitle : config.newLabel}
          {editing && name ? <span className="muted"> · {name}</span> : null}
        </h1>
      </div>
    </div>
  );
}

/** Situação da propriedade em edição: vinculada a um produtor (com o nome) ou disponível. */
function ProducerLink({ producerId }: { producerId?: number | null }) {
  const linked = producerId != null;
  const producer = useProducer(linked ? producerId : undefined);
  const who = producer.data?.nome ?? (producer.isError ? `o produtor #${producerId}` : "…");
  return (
    <div>
      <h3 className="card__title">Vínculo com produtor</h3>
      <p className="muted" style={{ marginTop: "var(--s-1)" }}>
        {linked ? `Vinculada a ${who}.` : "Disponível, sem produtor."} O vínculo com o produtor não é alterado aqui; ele é feito no formulário de produtor.
      </p>
    </div>
  );
}

interface RegistryFormProps {
  config: RegistryConfig;
  itemId?: number;
  initial?: RegistryFormValues;
  showHeader?: boolean;
  /** Produtor da propriedade em edição (só para propriedade). */
  linkedProducerId?: number | null;
}

function RegistryForm({ config, itemId, initial = emptyRegistryForm, showHeader = true, linkedProducerId }: RegistryFormProps) {
  const navigate = useNavigate();
  const { notify } = useToast();
  const save = config.useSave(itemId);
  const formRef = useRef<HTMLFormElement>(null);
  const [values, setValues] = useState<RegistryFormValues>(initial);
  const [touched, setTouched] = useState<Partial<Record<RegistryField, boolean>>>({});
  const [submitted, setSubmitted] = useState(false);
  const [serverErrors, setServerErrors] = useState<RegistryErrors>({});
  const [notice, setNotice] = useState<{ title: string; message: string; tag?: string } | null>(null);

  const clientErrors = useMemo(() => validateRegistry(config.kind, values), [config.kind, values]);
  const visible = (field: RegistryField): string | null => {
    if (serverErrors[field]) return serverErrors[field] ?? null;
    return (submitted || touched[field]) && clientErrors[field] ? (clientErrors[field] ?? null) : null;
  };
  const set = (field: RegistryField, value: string) => {
    setValues((current) => ({ ...current, [field]: value }));
    if (serverErrors[field]) setServerErrors((current) => ({ ...current, [field]: undefined }));
  };
  const blur = (field: RegistryField) => setTouched((current) => ({ ...current, [field]: true }));
  const focusFirstInvalid = () => requestAnimationFrame(() => formRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus());
  const summary = submitted ? errorSummary(config.kind, clientErrors) : null;

  const submit = (event: FormEvent) => {
    event.preventDefault();
    setSubmitted(true);
    setNotice(null);
    if (Object.keys(clientErrors).length > 0) {
      focusFirstInvalid();
      return;
    }
    save.mutate(
      { nome: values.nome.trim(), inscricaoFiscal: values.inscricaoFiscal, endereco: values.endereco.trim() },
      {
        onSuccess: (saved) => {
          notify(itemId === undefined ? { title: config.savedTitle, message: config.savedMessage(saved.nome ?? values.nome) } : { title: config.updatedTitle, message: "Os dados foram atualizados." });
          navigate(config.base);
        },
        onError: (error) => {
          if (!(error instanceof ApiError)) {
            setNotice({ title: "Não foi possível salvar", message: errorMessage(error) });
            return;
          }
          const mapped: RegistryErrors = {};
          const unmapped: string[] = [];
          for (const problem of error.fieldErrors) {
            if (isField(problem.field)) mapped[problem.field] = problem.message;
            else unmapped.push(problem.message);
          }
          // 409: a inscrição fiscal pertence a outro cadastro; mostra no campo e em aviso no topo.
          if (error.status === 409) mapped.inscricaoFiscal = error.message;
          setServerErrors(mapped);
          if (error.status === 409) {
            setNotice({ title: error.message, message: "Os demais dados foram mantidos.", tag: "HTTP 409" });
          } else if (Object.keys(mapped).length === 0 || unmapped.length > 0 || error.status !== 400) {
            setNotice({ title: error.title ?? "Não foi possível salvar", message: unmapped.length > 0 ? unmapped.join(" ") : error.message, tag: error.status > 0 ? `HTTP ${error.status}` : undefined });
          }
          focusFirstInvalid();
        },
      },
    );
  };

  return (
    <>
      {showHeader ? <Header config={config} editing={false} /> : null}
      <form ref={formRef} noValidate onSubmit={submit} aria-label={itemId === undefined ? config.newLabel : config.editTitle}>
        <fieldset className="fieldset-reset" disabled={save.isPending}>
          <div className="stack" style={{ maxWidth: 720 }}>
            {notice ? (
              <Notice tone="error" title={notice.title} tag={notice.tag}>
                {notice.message}
              </Notice>
            ) : null}
            {summary ? (
              <Notice tone="error" title={summary.title}>
                {summary.message}
              </Notice>
            ) : null}
            <section className="card card__pad stack" aria-labelledby="sec-data">
              <h2 className="section-title" id="sec-data" style={{ marginBottom: 0 }}>
                {config.sectionTitle}
              </h2>
              <Field label="Nome" required error={visible("nome")}>
                {(control) => <Input {...control} value={values.nome} autoComplete="off" placeholder={config.namePlaceholder} onChange={(e) => set("nome", e.target.value)} onBlur={() => blur("nome")} />}
              </Field>
              <Field label={config.taxIdLabel} required error={visible("inscricaoFiscal")} help={config.taxIdHelp}>
                {(control) => (
                  <Input
                    {...control}
                    className="mono-input"
                    inputMode="numeric"
                    placeholder={config.taxIdPlaceholder}
                    value={values.inscricaoFiscal}
                    onChange={(e) => set("inscricaoFiscal", maskTaxId(e.target.value))}
                    onBlur={() => blur("inscricaoFiscal")}
                  />
                )}
              </Field>
              <Field label="Endereço" required error={visible("endereco")}>
                {(control) => (
                  <Input {...control} value={values.endereco} autoComplete="off" placeholder="Logradouro, número, município – UF, CEP" onChange={(e) => set("endereco", e.target.value)} onBlur={() => blur("endereco")} />
                )}
              </Field>
              {config.kind === "property" && itemId !== undefined ? <ProducerLink producerId={linkedProducerId} /> : null}
            </section>
            <div className="row" style={{ justifyContent: "space-between" }}>
              <p className="field__help">
                <span style={{ color: "var(--err)" }}>*</span> Campos obrigatórios
              </p>
              <div className="row">
                <Button onClick={() => navigate(config.base)}>Cancelar</Button>
                <Button type="submit" variant="primary" loading={save.isPending}>
                  {save.isPending ? "Salvando…" : "Salvar"}
                </Button>
              </div>
            </div>
          </div>
        </fieldset>
      </form>
    </>
  );
}
