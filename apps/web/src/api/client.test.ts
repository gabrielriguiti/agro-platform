import { afterEach, describe, expect, it, vi } from "vitest";
import { api, unwrap, unwrapEmpty } from "./client";
import { ApiError, errorMessage, problemToError } from "./errors";

function stubFetch(response: Response | Error) {
  vi.stubGlobal(
    "fetch",
    vi.fn(async () => {
      if (response instanceof Error) throw response;
      return response;
    }),
  );
}

function problem(status: number, body: object) {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/problem+json" } });
}

afterEach(() => vi.unstubAllGlobals());

describe("tratamento de ProblemDetail", () => {
  it("devolve o corpo de sucesso", async () => {
    stubFetch(Response.json({ name: "agro-api" }));
    await expect(unwrap(api.GET("/api/info"))).resolves.toEqual({ name: "agro-api" });
  });

  it("converte 400 em ApiError com os campos inválidos", async () => {
    stubFetch(
      problem(400, {
        title: "Requisição inválida",
        status: 400,
        detail: "Há campos inválidos na requisição",
        errors: [{ field: "inscricaoFiscal", message: "CPF inválido" }],
      }),
    );
    const error = await unwrap(api.GET("/api/info")).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(400);
    expect(apiError.message).toBe("Há campos inválidos na requisição");
    expect(apiError.fieldErrors).toEqual([{ field: "inscricaoFiscal", message: "CPF inválido" }]);
  });

  it("converte 409 usando o detail do problema", async () => {
    stubFetch(problem(409, { title: "Conflito", status: 409, detail: "Propriedade já vinculada a outro produtor" }));
    const error = (await unwrap(api.GET("/api/info")).catch((e: unknown) => e)) as ApiError;
    expect(error.status).toBe(409);
    expect(error.title).toBe("Conflito");
    expect(error.message).toBe("Propriedade já vinculada a outro produtor");
    expect(error.fieldErrors).toEqual([]);
  });

  it("converte falha de rede em ApiError com status 0", async () => {
    stubFetch(new TypeError("Failed to fetch"));
    const error = (await unwrap(api.GET("/api/info")).catch((e: unknown) => e)) as ApiError;
    expect(error).toBeInstanceOf(ApiError);
    expect(error.isNetwork).toBe(true);
    expect(error.message).toMatch(/conexão com o servidor falhou/);
  });

  it("aceita resposta sem corpo (204)", async () => {
    stubFetch(new Response(null, { status: 204 }));
    await expect(unwrapEmpty(api.DELETE("/api/producers/{id}", { params: { path: { id: 1 } } }))).resolves.toBeUndefined();
  });

  it("usa texto simples e mensagem padrão quando não há ProblemDetail", () => {
    expect(problemToError(502, "Bad Gateway").message).toBe("Bad Gateway");
    expect(problemToError(500, undefined).message).toMatch(/erro/);
    expect(problemToError(404, undefined).message).toBe("Erro 404");
  });

  it("ignora itens malformados em errors", () => {
    const error = problemToError(400, { errors: [{ field: "nome" }, { field: "endereco", message: "Obrigatório" }, 3] });
    expect(error.fieldErrors).toEqual([{ field: "endereco", message: "Obrigatório" }]);
  });

  it("errorMessage cobre ApiError, Error e valores desconhecidos", () => {
    expect(errorMessage(new ApiError(409, "Conflito"))).toBe("Conflito");
    expect(errorMessage(new Error("falhou"))).toBe("falhou");
    expect(errorMessage("x")).toMatch(/Algo deu errado/);
  });
});
