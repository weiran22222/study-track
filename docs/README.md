# StudyTrack 文档索引

这里是历史决策、执行过程、验证证据和复盘反馈的统一入口。按当前任务只读取相关分类和
工件，避免把全部历史载入默认上下文。

## 当前事实与历史理由

- [产品规格](../SPEC.md)描述当前有效的产品行为和验收标准；
- [架构说明](../ARCHITECTURE.md)描述当前有效的技术选择、分层和依赖约束；
- [智能体导航](../AGENTS.md)描述项目目标、文档地图和根本原则；
- [操作工作流](../WORKFLOW.md)描述修改仓库前必须读取的权限、角色、门禁和命令；
- [Harness 目的与效果评估](../HARNESS.md)描述项目终极目的、实验载体和效果评估协议；
- [Harness 能力与信任边界图](../HARNESS-CAPABILITIES.md)只导航当前能力表面、触发条件、
  证据边界和已知覆盖缺口，不授予权限或替代权威协议；
- 决策记录解释现状形成的历史理由，可能被后续决策取代，不能凌驾于当前规格或架构之上。

历史工件中的状态、待办以及“尚未发生”等陈述，只绑定各工件记录时可用的证据截止点，
不是实时状态。判断当前事实时回到相应根级权威文档；判断 PR、Actions、分支保护或远端
SHA 等远程事实时回到 GitHub 权威记录。索引负责保留和渐进披露历史，不因后续状态变化
而批量回填或改写历史工件正文。

## 重要说明

- [构建环境](environment.md)：JDK 21、Maven Wrapper 和跨平台环境自检。

## Context 与通用语言

- [Context Map](../CONTEXT-MAP.md)：StudyTrack 与 Harness 上下文及其关系；
- [StudyTrack glossary](contexts/study-track/CONTEXT.md)：学习任务、任务 ID、标题、状态与
  任务库；
- [Harness glossary](contexts/harness/CONTEXT.md)：Harness 协作、验证与效果观察术语。

## Context ADR

- [Harness ADR 0001：采用原生 grill-with-docs](contexts/harness/docs/adr/0001-adopt-native-grill-with-docs.md)

## 决策记录

- [001：环境自举与冷启动检查](decisions/001-environment-bootstrap.md)
- [002：升级 GitHub Actions Node.js 24 运行时](decisions/002-actions-node24-upgrade.md)
- [003：公开仓库并建立 main 合并门禁](decisions/003-main-branch-gate.md)
- [004：并行 worktree 功能实验](decisions/004-parallel-worktree-experiment.md)
- [005：架构导航地图降粒度](decisions/005-architecture-navigation-map.md)
- [006：按风险分级的 Harness 变更流程](decisions/006-risk-tiered-harness-flow.md)
- [007：永久删除单个任务](decisions/007-delete-task.md)
- [008：接受原子替换失败的直接测试缺口](decisions/008-accept-atomic-replacement-test-gap.md)
- [009：控制智能体导航中的文档熵](decisions/009-documentation-entropy-control.md)
- [010：重命名单个任务](decisions/010-rename-task.md)
- [011：精简第三级工件与收尾流程](decisions/011-lean-tier3-artifacts.md)
- [012：使用 UTF-8 标题文件绕过 Windows 命令行编码边界](decisions/012-unicode-safe-title-file-input.md)
- [013：移除导航中的易变 Harness 进度](decisions/013-remove-volatile-harness-progress.md)
- [014：重新打开单个任务](decisions/014-reopen-task.md)
- [015：提交前检查完整暂存内容](decisions/015-staged-diff-check.md)
- [016：为 CI 增加 PR 完整差异空白门禁](decisions/016-ci-pr-diff-whitespace-gate.md)（“所有 push”触发范围已由 028 部分取代）
- [017：增强 reopen 幂等失败诊断](decisions/017-reopen-failure-diagnostics.md)
- [018：只读审计本地 worktree](decisions/018-worktree-hygiene-audit.md)
- [019：使用 Codex-managed Worktree/Handoff](decisions/019-codex-managed-worktree-lifecycle.md)（已由 022 取代）
- [020：develop 集成与 main 生产发布分支模型](decisions/020-develop-production-branch-model.md)
- [021：分离实现 generator 与独立验证 evaluator](decisions/021-generator-evaluator-role-separation.md)（交接细节已由 022 部分取代）
- [022：简化 generator/evaluator 交接](decisions/022-simplify-agent-handoff.md)
- [023：本地 develop 只从 origin/develop 安全纯快进](decisions/023-local-develop-fast-forward-policy.md)
- [024：以 Harness 落地效果验证为项目终极目标](decisions/024-harness-effect-validation-goal.md)
- [025：建立稳定的文档地图导航](decisions/025-documentation-map-navigation.md)
- [026：精简智能体导航并分离操作工作流](decisions/026-slim-agent-navigation.md)
- [027：按标题字面子串筛选任务列表](decisions/027-list-title-search.md)
- [028：精简 CI verify 触发](decisions/028-streamline-ci-triggers.md)
- [029：建立 Harness 能力与信任边界图](decisions/029-harness-capability-trust-map.md)
- [030：建立仓库 Markdown 本地链接一致性门禁](decisions/030-repository-markdown-link-consistency.md)
- [031：机械保护 PR evaluator 报告与合并后安全回流](decisions/031-pr-evaluator-report-lifecycle.md)
- [032：首次 Harness 文档与协议熵审计](decisions/032-harness-entropy-audit.md)

