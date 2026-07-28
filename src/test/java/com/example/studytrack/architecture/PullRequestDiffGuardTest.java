package com.example.studytrack.architecture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PullRequestDiffGuardTest {

  private static final Path WORKFLOW = Path.of(".github", "workflows", "verify.yml");
  private static final Path DIFF_GUARD = Path.of("scripts", "check-pr-diff.sh");

  @Test
  void workflowUsesCompleteHistoryAndPullRequestEventShas() throws IOException {
    String workflow = readRequiredFile(WORKFLOW);

    assertAll(
        () ->
            assertTrue(
                workflow.contains("fetch-depth: 0"),
                failure(
                    WORKFLOW,
                    "Checkout no longer fetches complete history.",
                    "Set actions/checkout fetch-depth to 0.")),
        () ->
            assertTrue(
                workflow.contains("if: github.event_name == 'pull_request'"),
                failure(
                    WORKFLOW,
                    "The diff guard is not limited to pull_request events.",
                    "Restore the pull_request-only condition on the diff guard step.")),
        () ->
            assertTrue(
                workflow.contains("github.event.pull_request.base.sha")
                    && workflow.contains("github.event.pull_request.head.sha"),
                failure(
                    WORKFLOW,
                    "The workflow no longer passes the event base and head SHAs.",
                    "Pass the pull_request base.sha and head.sha as separate script arguments.")),
        () ->
            assertTrue(
                workflow.contains("sh ./scripts/check-pr-diff.sh"),
                failure(
                    WORKFLOW,
                    "The verify job no longer invokes the repository diff guard.",
                    "Invoke scripts/check-pr-diff.sh from the pull_request-only step.")),
        () ->
            assertFalse(
                workflow.contains("HEAD^"),
                failure(
                    WORKFLOW,
                    "The workflow uses the final commit instead of the complete PR range.",
                    "Remove HEAD^ and preserve the event base...head wiring.")));
  }

  @Test
  void scriptChecksTheThreeDotRangeAndRetainsActionableFeedback() throws IOException {
    String script = readRequiredFile(DIFF_GUARD);

    assertAll(
        () ->
            assertTrue(
                script.contains("if [ \"$#\" -ne 2 ]"),
                failure(
                    DIFF_GUARD,
                    "The script no longer requires exactly two SHA arguments.",
                    "Require one base SHA and one head SHA.")),
        () ->
            assertTrue(
                script.contains("git diff --check \"$base...$head\""),
                failure(
                    DIFF_GUARD,
                    "The script no longer checks the quoted base...head range.",
                    "Run git diff --check with the quoted base...head range.")),
        () ->
            assertTrue(
                script.contains("diff_output=$(git diff --check \"$base...$head\" 2>&1)")
                    && script.contains("\"$diff_output\""),
                failure(
                    DIFF_GUARD,
                    "The script no longer places Git file and line diagnostics in Location.",
                    "Capture git diff --check output and pass it to the Location field.")),
        () ->
            assertTrue(
                script.contains("Location:")
                    && script.contains("Invariant:")
                    && script.contains("Reason:")
                    && script.contains("Fix:")
                    && script.contains("Recheck:")
                    && script.contains("Authority:"),
                failure(
                    DIFF_GUARD,
                    "The script no longer reports all six required failure fields.",
                    "Restore location, invariant, reason, fix, recheck, and authority fields.")),
        () ->
            assertTrue(
                script.contains("docs/decisions/016-ci-pr-diff-whitespace-gate.md")
                    && script.contains(
                        "https://git-scm.com/docs/git-diff"
                            + "#Documentation/git-diff.txt---check"),
                failure(
                    DIFF_GUARD,
                    "The failure does not cite both repository and Git authorities.",
                    "Restore decision 016 and the stable official git diff --check link.")));
  }

  @Test
  void windowsMavenVerificationDoesNotInvokeThePosixGuard() throws IOException {
    Path pom = Path.of("pom.xml");
    Path windowsEnvironmentCheck = Path.of("scripts", "check-environment.ps1");

    assertAll(
        () ->
            assertFalse(
                readRequiredFile(pom).contains("check-pr-diff.sh"),
                failure(
                    pom,
                    "The Maven lifecycle invokes the POSIX pull request diff guard.",
                    "Keep the PR-only guard in the GitHub Actions workflow.")),
        () ->
            assertFalse(
                readRequiredFile(windowsEnvironmentCheck).contains("check-pr-diff.sh"),
                failure(
                    windowsEnvironmentCheck,
                    "The Windows environment check invokes the POSIX pull request diff guard.",
                    "Keep the Windows environment check independent of system sh.")));
  }

  private static String readRequiredFile(Path file) throws IOException {
    assertTrue(
        Files.isRegularFile(file),
        failure(
            file,
            "A required PR diff guard contract file is missing.",
            "Restore " + file + "."));
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  private static String failure(Path location, String reason, String fix) {
    return """
        Pull request diff guard invariant violated.
        Location: %s
        Invariant: the verify job must check every PR's complete base...head diff with Git,
        while push and Windows Maven verification remain independent of PR-only POSIX context.
        Reason: %s
        Fix: %s
        Recheck: .\\mvnw.cmd -Dtest=PullRequestDiffGuardTest test, then .\\mvnw.cmd verify.
        Authority: docs/decisions/016-ci-pr-diff-whitespace-gate.md and ARCHITECTURE.md section 7.
        """
        .formatted(location, reason, fix);
  }
}
