package com.example.studytrack.cli;

import com.example.studytrack.application.StudyTaskService;
import com.example.studytrack.application.TaskNotFoundException;
import com.example.studytrack.application.TaskPersistenceException;
import com.example.studytrack.domain.StudyTask;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

/** Shows one learning task. */
@Command(name = "show", description = "Show a learning task.")
public final class ShowCommand implements Callable<Integer> {

  @ParentCommand private StudyTrackCommand parentCommand;

  @Parameters(index = "0", paramLabel = "ID", description = "Learning task identifier.")
  private long taskId;

  @Spec private CommandSpec commandSpec;

  @Override
  public Integer call() {
    StudyTaskService service = parentCommand.serviceFactory().create(parentCommand.dataFile());
    try {
      StudyTask task = service.showTask(taskId);
      String marker = task.completed() ? "x" : " ";
      commandSpec
          .commandLine()
          .getOut()
          .printf("[%s] %d %s%n", marker, task.id(), task.title());
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
