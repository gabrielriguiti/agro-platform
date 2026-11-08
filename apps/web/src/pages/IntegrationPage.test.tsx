import type { Batch, IntegrationMessage } from "@agro/api-client";
import { act, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { App } from "../App";
import { json, mockApi, renderApp } from "../test/render";
import type { FetchCall } from "../test/render";

function batch(id: number, overrides: Partial<Batch> = {}): Batch {
  return { id, source: "ERP Cooperativa Rio Pardo", mergeStrategy: "MERGE", createdAt: new Date(2026, 10, 6, 14, 32).toISOString(), total: 60, pending: 0, processing: 0, done: 60, dead: 0, state: "CONCLUIDO", ...overrides };
}

function message(id: number, overrides: Partial<IntegrationMessage> = {}): IntegrationMessage {
  return { id, batchId: 2, type: "PRODUCER", externalId: `PAP-PROD-${id}`, status: "DONE", attempts: 1, lastError: "", ...overrides };
}

const deadMessages = [
  message(1, { externalId: "PAP-PROD-00901" }),
  message(2, { externalId: "PAP-PROD-00902", status: "DEAD", attempts: 5, lastError: "Inscrição fiscal inválida: 111.222.333-44", payload: '{"nome":"João","inscricaoFiscal":"111.222.333-44"}' }),
  message(3, { externalId: "PAP-LAB-0032", type: "LABORATORY", status: "DEAD", attempts: 5, lastError: "Tempo esgotado ao gravar laboratório (30 s)" }),
];

function page(content: IntegrationMessage[], totalElements = content.length) {
  return { content, page: 0, size: 10, totalElements, totalPages: Math.max(1, Math.ceil(totalElements / 10)) };
}

const route = (method: string, path: string, response: () => Response) => (call: FetchCall) => (call.method === method && call.path === path ? response() : undefined);

describe("lista de lotes", () => {
  it("lista os lotes e se atualiza sozinha a cada 5 s até concluir", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    let current = batch(3, { total: 60, pending: 12, done: 48, state: "EM_ANDAMENTO" });
    const calls = mockApi(route("GET", "/api/integration/batches", () => json([current])));
    renderApp(<App />, "/integracao");

    const table = await screen.findByRole("table", { name: "Lotes" });
    const row = within(table).getByText("Lote #3").closest("tr") as HTMLElement;
    expect(within(row).getByText("ERP Cooperativa Rio Pardo")).toBeInTheDocument();
    expect(within(row).getByText(/em andamento/)).toBeInTheDocument();
    expect(within(row).getByText(/a cada 5 s/)).toBeInTheDocument();
    expect(within(row).getAllByRole("cell")[4]).toHaveTextContent("12");
    expect(within(row).getAllByRole("cell")[6]).toHaveTextContent("48");
    const first = calls.length;

    current = batch(3, { pending: 0, done: 60, state: "CONCLUIDO" });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5100);
    });

    expect(calls.length).toBeGreaterThan(first);
    await waitFor(() => expect(within(screen.getByRole("table", { name: "Lotes" })).getByText("concluído")).toBeInTheDocument());
    const updated = within(screen.getByRole("table", { name: "Lotes" })).getByText("Lote #3").closest("tr") as HTMLElement;
    expect(within(updated).getAllByRole("cell")[4]).toHaveTextContent("0");
    expect(within(updated).getAllByRole("cell")[6]).toHaveTextContent("60");
  });

  it("mostra os três estados de lote", async () => {
    mockApi(
      route("GET", "/api/integration/batches", () =>
        json([batch(3, { pending: 5, done: 55, state: "EM_ANDAMENTO" }), batch(2, { done: 57, dead: 3, state: "CONCLUIDO_COM_FALHAS" }), batch(1)]),
      ),
    );
    renderApp(<App />, "/integracao");
    const table = await screen.findByRole("table", { name: "Lotes" });
    expect(within(table).getByText("concluído com falhas")).toBeInTheDocument();
    expect(within(table).getByText("concluído")).toBeInTheDocument();
    expect(within(table).getByText(/em andamento/)).toBeInTheDocument();
    expect(screen.getByText("3 lotes recebidos")).toBeInTheDocument();
  });

  it("mostra vazio e leva ao envio do lote de exemplo", async () => {
    const user = userEvent.setup();
    mockApi(route("GET", "/api/integration/batches", () => json([])));
    renderApp(<App />, "/integracao");
    expect(await screen.findByText("Nenhum lote recebido")).toBeInTheDocument();
    await user.click(screen.getAllByRole("button", { name: "Enviar lote de exemplo" })[0]);
    expect(await screen.findByRole("heading", { level: 1, name: "Enviar lote de exemplo" })).toBeInTheDocument();
  });

  it("mostra o erro e tenta de novo", async () => {
    const user = userEvent.setup();
    let fail = true;
    mockApi(route("GET", "/api/integration/batches", () => (fail ? json({ title: "Erro interno", status: 500, detail: "O serviço de integração não respondeu." }, 500) : json([batch(1)]))));
    renderApp(<App />, "/integracao");
    expect(await screen.findByText("Não foi possível carregar os lotes")).toBeInTheDocument();
    fail = false;
    await user.click(screen.getByRole("button", { name: "Tentar de novo" }));
    expect(await screen.findByText("Lote #1")).toBeInTheDocument();
  });
});

