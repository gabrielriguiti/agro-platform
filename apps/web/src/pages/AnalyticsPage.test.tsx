import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { App } from "../App";
import { niceTicks } from "../components/BarChart";
import { json, mockApi, renderApp } from "../test/render";
import { defaultFilters, sumCurrency, toParams, validateFilters } from "./analyticsModel";

const result = [
  { id: 3, nome: "Análise Agro Paraná", totalProdutores: 9, valorFinanceiroConsolidado: 184320 },
  { id: 1, nome: "LabSolo Cerrado", totalProdutores: 8, valorFinanceiroConsolidado: 162750.5 },
  { id: 2, nome: "Laboratório Terra Viva", totalProdutores: 2, valorFinanceiroConsolidado: 97215.8 },
];

describe("dashboard de laboratórios", () => {
  it("só consulta com o botão e envia os filtros válidos", async () => {
    const user = userEvent.setup();
    const calls = mockApi((call) => (call.path === "/api/laboratories/analytics" ? json(result) : undefined));
    renderApp(<App />, "/analise");

    expect(screen.getByRole("heading", { level: 1, name: "Dashboard de laboratórios" })).toBeInTheDocument();
    expect(screen.getByText("Defina os filtros e consulte")).toBeInTheDocument();
    expect(calls).toHaveLength(0);

    await user.clear(screen.getByLabelText(/Qtd. mínima/));
    await user.type(screen.getByLabelText(/Qtd. mínima/), "2");
    await user.type(screen.getByLabelText("Busca nas observações"), "soja");
    await user.click(screen.getByRole("button", { name: "Consultar" }));

    await screen.findByRole("table", { name: "Laboratórios" });
    const query = new URLSearchParams(calls[0].search);
    expect(query.get("qtdMinimaProdutores")).toBe("2");
    expect(query.get("buscaTexto")).toBe("soja");
    expect(query.has("dataInicialInicio")).toBe(false);
  });

  it("desabilita Consultar e indica a obrigatoriedade quando a quantidade mínima está vazia", async () => {
    const user = userEvent.setup();
    const calls = mockApi();
    renderApp(<App />, "/analise");

    await user.clear(screen.getByLabelText(/Qtd. mínima/));

    expect(screen.getByRole("button", { name: "Consultar" })).toBeDisabled();
    expect(screen.getByLabelText(/Qtd. mínima/)).toHaveAccessibleDescription("Informe a quantidade mínima de produtores.");
    expect(screen.getByLabelText(/Qtd. mínima/)).toHaveAttribute("aria-invalid", "true");
    expect(calls).toHaveLength(0);
  });

  it("desabilita Consultar com intervalo de datas invertido", async () => {
    const user = userEvent.setup();
    renderApp(<App />, "/analise");
    await user.type(screen.getAllByLabelText("De")[0], "2026-10-31");
    await user.type(screen.getAllByLabelText("Até")[0], "2026-01-01");
    expect(screen.getByRole("button", { name: "Consultar" })).toBeDisabled();
    expect(screen.getByText("O fim do intervalo não pode ser anterior ao início.")).toBeInTheDocument();
  });

  it("mostra tabela e gráfico com os laboratórios na mesma ordem da API", async () => {
    const user = userEvent.setup();
    mockApi((call) => (call.path === "/api/laboratories/analytics" ? json(result) : undefined));
    renderApp(<App />, "/analise");

    await user.click(screen.getByRole("button", { name: "Consultar" }));

    const table = await screen.findByRole("table", { name: "Laboratórios" });
    const names = within(table)
      .getAllByRole("row")
      .slice(1, 4)
      .map((row) => within(row).getAllByRole("cell")[0].textContent);
    expect(names).toEqual(["Análise Agro Paraná", "LabSolo Cerrado", "Laboratório Terra Viva"]);
    expect(within(table).getByText(/R\$\s184\.320,00/)).toBeInTheDocument();
    expect(within(table).getByText("Total · 3 laboratórios")).toBeInTheDocument();
    expect(within(table).getByText(/R\$\s444\.286,30/)).toBeInTheDocument();

    const chart = screen.getByRole("img", { name: "Total de produtores por laboratório" });
    const bars = within(chart).getAllByTestId("bar");
    expect(bars.map((bar) => bar.querySelector("text")?.textContent)).toEqual(names);
    expect(bars.map((bar) => bar.querySelectorAll("text")[1]?.textContent)).toEqual(["9", "8", "2"]);
    const widths = bars.map((bar) => Number(bar.querySelector("rect")?.getAttribute("width")));
    expect(widths[0]).toBeGreaterThan(widths[1]);
    expect(widths[1]).toBeGreaterThan(widths[2]);
  });

  it("mostra a mensagem de vazio e restaura os filtros padrão", async () => {
    const user = userEvent.setup();
    let empty = true;
    const calls = mockApi((call) => (call.path === "/api/laboratories/analytics" ? json(empty ? [] : result) : undefined));
    renderApp(<App />, "/analise");

    await user.clear(screen.getByLabelText(/Qtd. mínima/));
    await user.type(screen.getByLabelText(/Qtd. mínima/), "20");
    await user.click(screen.getByRole("button", { name: "Consultar" }));
    expect(await screen.findByText("Nenhum laboratório atende aos filtros")).toBeInTheDocument();

    empty = false;
    await user.click(screen.getByRole("button", { name: "Restaurar filtros padrão" }));
    await screen.findByRole("table", { name: "Laboratórios" });
    expect(screen.getByLabelText(/Qtd. mínima/)).toHaveValue("1");
    expect(new URLSearchParams(calls[calls.length - 1].search).get("qtdMinimaProdutores")).toBe("1");
  });

  it("mostra o erro da API e tenta de novo", async () => {
    const user = userEvent.setup();
    let fail = true;
    mockApi((call) => {
      if (call.path !== "/api/laboratories/analytics") return undefined;
      return fail ? json({ title: "Requisição inválida", status: 400, detail: "Intervalo invertido" }, 400) : json(result);
    });
    renderApp(<App />, "/analise");

    await user.click(screen.getByRole("button", { name: "Consultar" }));
    expect(await screen.findByText("Não foi possível consultar os laboratórios")).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Intervalo invertido");

    fail = false;
    await user.click(screen.getByRole("button", { name: "Tentar de novo" }));
    expect(await screen.findByRole("table", { name: "Laboratórios" })).toBeInTheDocument();
  });
});

