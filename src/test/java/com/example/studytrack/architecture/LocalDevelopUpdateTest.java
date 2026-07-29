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

class LocalDevelopUpdateTest {

  private static final Path WORKFLOW = Path.of("WORKFLOW.md");
  private static final Path POWERSHELL_UPDATER =
      Path.of("scripts", "update-local-develop.ps1");
  private static final Path POSIX_UPDATER = Path.of("scripts", "update-local-develop.sh");
  private static final List<String> SIX_FIELDS =
      List.of("Location:", "Invariant:", "Reason:", "Fix:", "Recheck:", "Authority:");
  private static final Pattern FORBIDDEN_GIT_COMMAND =
      Pattern.compile(
          "(?i)\\bgit\\s+(?:checkout|cherry-pick|push|rebase|reset|restore|switch)\\b");
  private static final Pattern FORBIDDEN_REMOTE_COMMAND =
      Pattern.compile("(?im)^\\s*(?:&\\s*)?(?:gh|curl|wget)\\s+");
  private static final Pattern VALIDATION_SHA_AS_SOURCE =
      Pattern.compile(
          "(?i)git\\s+(?:fetch|merge|rev-list|rev-parse)[^\\r\\n]*"
              + "\\$(?:VerifiedDevelopSha|verified_develop_sha)");

  @TempDir Path temporaryDirectory;

  @Test
  void stableWorkflowAndBothScriptsDeclareTheApprovedContract() throws IOException {
    String workflow = readRequiredFile(WORKFLOW);
    String powershell = readRequiredFile(POWERSHELL_UPDATER);
    String posix = readRequiredFile(POSIX_UPDATER);

    assertAll(
        () ->
            assertTrue(
                workflow.contains("禁止在本地 `develop` 上 merge、rebase 或 cherry-pick")
                    && workflow.contains("受保护 GitHub PR")
                    && workflow.contains("required")
                    && workflow.contains("push `verify` 成功"),
                failure(
                    WORKFLOW,
                    "The GitHub-only integration or final push verify rule is missing.",
                    "Restore the stable local develop update policy from decision 023.")),
        () ->
            assertTrue(
                workflow.contains("scripts\\update-local-develop.ps1")
                    && workflow.contains("scripts/update-local-develop.sh")
                    && workflow.contains("唯一更新源为 `origin/develop`"),
                failure(
                    WORKFLOW,
                    "The cross-platform origin/develop-only entry points are missing.",
                    "Navigate to both stable updater paths and their only permitted source.")),
        () ->
            assertTrue(
                Files.isRegularFile(POWERSHELL_UPDATER) && Files.isRegularFile(POSIX_UPDATER),
                failure(
                    Path.of("scripts"),
                    "One or both local develop updater scripts are missing.",
                    "Restore both repository-owned updater entry points.")),
        () -> assertScriptContract(POWERSHELL_UPDATER, powershell),
        () -> assertScriptContract(POSIX_UPDATER, posix));
  }

