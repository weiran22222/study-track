package com.example.studytrack.application;

/** Signals that a task title violates the product constraints. */
public final class InvalidTaskTitleException extends RuntimeException {

  /** Product-facing validation message defined by SPEC.md. */
  public static final String MESSAGE =
      "Task title must contain between 1 and 200 characters.";

  /** Creates the exception with the product-facing validation message. */
  public InvalidTaskTitleException() {
    super(MESSAGE);
  }
}