describe("modelo dos filtros", () => {
  it("valida a quantidade mínima", () => {
    expect(validateFilters(defaultFilters)).toEqual({});
    expect(validateFilters({ ...defaultFilters, qtdMinima: "" }).qtdMinima).toMatch(/Informe/);
    expect(validateFilters({ ...defaultFilters, qtdMinima: "0" }).qtdMinima).toMatch(/inteiro/);
    expect(validateFilters({ ...defaultFilters, qtdMinima: "1.5" }).qtdMinima).toMatch(/inteiro/);
  });

  it("converte os filtros em parâmetros da API", () => {
    const params = toParams({ ...defaultFilters, qtdMinima: " 3 ", busca: " soja ", dataInicialDe: "2026-01-01", dataFinalAte: "2026-10-31" });
    expect(params.qtdMinimaProdutores).toBe(3);
    expect(params.buscaTexto).toBe("soja");
    expect(new Date(params.dataInicialInicio as string).getTime()).toBe(new Date("2026-01-01T00:00:00").getTime());
    expect(new Date(params.dataFinalFim as string).getTime()).toBe(new Date("2026-10-31T23:59:59.999").getTime());
    expect(params.dataInicialFim).toBeUndefined();
  });

  it("soma valores em reais sem erro de ponto flutuante", () => {
    expect(sumCurrency([0.1, 0.2])).toBe(0.3);
    expect(sumCurrency([184320, 162750.5, 97215.8])).toBe(444286.3);
  });

  it("calcula as marcas do eixo do gráfico", () => {
    expect(niceTicks(9)).toEqual([0, 2, 4, 6, 8, 10]);
    expect(niceTicks(0)).toEqual([0, 1]);
  });
});
