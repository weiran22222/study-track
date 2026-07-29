package com.example.studytrack.architecture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class BranchFlowGuardTest {

  private static final Path WORKFLOW = Path.of(".github", "workflows", "verify.yml");
  private static final Path BRANCH_FLOW_GUARD = Path.of("scripts", "check-branch-flow.sh");
  private static final Pattern ALLOWED_CASE =
      Pattern.compile(
          "^\\s*(develop:codex/\\?\\*|main:develop|main:hotfix/\\?\\*|develop:main)\\)$",
          Pattern.MULTILINE);
  private static final Pattern WORKFLOW_EVENTS =
      Pattern.compile("(?m)^on:\\R\\s{2}push:\\R\\s{2}pull_request:");

  @Test
  void workflowRunsBranchFlowFirstAndOnlyForPullRequests() throws IOException {
    String workflow = readRequiredFile(WORKFLOW);
    int branchStep = workflow.indexOf("- name: Check pull request branch flow");
    int branchCommand = workflow.indexOf("sh ./scripts/check-branch-flow.sh");
    int diffStep = workflow.indexOf("- name: Check the complete pull request diff");

    assertAll(
        () ->
            assertTrue(
                branchStep >= 0 && branchCommand > branchStep && diffStep > branchCommand,
                failure(
                    WORKFLOW,
                    "The branch-flow step is missing or no longer precedes the PR diff check.",
                    "Run check-branch-flow.sh before check-pr-diff.sh in the verify job.")),
        () -> {
          String branchSection = workflow.substring(branchStep, diffStep);
          assertTrue(
              branchSection.contains("if: github.event_name == 'pull_request'"),
              failure(
                  WORKFLOW,
                  "The branch-flow step is no longer limited to pull_request events.",
                  "Restore the pull_request-only condition so push verification skips it."));
        },
        () -> {
          String branchSection = workflow.substring(branchStep, diffStep);
          assertTrue(
              branchSection.contains("github.event.pull_request.base.ref")
                  && branchSection.contains("github.event.pull_request.head.ref"),
              failure(
                  WORKFLOW,
                  "The branch-flow step no longer receives the event base/head refs.",
                  "Pass pull_request.base.ref and head.ref as separate quoted arguments."));
        },
        () ->
            assertTrue(
                WORKFLOW_EVENTS.matcher(workflow).find(),
                failure(
                    WORKFLOW,
                    "The verify workflow no longer covers both push and pull_request events.",
                    "Keep push verification while limiting branch flow to pull requests.")));
  }

  @Test
  void scriptAllowsExactlyTheApprovedFourRouteMatrix() throws IOException {
    String script = readRequiredFile(BRANCH_FLOW_GUARD);
    Matcher matcher = ALLOWED_CASE.matcher(script);
    Set<String> actualCases =
        matcher.results().map(result -> result.group(1)).collect(Collectors.toSet());
    Set<String> expectedCases =
        Set.of("develop:codex/?*", "main:develop", "main:hotfix/?*", "develop:main");

    assertAll(
        () ->
            assertEquals(
                expectedCases,
                actualCases,
                failure(
                    BRANCH_FLOW_GUARD,
                    "The allowed case arms no longer equal the approved four-route matrix.",
                    "Restore only codex/* -> develop, develop -> main, hotfix/* -> main, "
                        + "and main -> develop.")),
        () ->
            assertTrue(
                script.contains("if [ \"$#\" -ne 2 ]")
                    && script.contains("if [ -z \"$base\" ] || [ -z \"$head\" ]"),
                failure(
                    BRANCH_FLOW_GUARD,
                    "The script no longer validates its exact, non-empty base/head inputs.",
                    "Require exactly two non-empty pull request refs.")),
        () ->
            assertTrue(
                script.contains("*)")
                    && script.contains("not in the approved branch-flow matrix")
                    && script.contains("exit 1"),
                failure(
                    BRANCH_FLOW_GUARD,
                    "Unknown base/head combinations no longer fail closed.",
                    "Keep a rejecting default case with a nonzero exit.")),
        () ->
            assertFalse(
                script.contains("codex/develop-production-branch"),
                failure(
                    BRANCH_FLOW_GUARD,
                    "A migration branch whitelist was added to the permanent guard.",
                    "Remove all migration-specific exceptions.")));
  }

  @Test
  void scriptRetainsActionableDiagnosticsAndExplicitSuccess() throws IOException {
    String script = readRequiredFile(BRANCH_FLOW_GUARD);

    assertAll(
        () ->
            assertTrue(
                script.contains("Location:")
                    && script.contains("Invariant:")
                    && script.contains("Reason:")
                    && script.contains("Fix:")
                    && script.contains("Recheck:")
                    && script.contains("Authority:"),
                failure(
                    BRANCH_FLOW_GUARD,
                    "Rejected branch flows no longer report all six required fields.",
                    "Restore location, invariant, reason, fix, recheck, and authority.")),
        () ->
            assertTrue(
                script.contains("docs/decisions/020-develop-production-branch-model.md")
                    && script.contains(
                        "docs/exec-plans/completed/017-develop-production-branch-model.md"),
                failure(
                    BRANCH_FLOW_GUARD,
                    "The guard no longer cites the approved decision and execution plan.",
                    "Restore decision 020 and plan 017 as authorities.")),
        () ->
            assertTrue(
                script.contains("StudyTrack branch flow allowed: base=%s head=%s route=%s"),
                failure(
                    BRANCH_FLOW_GUARD,
                    "Successful checks no longer identify base, head, and allowed route.",
                    "Print the accepted base/head refs and route.")));
  }

  private static String readRequiredFile(Path file) throws IOException {
    assertTrue(
        Files.isRegularFile(file),
        failure(file, "A required branch-flow contract file is missing.", "Restore " + file + "."));
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  private static String failure(Path location, String reason, String fix) {
    return """
        Pull request branch-flow invariant violated.
        Location: %s
        Invariant: the required verify job must reject every PR topology except the four
        routes authorized by decision 020, while push continues normal non-PR verification.
        Reason: %s
        Fix: %s
        Recheck: .\\mvnw.cmd -Dtest=BranchFlowGuardTest test, then .\\mvnw.cmd verify.
        Authority: docs/decisions/020-develop-production-branch-model.md and
        docs/exec-plans/completed/017-develop-production-branch-model.md
        """
        .formatted(location, reason, fix);
  }
}
