package com.example.studytrack.application;

/** Signals that a requested learning task does not exist. */
public final class TaskNotFoundException extends RuntimeException {

  /**
   * Creates an exception for a missing task.
   *
   * @param taskId requested task identifier
   */
  public TaskNotFoundException(long taskId) {
    super("Task " + taskId + " not found.");
  }
}
