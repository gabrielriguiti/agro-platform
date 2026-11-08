import type { ReactNode, TableHTMLAttributes } from "react";

/** Tabela do design system (cabeçalho de 42 px, linhas de 52 px) com rolagem horizontal quando necessário. */
export function Table({ children, className, ...rest }: TableHTMLAttributes<HTMLTableElement>) {
  return (
    <div className="table-wrap">
      <table {...rest} className={["table", className ?? ""].filter(Boolean).join(" ")}>
        {children}
      </table>
    </div>
  );
}

export interface TableSkeletonProps {
  /** Rótulos reais do cabeçalho (o cabeçalho não é animado). */
  columns: string[];
  /** Quantidade de linhas animadas (padrão: 5). */
  rows?: number;
  /** Rótulo acessível da tabela. */
  label: string;
}

/** Tabela de carregamento: cabeçalho real e linhas animadas. */
export function TableSkeleton({ columns, rows = 5, label }: TableSkeletonProps) {
  return (
    <Table aria-busy="true" aria-label={label}>
      <thead>
        <tr>
          {columns.map((column) => (
            <th key={column}>{column}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {Array.from({ length: rows }, (_, row) => (
          <tr key={row} aria-hidden="true">
            {columns.map((column) => (
              <td key={column}>
                <span className="skeleton" style={{ width: `${55 + ((row * 7 + column.length * 5) % 40)}%` }} />
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </Table>
  );
}

/** Conteúdo auxiliar para células vazias. */
export function Dash({ children }: { children?: ReactNode }) {
  return <span className="muted">{children ?? "—"}</span>;
}
