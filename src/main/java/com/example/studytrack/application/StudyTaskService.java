package com.example.studytrack.application;

import com.example.studytrack.application.port.TaskRepository;
import com.example.studytrack.domain.StudyTask;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Application service for learning-task use cases. */
public final class StudyTaskService {

  private static final int MAXIMUM_TITLE_CODE_POINTS = 200;

  private final TaskRepository repository;

  /**
   * Creates the service.
   *
   * @param repository persistence port
   */
  public StudyTaskService(TaskRepository repository) {
    this.repository = Objects.requireNonNull(repository);
  }

  /**
   * Validates and adds a pending learning task.
   *
   * @param rawTitle title supplied by the user
   * @return the persisted task
   * @throws InvalidTaskTitleException when the stripped title is outside the allowed range
   */
  public StudyTask addTask(String rawTitle) {
    if (rawTitle == null) {
      throw new InvalidTaskTitleException();
    }

    String title = rawTitle.strip();
    int codePointCount = title.codePointCount(0, title.length());
    if (codePointCount < 1 || codePointCount > MAXIMUM_TITLE_CODE_POINTS) {
      throw new InvalidTaskTitleException();
    }

    return repository.create(title);
  }

  /**
   * Lists learning tasks in ascending identifier order.
   *
   * @return persisted tasks ordered by identifier
   */
  public List<StudyTask> listTasks() {
    return repository.findAll().stream()
        .sorted(Comparator.comparingLong(StudyTask::id))
        .toList();
  }
}
