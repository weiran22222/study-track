# 执行计划 024：采用原生 grill-with-docs

状态：进行中（规划基线已形成；第二个迁移 PR 尚未实施，2026-07-30）

## 目标与边界

第二个迁移 PR 将实施
[Harness ADR 0001](../contexts/harness/docs/adr/0001-adopt-native-grill-with-docs.md)：把
固定上游快照的原生 `grill-with-docs` 作为人类显式 opt-in 的复杂设计访谈，激活
facilitator 的窄文档写权限、共享理解退出门禁，以及 coordinator 从已验证干净
`develop` 准备干净 `codex/*` 分支的有限自主权。

本计划不把 grilling 设为通用门禁，不 vendoring 技能或授权智能体安装、更新技能，不
改变 StudyTrack 产品行为、架构、构建、CI、脚本、required check 身份、分支保护、远程
权限或发布语义。当前规划 PR 也不激活上述工作流权限。

## 迁移前置条件

1. 本规划 PR 已经通过独立 evaluator 和同一 Subject SHA 的 required `verify`，并实际
   合入 `develop`；
2. 读取合并后的精确 `origin/develop` SHA，确认其 required push `verify` 成功，再使用
   既有安全更新入口得到干净且未分叉的本地 `develop`；
3. 只在上述已验证基线满足 ADR 约束时，由 coordinator 创建并切换迁移用
   `codex/*` 分支；
4. 运行环境实际提供固定快照所定义的原生 `grill-with-docs`、`grilling` 与
   `domain-modeling` 时才执行会话；本计划不授权静默安装、更新或从上游复制文件。

任何前置条件未满足都应停止；不得把计划中的 PR、CI、合并或技能可用性写成已发生事实。

## 精确文件与语义

迁移 PR 只计划修改以下文件：

- `AGENTS.md`：在稳定第一跳地图中增加 `CONTEXT-MAP.md`，说明处理跨上下文术语或复杂
  设计时按需读取；不复制 glossary、ADR 或访谈协议；
- `HARNESS.md`：记录固定的 `mattpocock/skills` commit 与三个路径是学习输入而非本地
  权威，记录显式 opt-in、非通用门禁、原生组合、不 vendoring，以及前三次会话的前瞻
  效果假设与证据边界；
- `docs/environment.md`：记录固定上游 commit
  `2ab958093e83e0ec752e6c1c5932da465bf23e0c` 与
  `skills/engineering/grill-with-docs/SKILL.md`、
  `skills/productivity/grilling/SKILL.md`、
  `skills/engineering/domain-modeling/SKILL.md` 三个精确路径，提供由用户显式管理的
  安装选择和技能不可用时的诊断指引；明确智能体不得静默安装或更新技能，也不得把诊断
  成功写成仓库已安装证明；
- `WORKFLOW.md`：激活一次一问且附推荐答案、事实先自查、facilitator 仅写 context map/
  glossary/context ADR、人类显式共享理解退出、coordinator 仅从已验证干净 `develop`
  准备干净 `codex/*` 分支的规则；保持 generator/evaluator、冻结 SHA、stage/commit/
  push/GitHub 与发布权限的既有边界；
- `src/test/java/com/example/studytrack/architecture/DocumentationNavigationTest.java`：
  检查 context map 第一跳、两个 glossary 与 ADR 可发现，固定 commit/三个上游路径、
  explicit opt-in、非通用门禁、不 vendoring、facilitator 写范围、共享理解退出门禁、
  coordinator 分支限制、用户显式管理安装/诊断且禁止静默安装更新、前三次观察假设均未
  漂移，并继续给出六字段可行动诊断；
- `docs/README.md`：保持 context map、两个 glossary 与 ADR 索引，迁移完成后把计划链接
  更新到 `exec-plans/completed/024-adopt-native-grill-with-docs.md`；
- `docs/exec-plans/024-adopt-native-grill-with-docs.md`：只按真实本地实施与 generator
  自检更新事实，完成后移入 `completed/`。

`CONTEXT-MAP.md`、两个 `CONTEXT.md` 与 Harness ADR 0001 是迁移输入，默认不在第二个 PR
改写。若实施发现必须改变它们的已批准语义、扩大上述文件集合，或需要触碰
`SPEC.md`、`ARCHITECTURE.md`、产品代码、`docs/decisions/`、CI、脚本、构建或远程设置，
立即停止并交还人类决策。

## 实施顺序与职责

1. coordinator 在满足前置条件后准备迁移分支，并把 ADR、精确范围和禁止项交给
   generator；这次有限分支动作不扩大其他 Git/GitHub 权限；
2. generator 先更新 `HARNESS.md` 与 `WORKFLOW.md` 的当前语义，并在
   `docs/environment.md` 增加固定快照、用户显式管理安装和不可用诊断指引，再更新
   `AGENTS.md` 的第一跳导航、文档索引和单一职责测试；
