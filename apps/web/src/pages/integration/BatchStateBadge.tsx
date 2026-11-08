import { Badge } from "../../components";
import { POLL_MS } from "../../api/integration";
import { batchStateLabel } from "./integrationModel";

/** Selo do estado do lote: azul com indicador e "a cada 5 s" em andamento, verde concluído, âmbar com falhas. */
export function BatchStateBadge({ state }: { state: string | undefined }) {
  const label = batchStateLabel(state);
  if (state === "EM_ANDAMENTO") {
    return (
      <Badge tone="info">
        <span className="spinner" aria-hidden="true" style={{ width: 10, height: 10, borderWidth: 1.5 }} />
        {label}
        <span className="badge__hint">a cada {POLL_MS / 1000} s</span>
      </Badge>
    );
  }
  if (state === "CONCLUIDO_COM_FALHAS") {
    return (
      <Badge tone="warn">
        <span aria-hidden="true" style={{ color: "var(--err)" }}>
          ▲
        </span>
        {label}
      </Badge>
    );
  }
  return <Badge tone={state === "CONCLUIDO" ? "ok" : "neutral"}>{label}</Badge>;
}
