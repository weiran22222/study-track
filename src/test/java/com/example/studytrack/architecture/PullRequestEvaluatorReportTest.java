package com.example.studytrack.architecture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PullRequestEvaluatorReportTest {

  private static final Path WORKFLOW = Path.of(".github", "workflows", "verify.yml");
  private static final Path REPORT_GUARD =
      Path.of("scripts", "check-pr-evaluator-report.sh");
  private static final String SUBJECT = "0123456789abcdef0123456789abcdef01234567";

  @Test
  void workflowSafelyMaterializesEventBodyAndInvokesTheLocalGuard() throws IOException {
    String workflow = readRequiredFile(WORKFLOW);

    assertAll(
        () ->
            assertTrue(
                workflow.contains("- name: Check the current evaluator report")
                    && workflow.contains("if: github.event_name == 'pull_request'"),
                failure(
                    WORKFLOW,
                    "The evaluator report guard is missing or not PR-only.",
                    "Restore the PR-only evaluator report step in jobs.verify.")),
        () ->
            assertTrue(
                workflow.contains("$GITHUB_EVENT_PATH")
                    && workflow.contains(".pull_request.body // \"\"")
                    && workflow.contains(".pull_request.head.sha // \"\"")
                    && workflow.contains("$RUNNER_TEMP"),
                failure(
                    WORKFLOW,
                    "The workflow no longer reads body and head SHA from the local event file.",
                    "Read both values from GITHUB_EVENT_PATH and materialize body under "
                        + "RUNNER_TEMP.")),
        () ->
            assertTrue(
                workflow.contains(
                    "sh ./scripts/check-pr-evaluator-report.sh \"$body_file\" \"$head_sha\""),
                failure(
                    WORKFLOW,
                    "The workflow no longer passes the body file and event head to the guard.",
                    "Invoke the report guard with the quoted local body file and head SHA.")),
        () ->
            assertFalse(
                workflow.contains("${{ github.event.pull_request.body"),
                failure(
                    WORKFLOW,
                    "Untrusted PR body content is interpolated directly into the workflow shell.",
                    "Remove body expression interpolation and use GITHUB_EVENT_PATH.")));
  }

  @Test
  void scriptKeepsTheNarrowOfflineFailClosedContract() throws IOException {
    String script = readRequiredFile(REPORT_GUARD);
    String lower = script.toLowerCase(Locale.ROOT);

    assertAll(
        () ->
            assertTrue(
                script.contains("if [ \"$#\" -ne 2 ]")
                    && script.contains(
                        "<!-- studytrack-evaluator-report:v1:start -->")
                    && script.contains(
                        "<!-- studytrack-evaluator-report:v1:end -->"),
                failure(
                    REPORT_GUARD,
                    "The script no longer requires two inputs and the exact v1 markers.",
                    "Restore the two-argument contract and exact begin/end markers.")),
        () ->
            assertTrue(
                List.of(
                        "Subject SHA:",
                        "Generator:",
                        "Evaluator:",
                        "Commands executed:",
                        "Independent scenarios:",
                        "Findings:",
                        "Residual gaps:",
                        "Verdict:")
                    .stream()
                    .allMatch(script::contains),
                failure(
                    REPORT_GUARD,
                    "A required v1 report field is missing from the parser.",
                    "Restore all eight fixed-order report fields.")),
        () ->
            assertTrue(
                script.contains("Location:")
                    && script.contains("Invariant:")
                    && script.contains("Reason:")
                    && script.contains("Fix:")
                    && script.contains("Recheck:")
                    && script.contains("Authority:"),
                failure(
                    REPORT_GUARD,
                    "The guard no longer retains all six actionable diagnostic fields.",
                    "Restore Location, Invariant, Reason, Fix, Recheck, and Authority.")),
        () ->
            assertFalse(
                lower.contains("curl ")
                    || lower.contains("wget ")
                    || lower.contains("gh ")
                    || lower.contains("git ")
                    || lower.contains("http://")
                    || lower.contains("https://"),
                failure(
                    REPORT_GUARD,
                    "The local report parser contains a network, GitHub, or Git command.",
                    "Keep the script limited to local body-file and SHA parsing.")));
  }

  @Test
  void posixGuardRejectsInvalidReportsAndAcceptsTheValidEnvelope(
      @TempDir Path temporaryDirectory) throws Exception {
    String shell = posixShell();

    assertRejected(
        shell, temporaryDirectory, "missing-marker", "PR body without a report", SUBJECT);
    assertRejected(
        shell,
        temporaryDirectory,
        "duplicate-marker",
        validReport(SUBJECT) + "\n" + validReport(SUBJECT),
        SUBJECT);
    assertRejected(
        shell,
        temporaryDirectory,
        "missing-field",
        validReport(SUBJECT).replace("Findings:\nnone\n", ""),
        SUBJECT);
    assertRejected(
        shell,
        temporaryDirectory,
        "empty-field",
        validReport(SUBJECT).replace("Generator: generator", "Generator:   "),
        SUBJECT);
    assertRejected(
        shell,
        temporaryDirectory,
        "empty-section",
        validReport(SUBJECT).replace("Findings:\nnone\n", "Findings:\n"),
        SUBJECT);
    assertRejected(
        shell,
        temporaryDirectory,
        "out-of-order-fields",
        validReport(SUBJECT)
            .replace(
                "Generator: generator\nEvaluator: evaluator",
                "Evaluator: evaluator\nGenerator: generator"),
        SUBJECT);
    assertRejected(
        shell,
        temporaryDirectory,
        "sha-mismatch",
        validReport("fedcba9876543210fedcba9876543210fedcba98"),
        SUBJECT);
    assertRejected(
        shell,
        temporaryDirectory,
        "non-pass",
        validReport(SUBJECT).replace("Verdict: PASS", "Verdict: FAIL"),
        SUBJECT);
    assertRejected(
        shell,
        temporaryDirectory,
        "inconclusive",
        validReport(SUBJECT).replace("Verdict: PASS", "Verdict: INCONCLUSIVE"),
        SUBJECT);

    ProcessResult valid =
        runGuard(shell, temporaryDirectory, "valid", validReport(SUBJECT), SUBJECT);
    assertEquals(
        0,
        valid.exitCode(),
        failure(
            REPORT_GUARD,
            "A complete current PASS report was rejected: " + valid.output(),
            "Accept the unique, fixed-order v1 report bound to the expected SHA."));
  }

  private static void assertRejected(
      String shell,
      Path temporaryDirectory,
      String name,
      String body,
      String expectedHead)
      throws Exception {
    ProcessResult result =
        runGuard(shell, temporaryDirectory, name, body, expectedHead);

    assertTrue(
        result.exitCode() != 0,
        failure(
            REPORT_GUARD,
            "The invalid report scenario was accepted: " + name,
            "Reject " + name + " with a non-zero exit code."));
    for (String field :
        List.of("Location:", "Invariant:", "Reason:", "Fix:", "Recheck:", "Authority:")) {
      assertTrue(
          result.output().contains(field),
          failure(
              REPORT_GUARD,
              "Scenario " + name + " omitted diagnostic field " + field,
              "Route every failure through the six-field diagnostic."));
    }
  }

  private static ProcessResult runGuard(
      String shell,
      Path temporaryDirectory,
      String name,
      String body,
      String expectedHead)
      throws Exception {
    Path bodyFile =
        Files.writeString(
            temporaryDirectory.resolve(name + ".md"), body, StandardCharsets.UTF_8);
    Process process =
        new ProcessBuilder(
                shell,
                REPORT_GUARD.toAbsolutePath().toString(),
                bodyFile.toAbsolutePath().toString(),
                expectedHead)
            .redirectErrorStream(true)
            .start();
    String output =
        new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    return new ProcessResult(process.waitFor(), output);
  }

  private static String posixShell() {
    if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
      return "sh";
    }

    String explicitShell = System.getenv("STUDYTRACK_POSIX_SHELL");
    Assumptions.assumeTrue(
        explicitShell != null && Files.isRegularFile(Path.of(explicitShell)),
        "Set STUDYTRACK_POSIX_SHELL to an explicit sh.exe to run POSIX behavior on Windows.");
    return Path.of(explicitShell).toAbsolutePath().toString();
  }

  private static String validReport(String subject) {
    return """
        <!-- studytrack-evaluator-report:v1:start -->
        Subject SHA: %s
        Generator: generator
        Evaluator: evaluator
        Commands executed:
        command and result
        Independent scenarios:
        independent scenario
        Findings:
        none
        Residual gaps:
        none
        Verdict: PASS
        <!-- studytrack-evaluator-report:v1:end -->
        """
        .formatted(subject);
  }

  private static String readRequiredFile(Path file) throws IOException {
    assertTrue(
        Files.isRegularFile(file),
        failure(
            file,
            "A required evaluator report contract file is missing.",
            "Restore " + file + "."));
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  private static String failure(Path location, String reason, String fix) {
    return """
        Pull request evaluator report invariant violated.
        Location: %s
        Invariant: the PR-only verify path must safely validate one complete v1 evaluator
        PASS report bound to the event head, without network, GitHub API, or Git access.
        Reason: %s
        Fix: %s
        Recheck: .\\mvnw.cmd -Dtest=PullRequestEvaluatorReportTest test, then
        .\\mvnw.cmd verify.
        Authority: WORKFLOW.md, ARCHITECTURE.md section 7,
        and docs/decisions/031-pr-evaluator-report-lifecycle.md.
        """
        .formatted(location, reason, fix);
  }

  private record ProcessResult(int exitCode, String output) {}
}
