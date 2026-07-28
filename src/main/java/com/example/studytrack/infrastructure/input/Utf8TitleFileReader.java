package com.example.studytrack.infrastructure.input;

import com.example.studytrack.application.TitleFileException;
import com.example.studytrack.application.port.TitleFileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads complete title files using strict UTF-8 decoding. */
public final class Utf8TitleFileReader implements TitleFileReader {

  private static final byte UTF8_BOM_FIRST = (byte) 0xEF;
  private static final byte UTF8_BOM_SECOND = (byte) 0xBB;
  private static final byte UTF8_BOM_THIRD = (byte) 0xBF;

  @Override
  public String read(String location) {
    try {
      byte[] content = Files.readAllBytes(Path.of(location));
      int offset = hasUtf8Bom(content) ? 3 : 0;
      return decode(content, offset);
    } catch (IOException | RuntimeException exception) {
      throw new TitleFileException(location, exception);
    }
  }

  private static boolean hasUtf8Bom(byte[] content) {
    return content.length >= 3
        && content[0] == UTF8_BOM_FIRST
        && content[1] == UTF8_BOM_SECOND
        && content[2] == UTF8_BOM_THIRD;
  }

  private static String decode(byte[] content, int offset) throws CharacterCodingException {
    ByteBuffer bytes = ByteBuffer.wrap(content, offset, content.length - offset);
    return StandardCharsets.UTF_8
        .newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(bytes)
        .toString();
  }
}
