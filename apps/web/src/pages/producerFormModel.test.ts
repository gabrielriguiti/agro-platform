import { describe, expect, it } from "vitest";
import { emptyProducerForm, periodDays, toFormValues, toRequest, validateProducer } from "./producerFormModel";
import type { ProducerFormValues } from "./producerFormModel";
import { producer } from "../test/fixtures";

const valid: ProducerFormValues = {
  nome: "Carlos Eduardo Lima",
  inscricaoFiscal: "529.982.247-25",
  endereco: "Rodovia GO-174, km 12",
  dataInicial: "2026-03-01T08:00",
  dataFinal: "2026-09-30T18:00",
  observacoes: "",
  laboratorioId: "1",
  propriedadeIds: [10, 12],
};

describe("validação do formulário de produtor", () => {
  it("aceita um formulário válido", () => {
    expect(validateProducer(valid)).toEqual({});
  });

  it("exige os campos obrigatórios", () => {
    const errors = validateProducer(emptyProducerForm);
    expect(Object.keys(errors).sort()).toEqual(["dataFinal", "dataInicial", "endereco", "inscricaoFiscal", "laboratorioId", "nome"]);
    expect(errors.nome).toBe("Informe o nome.");
    expect(errors.laboratorioId).toBe("Selecione um laboratório.");
  });

  it("rejeita CPF com dígito verificador errado", () => {
    expect(validateProducer({ ...valid, inscricaoFiscal: "111.222.333-44" }).inscricaoFiscal).toBe("CPF inválido.");
  });

  it("aceita CNPJ válido e rejeita inválido", () => {
    expect(validateProducer({ ...valid, inscricaoFiscal: "11.222.333/0001-81" })).toEqual({});
    expect(validateProducer({ ...valid, inscricaoFiscal: "11.222.333/0001-82" }).inscricaoFiscal).toBe("CNPJ inválido.");
  });

  it("rejeita período com data final anterior à inicial, mas aceita iguais", () => {
    expect(validateProducer({ ...valid, dataFinal: "2026-02-28T18:00" }).dataFinal).toBe("A data final não pode ser anterior à inicial.");
    expect(validateProducer({ ...valid, dataFinal: valid.dataInicial })).toEqual({});
  });

  it("limita nome e endereço ao tamanho do contrato", () => {
    expect(validateProducer({ ...valid, nome: "a".repeat(201) }).nome).toBe("Máximo de 200 caracteres.");
    expect(validateProducer({ ...valid, endereco: "a".repeat(501) }).endereco).toBe("Máximo de 500 caracteres.");
  });
});

describe("conversões do formulário", () => {
  it("monta o corpo da requisição", () => {
    const body = toRequest({ ...valid, observacoes: "  Coleta em duas glebas  " });
    expect(body).toMatchObject({ nome: "Carlos Eduardo Lima", laboratorioId: 1, propriedadeIds: [10, 12], observacoes: "Coleta em duas glebas" });
    expect(new Date(body.dataInicial).getTime()).toBe(new Date("2026-03-01T08:00").getTime());
    expect(toRequest(valid).observacoes).toBeNull();
  });

  it("preenche o formulário a partir do produtor", () => {
    const values = toFormValues(producer(7, "Maria", { observacoes: "Café", propriedades: [{ id: 11, nome: "Fazenda Boa Esperança" }] }));
    expect(values).toMatchObject({ nome: "Maria", laboratorioId: "1", observacoes: "Café", propriedadeIds: [11], dataInicial: "2026-03-01T08:00" });
  });

  it("calcula a duração do período em dias", () => {
    expect(periodDays({ dataInicial: "2026-03-01T08:00", dataFinal: "2026-09-30T18:00" })).toBe(214);
    expect(periodDays({ dataInicial: "2026-03-01T08:00", dataFinal: "2026-03-01T08:00" })).toBe(1);
    expect(periodDays({ dataInicial: "2026-03-02T08:00", dataFinal: "2026-03-01T08:00" })).toBeNull();
    expect(periodDays({ dataInicial: "", dataFinal: "" })).toBeNull();
  });
});
