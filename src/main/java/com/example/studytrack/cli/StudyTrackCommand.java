package com.example.studytrack.cli;

import java.util.Objects;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** Root command for the StudyTrack command-line interface. */
@Command(
    name = "study-track",
    mixinStandardHelpOptions = true,
    version = "study-track 0.1.0",
    description = "Track local learning tasks.",
    subcommands = {
      AddCommand.class,
      ListCommand.class,
      CompleteCommand.class,
      ShowCommand.class,
      SummaryCommand.class,
      DeleteCommand.class,
      RenameCommand.class
    })
public final class StudyTrackCommand implements Callable<Integer> {

  private final StudyTaskServiceFactory serviceFactory;

  @Option(
      names = "--data-file",
      defaultValue = "study-tasks.json",
      description = "JSON file used to store learning tasks.")
  private String dataFile;

  @Spec private CommandSpec commandSpec;

  /**
   * Creates the root command.
   *
   * @param serviceFactory factory supplied by the bootstrap layer
   */
  public StudyTrackCommand(StudyTaskServiceFactory serviceFactory) {
    this.serviceFactory = Objects.requireNonNull(serviceFactory);
  }

  @Override
  public Integer call() {
    commandSpec.commandLine().usage(commandSpec.commandLine().getOut());
    return ExitCode.OK;
  }

  StudyTaskServiceFactory serviceFactory() {
    return serviceFactory;
  }

  String dataFile() {
    return dataFile;
  }
}
