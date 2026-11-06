package br.com.gabrielriguiti.agro.infrastructure.web.registry;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige que o texto seja um CPF ou CNPJ válido (com ou sem máscara). Valores nulos ou em branco são ignorados:
 * a obrigatoriedade é verificada por {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = TaxIdFormatValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface TaxIdFormat {

    String message() default "CPF/CNPJ inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
