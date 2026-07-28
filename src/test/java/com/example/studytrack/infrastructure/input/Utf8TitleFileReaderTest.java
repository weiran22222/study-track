package com.example.studytrack.infrastructure.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.studytrack.application.TitleFileException;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Utf8TitleFileReaderTest {

  private final Utf8TitleFileReader reader = new Utf8TitleFileReader();

  @Test
  void readsCompleteStrictUtf8IncludingSupplementaryCodePoints(
      @TempDir Path temporaryDirectory) throws Exception {
    Path titleFile = temporaryDirectory.resolve("title.txt");
    String expected = "  ASCII 中文 😀\r\nsecond line  ";
    Files.writeString(titleFile, expected, StandardCharsets.UTF_8);

    assertEquals(expected, reader.read(titleFile.toString()));
  }

  @Test
  void ignoresOneOptionalUtf8Bom(@TempDir Path temporaryDirectory) throws Exception {
    final Path titleFile = temporaryDirectory.resolve("bom-title.txt");
    byte[] title = "😀 title".getBytes(StandardCharsets.UTF_8);
    byte[] withBom = new byte[title.length + 3];
    withBom[0] = (byte) 0xEF;
    withBom[1] = (byte) 0xBB;
    withBom[2] = (byte) 0xBF;
    System.arraycopy(title, 0, withBom, 3, title.length);
    Files.write(titleFile, withBom);

    assertEquals("😀 title", reader.read(titleFile.toString()));
  }

  @Test
  void returnsEmptyTextForEmptyFile(@TempDir Path temporaryDirectory) throws Exception {
    Path titleFile = Files.createFile(temporaryDirectory.resolve("empty.txt"));

    assertEquals("", reader.read(titleFile.toString()));
  }

  @Test
  void reportsMissingFileWithStableMessage(@TempDir Path temporaryDirectory) {
    Path titleFile = temporaryDirectory.resolve("missing.txt");

    TitleFileException exception =
        assertThrows(TitleFileException.class, () -> reader.read(titleFile.toString()));

    assertEquals(
        "Unable to read UTF-8 title file: " + titleFile,
        exception.getMessage());
    assertInstanceOf(IOException.class, exception.getCause());
  }

  @Test
  void reportsDirectoryWithStableMessage(@TempDir Path temporaryDirectory) throws Exception {
    Path titleDirectory =
        Files.createDirectory(temporaryDirectory.resolve("title-directory"));

    TitleFileException exception =
        assertThrows(TitleFileException.class, () -> reader.read(titleDirectory.toString()));

    assertEquals(
        "Unable to read UTF-8 title file: " + titleDirectory,
        exception.getMessage());
    assertInstanceOf(IOException.class, exception.getCause());
  }

  @Test
  void rejectsMalformedUtf8InsteadOfReplacingIt(@TempDir Path temporaryDirectory)
      throws Exception {
    Path titleFile = temporaryDirectory.resolve("invalid.txt");
    Files.write(titleFile, new byte[] {(byte) 0xC3, (byte) 0x28});

    TitleFileException exception =
        assertThrows(TitleFileException.class, () -> reader.read(titleFile.toString()));

    assertEquals(
        "Unable to read UTF-8 title file: " + titleFile,
        exception.getMessage());
    assertInstanceOf(CharacterCodingException.class, exception.getCause());
  }
}
