package com.example.studytrack.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

class DeleteCommandTest {

  @Test
  void persistenceFailureReturnsSoftwareExitCodeWithoutStackTrace() {
    TaskRepository repository = new FailingDeleteRepository();
    StudyTrackCommand rootCommand =
        new StudyTrackCommand(dataFile -> new StudyTaskService(repository));
    CommandLine commandLine = new CommandLine(rootCommand);
    StringWriter output = new StringWriter();
    StringWriter error = new StringWriter();
    commandLine.setOut(new PrintWriter(output));
    commandLine.setErr(new PrintWriter(error));

    int exitCode = commandLine.execute("delete", "8");

    assertEquals(1, exitCode);
    assertEquals("", output.toString());
    assertEquals(
        "Data file error: Injected delete persistence failure." + System.lineSeparator(),
        error.toString());
  }

  private static final class FailingDeleteRepository implements TaskRepository {

    @Override
    public StudyTask create(String title) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<StudyTask> findAll() {
      return List.of(new StudyTask(8, "删除失败", false));
    }

    @Override
    public void update(StudyTask task) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long taskId) {
      throw new TaskPersistenceException(
          "Injected delete persistence failure.", new IOException("Injected failure."));
    }
  }
}
