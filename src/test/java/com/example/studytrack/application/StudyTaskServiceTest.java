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
  void stripsSearchTextAndMatchesSavedTitlesLiterallyWithCaseSensitivity() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks =
        List.of(
            new StudyTask(5, "Harness .* details", false),
            new StudyTask(1, "literal .* marker", true),
            new StudyTask(3, "harness .* lowercase", false),
            new StudyTask(2, "Harness plain", false));
    StudyTaskService service = new StudyTaskService(repository);

    List<StudyTask> literalMatches = service.listTasks("  .*  ");
    List<StudyTask> caseSensitiveMatches = service.listTasks("Harness");

    assertEquals(
        List.of(
            new StudyTask(1, "literal .* marker", true),
            new StudyTask(3, "harness .* lowercase", false),
            new StudyTask(5, "Harness .* details", false)),
        literalMatches);
    assertEquals(
        List.of(
            new StudyTask(2, "Harness plain", false),
            new StudyTask(5, "Harness .* details", false)),
        caseSensitiveMatches);
    assertEquals(2, repository.findAllCalls);
    assertEquals(0, repository.createCalls);
    assertEquals(0, repository.updateCalls);
    assertEquals(0, repository.deleteCalls);
  }

  @Test
  void acceptsExactlyTwoHundredSupplementarySearchCodePoints() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    String searchText = "😀".repeat(200);
    repository.tasks = List.of(new StudyTask(4, searchText, false));
    StudyTaskService service = new StudyTaskService(repository);

    List<StudyTask> tasks = service.listTasks(searchText);

    assertEquals(List.of(new StudyTask(4, searchText, false)), tasks);
    assertEquals(1, repository.findAllCalls);
  }

  @Test
  void invalidSearchTextFailsBeforeReadingRepository() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    StudyTaskService service = new StudyTaskService(repository);

    for (String searchText : List.of(" \t ", "😀".repeat(201))) {
      InvalidSearchTextException exception =
          assertThrows(
              InvalidSearchTextException.class,
              () -> service.listTasks(searchText));

      assertEquals(InvalidSearchTextException.MESSAGE, exception.getMessage());
    }
    assertThrows(InvalidSearchTextException.class, () -> service.listTasks(null));
    assertEquals(0, repository.findAllCalls);
    assertEquals(0, repository.createCalls);
    assertEquals(0, repository.updateCalls);
    assertEquals(0, repository.deleteCalls);
  }

  @Test
  void summarizesMixedTaskStatesWithoutPersisting() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks =
        List.of(
            new StudyTask(1, "待完成一", false),
            new StudyTask(2, "已完成", true),
            new StudyTask(3, "待完成二", false));
    StudyTaskService service = new StudyTaskService(repository);

    TaskSummary summary = service.summarizeTasks();

    assertEquals(new TaskSummary(3, 2, 1), summary);
    assertEquals(1, repository.findAllCalls);
    assertEquals(0, repository.createCalls);
    assertEquals(0, repository.updateCalls);
  }

  @Test
  void summarizesEmptyRepositoryAsZeroCounts() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    StudyTaskService service = new StudyTaskService(repository);

    TaskSummary summary = service.summarizeTasks();

    assertEquals(new TaskSummary(0, 0, 0), summary);
    assertEquals(1, repository.findAllCalls);
    assertEquals(0, repository.createCalls);
    assertEquals(0, repository.updateCalls);
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

  @Test
  void reopensCompletedTaskAndPreservesIdentifierAndTitle() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks =
        List.of(
            new StudyTask(2, "保留任务", false),
            new StudyTask(7, "重新打开", true));
    StudyTaskService service = new StudyTaskService(repository);

    ReopenTaskResult result = service.reopenTask(7);

    assertEquals(ReopenTaskResult.REOPENED, result);
    assertEquals(new StudyTask(7, "重新打开", false), repository.updatedTask);
    assertEquals(1, repository.findAllCalls);
    assertEquals(1, repository.updateCalls);
  }

  @Test
  void reopeningAlreadyPendingTaskDoesNotPersist() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks = List.of(new StudyTask(1, "保持未完成", false));
    StudyTaskService service = new StudyTaskService(repository);

    ReopenTaskResult result = service.reopenTask(1);

    assertEquals(ReopenTaskResult.ALREADY_PENDING, result);
    assertEquals(1, repository.findAllCalls);
    assertEquals(
        0,
        repository.updateCalls,
        "Location: StudyTaskServiceTest.reopeningAlreadyPendingTaskDoesNotPersist\n"
            + "Invariant: Reopening a pending task must not call repository.update.\n"
            + "Reason: Production called repository.update on the already-pending branch.\n"
            + "Fix: Return ALREADY_PENDING before any repository update.\n"
            + "Recheck: .\\mvnw.cmd "
            + "-Dtest=StudyTaskServiceTest#reopeningAlreadyPendingTaskDoesNotPersist test\n"
            + "Authority: SPEC.md 2.8 and AC-17.");
  }

  @Test
  void reopeningMissingTaskFailsWithoutPersisting() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks = List.of(new StudyTask(1, "已有任务", true));
    StudyTaskService service = new StudyTaskService(repository);

    TaskNotFoundException exception =
        assertThrows(TaskNotFoundException.class, () -> service.reopenTask(99));

    assertEquals("Task 99 not found.", exception.getMessage());
    assertEquals(1, repository.findAllCalls);
    assertEquals(0, repository.updateCalls);
  }

  @Test
  void deletesExistingTaskAfterConfirmingItExists() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks =
        List.of(
            new StudyTask(1, "保留任务", false),
            new StudyTask(2, "删除任务", true));
    StudyTaskService service = new StudyTaskService(repository);

    service.deleteTask(2);

    assertEquals(1, repository.findAllCalls);
    assertEquals(1, repository.deleteCalls);
    assertEquals(2, repository.deletedTaskId);
  }

  @Test
  void deletingMissingTaskFailsWithoutCallingDeletePort() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks = List.of(new StudyTask(1, "已有任务", false));
    StudyTaskService service = new StudyTaskService(repository);

    TaskNotFoundException exception =
        assertThrows(TaskNotFoundException.class, () -> service.deleteTask(99));

    assertEquals("Task 99 not found.", exception.getMessage());
    assertEquals(1, repository.findAllCalls);
    assertEquals(0, repository.deleteCalls);
  }

  @Test
  void renamesPendingTaskWithStrippedTitleAndPersistsOnce() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks = List.of(new StudyTask(7, "Old title", false));
    StudyTaskService service = new StudyTaskService(repository);

    RenameTaskResult result = service.renameTask(7, "  New title  ");

    assertEquals(RenameTaskResult.RENAMED, result);
    assertEquals(new StudyTask(7, "New title", false), repository.updatedTask);
    assertEquals(1, repository.findAllCalls);
    assertEquals(1, repository.updateCalls);
  }

  @Test
  void renamesCompletedTaskWithoutChangingCompletionState() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks = List.of(new StudyTask(4, "Old title", true));
    StudyTaskService service = new StudyTaskService(repository);

    RenameTaskResult result = service.renameTask(4, "New title");

    assertEquals(RenameTaskResult.RENAMED, result);
    assertEquals(new StudyTask(4, "New title", true), repository.updatedTask);
    assertEquals(1, repository.findAllCalls);
    assertEquals(1, repository.updateCalls);
  }

  @Test
  void renamingToExistingTitleIsIdempotentWithoutPersisting() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks = List.of(new StudyTask(3, "Same title", true));
    StudyTaskService service = new StudyTaskService(repository);

    RenameTaskResult result = service.renameTask(3, "  Same title  ");

    assertEquals(RenameTaskResult.ALREADY_NAMED, result);
    assertEquals(1, repository.findAllCalls);
    assertEquals(0, repository.updateCalls);
  }

  @Test
  void invalidRenameTitleFailsBeforeReadingRepository() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks = List.of(new StudyTask(1, "Existing title", false));
    StudyTaskService service = new StudyTaskService(repository);

    InvalidTaskTitleException exception =
        assertThrows(InvalidTaskTitleException.class, () -> service.renameTask(1, " \t "));

    assertEquals(InvalidTaskTitleException.MESSAGE, exception.getMessage());
    assertEquals(0, repository.findAllCalls);
    assertEquals(0, repository.updateCalls);
  }

  @Test
  void overlongRenameTitleFailsBeforeReadingRepository() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    StudyTaskService service = new StudyTaskService(repository);

    assertThrows(
        InvalidTaskTitleException.class, () -> service.renameTask(99, "x".repeat(201)));

    assertEquals(0, repository.findAllCalls);
    assertEquals(0, repository.updateCalls);
  }

  @Test
  void renameAcceptsExactlyTwoHundredUnicodeCodePoints() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks = List.of(new StudyTask(1, "Old title", false));
    StudyTaskService service = new StudyTaskService(repository);
    String title = "😃".repeat(200);

    RenameTaskResult result = service.renameTask(1, title);

    assertEquals(RenameTaskResult.RENAMED, result);
    assertEquals(new StudyTask(1, title, false), repository.updatedTask);
    assertEquals(1, repository.findAllCalls);
    assertEquals(1, repository.updateCalls);
  }

  @Test
  void renamingMissingTaskFailsWithoutPersisting() {
    RecordingTaskRepository repository = new RecordingTaskRepository();
    repository.tasks = List.of(new StudyTask(1, "Existing title", false));
    StudyTaskService service = new StudyTaskService(repository);

    TaskNotFoundException exception =
        assertThrows(TaskNotFoundException.class, () -> service.renameTask(99, "New title"));

    assertEquals("Task 99 not found.", exception.getMessage());
    assertEquals(1, repository.findAllCalls);
    assertEquals(0, repository.updateCalls);
  }

  private static final class RecordingTaskRepository implements TaskRepository {

    private int createCalls;
    private int findAllCalls;
    private int updateCalls;
    private int deleteCalls;
    private String lastTitle;
    private List<StudyTask> tasks = List.of();
    private StudyTask updatedTask;
    private long deletedTaskId;

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

    @Override
    public void delete(long taskId) {
      deleteCalls++;
      deletedTaskId = taskId;
    }
  }
}
