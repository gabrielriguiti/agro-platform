import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { App } from "../App";
import { json, mockApi, renderApp } from "../test/render";
import type { FetchCall } from "../test/render";
import { labs, producer, producerPage, properties } from "../test/fixtures";

/** Respostas comuns às telas de cadastro: laboratórios, propriedades disponíveis e a lista de produtores. */
function registryHandlers(extra: Array<(call: FetchCall) => Response | Promise<Response> | undefined> = []) {
  return [
    ...extra,
    (call: FetchCall) => {
      if (call.method === "GET" && call.path === "/api/laboratories") return json(labs);
      if (call.method === "GET" && call.path === "/api/properties") return json(properties);
      if (call.method === "GET" && call.path === "/api/producers") return json(producerPage([]));
    },
  ];
}

async function fillValid(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/^Nome/), "Carlos Eduardo Lima");
  await user.type(screen.getByLabelText(/Inscrição fiscal/), "52998224725");
  await user.type(screen.getByLabelText(/Endereço/), "Rodovia GO-174, km 12");
  fireEvent.change(screen.getByLabelText(/Data e hora inicial/), { target: { value: "2026-03-01T08:00" } });
  fireEvent.change(screen.getByLabelText(/Data e hora final/), { target: { value: "2026-09-30T18:00" } });
  await user.selectOptions(screen.getByLabelText(/Laboratório/), "LabSolo Cerrado");
}

async function pick(user: ReturnType<typeof userEvent.setup>, names: string[]) {
  await user.click(screen.getByRole("button", { name: /Propriedades/ }));
  for (const name of names) await user.click(await screen.findByRole("checkbox", { name: new RegExp(name) }));
  await user.click(screen.getByRole("button", { name: "Concluir" }));
}

