import { screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "./App";
import { json, mockApi, renderApp } from "./test/render";

describe("App", () => {
  it.each([
    ["/produtores", "Produtores"],
    ["/produtores/novo", "Novo produtor"],
    ["/analise", "Dashboard de laboratórios"],
    ["/integracao", "Monitor de integração"],
    ["/integracao/enviar", "Enviar lote de exemplo"],
  ])("renderiza a rota %s", (rota, titulo) => {
    mockApi(() => json([]));
    renderApp(<App />, rota);
    expect(screen.getByRole("heading", { level: 1, name: titulo })).toBeInTheDocument();
  });

  it("redireciona a raiz e rotas desconhecidas para os produtores", () => {
    mockApi(() => json({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 1 }));
    renderApp(<App />, "/qualquer-coisa");
    expect(screen.getByRole("heading", { level: 1, name: "Produtores" })).toBeInTheDocument();
  });

  it("destaca o item ativo da navegação", () => {
    mockApi(() => json([]));
    renderApp(<App />, "/produtores/novo");
    const nav = screen.getByRole("navigation", { name: "Principal" });
    expect(within(nav).getByRole("link", { name: "Novo produtor" })).toHaveClass("active");
    expect(within(nav).getByRole("link", { name: "Produtores" })).not.toHaveClass("active");
    expect(nav).toHaveTextContent("Dashboard");
    expect(nav).toHaveTextContent("Integração");
  });
});
