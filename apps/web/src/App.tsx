import { NavLink, Navigate, Outlet, Route, Routes, useMatch } from "react-router-dom";
import { AnalyticsPage } from "./pages/AnalyticsPage";
import { IntegrationPage } from "./pages/IntegrationPage";
import { ProducerFormPage } from "./pages/ProducerFormPage";
import { ProducersPage } from "./pages/ProducersPage";
import { laboratoryConfig, propertyConfig } from "./pages/registry/registryConfig";
import { RegistryFormPage } from "./pages/registry/RegistryFormPage";
import { RegistryListPage } from "./pages/registry/RegistryListPage";

/** Cabeçalho com a navegação e a área de conteúdo das telas. */
function Layout() {
  const onNew = useMatch("/produtores/novo") !== null;
  return (
    <>
      <header className="app-header">
        <div className="app-header__inner">
          <span className="brand">agro-platform</span>
          <nav className="nav" aria-label="Principal">
            <NavLink to="/produtores" className={({ isActive }) => (isActive && !onNew ? "active" : undefined)}>
              Produtores
            </NavLink>
            <NavLink to="/produtores/novo">Novo produtor</NavLink>
            <NavLink to="/laboratorios">Laboratórios</NavLink>
            <NavLink to="/propriedades">Propriedades</NavLink>
            <NavLink to="/analise">Dashboard</NavLink>
            <NavLink to="/integracao">Integração</NavLink>
          </nav>
        </div>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </>
  );
}

/** Casca da aplicação: navegação e as rotas das telas. */
export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/produtores" replace />} />
        <Route path="/produtores" element={<ProducersPage />} />
        <Route path="/produtores/novo" element={<ProducerFormPage />} />
        <Route path="/produtores/:id/editar" element={<ProducerFormPage />} />
        <Route path="/laboratorios" element={<RegistryListPage config={laboratoryConfig} />} />
        <Route path="/laboratorios/novo" element={<RegistryFormPage config={laboratoryConfig} />} />
        <Route path="/laboratorios/:id/editar" element={<RegistryFormPage config={laboratoryConfig} />} />
        <Route path="/propriedades" element={<RegistryListPage config={propertyConfig} />} />
        <Route path="/propriedades/novo" element={<RegistryFormPage config={propertyConfig} />} />
        <Route path="/propriedades/:id/editar" element={<RegistryFormPage config={propertyConfig} />} />
        <Route path="/analise" element={<AnalyticsPage />} />
        <Route path="/integracao/*" element={<IntegrationPage />} />
        <Route path="*" element={<Navigate to="/produtores" replace />} />
      </Route>
    </Routes>
  );
}
