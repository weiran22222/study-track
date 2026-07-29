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
  private static final Path HARNESS = Path.of("HARNESS.md");
  private static final Path DOCUMENT_INDEX = Path.of("docs", "README.md");
  private static final String UPSTREAM_SNAPSHOT =
      "github.com/deusyu/harness-engineering/blob/"
          + "90208d60687e47eb350606a584837e4cce7ab403/";
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
  void agentDocumentationMapKeepsStableFirstHopAndProgressiveDisclosure()
      throws IOException {
    String agents = readRequiredFile(AGENTS);
    String map =
        requiredSection(
            agents, "## 开始工作前：文档地图", "## 权威事实与机械验收");
    Set<String> linkTargets = markdownLinkTargets(map);

    assertContainsAll(
        AGENTS,
        map,
        Set.of(
            "工作约定",
            "当前权威事实",
            "辅助说明",
            "历史入口",
            "历史工件",
            "docs/decisions/",
            "docs/exec-plans/",
            "docs/evidence/",
            "docs/feedback/",
            "根级稳定文档",
            "渐进查阅",
            "只提供第一跳",
            "不逐项列出历史工件",
            "不形成完整文件清单"),
        "Restore the compact first-hop map and progressive history disclosure.");

    for (String stableEntry :
        Set.of(
            "AGENTS.md",
            "HARNESS.md",
            "SPEC.md",
            "ARCHITECTURE.md",
            "docs/README.md",
            "docs/environment.md")) {
      assertTrue(
          linkTargets.contains(stableEntry),
          failure(
              AGENTS,
              "The documentation map no longer links stable entry: " + stableEntry,
              "Restore the stable first-hop link and its reading guidance."));
    }

    assertFalse(
        linkTargets.stream()
            .anyMatch(
                target ->
                    target.startsWith("docs/decisions/")
                        || target.startsWith("docs/exec-plans/")
                        || target.startsWith("docs/evidence/")
                        || target.startsWith("docs/feedback/")),
        failure(
            AGENTS,
            "The first-hop map links individual historical artifacts.",
            "Keep historical categories descriptive and disclose files through docs/README.md."));
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
    String agents = readRequiredFile(AGENTS);
    String index = readRequiredFile(DOCUMENT_INDEX);
    Set<String> linkTargets = markdownLinkTargets(index);

    assertTrue(
        linkTargets.contains("environment.md"),
        failure(
            DOCUMENT_INDEX,
            "The build environment guide is not discoverable from the documentation index.",
            "Add a Markdown link to environment.md under the important documentation section."));

    assertLocalMarkdownLinksResolve(AGENTS, agents);
    assertLocalMarkdownLinksResolve(DOCUMENT_INDEX, index);
  }

  private static void assertLocalMarkdownLinksResolve(Path document, String markdown) {
    Path parent = document.getParent();
    Path base = parent == null ? Path.of("") : parent;

    for (String linkTarget : markdownLinkTargets(markdown)) {
      if (!linkTarget.endsWith(".md")) {
        continue;
      }
      Path linkedDocument = base.resolve(linkTarget).normalize();
      assertTrue(
          Files.isRegularFile(linkedDocument),
          failure(
              document,
              "A local Markdown link points to a missing file: " + linkTarget,
              "Restore "
                  + linkedDocument
                  + " or update "
                  + document
                  + " to the current Markdown path."));
    }
  }

  @Test
  void harnessPurposeAndEffectProtocolStayDiscoverable() throws IOException {
    String agents = readRequiredFile(AGENTS);
    String harness = readRequiredFile(HARNESS);
    String index = readRequiredFile(DOCUMENT_INDEX);

    assertAll(
        () ->
            assertTrue(
                agents.contains("](HARNESS.md)"),
                failure(
                    AGENTS,
                    "AGENTS.md no longer links the stable Harness purpose document.",
                    "Add a concise Markdown link to HARNESS.md without copying its protocol.")),
        () ->
            assertTrue(
                markdownLinkTargets(index).contains("../HARNESS.md"),
                failure(
                    DOCUMENT_INDEX,
                    "The current-fact index no longer links the Harness purpose document.",
                    "Add ../HARNESS.md to the current-fact navigation.")));

    assertContainsAll(
        HARNESS,
        harness,
        Set.of(
            "终极目标",
            "Harness 变化落地",
            "受控实验载体",
            "[SPEC.md](SPEC.md)",
            "完整产品行为与验收标准的唯一权威",
            "学习输入，不自动成为本仓库权威",
            UPSTREAM_SNAPSHOT),
        "Restore the three-layer purpose and authority boundary in HARNESS.md.");

    assertContainsAll(
        HARNESS,
        harness,
        Set.of(
            "人类掌舵、智能体执行",
            "仓库即记录系统",
            "地图而非手册",
            "机械化执行",
            "智能体可读性",
            "反馈回路",
            "熵管理"),
        "Restore the locally adopted Harness Engineering directions.");

    assertContainsAll(
        HARNESS,
        harness,
        Set.of(
            "落地是评估前提，不是正向效果证明",
            "结果正确性",
            "自主性与人类掌舵负担",
            "反馈回路有效性",
            "可复现性与可追踪性",
            "交付效率",
            "熵与维护成本"),
        "Restore the landing/effect distinction and all six effect dimensions.");

    assertContainsAll(
        HARNESS,
        harness,
        Set.of(
            "变化与假设",
            "观察单元",
            "适用维度",
            "基线状态",
            "无基线",
            "实际结果与证据",
            "反例与残余缺口",
            "维护成本",
            "正向 | 混合 | 无明显效果 | 负向 | 证据不足"),
        "Restore every field and allowed conclusion in the minimal evaluation statement.");

    assertContainsAll(
        HARNESS,
        harness,
        Set.of(
            "PR diff/review",
            "GitHub Actions",
            "冻结 Subject SHA",
            "generator/evaluator 报告",
            "现有反馈记录",
            "不强制创建重复 evidence 文件",
            "不得补造",
            "PR 数",
            "提交数",
            "代码量",
            "文档数量",
            "检查数量",
            "智能体数量",
            "流程步骤数量",
            "一次绿色 `verify`",
            "一次 evaluator `PASS`",
            "一次 PR 合并",
            "不能单独证明 Harness 产生正向效果"),
        "Restore the evidence-reuse, no-fabrication, and anti-metric boundaries.");
  }

  @Test
  void documentationFailuresRemainActionable() {
    String diagnostic = failure(HARNESS, "example reason", "example fix");

    assertContainsAll(
        HARNESS,
        diagnostic,
        Set.of("Location:", "Invariant:", "Reason:", "Fix:", "Recheck:", "Authority:"),
        "Restore all six actionable diagnostic fields in failure().");
  }

  private static void assertContainsAll(
      Path location, String content, Set<String> required, String fix) {
    for (String anchor : required) {
      assertTrue(
          content.contains(anchor),
          failure(
              location,
              "Required stable documentation anchor is missing: " + anchor,
              fix));
    }
  }

  private static Set<String> markdownLinkTargets(String markdown) {
    Matcher matcher = MARKDOWN_LINK.matcher(markdown);
    return matcher.results().map(result -> result.group(1)).collect(Collectors.toSet());
  }

  private static String requiredSection(String markdown, String heading, String nextHeading) {
    int start = markdown.indexOf(heading);
    int end = markdown.indexOf(nextHeading);
    assertTrue(
        start >= 0 && end > start,
        failure(
            AGENTS,
            "The stable documentation map section cannot be located.",
            "Restore the documentation map before the authority and verification section."));
    return markdown.substring(start, end);
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
        Invariant: AGENTS.md must remain a stable map, HARNESS.md must preserve the project
        purpose and evaluation contract, and managed documents must remain discoverable.
        Reason: %s
        Fix: %s
        Recheck: .\\mvnw.cmd -Dtest=DocumentationNavigationTest test, then .\\mvnw.cmd verify.
        Authority: HARNESS.md, docs/decisions/009-documentation-entropy-control.md,
        docs/decisions/024-harness-effect-validation-goal.md,
        docs/decisions/025-documentation-map-navigation.md, and docs/README.md
        """
        .formatted(location, reason, fix);
  }
}
