package br.com.gabrielriguiti.agro.domain.registry;

/**
 * Inscrição fiscal brasileira (CPF ou CNPJ) como Value Object (ADR-0009).
 *
 * <p>Guarda apenas os dígitos, já validados (tamanho, dígitos verificadores e sequência repetida). A máscara é
 * tolerada na entrada por {@link #of(String)} e reaplicada na saída por {@link #formatted()}.
 *
 * @param digits somente dígitos: 11 (CPF) ou 14 (CNPJ)
 */
public record TaxId(String digits) {

    private static final int CPF_LENGTH = 11;
    private static final int CNPJ_LENGTH = 14;
    private static final int[] CNPJ_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    /**
     * Valida os dígitos recebidos.
     *
     * @throws InvalidTaxIdException se não for um CPF ou CNPJ válido
     */
    public TaxId {
        if (digits == null || !digits.chars().allMatch(c -> c >= '0' && c <= '9')) {
            throw new InvalidTaxIdException();
        }
        boolean valid = switch (digits.length()) {
            case CPF_LENGTH -> validCpf(digits);
            case CNPJ_LENGTH -> validCnpj(digits);
            default -> false;
        };
        if (!valid) {
            throw new InvalidTaxIdException();
        }
    }

    /**
     * Cria a partir de texto livre, removendo pontos, traço, barra e espaços.
     *
     * @param raw inscrição, com ou sem máscara
     * @return inscrição válida e normalizada
     * @throws InvalidTaxIdException se for nula, tiver outros caracteres ou for inválida
     */
    public static TaxId of(String raw) {
        if (raw == null) {
            throw new InvalidTaxIdException();
        }
        return new TaxId(raw.replaceAll("[.\\-/\\s]", ""));
    }

    /** @return {@code true} se for um CPF (11 dígitos) */
    public boolean isCpf() {
        return digits.length() == CPF_LENGTH;
    }

    /** @return {@code true} se for um CNPJ (14 dígitos) */
    public boolean isCnpj() {
        return digits.length() == CNPJ_LENGTH;
    }

    /** @return a inscrição com máscara: {@code 000.000.000-00} ou {@code 00.000.000/0000-00} */
    public String formatted() {
        if (isCpf()) {
            return digits.substring(0, 3) + "." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-"
                    + digits.substring(9);
        }
        return digits.substring(0, 2) + "." + digits.substring(2, 5) + "." + digits.substring(5, 8) + "/"
                + digits.substring(8, 12) + "-" + digits.substring(12);
    }

    private static boolean validCpf(String d) {
        if (allSame(d)) {
            return false;
        }
        int first = cpfDigit(d, 9);
        int second = cpfDigit(d, 10);
        return first == digit(d, 9) && second == digit(d, 10);
    }

    private static int cpfDigit(String d, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += digit(d, i) * (length + 1 - i);
        }
        int rest = (sum * 10) % 11;
        return rest == 10 ? 0 : rest;
    }

    private static boolean validCnpj(String d) {
        if (allSame(d)) {
            return false;
        }
        int first = cnpjDigit(d, 12);
        int second = cnpjDigit(d, 13);
        return first == digit(d, 12) && second == digit(d, 13);
    }

    private static int cnpjDigit(String d, int length) {
        int sum = 0;
        int offset = CNPJ_WEIGHTS.length - length;
        for (int i = 0; i < length; i++) {
            sum += digit(d, i) * CNPJ_WEIGHTS[i + offset];
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }

    private static boolean allSame(String d) {
        return d.chars().allMatch(c -> c == d.charAt(0));
    }

    private static int digit(String d, int index) {
        return d.charAt(index) - '0';
    }
}
