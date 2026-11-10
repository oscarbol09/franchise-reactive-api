package com.franchise.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.franchise", importOptions = ImportOption.DoNotIncludeTests.class)
class CleanArchitectureFitnessTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnSpringOrInfrastructure = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..infrastructure..",
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.validation.."
            );

    @ArchTest
    static final ArchRule domainModelMustOnlyDependOnJavaOrDomain = classes()
            .that().resideInAPackage("..domain.model..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "..domain..",
                    "java..",
                    "reactor.core.publisher.."
            );

    @ArchTest
    static final ArchRule entryPointsMustNotAccessR2dbcAdaptersDirectly = noClasses()
            .that().resideInAPackage("..entrypoint..")
            .should().dependOnClassesThat()
            .resideInAPackage("..drivenadapter..");
}
