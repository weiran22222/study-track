package com.example.studytrack.cli;

import com.example.studytrack.application.ReopenTaskResult;
import com.example.studytrack.application.StudyTaskService;
import com.example.studytrack.application.TaskNotFoundException;
import com.example.studytrack.application.TaskPersistenceException;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

/** Reopens one completed learning task. */
@Command(name = "reopen", description = "Reopen a completed learning task.")
public final class ReopenCommand implements Callable<Integer> {

  @ParentCommand private StudyTrackCommand parentCommand;

  @Parameters(index = "0", paramLabel = "ID", description = "Learning task identifier.")
  private long taskId;

  @Spec private CommandSpec commandSpec;

  @Override
  public Integer call() {
    StudyTaskService service = parentCommand.serviceFactory().create(parentCommand.dataFile());
    try {
      ReopenTaskResult result = service.reopenTask(taskId);
      if (result == ReopenTaskResult.REOPENED) {
        commandSpec.commandLine().getOut().printf("Reopened task %d.%n", taskId);
      } else {
        commandSpec.commandLine().getOut().printf("Task %d is already pending.%n", taskId);
      }
      return ExitCode.OK;
    } catch (TaskNotFoundException exception) {
      commandSpec.commandLine().getErr().println(exception.getMessage());
      return ExitCode.USAGE;
    } catch (TaskPersistenceException exception) {
      commandSpec.commandLine().getErr().println("Data file error: " + exception.getMessage());
      return ExitCode.SOFTWARE;
    }
  }
}
