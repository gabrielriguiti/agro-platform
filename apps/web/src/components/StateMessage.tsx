import type { ReactNode } from "react";
import { Button } from "./Button";

export interface StateMessageProps {
  /** `empty` para lista vazia e `error` para falha de carregamento. */
  kind: "empty" | "error";
  title: string;
  children?: ReactNode;
  /** Rótulo do botão de ação (ex.: "Tentar de novo"). */
  actionLabel?: string;
  onAction?: () => void;
}

/** Estado vazio ou de erro de uma área de conteúdo, com ação opcional. */
export function StateMessage({ kind, title, children, actionLabel, onAction }: StateMessageProps) {
  return (
    <div className={`state state--${kind}`} role={kind === "error" ? "alert" : "status"}>
      <span className="state__icon" aria-hidden="true">
        {kind === "error" ? "!" : "∅"}
      </span>
      <p className="state__title">{title}</p>
      {children ? <p className="state__text">{children}</p> : null}
      {actionLabel && onAction ? (
        <Button variant={kind === "error" ? "secondary" : "primary"} onClick={onAction}>
          {actionLabel}
        </Button>
      ) : null}
    </div>
  );
}