  @Test
  void nativeUpdaterFastForwardsAndThenAcceptsAnExactNoOp() throws Exception {
    Fixture fixture = createFixture("success");
    String oldHead = head(fixture.local());
    String verifiedSha = advanceRemote(fixture, "remote second\n", "remote second");

    CommandResult update = runNativeUpdater(fixture.local(), verifiedSha);
    assertAll(
        () ->
            assertEquals(
                0,
                update.exitCode(),
                failure(
                    fixture.local(),
                    "The updater rejected a clean strict-behind develop: " + update.output(),
                    "Restore the origin/develop-only fast-forward success path.")),
        () ->
            assertEquals(
                verifiedSha,
                head(fixture.local()),
                failure(
                    fixture.local(),
                    "The successful update did not reach the verified remote SHA.",
                    "Fast-forward local develop to the exact fetched and verified SHA.")),
        () ->
            assertEquals(
                "1",
                runSuccessfully(
                        fixture.local(),
                        "git",
                        "rev-list",
                        "--count",
                        oldHead + ".." + verifiedSha)
                    .output()
                    .trim(),
                failure(
                    fixture.local(),
                    "The fixture no longer demonstrates one strict fast-forward commit.",
                    "Keep the success fixture deterministic and linear.")),
        () ->
            assertTrue(
                update.output().contains("StudyTrack local develop update passed.")
                    && update.output().contains("HEAD: " + verifiedSha)
                    && update.output().contains("remote-ahead=1"),
                failure(
                    fixture.local(),
                    "Successful output no longer reports the exact updated state.",
                    "Report the verified SHA, final HEAD, and prior relationship.")),
        () -> assertClean(fixture.local()));

    CommandResult noOp = runNativeUpdater(fixture.local(), verifiedSha);
    assertAll(
        () ->
            assertEquals(
                0,
                noOp.exitCode(),
                failure(
                    fixture.local(),
                    "The updater rejected an exact clean no-op: " + noOp.output(),
                    "Accept local develop already equal to the verified origin/develop SHA.")),
        () ->
            assertEquals(
                verifiedSha,
                head(fixture.local()),
                failure(
                    fixture.local(),
                    "The no-op changed the exact local develop commit.",
                    "Leave HEAD unchanged when local and remote develop are equal.")),
        () ->
            assertTrue(
                noOp.output().contains("ahead=0, remote-ahead=0"),
                failure(
                    fixture.local(),
                    "The no-op output does not report the equal relationship.",
                    "Report zero ahead counts for an exact no-op.")),
        () -> assertClean(fixture.local()));
  }

  @Test
  void nativeUpdaterRejectsArgumentsRepositoryAndBranchBeforeMutation() throws Exception {
    Path nonRepository = temporaryDirectory.resolve("not-a-repository");
    Files.createDirectories(nonRepository);
    String fakeSha = "0123456789012345678901234567890123456789";

    assertFailureWithoutRepository(
        runNativeUpdater(nonRepository), "arguments", "Missing SHA was accepted.");
    assertFailureWithoutRepository(
        runNativeUpdater(nonRepository, fakeSha, "extra"),
        "arguments",
        "An extra argument was accepted.");
    assertFailureWithoutRepository(
        runNativeUpdater(nonRepository, "abc"),
        "Verified develop SHA",
        "An abbreviated SHA was accepted.");
    final String invalidHexSha = "012345678901234567890123456789012345678g";
    assertFailureWithoutRepository(
        runNativeUpdater(nonRepository, invalidHexSha),
        "Verified develop SHA",
        "A 40-character non-hexadecimal SHA was accepted.");
    assertFailureWithoutRepository(
        runNativeUpdater(nonRepository, fakeSha),
        "current directory",
        "A non-repository directory was accepted.");

    Fixture wrongBranch = createFixture("wrong-branch");
    runSuccessfully(wrongBranch.local(), "git", "switch", "-c", "feature/local-test");
    String beforeHead = head(wrongBranch.local());
    assertFailure(
        runNativeUpdater(wrongBranch.local(), wrongBranch.baseSha()),
        "current branch",
        wrongBranch.local(),
        beforeHead,
        "A feature branch was accepted.");
  }

