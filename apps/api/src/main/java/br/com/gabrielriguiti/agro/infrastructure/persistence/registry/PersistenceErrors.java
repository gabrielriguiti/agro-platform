package br.com.gabrielriguiti.agro.infrastructure.persistence.registry;

import br.com.gabrielriguiti.agro.application.registry.ConflictException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/** Traduz violações de constraint do banco em {@link ConflictException} (409). */
final class PersistenceErrors {

    private PersistenceErrors() {
    }

    /**
     * @param e violação lançada pelo Spring Data
     * @return conflito com mensagem apropriada: inscrição duplicada ou integridade referencial
     */
    static ConflictException toConflict(DataIntegrityViolationException e) {
        String constraint = constraintName(e);
        if (constraint != null && constraint.endsWith("_tax_id")) {
            return new ConflictException("Já existe um registro com esta inscrição fiscal");
        }
        return new ConflictException("A operação viola uma restrição de integridade dos dados");
    }

    private static String constraintName(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve) {
                return cve.getConstraintName();
            }
        }
        return null;
    }
}
