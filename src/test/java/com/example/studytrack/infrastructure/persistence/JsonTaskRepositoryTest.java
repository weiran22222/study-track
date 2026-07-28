package com.example.studytrack.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.studytrack.application.TaskPersistenceException;
import com.example.studytrack.domain.StudyTask;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonTaskRepositoryTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void persistsTasksAndContinuesIdsAcrossRepositoryInstances(@TempDir Path temporaryDirectory)
      throws Exception {
    Path dataFile = temporaryDirectory.resolve("nested").resolve("tasks.json");

    StudyTask first = new JsonTaskRepository(dataFile).create("第一项");
    StudyTask second = new JsonTaskRepository(dataFile).create("第二项");

    assertEquals(new StudyTask(1, "第一项", false), first);
    assertEquals(new StudyTask(2, "第二项", false), second);

    JsonNode root = objectMapper.readTree(dataFile.toFile());
    assertEquals(3, root.get("nextId").asLong());
    assertEquals(2, root.get("tasks").size());
    assertEquals("第一项", root.get("tasks").get(0).get("title").asText());
    assertFalse(root.get("tasks").get(0).get("completed").asBoolean());
  }

  @Test
  void successfulWriteLeavesNoTemporaryFile(@TempDir Path temporaryDirectory) throws Exception {
    Path dataFile = temporaryDirectory.resolve("tasks.json");

    new JsonTaskRepository(dataFile).create("学习原子写入");

    List<Path> files;
    try (var paths = Files.list(temporaryDirectory)) {
      files = paths.toList();
    }
    assertEquals(List.of(dataFile), files);
    assertTrue(Files.exists(dataFile));
  }

  @Test
  void readsTasksSavedByEarlierRepositoryInstance(@TempDir Path temporaryDirectory) {
    Path dataFile = temporaryDirectory.resolve("tasks.json");
    new JsonTaskRepository(dataFile).create("跨进程读取");

    List<StudyTask> tasks = new JsonTaskRepository(dataFile).findAll();

    assertEquals(List.of(new StudyTask(1, "跨进程读取", false)), tasks);
  }

  @Test
  void missingDataFileIsAnEmptyRepository(@TempDir Path temporaryDirectory) {
    Path dataFile = temporaryDirectory.resolve("tasks.json");

    List<StudyTask> tasks = new JsonTaskRepository(dataFile).findAll();

    assertTrue(tasks.isEmpty());
    assertFalse(Files.exists(dataFile));
  }

  @Test
  void updatePersistsCompletedStateAndPreservesRepositoryMetadata(
      @TempDir Path temporaryDirectory) throws Exception {
    Path dataFile = temporaryDirectory.resolve("tasks.json");
    JsonTaskRepository repository = new JsonTaskRepository(dataFile);
    StudyTask task = repository.create("学习幂等性");

    repository.update(new StudyTask(task.id(), task.title(), true));

    JsonNode root = objectMapper.readTree(dataFile.toFile());
    assertEquals(2, root.get("nextId").asLong());
    assertEquals(1, root.get("tasks").size());
    assertTrue(root.get("tasks").get(0).get("completed").asBoolean());
    assertEquals(
        List.of(new StudyTask(1, "学习幂等性", true)),
        new JsonTaskRepository(dataFile).findAll());
  }

  @Test
  void deleteRemovesOnlyRequestedTaskAndKeepsNextIdForFollowingCreate(
      @TempDir Path temporaryDirectory) throws Exception {
    Path dataFile = temporaryDirectory.resolve("tasks.json");
    Files.writeString(
        dataFile,
        """
        {
          "nextId": 10,
          "tasks": [
            {"id": 1, "title": "第一项", "completed": false},
            {"id": 4, "title": "删除项", "completed": true},
            {"id": 7, "title": "第三项", "completed": false}
          ]
        }
        """);
    JsonTaskRepository repository = new JsonTaskRepository(dataFile);

    repository.delete(4);

    JsonNode root = objectMapper.readTree(dataFile.toFile());
    assertEquals(10, root.get("nextId").asLong());
    assertEquals(2, root.get("tasks").size());
    assertEquals(1, root.get("tasks").get(0).get("id").asLong());
    assertEquals("第一项", root.get("tasks").get(0).get("title").asText());
    assertFalse(root.get("tasks").get(0).get("completed").asBoolean());
    assertEquals(7, root.get("tasks").get(1).get("id").asLong());
    assertEquals("第三项", root.get("tasks").get(1).get("title").asText());
    assertFalse(root.get("tasks").get(1).get("completed").asBoolean());

    StudyTask created = repository.create("删除后新增");
    assertEquals(new StudyTask(10, "删除后新增", false), created);
    assertEquals(11, objectMapper.readTree(dataFile.toFile()).get("nextId").asLong());
  }

  @Test
  void deleteLastTaskPersistsEmptyCollectionWithoutChangingNextId(
      @TempDir Path temporaryDirectory) throws Exception {
    Path dataFile = temporaryDirectory.resolve("tasks.json");
    Files.writeString(
        dataFile,
        """
        {
          "nextId": 6,
          "tasks": [
            {"id": 5, "title": "最后一项", "completed": true}
          ]
        }
        """);

    new JsonTaskRepository(dataFile).delete(5);

    JsonNode root = objectMapper.readTree(dataFile.toFile());
    assertEquals(6, root.get("nextId").asLong());
    assertTrue(root.get("tasks").isArray());
    assertTrue(root.get("tasks").isEmpty());
  }

  @Test
  void deleteWriteFailurePreservesOriginalBytesAndCleansTemporaryFile(
      @TempDir Path temporaryDirectory) throws Exception {
    Path dataFile = temporaryDirectory.resolve("tasks.json");
    Files.writeString(
        dataFile,
        """
        {
          "nextId": 3,
          "tasks": [
            {"id": 1, "title": "保留原始数据", "completed": false},
            {"id": 2, "title": "删除失败", "completed": true}
          ]
        }
        """);
    byte[] originalData = Files.readAllBytes(dataFile);
    ObjectMapper failingMapper = new ObjectMapper(new FailingWriteJsonFactory());
    JsonTaskRepository repository = new JsonTaskRepository(dataFile, failingMapper);

    TaskPersistenceException exception =
        assertThrows(TaskPersistenceException.class, () -> repository.delete(2));

    assertTrue(exception.getMessage().startsWith("Unable to write data file "));
    assertArrayEquals(originalData, Files.readAllBytes(dataFile));
    try (var paths = Files.list(temporaryDirectory)) {
      assertEquals(List.of(dataFile), paths.toList());
    }
  }

  @Test
  void updateRenamesOnlyRequestedTaskAndPreservesMetadataAndOrder(
      @TempDir Path temporaryDirectory) throws Exception {
    Path dataFile = temporaryDirectory.resolve("tasks.json");
    Files.writeString(
        dataFile,
        """
        {
          "nextId": 12,
          "tasks": [
            {"id": 2, "title": "First", "completed": false},
            {"id": 6, "title": "Old title", "completed": true},
            {"id": 9, "title": "Third", "completed": false}
          ]
        }
        """);
    JsonTaskRepository repository = new JsonTaskRepository(dataFile);

    repository.update(new StudyTask(6, "New title", true));

    JsonNode root = objectMapper.readTree(dataFile.toFile());
    assertEquals(12, root.get("nextId").asLong());
    assertEquals(3, root.get("tasks").size());
    assertEquals(2, root.get("tasks").get(0).get("id").asLong());
    assertEquals("First", root.get("tasks").get(0).get("title").asText());
    assertFalse(root.get("tasks").get(0).get("completed").asBoolean());
    assertEquals(6, root.get("tasks").get(1).get("id").asLong());
    assertEquals("New title", root.get("tasks").get(1).get("title").asText());
    assertTrue(root.get("tasks").get(1).get("completed").asBoolean());
    assertEquals(9, root.get("tasks").get(2).get("id").asLong());
    assertEquals("Third", root.get("tasks").get(2).get("title").asText());
    assertFalse(root.get("tasks").get(2).get("completed").asBoolean());
  }

  @Test
  void updateWriteFailurePreservesOriginalBytesAndCleansTemporaryFile(
      @TempDir Path temporaryDirectory) throws Exception {
    Path dataFile = temporaryDirectory.resolve("tasks.json");
    Files.writeString(
        dataFile,
        """
        {
          "nextId": 3,
          "tasks": [
            {"id": 1, "title": "Keep original bytes", "completed": true},
            {"id": 2, "title": "Other task", "completed": false}
          ]
        }
        """);
    byte[] originalData = Files.readAllBytes(dataFile);
    ObjectMapper failingMapper = new ObjectMapper(new FailingWriteJsonFactory());
    JsonTaskRepository repository = new JsonTaskRepository(dataFile, failingMapper);

    TaskPersistenceException exception =
        assertThrows(
            TaskPersistenceException.class,
            () -> repository.update(new StudyTask(1, "Keep original bytes", false)));

    assertTrue(exception.getMessage().startsWith("Unable to write data file "));
    assertArrayEquals(originalData, Files.readAllBytes(dataFile));
    try (var paths = Files.list(temporaryDirectory)) {
      assertEquals(List.of(dataFile), paths.toList());
    }
  }

  private static final class FailingWriteJsonFactory extends JsonFactory {

    @Override
    public JsonGenerator createGenerator(Writer writer) throws IOException {
      throw new IOException("Injected write failure.");
    }
  }
}