  @Test
  void nativeUpdaterRejectsDirtyUntrackedAndStagedStateWithoutMovingDevelop()
      throws Exception {
    Fixture dirty = createFixture("dirty");
    String dirtyHead = head(dirty.local());
    Files.writeString(dirty.trackedFile(), "dirty worktree\n", StandardCharsets.UTF_8);
    assertFailure(
        runNativeUpdater(dirty.local(), dirty.baseSha()),
        "Git working tree status",
        dirty.local(),
        dirtyHead,
        "A tracked dirty worktree was accepted.");
    assertEquals(
        "dirty worktree\n",
        Files.readString(dirty.trackedFile(), StandardCharsets.UTF_8),
        failure(
            dirty.trackedFile(),
            "The failed updater changed dirty user content.",
            "Leave user files untouched on every failed precondition."));

    Fixture untracked = createFixture("untracked");
    String untrackedHead = head(untracked.local());
    Path untrackedFile = untracked.local().resolve("untracked.txt");
    Files.writeString(untrackedFile, "keep me\n", StandardCharsets.UTF_8);
    assertFailure(
        runNativeUpdater(untracked.local(), untracked.baseSha()),
        "Git working tree status",
        untracked.local(),
        untrackedHead,
        "An untracked file was accepted.");
    assertTrue(
        Files.isRegularFile(untrackedFile),
        failure(
            untrackedFile,
            "The failed updater removed an untracked user file.",
            "Never clean untracked files from the updater."));

    Fixture staged = createFixture("staged");
    String stagedHead = head(staged.local());
    Files.writeString(staged.trackedFile(), "staged content\n", StandardCharsets.UTF_8);
    runSuccessfully(staged.local(), "git", "add", "subject.txt");
    assertFailure(
        runNativeUpdater(staged.local(), staged.baseSha()),
        "Git index",
        staged.local(),
        stagedHead,
        "A non-empty index was accepted.");
    assertNotEquals(
        0,
        run(staged.local(), List.of("git", "diff", "--cached", "--quiet")).exitCode(),
        failure(
            staged.local(),
            "The failed updater cleared staged user work.",
            "Leave the index untouched on a failed precondition."));
  }

  @Test
  void nativeUpdaterRejectsShaDriftAheadAndDivergenceWithoutMovingDevelop()
      throws Exception {
    Fixture mismatch = createFixture("sha-mismatch");
    String mismatchHead = head(mismatch.local());
    advanceRemote(mismatch, "remote mismatch\n", "remote mismatch");
    assertFailure(
        runNativeUpdater(mismatch.local(), mismatch.baseSha()),
        "fetched origin/develop SHA",
        mismatch.local(),
        mismatchHead,
        "A fetched ref different from the verified SHA was accepted.");

    Fixture ahead = createFixture("ahead");
    writeAndCommit(ahead.local(), "local ahead\n", "local ahead");
    String aheadHead = head(ahead.local());
    assertFailure(
        runNativeUpdater(ahead.local(), ahead.baseSha()),
        "local develop relationship",
        ahead.local(),
        aheadHead,
        "A locally ahead develop was accepted.");

    Fixture diverged = createFixture("diverged");
    writeAndCommit(diverged.local(), "local divergence\n", "local divergence");
    String divergedHead = head(diverged.local());
    String remoteSha = advanceRemote(diverged, "remote divergence\n", "remote divergence");
    assertFailure(
        runNativeUpdater(diverged.local(), remoteSha),
        "local develop relationship",
        diverged.local(),
        divergedHead,
        "A diverged local develop was accepted.");
  }

  @Test
  void nativeUpdaterRejectsFetchAndFastForwardFailuresWithoutMovingDevelop()
      throws Exception {
    Fixture fetchFailure = createFixture("fetch-failure");
    String fetchFailureHead = head(fetchFailure.local());
    runSuccessfully(
        fetchFailure.local(),
        "git",
        "remote",
        "set-url",
        "origin",
        temporaryDirectory.resolve("missing-origin.git").toString());
    assertFailure(
        runNativeUpdater(fetchFailure.local(), fetchFailure.baseSha()),
        "origin/develop fetch",
        fetchFailure.local(),
        fetchFailureHead,
        "A failed exact origin/develop fetch was accepted.");

    Fixture updateFailure = createFixture("update-failure");
    String updateFailureHead = head(updateFailure.local());
    String remoteSha =
        advanceRemote(updateFailure, "remote lock test\n", "remote lock test");
    Path developLock =
        updateFailure
            .local()
            .resolve(".git")
            .resolve("refs")
            .resolve("heads")
            .resolve("develop.lock");
    Files.writeString(developLock, "intentional test lock\n", StandardCharsets.UTF_8);
    assertFailure(
        runNativeUpdater(updateFailure.local(), remoteSha),
        "fast-forward update",
        updateFailure.local(),
        updateFailureHead,
        "A failed fast-forward ref update was accepted.");
  }

