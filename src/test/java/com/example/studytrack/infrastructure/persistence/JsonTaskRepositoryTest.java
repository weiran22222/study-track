package com.example.studytrack.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.studytrack.domain.StudyTask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
}
