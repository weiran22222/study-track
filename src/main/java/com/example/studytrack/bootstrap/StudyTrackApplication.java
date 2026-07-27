package com.example.studytrack.bootstrap;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Composition root and executable entry point for StudyTrack. */
@Command(
    name = "study-track",
    mixinStandardHelpOptions = true,
    version = "study-track 0.1.0",
    description = "Track local learning tasks.")
public final class StudyTrackApplication implements Callable<Integer> {

  @Spec private CommandSpec commandSpec;

  /**
   * Starts the command-line application.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    int exitCode = new CommandLine(new StudyTrackApplication()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() {
    commandSpec.commandLine().usage(commandSpec.commandLine().getOut());
    return ExitCode.OK;
  }
}
