package com.example.studytrack.cli;

import com.example.studytrack.application.InvalidSearchTextException;
import com.example.studytrack.application.StudyTaskService;
import com.example.studytrack.application.TaskPersistenceException;
import com.example.studytrack.domain.StudyTask;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.TypeConversionException;

/** Lists learning tasks. */
@Command(name = "list", description = "List learning tasks.")
public final class ListCommand implements Callable<Integer> {

  @ParentCommand private StudyTrackCommand parentCommand;

  @Option(
      names = "--status",
      paramLabel = "STATUS",
      description = "Filter tasks by status: ${COMPLETION-CANDIDATES}.",
      converter = TaskStatusConverter.class)
  private TaskStatus status;

  @Option(
      names = "--contains",
      paramLabel = "TEXT",
      description = "Filter tasks whose saved title contains the text.")
  private String contains;

  @Spec private CommandSpec commandSpec;

  @Override
  public Integer call() {
    StudyTaskService service = parentCommand.serviceFactory().create(parentCommand.dataFile());
    try {
      List<StudyTask> tasks =
          (contains == null ? service.listTasks() : service.listTasks(contains))
              .stream()
              .filter(this::matchesSelectedStatus)
              .toList();
      if (tasks.isEmpty()) {
        commandSpec.commandLine().getOut().println("No tasks.");
        return ExitCode.OK;
      }

      for (StudyTask task : tasks) {
        String marker = task.completed() ? "x" : " ";
        commandSpec
            .commandLine()
            .getOut()
            .printf("[%s] %d %s%n", marker, task.id(), task.title());
      }
      return ExitCode.OK;
    } catch (InvalidSearchTextException exception) {
      commandSpec.commandLine().getErr().println(exception.getMessage());
      return ExitCode.USAGE;
    } catch (TaskPersistenceException exception) {
      commandSpec.commandLine().getErr().println("Data file error: " + exception.getMessage());
      return ExitCode.SOFTWARE;
    }
  }

  private boolean matchesSelectedStatus(StudyTask task) {
    return status == null || task.completed() == status.completed;
  }

  /** List status values accepted by the command-line protocol. */
  public enum TaskStatus {
    PENDING(false, "pending"),
    COMPLETED(true, "completed");

    private final boolean completed;
    private final String cliValue;

    TaskStatus(boolean completed, String cliValue) {
      this.completed = completed;
      this.cliValue = cliValue;
    }

    @Override
    public String toString() {
      return cliValue;
    }
  }

  /** Strictly converts the lowercase status values defined by the product specification. */
  public static final class TaskStatusConverter implements ITypeConverter<TaskStatus> {

    @Override
    public TaskStatus convert(String value) {
      return switch (value) {
        case "pending" -> TaskStatus.PENDING;
        case "completed" -> TaskStatus.COMPLETED;
        default -> throw new TypeConversionException("expected one of [pending, completed]");
      };
    }
  }
}