describe("mensagens e DLQ", () => {
  const withBatch = (extra: Array<(call: FetchCall) => Response | undefined> = []) =>
    mockApi(
      ...extra,
      route("GET", "/api/integration/batches/2", () => json(batch(2, { total: 120, done: 117, dead: 3, state: "CONCLUIDO_COM_FALHAS" }))),
      (call) => {
        if (call.method !== "GET" || call.path !== "/api/integration/messages") return undefined;
        return json(new URLSearchParams(call.search).get("status") === "DEAD" ? page(deadMessages.slice(1)) : page(deadMessages, 120));
      },
      (call) => {
        const m = /^\/api\/integration\/messages\/(\d+)$/.exec(call.path);
        return call.method === "GET" && m ? json(deadMessages.find((x) => x.id === Number(m[1]))) : undefined;
      },
    );

  it("lista as mensagens do lote com destaque para DEAD", async () => {
    withBatch();
    renderApp(<App />, "/integracao/lotes/2");

    const table = await screen.findByRole("table", { name: "Mensagens" });
    expect(screen.getByRole("heading", { level: 1, name: "Lote #2" })).toBeInTheDocument();
    const rows = within(table).getAllByRole("row").slice(1);
    expect(rows).toHaveLength(3);
    expect(rows[1]).toHaveClass("row--dead");
    expect(rows[0]).not.toHaveClass("row--dead");
    expect(within(rows[1]).getByText("DEAD")).toHaveClass("badge--solid-err");
    expect(within(rows[1]).getByText("Inscrição fiscal inválida: 111.222.333-44")).toBeInTheDocument();
    expect(within(rows[1]).getAllByRole("cell")[3]).toHaveTextContent("5");
    expect(screen.getByRole("button", { name: "Reprocessar mortas (3)" })).toBeEnabled();
  });

  it("filtra por status pedindo só as mensagens daquele status", async () => {
    const user = userEvent.setup();
    const calls = withBatch();
    renderApp(<App />, "/integracao/lotes/2");
    await screen.findByRole("table", { name: "Mensagens" });

    await user.click(screen.getByRole("button", { name: /^DEAD/ }));

    await waitFor(() => expect(within(screen.getByRole("table", { name: "Mensagens" })).getAllByRole("row")).toHaveLength(3));
    const last = calls.filter((c) => c.path === "/api/integration/messages").at(-1) as FetchCall;
    expect(new URLSearchParams(last.search).get("status")).toBe("DEAD");
    expect(new URLSearchParams(last.search).get("batchId")).toBe("2");
    expect(screen.getByRole("button", { name: /^DEAD/ })).toHaveAttribute("aria-pressed", "true");
  });

  it("abre uma mensagem DEAD e mostra o motivo, as tentativas e o payload original", async () => {
    const user = userEvent.setup();
    withBatch();
    renderApp(<App />, "/integracao/lotes/2");
    await screen.findByRole("table", { name: "Mensagens" });

    await user.click(screen.getByRole("button", { name: "Ver detalhes de PAP-PROD-00902" }));

    const dialog = await screen.findByRole("dialog", { name: "Mensagem PAP-PROD-00902" });
    expect(await within(dialog).findByText("Motivo da falha")).toBeInTheDocument();
    expect(within(dialog).getByRole("alert")).toHaveTextContent("Inscrição fiscal inválida: 111.222.333-44");
    expect(within(dialog).getByText("5")).toBeInTheDocument();
    expect(within(dialog).getByText(/"inscricaoFiscal": "111.222.333-44"/)).toBeInTheDocument();
    await user.click(within(dialog).getByRole("button", { name: "Fechar" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("reprocessa as mortas do lote depois de confirmar", async () => {
    const user = userEvent.setup();
    let requeued = false;
    const calls = withBatch([
      route("POST", "/api/integration/batches/2/reprocess", () => {
        requeued = true;
        return json({ requeued: 3 });
      }),
      (call) =>
        requeued && call.method === "GET" && call.path === "/api/integration/batches/2"
          ? json(batch(2, { total: 120, done: 117, pending: 3, dead: 0, state: "EM_ANDAMENTO" }))
          : undefined,
    ]);
    renderApp(<App />, "/integracao/lotes/2");
    await screen.findByRole("table", { name: "Mensagens" });

    await user.click(screen.getByRole("button", { name: "Reprocessar mortas (3)" }));
    const dialog = screen.getByRole("dialog", { name: "Reprocessar 3 mensagens mortas?" });
    expect(dialog).toHaveTextContent("As mensagens voltam para PENDING");
    await user.click(within(dialog).getByRole("button", { name: "Reprocessar mortas" }));

    expect(await screen.findByText("3 mensagens voltaram a PENDING.")).toBeInTheDocument();
    expect(calls.some((c) => c.method === "POST" && c.path === "/api/integration/batches/2/reprocess")).toBe(true);
    await waitFor(() => expect(screen.getByRole("button", { name: /Reprocessar mortas \(0\)/ })).toBeDisabled());
    expect(screen.getByText(/em andamento/)).toBeInTheDocument();
  });

  it("reprocessa uma única mensagem e avisa quando o servidor recusa", async () => {
    const user = userEvent.setup();
    const calls = withBatch([
      route("POST", "/api/integration/messages/2/reprocess", () => json(message(2, { status: "PENDING", attempts: 0 }))),
      route("POST", "/api/integration/messages/3/reprocess", () => json({ title: "Conflito", status: 409, detail: "A mensagem não está em DEAD" }, 409)),
    ]);
    renderApp(<App />, "/integracao/lotes/2");
    await screen.findByRole("table", { name: "Mensagens" });

    await user.click(screen.getByRole("button", { name: "Reprocessar PAP-PROD-00902" }));
    expect(await screen.findByText("Mensagem reprocessada")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Reprocessar PAP-LAB-0032" }));
    expect(await screen.findByText("A mensagem não está em DEAD")).toBeInTheDocument();
    expect(calls.filter((c) => c.method === "POST")).toHaveLength(2);
  });

  it("não oferece reprocessar quando não há mensagens mortas", async () => {
    mockApi(
      route("GET", "/api/integration/batches/1", () => json(batch(1))),
      route("GET", "/api/integration/messages", () => json(page([message(1)]))),
    );
    renderApp(<App />, "/integracao/lotes/1");
    await screen.findByRole("table", { name: "Mensagens" });
    expect(screen.getByRole("button", { name: "Reprocessar mortas (0)" })).toBeDisabled();
  });
});

describe("enviar lote de exemplo", () => {
  it("carrega o exemplo, envia o lote e abre o lote criado", async () => {
    const user = userEvent.setup();
    const calls = mockApi(
      route("POST", "/api/integration/batches", () => json({ batchId: 9, accepted: 4, duplicates: 0 }, 202)),
      route("GET", "/api/integration/batches/9", () => json(batch(9, { total: 4, pending: 4, done: 0, state: "EM_ANDAMENTO" }))),
      route("GET", "/api/integration/messages", () => json(page([message(1, { batchId: 9, status: "PENDING" })]))),
    );
    renderApp(<App />, "/integracao/enviar");

    const editor = screen.getByLabelText(/JSON do lote/);
    expect(editor).toHaveValue("");
    await user.click(screen.getByRole("button", { name: "Carregar exemplo" }));
    expect((editor as HTMLTextAreaElement).value).toContain("PROD-INVALIDO-");
    expect(JSON.parse((editor as HTMLTextAreaElement).value).items).toHaveLength(4);
    await user.click(screen.getByRole("button", { name: "Enviar lote" }));

    expect(await screen.findByRole("heading", { level: 1, name: "Lote #9" })).toBeInTheDocument();
    expect(screen.getByText(/Lote #9 entrou na fila: 4 itens aceitos/)).toBeInTheDocument();
    const post = calls.find((c) => c.method === "POST");
    expect(post?.body).toMatchObject({ source: "erp-demo", mergeStrategy: "MERGE" });
    expect((post?.body as { items: unknown[] }).items).toHaveLength(4);
  });

  it("não envia JSON inválido e mostra o erro junto ao campo", async () => {
    const user = userEvent.setup();
    const calls = mockApi();
    renderApp(<App />, "/integracao/enviar");

    await user.click(screen.getByRole("button", { name: "Enviar lote" }));
    expect(screen.getByLabelText(/JSON do lote/)).toHaveAccessibleDescription("Cole ou carregue um JSON de lote.");

    await user.click(screen.getByLabelText(/JSON do lote/));
    await user.paste("{ nope");
    await user.click(screen.getByRole("button", { name: "Enviar lote" }));
    expect(screen.getByLabelText(/JSON do lote/)).toHaveAccessibleDescription(/^JSON inválido/);
    expect(calls).toHaveLength(0);
  });

  it("mostra a recusa do servidor e mantém o texto", async () => {
    const user = userEvent.setup();
    mockApi(route("POST", "/api/integration/batches", () => json({ title: "Lote grande demais", status: 413, detail: "O lote excede 1000 itens" }, 413)));
    renderApp(<App />, "/integracao/enviar");

    await user.click(screen.getByRole("button", { name: "Carregar exemplo" }));
    await user.click(screen.getByRole("button", { name: "Enviar lote" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("O lote excede 1000 itens");
    expect((screen.getByLabelText(/JSON do lote/) as HTMLTextAreaElement).value).toContain("erp-demo");
  });
});
