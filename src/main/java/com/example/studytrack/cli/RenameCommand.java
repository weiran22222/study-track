package com.example.studytrack.cli;

import com.example.studytrack.application.InvalidTaskTitleException;
import com.example.studytrack.application.RenameTaskResult;
import com.example.studytrack.application.StudyTaskService;
import com.example.studytrack.application.TaskNotFoundException;
import com.example.studytrack.application.TaskPersistenceException;
import com.example.studytrack.application.TitleFileException;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IParameterPreprocessor;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

/** Renames one learning task. */
@Command(
    name = "rename",
    description = "Rename a learning task.",
    preprocessor = RenameCommand.RenameArgumentPreprocessor.class)
public final class RenameCommand implements Callable<Integer> {

  @ParentCommand private StudyTrackCommand parentCommand;

  @Parameters(index = "0", paramLabel = "ID", description = "Learning task identifier.")
  private long taskId;

  @Parameters(
      index = "1",
      arity = "0..1",
      paramLabel = "NEW-TITLE",
      description = "New learning task title.")
  private String inlineTitle;

  @Option(
      names = "--title-file",
      paramLabel = "PATH",
      description = "UTF-8 file containing the new learning task title.")
  private String titleFile;

  @Spec private CommandSpec commandSpec;

  @Override
  public Integer call() {
    String title;
    try {
      title =
          titleFile == null
              ? inlineTitle
              : parentCommand.titleFileReader().read(titleFile);
    } catch (TitleFileException exception) {
      commandSpec.commandLine().getErr().println("Title file error: " + exception.getMessage());
      return ExitCode.SOFTWARE;
    }

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

  /** Validates the title-source shape before Picocli binds or converts any arguments. */
  public static final class RenameArgumentPreprocessor implements IParameterPreprocessor {

    @Override
    public boolean preprocess(
        Stack<String> args,
        CommandSpec commandSpec,
        ArgSpec argSpec,
        Map<String, Object> info) {
      Stack<String> remaining = new Stack<>();
      remaining.addAll(args);
      int positionalCount = 0;
      boolean hasTitleFile = false;
      boolean endOfOptions = false;

      while (!remaining.empty()) {
        String argument = remaining.pop();
        if (!endOfOptions && "--".equals(argument)) {
          endOfOptions = true;
        } else if (!endOfOptions && "--title-file".equals(argument)) {
          hasTitleFile = true;
          if (!remaining.empty()) {
            remaining.pop();
          }
        } else if (!endOfOptions && argument.startsWith("--title-file=")) {
          hasTitleFile = true;
        } else {
          positionalCount++;
        }
      }

      boolean hasInlineTitle = positionalCount >= 2;
      if (hasTitleFile && hasInlineTitle) {
        throw new ParameterException(
            commandSpec.commandLine(),
            "NEW-TITLE and --title-file are mutually exclusive.");
      }
      if (!hasTitleFile && !hasInlineTitle) {
        throw new ParameterException(
            commandSpec.commandLine(),
            "Missing required parameter: specify NEW-TITLE or --title-file.");
      }
      return false;
    }
  }
}