describe("formulário de produtor", () => {
  it("cria o produtor e volta ao grid com a confirmação", async () => {
    const user = userEvent.setup();
    const calls = mockApi(
      ...registryHandlers([
        (call) => (call.method === "POST" && call.path === "/api/producers" ? json(producer(99, "Carlos Eduardo Lima"), 201) : undefined),
      ]),
    );
    renderApp(<App />, "/produtores/novo");

    await fillValid(user);
    await pick(user, ["Fazenda Santa Rita", "Sítio São José"]);
    expect(screen.getByText("2 selecionadas", { selector: "dd" })).toBeInTheDocument();
    expect(screen.getByText("214 dias")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    expect(await screen.findByRole("heading", { level: 1, name: "Produtores" })).toBeInTheDocument();
    expect(screen.getByText("Produtor salvo")).toBeInTheDocument();
    const post = calls.find((c) => c.method === "POST");
    expect(post?.body).toMatchObject({
      nome: "Carlos Eduardo Lima",
      inscricaoFiscal: "529.982.247-25",
      laboratorioId: 1,
      propriedadeIds: [10, 12],
    });
  });

  it("bloqueia o envio e mostra o erro de CPF inválido junto ao campo", async () => {
    const user = userEvent.setup();
    const calls = mockApi(...registryHandlers());
    renderApp(<App />, "/produtores/novo");

    await fillValid(user);
    await user.clear(screen.getByLabelText(/Inscrição fiscal/));
    await user.type(screen.getByLabelText(/Inscrição fiscal/), "11122233344");
    expect(screen.getByLabelText(/Inscrição fiscal/)).toHaveValue("111.222.333-44");
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    expect(screen.getByLabelText(/Inscrição fiscal/)).toHaveAccessibleDescription(/CPF inválido/);
    expect(screen.getByLabelText(/Inscrição fiscal/)).toHaveAttribute("aria-invalid", "true");
    expect(calls.some((c) => c.method === "POST")).toBe(false);
  });

  it("mostra os campos obrigatórios e o período invertido", async () => {
    const user = userEvent.setup();
    const calls = mockApi(...registryHandlers());
    renderApp(<App />, "/produtores/novo");

    await user.click(screen.getByRole("button", { name: "Salvar" }));
    expect(screen.getByLabelText(/^Nome/)).toHaveAccessibleDescription("Informe o nome.");
    expect(screen.getByLabelText(/Laboratório/)).toHaveAccessibleDescription(/Selecione um laboratório/);

    fireEvent.change(screen.getByLabelText(/Data e hora inicial/), { target: { value: "2026-03-01T08:00" } });
    fireEvent.change(screen.getByLabelText(/Data e hora final/), { target: { value: "2026-02-28T08:00" } });
    expect(screen.getByLabelText(/Data e hora final/)).toHaveAccessibleDescription("A data final não pode ser anterior à inicial.");
    expect(calls.some((c) => c.method === "POST")).toBe(false);
  });

  it("edita o conjunto de propriedades: desmarca uma, marca outra e salva", async () => {
    const user = userEvent.setup();
    const calls = mockApi(
      ...registryHandlers([
        (call) => {
          if (call.method === "GET" && call.path === "/api/producers/7") return json(producer(7, "Maria Aparecida Souza"));
          if (call.method === "PUT" && call.path === "/api/producers/7") return json(producer(7, "Maria Aparecida Souza"));
        },
      ]),
    );
    renderApp(<App />, "/produtores/7/editar");

    expect(await screen.findByDisplayValue("Maria Aparecida Souza")).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Editar produtor · Maria Aparecida Souza");
    expect(screen.getByRole("button", { name: /1 selecionada/ })).toBeInTheDocument();
    await pick(user, ["Fazenda Boa Esperança", "Sítio São José"]);
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() => expect(calls.some((c) => c.method === "PUT")).toBe(true));
    expect(calls.find((c) => c.method === "PUT")?.body).toMatchObject({ propriedadeIds: [12], laboratorioId: 1 });
    expect(await screen.findByText("Alterações salvas")).toBeInTheDocument();
    // a edição pede as propriedades livres mais as do produtor
    const listCall = calls.find((c) => c.path === "/api/properties");
    expect(new URLSearchParams(listCall?.search).get("produtorId")).toBe("7");
    expect(new URLSearchParams(listCall?.search).get("disponiveis")).toBe("true");
  });

  it("mostra o 409 de propriedade já vinculada, mantém os dados e recarrega a lista", async () => {
    const user = userEvent.setup();
    const calls = mockApi(
      ...registryHandlers([
        (call) =>
          call.method === "POST" && call.path === "/api/producers"
            ? json({ title: "Conflito", status: 409, detail: "A propriedade Fazenda Santa Rita já pertence a outro produtor" }, 409)
            : undefined,
      ]),
    );
    renderApp(<App />, "/produtores/novo");

    await fillValid(user);
    await pick(user, ["Fazenda Santa Rita"]);
    const before = calls.filter((c) => c.path === "/api/properties").length;
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("A propriedade Fazenda Santa Rita já pertence a outro produtor");
    expect(screen.getByLabelText(/^Nome/)).toHaveValue("Carlos Eduardo Lima");
    expect(screen.getByLabelText(/Inscrição fiscal/)).toHaveValue("529.982.247-25");
    expect(within(screen.getByRole("list", { name: /selecionadas/ })).getByText("Fazenda Santa Rita")).toBeInTheDocument();
    await waitFor(() => expect(calls.filter((c) => c.path === "/api/properties").length).toBeGreaterThan(before));
    expect(screen.getByRole("button", { name: "Salvar" })).toBeEnabled();
  });

  it("marca como indisponível a propriedade que saiu da lista após o 409", async () => {
    const user = userEvent.setup();
    let taken = false;
    mockApi(
      ...registryHandlers([
        (call) => {
          if (call.method === "POST" && call.path === "/api/producers") {
            taken = true;
            return json({ title: "Conflito", status: 409, detail: "Propriedade de outro produtor" }, 409);
          }
          if (call.method === "GET" && call.path === "/api/properties") return json(taken ? properties.filter((p) => p.id !== 10) : properties);
        },
      ]),
    );
    renderApp(<App />, "/produtores/novo");

    await fillValid(user);
    await pick(user, ["Fazenda Santa Rita"]);
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    expect(await screen.findByText(/não estão mais disponíveis/)).toBeInTheDocument();
    expect(within(screen.getByRole("list", { name: /selecionadas/ })).getByText("Fazenda Santa Rita")).toBeInTheDocument();
  });

  it("mostra o 400 do servidor junto ao campo correspondente", async () => {
    const user = userEvent.setup();
    mockApi(
      ...registryHandlers([
        (call) =>
          call.method === "POST" && call.path === "/api/producers"
            ? json({ title: "Requisição inválida", status: 400, detail: "Há campos inválidos", errors: [{ field: "inscricaoFiscal", message: "Inscrição fiscal já cadastrada" }] }, 400)
            : undefined,
      ]),
    );
    renderApp(<App />, "/produtores/novo");

    await fillValid(user);
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() => expect(screen.getByLabelText(/Inscrição fiscal/)).toHaveAccessibleDescription(/Inscrição fiscal já cadastrada/));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.getByLabelText(/^Nome/)).toHaveValue("Carlos Eduardo Lima");
    // editar o campo limpa o erro do servidor
    await user.type(screen.getByLabelText(/Inscrição fiscal/), "0");
    expect(screen.getByLabelText(/Inscrição fiscal/)).not.toHaveAccessibleDescription(/já cadastrada/);
  });

  it("orienta quando não há laboratório nem propriedade (banco vazio) e leva ao cadastro", async () => {
    const user = userEvent.setup();
    mockApi((call) => {
      if (call.method === "GET" && (call.path === "/api/laboratories" || call.path === "/api/properties")) return json([]);
    });
    renderApp(<App />, "/produtores/novo");

    expect(await screen.findByText("Nenhum laboratório cadastrado")).toBeInTheDocument();
    expect(screen.getByText("Nenhuma propriedade disponível")).toBeInTheDocument();
    await user.click(screen.getByRole("link", { name: "cadastre uma propriedade" }));
    expect(await screen.findByRole("heading", { level: 1, name: "Nova propriedade" })).toBeInTheDocument();
  });

  it("não mostra a orientação quando há laboratórios e propriedades", async () => {
    mockApi(...registryHandlers());
    renderApp(<App />, "/produtores/novo");
    await screen.findByRole("option", { name: "LabSolo Cerrado" });
    expect(screen.queryByText("Nenhum laboratório cadastrado")).not.toBeInTheDocument();
    expect(screen.queryByText("Nenhuma propriedade disponível")).not.toBeInTheDocument();
  });

  it("mostra erro quando o produtor em edição não carrega", async () => {
    mockApi(...registryHandlers([(call) => (call.path === "/api/producers/7" ? json({ title: "Recurso não encontrado", status: 404, detail: "Produtor 7 não encontrado" }, 404) : undefined)]));
    renderApp(<App />, "/produtores/7/editar");
    expect(await screen.findByText("Não foi possível carregar o produtor")).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Produtor 7 não encontrado");
  });
});
