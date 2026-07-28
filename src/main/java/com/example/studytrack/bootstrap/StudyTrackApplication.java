package com.example.studytrack.bootstrap;

import com.example.studytrack.application.StudyTaskService;
import com.example.studytrack.cli.StudyTrackCommand;
import com.example.studytrack.infrastructure.input.Utf8TitleFileReader;
import com.example.studytrack.infrastructure.persistence.JsonTaskRepository;
import java.nio.file.Path;
import picocli.CommandLine;

/** Composition root and executable entry point for StudyTrack. */
public final class StudyTrackApplication {

  private StudyTrackApplication() {}

  /**
   * Starts the command-line application.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    int exitCode = createCommandLine().execute(args);
    System.exit(exitCode);
  }

  /**
   * Creates a fully composed StudyTrack command line.
   *
   * @return configured command line
   */
  public static CommandLine createCommandLine() {
    StudyTrackCommand rootCommand =
        new StudyTrackCommand(
            dataFile ->
                new StudyTaskService(new JsonTaskRepository(Path.of(dataFile))),
            new Utf8TitleFileReader());
    return new CommandLine(rootCommand);
  }
}
