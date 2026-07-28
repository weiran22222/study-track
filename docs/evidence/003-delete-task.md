# 永久删除任务实施证据

状态：已完成

日期：2026-07-27

## 验证目标

证明 `delete <id>` 按人类批准的 AC-14 永久删除一个任务，同时保持分层、单调 ID、
持久化失败安全和既有命令行为。

## 规格先行

学习者明确批准永久删除、ID 永不复用以及第一版不支持交互确认和 `--force`。决策卡 007、
`SPEC.md` 2.6 节、AC-14 和执行计划 012 通过
[规划 PR #10](https://github.com/weiran22222/study-track/pull/10) 合并为 `main`
提交 `8ab2a64`。合并后的
[`verify #41`](https://github.com/weiran22222/study-track/actions/runs/30280348799)
成功，随后才从该基线创建实现 worktree。

## 无父对话实施

实现子智能体没有继承父对话，只从 `AGENTS.md`、决策卡 007、`SPEC.md`、
`ARCHITECTURE.md` 和计划 012 恢复任务。实现提交为 `c9ced60`，主智能体审查记录提交为
`a9d9d4a`。

实现保持以下依赖链：

```text
DeleteCommand -> StudyTaskService -> TaskRepository <- JsonTaskRepository
```

CLI 没有直接访问文件系统或 Jackson，Application 没有依赖具体 Repository 实现。

## 自动验证

| 验证层 | 结果 |
|---|---:|
| Application 与 Repository 定向测试 | 22 项通过 |
| CLI 集成与映射测试 | 24 项通过 |
| 损坏 JSON、写入失败和异常映射定向复验 | 3 项通过 |
| 子智能体完整 `verify` | 54 项通过 |
| 主智能体独立完整 `verify` | 54 项通过 |

自动测试覆盖：

- 删除未完成和已完成任务；
- 只删除目标并保留其他任务的内容与顺序；
- 保持原 `nextId`，删除后新增任务不复用 ID；
- 删除最后一项后保存合法空数组；
- 不存在任务和无数据文件路径无副作用；
- 非整数 ID 和未定义 `--force` 返回退出码 `2`；
- 损坏 JSON 返回退出码 `1` 且原字节不变；
- 写入失败时原字节不变且临时文件被清理；
- `list`、`show` 和 `summary` 读取到一致的新状态；
- Checkstyle、ArchUnit 和可执行 JAR 打包。

## 真实 JAR 验收

子智能体和主智能体分别使用构建后的 `target/study-track.jar` 运行真实命令。主智能体独立
观察到：

```text
Created task 1: 主智能体验收任务
Deleted task 1.
Task 99 not found.
```

对应退出码依次为 `0`、`0`、`2`。不存在任务调用前后数据文件 SHA-256 相同；删除后的
JSON 解析结果为 `nextId = 2`、任务数 `0`。

## 远程门禁

功能 [PR #11](https://github.com/weiran22222/study-track/pull/11) 的最终头提交
`a9d9d4a` 通过：

- [`push verify #42`](https://github.com/weiran22222/study-track/actions/runs/30281483251)；
- [`PR verify #43`](https://github.com/weiran22222/study-track/actions/runs/30281514769)。

PR 正常合并后生成 `main` 提交 `3cb8138`，
[`main verify #44`](https://github.com/weiran22222/study-track/actions/runs/30281636291)
成功。最终主线完整门禁包含 54 项测试。

## 证据边界

测试通过定制 `JsonFactory` 在 Jackson 创建临时文件写入器时确定性抛出 `IOException`，
直接证明临时文件写入失败时原文件字节不变且临时文件被清理。

现有生产代码没有围绕 `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)` 的可注入 seam。
因此，本轮没有跨平台确定性地直接注入“临时文件已完整写入、最终替换失败”，也不把它
表述为已直接验证。删除复用了既有原子 `write` 路径；是否为了这一故障点增加生产抽象，
需要单独比较可测性收益和架构成本，不能在功能实现中顺手扩大范围。

学习者于 2026-07-28 明确接受当前直接测试缺口，不增加生产 seam 或平台相关测试。该接受
不把结构推理升级为直接证据，也不是永久豁免：若真实发生原子替换失败或数据丢失、持久化
代码被更多产品复用、数据损失后果或合规与恢复成本提高、项目引入统一文件系统抽象，或者
更多关键文件系统故障需要同类注入边界，必须重新评估。完整不变边界与重新评估条件见
[决策卡 008](../decisions/008-accept-atomic-replacement-test-gap.md)。
