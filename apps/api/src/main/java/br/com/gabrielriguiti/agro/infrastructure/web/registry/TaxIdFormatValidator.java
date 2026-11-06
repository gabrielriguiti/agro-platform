package br.com.gabrielriguiti.agro.infrastructure.web.registry;

import br.com.gabrielriguiti.agro.domain.registry.DomainValidationException;
import br.com.gabrielriguiti.agro.domain.registry.TaxId;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Valida a inscrição fiscal com a mesma regra do domínio ({@link TaxId}). */
class TaxIdFormatValidator implements ConstraintValidator<TaxIdFormat, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            TaxId.of(value);
            return true;
        } catch (DomainValidationException e) {
            return false;
        }
    }
}
