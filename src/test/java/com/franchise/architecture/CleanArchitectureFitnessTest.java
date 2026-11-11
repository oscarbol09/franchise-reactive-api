package com.franchise.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

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
    static final ArchRule domainMustOnlyDependOnStandardLibraryAndReactor = classes()
            .that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "..domain..",
                    "java..",
                    "reactor.core..",
                    "org.reactivestreams.."
            );

    @ArchTest
    static final ArchRule entryPointsMustNotAccessR2dbcAdaptersDirectly = noClasses()
            .that().resideInAPackage("..entrypoint..")
            .should().dependOnClassesThat()
            .resideInAPackage("..drivenadapter..");

    @ArchTest
    static final ArchRule hexagonalLayersMustBeRespected = Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("..domain..")
            .layer("EntryPoint").definedBy("..infrastructure.entrypoint..")
            .layer("DrivenAdapter").definedBy("..infrastructure.drivenadapter..")
            .layer("Config").definedBy("..infrastructure.config..")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("EntryPoint", "DrivenAdapter", "Config")
            .whereLayer("EntryPoint").mayNotBeAccessedByAnyLayer()
            .whereLayer("DrivenAdapter").mayOnlyBeAccessedByLayers("Config")
            .whereLayer("Config").mayNotBeAccessedByAnyLayer();

    @ArchTest
    static final ArchRule packagesMustBeFreeOfCycles = SlicesRuleDefinition.slices()
            .matching("com.franchise.(*)..")
            .should().beFreeOfCycles();
}
