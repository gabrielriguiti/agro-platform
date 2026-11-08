import type { ButtonHTMLAttributes } from "react";

/** Variantes visuais do botão. Uma ação primária por área; destrutivo só para confirmar exclusão. */
export type ButtonVariant = "primary" | "secondary" | "danger" | "ghost" | "ghost-danger";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** Variante visual (padrão: `secondary`). */
  variant?: ButtonVariant;
  /** `sm` (32 px) para tabelas e barras de ferramentas; `md` (40 px) é o padrão. */
  size?: "md" | "sm";
  /** Mostra o indicador de carregamento e desabilita o botão. */
  loading?: boolean;
}

/** Botão do design system. O tipo padrão é `button`, para não enviar formulários sem querer. */
export function Button({ variant = "secondary", size = "md", loading = false, className, children, disabled, type = "button", ...rest }: ButtonProps) {
  const classes = ["btn", `btn--${variant}`, size === "sm" ? "btn--sm" : "", className ?? ""].filter(Boolean).join(" ");
  return (
    <button {...rest} type={type} className={classes} disabled={disabled || loading} aria-busy={loading || undefined}>
      {loading ? <span className="spinner" aria-hidden="true" /> : null}
      {children}
    </button>
  );
}
