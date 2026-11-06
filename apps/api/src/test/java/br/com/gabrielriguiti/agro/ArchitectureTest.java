package br.com.gabrielriguiti.agro;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Impõe a arquitetura hexagonal (ADR-0001, ADR-0005) no build; cobre a spec {@code architecture-boundaries}.
 * Roda no Surefire, sem Docker.
 */
@AnalyzeClasses(packages = "br.com.gabrielriguiti.agro", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String BASE = "br.com.gabrielriguiti.agro";

    /** O domínio depende apenas de Java padrão e de si mesmo. */
    @ArchTest
    static final ArchRule dominioSemFramework = classes()
            .that().resideInAPackage(BASE + ".domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(BASE + ".domain..", "java..", "javax..")
            .because("o domínio não pode depender de Spring, JPA ou outro framework")
            .allowEmptyShould(true);

    /** Casos de uso não conhecem adaptadores. */
    @ArchTest
    static final ArchRule aplicacaoSemInfraestrutura = noClasses()
            .that().resideInAPackage(BASE + ".application..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".infrastructure..")
            .because("application declara portas; infrastructure as implementa")
            .allowEmptyShould(true);

    /** Controllers entram pelos casos de uso, nunca pela persistência. */
    @ArchTest
    static final ArchRule controllersSemPersistencia = noClasses()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .or().areAnnotatedWith("org.springframework.stereotype.Controller")
            .should().dependOnClassesThat().resideInAnyPackage(
                    BASE + ".infrastructure.persistence..", "jakarta.persistence..", "org.springframework.data..",
                    "org.springframework.jdbc..")
            .because("controllers devem chamar casos de uso de application")
            .allowEmptyShould(true);

    /** O motor financeiro é puro e testável sem contexto do Spring. */
    @ArchTest
    static final ArchRule motorFinanceiroPuro = noClasses()
            .that().resideInAnyPackage(BASE + ".domain.financial..", BASE + ".application.financial..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .because("calculadoras financeiras são testadas sem Spring")
            .allowEmptyShould(true);
}
