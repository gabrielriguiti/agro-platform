import { describe, expect, it } from "vitest";
import {
  endOfDayIso,
  formatCurrency,
  formatDate,
  formatPeriod,
  fromDateTimeLocal,
  startOfDayIso,
  toDateTimeLocal,
} from "./format";
import { isValidCnpj, isValidCpf, maskTaxId, validateCnpj, validateTaxId } from "./taxId";

describe("inscrição fiscal", () => {
  it("aplica a máscara de CPF e de CNPJ enquanto digita", () => {
    expect(maskTaxId("5299")).toBe("529.9");
    expect(maskTaxId("52998224725")).toBe("529.982.247-25");
    expect(maskTaxId("11222333000181")).toBe("11.222.333/0001-81");
    expect(maskTaxId("112223330001819999")).toBe("11.222.333/0001-81");
    expect(maskTaxId("abc")).toBe("");
  });

  it("valida os dígitos verificadores do CPF", () => {
    expect(isValidCpf("52998224725")).toBe(true);
    expect(isValidCpf("52998224726")).toBe(false);
    expect(isValidCpf("11111111111")).toBe(false);
  });

  it("valida os dígitos verificadores do CNPJ", () => {
    expect(isValidCnpj("11222333000181")).toBe(true);
    expect(isValidCnpj("11222333000182")).toBe(false);
    expect(isValidCnpj("00000000000000")).toBe(false);
  });

  it("devolve a mensagem de erro de cada caso", () => {
    expect(validateTaxId("")).toBe("Informe a inscrição fiscal.");
    expect(validateTaxId("529.982")).toBe("CPF incompleto.");
    expect(validateTaxId("111.222.333-44")).toBe("CPF inválido.");
    expect(validateTaxId("529.982.247-25")).toBeNull();
    expect(validateTaxId("11.222.333/0001")).toBe("CNPJ incompleto.");
    expect(validateTaxId("11.222.333/0001-82")).toBe("CNPJ inválido.");
    expect(validateTaxId("11.222.333/0001-81")).toBeNull();
  });
});

describe("inscrição fiscal de laboratório (só CNPJ)", () => {
  it("rejeita CPF, incompleto e inválido; aceita CNPJ válido", () => {
    expect(validateCnpj("")).toBe("Informe a inscrição fiscal.");
    expect(validateCnpj("529.982.247-25")).toBe("O laboratório exige um CNPJ, não um CPF.");
    expect(validateCnpj("11.222.333/0001")).toBe("CNPJ incompleto.");
    expect(validateCnpj("111.222.333-44")).toBe("CNPJ incompleto.");
    expect(validateCnpj("11.222.333/0001-82")).toBe("CNPJ inválido.");
    expect(validateCnpj("11.222.333/0001-81")).toBeNull();
  });
});

describe("formatação", () => {
  it("formata datas e período", () => {
    const start = new Date(2026, 2, 1, 8, 0).toISOString();
    const end = new Date(2026, 8, 30, 18, 0).toISOString();
    expect(formatDate(start)).toBe("01/03/2026");
    expect(formatPeriod(start, end)).toBe("01/03/2026 – 30/09/2026");
  });

  it("formata valores em reais", () => {
    expect(formatCurrency(184320).replace(/\s/g, " ")).toBe("R$ 184.320,00");
    expect(formatCurrency(162750.5).replace(/\s/g, " ")).toBe("R$ 162.750,50");
  });

  it("converte datetime-local de ida e volta", () => {
    const iso = fromDateTimeLocal("2026-03-01T08:00");
    expect(toDateTimeLocal(iso)).toBe("2026-03-01T08:00");
  });

  it("converte um dia em início e fim do dia", () => {
    expect(toDateTimeLocal(startOfDayIso("2026-01-01"))).toBe("2026-01-01T00:00");
    expect(toDateTimeLocal(endOfDayIso("2026-10-31"))).toBe("2026-10-31T23:59");
  });
});
