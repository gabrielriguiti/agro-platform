import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { ToastProvider } from "./components";

/**
 * Cria o cliente de cache do servidor.
 *
 * @param retry quantidade de novas tentativas em consultas que falham (padrão: 1; nos testes, 0)
 */
export function createQueryClient(retry: number | false = 1): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry, retryDelay: 500, refetchOnWindowFocus: false },
      mutations: { retry: false },
    },
  });
}

/** Provedores globais: cache do servidor (React Query) e toasts. */
export function AppProviders({ client, children }: { client?: QueryClient; children: ReactNode }) {
  const queryClient = client ?? defaultClient;
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>{children}</ToastProvider>
    </QueryClientProvider>
  );
}

const defaultClient = createQueryClient();
