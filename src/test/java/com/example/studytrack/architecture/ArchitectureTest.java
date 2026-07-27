package com.example.studytrack.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter().importPackages("com.example.studytrack");

  @Test
  void domainHasNoOutwardDependencies() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..application..",
            "..bootstrap..",
            "..cli..",
            "..infrastructure..",
            "com.fasterxml.jackson..",
            "picocli..")
        .because("Domain must remain independent; see ARCHITECTURE.md section 4")
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void applicationDoesNotDependOnAdapters() {
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..bootstrap..",
            "..cli..",
            "..infrastructure..",
            "com.fasterxml.jackson..",
            "java.nio.file..",
            "picocli..")
        .because("Application must depend on abstractions; see ARCHITECTURE.md section 4")
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void cliDoesNotAccessPersistenceDirectly() {
    noClasses()
        .that()
        .resideInAPackage("..cli..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infrastructure..", "com.fasterxml.jackson..", "java.nio.file..")
        .because("CLI must call Application services; see ARCHITECTURE.md section 4")
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void infrastructureDoesNotDependOnCli() {
    noClasses()
        .that()
        .resideInAPackage("..infrastructure..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..cli..", "picocli..")
        .because(
            "Infrastructure must not contain presentation logic; "
                + "see ARCHITECTURE.md section 4")
        .allowEmptyShould(true)
        .check(CLASSES);
  }
}
