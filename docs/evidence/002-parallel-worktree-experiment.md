# 并行 worktree 实验证据

状态：已完成

日期：2026-07-27

## 验证目标

证明两个无父对话子智能体可以从同一仓库基线在隔离 worktree 中并行实现独立切片，并在
共享热点发生真实冲突时，通过规格、计划、本地门禁、严格 PR 门禁和主智能体审查安全整合。

## 隔离与共同基线

两个分支均从包含决策卡、完整规格和两份计划的提交 `1c06866` 创建：

| 切片 | 分支 | worktree |
|---|---|---|
| `show` | `codex/show-task` | `D:/work/project/study-track-worktrees/show-task` |
| `summary` | `codex/task-summary` | `D:/work/project/study-track-worktrees/task-summary` |

两个子智能体不继承父对话，委派中未提供本机 JDK 路径；二者均从仓库入口和本机环境中
自行定位 JDK 21，未请求产品或架构补充信息，也没有修改对方功能。

## 独立实现与审查

| 切片 | 实现提交 | 子智能体完整门禁 | 主智能体独立复验 | PR |
|---|---|---:|---:|---|
| `show` | `1d5cec1` | 39 项测试 | 39 项测试 | [#4](https://github.com/weiran22222/study-track/pull/4) |
| `summary` | `91ebd9c` | 37 项测试 | 37 项测试 | [#5](https://github.com/weiran22222/study-track/pull/5) |

两次复验均在 JDK 21 下通过 Checkstyle、JUnit、ArchUnit 和可执行 JAR 打包。初始远程
检查也全部成功：

- `show`：
  [`push verify #20`](https://github.com/weiran22222/study-track/actions/runs/30272517265)、
  [`PR verify #21`](https://github.com/weiran22222/study-track/actions/runs/30272614895)；
- `summary`：
  [`push verify #19`](https://github.com/weiran22222/study-track/actions/runs/30272516832)、
  [`PR verify #22`](https://github.com/weiran22222/study-track/actions/runs/30272616453)。

## 顺序合并与冲突

PR #4 先以 `5827ad8` 为头提交合并，生成 `main` 提交 `6d42130`。随后 GitHub 比较结果
显示 PR #5 的旧头提交 `b02c3c0` 落后 `main` 1 个提交，因此没有直接合并旧结果。

`summary` 分支执行 `git merge main`，产生 4 个冲突：

1. `StudyTaskService`：保留 `showTask` 和 `summarizeTasks` 两个只读用例；
2. `StudyTrackCommand`：同时注册 `ShowCommand` 和 `SummaryCommand`；
3. `StudyTaskServiceTest`：保留双方 Application 行为及无写入断言；
4. `StudyTrackApplicationTest`：保留双方 CLI 场景，并让损坏 JSON 矩阵覆盖两个命令。

冲突解决提交 `b46647d` 的双亲为 `b02c3c0` 和 `6d42130`。子智能体和主智能体分别复跑
组合结果，均得到 43 项测试、0 失败、0 Checkstyle 违规，ArchUnit 和 JAR 打包成功。
同步后的远程检查重新执行并成功：

- [`PR verify #24`](https://github.com/weiran22222/study-track/actions/runs/30273085470)；
- [`push verify #25`](https://github.com/weiran22222/study-track/actions/runs/30273092084)。

这两次新检查针对 `b46647d`，没有复用同步前 `b02c3c0` 的绿色结果。

## 最终主线

PR #5 合并后生成最终 `main` 提交 `cb0b930`。
[`main verify #26`](https://github.com/weiran22222/study-track/actions/runs/30273220860)
成功，完整门禁包含 43 项测试。最终主线同时满足 AC-01～AC-13。

## 证据边界

- 两个实现提交时间相差 68 秒，能够证明执行发生重叠，但没有记录两个任务的精确开始时间，
  因此不能据此计算并行加速比；
- 本实验验证了同一小型 Java 项目中的两个只读切片，不代表大规模多智能体协作已经解决；
- 冲突解决的正确性由明确规格、双方测试并集、两轮本地复验和新的远程检查共同支持，不把
  “Git 能完成 merge”本身视为行为正确的证据。
