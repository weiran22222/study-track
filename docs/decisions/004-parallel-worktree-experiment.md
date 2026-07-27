# 决策卡 004：并行 worktree 功能实验

状态：已验证

日期：2026-07-27

## 问题

当前证据证明单个子智能体可以从仓库恢复上下文、通过本地门禁并经受保护 PR 合并，但尚未
验证两个智能体从同一基线并行开发时：

- 工作区是否真正隔离；
- 规格和非目标是否保持一致；
- 独立 PR 是否都能通过门禁；
- 第一个 PR 合并后，第二个 PR 能否正确同步最新 `main`；
- 共享热点发生冲突时，能否在不丢失任一功能的情况下解决。

## 人类决策

2026-07-27，学习者批准并行实现两个只读功能：

1. `show <id>`：按 `list` 的单行格式查看指定任务；
2. `summary`：输出总数、未完成数和已完成数。

产品协议已经写入 `SPEC.md` 的 2.4、2.5 节以及 AC-12、AC-13。

## 选择原因

- 两个功能都不改变 JSON 数据格式，适合作为首次并行实验；
- 两个功能可以独立测试和验收；
- 它们会共享 `StudyTaskService`、根命令注册等真实热点，能够验证合并协调；
- 两个功能均为只读，可以精确验证“不创建或修改数据文件”的不变量；
- 先合并一个 PR 后，另一个 PR 必须同步严格保护的最新 `main` 并重新运行 `verify`。

## 并行模型

| 切片 | 分支 | worktree | 计划 |
|---|---|---|---|
| `show` | `codex/show-task` | `study-track-worktrees/show-task` | `009-show-task.md` |
| `summary` | `codex/task-summary` | `study-track-worktrees/task-summary` | `010-task-summary.md` |

两个分支必须从包含本决策、完整规格和两份计划的同一个 `main` 提交创建。两个实现子智能体
不继承父对话，只能从各自 worktree 中的 `AGENTS.md` 恢复任务。

## 合并策略

1. 两个子智能体并行实现并运行完整本地门禁；
2. 两个分支分别创建 PR；
3. 先合并 `show`；
4. `summary` 分支同步新的 `main`；
5. 如发生冲突，先解释共享不变量和双方意图，再最小化解决；
6. `summary` 重新运行完整门禁并通过 PR 合并；
7. 最终从 `main` 同时验收 AC-12 和 AC-13。

## 权限边界

- 产品输出、退出码和只读语义已经由人类批准，子智能体不得更改；
- 子智能体不得增加编辑、删除、标签、优先级或数据迁移；
- 子智能体不得修改 Branch Protection、Actions 版本或其他 Harness 权限；
- 冲突解决不得静默选择一方实现；主智能体必须保留两项规格；
- 若需要改变产品协议或架构，停止并请求新的明确决定。

## 验收

- 两个 worktree 从同一基线创建，路径和分支彼此隔离；
- 两个子智能体均能无父对话完成本地实现；
- 两个 PR 均独立触发并通过 `verify`；
- 第二个分支在第一个合并后同步最新 `main`，冲突处理有证据；
- 最终 `main` 同时满足 AC-12、AC-13 和既有 AC-01～AC-11；
- 并行耗时、共享热点、冲突和 Harness 缺口进入复盘。

## 验证结果

全部验收项已于 2026-07-27 完成。两个实现分支从共同基线 `1c06866` 创建，分别通过
[PR #4](https://github.com/weiran22222/study-track/pull/4) 和
[PR #5](https://github.com/weiran22222/study-track/pull/5) 合并。`summary` 在 `show`
合并后同步最新 `main`，实际解决 4 个共享热点冲突；最终 `main` 提交 `cb0b930` 的
远程 `verify` 成功。

完整过程证据见
[并行 worktree 实验证据](../evidence/002-parallel-worktree-experiment.md)，经验与缺口见
[第二次复盘](../feedback/002-parallel-worktree-retrospective.md)。
