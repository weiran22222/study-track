package com.example.studytrack.architecture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DocumentationNavigationTest {

  private static final Path AGENTS = Path.of("AGENTS.md");
  private static final Path DOCUMENT_INDEX = Path.of("docs", "README.md");
  private static final Set<Path> MANAGED_CATEGORIES =
      Set.of(
          Path.of("docs", "decisions"),
          Path.of("docs", "exec-plans"),
          Path.of("docs", "evidence"),
          Path.of("docs", "feedback"));
  private static final Pattern MARKDOWN_LINK =
      Pattern.compile("\\[[^]]*]\\(([^)#]+)(?:#[^)]*)?\\)");

  @Test
  void agentInstructionsStayStableWithoutHistoryLedger() throws IOException {
    String agents = readRequiredFile(AGENTS);

    assertAll(
        () ->
            assertTrue(
                agents.contains("](docs/README.md)"),
                failure(
                    AGENTS,
                    "AGENTS.md no longer links the central documentation index.",
                    "Add a Markdown link to docs/README.md as the history entry point.")),
        () ->
            assertFalse(
                agents.contains("docs/exec-plans/completed/"),
                failure(
                    AGENTS,
                    "AGENTS.md directly links an individual completed execution plan.",
                    "Remove the completed-plan link and register the plan in docs/README.md.")),
        () ->
            assertFalse(
                agents.contains("最近完成"),
                failure(
                    AGENTS,
                    "AGENTS.md uses the historical append pattern marked by '最近完成'.",
                    "Keep only concise current state and move historical navigation to "
                        + "docs/README.md.")));
  }

  @Test
  void everyManagedDocumentIsLinkedFromTheIndex() throws IOException {
    String index = readRequiredFile(DOCUMENT_INDEX);
    Set<String> linkTargets = markdownLinkTargets(index);

    for (Path category : MANAGED_CATEGORIES) {
      assertTrue(
          Files.isDirectory(category),
          failure(
              category,
              "A managed documentation category is missing.",
              "Restore the category and register its Markdown files in docs/README.md."));

      try (Stream<Path> files = Files.walk(category)) {
        for (Path document :
            files
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".md"))
                .toList()) {
          String relativeTarget =
              DOCUMENT_INDEX.getParent().relativize(document).toString().replace('\\', '/');
          assertTrue(
              linkTargets.contains(relativeTarget),
              failure(
                  DOCUMENT_INDEX,
                  "Managed document is not discoverable from the index: " + document,
                  "Add a Markdown link to "
                      + relativeTarget
                      + " under its docs/README.md category."));
        }
      }
    }
  }

  @Test
  void localMarkdownLinksAndEnvironmentEntryRemainValid() throws IOException {
    String index = readRequiredFile(DOCUMENT_INDEX);
    Set<String> linkTargets = markdownLinkTargets(index);

    assertTrue(
        linkTargets.contains("environment.md"),
        failure(
            DOCUMENT_INDEX,
            "The build environment guide is not discoverable from the documentation index.",
            "Add a Markdown link to environment.md under the important documentation section."));

    for (String linkTarget : linkTargets) {
      if (!linkTarget.endsWith(".md")) {
        continue;
      }
      Path linkedDocument = DOCUMENT_INDEX.getParent().resolve(linkTarget).normalize();
      assertTrue(
          Files.isRegularFile(linkedDocument),
          failure(
              DOCUMENT_INDEX,
              "A local Markdown link points to a missing file: " + linkTarget,
              "Restore "
                  + linkedDocument
                  + " or update docs/README.md to the current Markdown path."));
    }
  }

  private static Set<String> markdownLinkTargets(String markdown) {
    Matcher matcher = MARKDOWN_LINK.matcher(markdown);
    return matcher.results().map(result -> result.group(1)).collect(Collectors.toSet());
  }

  private static String readRequiredFile(Path file) throws IOException {
    assertTrue(
        Files.isRegularFile(file),
        failure(
            file,
            "A required documentation navigation file is missing.",
            "Restore the file and its required navigation content."));
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  private static String failure(Path location, String reason, String fix) {
    return """
        Documentation navigation invariant violated.
        Location: %s
        Invariant: AGENTS.md must remain a stable map, while every Markdown file under
        docs/decisions, docs/exec-plans, docs/evidence, and docs/feedback is discoverable
        through docs/README.md.
        Reason: %s
        Fix: %s
        Recheck: .\\mvnw.cmd -Dtest=DocumentationNavigationTest test, then .\\mvnw.cmd verify.
        Authority: docs/decisions/009-documentation-entropy-control.md and docs/README.md
        """
        .formatted(location, reason, fix);
  }
}
