/** Só os dígitos de um texto. */
export function onlyDigits(value: string): string {
  return value.replace(/\D/g, "");
}

/**
 * Aplica a máscara de CPF (até 11 dígitos) ou CNPJ (12 a 14 dígitos) enquanto o usuário digita.
 *
 * @param value texto digitado, com ou sem máscara
 */
export function maskTaxId(value: string): string {
  const d = onlyDigits(value).slice(0, 14);
  if (d.length <= 11) {
    return d
      .replace(/^(\d{3})(\d)/, "$1.$2")
      .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
      .replace(/^(\d{3})\.(\d{3})\.(\d{3})(\d)/, "$1.$2.$3-$4");
  }
  return d
    .replace(/^(\d{2})(\d)/, "$1.$2")
    .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/^(\d{2})\.(\d{3})\.(\d{3})(\d)/, "$1.$2.$3/$4")
    .replace(/^(\d{2})\.(\d{3})\.(\d{3})\/(\d{4})(\d)/, "$1.$2.$3/$4-$5");
}

function checkDigit(base: number[], weights: number[]): number {
  const sum = base.reduce((acc, digit, i) => acc + digit * weights[i], 0);
  const rest = sum % 11;
  return rest < 2 ? 0 : 11 - rest;
}

/** @returns verdadeiro quando os 11 dígitos formam um CPF válido (dígitos verificadores corretos). */
export function isValidCpf(digits: string): boolean {
  if (!/^\d{11}$/.test(digits) || /^(\d)\1{10}$/.test(digits)) return false;
  const n = [...digits].map(Number);
  const d1 = checkDigit(n.slice(0, 9), [10, 9, 8, 7, 6, 5, 4, 3, 2]);
  const d2 = checkDigit(n.slice(0, 10), [11, 10, 9, 8, 7, 6, 5, 4, 3, 2]);
  return n[9] === d1 && n[10] === d2;
}

/** @returns verdadeiro quando os 14 dígitos formam um CNPJ válido (dígitos verificadores corretos). */
export function isValidCnpj(digits: string): boolean {
  if (!/^\d{14}$/.test(digits) || /^(\d)\1{13}$/.test(digits)) return false;
  const n = [...digits].map(Number);
  const d1 = checkDigit(n.slice(0, 12), [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  const d2 = checkDigit(n.slice(0, 13), [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  return n[12] === d1 && n[13] === d2;
}

/**
 * Valida a inscrição fiscal (CPF ou CNPJ) e devolve a mensagem de erro, ou `null` quando válida.
 *
 * @param value texto do campo, com ou sem máscara
 */
export function validateTaxId(value: string): string | null {
  const d = onlyDigits(value);
  if (d.length === 0) return "Informe a inscrição fiscal.";
  if (d.length <= 11) {
    if (d.length < 11) return "CPF incompleto.";
    return isValidCpf(d) ? null : "CPF inválido.";
  }
  if (d.length < 14) return "CNPJ incompleto.";
  return isValidCnpj(d) ? null : "CNPJ inválido.";
}

/**
 * Valida uma inscrição fiscal que só pode ser CNPJ (laboratório) e devolve a mensagem de erro, ou `null`.
 *
 * @param value texto do campo, com ou sem máscara
 */
export function validateCnpj(value: string): string | null {
  const d = onlyDigits(value);
  if (d.length === 0) return "Informe a inscrição fiscal.";
  if (d.length === 11 && isValidCpf(d)) return "O laboratório exige um CNPJ, não um CPF.";
  if (d.length < 14) return "CNPJ incompleto.";
  return isValidCnpj(d) ? null : "CNPJ inválido.";
}
