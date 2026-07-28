package com.example.studytrack.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.studytrack.domain.StudyTask;
import com.example.studytrack.infrastructure.persistence.JsonTaskRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UnicodeTitleFileProcessTest {

  @Test
  void realJavaProcessAccepts200SupplementaryCodePointsAndRejects201WithoutChange(
      @TempDir Path temporaryDirectory) throws Exception {
    Path dataFile = temporaryDirectory.resolve("tasks.json");
    Path titleFile = temporaryDirectory.resolve("title.txt");
    assertAsciiPath(dataFile);
    assertAsciiPath(titleFile);

    ProcessResult add =
        runProcess("--data-file", dataFile.toString(), "add", "Seed");
    assertEquals(0, add.exitCode());
    assertEquals("Created task 1: Seed" + System.lineSeparator(), add.output());
    assertEquals("", add.error());

    String validTitle = "😀".repeat(200);
    Files.writeString(titleFile, validTitle, StandardCharsets.UTF_8);
    ProcessResult validRename =
        runProcess(
            "--data-file",
            dataFile.toString(),
            "rename",
            "1",
            "--title-file",
            titleFile.toString());

    assertEquals(0, validRename.exitCode());
    assertEquals("Renamed task 1." + System.lineSeparator(), validRename.output());
    assertEquals("", validRename.error());
    List<StudyTask> tasks = new JsonTaskRepository(dataFile).findAll();
    assertEquals(1, tasks.size());
    String persistedTitle = tasks.getFirst().title();
    assertEquals(200, persistedTitle.codePointCount(0, persistedTitle.length()));
    assertEquals(validTitle, tasks.getFirst().title());

    final String hashBeforeInvalidRename = sha256(dataFile);
    Files.writeString(titleFile, "😀".repeat(201), StandardCharsets.UTF_8);
    ProcessResult invalidRename =
        runProcess(
            "--data-file",
            dataFile.toString(),
            "rename",
            "1",
            "--title-file",
            titleFile.toString());

    assertEquals(2, invalidRename.exitCode());
    assertEquals("", invalidRename.output());
    assertEquals(
        "Task title must contain between 1 and 200 characters."
            + System.lineSeparator(),
        invalidRename.error());
    assertEquals(hashBeforeInvalidRename, sha256(dataFile));
  }

  private static ProcessResult runProcess(String... arguments)
      throws IOException, InterruptedException {
    String javaExecutable =
        Path.of(System.getProperty("java.home"), "bin", executableName()).toString();
    String classPath =
        System.getProperty(
            "surefire.test.class.path", System.getProperty("java.class.path"));
    String[] command = new String[arguments.length + 4];
    command[0] = javaExecutable;
    command[1] = "-cp";
    command[2] = classPath;
    command[3] = StudyTrackApplication.class.getName();
    System.arraycopy(arguments, 0, command, 4, arguments.length);

    Process process = new ProcessBuilder(command).start();
    int exitCode = process.waitFor();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    return new ProcessResult(exitCode, output, error);
  }

  private static String executableName() {
    return System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
  }

  private static void assertAsciiPath(Path path) {
    assertTrue(
        path.toString().codePoints().allMatch(codePoint -> codePoint <= 0x7F),
        () -> "Subprocess acceptance path must be ASCII: " + path);
  }

  private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
    byte[] digest =
        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
    return HexFormat.of().formatHex(digest);
  }

  private record ProcessResult(int exitCode, String output, String error) {}
}
