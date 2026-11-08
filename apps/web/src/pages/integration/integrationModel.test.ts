import { describe, expect, it } from "vitest";
import { isValidCnpj, isValidCpf, onlyDigits } from "../../lib/taxId";
import { batchStateLabel, buildSampleBatch, messageTypeLabel, parseBatch, prettyPayload, sampleBatchJson } from "./integrationModel";

describe("modelo do monitor de integração", () => {
  it("traduz os estados e os tipos", () => {
    expect(batchStateLabel("EM_ANDAMENTO")).toBe("em andamento");
    expect(batchStateLabel("CONCLUIDO")).toBe("concluído");
    expect(batchStateLabel("CONCLUIDO_COM_FALHAS")).toBe("concluído com falhas");
    expect(messageTypeLabel("PRODUCER")).toBe("PRODUTOR");
    expect(messageTypeLabel("LABORATORY")).toBe("LABORATÓRIO");
  });

  it("formata o payload JSON e preserva texto que não é JSON", () => {
    expect(prettyPayload('{"a":1}')).toBe('{\n  "a": 1\n}');
    expect(prettyPayload("texto livre")).toBe("texto livre");
    expect(prettyPayload(null)).toBe("");
  });

  it("o lote de exemplo tem itens válidos e um com inscrição fiscal inválida", () => {
    const batch = buildSampleBatch("T1");
    const taxIds = batch.items.map((item) => onlyDigits(String(item.data.inscricaoFiscal)));
    const valid = taxIds.map((d) => (d.length === 14 ? isValidCnpj(d) : isValidCpf(d)));
    expect(valid).toEqual([true, true, true, false]);
    expect(batch.items.at(-1)?.externalId).toBe("PROD-INVALIDO-T1");
    // o produtor referencia o mesmo id externo de laboratório e de propriedade que os itens do lote
    const producer = batch.items[2].data as { laboratorio: { id: string }; propriedades: Array<{ id: string }> };
    expect(producer.laboratorio.id).toBe(batch.items[0].externalId);
    expect(producer.propriedades[0].id).toBe(batch.items[1].externalId);
  });

  it("gera ids externos diferentes a cada carregamento, para o lote não virar duplicado", () => {
    const ids = (json: string) => (JSON.parse(json) as { items: Array<{ externalId: string }> }).items.map((i) => i.externalId);
    const first = ids(sampleBatchJson(new Date(2026, 10, 7, 10, 0, 0)));
    const second = ids(sampleBatchJson(new Date(2026, 10, 7, 10, 0, 1)));
    expect(new Set(first).size).toBe(4);
    expect(first.filter((id) => second.includes(id))).toEqual([]);
  });

  it("valida o envelope do JSON digitado", () => {
    expect(parseBatch(sampleBatchJson()).ok).toBe(true);
    expect(parseBatch("")).toEqual({ ok: false, error: "Cole ou carregue um JSON de lote." });
    expect(parseBatch("{ nope")).toMatchObject({ ok: false, error: expect.stringMatching(/^JSON inválido/) });
    expect(parseBatch("[]")).toMatchObject({ ok: false });
    expect(parseBatch('{"mergeStrategy":"MERGE","items":[]}')).toMatchObject({ ok: false, error: expect.stringMatching(/source/) });
    expect(parseBatch('{"source":"x","items":[]}')).toMatchObject({ ok: false, error: expect.stringMatching(/mergeStrategy/) });
    expect(parseBatch('{"source":"x","mergeStrategy":"MERGE"}')).toMatchObject({ ok: false, error: expect.stringMatching(/items/) });
  });
});
