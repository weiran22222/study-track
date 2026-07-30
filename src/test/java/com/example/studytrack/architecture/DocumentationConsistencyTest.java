package com.example.studytrack.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentationConsistencyTest {

  private static final Path REPOSITORY_ROOT = Path.of("").toAbsolutePath().normalize();
  private static final Pattern URI_SCHEME =
      Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:");

  @Test
  void allRepositoryMarkdownLocalLinksResolve() throws IOException {
    List<String> violations = inspectRepository(REPOSITORY_ROOT);

    assertTrue(
        violations.isEmpty(),
        () ->
            "Repository Markdown local-link consistency invariant violated.\n\n"
                + String.join("\n\n", violations));
  }

  @Test
  void scannerSupportsParenthesesEscapesAnglesImagesAndTitles(
      @TempDir Path repositoryRoot) throws IOException {
    Path docs = Files.createDirectories(repositoryRoot.resolve("docs"));
    Files.writeString(docs.resolve("foo(bar).md"), "# target", StandardCharsets.UTF_8);
    Files.writeString(docs.resolve("foo).md"), "# escaped", StandardCharsets.UTF_8);
    Files.write(docs.resolve("diagram(v1).png"), new byte[] {0});
    Files.writeString(
        docs.resolve("source.md"),
        """
        [balanced](foo(bar).md)
        [escaped](foo\\).md)
        [angle](<foo(bar).md>)
        [with title](foo(bar).md "Title (kept)")
        ![image](diagram(v1).png 'Diagram title')
        """,
        StandardCharsets.UTF_8);

    assertTrue(
        inspectRepository(repositoryRoot).isEmpty(),
        "Every supported parenthesized destination should resolve.");
  }

  @Test
  void scannerSkipsExcludedTargetsAndFencedExamples(
      @TempDir Path repositoryRoot) throws IOException {
    Path docs = Files.createDirectories(repositoryRoot.resolve("docs"));
    Files.writeString(
        docs.resolve("source.md"),
        """
        [anchor](#section)
        [external](https://example.com/missing(foo).md)
        [protocol](mailto:docs@example.com)
        [protocol relative](//example.com/missing.md)
        [root relative](/missing.md)
        ```markdown
        [backtick example](missing(foo).md)
        ```
           ~~~
        ![tilde example](missing(image).png)
           ~~~
        """,
        StandardCharsets.UTF_8);

    assertTrue(
        inspectRepository(repositoryRoot).isEmpty(),
        "Excluded targets and fenced examples must not become live local links.");
  }

  @Test
  void scannerRejectsMissingEscapingAndMalformedPercentTargets(
      @TempDir Path repositoryRoot) throws IOException {
    Path docs = Files.createDirectories(repositoryRoot.resolve("docs"));
    Files.writeString(
        docs.resolve("source.md"),
        """
        [missing](missing(foo).md)
        [escape](../../outside.md)
        [percent](bad%ZZ.md)
        [malformed angle](<missing(foo).md)
        [unbalanced](missing(foo.md)
        """,
        StandardCharsets.UTF_8);

    List<String> violations = inspectRepository(repositoryRoot);

    assertEquals(3, violations.size());
    assertTrue(
        violations.stream()
            .allMatch(
                diagnostic ->
                    diagnostic.contains("Location:")
                        && diagnostic.contains("Invariant:")
                        && diagnostic.contains("Reason:")
                        && diagnostic.contains("Fix:")
                        && diagnostic.contains("Recheck:")
                        && diagnostic.contains("Authority:")),
        "Every rejected live target must have all actionable diagnostic fields.");
  }

  @Test
  void percentEncodedLocalPathResolves(@TempDir Path repositoryRoot)
      throws IOException {
    Path docs = Files.createDirectories(repositoryRoot.resolve("docs"));
    Files.writeString(
        docs.resolve("target file.md"), "# encoded target", StandardCharsets.UTF_8);
    Files.writeString(
        docs.resolve("source.md"),
        "[encoded](target%20file.md)\n",
        StandardCharsets.UTF_8);

    assertTrue(
        inspectRepository(repositoryRoot).isEmpty(),
        "A percent-encoded local path must resolve to the decoded repository target.");
  }

  @Test
  void exactCaseMismatchIsRejectedOnEveryHost(@TempDir Path repositoryRoot)
      throws IOException {
    Path docs = Files.createDirectories(repositoryRoot.resolve("docs"));
    Files.writeString(
        docs.resolve("ExactCase.md"), "# exact case", StandardCharsets.UTF_8);
    Files.writeString(
        docs.resolve("source.md"),
        "[wrong case](exactcase.md)\n",
        StandardCharsets.UTF_8);

    List<String> violations = inspectRepository(repositoryRoot);

    assertEquals(1, violations.size());
    assertTrue(
        violations.getFirst().contains("does not exist with exact path casing")
            && violations.getFirst().contains("exactcase.md"),
        "Directory enumeration must reject mismatched casing even on case-insensitive hosts.");
  }

  @Test
  void realPathContainmentRejectsAnOutsideTarget(@TempDir Path parent)
      throws IOException {
    Path repositoryRoot = Files.createDirectory(parent.resolve("repository"));
    Path document =
        Files.writeString(
            repositoryRoot.resolve("source.md"), "# source", StandardCharsets.UTF_8);
    Path outside =
        Files.writeString(parent.resolve("outside.md"), "# outside", StandardCharsets.UTF_8);
    List<String> violations = new ArrayList<>();

    verifyRealPathContainment(
        repositoryRoot.toAbsolutePath().normalize(),
        document.toAbsolutePath().normalize(),
        1,
        "../outside.md",
        outside.toAbsolutePath().normalize(),
        violations);

    assertEquals(1, violations.size());
    assertTrue(
        violations.getFirst().contains("resolves outside the repository"),
        "Real-path containment must reject a target outside the repository root.");
  }

  @Test
  void discoveryIncludesOnlyRootAndNestedDocumentationMarkdown(
      @TempDir Path repositoryRoot) throws IOException {
    Path nestedDocs =
        Files.createDirectories(repositoryRoot.resolve("docs").resolve("nested"));
    Path outsideSurface = Files.createDirectories(repositoryRoot.resolve("other"));
    Files.writeString(
        repositoryRoot.resolve("root.md"), "# root", StandardCharsets.UTF_8);
    Files.writeString(
        nestedDocs.resolve("nested.md"), "# nested", StandardCharsets.UTF_8);
    Files.writeString(
        outsideSurface.resolve("excluded.md"), "# excluded", StandardCharsets.UTF_8);
    Files.writeString(
        repositoryRoot.resolve("not-markdown.txt"), "not Markdown", StandardCharsets.UTF_8);

    List<String> discovered =
        discoverMarkdownDocuments(repositoryRoot.toAbsolutePath().normalize()).stream()
            .map(path -> repositoryPath(repositoryRoot.toAbsolutePath().normalize(), path))
            .toList();

    assertEquals(List.of("docs/nested/nested.md", "root.md"), discovered);
  }

  @Test
  void violationsKeepStableDocumentLineAndLinkOrder(@TempDir Path repositoryRoot)
      throws IOException {
    Path docs = Files.createDirectories(repositoryRoot.resolve("docs"));
    Files.writeString(
        repositoryRoot.resolve("z.md"),
        "[z](missing-z.md)\n",
        StandardCharsets.UTF_8);
    Files.writeString(
        repositoryRoot.resolve("a.md"),
        "[a](missing-a.md)\n",
        StandardCharsets.UTF_8);
    Files.writeString(
        docs.resolve("ordered.md"),
        """
        [first](missing-first.md) [second](missing-second.md)
        [third](missing-third.md)
        """,
        StandardCharsets.UTF_8);

    List<String> violationOrder =
        inspectRepository(repositoryRoot).stream()
            .map(DocumentationConsistencyTest::diagnosticIdentity)
            .toList();

    assertEquals(
        List.of(
            "a.md:1|missing-a.md",
            "docs/ordered.md:1|missing-first.md",
            "docs/ordered.md:1|missing-second.md",
            "docs/ordered.md:2|missing-third.md",
            "z.md:1|missing-z.md"),
        violationOrder);
  }

  private static String diagnosticIdentity(String diagnostic) {
    String location = diagnosticField(diagnostic, "Location: ");
    String reason = diagnosticField(diagnostic, "Reason: ");
    int targetStart = reason.lastIndexOf(": ");
    return location + "|" + reason.substring(targetStart + 2);
  }

  private static String diagnosticField(String diagnostic, String prefix) {
    return diagnostic.lines()
        .filter(line -> line.startsWith(prefix))
        .map(line -> line.substring(prefix.length()))
        .findFirst()
        .orElseThrow();
  }

  private static List<String> inspectRepository(Path repositoryRoot) throws IOException {
    Path normalizedRoot = repositoryRoot.toAbsolutePath().normalize();
    List<String> violations = new ArrayList<>();

    for (Path document : discoverMarkdownDocuments(normalizedRoot)) {
      inspectDocument(normalizedRoot, document, violations);
    }
    return violations;
  }

  private static List<Path> discoverMarkdownDocuments(Path repositoryRoot)
      throws IOException {
    List<Path> documents = new ArrayList<>();

    try (Stream<Path> rootFiles = Files.list(repositoryRoot)) {
      documents.addAll(rootFiles.filter(DocumentationConsistencyTest::isMarkdownFile).toList());
    }

    Path documentationRoot = repositoryRoot.resolve("docs");
    if (Files.isDirectory(documentationRoot)) {
      try (Stream<Path> documentationFiles = Files.walk(documentationRoot)) {
        documents.addAll(
            documentationFiles.filter(DocumentationConsistencyTest::isMarkdownFile).toList());
      }
    }

    documents.sort(
        Comparator.comparing(path -> repositoryPath(repositoryRoot, path)));
    return documents;
  }

  private static boolean isMarkdownFile(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".md");
  }

  private static void inspectDocument(
      Path repositoryRoot, Path document, List<String> violations) throws IOException {
    List<String> lines = Files.readAllLines(document, StandardCharsets.UTF_8);
    FenceMarker openFence = null;

    for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
      String line = lines.get(lineIndex);
      FenceMarker marker = fenceMarker(line);

      if (openFence != null) {
        if (marker != null
            && marker.character() == openFence.character()
            && marker.length() >= openFence.length()
            && marker.hasOnlyTrailingWhitespace()) {
          openFence = null;
        }
        continue;
      }

      if (marker != null) {
        openFence = marker;
        continue;
      }

      inspectLinks(repositoryRoot, document, lineIndex + 1, line, violations);
    }
  }

  private static FenceMarker fenceMarker(String line) {
    int markerStart = 0;
    while (markerStart < line.length()
        && markerStart < 4
        && line.charAt(markerStart) == ' ') {
      markerStart++;
    }
    if (markerStart > 3 || markerStart >= line.length()) {
      return null;
    }

    char character = line.charAt(markerStart);
    if (character != '`' && character != '~') {
      return null;
    }

    int markerEnd = markerStart;
    while (markerEnd < line.length() && line.charAt(markerEnd) == character) {
      markerEnd++;
    }
    int length = markerEnd - markerStart;
    if (length < 3) {
      return null;
    }

    return new FenceMarker(character, length, line.substring(markerEnd).isBlank());
  }

  private static void inspectLinks(
      Path repositoryRoot,
      Path document,
      int lineNumber,
      String line,
      List<String> violations) {
    for (LinkDestination destination : scanLinkDestinations(line)) {
      String originalTarget = destination.source();
      String targetWithoutAnchor = removeAnchor(destination.unescaped());

      if (targetWithoutAnchor.isBlank()
          || isExternalOrProtocolTarget(targetWithoutAnchor)
          || isRootRelativeTarget(targetWithoutAnchor)) {
        continue;
      }

      inspectLocalTarget(
          repositoryRoot,
          document,
          lineNumber,
          originalTarget,
          targetWithoutAnchor,
          violations);
    }
  }

  private static List<LinkDestination> scanLinkDestinations(String line) {
    List<LinkDestination> destinations = new ArrayList<>();
    int searchIndex = 0;

    while (searchIndex < line.length()) {
      int openingBracket = findUnescaped(line, '[', searchIndex);
      if (openingBracket < 0) {
        break;
      }

      int closingBracket = findMatchingBracket(line, openingBracket);
      if (closingBracket < 0
          || closingBracket + 1 >= line.length()
          || line.charAt(closingBracket + 1) != '(') {
        searchIndex = openingBracket + 1;
        continue;
      }

      ParsedDestination parsed = parseDestination(line, closingBracket + 2);
      if (parsed == null) {
        searchIndex = openingBracket + 1;
        continue;
      }

      destinations.add(
          new LinkDestination(parsed.source(), unescapeMarkdown(parsed.source())));
      searchIndex = parsed.linkEnd() + 1;
    }

    return destinations;
  }

  private static int findUnescaped(String text, char expected, int start) {
    for (int index = start; index < text.length(); index++) {
      if (text.charAt(index) == expected && !isEscaped(text, index)) {
        return index;
      }
    }
    return -1;
  }

  private static int findMatchingBracket(String line, int openingBracket) {
    int depth = 1;
    for (int index = openingBracket + 1; index < line.length(); index++) {
      if (isEscaped(line, index)) {
        continue;
      }
      if (line.charAt(index) == '[') {
        depth++;
      } else if (line.charAt(index) == ']' && --depth == 0) {
        return index;
      }
    }
    return -1;
  }

  private static ParsedDestination parseDestination(String line, int contentsStart) {
    int targetStart = skipWhitespace(line, contentsStart);
    if (targetStart >= line.length()) {
      return null;
    }

    if (line.charAt(targetStart) == '<') {
      return parseAngleDestination(line, targetStart);
    }
    return parseBalancedDestination(line, targetStart);
  }

  private static ParsedDestination parseAngleDestination(String line, int targetStart) {
    for (int index = targetStart + 1; index < line.length(); index++) {
      if (line.charAt(index) == '>' && !isEscaped(line, index)) {
        int linkEnd = parseLinkEnding(line, index + 1);
        if (linkEnd < 0) {
          return null;
        }
        return new ParsedDestination(
            line.substring(targetStart + 1, index), linkEnd);
      }
    }
    return null;
  }

  private static ParsedDestination parseBalancedDestination(String line, int targetStart) {
    int depth = 0;
    int index = targetStart;

    while (index < line.length()) {
      char character = line.charAt(index);
      if (character == '\\' && index + 1 < line.length()) {
        index += 2;
        continue;
      }
      if (character == '(') {
        depth++;
      } else if (character == ')') {
        if (depth == 0) {
          return new ParsedDestination(line.substring(targetStart, index), index);
        }
        depth--;
      } else if (Character.isWhitespace(character) && depth == 0) {
        int linkEnd = parseLinkEnding(line, index);
        if (linkEnd < 0) {
          return null;
        }
        return new ParsedDestination(line.substring(targetStart, index), linkEnd);
      }
      index++;
    }
    return null;
  }

  private static int parseLinkEnding(String line, int afterDestination) {
    int index = skipWhitespace(line, afterDestination);
    if (index >= line.length()) {
      return -1;
    }
    if (line.charAt(index) == ')') {
      return index;
    }

    char delimiter = line.charAt(index);
    if (delimiter != '"' && delimiter != '\'' && delimiter != '(') {
      return -1;
    }
    char closingDelimiter = delimiter == '(' ? ')' : delimiter;
    int titleEnd = findUnescaped(line, closingDelimiter, index + 1);
    if (titleEnd < 0) {
      return -1;
    }

    int linkEnd = skipWhitespace(line, titleEnd + 1);
    return linkEnd < line.length() && line.charAt(linkEnd) == ')' ? linkEnd : -1;
  }

  private static int skipWhitespace(String text, int start) {
    int index = start;
    while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
      index++;
    }
    return index;
  }

  private static boolean isEscaped(String text, int index) {
    int backslashes = 0;
    for (int cursor = index - 1; cursor >= 0 && text.charAt(cursor) == '\\'; cursor--) {
      backslashes++;
    }
    return backslashes % 2 == 1;
  }

  private static String unescapeMarkdown(String target) {
    StringBuilder unescaped = new StringBuilder(target.length());
    for (int index = 0; index < target.length(); index++) {
      char character = target.charAt(index);
      if (character == '\\'
          && index + 1 < target.length()
          && isAsciiPunctuation(target.charAt(index + 1))) {
        unescaped.append(target.charAt(++index));
      } else {
        unescaped.append(character);
      }
    }
    return unescaped.toString();
  }

  private static boolean isAsciiPunctuation(char character) {
    return (character >= '!' && character <= '/')
        || (character >= ':' && character <= '@')
        || (character >= '[' && character <= '`')
        || (character >= '{' && character <= '~');
  }

  private static String removeAnchor(String target) {
    int anchor = target.indexOf('#');
    return anchor >= 0 ? target.substring(0, anchor) : target;
  }

  private static boolean isExternalOrProtocolTarget(String target) {
    return target.startsWith("//") || URI_SCHEME.matcher(target).find();
  }

  private static boolean isRootRelativeTarget(String target) {
    return target.startsWith("/") || target.startsWith("\\");
  }

  private static void inspectLocalTarget(
      Path repositoryRoot,
      Path document,
      int lineNumber,
      String originalTarget,
      String targetWithoutAnchor,
      List<String> violations) {
    String decodedTarget;
    try {
      decodedTarget =
          URLDecoder.decode(
              targetWithoutAnchor.replace("+", "%2B"), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException exception) {
      violations.add(
          failure(
              repositoryRoot,
              document,
              lineNumber,
              "Local link target has invalid percent-encoding: " + originalTarget,
              "Correct the encoded path in the Markdown link."));
      return;
    }

    Path target;
    try {
      target = document.getParent().resolve(decodedTarget).normalize();
    } catch (InvalidPathException exception) {
      violations.add(
          failure(
              repositoryRoot,
              document,
              lineNumber,
              "Local link target is not a valid filesystem path: " + originalTarget,
              "Replace it with a valid repository-relative path."));
      return;
    }

    if (!target.startsWith(repositoryRoot)) {
      violations.add(
          failure(
              repositoryRoot,
              document,
              lineNumber,
              "Local link target escapes the repository: " + originalTarget,
              "Point the link to a target inside the repository."));
      return;
    }

    if (!existsWithExactCase(repositoryRoot, target)) {
      violations.add(
          failure(
              repositoryRoot,
              document,
              lineNumber,
              "Relative local link target does not exist with exact path casing: "
                  + originalTarget,
              "Restore "
                  + repositoryPath(repositoryRoot, target)
                  + " or update the link to the target's exact repository path."));
      return;
    }

    verifyRealPathContainment(
        repositoryRoot, document, lineNumber, originalTarget, target, violations);
  }

  private static boolean existsWithExactCase(Path repositoryRoot, Path target) {
    Path current = repositoryRoot;
    for (Path segment : repositoryRoot.relativize(target)) {
      try (Stream<Path> children = Files.list(current)) {
        if (children.noneMatch(
            child -> child.getFileName().toString().equals(segment.toString()))) {
          return false;
        }
      } catch (IOException exception) {
        return false;
      }
      current = current.resolve(segment);
    }
    return Files.exists(current);
  }

  private static void verifyRealPathContainment(
      Path repositoryRoot,
      Path document,
      int lineNumber,
      String originalTarget,
      Path target,
      List<String> violations) {
    try {
      if (!target.toRealPath().startsWith(repositoryRoot.toRealPath())) {
        violations.add(
            failure(
                repositoryRoot,
                document,
                lineNumber,
                "Local link target resolves outside the repository through a symbolic link: "
                    + originalTarget,
                "Point the link to a target whose real path remains inside the repository."));
      }
    } catch (IOException exception) {
      violations.add(
          failure(
              repositoryRoot,
              document,
              lineNumber,
              "Local link target real path cannot be resolved: " + originalTarget,
              "Restore a resolvable in-repository target or update the link."));
    }
  }

  private static String repositoryPath(Path repositoryRoot, Path path) {
    Path normalized = path.toAbsolutePath().normalize();
    if (!normalized.startsWith(repositoryRoot)) {
      return normalized.toString().replace('\\', '/');
    }
    return repositoryRoot.relativize(normalized).toString().replace('\\', '/');
  }

  private static String failure(
      Path repositoryRoot,
      Path document,
      int lineNumber,
      String reason,
      String fix) {
    return """
        Location: %s:%d
        Invariant: every relative local link in root-level *.md and docs/**/*.md must resolve
        to an existing repository target after its anchor is removed.
        Reason: %s
        Fix: %s
        Recheck: .\\mvnw.cmd -Dtest=DocumentationConsistencyTest test, then
        .\\mvnw.cmd verify (Windows); use ./mvnw with the same goals on macOS/Linux.
        Authority: docs/decisions/030-repository-markdown-link-consistency.md and WORKFLOW.md.
        """
        .formatted(repositoryPath(repositoryRoot, document), lineNumber, reason, fix);
  }

  private record FenceMarker(
      char character, int length, boolean hasOnlyTrailingWhitespace) {}

  private record ParsedDestination(String source, int linkEnd) {}

  private record LinkDestination(String source, String unescaped) {}
}
