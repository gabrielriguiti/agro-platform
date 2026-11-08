import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";

/** Mensagem de toast. */
export interface ToastInput {
  tone?: "ok" | "error" | "info";
  title: string;
  message?: string;
}

interface ToastItem extends ToastInput {
  id: number;
}

interface ToastApi {
  /** Exibe um toast no canto superior direito; some sozinho em 5 s. */
  notify: (toast: ToastInput) => void;
}

/** Tempo de exibição do toast, em milissegundos. */
export const TOAST_DURATION_MS = 5000;

const ToastContext = createContext<ToastApi>({ notify: () => undefined });

/** Acesso à função `notify` dos toasts (sem efeito fora de um {@link ToastProvider}). */
export function useToast(): ToastApi {
  return useContext(ToastContext);
}

/** Provê os toasts à aplicação e renderiza a região de avisos. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);
  const nextId = useRef(1);
  const timers = useRef(new Map<number, ReturnType<typeof setTimeout>>());

  const dismiss = useCallback((id: number) => {
    const timer = timers.current.get(id);
    if (timer) clearTimeout(timer);
    timers.current.delete(id);
    setItems((current) => current.filter((item) => item.id !== id));
  }, []);

  const notify = useCallback(
    (toast: ToastInput) => {
      const id = nextId.current++;
      setItems((current) => [...current, { ...toast, id }]);
      timers.current.set(id, setTimeout(() => dismiss(id), TOAST_DURATION_MS));
    },
    [dismiss],
  );

  useEffect(() => {
    const active = timers.current;
    return () => {
      active.forEach((timer) => clearTimeout(timer));
      active.clear();
    };
  }, []);

  const api = useMemo(() => ({ notify }), [notify]);

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toasts" aria-live="polite">
        {items.map((item) => (
          <div key={item.id} className={`toast toast--${item.tone ?? "ok"}`} role={item.tone === "error" ? "alert" : "status"}>
            <div>
              <p className="toast__title">{item.title}</p>
              {item.message ? <p className="toast__text">{item.message}</p> : null}
            </div>
            <button type="button" className="toast__close" aria-label="Fechar aviso" onClick={() => dismiss(item.id)}>
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
