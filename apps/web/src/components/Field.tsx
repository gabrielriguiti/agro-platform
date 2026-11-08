import { forwardRef, useId } from "react";
import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";

/** Atributos que o {@link Field} entrega ao controle para ligar rótulo, erro e ajuda. */
export interface FieldControlProps {
  id: string;
  "aria-invalid": boolean | undefined;
  "aria-describedby": string | undefined;
  "aria-required": boolean | undefined;
}

export interface FieldProps {
  /** Texto do rótulo (12 px, acima do campo). */
  label: string;
  /** Marca o campo como obrigatório (asterisco vermelho e `aria-required`). */
  required?: boolean;
  /** Mensagem de erro, exibida logo abaixo do campo. */
  error?: string | null;
  /** Texto de ajuda opcional. */
  help?: ReactNode;
  className?: string;
  /** Recebe os atributos de acessibilidade e devolve o controle (`Input`, `Select`, `Textarea`...). */
  children: (control: FieldControlProps) => ReactNode;
}

/**
 * Rótulo, controle, ajuda e erro de um campo de formulário. O erro é ligado ao controle por `aria-describedby`.
 */
export function Field({ label, required, error, help, className, children }: FieldProps) {
  const id = useId();
  const errorId = `${id}-error`;
  const helpId = `${id}-help`;
  const describedBy = [error ? errorId : null, help ? helpId : null].filter(Boolean).join(" ") || undefined;
  return (
    <div className={["field", className ?? ""].filter(Boolean).join(" ")}>
      <label className="field__label" htmlFor={id}>
        {label}
        {required ? (
          <span className="field__req" aria-hidden="true">
            *
          </span>
        ) : null}
      </label>
      {children({
        id,
        "aria-invalid": error ? true : undefined,
        "aria-describedby": describedBy,
        "aria-required": required || undefined,
      })}
      {error ? (
        <p className="field__error" id={errorId}>
          {error}
        </p>
      ) : null}
      {help ? (
        <p className="field__help" id={helpId}>
          {help}
        </p>
      ) : null}
    </div>
  );
}

/** Campo de texto de uma linha (40 px). */
export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(function Input({ className, ...rest }, ref) {
  return <input ref={ref} {...rest} className={["input", className ?? ""].filter(Boolean).join(" ")} />;
});

/** Área de texto de várias linhas. */
export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(function Textarea({ className, ...rest }, ref) {
  return <textarea ref={ref} {...rest} className={["textarea", className ?? ""].filter(Boolean).join(" ")} />;
});

/** Seleção única nativa (`<select>`), estilizada. */
export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(function Select({ className, ...rest }, ref) {
  return <select ref={ref} {...rest} className={["select", className ?? ""].filter(Boolean).join(" ")} />;
});
