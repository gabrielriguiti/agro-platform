const pad = (n: number) => String(n).padStart(2, "0");

/** Data no formato dd/mm/aaaa (fuso do navegador). */
export function formatDate(iso: string): string {
  const d = new Date(iso);
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}

/** Data e hora no formato dd/mm/aaaa hh:mm (fuso do navegador). */
export function formatDateTime(iso: string): string {
  const d = new Date(iso);
  return `${formatDate(iso)} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Hora no formato hh:mm:ss (fuso do navegador). */
export function formatTime(date: Date): string {
  return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

/** Período "dd/mm/aaaa – dd/mm/aaaa". */
export function formatPeriod(startIso: string, endIso: string): string {
  return `${formatDate(startIso)} – ${formatDate(endIso)}`;
}

const currency = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

/** Valor em reais, ex.: "R$ 184.320,00". */
export function formatCurrency(value: number): string {
  return currency.format(value);
}

/** Converte um instante ISO no valor de um `<input type="datetime-local">` (fuso do navegador). */
export function toDateTimeLocal(iso: string): string {
  const d = new Date(iso);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Converte o valor de um `<input type="datetime-local">` em instante ISO-8601 (UTC). */
export function fromDateTimeLocal(value: string): string {
  return new Date(value).toISOString();
}

/** Converte `<input type="date">` (aaaa-mm-dd) no início do dia, em ISO-8601 (UTC). */
export function startOfDayIso(date: string): string {
  return new Date(`${date}T00:00:00`).toISOString();
}

/** Converte `<input type="date">` (aaaa-mm-dd) no fim do dia, em ISO-8601 (UTC). */
export function endOfDayIso(date: string): string {
  return new Date(`${date}T23:59:59.999`).toISOString();
}
