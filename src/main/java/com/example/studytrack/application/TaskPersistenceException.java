package com.example.studytrack.application;

/** Signals that the configured task data file could not be read or written safely. */
public final class TaskPersistenceException extends RuntimeException {

  /**
   * Creates a persistence exception.
   *
   * @param message safe user-facing context
   * @param cause underlying storage failure
   */
  public TaskPersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
