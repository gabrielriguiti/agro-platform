import { useEffect, useId, useRef } from "react";
import type { ReactNode } from "react";

export interface ModalProps {
  /** Controla a abertura; o conteúdo só é montado enquanto aberto. */
  open: boolean;
  title: string;
  /** Texto de apoio abaixo do título. */
  description?: ReactNode;
  /** Chamado ao pressionar Esc ou clicar no fundo. */
  onClose: () => void;
  /** Botões do rodapé (ex.: Cancelar e Confirmar). */
  actions?: ReactNode;
  /** Exibe como painel lateral, à direita. */
  side?: boolean;
  children?: ReactNode;
}

/**
 * Janela modal sobre `<dialog>` nativo: foco preso, Esc e fundo fecham. Em ambientes sem `showModal`
 * (jsdom) cai para o atributo `open`.
 */
export function Modal({ open, title, description, onClose, actions, side = false, children }: ModalProps) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.hasAttribute("open")) {
      if (typeof dialog.showModal === "function") dialog.showModal();
      else dialog.setAttribute("open", "");
    }
    if (!open && dialog.hasAttribute("open")) {
      if (typeof dialog.close === "function") dialog.close();
      else dialog.removeAttribute("open");
    }
  }, [open]);

  if (!open) return null;

  return (
    <dialog
      ref={ref}
      className={["modal", side ? "modal--side" : ""].filter(Boolean).join(" ")}
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div className="modal__body">
        <h2 className="modal__title" id={titleId}>
          {title}
        </h2>
        {description ? <p className="modal__text">{description}</p> : null}
        {children ? <div className="modal__content">{children}</div> : null}
      </div>
      {actions ? <div className="modal__actions">{actions}</div> : null}
    </dialog>
  );
}
