import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { App } from "../App";
import { json, mockApi, renderApp } from "../test/render";
import { producer, producerPage } from "../test/fixtures";

const rows = Array.from({ length: 10 }, (_, i) => producer(i + 1, `Produtor ${i + 1}`));

describe("grid de produtores", () => {
  it("lista com paginação e navega entre páginas sem recarregar tudo", async () => {
    const user = userEvent.setup();
    const calls = mockApi((call) => {
      if (call.method === "GET" && call.path === "/api/producers") {
        const page = Number(new URLSearchParams(call.search).get("page") ?? 0);
        return json(page === 0 ? producerPage(rows, 0, 10, 23) : producerPage([producer(11, "Produtor 11")], 1, 10, 23));
      }
    });
    renderApp(<App />, "/produtores");

    const table = await screen.findByRole("table", { name: "Produtores" });
    expect(within(table).getAllByRole("row")).toHaveLength(11);
    expect(screen.getByText("23 produtores cadastrados")).toBeInTheDocument();
    expect(within(table).getByText("Produtor 1")).toBeInTheDocument();
    expect(within(table).getAllByText("LabSolo Cerrado")[0]).toBeInTheDocument();
    expect(within(table).getAllByText("01/03/2026 – 30/09/2026")[0]).toBeInTheDocument();
    expect(screen.getByText(/1–10/)).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Próxima" }));
    expect(await screen.findByText("Produtor 11")).toBeInTheDocument();
    expect(screen.getByText(/11–20|11–23/)).toBeInTheDocument();
    expect(calls.filter((c) => c.path === "/api/producers").map((c) => new URLSearchParams(c.search).get("page"))).toEqual(["0", "1"]);
  });

  it("mostra o carregamento enquanto a lista não chega", () => {
    mockApi(() => new Promise<Response>(() => undefined));
    renderApp(<App />, "/produtores");
    expect(screen.getByRole("table", { name: "Carregando produtores" })).toHaveAttribute("aria-busy", "true");
  });

  it("exclui com confirmação e atualiza a lista", async () => {
    const user = userEvent.setup();
    let list = [producer(1, "Antônio Carlos Ferreira"), producer(2, "Maria Aparecida Souza")];
    const calls = mockApi((call) => {
      if (call.method === "GET" && call.path === "/api/producers") return json(producerPage(list));
      if (call.method === "DELETE" && call.path === "/api/producers/1") {
        list = list.filter((p) => p.id !== 1);
        return new Response(null, { status: 204 });
      }
    });
    renderApp(<App />, "/produtores");

    await user.click(await screen.findByRole("button", { name: "Excluir Antônio Carlos Ferreira" }));
    const dialog = screen.getByRole("dialog", { name: "Excluir produtor?" });
    expect(dialog).toHaveTextContent("Antônio Carlos Ferreira");
    await user.click(within(dialog).getByRole("button", { name: "Excluir" }));

    await waitFor(() => expect(screen.queryByText("Antônio Carlos Ferreira")).not.toBeInTheDocument());
    expect(screen.getByText("Maria Aparecida Souza")).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(calls.some((c) => c.method === "DELETE" && c.path === "/api/producers/1")).toBe(true);
    expect(screen.getByText("Produtor excluído")).toBeInTheDocument();
  });

  it("não remove nada ao cancelar a exclusão", async () => {
    const user = userEvent.setup();
    const calls = mockApi((call) => {
      if (call.method === "GET" && call.path === "/api/producers") return json(producerPage([producer(1, "Antônio Carlos Ferreira")]));
    });
    renderApp(<App />, "/produtores");

    await user.click(await screen.findByRole("button", { name: "Excluir Antônio Carlos Ferreira" }));
    await user.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Cancelar" }));

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.getByText("Antônio Carlos Ferreira")).toBeInTheDocument();
    expect(calls.some((c) => c.method === "DELETE")).toBe(false);
  });

  it("avisa quando a exclusão falha", async () => {
    const user = userEvent.setup();
    mockApi((call) => {
      if (call.method === "GET") return json(producerPage([producer(1, "Antônio Carlos Ferreira")]));
      if (call.method === "DELETE") return json({ title: "Erro interno", status: 500, detail: "Erro interno do servidor" }, 500);
    });
    renderApp(<App />, "/produtores");

    await user.click(await screen.findByRole("button", { name: "Excluir Antônio Carlos Ferreira" }));
    await user.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Excluir" }));

    expect(await screen.findByText("Falha ao excluir")).toBeInTheDocument();
    expect(screen.getByText("Antônio Carlos Ferreira")).toBeInTheDocument();
  });

  it("mostra o estado vazio", async () => {
    mockApi((call) => (call.path === "/api/producers" ? json(producerPage([])) : undefined));
    renderApp(<App />, "/produtores");
    expect(await screen.findByText("Nenhum produtor cadastrado")).toBeInTheDocument();
  });

  it("mostra o erro de rede e tenta de novo", async () => {
    const user = userEvent.setup();
    let fail = true;
    mockApi((call) => {
      if (call.path !== "/api/producers") return undefined;
      if (fail) return Promise.reject(new TypeError("Failed to fetch"));
      return json(producerPage([producer(1, "Antônio Carlos Ferreira")]));
    });
    renderApp(<App />, "/produtores");

    expect(await screen.findByText("Não foi possível carregar os produtores")).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent(/conexão com o servidor falhou/);
    fail = false;
    await user.click(screen.getByRole("button", { name: "Tentar de novo" }));
    expect(await screen.findByText("Antônio Carlos Ferreira")).toBeInTheDocument();
  });
});
