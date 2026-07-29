package com.example.studytrack.architecture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VerificationSubjectGuardTest {

  private static final Path AGENTS = Path.of("AGENTS.md");
  private static final Path WORKFLOW = Path.of(".github", "workflows", "verify.yml");
  private static final Path POWERSHELL_GUARD =
      Path.of("scripts", "check-verification-subject.ps1");
  private static final Path POSIX_GUARD =
      Path.of("scripts", "check-verification-subject.sh");
  private static final String TEST_BRANCH = "codex/verification-subject-fixture";
  private static final List<String> SIX_FIELDS =
      List.of("Location:", "Invariant:", "Reason:", "Fix:", "Recheck:", "Authority:");
  private static final Pattern FORBIDDEN_GIT_COMMAND =
      Pattern.compile(
          "(?i)\\bgit\\s+(?:add|branch|checkout|clean|clone|commit|fetch|merge|pull|push|"
              + "rebase|remote|reset|restore|stash|switch|tag|update-ref|worktree)\\b");
  private static final Pattern FORBIDDEN_POWERSHELL_WRITE =
      Pattern.compile(
          "(?i)\\b(?:Add-Content|Clear-Content|Copy-Item|Move-Item|New-Item|Out-File|"
              + "Remove-Item|Rename-Item|Set-Content)\\b");

  @TempDir Path temporaryDirectory;

  @Test
  void stableNavigationReferencesBothReadOnlyGuards() throws IOException {
    String agents = readRequiredFile(AGENTS);

    assertAll(
        () ->
            assertTrue(
                Files.isRegularFile(POWERSHELL_GUARD),
                failure(
                    POWERSHELL_GUARD,
                    "The Windows verification subject guard is missing.",
                    "Restore the PowerShell guard at its stable scripts path.")),
        () ->
            assertTrue(
                Files.isRegularFile(POSIX_GUARD),
                failure(
                    POSIX_GUARD,
                    "The POSIX verification subject guard is missing.",
                    "Restore the shell guard at its stable scripts path.")),
        () ->
            assertTrue(
                agents.contains("scripts/check-verification-subject.ps1")
                    && agents.contains("scripts/check-verification-subject.sh"),
                failure(
                    AGENTS,
                    "AGENTS.md no longer navigates to both subject guard entry points.",
                    "Reference both stable scripts paths in the frozen-SHA handoff workflow.")));
  }

  @Test
  void bothGuardsEnforceTheSameReadOnlySubjectInvariants() throws IOException {
    String powershell = readRequiredFile(POWERSHELL_GUARD);
    String posix = readRequiredFile(POSIX_GUARD);

    assertGuardContract(POWERSHELL_GUARD, powershell);
    assertGuardContract(POSIX_GUARD, posix);

    assertAll(
        () ->
            assertTrue(
                powershell.contains("git rev-parse --verify HEAD")
                    && posix.contains("git rev-parse --verify HEAD"),
                failure(
                    Path.of("scripts"),
                    "The guards no longer perform the same exact HEAD check.",
                    "Use git rev-parse --verify HEAD in both guards.")),
        () ->
            assertTrue(
                powershell.contains("git status --porcelain")
                    && posix.contains("git status --porcelain"),
                failure(
                    Path.of("scripts"),
                    "The guards no longer perform the same clean-worktree check.",
                    "Use git status --porcelain in both guards.")),
        () ->
            assertTrue(
                powershell.contains("git diff --cached --quiet --exit-code")
                    && posix.contains("git diff --cached --quiet --exit-code"),
                failure(
                    Path.of("scripts"),
                    "The guards no longer perform the same empty-index check.",
                    "Use git diff --cached --quiet --exit-code in both guards.")),
        () ->
            assertFalse(
                powershell.contains("symbolic-ref")
                    || posix.contains("symbolic-ref")
                    || powershell.contains("ExpectedBranch")
                    || posix.contains("expected_branch"),
                failure(
                    Path.of("scripts"),
                    "A guard still couples the Subject SHA to a branch.",
                    "Keep the guard input and invariants limited to the full Subject SHA.")));
  }

  @Test
  void guardsContainNoRepairWorktreeOrRemoteMutationCommands() throws IOException {
    String powershell = readRequiredFile(POWERSHELL_GUARD);
    String posix = readRequiredFile(POSIX_GUARD);

    assertAll(
        () ->
            assertFalse(
                FORBIDDEN_GIT_COMMAND.matcher(powershell).find(),
                failure(
                    POWERSHELL_GUARD,
                    "The PowerShell guard contains a forbidden mutating Git command.",
                    "Keep the guard diagnostic-only and remove the mutating command.")),
        () ->
            assertFalse(
                FORBIDDEN_GIT_COMMAND.matcher(posix).find(),
                failure(
                    POSIX_GUARD,
                    "The POSIX guard contains a forbidden mutating Git command.",
                    "Keep the guard diagnostic-only and remove the mutating command.")),
        () ->
            assertFalse(
                FORBIDDEN_POWERSHELL_WRITE.matcher(powershell).find(),
                failure(
                    POWERSHELL_GUARD,
                    "The PowerShell guard contains a filesystem write command.",
                    "Remove filesystem repair or mutation from the read-only guard.")),
        () ->
            assertFalse(
                powershell.contains("gh ")
                    || posix.contains("gh ")
                    || powershell.contains("Invoke-RestMethod")
                    || powershell.contains("Invoke-WebRequest")
                    || posix.contains("curl ")
                    || posix.contains("wget "),
                failure(
                    Path.of("scripts"),
                    "A verification subject guard contains a remote access command.",
                    "Keep both guards local and read-only.")));
  }

  @Test
  void agentsDefinesRolesLightweightHandoffsAndSameShaCompletionContract() throws IOException {
    String agents = readRequiredFile(AGENTS);

    assertAll(
        () ->
            assertTrue(
                agents.contains("**generator**")
                    && agents.contains("**evaluator**")
                    && agents.contains("**协调者**")
                    && agents.contains("**人类**"),
                failure(
                    AGENTS,
                    "One or more of the four stable roles is missing.",
                    "Define generator, evaluator, coordinator, and human responsibilities.")),
        () ->
            assertTrue(
                agents.contains("SPEC_READY → IMPLEMENTING → FROZEN(<Subject SHA>) → VERIFYING")
                    && agents.contains("PASS")
                    && agents.contains("FAIL → IMPLEMENTING")
                    && agents.contains("INCONCLUSIVE"),
                failure(
                    AGENTS,
                    "The approved handoff state machine is incomplete.",
                    "Restore the frozen-SHA verification state transitions.")),
        () ->
            assertTrue(
                Pattern.compile(
                        "协调者交给 generator 的最小任务交接只包含：\\R+```text\\R"
                            + "Task:\\RAcceptance criteria:\\RAllowed scope:\\R"
                            + "Prohibitions:\\R```")
                    .matcher(agents)
                    .find(),
                failure(
                    AGENTS,
                    "The generator handoff is not the approved four-field minimum.",
                    "Keep only Task, Acceptance criteria, Allowed scope, and Prohibitions.")),
        () ->
            assertTrue(
                Pattern.compile(
                        "协调者交给 evaluator 的最小交接只包含：\\R+```text\\R"
                            + "Task:\\RAcceptance criteria:\\RSubject SHA:\\RGenerator:\\R"
                            + "Evaluator:\\RMutation allowed: no\\R```")
                    .matcher(agents)
                    .find(),
                failure(
                    AGENTS,
                    "The evaluator handoff is not the approved six-field minimum.",
                    "Keep only the six evaluator handoff fields from decision 022.")),
        () ->
            assertTrue(
                agents.contains("Commands executed:")
                    && agents.contains("Independent scenarios:")
                    && agents.contains("Findings:")
                    && agents.contains("Residual gaps:")
                    && agents.contains("Verdict: PASS | FAIL | INCONCLUSIVE"),
                failure(
                    AGENTS,
                    "The evaluator report no longer records the required evidence.",
                    "Restore exact SHA, commands, scenarios, findings, gaps, and verdict.")),
        () ->
            assertTrue(
                agents.contains("任何修复产生新 SHA 后，旧报告立即失效")
                    && agents.contains("required `verify`")
                    && agents.contains("同一 Subject SHA"),
                failure(
                    AGENTS,
                    "SHA invalidation or same-SHA required verify completion is missing.",
                    "Bind evaluator PASS and required verify to the same immutable SHA.")),
        () ->
            assertTrue(
                agents.contains("`independent-verification` 的普通 CI Job")
                    && agents.contains("只有收到人类明确指令后才能创建或切换分支")
                    && agents.contains(
                        "协调者可按任务需要自行决定是否使用额外子智能体以及是否并行")
                    && agents.contains("仍必须依次进行"),
                failure(
                    AGENTS,
                    "The human branch authority, coordinator discretion, or serial SHA flow "
                        + "is missing.",
                    "Restore the lightweight coordination contract from decision 022.")),
        () ->
            assertFalse(
                agents.contains("serial shared")
                    || agents.contains("managed detached")
                    || agents.contains("Codex-managed Worktree")
                    || agents.contains("git worktree"),
                failure(
                    AGENTS,
                    "Active navigation still mandates a worktree or branch verification mode.",
                    "Keep runtime layout out of the active generator/evaluator contract.")));
  }

  @Test
  void workflowRetainsOnlyTheRequiredVerifyIdentity() throws IOException {
    String workflow = readRequiredFile(WORKFLOW);

    assertAll(
        () ->
            assertTrue(
                Pattern.compile("(?m)^jobs:\\R\\s{2}verify:\\s*$").matcher(workflow).find(),
                failure(
                    WORKFLOW,
                    "The required workflow job is no longer named verify.",
                    "Keep the existing jobs.verify identity unchanged.")),
        () ->
            assertFalse(
                workflow.toLowerCase(Locale.ROOT).contains("independent-verification"),
                failure(
                    WORKFLOW,
                    "The workflow claims an identity it cannot authenticate.",
                    "Remove the independent-verification job or check.")));
  }

  @Test
  void nativeGuardCoversCleanAndControlledFailureScenarios() throws Exception {
    Path repository = temporaryDirectory.resolve("guard-fixture");
    Files.createDirectories(repository);
    runSuccessfully(repository, "git", "init", "--initial-branch=" + TEST_BRANCH);
    runSuccessfully(repository, "git", "config", "user.name", "StudyTrack Test");
    runSuccessfully(repository, "git", "config", "user.email", "studytrack@example.invalid");

    Path trackedFile = repository.resolve("subject.txt");
    Files.writeString(trackedFile, "first\n", StandardCharsets.UTF_8);
    runSuccessfully(repository, "git", "add", "subject.txt");
    runSuccessfully(repository, "git", "commit", "-m", "first subject");
    final String oldSha =
        runSuccessfully(repository, "git", "rev-parse", "HEAD").output().trim();

    Files.writeString(trackedFile, "second\n", StandardCharsets.UTF_8);
    runSuccessfully(repository, "git", "add", "subject.txt");
    runSuccessfully(repository, "git", "commit", "-m", "new subject");
    String subjectSha = runSuccessfully(repository, "git", "rev-parse", "HEAD").output().trim();

    CommandResult clean = runNativeGuard(repository, subjectSha);
    assertAll(
        () ->
            assertEquals(
                0,
                clean.exitCode(),
                failure(
                    repository,
                    "The native guard rejected the exact clean subject: " + clean.output(),
                    "Restore acceptance of matching HEAD, clean working tree, and empty index.")),
        () ->
            assertTrue(
                clean.output().contains("Subject SHA: " + subjectSha)
                    && clean.output().contains("HEAD: " + subjectSha)
                    && clean.output().contains("Working tree: clean")
                    && clean.output().contains("Index: empty"),
                failure(
                    repository,
                    "Successful guard output no longer precisely reports the checked state.",
                    "Report Subject SHA, HEAD, clean working tree, and empty index.")));

    assertGuardFailure(
        runNativeGuard(repository, oldSha),
        "HEAD",
        "A report for the old SHA remained valid after a new commit.");

    Files.writeString(trackedFile, "dirty\n", StandardCharsets.UTF_8);
    assertGuardFailure(
        runNativeGuard(repository, subjectSha),
        "Git working tree status",
        "The guard accepted a dirty worktree.");

    Files.writeString(trackedFile, "second\n", StandardCharsets.UTF_8);
    Files.writeString(trackedFile, "staged\n", StandardCharsets.UTF_8);
    runSuccessfully(repository, "git", "add", "subject.txt");
    assertGuardFailure(
        runNativeGuard(repository, subjectSha),
        "Git index",
        "The guard accepted a non-empty index.");
  }

  @Test
  void nativeGuardReportsArgumentAndNonRepositoryFailures() throws Exception {
    Path nonRepository = temporaryDirectory.resolve("not-a-repository");
    Files.createDirectories(nonRepository);

    assertGuardFailure(
        runNativeGuard(nonRepository),
        "arguments",
        "The guard silently accepted missing handoff arguments.");
    assertGuardFailure(
        runNativeGuard(nonRepository, "0123456789012345678901234567890123456789", "extra"),
        "arguments",
        "The guard silently accepted more than one handoff argument.");
    assertGuardFailure(
        runNativeGuard(nonRepository, "abc"),
        "Subject SHA",
        "The guard silently accepted an abbreviated Subject SHA.");
    assertGuardFailure(
        runNativeGuard(nonRepository, ""),
        "Subject SHA argument",
        "The guard silently accepted an empty Subject SHA.");
    assertGuardFailure(
        runNativeGuard(nonRepository, "0123456789012345678901234567890123456789"),
        "current directory",
        "The guard silently accepted a non-Git directory.");
  }

  private static void assertGuardContract(Path path, String script) {
    assertAll(
        () ->
            assertFalse(
                script.contains("serial shared")
                    || script.contains("managed detached")
                    || script.contains("Codex-managed Worktree"),
                failure(
                    path,
                    "The guard still declares a worktree verification mode.",
                    "Limit the guard contract to one immutable Subject SHA.")),
        () -> {
          for (String field : SIX_FIELDS) {
            assertTrue(
                script.contains(field),
                failure(
                    path,
                    "Guard failures no longer include " + field,
                    "Restore all six actionable diagnostic fields."));
          }
        },
        () ->
            assertTrue(
                script.contains("docs/decisions/022-simplify-agent-handoff.md")
                    && script.contains(
                        "docs/exec-plans/completed/019-simplify-agent-handoff.md"),
                failure(
                    path,
                    "The guard no longer links its decision and execution-plan authorities.",
                    "Restore links to decision 022 and plan 019.")));
  }

  private static void assertGuardFailure(
      CommandResult result, String expectedLocation, String reason) {
    assertNotEquals(
        0,
        result.exitCode(),
        failure(
            Path.of("scripts"),
            reason + " Output: " + result.output(),
            "Reject the controlled failure with a nonzero exit."));
    for (String field : SIX_FIELDS) {
      assertTrue(
          result.output().contains(field),
          failure(
              Path.of("scripts"),
              reason + " Missing field " + field + " Output: " + result.output(),
              "Route every failure through the shared six-field diagnostic."));
    }
    assertTrue(
        result.output().contains(expectedLocation),
        failure(
            Path.of("scripts"),
            reason + " Output: " + result.output(),
            "Identify the rejected invariant in the Location field."));
  }

  private static CommandResult runNativeGuard(Path directory, String... arguments)
      throws Exception {
    List<String> command = new ArrayList<>();
    if (isWindows()) {
      command.add("powershell");
      command.add("-NoProfile");
      command.add("-ExecutionPolicy");
      command.add("Bypass");
      command.add("-File");
      command.add(POWERSHELL_GUARD.toAbsolutePath().toString());
    } else {
      command.add("sh");
      command.add(POSIX_GUARD.toAbsolutePath().toString());
    }
    command.addAll(List.of(arguments));
    return run(directory, command);
  }

  private static CommandResult runSuccessfully(Path directory, String... command)
      throws Exception {
    CommandResult result = run(directory, List.of(command));
    assertEquals(
        0,
        result.exitCode(),
        failure(
            directory,
            "Fixture command failed: "
                + String.join(" ", command)
                + " Output: "
                + result.output(),
            "Restore the deterministic temporary Git fixture."));
    return result;
  }

  private static CommandResult run(Path directory, List<String> command) throws Exception {
    Process process =
        new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
    boolean completed = process.waitFor(30, TimeUnit.SECONDS);
    if (!completed) {
      process.destroyForcibly();
      throw new IOException("Timed out running fixture command: " + String.join(" ", command));
    }
    String output =
        new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    return new CommandResult(process.exitValue(), output);
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
  }

  private static String readRequiredFile(Path file) throws IOException {
    assertTrue(
        Files.isRegularFile(file),
        failure(
            file,
            "A required generator/evaluator contract file is missing.",
            "Restore " + file + "."));
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  private static String failure(Path location, String reason, String fix) {
    return """
        Verification subject guard invariant violated.
        Location: %s
        Invariant: generator/evaluator handoff must bind a read-only evaluator to one exact
        Subject SHA with matching HEAD, a clean working tree, empty index, and unchanged verify
        identity.
        Reason: %s
        Fix: %s
        Recheck: .\\mvnw.cmd -Dtest=VerificationSubjectGuardTest test, then .\\mvnw.cmd verify.
        Authority: docs/decisions/022-simplify-agent-handoff.md and
        docs/exec-plans/completed/019-simplify-agent-handoff.md
        """
        .formatted(location, reason, fix);
  }

  private record CommandResult(int exitCode, String output) {}
}
