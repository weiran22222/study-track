package com.example.studytrack.application.port;

import com.example.studytrack.domain.StudyTask;

/** Persistence boundary used by learning-task use cases. */
public interface TaskRepository {

  /**
   * Creates and persists a pending task with the next available identifier.
   *
   * @param title validated task title
   * @return the persisted task
   */
  StudyTask create(String title);
}
