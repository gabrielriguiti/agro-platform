import { Route, Routes } from "react-router-dom";
import { BatchDetail } from "./integration/BatchDetail";
import { BatchList } from "./integration/BatchList";
import { SendBatch } from "./integration/SendBatch";

/** Monitor de integração: lotes, mensagens/DLQ, reprocessamento e envio de lote de exemplo. */
export function IntegrationPage() {
  return (
    <Routes>
      <Route index element={<BatchList />} />
      <Route path="lotes/:batchId" element={<BatchDetail />} />
      <Route path="enviar" element={<SendBatch />} />
    </Routes>
  );
}
