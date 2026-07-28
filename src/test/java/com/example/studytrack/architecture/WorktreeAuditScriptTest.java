package com.example.studytrack.architecture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class WorktreeAuditScriptTest {

  private static final Path AGENTS = Path.of("AGENTS.md");
  private static final Path AUDIT_SCRIPT = Path.of("scripts", "audit-worktrees.ps1");
  private static final Pattern MUTATING_COMMAND =
      Pattern.compile(
          "\\bworktree\\s+(?:remove|prune)\\b"
              + "|\\bbranch\\s+(?:-d|-D|--delete)\\b"
              + "|\\bRemove-Item\\b"
              + "|\\bgit\\s+push\\b",
          Pattern.CASE_INSENSITIVE);

  @Test
  void scriptExistsAndIsDiscoverableFromTheStableAgentMap() throws IOException {
    String agents = readRequiredFile(AGENTS);

    assertAll(
        () ->
            assertTrue(
                Files.isRegularFile(AUDIT_SCRIPT),
                failure(
                    AUDIT_SCRIPT,
                    "The worktree audit script is missing.",
                    "Restore scripts/audit-worktrees.ps1.")),
        () ->
            assertTrue(
                agents.contains(".\\scripts\\audit-worktrees.ps1"),
                failure(
                    AGENTS,
                    "The repository-root discovery command is missing.",
                    "Add the stable audit command without listing current worktrees.")),
        () ->
            assertTrue(
                agents.contains("Windows PowerShell 5.1"),
                failure(
                    AGENTS,
                    "The supported Windows PowerShell runtime is not documented.",
                    "Document Windows PowerShell 5.1 beside the discovery command.")));
  }

  @Test
  void scriptRetainsReadOnlyAndActionableFailureContracts() throws IOException {
    String script = readRequiredFile(AUDIT_SCRIPT);

    assertAll(
        () ->
            assertFalse(
                MUTATING_COMMAND.matcher(script).find(),
                failure(
                    AUDIT_SCRIPT,
                    "The audit contains a forbidden worktree, branch, file, or remote mutation.",
                    "Keep the script limited to read-only Git discovery commands.")),
        () ->
            assertFalse(
                script.contains("pwsh"),
                failure(
                    AUDIT_SCRIPT,
                    "The script requires a PowerShell runtime not present in the repository.",
                    "Keep the script compatible with Windows PowerShell 5.1.")),
        () ->
            assertTrue(
                script.contains("Location:")
                    && script.contains("Invariant:")
                    && script.contains("Reason:")
                    && script.contains("Fix:")
                    && script.contains("Recheck:")
                    && script.contains("Authority:"),
                failure(
                    AUDIT_SCRIPT,
                    "Git or parsing failures no longer provide all six diagnostic fields.",
                    "Restore the six labeled fields in the nonzero failure path.")));
  }

  @Test
  void staticContractProtectsClassificationWithoutClaimingPowerShellExecution()
      throws IOException {
    String script = readRequiredFile(AUDIT_SCRIPT);

    assertAll(
        () ->
            assertTrue(
                script.contains("\"PRIMARY path=")
                    && script.contains("\"ATTENTION path=")
                    && script.contains("\"REVIEW-CANDIDATE path="),
                failure(
                    AUDIT_SCRIPT,
                    "One or more required output classifications are missing.",
                    "Restore PRIMARY, ATTENTION, and REVIEW-CANDIDATE output.")),
        () ->
            assertTrue(
                script.contains("\"status\"")
                    && script.contains("\"--porcelain=v1\"")
                    && script.contains("\"--format=%(upstream)\"")
                    && script.contains("\"--left-right\"")
                    && script.contains("\"--count\""),
                failure(
                    AUDIT_SCRIPT,
                    "Candidate classification no longer reads dirty, upstream, and sync state.",
                    "Restore all read-only inputs required by decision 018.")),
        () ->
            assertTrue(
                script.contains("dirty worktree")
                    && script.contains("detached HEAD")
                    && script.contains("no local branch")
                    && script.contains("no upstream")
                    && script.contains("out of sync ahead="),
                failure(
                    AUDIT_SCRIPT,
                    "One or more unsafe linked-worktree states no longer produce ATTENTION.",
                    "Restore dirty, detached, branch, upstream, and sync attention reasons.")),
        () ->
            assertTrue(
                script.contains("($reasons.Count -eq 0)")
                    && script.contains("($ahead -eq 0)")
                    && script.contains("($behind -eq 0)")
                    && script.contains("Confirm the task and PR are complete before removal."),
                failure(
                    AUDIT_SCRIPT,
                    "The candidate gate or mandatory human-confirmation message was weakened.",
                    "Require no attention reasons, zero ahead/behind, "
                        + "and explicit human review.")));
  }

  private static String readRequiredFile(Path file) throws IOException {
    assertTrue(
        Files.isRegularFile(file),
        failure(
            file,
            "A required worktree audit contract file is missing.",
            "Restore " + file + "."));
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  private static String failure(Path location, String reason, String fix) {
    return """
        Worktree audit static contract violated.
        Location: %s
        Invariant: the PowerShell 5.1 audit must remain read-only, discoverable, and conservative;
        cross-platform Maven CI checks its text contract but does not execute PowerShell behavior.
        Reason: %s
        Fix: %s
        Recheck: .\\mvnw.cmd -Dtest=WorktreeAuditScriptTest test, then .\\mvnw.cmd verify.
        Authority: docs/decisions/018-worktree-hygiene-audit.md and AGENTS.md
        """
        .formatted(location, reason, fix);
  }
}
