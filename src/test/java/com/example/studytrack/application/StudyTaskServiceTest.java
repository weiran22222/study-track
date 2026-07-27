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

  @Test
  void showsTaskWithRequestedIdentifierWithoutPersisting() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks =
        List.of(
            new StudyTask(1, "First task", false),
            new StudyTask(2, "Requested task", true));
    StudyTaskService service = new StudyTaskService(repository);

    StudyTask task = service.showTask(2);

    assertEquals(new StudyTask(2, "Requested task", true), task);
    assertEquals(1, repository.findAllCalls);
    assertEquals(0, repository.createCalls);
    assertEquals(0, repository.updateCalls);
  }

  @Test
  void showingMissingTaskFailsWithoutPersisting() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks = List.of(new StudyTask(1, "Existing task", false));
    StudyTaskService service = new StudyTaskService(repository);

    TaskNotFoundException exception =
        assertThrows(TaskNotFoundException.class, () -> service.showTask(99));

    assertEquals("Task 99 not found.", exception.getMessage());
    assertEquals(1, repository.findAllCalls);
    assertEquals(0, repository.createCalls);
    assertEquals(0, repository.updateCalls);
  }

  @Test
  void completesPendingTaskAndPersistsTransition() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks = List.of(new StudyTask(1, "学习幂等性", false));
    StudyTaskService service = new StudyTaskService(repository);

    CompleteTaskResult result = service.completeTask(1);

    assertEquals(CompleteTaskResult.COMPLETED, result);
    assertEquals(new StudyTask(1, "学习幂等性", true), repository.updatedTask);
    assertEquals(1, repository.findAllCalls);
    assertEquals(1, repository.updateCalls);
  }

  @Test
  void completingAlreadyCompletedTaskDoesNotPersist() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks = List.of(new StudyTask(1, "学习幂等性", true));
    StudyTaskService service = new StudyTaskService(repository);

    CompleteTaskResult result = service.completeTask(1);

    assertEquals(CompleteTaskResult.ALREADY_COMPLETED, result);
    assertEquals(1, repository.findAllCalls);
    assertEquals(0, repository.updateCalls);
  }

  @Test
  void completingMissingTaskFailsWithoutPersisting() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks = List.of(new StudyTask(1, "已有任务", false));
    StudyTaskService service = new StudyTaskService(repository);

    TaskNotFoundException exception =
        assertThrows(TaskNotFoundException.class, () -> service.completeTask(99));

    assertEquals("Task 99 not found.", exception.getMessage());
    assertEquals(1, repository.findAllCalls);
    assertEquals(0, repository.updateCalls);
  }

  private static final class RecordingTaskRepository implements TaskRepository {

    private int createCalls;
    private int findAllCalls;
    private int updateCalls;
    private String lastTitle;
    private List<StudyTask> tasks = List.of();
    private StudyTask updatedTask;

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

    @Override
    public void update(StudyTask task) {
      updateCalls++;
      updatedTask = task;
    }
  }
}