  private static void assertScriptContract(Path path, String script) {
    assertAll(
        () -> {
          for (String field : SIX_FIELDS) {
            assertTrue(
                script.contains(field),
                failure(
                    path,
                    "Updater failures no longer include " + field,
                    "Restore all six actionable diagnostic fields."));
          }
        },
        () ->
            assertTrue(
                script.contains("refs/heads/develop:refs/remotes/origin/develop")
                    && script.contains("refs/remotes/origin/develop")
                    && script.contains("git fetch --no-tags origin"),
                failure(
                    path,
                    "The exact origin/develop fetch contract is missing.",
                    "Hard-code the origin develop refspec and remote-tracking ref.")),
        () ->
            assertTrue(
                script.contains("git symbolic-ref --quiet --short HEAD")
                    && script.contains("git status --porcelain")
                    && script.contains("git diff --cached --quiet --exit-code")
                    && script.contains("git rev-list --left-right --count")
                    && script.contains("git merge --ff-only refs/remotes/origin/develop"),
                failure(
                    path,
                    "An exact-branch, clean-state, relationship, or ff-only check is missing.",
                    "Restore all fail-closed preconditions and the fixed fast-forward source.")),
        () ->
            assertTrue(
                script.contains("post-update branch")
                    && script.contains("post-update HEAD")
                    && script.contains("post-update Git state")
                    && script.contains("fast-forward update"),
                failure(
                    path,
                    "A fast-forward or postcondition failure path is missing.",
                    "Fail closed on update, final branch, HEAD, index, and worktree errors.")),
        () ->
            assertFalse(
                FORBIDDEN_GIT_COMMAND.matcher(script).find()
                    || FORBIDDEN_REMOTE_COMMAND.matcher(script).find()
                    || script.contains("Invoke-RestMethod")
                    || script.contains("Invoke-WebRequest")
                    || script.contains("curl http")
                    || script.contains("wget http"),
                failure(
                    path,
                    "The updater contains a forbidden repair, push, or GitHub operation.",
                    "Keep the updater limited to exact fetch, inspection, and ff-only update.")),
        () ->
            assertTrue(
                script.contains("docs/decisions/023-local-develop-fast-forward-policy.md")
                    && script.contains(
                        "docs/exec-plans/completed/020-local-develop-fast-forward-policy.md"),
                failure(
                    path,
                    "The updater no longer links its decision and completed plan authorities.",
                    "Restore links to decision 023 and completed plan 020.")),
        () ->
            assertFalse(
                VALIDATION_SHA_AS_SOURCE.matcher(script).find()
                    || script.contains("merge --ff-only $VerifiedDevelopSha")
                    || script.contains("merge --ff-only \"$verified_develop_sha\"")
                    || script.contains("fetch --no-tags origin $VerifiedDevelopSha")
                    || script.contains("fetch --no-tags origin \"$verified_develop_sha\""),
                failure(
                    path,
                    "The validation SHA is being used as a Git revision source.",
                    "Use the SHA only for equality checks; keep the Git source fixed.")));
  }

  private Fixture createFixture(String name) throws Exception {
    Path fixtureRoot = temporaryDirectory.resolve(name);
    Path origin = fixtureRoot.resolve("origin.git");
    Path seed = fixtureRoot.resolve("seed");
    final Path local = fixtureRoot.resolve("local");
    Files.createDirectories(fixtureRoot);
    Files.createDirectories(origin);
    Files.createDirectories(seed);

    runSuccessfully(origin, "git", "init", "--bare", "--initial-branch=develop");
    runSuccessfully(seed, "git", "init", "--initial-branch=develop");
    configureIdentity(seed);
    Path seedFile = seed.resolve("subject.txt");
    Files.writeString(seedFile, "base\n", StandardCharsets.UTF_8);
    runSuccessfully(seed, "git", "add", "subject.txt");
    runSuccessfully(seed, "git", "commit", "-m", "base");
    final String baseSha = head(seed);
    runSuccessfully(seed, "git", "remote", "add", "origin", origin.toString());
    runSuccessfully(seed, "git", "push", "-u", "origin", "develop");

    runSuccessfully(fixtureRoot, "git", "clone", origin.toString(), local.toString());
    configureIdentity(local);
    return new Fixture(origin, seed, local, local.resolve("subject.txt"), baseSha);
  }

