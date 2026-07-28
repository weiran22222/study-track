package com.example.studytrack.cli;

import com.example.studytrack.application.InvalidTaskTitleException;
import com.example.studytrack.application.RenameTaskResult;
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

/** Renames one learning task. */
@Command(name = "rename", description = "Rename a learning task.")
public final class RenameCommand implements Callable<Integer> {

  @ParentCommand private StudyTrackCommand parentCommand;

  @Parameters(index = "0", paramLabel = "ID", description = "Learning task identifier.")
  private long taskId;

  @Parameters(index = "1", paramLabel = "NEW-TITLE", description = "New learning task title.")
  private String title;

  @Spec private CommandSpec commandSpec;

  @Override
  public Integer call() {
    StudyTaskService service = parentCommand.serviceFactory().create(parentCommand.dataFile());
    try {
      RenameTaskResult result = service.renameTask(taskId, title);
      if (result == RenameTaskResult.RENAMED) {
        commandSpec.commandLine().getOut().printf("Renamed task %d.%n", taskId);
      } else {
        commandSpec
            .commandLine()
            .getOut()
            .printf("Task %d already has that title.%n", taskId);
      }
      return ExitCode.OK;
    } catch (InvalidTaskTitleException | TaskNotFoundException exception) {
      commandSpec.commandLine().getErr().println(exception.getMessage());
      return ExitCode.USAGE;
    } catch (TaskPersistenceException exception) {
      commandSpec.commandLine().getErr().println("Data file error: " + exception.getMessage());
      return ExitCode.SOFTWARE;
    }
  }
}
