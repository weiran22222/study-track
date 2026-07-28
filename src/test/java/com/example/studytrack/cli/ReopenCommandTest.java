package com.example.studytrack.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.studytrack.application.StudyTaskService;
import com.example.studytrack.application.TaskPersistenceException;
import com.example.studytrack.application.port.TaskRepository;
import com.example.studytrack.domain.StudyTask;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class ReopenCommandTest {

  @Test
  void successfulReopenPrintsConfirmationAndPersistsPendingTask() {
    RecordingTaskRepository repository =
        new RecordingTaskRepository(List.of(new StudyTask(8, "Reopen me", true)));

    CommandResult result = execute(repository, "reopen", "8");

    assertEquals(0, result.exitCode());
    assertEquals("Reopened task 8." + System.lineSeparator(), result.output());
    assertEquals("", result.error());
    assertEquals(new StudyTask(8, "Reopen me", false), repository.updatedTask);
    assertEquals(1, repository.findAllCalls);
    assertEquals(1, repository.updateCalls);
  }

  @Test
  void alreadyPendingTaskIsSuccessfulWithoutPersisting() {
    RecordingTaskRepository repository =
        new RecordingTaskRepository(List.of(new StudyTask(8, "Pending", false)));

    CommandResult result = execute(repository, "reopen", "8");

    assertEquals(0, result.exitCode());
    assertEquals("Task 8 is already pending." + System.lineSeparator(), result.output());
    assertEquals("", result.error());
    assertEquals(1, repository.findAllCalls);
    assertEquals(0, repository.updateCalls);
  }

  @Test
  void missingTaskReturnsUsageWithoutPersisting() {
    RecordingTaskRepository repository =
        new RecordingTaskRepository(List.of(new StudyTask(1, "Existing", true)));

    CommandResult result = execute(repository, "reopen", "99");

    assertEquals(2, result.exitCode());
    assertEquals("", result.output());
    assertEquals("Task 99 not found." + System.lineSeparator(), result.error());
    assertEquals(1, repository.findAllCalls);
    assertEquals(0, repository.updateCalls);
  }

  @Test
  void persistenceFailureReturnsSoftwareExitCodeWithoutStackTrace() {
    RecordingTaskRepository repository =
        new RecordingTaskRepository(List.of(new StudyTask(8, "Reopen me", true)));
    repository.failUpdate = true;

    CommandResult result = execute(repository, "reopen", "8");

    assertEquals(1, result.exitCode());
    assertEquals("", result.output());
    assertEquals(
        "Data file error: Injected reopen persistence failure." + System.lineSeparator(),
        result.error());
    assertFalse(result.error().contains("Exception"));
    assertFalse(result.error().contains("\tat "));
  }

  @Test
  void nonIntegerIdIsRejectedBeforeCreatingServiceOrAccessingData() {
    int[] factoryCalls = {0};
    StudyTrackCommand rootCommand =
        new StudyTrackCommand(
            dataFile -> {
              factoryCalls[0]++;
              return new StudyTaskService(new RecordingTaskRepository(List.of()));
            },
            location -> {
              throw new AssertionError("Title-file reader must not be called.");
            });

    CommandResult result = execute(rootCommand, "reopen", "not-an-integer");

    assertEquals(2, result.exitCode());
    assertEquals("", result.output());
    assertTrue(result.error().contains("Invalid value for positional parameter"));
    assertEquals(0, factoryCalls[0]);
  }

  private static CommandResult execute(TaskRepository repository, String... arguments) {
    return execute(
        new StudyTrackCommand(
            dataFile -> new StudyTaskService(repository),
            location -> {
              throw new AssertionError("Title-file reader must not be called.");
            }),
        arguments);
  }

  private static CommandResult execute(StudyTrackCommand rootCommand, String... arguments) {
    CommandLine commandLine = new CommandLine(rootCommand);
    StringWriter output = new StringWriter();
    StringWriter error = new StringWriter();
    commandLine.setOut(new PrintWriter(output));
    commandLine.setErr(new PrintWriter(error));

    int exitCode = commandLine.execute(arguments);
    return new CommandResult(exitCode, output.toString(), error.toString());
  }

  private record CommandResult(int exitCode, String output, String error) {}

  private static final class RecordingTaskRepository implements TaskRepository {

    private final List<StudyTask> tasks;
    private int findAllCalls;
    private int updateCalls;
    private StudyTask updatedTask;
    private boolean failUpdate;

    private RecordingTaskRepository(List<StudyTask> tasks) {
      this.tasks = tasks;
    }

    @Override
    public StudyTask create(String title) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<StudyTask> findAll() {
      findAllCalls++;
      return tasks;
    }

    @Override
    public void update(StudyTask task) {
      updateCalls++;
      if (failUpdate) {
        throw new TaskPersistenceException(
            "Injected reopen persistence failure.", new IOException("Injected failure."));
      }
      updatedTask = task;
    }

    @Override
    public void delete(long taskId) {
      throw new UnsupportedOperationException();
    }
  }
}
