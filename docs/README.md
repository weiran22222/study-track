# StudyTrack 文档索引

这里是历史决策、执行过程、验证证据和复盘反馈的统一入口。按当前任务只读取相关分类和
工件，避免把全部历史载入默认上下文。

## 当前事实与历史理由

- [产品规格](../SPEC.md)描述当前有效的产品行为和验收标准；
- [架构说明](../ARCHITECTURE.md)描述当前有效的技术选择、分层和依赖约束；
- [智能体导航](../AGENTS.md)描述当前有效的工作约定和机械门禁入口；
- 决策记录解释现状形成的历史理由，可能被后续决策取代，不能凌驾于当前规格或架构之上。

## 重要说明

- [构建环境](environment.md)：JDK 21、Maven Wrapper 和跨平台环境自检。

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

## 执行计划

进行中的计划直接保存在 `exec-plans/`，已完成计划保存在 `exec-plans/completed/`：

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

## 复盘反馈

- [001：第一次 Harness 实战复盘](feedback/001-first-harness-retrospective.md)
- [002：并行 worktree 实战复盘](feedback/002-parallel-worktree-retrospective.md)
- [003：rename 三级流程复盘](feedback/003-rename-task-retrospective.md)
