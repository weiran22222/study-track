package com.example.studytrack.architecture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EnvironmentBootstrapTest {

  private static final Path WINDOWS_CHECK = Path.of("scripts", "check-environment.ps1");
  private static final Path POSIX_CHECK = Path.of("scripts", "check-environment.sh");
  private static final Path ENVIRONMENT_DOCUMENTATION = Path.of("docs", "environment.md");

  @Test
  void environmentEntryPointsAndDocumentationRemainDiscoverable() throws IOException {
    Path agentsPath = Path.of("AGENTS.md");
    Path workflowPath = Path.of("WORKFLOW.md");
    String agents = readRequiredFile(agentsPath, "Restore AGENTS.md at the repository root.");
    String workflow =
        readRequiredFile(workflowPath, "Restore WORKFLOW.md at the repository root.");

    assertAll(
        () ->
            assertTrue(
                Files.isRegularFile(WINDOWS_CHECK),
                failure(
                    WINDOWS_CHECK,
                    "The Windows environment check is missing.",
                    "Restore scripts/check-environment.ps1.")),
        () ->
            assertTrue(
                Files.isRegularFile(POSIX_CHECK),
                failure(
                    POSIX_CHECK,
                    "The POSIX environment check is missing.",
                    "Restore scripts/check-environment.sh.")),
        () ->
            assertTrue(
                Files.isRegularFile(ENVIRONMENT_DOCUMENTATION),
                failure(
                    ENVIRONMENT_DOCUMENTATION,
                    "The environment documentation is missing.",
                    "Restore docs/environment.md.")),
        () ->
            assertTrue(
                agents.contains("docs/environment.md")
                    && agents.contains("WORKFLOW.md")
                    && workflow.contains(".\\scripts\\check-environment.ps1")
                    && workflow.contains("sh ./scripts/check-environment.sh"),
                failure(
                    workflowPath,
                    "A fresh contributor cannot discover every environment check.",
                    "Link AGENTS.md to WORKFLOW.md and docs/environment.md, then retain both "
                        + "platform commands in WORKFLOW.md.")));
  }

  @Test
  void ciRunsTheEnvironmentCheckBeforeCanonicalVerification() throws IOException {
    Path workflow = Path.of(".github", "workflows", "verify.yml");
    String workflowText =
        readRequiredFile(workflow, "Restore .github/workflows/verify.yml.");
    int environmentCheck = workflowText.indexOf("sh ./scripts/check-environment.sh");
    int verification = workflowText.indexOf("./mvnw --batch-mode --no-transfer-progress verify");

    assertAll(
        () ->
            assertTrue(
                environmentCheck >= 0,
                failure(
                    workflow,
                    "CI no longer invokes the repository environment check.",
                    "Add sh ./scripts/check-environment.sh to the verify job.")),
        () ->
            assertTrue(
                verification >= 0 && environmentCheck < verification,
                failure(
                    workflow,
                    "CI does not run the environment check before the canonical verify command.",
                    "Place the environment check step before the mvnw verify step.")),
        () ->
            assertTrue(
                workflowText.contains("uses: actions/checkout@v6"),
                actionVersionFailure(
                    workflow,
                    "The checkout step does not use actions/checkout@v6.",
                    "Set the checkout step to uses: actions/checkout@v6.")),
        () ->
            assertTrue(
                workflowText.contains("uses: actions/setup-java@v5"),
                actionVersionFailure(
                    workflow,
                    "The Java setup step does not use actions/setup-java@v5.",
                    "Set the Java setup step to uses: actions/setup-java@v5.")));
  }

  @Test
  void platformChecksRetainTheSharedEnvironmentContract() throws IOException {
    String windowsCheck =
        readRequiredFile(WINDOWS_CHECK, "Restore scripts/check-environment.ps1.");
    String posixCheck =
        readRequiredFile(POSIX_CHECK, "Restore scripts/check-environment.sh.");

    assertAll(
        () -> assertContractMarkers(WINDOWS_CHECK, windowsCheck),
        () -> assertContractMarkers(POSIX_CHECK, posixCheck));
  }

  private static void assertContractMarkers(Path script, String scriptText) {
    assertTrue(
        scriptText.contains("JDK 21")
            && scriptText.contains("Maven Wrapper")
            && scriptText.contains("docs/environment.md")
            && scriptText.contains("verify")
            && scriptText.contains("Location:")
            && scriptText.contains("Invariant:")
            && scriptText.contains("Reason:")
            && scriptText.contains("Fix:")
            && scriptText.contains("Recheck:")
            && scriptText.contains("Then verify:")
            && scriptText.contains("Authority:"),
        failure(
            script,
            "The platform check no longer reports every required environment feedback field.",
            "Restore all shared feedback fields in the platform check."));
  }

  private static String readRequiredFile(Path file, String fix) throws IOException {
    assertTrue(
        Files.isRegularFile(file),
        failure(file, "A required environment bootstrap file is missing.", fix));
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  private static String failure(Path location, String reason, String fix) {
    return """
        Environment bootstrap invariant violated.
        Location: %s
        Invariant: AGENTS.md, WORKFLOW.md, platform checks, environment documentation, and CI
        must preserve one discoverable JDK 21 and repository Maven Wrapper feedback loop.
        Reason: %s
        Fix: %s
        Recheck: .\\scripts\\check-environment.ps1 (Windows) or
        sh ./scripts/check-environment.sh (macOS/Linux), then run the canonical verify command.
        Authority: WORKFLOW.md, docs/environment.md,
        docs/decisions/026-slim-agent-navigation.md, and
        docs/exec-plans/completed/006-environment-bootstrap.md
        """
        .formatted(location, reason, fix);
  }

  private static String actionVersionFailure(Path location, String reason, String fix) {
    return """
        GitHub Actions runtime invariant violated.
        Location: %s
        Invariant: CI must use actions/checkout@v6 and actions/setup-java@v5 so both official
        Actions run on the approved Node.js 24 runtime.
        Reason: %s
        Fix: %s
        Recheck: .\\scripts\\check-environment.ps1, then run .\\mvnw.cmd verify.
        Authority: docs/decisions/002-actions-node24-upgrade.md
        """
        .formatted(location, reason, fix);
  }
}
