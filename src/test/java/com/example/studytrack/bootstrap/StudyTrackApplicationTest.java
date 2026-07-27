package com.example.studytrack.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class StudyTrackApplicationTest {

  @Test
  void noArgumentsPrintUsageAndSucceed() {
    StringWriter output = new StringWriter();
    CommandLine commandLine = StudyTrackApplication.createCommandLine();
    commandLine.setOut(new PrintWriter(output));

    int exitCode = commandLine.execute();

    assertEquals(0, exitCode);
    assertTrue(output.toString().contains("Usage: study-track"));
  }

  @Test
  void addCommandPersistsTrimmedTaskAndIncrementsId(@TempDir Path temporaryDirectory)
      throws Exception {
    Path dataFile = temporaryDirectory.resolve("tasks.json");

    CommandResult first = execute("--data-file", dataFile.toString(), "add", "  阅读 Harness  ");

    assertEquals(0, first.exitCode());
    assertEquals("Created task 1: 阅读 Harness" + System.lineSeparator(), first.output());
    assertEquals("", first.error());

    CommandResult second = execute("--data-file", dataFile.toString(), "add", "编写验收测试");

    assertEquals(0, second.exitCode());
    assertEquals("Created task 2: 编写验收测试" + System.lineSeparator(), second.output());
    assertEquals("", second.error());

    String json = Files.readString(dataFile, StandardCharsets.UTF_8);
    assertTrue(json.contains("\"nextId\" : 3"));
    assertTrue(json.contains("\"id\" : 1"));
    assertTrue(json.contains("\"title\" : \"阅读 Harness\""));
    assertTrue(json.contains("\"completed\" : false"));
    assertTrue(json.contains("\"id\" : 2"));
  }

  @Test
  void invalidTitlesReturnUsageExitCodeAndDoNotCreateDataFile(@TempDir Path temporaryDirectory) {
    String[] invalidTitles = {"", "   ", "😀".repeat(201)};

    for (int index = 0; index < invalidTitles.length; index++) {
      Path dataFile = temporaryDirectory.resolve("tasks-" + index + ".json");
      CommandResult result =
          execute("--data-file", dataFile.toString(), "add", invalidTitles[index]);

      assertEquals(2, result.exitCode());
      assertEquals("", result.output());
      assertEquals(
          "Task title must contain between 1 and 200 characters." + System.lineSeparator(),
          result.error());
      assertFalse(Files.exists(dataFile));
    }
  }

  private static CommandResult execute(String... arguments) {
    StringWriter output = new StringWriter();
    StringWriter error = new StringWriter();
    CommandLine commandLine = StudyTrackApplication.createCommandLine();
    commandLine.setOut(new PrintWriter(output));
    commandLine.setErr(new PrintWriter(error));

    int exitCode = commandLine.execute(arguments);
    return new CommandResult(exitCode, output.toString(), error.toString());
  }

  private record CommandResult(int exitCode, String output, String error) {}
}