## 执行计划

进行中的计划直接保存在 `exec-plans/`，已完成计划保存在 `exec-plans/completed/`：

- [025：实施 PR evaluator 报告生命周期](exec-plans/completed/025-pr-evaluator-report-lifecycle.md)（仓库内实施已完成）
- [024：采用原生 grill-with-docs](exec-plans/completed/024-adopt-native-grill-with-docs.md)（仓库内实施已完成）
- [023：精简 CI verify 触发](exec-plans/completed/023-streamline-ci-triggers.md)（仓库内实施已完成）
- [022：按标题字面子串筛选任务列表](exec-plans/completed/022-list-title-search.md)（已完成）
- [021：建立 Harness 目的与落地效果验证框架](exec-plans/completed/021-harness-effect-validation-goal.md)（仓库内实施已完成）
- [020：实施本地 develop 安全纯快进策略](exec-plans/completed/020-local-develop-fast-forward-policy.md)（仓库内实施已完成）
- [019：简化 generator/evaluator 交接](exec-plans/completed/019-simplify-agent-handoff.md)（实施计划已完成）
- [018：实施 generator/evaluator 职责分离](exec-plans/completed/018-generator-evaluator-role-separation.md)（已完成）
- [017：迁移 develop 集成与 main 生产发布分支模型](exec-plans/completed/017-develop-production-branch-model.md)（已完成）
- [016：实施 CI PR 完整差异空白门禁](exec-plans/completed/016-ci-pr-diff-whitespace-gate.md)
- [015：重新打开单个任务](exec-plans/completed/015-reopen-task.md)
- [014：Unicode 安全的标题文件输入](exec-plans/completed/014-unicode-safe-title-file-input.md)
- [013：重命名单个任务](exec-plans/completed/013-rename-task.md)
- [001：实现添加学习任务](exec-plans/completed/001-add-task.md)
- [002：保护构建幂等性](exec-plans/completed/002-build-idempotency-guard.md)
- [003：查看学习任务](exec-plans/completed/003-list-tasks.md)
- [004：完成学习任务](exec-plans/completed/004-complete-task.md)
- [005：损坏数据失败安全](exec-plans/completed/005-corrupt-data-safety.md)
- [006：建立环境自举反馈闭环](exec-plans/completed/006-environment-bootstrap.md)
- [007：升级 GitHub Actions Node.js 24 运行时](exec-plans/completed/007-actions-node24-upgrade.md)
- [008：验证 main 的 PR 合并门禁](exec-plans/completed/008-main-branch-gate.md)
- [009：show 单任务查询](exec-plans/completed/009-show-task.md)
- [010：summary 任务统计](exec-plans/completed/010-task-summary.md)
- [011：架构导航地图降粒度](exec-plans/completed/011-architecture-navigation-map.md)
- [012：永久删除单个任务](exec-plans/completed/012-delete-task.md)

## 验证证据

- [001：main 合并门禁验证](evidence/001-main-branch-gate.md)
- [002：并行 worktree 实验](evidence/002-parallel-worktree-experiment.md)
- [003：永久删除任务实施证据](evidence/003-delete-task.md)
- [004：重命名任务实施证据](evidence/004-rename-task.md)
- [005：Unicode 安全的标题文件输入实施证据](evidence/005-unicode-safe-title-file-input.md)
- [006：重新打开单个任务实施证据](evidence/006-reopen-task.md)
- [007：CI PR 完整差异空白门禁本地 POC](evidence/007-ci-pr-diff-whitespace-poc.md)
- [008：`list --contains` 本地 generator 实施证据](evidence/008-list-title-search.md)

## 复盘反馈

- [007：PR evaluator 报告生命周期远程负路径演练](feedback/007-evaluator-report-lifecycle-drill.md)（已完成）
- [001：第一次 Harness 实战复盘](feedback/001-first-harness-retrospective.md)
- [002：并行 worktree 实战复盘](feedback/002-parallel-worktree-retrospective.md)
- [003：rename 三级流程复盘](feedback/003-rename-task-retrospective.md)
- [004：Unicode 边界与状态漂移复盘](feedback/004-unicode-boundary-harness-retrospective.md)
- [005：当前 Harness 效果基线](feedback/005-current-harness-effect-baseline.md)
- [006：`list --contains` 与精简导航后的 Harness 前瞻观察](feedback/006-list-title-search-observation.md)（已完成，证据不足）
