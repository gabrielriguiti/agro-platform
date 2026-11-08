import { act, fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { Badge, Button, Field, Input, Modal, MultiSelect, Pagination, StateMessage, ToastProvider, useToast } from "./index";
import type { MultiSelectOption } from "./index";

const properties: MultiSelectOption[] = [
  { value: 1, label: "Fazenda Santa Rita", description: "Rio Verde – GO" },
  { value: 2, label: "Fazenda Boa Esperança", description: "Montividiu – GO" },
  { value: 3, label: "Sítio São José", description: "Uberlândia – MG" },
];

function Harness({ initial = [] as number[], onChange }: { initial?: number[]; onChange?: (v: number[]) => void }) {
  const [value, setValue] = useState(initial);
  return (
    <MultiSelect
      label="Propriedades"
      options={properties}
      value={value}
      onChange={(next) => {
        setValue(next);
        onChange?.(next);
      }}
    />
  );
}

describe("Field", () => {
  it("liga rótulo, erro e ajuda ao controle", () => {
    render(
      <Field label="Nome" required error="Informe o nome." help="Nome completo">
        {(control) => <Input {...control} />}
      </Field>,
    );
    const input = screen.getByLabelText(/Nome/);
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input).toHaveAttribute("aria-required", "true");
    expect(input).toHaveAccessibleDescription("Informe o nome. Nome completo");
  });

  it("não marca erro quando o campo é válido", () => {
    render(<Field label="Nome">{(control) => <Input {...control} />}</Field>);
    expect(screen.getByLabelText("Nome")).not.toHaveAttribute("aria-invalid");
  });
});

describe("MultiSelect", () => {
  it("mostra o contador e os chips da seleção", () => {
    render(<Harness initial={[1, 2]} />);
    expect(screen.getByRole("button", { name: /2 selecionadas/ })).toBeInTheDocument();
    const chips = within(screen.getByRole("list", { name: "Propriedades selecionadas" }));
    expect(chips.getByText("Fazenda Santa Rita")).toBeInTheDocument();
    expect(chips.getByText("Fazenda Boa Esperança")).toBeInTheDocument();
  });

  it("marca e desmarca itens pelo painel (editar conjunto)", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Harness initial={[1, 2]} onChange={onChange} />);
    await user.click(screen.getByRole("button", { name: /2 selecionadas/ }));
    await user.click(screen.getByRole("checkbox", { name: /Fazenda Boa Esperança/ }));
    await user.click(screen.getByRole("checkbox", { name: /Sítio São José/ }));
    expect(onChange).toHaveBeenLastCalledWith([1, 3]);
    expect(screen.getAllByText("2 selecionadas").length).toBeGreaterThan(0);
  });

  it("filtra por nome e informa quando nada é encontrado", async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole("button", { name: /Selecione/ }));
    await user.type(screen.getByRole("searchbox", { name: /Buscar/ }), "sítio");
    expect(screen.getAllByRole("checkbox")).toHaveLength(1);
    expect(screen.getByText("1 resultado")).toBeInTheDocument();
    await user.clear(screen.getByRole("searchbox", { name: /Buscar/ }));
    await user.type(screen.getByRole("searchbox", { name: /Buscar/ }), "xingu");
    expect(screen.getByText(/Nenhum item encontrado para “xingu”/)).toBeInTheDocument();
  });

  it("remove pelo chip, limpa a seleção e fecha com Esc", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Harness initial={[1, 2]} onChange={onChange} />);
    await user.click(screen.getByRole("button", { name: "Remover Fazenda Santa Rita" }));
    expect(onChange).toHaveBeenLastCalledWith([2]);
    await user.click(screen.getByRole("button", { name: /1 selecionada/ }));
    await user.click(screen.getByRole("button", { name: "Limpar seleção" }));
    expect(onChange).toHaveBeenLastCalledWith([]);
    await user.keyboard("{Escape}");
    expect(screen.queryByRole("group", { name: /Opções de/ })).not.toBeInTheDocument();
  });

  it("exibe o erro junto ao campo", () => {
    render(<MultiSelect label="Propriedades" options={properties} value={[1]} onChange={() => undefined} error="Fazenda Santa Rita já está vinculada a outro produtor." invalidValues={[1]} />);
    expect(screen.getByRole("button", { name: /1 selecionada/ })).toHaveAccessibleDescription(/já está vinculada/);
  });
});

