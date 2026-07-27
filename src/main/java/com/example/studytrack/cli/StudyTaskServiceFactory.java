package com.example.studytrack.cli;

import com.example.studytrack.application.StudyTaskService;

/** Creates an application service for the data file selected by the CLI. */
@FunctionalInterface
public interface StudyTaskServiceFactory {

  /**
   * Creates a service backed by the selected data file.
   *
   * @param dataFile CLI data-file value
   * @return configured application service
   */
  StudyTaskService create(String dataFile);
}
