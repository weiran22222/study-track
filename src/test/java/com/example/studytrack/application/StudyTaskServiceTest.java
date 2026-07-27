package com.example.studytrack.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.studytrack.application.port.TaskRepository;
import com.example.studytrack.domain.StudyTask;
import java.util.List;
import org.junit.jupiter.api.Test;

class StudyTaskServiceTest {

  @Test
  void stripsTitleBeforeCreatingTask() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    StudyTaskService service = new StudyTaskService(repository);

    StudyTask task = service.addTask("  阅读 Harness  ");

    assertEquals(1, task.id());
    assertEquals("阅读 Harness", task.title());
    assertFalse(task.completed());
    assertEquals("阅读 Harness", repository.lastTitle);
    assertEquals(1, repository.createCalls);
  }

  @Test
  void rejectsBlankTitleWithoutCallingRepository() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    StudyTaskService service = new StudyTaskService(repository);

    InvalidTaskTitleException exception =
        assertThrows(InvalidTaskTitleException.class, () -> service.addTask(" \t "));

    assertEquals(
        "Task title must contain between 1 and 200 characters.", exception.getMessage());
    assertEquals(0, repository.createCalls);
  }

  @Test
  void acceptsExactlyTwoHundredUnicodeCodePoints() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    StudyTaskService service = new StudyTaskService(repository);
    String title = "😀".repeat(200);

    StudyTask task = service.addTask(title);

    assertEquals(title, task.title());
    assertEquals(1, repository.createCalls);
  }

  @Test
  void rejectsMoreThanTwoHundredUnicodeCodePointsWithoutCallingRepository() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    StudyTaskService service = new StudyTaskService(repository);
    String title = "😀".repeat(201);

    assertThrows(InvalidTaskTitleException.class, () -> service.addTask(title));

    assertEquals(0, repository.createCalls);
  }

  @Test
  void listsTasksInAscendingIdentifierOrder() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks =
        List.of(
            new StudyTask(3, "第三项", false),
            new StudyTask(1, "第一项", true),
            new StudyTask(2, "第二项", false));
    StudyTaskService service = new StudyTaskService(repository);

    List<StudyTask> tasks = service.listTasks();

    assertEquals(
        List.of(
            new StudyTask(1, "第一项", true),
            new StudyTask(2, "第二项", false),
            new StudyTask(3, "第三项", false)),
        tasks);
    assertEquals(1, repository.findAllCalls);
  }

  private static final class RecordingTaskRepository implements TaskRepository {

    private int createCalls;
    private int findAllCalls;
    private String lastTitle;
    private List<StudyTask> tasks = List.of();

    @Override
    public StudyTask create(String title) {
      createCalls++;
      lastTitle = title;
      return new StudyTask(createCalls, title, false);
    }

    @Override
    public List<StudyTask> findAll() {
      findAllCalls++;
      return tasks;
    }
  }
}
