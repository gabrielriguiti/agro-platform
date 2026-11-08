import type { ReactNode } from "react";

export type BadgeTone = "neutral" | "ok" | "warn" | "info" | "err" | "solid-err";

export interface BadgeProps {
  /** Cor semântica. `solid-err` é o vermelho preenchido, reservado à DLQ (DEAD). */
  tone?: BadgeTone;
  /** Usa fonte monoespaçada (códigos de status). */
  mono?: boolean;
  children: ReactNode;
}

/** Selo de status em formato de pílula. */
export function Badge({ tone = "neutral", mono = false, children }: BadgeProps) {
  return <span className={["badge", `badge--${tone}`, mono ? "badge--mono" : ""].filter(Boolean).join(" ")}>{children}</span>;
}
