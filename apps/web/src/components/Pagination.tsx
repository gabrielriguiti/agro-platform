import { Button } from "./Button";

export interface PaginationProps {
  /** Página atual, a partir de 0 (como na API). */
  page: number;
  /** Itens por página. */
  size: number;
  /** Total de itens. */
  totalElements: number;
  /** Total de páginas. */
  totalPages: number;
  onPageChange: (page: number) => void;
}

/** Rodapé de paginação: faixa exibida, "Anterior", "Página N de M" e "Próxima". */
export function Pagination({ page, size, totalElements, totalPages, onPageChange }: PaginationProps) {
  if (totalElements === 0) return null;
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);
  const pages = Math.max(totalPages, 1);
  return (
    <nav className="pagination" aria-label="Paginação">
      <span>
        Mostrando{" "}
        <b>
          {from}–{to}
        </b>{" "}
        de <b>{totalElements}</b>
      </span>
      <div className="pagination__nav">
        <Button size="sm" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
          Anterior
        </Button>
        <span>
          Página <b>{page + 1}</b> de {pages}
        </span>
        <Button size="sm" disabled={page + 1 >= pages} onClick={() => onPageChange(page + 1)}>
          Próxima
        </Button>
      </div>
    </nav>
  );
}
