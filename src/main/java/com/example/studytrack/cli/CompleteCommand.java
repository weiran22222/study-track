package com.example.studytrack.cli;

import com.example.studytrack.application.CompleteTaskResult;
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

/** Completes one learning task. */
@Command(name = "complete", description = "Complete a learning task.")
public final class CompleteCommand implements Callable<Integer> {

  @ParentCommand private StudyTrackCommand parentCommand;

  @Parameters(index = "0", paramLabel = "ID", description = "Learning task identifier.")
  private long taskId;

  @Spec private CommandSpec commandSpec;

  @Override
  public Integer call() {
    StudyTaskService service = parentCommand.serviceFactory().create(parentCommand.dataFile());
    try {
      CompleteTaskResult result = service.completeTask(taskId);
      if (result == CompleteTaskResult.COMPLETED) {
        commandSpec.commandLine().getOut().printf("Completed task %d.%n", taskId);
      } else {
        commandSpec
            .commandLine()
            .getOut()
            .printf("Task %d is already completed.%n", taskId);
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
