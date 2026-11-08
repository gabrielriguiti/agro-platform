import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { App } from "../../App";
import { labs, properties } from "../../test/fixtures";
import { json, mockApi, renderApp } from "../../test/render";
import type { FetchCall } from "../../test/render";
import { emptyRegistryForm, validateRegistry } from "./registryModel";

const route = (method: string, path: string, response: (call: FetchCall) => Response) => (call: FetchCall) => (call.method === method && call.path === path ? response(call) : undefined);

describe("modelo do cadastro", () => {
  it("laboratório exige CNPJ; propriedade aceita CPF ou CNPJ", () => {
    const base = { nome: "Fazenda", endereco: "Rio Verde – GO" };
    expect(validateRegistry("laboratory", { ...base, inscricaoFiscal: "529.982.247-25" }).inscricaoFiscal).toMatch(/exige um CNPJ/);
    expect(validateRegistry("property", { ...base, inscricaoFiscal: "529.982.247-25" })).toEqual({});
    expect(validateRegistry("property", { ...base, inscricaoFiscal: "11.222.333/0001-81" })).toEqual({});
    expect(Object.keys(validateRegistry("property", emptyRegistryForm)).sort()).toEqual(["endereco", "inscricaoFiscal", "nome"]);
  });
});

describe("laboratórios: listagem", () => {
  it("lista os laboratórios na ordem da API", async () => {
    mockApi(route("GET", "/api/laboratories", () => json(labs)));
    renderApp(<App />, "/laboratorios");
    const table = await screen.findByRole("table", { name: "Laboratórios" });
    const names = within(table).getAllByRole("row").slice(1).map((row) => within(row).getAllByRole("cell")[0].textContent);
    expect(names).toEqual(["LabSolo Cerrado", "Laboratório Terra Viva"]);
    expect(screen.getByText("2 laboratórios cadastrados")).toBeInTheDocument();
    expect(within(table).getByText("11.222.333/0001-81")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Laboratórios" })).toHaveClass("active");
  });

  it("mostra o vazio com a ação de cadastrar o primeiro", async () => {
    const user = userEvent.setup();
    mockApi(route("GET", "/api/laboratories", () => json([])));
    renderApp(<App />, "/laboratorios");
    expect(await screen.findByText("Nenhum laboratório cadastrado")).toBeInTheDocument();
    await user.click(screen.getAllByRole("button", { name: "Novo laboratório" })[0]);
    expect(await screen.findByRole("heading", { level: 1, name: "Novo laboratório" })).toBeInTheDocument();
  });

  it("mostra o erro e tenta de novo", async () => {
    const user = userEvent.setup();
    let fail = true;
    mockApi(route("GET", "/api/laboratories", () => (fail ? json({ title: "Erro interno", status: 500, detail: "Erro interno do servidor" }, 500) : json(labs))));
    renderApp(<App />, "/laboratorios");
    expect(await screen.findByText("Não foi possível carregar os laboratórios")).toBeInTheDocument();
    fail = false;
    await user.click(screen.getByRole("button", { name: "Tentar de novo" }));
    expect(await screen.findByText("LabSolo Cerrado")).toBeInTheDocument();
  });

  it("exclui depois de confirmar e não exclui ao cancelar", async () => {
    const user = userEvent.setup();
    let list = [...labs];
    const calls = mockApi(
      route("GET", "/api/laboratories", () => json(list)),
      route("DELETE", "/api/laboratories/1", () => {
        list = list.filter((l) => l.id !== 1);
        return new Response(null, { status: 204 });
      }),
    );
    renderApp(<App />, "/laboratorios");

    await user.click(await screen.findByRole("button", { name: "Excluir LabSolo Cerrado" }));
    await user.click(within(screen.getByRole("dialog", { name: "Excluir laboratório?" })).getByRole("button", { name: "Cancelar" }));
    expect(calls.some((c) => c.method === "DELETE")).toBe(false);

    await user.click(screen.getByRole("button", { name: "Excluir LabSolo Cerrado" }));
    expect(screen.getByRole("dialog")).toHaveTextContent("Só é possível excluir laboratórios sem produtores vinculados");
    await user.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Excluir" }));
    await waitFor(() => expect(screen.queryByText("LabSolo Cerrado")).not.toBeInTheDocument());
    expect(screen.getByText("Laboratório excluído")).toBeInTheDocument();
  });

  it("mostra o motivo quando o laboratório tem produtores vinculados (409) e o mantém", async () => {
    const user = userEvent.setup();
    mockApi(
      route("GET", "/api/laboratories", () => json(labs)),
      route("DELETE", "/api/laboratories/1", () => json({ title: "Conflito", status: 409, detail: "Há produtores vinculados" }, 409)),
    );
    renderApp(<App />, "/laboratorios");
    await user.click(await screen.findByRole("button", { name: "Excluir LabSolo Cerrado" }));
    await user.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Excluir" }));
    expect(await screen.findByText("Há produtores vinculados")).toBeInTheDocument();
    expect(screen.getByText("Não foi possível excluir")).toBeInTheDocument();
    expect(screen.getByText("LabSolo Cerrado")).toBeInTheDocument();
  });
});

describe("laboratórios: formulário", () => {
  it("cria o laboratório e volta à lista com a confirmação", async () => {
    const user = userEvent.setup();
    const calls = mockApi(
      route("POST", "/api/laboratories", () => json({ id: 9, nome: "LabSolo Cerrado", inscricaoFiscal: "11.222.333/0001-81", endereco: "Rio Verde – GO" }, 201)),
      route("GET", "/api/laboratories", () => json(labs)),
    );
    renderApp(<App />, "/laboratorios/novo");

    await user.type(screen.getByLabelText(/^Nome/), "LabSolo Cerrado");
    await user.type(screen.getByLabelText(/CNPJ/), "11222333000181");
    expect(screen.getByLabelText(/CNPJ/)).toHaveValue("11.222.333/0001-81");
    await user.type(screen.getByLabelText(/Endereço/), "Rio Verde – GO");
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    expect(await screen.findByRole("heading", { level: 1, name: "Laboratórios" })).toBeInTheDocument();
    expect(screen.getByText("Laboratório salvo")).toBeInTheDocument();
    expect(calls.find((c) => c.method === "POST")?.body).toEqual({ nome: "LabSolo Cerrado", inscricaoFiscal: "11.222.333/0001-81", endereco: "Rio Verde – GO" });
  });

  it("bloqueia CPF no lugar do CNPJ e campos vazios", async () => {
    const user = userEvent.setup();
    const calls = mockApi();
    renderApp(<App />, "/laboratorios/novo");

    await user.click(screen.getByRole("button", { name: "Salvar" }));
    expect(screen.getByLabelText(/^Nome/)).toHaveAccessibleDescription("Informe o nome do laboratório.");
    expect(screen.getByLabelText(/CNPJ/)).toHaveAccessibleDescription(/Informe o CNPJ\./);
    expect(screen.getByLabelText(/Endereço/)).toHaveAccessibleDescription("Informe o endereço.");
    const summary = screen.getByRole("alert");
    expect(summary).toHaveTextContent("Corrija 3 campos para salvar");
    expect(summary).toHaveTextContent("Nome, CNPJ e Endereço são obrigatórios.");
    await user.type(screen.getByLabelText(/^Nome/), "LabSolo");
    await user.type(screen.getByLabelText(/Endereço/), "Rio Verde");
    await user.type(screen.getByLabelText(/CNPJ/), "52998224725");
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    expect(screen.getByLabelText(/CNPJ/)).toHaveAccessibleDescription(/O laboratório exige um CNPJ, não um CPF/);
    expect(calls.some((c) => c.method === "POST")).toBe(false);
  });

  it("mostra o 409 de CNPJ de outro laboratório e mantém os dados", async () => {
    const user = userEvent.setup();
    mockApi(route("POST", "/api/laboratories", () => json({ title: "Conflito", status: 409, detail: "CNPJ pertence a outro laboratório" }, 409)));
    renderApp(<App />, "/laboratorios/novo");

    await user.type(screen.getByLabelText(/^Nome/), "LabSolo");
    await user.type(screen.getByLabelText(/CNPJ/), "11222333000181");
    await user.type(screen.getByLabelText(/Endereço/), "Rio Verde");
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("CNPJ pertence a outro laboratório");
    expect(alert).toHaveTextContent("Os demais dados foram mantidos.");
    expect(alert).toHaveTextContent("HTTP 409");
    expect(screen.getByLabelText(/CNPJ/)).toHaveAccessibleDescription(/CNPJ pertence a outro laboratório/);
    expect(screen.getByLabelText(/^Nome/)).toHaveValue("LabSolo");
    expect(screen.getByRole("button", { name: "Salvar" })).toBeEnabled();
  });

  it("edita um laboratório existente com PUT", async () => {
    const user = userEvent.setup();
    const calls = mockApi(
      route("GET", "/api/laboratories/1", () => json(labs[0])),
      route("PUT", "/api/laboratories/1", () => json(labs[0])),
      route("GET", "/api/laboratories", () => json(labs)),
    );
    renderApp(<App />, "/laboratorios/1/editar");

    expect(await screen.findByDisplayValue("LabSolo Cerrado")).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Editar laboratório · LabSolo Cerrado");
    await user.clear(screen.getByLabelText(/Endereço/));
    await user.type(screen.getByLabelText(/Endereço/), "Jataí – GO");
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    expect(await screen.findByText("Alterações salvas")).toBeInTheDocument();
    expect(calls.find((c) => c.method === "PUT")?.body).toMatchObject({ endereco: "Jataí – GO", nome: "LabSolo Cerrado" });
  });
});

describe("propriedades", () => {
  it("lista com a situação: disponível ou vinculada", async () => {
    mockApi(route("GET", "/api/properties", () => json(properties)));
    renderApp(<App />, "/propriedades");
    const table = await screen.findByRole("table", { name: "Propriedades" });
    const rowOf = (name: string) => within(table).getByText(name).closest("tr") as HTMLElement;
    expect(within(rowOf("Fazenda Santa Rita")).getByText("disponível")).toBeInTheDocument();
    expect(within(rowOf("Fazenda Boa Esperança")).getByText("vinculada")).toBeInTheDocument();
    expect(screen.getByText("3 propriedades cadastradas")).toBeInTheDocument();
  });

  it("avisa na confirmação que excluir desvincula do produtor", async () => {
    const user = userEvent.setup();
    let list = [...properties];
    const calls = mockApi(
      route("GET", "/api/properties", () => json(list)),
      route("DELETE", "/api/properties/11", () => {
        list = list.filter((p) => p.id !== 11);
        return new Response(null, { status: 204 });
      }),
    );
    renderApp(<App />, "/propriedades");
    await user.click(await screen.findByRole("button", { name: "Excluir Fazenda Boa Esperança" }));
    const dialog = screen.getByRole("dialog", { name: "Excluir propriedade?" });
    expect(dialog).toHaveTextContent("ela é desvinculada e o produtor continua existindo");
    await user.click(within(dialog).getByRole("button", { name: "Excluir" }));
    await waitFor(() => expect(screen.queryByText("Fazenda Boa Esperança")).not.toBeInTheDocument());
    expect(screen.getByText("Propriedade excluída")).toBeInTheDocument();
    expect(calls.some((c) => c.method === "DELETE" && c.path === "/api/properties/11")).toBe(true);
  });

  it("mostra vazio e erro de carregamento", async () => {
    mockApi(route("GET", "/api/properties", () => json([])));
    const { unmount } = renderApp(<App />, "/propriedades");
    expect(await screen.findByText("Nenhuma propriedade cadastrada")).toBeInTheDocument();
    unmount();
    mockApi(route("GET", "/api/properties", () => json({ title: "Erro interno", status: 500, detail: "Erro interno do servidor" }, 500)));
    renderApp(<App />, "/propriedades");
    expect(await screen.findByText("Não foi possível carregar as propriedades")).toBeInTheDocument();
  });

  it("cria com CPF ou CNPJ e bloqueia CPF inválido", async () => {
    const user = userEvent.setup();
    const calls = mockApi(
      route("POST", "/api/properties", () => json({ id: 30, nome: "Sítio Novo" }, 201)),
      route("GET", "/api/properties", () => json(properties)),
    );
    renderApp(<App />, "/propriedades/novo");

    await user.type(screen.getByLabelText(/^Nome/), "Sítio Novo");
    await user.type(screen.getByLabelText(/Inscrição fiscal/), "11122233344");
    await user.type(screen.getByLabelText(/Endereço/), "Sorriso – MT");
    await user.click(screen.getByRole("button", { name: "Salvar" }));
    expect(screen.getByLabelText(/Inscrição fiscal/)).toHaveAccessibleDescription(/CPF inválido\./);
    expect(calls.some((c) => c.method === "POST")).toBe(false);

    await user.clear(screen.getByLabelText(/Inscrição fiscal/));
    await user.type(screen.getByLabelText(/Inscrição fiscal/), "52998224725");
    await user.click(screen.getByRole("button", { name: "Salvar" }));

    expect(await screen.findByText("Propriedade salva")).toBeInTheDocument();
    expect(calls.find((c) => c.method === "POST")?.body).toMatchObject({ nome: "Sítio Novo", inscricaoFiscal: "529.982.247-25" });
  });

  it("mostra o 400 do servidor junto ao campo", async () => {
    const user = userEvent.setup();
    mockApi(
      route("POST", "/api/properties", () =>
        json({ title: "Requisição inválida", status: 400, detail: "Há campos inválidos", errors: [{ field: "endereco", message: "Endereço muito longo" }] }, 400),
      ),
    );
    renderApp(<App />, "/propriedades/novo");
    await user.type(screen.getByLabelText(/^Nome/), "Sítio Novo");
    await user.type(screen.getByLabelText(/Inscrição fiscal/), "52998224725");
    await user.type(screen.getByLabelText(/Endereço/), "Sorriso – MT");
    await user.click(screen.getByRole("button", { name: "Salvar" }));
    await waitFor(() => expect(screen.getByLabelText(/Endereço/)).toHaveAccessibleDescription("Endereço muito longo"));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
});

describe("propriedades: paginação, exclusão e vínculo (handoff)", () => {
  const many = Array.from({ length: 23 }, (_, i) => ({ id: 100 + i, nome: `Propriedade ${String(i + 1).padStart(2, "0")}`, inscricaoFiscal: "529.982.247-25", endereco: "Rio Verde – GO", produtorId: i === 1 ? 7 : null }));

  it("pagina a lista de propriedades no cliente, 10 por página", async () => {
    const user = userEvent.setup();
    const calls = mockApi(route("GET", "/api/properties", () => json(many)));
    renderApp(<App />, "/propriedades");

    const table = await screen.findByRole("table", { name: "Propriedades" });
    expect(within(table).getAllByRole("row")).toHaveLength(11);
    expect(screen.getByText("23 propriedades cadastradas")).toBeInTheDocument();
    expect(screen.getByText(/1–10/)).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Próxima" }));
    await user.click(screen.getByRole("button", { name: "Próxima" }));
    expect(within(screen.getByRole("table", { name: "Propriedades" })).getAllByRole("row")).toHaveLength(4);
    expect(screen.getByText(/21–23/)).toBeInTheDocument();
    expect(calls.filter((c) => c.path === "/api/properties")).toHaveLength(1);
  });

  it("a lista de laboratórios não pagina", async () => {
    mockApi(route("GET", "/api/laboratories", () => json(Array.from({ length: 12 }, (_, i) => ({ id: i + 1, nome: `Lab ${i + 1}`, inscricaoFiscal: "11.222.333/0001-81", endereco: "x" })))));
    renderApp(<App />, "/laboratorios");
    const table = await screen.findByRole("table", { name: "Laboratórios" });
    expect(within(table).getAllByRole("row")).toHaveLength(13);
    expect(screen.queryByRole("navigation", { name: "Paginação" })).not.toBeInTheDocument();
  });

  it("mostra a situação e o produtor vinculado na confirmação de exclusão", async () => {
    const user = userEvent.setup();
    mockApi(
      route("GET", "/api/properties", () => json(many)),
      route("GET", "/api/producers/7", () => json({ id: 7, nome: "Antônio Carlos Ferreira" })),
    );
    renderApp(<App />, "/propriedades");

    await user.click(await screen.findByRole("button", { name: "Excluir Propriedade 02" }));
    const dialog = screen.getByRole("dialog", { name: "Excluir propriedade?" });
    expect(within(dialog).getByText("vinculada")).toBeInTheDocument();
    expect(await within(dialog).findByText("Antônio Carlos Ferreira")).toBeInTheDocument();
    await user.click(within(dialog).getByRole("button", { name: "Cancelar" }));

    await user.click(screen.getByRole("button", { name: "Excluir Propriedade 01" }));
    const free = screen.getByRole("dialog");
    expect(within(free).getByText("disponível")).toBeInTheDocument();
    expect(within(free).queryByText("Produtor vinculado")).not.toBeInTheDocument();
  });

  it("na edição, mostra a seção de vínculo com o produtor", async () => {
    mockApi(
      route("GET", "/api/properties/101", () => json(many[1])),
      route("GET", "/api/producers/7", () => json({ id: 7, nome: "Antônio Carlos Ferreira" })),
    );
    renderApp(<App />, "/propriedades/101/editar");
    expect(await screen.findByDisplayValue("Propriedade 02")).toBeInTheDocument();
    expect(await screen.findByText(/Vinculada a Antônio Carlos Ferreira\./)).toBeInTheDocument();
    expect(screen.getByText(/ele é feito no formulário de produtor/)).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 2, name: "Dados da propriedade" })).toBeInTheDocument();
  });

  it("mostra os placeholders, o rodapé de obrigatórios e a mensagem de CPF ou CNPJ", async () => {
    const user = userEvent.setup();
    mockApi();
    renderApp(<App />, "/propriedades/novo");
    expect(screen.getByPlaceholderText("Nome da propriedade")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("000.000.000-00 ou 00.000.000/0000-00")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Logradouro, número, município – UF, CEP")).toBeInTheDocument();
    expect(screen.getByText("Campos obrigatórios")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Salvar" }));
    expect(screen.getByLabelText(/Inscrição fiscal/)).toHaveAccessibleDescription(/Informe o CPF ou CNPJ\./);
    expect(screen.getByRole("alert")).toHaveTextContent("Nome, Inscrição fiscal e Endereço são obrigatórios.");
  });
});

describe("navegação", () => {
  it("tem os itens de Laboratórios e Propriedades e destaca o ativo", async () => {
    mockApi(route("GET", "/api/properties", () => json(properties)));
    renderApp(<App />, "/propriedades/novo");
    const nav = screen.getByRole("navigation", { name: "Principal" });
    expect(within(nav).getAllByRole("link").map((l) => l.textContent)).toEqual(["Produtores", "Novo produtor", "Laboratórios", "Propriedades", "Dashboard", "Integração"]);
    expect(within(nav).getByRole("link", { name: "Propriedades" })).toHaveClass("active");
    expect(within(nav).getByRole("link", { name: "Laboratórios" })).not.toHaveClass("active");
  });
});
