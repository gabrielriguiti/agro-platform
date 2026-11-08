import { useEffect, useId, useMemo, useRef, useState } from "react";
import { Button } from "./Button";
import { Input } from "./Field";

/** Opção do {@link MultiSelect}. */
export interface MultiSelectOption {
  value: number;
  label: string;
  /** Linha secundária (ex.: endereço). */
  description?: string;
}

export interface MultiSelectProps {
  label: string;
  options: MultiSelectOption[];
  /** Valores selecionados. */
  value: number[];
  onChange: (value: number[]) => void;
  /** Texto do controle quando nada está selecionado. */
  placeholder?: string;
  /** Mensagem de erro, exibida abaixo do campo. */
  error?: string | null;
  help?: string;
  disabled?: boolean;
  /** Valores cujo chip é exibido em vermelho (ex.: item em conflito). */
  invalidValues?: number[];
}

function selectedText(count: number): string {
  return count === 1 ? "1 selecionada" : `${count} selecionadas`;
}

/**
 * Seleção múltipla com busca: lista de checkboxes num painel, chips das escolhas e contador.
 * Sem combobox customizado: o painel é um grupo de checkboxes nativos, navegável por teclado (Tab/Espaço).
 */
export function MultiSelect({ label, options, value, onChange, placeholder = "Selecione", error, help, disabled, invalidValues = [] }: MultiSelectProps) {
  const id = useId();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const root = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onPointerDown = (event: MouseEvent) => {
      if (root.current && !root.current.contains(event.target as Node)) setOpen(false);
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  const byValue = useMemo(() => new Map(options.map((option) => [option.value, option])), [options]);
  const filtered = useMemo(() => {
    const needle = query.trim().toLocaleLowerCase("pt-BR");
    if (!needle) return options;
    return options.filter((option) => option.label.toLocaleLowerCase("pt-BR").includes(needle));
  }, [options, query]);

  const toggle = (item: number) => onChange(value.includes(item) ? value.filter((v) => v !== item) : [...value, item]);
  const close = () => {
    setOpen(false);
    setQuery("");
  };
  const errorId = `${id}-error`;
  const panelId = `${id}-panel`;

  return (
    <div className="field">
      <span className="field__label" id={`${id}-label`}>
        {label}
      </span>
      <div className={["ms", open ? "ms--open" : ""].filter(Boolean).join(" ")} ref={root}>
        <button
          type="button"
          className="ms__control"
          aria-haspopup="true"
          aria-expanded={open}
          aria-controls={panelId}
          aria-labelledby={`${id}-label ${id}-control`}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          disabled={disabled}
          onClick={() => setOpen((current) => !current)}
        >
          <span id={`${id}-control`} className={value.length === 0 ? "ms__placeholder" : undefined}>
            {value.length === 0 ? placeholder : selectedText(value.length)}
          </span>
        </button>
        {open ? (
          <div className="ms__panel" id={panelId} role="group" aria-label={`Opções de ${label}`}>
            <div className="ms__search">
              <Input type="search" placeholder="Buscar por nome" aria-label={`Buscar em ${label}`} value={query} onChange={(event) => setQuery(event.target.value)} autoFocus />
            </div>
            <div className="ms__meta">
              <span>{filtered.length === 1 ? "1 resultado" : `${filtered.length} resultados`}</span>
              <span>{selectedText(value.length)}</span>
            </div>
            {filtered.length === 0 ? (
              <p className="ms__empty">{query.trim() ? `Nenhum item encontrado para “${query.trim()}”.` : "Nenhum item disponível."}</p>
            ) : (
              <ul className="ms__list">
                {filtered.map((option) => (
                  <li key={option.value}>
                    <label className="ms__option">
                      <input type="checkbox" className="checkbox" checked={value.includes(option.value)} onChange={() => toggle(option.value)} />
                      <span>
                        <span className="ms__option-label">{option.label}</span>
                        {option.description ? <span className="ms__option-desc">{option.description}</span> : null}
                      </span>
                    </label>
                  </li>
                ))}
              </ul>
            )}
            <div className="ms__foot">
              <Button variant="ghost" size="sm" disabled={value.length === 0} onClick={() => onChange([])}>
                Limpar seleção
              </Button>
              <Button variant="primary" size="sm" onClick={close}>
                Concluir
              </Button>
            </div>
          </div>
        ) : null}
      </div>
      {value.length > 0 ? (
        <ul className="chips" aria-label={`${label} selecionadas`} style={{ listStyle: "none", padding: 0 }}>
          {value.map((item) => {
            const option = byValue.get(item);
            const name = option?.label ?? `#${item}`;
            return (
              <li key={item} className={["chip", invalidValues.includes(item) ? "chip--error" : ""].filter(Boolean).join(" ")}>
                {name}
                <button type="button" className="chip__remove" aria-label={`Remover ${name}`} disabled={disabled} onClick={() => toggle(item)}>
                  ×
                </button>
              </li>
            );
          })}
        </ul>
      ) : null}
      {error ? (
        <p className="field__error" id={errorId}>
          {error}
        </p>
      ) : null}
      {help && !error ? <p className="field__help">{help}</p> : null}
    </div>
  );
}
