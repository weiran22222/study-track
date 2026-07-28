# 重新打开单个任务实施证据

状态：本地实施完成，待功能 PR 与最终 `main` 验证

日期：2026-07-28

## 验证目标

证明 `reopen <id>` 按决策卡 014、`SPEC.md` 2.8 节与 AC-17 只把指定已完成任务设为
未完成，同时保持幂等零写、失败安全、既有数据格式与分层约束。

## 冷启动实施与设计

实施从仓库 `AGENTS.md` 及其引用规则、`SPEC.md`、`ARCHITECTURE.md`、文档索引、决策卡
014 和执行计划 015 恢复目标与停止条件。实现后的依赖链为：

```text
ReopenCommand -> StudyTaskService -> TaskRepository <- JsonTaskRepository

StudyTrackApplication 组合 CLI 与 Infrastructure
```

Application 使用 `ReopenTaskResult.REOPENED` 与 `ALREADY_PENDING` 区分实际状态转换和
幂等成功。只有目标任务的 `completed` 为 `true` 时，才构造保留 `id` 与 `title` 的
`StudyTask` 并调用既有 `TaskRepository.update`；已未完成与不存在路径不调用写端口。

CLI 使用 Picocli 解析一个 `long` ID，映射批准的 stdout、stderr 与退出码。没有新增
Repository 协议、JSON 字段、迁移、依赖、架构方向、CI 或远程设置。

## 自动验证

JDK 固定为 `D:\work\jdk\jdk-21.0.11`。环境自检报告 Java 21 与 Maven Wrapper
3.9.12。定向命令为：

```powershell
.\mvnw.cmd `
  -Dtest=StudyTaskServiceTest,ReopenCommandTest,StudyTrackApplicationTest,JsonTaskRepositoryTest `
  test
```

最终结果为 76 项通过、0 失败、0 错误，Checkstyle 无告警。随后运行：

```powershell
.\mvnw.cmd package
.\mvnw.cmd verify
```

两次均为 109 项通过、0 失败、0 错误；Checkstyle 与 ArchUnit 通过，并生成
`target/study-track.jar`。本轮新增 12 个 JUnit 测试，并把 `reopen` 加入现有 4 类损坏
数据的跨命令矩阵。自动测试直接覆盖：

- Application 的已完成到未完成转换、幂等零写、不存在零写，以及 `id`、`title` 保留；
- CLI 成功、幂等、不存在、非整数 ID 在服务创建前失败，以及持久化异常的输出和退出码；
- 真实 JSON 中 `nextId`、其他任务内容和任务相对顺序保持；
- `list`、`show` 与 `summary` 在重新打开后的读取视图一致；
- 幂等、不存在、无数据文件和非整数参数的文件无副作用；
- 4 类损坏 JSON 均返回退出码 `1` 并保留原始字节；
- 更新路径在临时文件写入失败时保留原文件字节并清理本次临时文件。

## 主智能体独立复验

主智能体审查生产代码、测试和文档后，独立复跑相同的 4 个定向测试类，76 项通过；随后
在 JDK 21 下复跑完整 `verify`，109 项通过，Checkstyle 0 违规，ArchUnit 4 项通过。

构建后的真实 JAR 独立验证了成功重新打开、`show` 状态、幂等输出与文件哈希不变。首次
损坏 JSON 检查把 PowerShell 5 包装的原生命令 stderr 与产品文本合并，导致“输出必须以
产品错误开头”的验收脚本失败；改用 `System.Diagnostics.Process` 分离 stdout 与 stderr
后确认：退出码为 `1`、stdout 为空、stderr 以 `Data file error:` 开头，损坏文件
SHA-256 不变。两个临时目录均在验证绝对路径前缀后清理，最终不存在。

## 真实 JAR 验收

`mvnw package` 生成的真实 JAR 在系统临时目录
`C:\Users\weiran\AppData\Local\Temp\study-track-reopen-46e8299c46a04357a611f93880cb0cf1`
运行。先创建三个任务并完成任务 2、3，再执行 `reopen 2`，观察到：

```text
Reopened task 2.
[ ] 2 Reopen target
[ ] 1 Keep pending
[ ] 2 Reopen target
[x] 3 Keep completed
Total: 3
Pending: 2
Completed: 1
```

上述 `reopen`、`show`、`list` 与 `summary` 均退出 `0`。持久化 JSON 保持
`nextId = 4`，任务 ID、标题、顺序与非目标任务状态不变，只把任务 2 的
`completed` 改为 `false`。

幂等路径输出 `Task 2 is already pending.` 并退出 `0`；不存在任务输出
`Task 99 not found.` 并退出 `2`；非整数 ID 由 Picocli 报告并退出 `2`。三个场景使用
现有数据文件时，执行前后 SHA-256 均为：

```text
B0AAA87D5CE82BD827776990067FB3AC2FB045BE255112AA529D5CA3EAA6016A
```

无数据文件路径输出 `Task 99 not found.`、退出 `2`，且目标文件不存在。损坏 JSON
路径输出 `Data file error: Unable to read data file ...`、退出 `1`，执行前后 SHA-256
均为：

```text
A74ABF89A06DE4070E51D25FB83CC846323BA2444B68A47400C2B67A8F548241
```

验收目录只包含预期的 `tasks.json` 与手工构造的 `corrupt.json`，没有 Repository 临时
文件残留。确认该目录是系统临时目录的直接子目录后已递归清理，最终
`cleanup.exists=False`。

## 证据边界

自动测试通过记录型 Repository 直接证明幂等和不存在路径不调用 `update`，并通过注入
`TaskPersistenceException` 证明 CLI 的写入失败映射。`JsonTaskRepositoryTest` 使用定制
`JsonFactory` 在临时文件写入阶段抛出 `IOException`，直接证明更新为未完成状态的写入
失败会保留原始字节并清理临时文件。

现有生产代码没有围绕最终
`Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)` 的可注入 seam，本轮没有直接注入
“临时文件已完整写入但最终原子替换失败”，也不把它描述为已验证；继续沿用决策卡 008
的既有边界。并发进程竞态仍属于现有单文件 Repository 的边界，本轮没有增加锁或并发
协议。

本地 Maven 使用已有依赖缓存，不能证明干净机器无缓存冷启动。真实 JAR 文件系统结论
只直接适用于本次 Windows 系统临时目录。本证据不记录功能 PR、远程 CI、功能合并或
最终 `main` 验证；这些事实以 GitHub PR 与 Actions 为权威，不能由本地证据推断。
