import { useState } from "react";
import type { FormEvent } from "react";
import { errorMessage } from "../api/errors";
import { useLabAnalytics } from "../api/analytics";
import type { AnalyticsParams } from "../api/analytics";
import { BarChart, Button, Field, Input, StateMessage, Table, TableSkeleton } from "../components";
import { formatCurrency } from "../lib/format";
import { defaultFilters, sumCurrency, toParams, validateFilters } from "./analyticsModel";
import type { AnalyticsFilters } from "./analyticsModel";

const COLUMNS = ["Nome do laboratório", "Total de produtores", "Valor financeiro"];

/** Dashboard analítico: filtros da consulta, tabela e gráfico de barras na ordem devolvida pela API. */
export function AnalyticsPage() {
  const [filters, setFilters] = useState<AnalyticsFilters>(defaultFilters);
  const [submitted, setSubmitted] = useState<AnalyticsParams | null>(null);
  const query = useLabAnalytics(submitted);
  const errors = validateFilters(filters);
  const invalid = Object.keys(errors).length > 0;

  const set = (field: keyof AnalyticsFilters, value: string) => setFilters((current) => ({ ...current, [field]: value }));

  const run = (next: AnalyticsFilters) => {
    const params = toParams(next);
    if (submitted !== null && JSON.stringify(params) === JSON.stringify(submitted)) void query.refetch();
    else setSubmitted(params);
  };
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!invalid) run(filters);
  };
  const restore = () => {
    setFilters(defaultFilters);
    run(defaultFilters);
  };

  const loading = submitted !== null && (query.isPending || query.isFetching);
  const rows = query.data ?? [];

  return (
    <>
      <div className="page-head">
        <div>
          <h1 className="page-title">Dashboard de laboratórios</h1>
          <p className="page-sub">Total de produtores e valor financeiro consolidado por laboratório, conforme os filtros.</p>
        </div>
      </div>

      <form className="card card__pad" noValidate onSubmit={submit} aria-label="Filtros">
        <div className="filters">
          <div className="filters__group" role="group" aria-labelledby="grp-inicial">
            <span className="filters__legend" id="grp-inicial">
              Data inicial do produtor
            </span>
            <Field label="De" error={errors.dataInicial}>
              {(control) => <Input {...control} type="date" value={filters.dataInicialDe} onChange={(e) => set("dataInicialDe", e.target.value)} />}
            </Field>
            <Field label="Até">{(control) => <Input {...control} type="date" value={filters.dataInicialAte} onChange={(e) => set("dataInicialAte", e.target.value)} />}</Field>
          </div>
          <div className="filters__group" role="group" aria-labelledby="grp-final">
            <span className="filters__legend" id="grp-final">
              Data final do produtor
            </span>
            <Field label="De" error={errors.dataFinal}>
              {(control) => <Input {...control} type="date" value={filters.dataFinalDe} onChange={(e) => set("dataFinalDe", e.target.value)} />}
            </Field>
            <Field label="Até">{(control) => <Input {...control} type="date" value={filters.dataFinalAte} onChange={(e) => set("dataFinalAte", e.target.value)} />}</Field>
          </div>
          <Field label="Busca nas observações" className="filters__search">
            {(control) => <Input {...control} type="search" placeholder="Texto nas observações" value={filters.busca} onChange={(e) => set("busca", e.target.value)} />}
          </Field>
          <Field label="Qtd. mínima de produtores" required error={errors.qtdMinima} help={errors.qtdMinima ? undefined : "Padrão: 1"}>
            {(control) => <Input {...control} inputMode="numeric" value={filters.qtdMinima} onChange={(e) => set("qtdMinima", e.target.value)} />}
          </Field>
          <div className="filters__action">
            <Button type="submit" variant="primary" disabled={invalid} loading={loading}>
              {loading ? "Consultando…" : "Consultar"}
            </Button>
          </div>
        </div>
      </form>

      <div className="stack" style={{ marginTop: "var(--s-6)" }}>
        {submitted === null ? (
          <div className="card">
            <StateMessage kind="empty" title="Defina os filtros e consulte">
              Escolha os filtros acima e clique em Consultar para ver os laboratórios.
            </StateMessage>
          </div>
        ) : loading ? (
          <div className="card">
            <TableSkeleton columns={COLUMNS} label="Consultando laboratórios" />
          </div>
        ) : query.isError ? (
          <div className="card">
            <StateMessage kind="error" title="Não foi possível consultar os laboratórios" actionLabel="Tentar de novo" onAction={() => void query.refetch()}>
              {errorMessage(query.error)}
            </StateMessage>
          </div>
        ) : rows.length === 0 ? (
          <div className="card">
            <StateMessage kind="empty" title="Nenhum laboratório atende aos filtros" actionLabel="Restaurar filtros padrão" onAction={restore}>
              Reduza a quantidade mínima ou amplie os períodos.
            </StateMessage>
          </div>
        ) : (
          <>
            <section className="card" aria-labelledby="lab-table">
              <div className="card__pad" style={{ paddingBottom: 0 }}>
                <h2 className="card__title" id="lab-table">
                  Laboratórios
                </h2>
                <p className="page-sub">Na ordem devolvida pela API</p>
              </div>
              <Table aria-labelledby="lab-table">
                <thead>
                  <tr>
                    <th>Nome do laboratório</th>
                    <th className="num">Total de produtores</th>
                    <th className="num">Valor financeiro</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((lab) => (
                    <tr key={lab.id}>
                      <td>{lab.nome}</td>
                      <td className="num">{lab.totalProdutores ?? 0}</td>
                      <td className="num">{formatCurrency(lab.valorFinanceiroConsolidado ?? 0)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <td>{rows.length === 1 ? "Total · 1 laboratório" : `Total · ${rows.length} laboratórios`}</td>
                    <td className="num">{rows.reduce((sum, lab) => sum + (lab.totalProdutores ?? 0), 0)}</td>
                    <td className="num">{formatCurrency(sumCurrency(rows.map((lab) => lab.valorFinanceiroConsolidado ?? 0)))}</td>
                  </tr>
                </tfoot>
              </Table>
            </section>
            <section className="card chart" aria-label="Gráfico">
              <h2 className="card__title">Total de produtores por laboratório</h2>
              <p className="page-sub">Mesma ordem da tabela</p>
              <BarChart title="Total de produtores por laboratório" data={rows.map((lab) => ({ label: lab.nome ?? `Laboratório ${lab.id}`, value: lab.totalProdutores ?? 0 }))} />
            </section>
          </>
        )}
      </div>
    </>
  );
}
