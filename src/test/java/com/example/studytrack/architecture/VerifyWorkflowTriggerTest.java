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
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class VerifyWorkflowTriggerTest {

  private static final Path WORKFLOW = Path.of(".github", "workflows", "verify.yml");
  private static final String PULL_REQUEST_ONLY =
      "if: github.event_name == 'pull_request'";
  private static final Pattern JOB_ID = Pattern.compile("(?m)^  ([a-z][a-z0-9_-]*):\\s*$");

  @Test
  void workflowTriggersAllPullRequestsAndOnlyLongLivedBranchPushes() throws IOException {
    String workflow = readRequiredFile();
    String triggers = requiredSection(workflow, "on:", "jobs:");
    String normalizedTriggers = triggers.replace("\r\n", "\n").strip();
    String expectedTriggers =
        """
        on:
          push:
            branches:
              - develop
              - main
          pull_request:
        """
            .strip();

    assertEquals(
        expectedTriggers,
        normalizedTriggers,
        failure(
            "on:",
            "The event matrix no longer keeps every pull request while limiting push to exactly "
                + "develop and main.",
            "Restore an unfiltered pull_request trigger and a push.branches list containing only "
                + "develop and main."));
  }

  @Test
  void verifyJobKeepsPrOnlyGatesAndUnconditionalCanonicalVerification() throws IOException {
    String workflow = readRequiredFile();
    String jobs = workflow.substring(workflow.indexOf("jobs:"));
    List<String> jobIds =
        JOB_ID.matcher(jobs).results().map(result -> result.group(1)).toList();
    int branchFlow = workflow.indexOf("- name: Check pull request branch flow");
    int pullRequestDiff = workflow.indexOf("- name: Check the complete pull request diff");
    int setupJava = workflow.indexOf("- name: Set up JDK 21");
    int environmentCheck = workflow.indexOf("sh ./scripts/check-environment.sh");
    int mavenVerify = workflow.indexOf("./mvnw --batch-mode --no-transfer-progress verify");

    assertAll(
        () ->
            assertEquals(
                List.of("verify"),
                jobIds,
                failure(
                    "jobs:",
                    "The workflow job identity is no longer exactly jobs.verify.",
                    "Keep the existing verify job name and do not replace it with another job.")),
        () ->
            assertTrue(
                branchFlow >= 0
                    && pullRequestDiff > branchFlow
                    && setupJava > pullRequestDiff
                    && environmentCheck > setupJava
                    && mavenVerify > environmentCheck,
                failure(
                    "jobs.verify.steps",
                    "The PR gates, JDK setup, environment check, and Maven verify order changed.",
                    "Run branch-flow first, complete PR diff second, then JDK 21 setup, "
                        + "environment self-check, and Maven verify.")),
        () ->
            assertEquals(
                2,
                countOccurrences(workflow, PULL_REQUEST_ONLY),
                failure(
                    "jobs.verify.steps",
                    "The two PR gates are not the only pull_request-conditional steps.",
                    "Keep pull_request conditions on branch-flow and complete diff only.")),
        () -> {
          String branchSection = workflow.substring(branchFlow, pullRequestDiff);
          String diffSection = workflow.substring(pullRequestDiff, setupJava);
          assertTrue(
              branchSection.contains(PULL_REQUEST_ONLY)
                  && diffSection.contains(PULL_REQUEST_ONLY),
              failure(
                  "jobs.verify.steps",
                  "A PR-only gate lost its pull_request condition.",
                  "Restore the condition on both branch-flow and complete diff steps."));
        },
        () ->
            assertFalse(
                workflow.substring(setupJava).contains("if:"),
                failure(
                    "jobs.verify.steps",
                    "JDK setup, environment self-check, or Maven verify became conditional.",
                    "Keep the canonical verification tail unconditional for every triggered "
                        + "pull_request, develop push, and main push.")));
  }

  @Test
  void failuresRetainAllSixActionableFields() {
    String diagnostic = failure("example", "example reason", "example fix");

    for (String field :
        List.of("Location:", "Invariant:", "Reason:", "Fix:", "Recheck:", "Authority:")) {
      assertTrue(
          diagnostic.contains(field),
          "Verify workflow trigger diagnostics must retain the field " + field);
    }
  }

  private static String requiredSection(String content, String startMarker, String endMarker) {
    int start = content.indexOf(startMarker);
    int end = content.indexOf(endMarker);
    assertTrue(
        start >= 0 && end > start,
        failure(
            WORKFLOW,
            "The workflow event section cannot be located.",
            "Restore top-level on and jobs sections."));
    return content.substring(start, end);
  }

  private static int countOccurrences(String content, String expected) {
    int count = 0;
    int position = 0;
    while ((position = content.indexOf(expected, position)) >= 0) {
      count++;
      position += expected.length();
    }
    return count;
  }

  private static String readRequiredFile() throws IOException {
    assertTrue(
        Files.isRegularFile(WORKFLOW),
        failure(
            WORKFLOW,
            "The required verify workflow is missing.",
            "Restore .github/workflows/verify.yml."));
    return Files.readString(WORKFLOW, StandardCharsets.UTF_8);
  }

  private static String failure(Object location, String reason, String fix) {
    return """
        Verify workflow trigger invariant violated.
        Location: %s
        Invariant: all pull requests must run the PR-only gates and canonical verification;
        push must trigger only for develop and main; the required job identity is jobs.verify.
        Reason: %s
        Fix: %s
        Recheck: .\\mvnw.cmd -Dtest=VerifyWorkflowTriggerTest test, then .\\mvnw.cmd verify.
        Authority: ARCHITECTURE.md section 7, WORKFLOW.md,
        and docs/decisions/028-streamline-ci-triggers.md.
        """
        .formatted(location, reason, fix);
  }
}
