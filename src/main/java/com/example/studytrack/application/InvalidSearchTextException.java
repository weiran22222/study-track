package com.example.studytrack.application;

/** Signals that list search text violates the product constraints. */
public final class InvalidSearchTextException extends RuntimeException {

  /** Product-facing validation message defined by SPEC.md. */
  public static final String MESSAGE =
      "Search text must contain between 1 and 200 characters.";

  /** Creates the exception with the product-facing validation message. */
  public InvalidSearchTextException() {
    super(MESSAGE);
  }
}
