package com.example.studytrack.architecture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DocumentationNavigationTest {

  private static final Path AGENTS = Path.of("AGENTS.md");
  private static final Path WORKFLOW = Path.of("WORKFLOW.md");
  private static final Path HARNESS = Path.of("HARNESS.md");
  private static final Path HARNESS_CAPABILITIES = Path.of("HARNESS-CAPABILITIES.md");
  private static final Path CONTEXT_MAP = Path.of("CONTEXT-MAP.md");
  private static final Path STUDY_TRACK_CONTEXT =
      Path.of("docs", "contexts", "study-track", "CONTEXT.md");
  private static final Path HARNESS_CONTEXT =
      Path.of("docs", "contexts", "harness", "CONTEXT.md");
  private static final Path HARNESS_ADR =
      Path.of(
          "docs",
          "contexts",
          "harness",
          "docs",
          "adr",
          "0001-adopt-native-grill-with-docs.md");
  private static final Path ENVIRONMENT = Path.of("docs", "environment.md");
  private static final Path DOCUMENT_INDEX = Path.of("docs", "README.md");
  private static final Path SLIM_NAVIGATION_DECISION =
      Path.of("docs", "decisions", "026-slim-agent-navigation.md");
  private static final Path HARNESS_CAPABILITY_DECISION =
      Path.of("docs", "decisions", "029-harness-capability-trust-map.md");
  private static final String UPSTREAM_SNAPSHOT =
      "github.com/deusyu/harness-engineering/blob/"
          + "90208d60687e47eb350606a584837e4cce7ab403/";
  private static final String GRILL_COMMIT = "2ab958093e83e0ec752e6c1c5932da465bf23e0c";
  private static final String GRILL_UPSTREAM_PREFIX =
      "github.com/mattpocock/skills/blob/" + GRILL_COMMIT + "/";
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
            agents, "## 开始工作前：文档地图", "## 根本原则");
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
            "WORKFLOW.md",
            "HARNESS.md",
            "HARNESS-CAPABILITIES.md",
            "SPEC.md",
            "ARCHITECTURE.md",
            "CONTEXT-MAP.md",
            "docs/README.md",
            "docs/environment.md")) {
      assertTrue(
          linkTargets.contains(stableEntry),
          failure(
              AGENTS,
              "The documentation map no longer links stable entry: " + stableEntry,
              "Restore the stable first-hop link and its reading guidance."));
    }

    assertTrue(
        map.contains("任何仓库修改前必须读取"),
        failure(
            AGENTS,
            "The workflow is no longer mandatory before repository modifications.",
            "Mark WORKFLOW.md as required reading before any repository modification."));

    assertFalse(
        markdownLinkTargets(agents).stream()
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
  void slimAgentNavigationKeepsTheCompleteWorkflowDiscoverable() throws IOException {
    String agents = readRequiredFile(AGENTS);
    String workflow = readRequiredFile(WORKFLOW);
    String index = readRequiredFile(DOCUMENT_INDEX);
    List<String> secondLevelHeadings =
        Pattern.compile("(?m)^## (.+)$")
            .matcher(agents)
            .results()
            .map(result -> result.group(1))
            .toList();

    assertEquals(
        List.of("项目目标", "开始工作前：文档地图", "根本原则"),
        secondLevelHeadings,
        failure(
            AGENTS,
            "AGENTS.md no longer contains exactly the three stable navigation sections.",
            "Move operational sections to WORKFLOW.md and retain only the three approved "
                + "headings."));
    assertFalse(
        Pattern.compile("(?m)^### ").matcher(agents).find(),
        failure(
            AGENTS,
            "AGENTS.md contains a third-level operational section.",
            "Keep operational detail in WORKFLOW.md."));

    assertAll(
        () ->
            assertTrue(
                markdownLinkTargets(agents).contains("WORKFLOW.md"),
                failure(
                    AGENTS,
                    "The documentation map no longer links WORKFLOW.md.",
                    "Restore WORKFLOW.md as the required modification workflow.")),
        () ->
            assertTrue(
                markdownLinkTargets(index).contains("../WORKFLOW.md"),
                failure(
                    DOCUMENT_INDEX,
                    "The current-fact index no longer links WORKFLOW.md.",
                    "Restore ../WORKFLOW.md to the current-fact navigation.")),
        () ->
            assertTrue(
                workflow.contains("任何仓库修改前必须先读取 [AGENTS.md](AGENTS.md)")
                    && agents.contains("任何仓库修改前必须读取"),
                failure(
                    WORKFLOW,
                    "The mandatory AGENTS.md-to-WORKFLOW.md reading sequence is incomplete.",
                    "Restore reciprocal context and require WORKFLOW.md before modifications.")));

    assertContainsAll(
        WORKFLOW,
        workflow,
        Set.of(
            "## 权威事实与机械验收",
            "## 关键边界",
            "## Harness 变更权限与风险分级",
            "### 第一级：事实修正",
            "### 第二级：小型 Harness 决策",
            "### 第三级：重大或多步骤变更",
            "### 工件单一职责",
            "### 所有级别的不变边界",
            "## 标准工作流",
            "### 角色与冻结 SHA 交接",
            "## 提交前暂存检查",
            "## 验证范围",
            "## 分支工作流",
            "### 本地 develop 安全更新",
            "## 验证命令",
            "## 完成定义",
            "## 失败反馈要求"),
        "Restore every stable operational section migrated from AGENTS.md.");

    assertContainsAll(
        WORKFLOW,
        workflow,
        Set.of(
            "不得实现 [SPEC.md](SPEC.md)“本版本不包含”中的功能",
            "修改架构前必须先更新 [ARCHITECTURE.md](ARCHITECTURE.md)",
            "修改产品行为前必须先更新 [SPEC.md](SPEC.md) 及验收标准",
            "不得绕过受保护 `main`、必需 PR 或 `verify`",
            "**generator**",
            "**evaluator**",
            "**协调者**",
            "Mutation allowed: no",
            "scripts/check-verification-subject.ps1",
            "git diff --cached --check",
            "`pull_request` 未按目标分支过滤",
            "activity types 采用 GitHub 对未配置 `types` 的默认集合",
            "`push` 只匹配 `develop` 与 `main`",
            "工作分支 push 不触发 CI",
            "`develop → main` PR",
            "`hotfix/* → main` PR",
            "update-local-develop.ps1",
            "update-local-develop.sh",
            ".\\scripts\\check-environment.ps1",
            ".\\mvnw.cmd verify",
            "./mvnw verify",
            "required `verify` 覆盖同一 SHA",
            "错误位置",
            "权威文档链接"),
        "Restore migrated commands, role boundaries, branch rules, and completion diagnostics.");
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
    String workflow = readRequiredFile(WORKFLOW);
    final String harness = readRequiredFile(HARNESS);
    final String harnessCapabilities = readRequiredFile(HARNESS_CAPABILITIES);
    final String contextMap = readRequiredFile(CONTEXT_MAP);
    final String harnessAdr = readRequiredFile(HARNESS_ADR);
    final String slimNavigationDecision = readRequiredFile(SLIM_NAVIGATION_DECISION);
    final String harnessCapabilityDecision =
        readRequiredFile(HARNESS_CAPABILITY_DECISION);
    String index = readRequiredFile(DOCUMENT_INDEX);
    Set<String> linkTargets = markdownLinkTargets(index);

    assertTrue(
        linkTargets.contains("environment.md"),
        failure(
            DOCUMENT_INDEX,
            "The build environment guide is not discoverable from the documentation index.",
            "Add a Markdown link to environment.md under the important documentation section."));

    assertLocalMarkdownLinksResolve(AGENTS, agents);
    assertLocalMarkdownLinksResolve(WORKFLOW, workflow);
    assertLocalMarkdownLinksResolve(HARNESS, harness);
    assertLocalMarkdownLinksResolve(HARNESS_CAPABILITIES, harnessCapabilities);
    assertLocalMarkdownLinksResolve(CONTEXT_MAP, contextMap);
    assertLocalMarkdownLinksResolve(HARNESS_ADR, harnessAdr);
    assertLocalMarkdownLinksResolve(SLIM_NAVIGATION_DECISION, slimNavigationDecision);
    assertLocalMarkdownLinksResolve(HARNESS_CAPABILITY_DECISION, harnessCapabilityDecision);
    assertLocalMarkdownLinksResolve(DOCUMENT_INDEX, index);
  }

  @Test
  void contextLanguageAndNativeGrillDecisionRemainDiscoverable() throws IOException {
    String agents = readRequiredFile(AGENTS);
    String contextMap = readRequiredFile(CONTEXT_MAP);
    String index = readRequiredFile(DOCUMENT_INDEX);
    Set<String> mapTargets = markdownLinkTargets(contextMap);
    Set<String> indexTargets = markdownLinkTargets(index);

    assertAll(
        () ->
            assertTrue(
                markdownLinkTargets(agents).contains("CONTEXT-MAP.md"),
                failure(
                    AGENTS,
                    "The stable first-hop map no longer links CONTEXT-MAP.md.",
                    "Restore CONTEXT-MAP.md for cross-context terminology and complex design.")),
        () ->
            assertTrue(
                mapTargets.contains("docs/contexts/study-track/CONTEXT.md"),
                failure(
                    CONTEXT_MAP,
                    "The StudyTrack glossary is no longer discoverable from the context map.",
                    "Restore the StudyTrack CONTEXT.md link in CONTEXT-MAP.md.")),
        () ->
            assertTrue(
                mapTargets.contains("docs/contexts/harness/CONTEXT.md"),
                failure(
                    CONTEXT_MAP,
                    "The Harness glossary is no longer discoverable from the context map.",
                    "Restore the Harness CONTEXT.md link in CONTEXT-MAP.md.")),
        () ->
            assertTrue(
                indexTargets.contains("contexts/study-track/CONTEXT.md")
                    && indexTargets.contains("contexts/harness/CONTEXT.md")
                    && indexTargets.contains(
                        "contexts/harness/docs/adr/0001-adopt-native-grill-with-docs.md"),
                failure(
                    DOCUMENT_INDEX,
                    "A native grill-with-docs context input is missing from the index.",
                    "Restore both glossary links and Harness ADR 0001 in docs/README.md.")),
        () ->
            assertTrue(
                Files.isRegularFile(STUDY_TRACK_CONTEXT)
                    && Files.isRegularFile(HARNESS_CONTEXT)
                    && Files.isRegularFile(HARNESS_ADR),
                failure(
                    DOCUMENT_INDEX,
                    "A linked context glossary or Harness ADR 0001 is missing.",
                    "Restore the documented context artifact at its indexed path.")));
  }

  @Test
  void nativeGrillWithDocsContractAndRuntimeBoundaryDoNotDrift() throws IOException {
    String harness = readRequiredFile(HARNESS);
    String workflow = readRequiredFile(WORKFLOW);
    String environment = readRequiredFile(ENVIRONMENT);

    assertContainsAll(
        HARNESS,
        harness,
        Set.of(
            GRILL_UPSTREAM_PREFIX + "skills/engineering/grill-with-docs/SKILL.md",
            GRILL_UPSTREAM_PREFIX + "skills/productivity/grilling/SKILL.md",
            GRILL_UPSTREAM_PREFIX + "skills/engineering/domain-modeling/SKILL.md",
            "人类显式调用",
            "opt-in",
            "通用门禁",
            "原生 `grill-with-docs` 组合",
            "不 vendoring",
            "前三次由人类显式调用的 grilling 会话",
            "至少一次 StudyTrack",
            "至少一次 Harness",
            "第三次主题不限",
            "当前无可靠量化",
            "基线，不倒推历史",
            "都不能单独证明正向"),
        "Restore the pinned native composition, explicit opt-in, and prospective evidence "
            + "boundary.");

    assertContainsAll(
        WORKFLOW,
        workflow,
        Set.of(
            "一次只提出一个",
            "附推荐答案",
            "facilitator 先自行查明",
            "`CONTEXT-MAP.md`",
            "对应 context 的 `CONTEXT.md`",
            "该 context 的 `docs/adr/*.md`",
            "只记录人类已经解决的术语与决定",
            "共享理解是人类显式确认的退出门禁",
            "确认前不得开始实现、创建实现交接或宣称规划完成",
            "完成上述远程验证和安全更新后",
            "安全更新",
            "干净且未分叉",
            "新建、干净的 `codex/*`",
            "不得用于其他基线",
            "不扩大 stage、commit、push、PR、merge、rebase、cherry-pick、GitHub"),
        "Restore the facilitator write scope, human exit gate, and coordinator branch limit.");

    assertContainsAll(
        ENVIRONMENT,
        environment,
        Set.of(
            GRILL_COMMIT,
            "skills/engineering/grill-with-docs/SKILL.md",
            "skills/productivity/grilling/SKILL.md",
            "skills/engineering/domain-modeling/SKILL.md",
            "由用户",
            "显式选择安装或启用固定快照",
            "只做只读诊断",
            "停止会话并请用户选择",
            "不得静默安装、启用、更新或替换技能",
            "不得把只读诊断",
            "成功描述成“仓库已安装”"),
        "Restore user-managed installation, read-only diagnosis, and no-silent-update rules.");
  }

  private static void assertLocalMarkdownLinksResolve(Path document, String markdown) {
    Path parent = document.getParent();
    Path base = parent == null ? Path.of("") : parent;

    for (String linkTarget : markdownLinkTargets(markdown)) {
      if (!linkTarget.endsWith(".md") || linkTarget.contains("://")) {
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
  void harnessCapabilityMapKeepsItsDescriptiveTrustBoundary() throws IOException {
    String agents = readRequiredFile(AGENTS);
    String harness = readRequiredFile(HARNESS);
    String capabilities = readRequiredFile(HARNESS_CAPABILITIES);
    String index = readRequiredFile(DOCUMENT_INDEX);
    Set<String> capabilityTargets = markdownLinkTargets(capabilities);

    assertAll(
        () ->
            assertTrue(
                markdownLinkTargets(agents).contains("HARNESS-CAPABILITIES.md"),
                failure(
                    AGENTS,
                    "The stable first-hop map no longer links the Harness capability map.",
                    "Restore HARNESS-CAPABILITIES.md as a concise Harness navigation entry.")),
        () ->
            assertTrue(
                markdownLinkTargets(index).contains("../HARNESS-CAPABILITIES.md"),
                failure(
                    DOCUMENT_INDEX,
                    "The current-fact index no longer links the Harness capability map.",
                    "Restore ../HARNESS-CAPABILITIES.md under current facts.")),
        () ->
            assertTrue(
                markdownLinkTargets(index)
                    .contains("decisions/029-harness-capability-trust-map.md"),
                failure(
                    DOCUMENT_INDEX,
                    "Decision 029 is no longer discoverable from the decision index.",
                    "Restore the decision 029 link in docs/README.md.")),
        () ->
            assertTrue(
                markdownLinkTargets(harness).contains("HARNESS-CAPABILITIES.md"),
                failure(
                    HARNESS,
                    "HARNESS.md no longer declares the capability map's narrow responsibility.",
                    "Restore the capability-map authority boundary in HARNESS.md.")));

    assertContainsAll(
        HARNESS_CAPABILITIES,
        capabilities,
        Set.of(
            "只描述与导航",
            "不授予权限",
            "不能替代",
            "## 权威来源与触发条件",
            "## 当前能力表面",
            "上下文与指导",
            "机械反馈",
            "编排与状态",
            "工具与运行时",
            "## 证据边界",
            "## 效果观察边界",
            "正向因果证明不是 Harness 交付的完成条件",
            "不是尚待补齐的 Harness 能力缺口",
            "## 已知部分覆盖与未覆盖",
            "验证中状态与报告留存",
            "当前仓库没有机械强制的持久存储通道",
            "报告必须留在被验证提交之外",
            "熵管理外循环",
            "仓库不提供定时漂移扫描、质量评分或自动维护 PR 机制",
            "不证明任何插件或技能在当前运行时可用",
            "不证明智能体身份、沙箱状态、远程",
            "状态或未来工作"),
        "Restore the descriptive-only responsibility, core categories, and explicit "
            + "trust boundaries.");

    for (String boundarySource :
        Set.of(
            "WORKFLOW.md",
            "HARNESS.md",
            "docs/decisions/008-accept-atomic-replacement-test-gap.md",
            "docs/decisions/021-generator-evaluator-role-separation.md",
            "docs/decisions/009-documentation-entropy-control.md",
            "docs/environment.md",
            "docs/feedback/005-current-harness-effect-baseline.md")) {
      assertTrue(
          capabilityTargets.contains(boundarySource),
          failure(
              HARNESS_CAPABILITIES,
              "A stable capability or trust-boundary source is no longer linked: "
                  + boundarySource,
              "Restore the focused boundary source without enumerating a volatile ledger."));
    }

    String decision = readRequiredFile(HARNESS_CAPABILITY_DECISION);
    assertContainsAll(
        HARNESS_CAPABILITY_DECISION,
        decision,
        Set.of(
            "日期：2026-07-30",
            "用户于 2026-07-30 明确批准",
            "第二级小型 Harness 决策",
            "## 候选方案与取舍",
            "## 非目标与不变边界",
            "不证明插件或技能可用、智能体身份、沙箱状态、远程状态或未来工作"),
        "Restore decision 029's approval, alternatives, tradeoffs, and non-goals.");
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
        purpose, evaluation, and native grill-with-docs contracts, HARNESS-CAPABILITIES.md
        must remain descriptive-only, WORKFLOW.md must preserve role permissions and gates,
        and managed documents must remain discoverable.
        Reason: %s
        Fix: %s
        Recheck: .\\mvnw.cmd -Dtest=DocumentationNavigationTest test, then .\\mvnw.cmd verify.
        Authority: HARNESS.md, docs/decisions/009-documentation-entropy-control.md,
        docs/decisions/024-harness-effect-validation-goal.md,
        docs/decisions/025-documentation-map-navigation.md,
        docs/decisions/026-slim-agent-navigation.md,
        docs/decisions/029-harness-capability-trust-map.md, WORKFLOW.md, and docs/README.md
        """
        .formatted(location, reason, fix);
  }
}
