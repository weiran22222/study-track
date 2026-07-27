package com.example.studytrack.bootstrap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

  @Test
  void listCommandSortsTasksAndFiltersByStatus(@TempDir Path temporaryDirectory)
      throws Exception {
    Path dataFile = temporaryDirectory.resolve("tasks.json");
    Files.writeString(
        dataFile,
        """
        {
          "nextId": 4,
          "tasks": [
            {"id": 3, "title": "编写 list 验收测试", "completed": false},
            {"id": 1, "title": "阅读 Harness", "completed": false},
            {"id": 2, "title": "完成 add 实验", "completed": true}
          ]
        }
        """,
        StandardCharsets.UTF_8);

    CommandResult all = execute("--data-file", dataFile.toString(), "list");

    assertEquals(0, all.exitCode());
    assertEquals(
        """
        [ ] 1 阅读 Harness
        [x] 2 完成 add 实验
        [ ] 3 编写 list 验收测试
        """
            .replace("\n", System.lineSeparator()),
        all.output());
    assertEquals("", all.error());

    CommandResult pending =
        execute("--data-file", dataFile.toString(), "list", "--status", "pending");

    assertEquals(0, pending.exitCode());
    assertEquals(
        """
        [ ] 1 阅读 Harness
        [ ] 3 编写 list 验收测试
        """
            .replace("\n", System.lineSeparator()),
        pending.output());
    assertEquals("", pending.error());

    CommandResult completed =
        execute("--data-file", dataFile.toString(), "list", "--status", "completed");

    assertEquals(0, completed.exitCode());
    assertEquals(
        "[x] 2 完成 add 实验" + System.lineSeparator(), completed.output());
    assertEquals("", completed.error());
  }

  @Test
  void listCommandReportsNoTasksForEmptyOrUnmatchedResults(@TempDir Path temporaryDirectory) {
    Path dataFile = temporaryDirectory.resolve("tasks.json");

    CommandResult empty = execute("--data-file", dataFile.toString(), "list");

    assertEquals(0, empty.exitCode());
    assertEquals("No tasks." + System.lineSeparator(), empty.output());
    assertEquals("", empty.error());

    execute("--data-file", dataFile.toString(), "add", "待办任务");
    CommandResult unmatched =
        execute("--data-file", dataFile.toString(), "list", "--status", "completed");

    assertEquals(0, unmatched.exitCode());
    assertEquals("No tasks." + System.lineSeparator(), unmatched.output());
    assertEquals("", unmatched.error());
  }

  @Test
  void unsupportedListStatusesReturnUsageExitCode(@TempDir Path temporaryDirectory) {
    String[] unsupportedStatuses = {"unknown", "PENDING"};

    for (int index = 0; index < unsupportedStatuses.length; index++) {
      Path dataFile = temporaryDirectory.resolve("tasks-" + index + ".json");
      CommandResult result =
          execute(
              "--data-file",
              dataFile.toString(),
              "list",
              "--status",
              unsupportedStatuses[index]);

      assertEquals(2, result.exitCode());
      assertEquals("", result.output());
      assertTrue(result.error().contains("Invalid value for option '--status'"));
      assertFalse(Files.exists(dataFile));
    }
  }

  @Test
  void completeCommandPersistsPendingTaskAndReportsSuccess(@TempDir Path temporaryDirectory)
      throws Exception {
    Path dataFile = temporaryDirectory.resolve("tasks.json");
    execute("--data-file", dataFile.toString(), "add", "学习幂等性");

    CommandResult result = execute("--data-file", dataFile.toString(), "complete", "1");

    assertEquals(0, result.exitCode());
    assertEquals("Completed task 1." + System.lineSeparator(), result.output());
    assertEquals("", result.error());
    assertTrue(Files.readString(dataFile, StandardCharsets.UTF_8).contains("\"completed\" : true"));

    CommandResult completed =
        execute("--data-file", dataFile.toString(), "list", "--status", "completed");
    assertEquals("[x] 1 学习幂等性" + System.lineSeparator(), completed.output());
  }

  @Test
  void completeCommandIsIdempotentWithoutChangingDataFile(@TempDir Path temporaryDirectory)
      throws Exception {
    Path dataFile = temporaryDirectory.resolve("tasks.json");
    execute("--data-file", dataFile.toString(), "add", "学习幂等性");
    execute("--data-file", dataFile.toString(), "complete", "1");
    final byte[] completedData = Files.readAllBytes(dataFile);

    CommandResult result = execute("--data-file", dataFile.toString(), "complete", "1");

    assertEquals(0, result.exitCode());
    assertEquals("Task 1 is already completed." + System.lineSeparator(), result.output());
    assertEquals("", result.error());
    assertArrayEquals(completedData, Files.readAllBytes(dataFile));
  }

  @Test
  void completeCommandReportsMissingTaskWithoutChangingExistingData(
      @TempDir Path temporaryDirectory) throws Exception {
    Path dataFile = temporaryDirectory.resolve("tasks.json");
    execute("--data-file", dataFile.toString(), "add", "已有任务");
    final byte[] originalData = Files.readAllBytes(dataFile);

    CommandResult result = execute("--data-file", dataFile.toString(), "complete", "99");

    assertEquals(2, result.exitCode());
    assertEquals("", result.output());
    assertEquals("Task 99 not found." + System.lineSeparator(), result.error());
    assertArrayEquals(originalData, Files.readAllBytes(dataFile));
  }

  @Test
  void completeCommandDoesNotCreateDataFileForMissingTask(@TempDir Path temporaryDirectory) {
    Path dataFile = temporaryDirectory.resolve("tasks.json");

    CommandResult result = execute("--data-file", dataFile.toString(), "complete", "99");

    assertEquals(2, result.exitCode());
    assertEquals("", result.output());
    assertEquals("Task 99 not found." + System.lineSeparator(), result.error());
    assertFalse(Files.exists(dataFile));
  }

  @Test
  void nonIntegerCompleteIdentifierReturnsUsageErrorWithoutCreatingDataFile(
      @TempDir Path temporaryDirectory) {
    Path dataFile = temporaryDirectory.resolve("tasks.json");

    CommandResult result =
        execute("--data-file", dataFile.toString(), "complete", "not-an-integer");

    assertEquals(2, result.exitCode());
    assertEquals("", result.output());
    assertTrue(result.error().contains("Invalid value for positional parameter"));
    assertFalse(Files.exists(dataFile));
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
