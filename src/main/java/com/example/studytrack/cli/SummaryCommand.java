package com.example.studytrack.cli;

import com.example.studytrack.application.StudyTaskService;
import com.example.studytrack.application.TaskPersistenceException;
import com.example.studytrack.application.TaskSummary;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

/** Summarizes learning tasks by completion state. */
@Command(name = "summary", description = "Summarize learning tasks.")
public final class SummaryCommand implements Callable<Integer> {

  @ParentCommand private StudyTrackCommand parentCommand;

  @Spec private CommandSpec commandSpec;

  @Override
  public Integer call() {
    StudyTaskService service = parentCommand.serviceFactory().create(parentCommand.dataFile());
    try {
      TaskSummary summary = service.summarizeTasks();
      commandSpec.commandLine().getOut().printf("Total: %d%n", summary.total());
      commandSpec.commandLine().getOut().printf("Pending: %d%n", summary.pending());
      commandSpec.commandLine().getOut().printf("Completed: %d%n", summary.completed());
      return ExitCode.OK;
    } catch (TaskPersistenceException exception) {
      commandSpec.commandLine().getErr().println("Data file error: " + exception.getMessage());
      return ExitCode.SOFTWARE;
    }
  }
}
