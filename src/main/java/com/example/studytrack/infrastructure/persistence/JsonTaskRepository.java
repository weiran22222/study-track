package com.example.studytrack.infrastructure.persistence;

import com.example.studytrack.application.TaskPersistenceException;
import com.example.studytrack.application.port.TaskRepository;
import com.example.studytrack.domain.StudyTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** JSON file implementation of the task repository. */
public final class JsonTaskRepository implements TaskRepository {

  private final Path dataFile;
  private final ObjectMapper objectMapper;

  /**
   * Creates a repository for a data file.
   *
   * @param dataFile configured JSON data file
   */
  public JsonTaskRepository(Path dataFile) {
    this(dataFile, new ObjectMapper());
  }

  JsonTaskRepository(Path dataFile, ObjectMapper objectMapper) {
    this.dataFile = Objects.requireNonNull(dataFile).toAbsolutePath().normalize();
    this.objectMapper = Objects.requireNonNull(objectMapper);
  }

  @Override
  public StudyTask create(String title) {
    StoredTasks storedTasks = read();
    StudyTask task = new StudyTask(storedTasks.nextId(), title, false);
    List<StudyTask> updatedTasks = new ArrayList<>(storedTasks.tasks());
    updatedTasks.add(task);
    write(new StoredTasks(storedTasks.nextId() + 1, updatedTasks));
    return task;
  }

  @Override
  public List<StudyTask> findAll() {
    return List.copyOf(read().tasks());
  }

  @Override
  public void update(StudyTask task) {
    StoredTasks storedTasks = read();
    List<StudyTask> updatedTasks = new ArrayList<>(storedTasks.tasks());
    int taskIndex = -1;
    for (int index = 0; index < updatedTasks.size(); index++) {
      if (updatedTasks.get(index).id() == task.id()) {
        taskIndex = index;
        break;
      }
    }
    if (taskIndex < 0) {
      throw persistenceFailure(
          "update", new IOException("Task " + task.id() + " does not exist."));
    }
    updatedTasks.set(taskIndex, task);
    write(new StoredTasks(storedTasks.nextId(), updatedTasks));
  }

  private StoredTasks read() {
    if (Files.notExists(dataFile)) {
      return new StoredTasks(1, List.of());
    }

    try (Reader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
      StoredTasks storedTasks = objectMapper.readValue(reader, StoredTasks.class);
      if (storedTasks.nextId() < 1 || storedTasks.tasks() == null) {
        throw new IOException("Data file is missing required fields.");
      }
      return storedTasks;
    } catch (IOException exception) {
      throw persistenceFailure("read", exception);
    }
  }

  private void write(StoredTasks storedTasks) {
    Path directory = dataFile.getParent();
    Path temporaryFile = null;
    try {
      Files.createDirectories(directory);
      temporaryFile = Files.createTempFile(directory, dataFile.getFileName().toString(), ".tmp");
      try (Writer writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, storedTasks);
      }
      Files.move(
          temporaryFile,
          dataFile,
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
      temporaryFile = null;
    } catch (IOException exception) {
      throw persistenceFailure("write", exception);
    } finally {
      deleteTemporaryFile(temporaryFile);
    }
  }

  private void deleteTemporaryFile(Path temporaryFile) {
    if (temporaryFile == null) {
      return;
    }

    try {
      Files.deleteIfExists(temporaryFile);
    } catch (IOException ignored) {
      // The original data file is still intact; cleanup can be retried by the user.
    }
  }

  private TaskPersistenceException persistenceFailure(String operation, IOException cause) {
    String message = "Unable to " + operation + " data file " + dataFile + ".";
    return new TaskPersistenceException(message, cause);
  }

  private record StoredTasks(long nextId, List<StudyTask> tasks) {}
}
