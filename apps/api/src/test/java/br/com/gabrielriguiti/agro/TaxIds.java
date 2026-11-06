package br.com.gabrielriguiti.agro;

import java.util.concurrent.atomic.AtomicLong;

/** Gera CPFs e CNPJs válidos e únicos para os testes (os exemplos do enunciado não passam na validação). */
public final class TaxIds {

    private static final AtomicLong CPF_SEQ = new AtomicLong(200_000_001L);
    private static final AtomicLong CNPJ_SEQ = new AtomicLong(20_000_001L);

    private TaxIds() {
    }

    /** @return CPF válido, único na JVM, apenas dígitos */
    public static String cpf() {
        String base = String.valueOf(CPF_SEQ.getAndIncrement());
        String d1 = base + check(base, 10, null);
        return d1 + check(d1, 11, null);
    }

    /** @return CNPJ válido, único na JVM, apenas dígitos */
    public static String cnpj() {
        String base = CNPJ_SEQ.getAndIncrement() + "0001";
        int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        String d1 = base + check(base, 0, w1);
        return d1 + check(d1, 0, w2);
    }

    private static int check(String digits, int cpfStartWeight, int[] cnpjWeights) {
        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            int weight = cnpjWeights != null ? cnpjWeights[i] : cpfStartWeight - i;
            sum += (digits.charAt(i) - '0') * weight;
        }
        if (cnpjWeights != null) {
            int rest = sum % 11;
            return rest < 2 ? 0 : 11 - rest;
        }
        int rest = (sum * 10) % 11;
        return rest == 10 ? 0 : rest;
    }
}
