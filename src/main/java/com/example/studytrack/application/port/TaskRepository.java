package com.example.studytrack.application.port;

import com.example.studytrack.domain.StudyTask;
import java.util.List;

/** Persistence boundary used by learning-task use cases. */
public interface TaskRepository {

  /**
   * Creates and persists a pending task with the next available identifier.
   *
   * @param title validated task title
   * @return the persisted task
   */
  StudyTask create(String title);

  /**
   * Reads all persisted tasks.
   *
   * @return persisted tasks
   */
  List<StudyTask> findAll();

  /**
   * Persists a changed version of an existing task.
   *
   * @param task changed task whose identifier already exists
   */
  void update(StudyTask task);
}
