package com.example.studytrack.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class StudyTrackApplicationTest {

  @Test
  void noArgumentsPrintUsageAndSucceed() {
    StringWriter output = new StringWriter();
    CommandLine commandLine = new CommandLine(new StudyTrackApplication());
    commandLine.setOut(new PrintWriter(output));

    int exitCode = commandLine.execute();

    assertEquals(0, exitCode);
    assertTrue(output.toString().contains("Usage: study-track"));
  }
}
