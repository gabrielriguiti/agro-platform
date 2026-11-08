import { useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useRef, useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError, errorMessage } from "../api/errors";
import { useAvailableProperties, useLaboratories, useProducer, useSaveProducer } from "../api/registry";
import { Button, Field, Input, MultiSelect, Notice, Select, StateMessage, Textarea, useToast } from "../components";
import type { MultiSelectOption } from "../components";
import { maskTaxId } from "../lib/taxId";
import { emptyProducerForm, PRODUCER_FIELDS, periodDays, toFormValues, toRequest, validateProducer } from "./producerFormModel";
import type { ProducerErrors, ProducerField, ProducerFormValues } from "./producerFormModel";

/** Aviso de nível de formulário (erro 409 ou 400 sem campo correspondente). */
interface FormNotice {
  title: string;
  message: string;
  tag?: string;
}

function isProducerField(field: string): field is ProducerField {
  return (PRODUCER_FIELDS as string[]).includes(field);
}

/** Rota de criação (`/produtores/novo`) e de edição (`/produtores/:id/editar`) de produtor. */
export function ProducerFormPage() {
  const params = useParams();
  const id = params.id !== undefined ? Number(params.id) : undefined;
  const producer = useProducer(Number.isFinite(id) ? id : undefined);

  if (id === undefined) {
    return <ProducerForm key="new" />;
  }
  return (
    <>
      <Header editing name={producer.data?.nome} />
      {producer.isPending ? (
        <div className="card card__pad" aria-busy="true" aria-label="Carregando produtor">
          <span className="skeleton" style={{ width: "40%" }} />
        </div>
      ) : producer.isError ? (
        <div className="card">
          <StateMessage kind="error" title="Não foi possível carregar o produtor" actionLabel="Tentar de novo" onAction={() => void producer.refetch()}>
            {errorMessage(producer.error)}
          </StateMessage>
        </div>
      ) : (
        <ProducerForm key={id} producerId={id} initial={toFormValues(producer.data)} showHeader={false} />
      )}
    </>
  );
}

/** Aviso para banco sem laboratório ou propriedade, com link para a tela de cadastro correspondente. */
function EmptyRegistryHint({ what }: { what: "laboratório" | "propriedade" }) {
  return (
    <Notice tone="info" title={what === "laboratório" ? "Nenhum laboratório cadastrado" : "Nenhuma propriedade disponível"}>
      {what === "laboratório" ? (
        <>
          Para escolher um laboratório, <Link to="/laboratorios/novo">cadastre um laboratório</Link>.
        </>
      ) : (
        <>
          Para vincular propriedades, <Link to="/propriedades/novo">cadastre uma propriedade</Link>.
        </>
      )}
    </Notice>
  );
}

function Header({ editing, name }: { editing: boolean; name?: string }) {
  return (
    <div className="page-head">
      <div>
        <p className="breadcrumb">
          <Link to="/produtores">Produtores</Link> / {editing ? "Editar" : "Novo produtor"}
        </p>
        <h1 className="page-title">
          {editing ? "Editar produtor" : "Novo produtor"}
          {editing && name ? <span className="muted"> · {name}</span> : null}
        </h1>
      </div>
    </div>
  );
}

interface ProducerFormProps {
  /** Produtor em edição; ausente na criação. */
  producerId?: number;
  initial?: ProducerFormValues;
  showHeader?: boolean;
}

