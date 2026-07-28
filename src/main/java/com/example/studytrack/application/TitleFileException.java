package com.example.studytrack.application;

/** Reports that a title file could not be read as strict UTF-8. */
public final class TitleFileException extends RuntimeException {

  /**
   * Creates a stable title-file error.
   *
   * @param location title-file location supplied by the caller
   * @param cause underlying input failure
   */
  public TitleFileException(String location, Throwable cause) {
    super("Unable to read UTF-8 title file: " + location, cause);
  }
}
