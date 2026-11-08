import { render } from "@testing-library/react";
import type { ReactElement } from "react";
import { MemoryRouter } from "react-router-dom";
import { AppProviders, createQueryClient } from "../AppProviders";

/**
 * Renderiza a árvore com roteador em memória, React Query (sem novas tentativas) e toasts.
 *
 * @param ui elemento a renderizar
 * @param route rota inicial
 */
export function renderApp(ui: ReactElement, route = "/") {
  return render(
    <AppProviders client={createQueryClient(false)}>
      <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
    </AppProviders>,
  );
}

/** Resposta JSON para o `fetch` simulado. */
export function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": status >= 400 ? "application/problem+json" : "application/json" } });
}

/** Chamada registrada pelo `fetch` simulado. */
export interface FetchCall {
  method: string;
  path: string;
  search: string;
  body: unknown;
}

type Handler = (call: FetchCall) => Response | Promise<Response> | undefined;

/**
 * Substitui o `fetch` global por uma função que roteia as chamadas pelos manipuladores, na ordem.
 * Chamadas sem manipulador respondem 404.
 *
 * @returns a lista de chamadas feitas, para asserções
 */
export function mockApi(...handlers: Handler[]): FetchCall[] {
  const calls: FetchCall[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: Request) => {
      const url = new URL(input.url);
      const text = await input.clone().text();
      const call: FetchCall = { method: input.method, path: url.pathname, search: url.search, body: text ? JSON.parse(text) : undefined };
      calls.push(call);
      for (const handler of handlers) {
        const response = await handler(call);
        if (response) return response;
      }
      return json({ title: "Recurso não encontrado", status: 404, detail: `sem manipulador para ${call.method} ${call.path}` }, 404);
    }),
  );
  return calls;
}