function ProducerForm({ producerId, initial = emptyProducerForm, showHeader = true }: ProducerFormProps) {
  const navigate = useNavigate();
  const { notify } = useToast();
  const queryClient = useQueryClient();
  const save = useSaveProducer(producerId);
  const labs = useLaboratories();
  const properties = useAvailableProperties(producerId);
  const formRef = useRef<HTMLFormElement>(null);

  const [values, setValues] = useState<ProducerFormValues>(initial);
  const [touched, setTouched] = useState<Partial<Record<ProducerField, boolean>>>({});
  const [submitted, setSubmitted] = useState(false);
  const [serverErrors, setServerErrors] = useState<ProducerErrors>({});
  const [notice, setNotice] = useState<FormNotice | null>(null);
  // Propriedades já vistas: mantêm o rótulo do chip mesmo depois de saírem da lista de disponíveis.
  const [known, setKnown] = useState<Record<number, MultiSelectOption>>({});

  const options = useMemo<MultiSelectOption[]>(
    () => (properties.data ?? []).flatMap((p) => (p.id !== undefined ? [{ value: p.id, label: p.nome ?? `#${p.id}`, description: p.endereco }] : [])),
    [properties.data],
  );
  useEffect(() => {
    setKnown((current) => ({ ...current, ...Object.fromEntries(options.map((o) => [o.value, o])) }));
  }, [options]);
  const selectOptions = useMemo(() => {
    const present = new Set(options.map((o) => o.value));
    const missing = values.propriedadeIds.filter((v) => !present.has(v)).map((v) => known[v] ?? { value: v, label: `Propriedade #${v}` });
    return [...options, ...missing];
  }, [options, known, values.propriedadeIds]);
  // Selecionadas que deixaram de estar disponíveis (ex.: vinculadas a outro produtor após um 409).
  const unavailable = useMemo(() => {
    if (!properties.isSuccess || properties.isFetching) return [];
    const present = new Set(options.map((o) => o.value));
    return values.propriedadeIds.filter((v) => !present.has(v));
  }, [properties.isSuccess, properties.isFetching, options, values.propriedadeIds]);

  const clientErrors = useMemo(() => validateProducer(values), [values]);
  const visible = (field: ProducerField): string | null => {
    if (serverErrors[field]) return serverErrors[field] ?? null;
    return (submitted || touched[field]) && clientErrors[field] ? (clientErrors[field] ?? null) : null;
  };
  const unavailableError = unavailable.length > 0 ? "Há propriedades que não estão mais disponíveis. Remova-as da seleção para salvar." : null;

  const set = <K extends keyof ProducerFormValues>(field: K, value: ProducerFormValues[K]) => {
    setValues((current) => ({ ...current, [field]: value }));
    if (isProducerField(field) && serverErrors[field]) setServerErrors((current) => ({ ...current, [field]: undefined }));
  };
  const blur = (field: ProducerField) => setTouched((current) => ({ ...current, [field]: true }));

  const focusFirstInvalid = () =>
    requestAnimationFrame(() => formRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus());

  const submit = (event: FormEvent) => {
    event.preventDefault();
    setSubmitted(true);
    setNotice(null);
    if (Object.keys(clientErrors).length > 0 || unavailable.length > 0) {
      focusFirstInvalid();
      return;
    }
    save.mutate(toRequest(values), {
      onSuccess: (saved) => {
        notify({ title: producerId === undefined ? "Produtor salvo" : "Alterações salvas", message: producerId === undefined ? `${saved.nome ?? values.nome} foi cadastrado com sucesso.` : "Os dados do produtor foram atualizados." });
        navigate("/produtores");
      },
      onError: (error) => {
        if (!(error instanceof ApiError)) {
          setNotice({ title: "Não foi possível salvar", message: errorMessage(error) });
          return;
        }
        const mapped: ProducerErrors = {};
        const unmapped: string[] = [];
        for (const problem of error.fieldErrors) {
          if (isProducerField(problem.field)) mapped[problem.field] = problem.message;
          else unmapped.push(problem.message);
        }
        setServerErrors(mapped);
        if (error.status === 409) {
          // Outra pessoa pode ter vinculado a propriedade: recarrega a lista, mantendo o que foi digitado.
          void queryClient.invalidateQueries({ queryKey: ["properties"] });
        }
        if (Object.keys(mapped).length === 0 || unmapped.length > 0 || error.status !== 400) {
          const tag = error.status > 0 ? `HTTP ${error.status}` : undefined;
          setNotice(
            error.status === 409
              ? { title: error.message, message: "Os demais dados foram mantidos.", tag }
              : { title: error.title ?? "Não foi possível salvar", message: unmapped.length > 0 ? unmapped.join(" ") : error.message, tag },
          );
        }
        focusFirstInvalid();
      },
    });
  };

  const labName = labs.data?.find((lab) => String(lab.id) === values.laboratorioId)?.nome;
  const days = periodDays(values);
  const count = values.propriedadeIds.length;

  return (
    <>
      {showHeader ? <Header editing={false} /> : null}
      <form ref={formRef} noValidate onSubmit={submit} aria-label={producerId === undefined ? "Novo produtor" : "Editar produtor"}>
        <fieldset className="fieldset-reset" disabled={save.isPending}>
          <div className="form-layout">
            <div className="stack">
              {notice ? (
                <Notice tone="error" title={notice.title} tag={notice.tag}>
                  {notice.message}
                </Notice>
              ) : null}

              <section className="card card__pad" aria-labelledby="sec-id">
                <h2 className="section-title" id="sec-id">
                  Identificação
                </h2>
                <div className="form-grid">
                  <Field label="Nome" required error={visible("nome")} className="span-2">
                    {(control) => <Input {...control} value={values.nome} autoComplete="off" placeholder="Nome completo" onChange={(e) => set("nome", e.target.value)} onBlur={() => blur("nome")} />}
                  </Field>
                  <Field label="Inscrição fiscal" required error={visible("inscricaoFiscal")} help="CPF ou CNPJ. A máscara é aplicada ao digitar.">
                    {(control) => (
                      <Input
                        {...control}
                        className="mono-input"
                        inputMode="numeric"
                        placeholder="000.000.000-00"
                        value={values.inscricaoFiscal}
                        onChange={(e) => set("inscricaoFiscal", maskTaxId(e.target.value))}
                        onBlur={() => blur("inscricaoFiscal")}
                      />
                    )}
                  </Field>
                  <Field label="Endereço" required error={visible("endereco")}>
                    {(control) => <Input {...control} value={values.endereco} autoComplete="off" onChange={(e) => set("endereco", e.target.value)} onBlur={() => blur("endereco")} />}
                  </Field>
                </div>
              </section>

              <section className="card card__pad" aria-labelledby="sec-period">
                <h2 className="section-title" id="sec-period">
                  Período
                </h2>
                <div className="form-grid">
                  <Field label="Data e hora inicial" required error={visible("dataInicial")}>
                    {(control) => <Input {...control} type="datetime-local" value={values.dataInicial} onChange={(e) => set("dataInicial", e.target.value)} onBlur={() => blur("dataInicial")} />}
                  </Field>
                  <Field label="Data e hora final" required error={visible("dataFinal")}>
                    {(control) => <Input {...control} type="datetime-local" value={values.dataFinal} onChange={(e) => set("dataFinal", e.target.value)} onBlur={() => blur("dataFinal")} />}
                  </Field>
                </div>
              </section>

              <section className="card card__pad" aria-labelledby="sec-links">
                <h2 className="section-title" id="sec-links">
                  Vínculos
                </h2>
                <div className="stack">
                  <Field label="Laboratório" required error={visible("laboratorioId")} help={labs.isError ? undefined : "Escolha única."}>
                    {(control) => (
                      <Select {...control} value={values.laboratorioId} onChange={(e) => set("laboratorioId", e.target.value)} onBlur={() => blur("laboratorioId")}>
                        <option value="">{labs.isPending ? "Carregando…" : "Selecione"}</option>
                        {(labs.data ?? []).map((lab) => (
                          <option key={lab.id} value={lab.id}>
                            {lab.nome}
                          </option>
                        ))}
                      </Select>
                    )}
                  </Field>
                  {labs.isError ? <Notice tone="error" title="Não foi possível carregar os laboratórios">{errorMessage(labs.error)}</Notice> : null}
                  {labs.isSuccess && labs.data.length === 0 ? <EmptyRegistryHint what="laboratório" /> : null}
                  <MultiSelect
                    label="Propriedades"
                    placeholder="Selecione as propriedades"
                    options={selectOptions}
                    value={values.propriedadeIds}
                    onChange={(next) => set("propriedadeIds", next)}
                    invalidValues={unavailable}
                    error={visible("propriedadeIds") ?? unavailableError}
                    help={properties.isError ? undefined : "Selecione uma ou mais propriedades."}
                  />
                  {properties.isError ? <Notice tone="error" title="Não foi possível carregar as propriedades">{errorMessage(properties.error)}</Notice> : null}
                  {properties.isSuccess && selectOptions.length === 0 ? <EmptyRegistryHint what="propriedade" /> : null}
                </div>
              </section>

              <section className="card card__pad" aria-labelledby="sec-notes">
                <h2 className="section-title" id="sec-notes">
                  Observações
                </h2>
                <Field label="Observações">
                  {(control) => <Textarea {...control} value={values.observacoes} placeholder="Informações adicionais" onChange={(e) => set("observacoes", e.target.value)} />}
                </Field>
              </section>
            </div>

            <aside className="card card__pad" aria-label="Resumo">
              <h2 className="card__title">Resumo</h2>
              <dl className="summary">
                <dt>Laboratório</dt>
                <dd>{labName ?? "—"}</dd>
                <dt>Propriedades</dt>
                <dd>{count === 0 ? "Nenhuma" : count === 1 ? "1 selecionada" : `${count} selecionadas`}</dd>
                <dt>Duração do período</dt>
                <dd>{days === null ? "—" : days === 1 ? "1 dia" : `${days} dias`}</dd>
              </dl>
              <div className="stack" style={{ marginTop: "var(--s-5)" }}>
                <Button type="submit" variant="primary" loading={save.isPending}>
                  {save.isPending ? "Salvando…" : "Salvar"}
                </Button>
                <Button onClick={() => navigate("/produtores")}>Cancelar</Button>
                <p className="field__help">
                  <span style={{ color: "var(--err)" }}>*</span> Campos obrigatórios
                </p>
              </div>
            </aside>
          </div>
        </fieldset>
      </form>
    </>
  );
}
