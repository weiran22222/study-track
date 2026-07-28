# 重命名单个任务本地实施证据

状态：本地实施与子智能体验证完成；主智能体审查和远程门禁待验证

日期：2026-07-28

## 验证目标

证明 `rename <id> <new-title>` 按决策卡 010、`SPEC.md` 2.7 节和 AC-15 只修改指定
任务标题，并保持标题校验顺序、分层、任务状态、Repository 元数据和持久化失败安全。

## 无父对话实施

实施子智能体没有继承父对话，先从 `AGENTS.md`、`SPEC.md`、`ARCHITECTURE.md`、
文档索引、决策卡 010、决策卡 008 和执行计划 013 恢复目标与约束。实施保持依赖链：

```text
RenameCommand -> StudyTaskService -> TaskRepository <- JsonTaskRepository
```

CLI 没有直接访问文件系统或 Jackson，Application 没有依赖具体 Repository 实现。
`TaskRepository.update(StudyTask)` 和既有 JSON 原子写入路径得到复用，没有新增端口、
数据字段、迁移、依赖、CI 或远程设置。

## 实现决策

`StudyTaskService` 使用一个私有 `normalizeTitle` 供 `addTask` 和 `renameTask` 复用标题
规范化、Unicode 码点计数和错误消息。`renameTask` 在该方法成功返回后才读取 Repository。

Application 使用 `RenameTaskResult.RENAMED` 和 `ALREADY_NAMED` 区分成功修改与成功
幂等。只有找到任务且规范化标题不同时，才构造保留 `id`、`completed` 的新
`StudyTask` 并调用一次 `update`。

## 自动验证

| 验证层 | 本地结果 |
|---|---:|
| Application 与 Repository 定向测试 | 31 项通过 |
| CLI 与 Bootstrap 定向测试 | 35 项通过 |
| JDK 21 打包 | 78 项通过 |
| JDK 21 完整 `verify` | 78 项通过 |

本轮新增 21 个 JUnit 测试，并把 `rename` 加入既有损坏 JSON 跨命令矩阵。自动测试
直接覆盖：

- 未完成和已完成任务成功重命名，首尾空白被去除；
- 成功路径只调用一次写端口并保留 `id`、`completed`、`nextId`、其他任务和相对顺序；
- `list`、`show` 和 `summary` 在重命名后读取一致状态；
- 相同标题幂等成功，写端口零调用且文件字节不变；
- 空白与超过 200 个 Unicode 码点的标题在 Repository 读取前失败；
- 不存在、无数据文件、非整数 ID 和缺少标题的输出、退出码与无副作用；
- 损坏 JSON 返回退出码 `1` 且原字节不变；
- 定制 `JsonFactory` 注入临时文件写入失败时，原文件字节不变且本次临时文件被清理；
- Checkstyle、ArchUnit 和真实可执行 JAR 打包。

## 真实 JAR 验收

构建后的 `target/study-track.jar` 在独立临时目录
`C:\Users\weiran\AppData\Local\Temp\study-track-rename-9cd084e5363c4c5f8c3b2faad7226d65`
运行。成功路径观察到：

```text
Created task 1: Keep task
Created task 2: Old title
Completed task 2.
Renamed task 2.
[x] 2 Final verified title
[ ] 1 Keep task
[x] 2 Final verified title
Total: 2
Pending: 1
Completed: 1
```

上述命令进程退出码均为 `0`。最终 JSON 为 `nextId = 3`，任务 1 保持未完成和原标题，
任务 2 保持 `id = 2` 与 `completed = true`，最终复验标题为 `Final verified title`。

最终 `verify` 生成的 JAR 再次运行成功修改和全部下列无副作用场景。幂等路径输出
`Task 2 already has that title.` 并退出 `0`。不存在任务输出
`Task 99 not found.` 并退出 `2`；无效标题输出批准的标题错误并退出 `2`；无数据
文件路径按不存在处理、退出 `2` 且文件不存在。前三个使用已有任务文件的无副作用场景
前后 SHA-256 均为：

```text
970327A1A692A5A6B1375D7B22E9750594C03E8913CAD4276E9F5B7927876A86
```

损坏 JSON 路径输出 `Data file error: Unable to read data file ...` 并退出 `1`，前后
SHA-256 均为：

```text
F4208908F3729D8B9D93799EEF623DF34FC54A18ACB5227B49F0B2789D1D28F9
```

临时目录最终只包含预期的 `tasks.json` 和手工构造的 `corrupt.json`，没有残留本次
写入产生的临时文件。

## 证据边界

自动测试通过定制 `JsonFactory` 在 Jackson 创建临时文件写入器时确定性抛出
`IOException`，直接证明 `rename` 复用的更新路径在临时文件写入失败时保留原文件字节
并清理临时文件。

现有生产代码没有围绕
`Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)` 的可注入 seam。本轮没有直接注入
“临时文件已完整写入、最终原子替换失败”，也不把它表述为已验证；继续沿用决策卡 008
已接受的边界与重评条件。

本地 Maven 使用已有依赖缓存，因此环境自检、测试和 JAR 成功不构成干净机器无缓存冷
启动证明。真实 JAR 的文件系统结论只直接适用于本次 Windows 临时目录场景。

## 待验证的远程结果

- 功能分支尚未 push；
- 功能 PR 尚未创建；
- push 与 PR 的远程 `verify` 尚未运行；
- 功能尚未合并到受保护 `main`；
- 最终 `main` 远程 `verify` 尚未运行；
- 在以上结果完成前，AC-15 不能在受保护主线上标记为已实现，计划 013 也不能归档。