3. generator 运行相关文档测试、JDK 21 环境自检和完整 Maven `verify`，只报告
   generator 自检；不得宣称最终 `PASS` 或远程状态；
4. generator 停止写入后，coordinator 审查、精确提交并冻结
   `FROZEN(<Subject SHA>)`；
5. 与 generator 不同且不继承其对话的 evaluator 在前置 guard 后做只读独立验证，并在
   后置 guard 前后保持 Subject SHA、`HEAD`、工作树和暂存区不变；
6. `FAIL` 回流 generator，任何修复产生新 SHA 后旧报告失效；`INCONCLUSIVE` 不视为
   通过；
7. 只有 evaluator `PASS` 与 required `verify` 覆盖同一 Subject SHA，coordinator 才能
   进入正常 PR 合并判断；合并和最终 `develop` 状态仍以实际 GitHub 记录为准。

## 风险与控制

- **把访谈扩成普遍门禁**：测试要求人类显式调用并保留“非通用门禁”语义，简单任务沿用
  现有流程；
- **facilitator 越权实现**：写范围精确限制到 context map、匹配 glossary 与 context
  ADR；共享理解确认前禁止实施交接；
- **推荐答案冒充决定**：每个真实取舍由人类回答，facilitator 只记录已解决内容；
- **分支自主权绕过人类掌舵**：只允许从已远程验证且安全更新的干净 `develop` 创建、
  切换干净 `codex/*`；不扩大提交、推送、PR、合并或发布权限；
- **上游漂移或本地分叉**：链接固定 commit 和三个精确路径，禁止 vendoring；任何升级
  另走 Harness 决策；
- **静默改变用户技能环境**：环境文档只提供用户显式管理的安装选择与只读诊断方向；
  智能体不自动安装、更新或把本地可用性冒充仓库状态；
- **Decision/ADR 双重权威**：决策卡保存跨仓库历史理由，context ADR 保存所属上下文的
  重大取舍；不迁移既有 28 张决策卡；
- **glossary 变成第二套规格**：测试与审查只允许简短“是什么”定义，行为、验收和实现
  继续引用既有权威；
- **效果倒推或只报成功**：前三次显式会话前瞻观察，无可靠量化基线；保留反例、成本和
  `证据不足`，不以合并或绿色检查替代效果；
- **新增文档类别漏索引**：相关测试遍历或精确检查 contexts/ADR 导航；只在既有测试因
  新类别不识别而失败时做最小直接调整，不扩张通用文档框架。

## 验收与测试

generator 在 JDK 21 中执行：

```powershell
.\scripts\check-environment.ps1
.\mvnw.cmd "-Dtest=DocumentationNavigationTest" test
.\mvnw.cmd verify
```

验收要求：

- context map、两个 glossary、ADR 与计划从稳定入口可发现，所有本地 Markdown 链接解析；
- 当前 `HARNESS.md` 与 `WORKFLOW.md` 精确表达 ADR 的 opt-in、权限、退出门禁、上游快照、
  职责和效果观察语义；
- `docs/environment.md` 精确列出固定 commit 和三个路径，提供用户显式管理安装与不可用
  诊断指引，并禁止智能体静默安装或更新；
- `AGENTS.md` 仍是简短第一跳地图，不复制工作流或历史清单；
- 相关测试和完整 `verify` 通过，且未出现产品、架构、CI、脚本、构建或权限范围外改动；
- generator 结果仍只是自检；最终完成还要求不同 evaluator 对冻结 Subject SHA 给出
  `PASS`，且 required `verify` 覆盖同一 SHA。

## 证据边界

本文件当前只记录获批准的迁移意图、风险、命令与门禁。第二个 PR、分支准备、技能可用性、
generator 实施、冻结 Subject SHA、evaluator 结论、required CI、PR 合并、最终
`develop` 验证和前三次会话效果均尚未由本计划证明。发生后只记录实际事实，不预写
成功；远程状态继续以 GitHub 权威记录为准。

## 当前规划 PR 的 generator 自检事实

- 当前 shell 最初使用 Java 17，环境自检按预期失败；只为后续验证子进程选择本机
  `D:\work\jdk\jdk-21.0.11` 后，重新运行相同入口通过，报告 Java 21 与 Maven Wrapper
  3.9.12；
- `DocumentationNavigationTest` 实际运行 7 项测试，0 失败、0 错误、0 跳过，
  Checkstyle 0 违规；既有测试能够识别当前新增导航，因此没有修改测试文件；
- 完整 `.\mvnw.cmd verify` 实际运行 142 项测试，0 失败、0 错误、0 跳过，Checkstyle
  0 违规，并成功完成可执行 JAR 打包。

以上只证明当前规划文档的 generator 本地自检，不表示第二个迁移 PR 已实施，也不是
evaluator `PASS`、required CI、PR、合并、最终 `develop` 验证、技能安装或 Harness
效果证据。
