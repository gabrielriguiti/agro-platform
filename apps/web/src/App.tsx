import { useEffect, useState } from "react";
import { NavLink, Navigate, Route, Routes } from "react-router-dom";
import { createApiClient, type InfoResponse } from "@agro/api-client";
import { AnalyticsPage } from "./pages/AnalyticsPage";
import { IntegrationPage } from "./pages/IntegrationPage";
import { ProducerFormPage } from "./pages/ProducerFormPage";
import { ProducersPage } from "./pages/ProducersPage";

const api = createApiClient();

/** Casca da aplicação: navegação e as quatro rotas (telas implementadas no change `web-ui`). */
export function App() {
  const [info, setInfo] = useState<InfoResponse | null>(null);

  useEffect(() => {
    api
      .GET("/api/info")
      .then(({ data }) => setInfo(data ?? null))
      .catch(() => setInfo(null));
  }, []);

  return (
    <>
      <header>
        <nav>
          <NavLink to="/produtores">Produtores</NavLink> <NavLink to="/produtores/novo">Novo produtor</NavLink>{" "}
          <NavLink to="/analise">Análise</NavLink> <NavLink to="/integracao">Integração</NavLink>
        </nav>
        <small>API: {info ? info.name : "indisponível"}</small>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/produtores" replace />} />
          <Route path="/produtores" element={<ProducersPage />} />
          <Route path="/produtores/novo" element={<ProducerFormPage />} />
          <Route path="/analise" element={<AnalyticsPage />} />
          <Route path="/integracao" element={<IntegrationPage />} />
        </Routes>
      </main>
    </>
  );
}
