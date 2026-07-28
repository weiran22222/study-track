package com.example.studytrack.application.port;

/** Reads a task title from an external text location. */
@FunctionalInterface
public interface TitleFileReader {

  /**
   * Reads the complete title text.
   *
   * @param location opaque location supplied by the input adapter
   * @return complete, undecorated title text
   */
  String read(String location);
}
