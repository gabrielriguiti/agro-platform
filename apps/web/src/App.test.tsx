import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { App } from "./App";

describe("App", () => {
  it.each([
    ["/produtores", "Produtores"],
    ["/produtores/novo", "Novo produtor"],
    ["/analise", "Análise de laboratórios"],
    ["/integracao", "Integração"],
  ])("renderiza a rota %s", (rota, titulo) => {
    render(
      <MemoryRouter initialEntries={[rota]}>
        <App />
      </MemoryRouter>,
    );
    expect(screen.getByRole("heading", { level: 1, name: titulo })).toBeInTheDocument();
  });
});
