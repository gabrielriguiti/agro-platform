import type { ReactNode } from "react";

export interface NoticeProps {
  /** Cor semântica do aviso. */
  tone?: "error" | "warn" | "info" | "ok";
  title: string;
  /** Rótulo curto à direita (ex.: "HTTP 409"). */
  tag?: string;
  children?: ReactNode;
}

/** Aviso em bloco (topo de formulário, resultado de operação). Erros são anunciados como alerta. */
export function Notice({ tone = "info", title, tag, children }: NoticeProps) {
  return (
    <div className={`notice notice--${tone}`} role={tone === "error" ? "alert" : "status"}>
      <div>
        <p className="notice__title">{title}</p>
        {children ? <div className="notice__text">{children}</div> : null}
      </div>
      {tag ? <span className="notice__tag">{tag}</span> : null}
    </div>
  );
}