  private static String advanceRemote(Fixture fixture, String content, String message)
      throws Exception {
    writeAndCommit(fixture.seed(), content, message);
    runSuccessfully(fixture.seed(), "git", "push", "origin", "develop");
    return head(fixture.seed());
  }

  private static void writeAndCommit(Path repository, String content, String message)
      throws Exception {
    Files.writeString(
        repository.resolve("subject.txt"), content, StandardCharsets.UTF_8);
    runSuccessfully(repository, "git", "add", "subject.txt");
    runSuccessfully(repository, "git", "commit", "-m", message);
  }

  private static void configureIdentity(Path repository) throws Exception {
    runSuccessfully(repository, "git", "config", "user.name", "StudyTrack Test");
    runSuccessfully(
        repository, "git", "config", "user.email", "studytrack@example.invalid");
  }

  private static void assertFailure(
      CommandResult result,
      String expectedLocation,
      Path repository,
      String beforeHead,
      String reason)
      throws Exception {
    assertFailureWithoutRepository(result, expectedLocation, reason);
    assertEquals(
        beforeHead,
        head(repository),
        failure(
            repository,
            reason + " The failed updater moved local develop.",
            "Fail without changing the local branch commit."));
  }

  private static void assertFailureWithoutRepository(
      CommandResult result, String expectedLocation, String reason) {
    assertNotEquals(
        0,
        result.exitCode(),
        failure(
            Path.of("scripts"),
            reason + " Output: " + result.output(),
            "Reject the scenario with a nonzero exit."));
    for (String field : SIX_FIELDS) {
      assertTrue(
          result.output().contains(field),
          failure(
              Path.of("scripts"),
              reason + " Missing field " + field + " Output: " + result.output(),
              "Route every failure through the six-field diagnostic."));
    }
    assertTrue(
        result.output().contains(expectedLocation),
        failure(
            Path.of("scripts"),
            reason + " Output: " + result.output(),
            "Identify the rejected invariant in Location."));
  }

  private static void assertClean(Path repository) throws Exception {
    assertEquals(
        "",
        runSuccessfully(repository, "git", "status", "--porcelain").output().trim(),
        failure(
            repository,
            "The updater left tracked, staged, or untracked changes.",
            "Keep the worktree and index clean after a successful update."));
  }

  private static String head(Path repository) throws Exception {
    return runSuccessfully(repository, "git", "rev-parse", "--verify", "HEAD")
        .output()
        .trim();
  }

  private static CommandResult runNativeUpdater(Path directory, String... arguments)
      throws Exception {
    List<String> command = new ArrayList<>();
    if (isWindows()) {
      command.add("powershell");
      command.add("-NoProfile");
      command.add("-ExecutionPolicy");
      command.add("Bypass");
      command.add("-File");
      command.add(POWERSHELL_UPDATER.toAbsolutePath().toString());
    } else {
      command.add("sh");
      command.add(POSIX_UPDATER.toAbsolutePath().toString());
    }
    command.addAll(List.of(arguments));
    return run(directory, command);
  }

  private static CommandResult runSuccessfully(
      Path directory, String... command) throws Exception {
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
            "A required local develop update contract file is missing.",
            "Restore " + file + "."));
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  private static String failure(Path location, String reason, String fix) {
    return """
        Local develop update invariant violated.
        Location: %s
        Invariant: local develop must update only from the exact verified origin/develop SHA,
        with exact develop branch, clean worktree/index, no local-only commits, and ff-only
        movement after GitHub PR integration.
        Reason: %s
        Fix: %s
        Recheck: .\\mvnw.cmd -Dtest=LocalDevelopUpdateTest test, then .\\mvnw.cmd verify.
        Authority: WORKFLOW.md, docs/decisions/023-local-develop-fast-forward-policy.md,
        docs/decisions/026-slim-agent-navigation.md, and
        docs/exec-plans/completed/020-local-develop-fast-forward-policy.md
        """
        .formatted(location, reason, fix);
  }

  private record CommandResult(int exitCode, String output) {}

  private record Fixture(
      Path origin, Path seed, Path local, Path trackedFile, String baseSha) {}
}