describe("Modal", () => {
  it("só monta o conteúdo aberto e fecha com Esc e com o fundo", () => {
    const onClose = vi.fn();
    const { rerender } = render(
      <Modal open={false} title="Excluir produtor?" onClose={onClose}>
        corpo
      </Modal>,
    );
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    rerender(
      <Modal open title="Excluir produtor?" description="Esta ação não pode ser desfeita." onClose={onClose} actions={<Button>Cancelar</Button>}>
        corpo
      </Modal>,
    );
    const dialog = screen.getByRole("dialog", { name: "Excluir produtor?" });
    expect(dialog).toHaveTextContent("Esta ação não pode ser desfeita.");
    fireEvent(dialog, new Event("cancel", { cancelable: true }));
    expect(onClose).toHaveBeenCalledTimes(1);
    fireEvent.click(dialog);
    expect(onClose).toHaveBeenCalledTimes(2);
    fireEvent.click(screen.getByText("corpo"));
    expect(onClose).toHaveBeenCalledTimes(2);
  });
});

describe("Toast", () => {
  function Trigger() {
    const { notify } = useToast();
    return <button onClick={() => notify({ title: "Produtor salvo", message: "Carlos foi cadastrado." })}>avisar</button>;
  }

  it("aparece e some sozinho em 5 s", () => {
    vi.useFakeTimers();
    render(
      <ToastProvider>
        <Trigger />
      </ToastProvider>,
    );
    fireEvent.click(screen.getByText("avisar"));
    expect(screen.getByRole("status")).toHaveTextContent("Produtor salvo");
    act(() => {
      vi.advanceTimersByTime(5000);
    });
    expect(screen.queryByText("Produtor salvo")).not.toBeInTheDocument();
  });
});

describe("Pagination, StateMessage, Badge e Button", () => {
  it("mostra faixa e navega entre páginas", async () => {
    const user = userEvent.setup();
    const onPageChange = vi.fn();
    render(<Pagination page={2} size={10} totalElements={47} totalPages={5} onPageChange={onPageChange} />);
    expect(screen.getByText(/21–30/)).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Próxima" }));
    expect(onPageChange).toHaveBeenCalledWith(3);
    await user.click(screen.getByRole("button", { name: "Anterior" }));
    expect(onPageChange).toHaveBeenCalledWith(1);
  });

  it("desabilita Anterior na primeira página e Próxima na última", () => {
    const { rerender } = render(<Pagination page={0} size={10} totalElements={47} totalPages={5} onPageChange={() => undefined} />);
    expect(screen.getByRole("button", { name: "Anterior" })).toBeDisabled();
    rerender(<Pagination page={4} size={10} totalElements={47} totalPages={5} onPageChange={() => undefined} />);
    expect(screen.getByRole("button", { name: "Próxima" })).toBeDisabled();
    expect(screen.getByText(/41–47/)).toBeInTheDocument();
  });

  it("mostra a ação do estado de erro", async () => {
    const user = userEvent.setup();
    const onAction = vi.fn();
    render(
      <StateMessage kind="error" title="Não foi possível carregar" actionLabel="Tentar de novo" onAction={onAction}>
        A conexão falhou.
      </StateMessage>,
    );
    expect(screen.getByRole("alert")).toHaveTextContent("A conexão falhou.");
    await user.click(screen.getByRole("button", { name: "Tentar de novo" }));
    expect(onAction).toHaveBeenCalled();
  });

  it("botão em carregamento fica desabilitado", () => {
    render(<Button loading>Salvando…</Button>);
    expect(screen.getByRole("button")).toBeDisabled();
    expect(screen.getByRole("button")).toHaveAttribute("aria-busy", "true");
    render(<Badge tone="solid-err" mono>DEAD</Badge>);
    expect(screen.getByText("DEAD")).toHaveClass("badge--solid-err");
  });
});
