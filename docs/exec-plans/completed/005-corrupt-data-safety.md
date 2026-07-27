# 执行计划 005：损坏数据失败安全

状态：已完成

## 学习目标

从成功路径转入可靠性工程，验证 Harness 能否把“失败时不能破坏用户数据”转化为
可执行、可回归的约束。

实现交给一个不继承父对话的新子智能体，继续检验仓库作为完整交接载体的能力。

## 产品目标

完成 AC-07：

- JSON 损坏时返回退出码 `1`；
- 向标准错误报告数据文件错误；
- 不向普通用户输出 Java 堆栈；
- 保留原文件的精确字节内容。

这项行为必须对所有会读取数据文件的命令成立：

- `add`；
- `list`；
- `complete`。

## 权威来源

- 失败行为：[../../../SPEC.md](../../../SPEC.md#3-数据格式)
- 验收标准：[../../../SPEC.md](../../../SPEC.md#5-验收标准)
- 异常边界：[../../../ARCHITECTURE.md](../../../ARCHITECTURE.md#6-持久化策略)
- 工作约定：[../../../AGENTS.md](../../../AGENTS.md)

## 损坏数据测试矩阵

至少覆盖：

1. 语法不完整的 JSON；
2. 根值为 `null`；
3. 缺失必需字段或 `tasks` 为 `null`。

这些情况都无法构成 `SPEC.md` 定义的任务库。未知扩展字段、历史数据迁移、重复 ID
和并发修改不在本切片范围。

## 允许修改的范围

- `src/main/java/com/example/studytrack/infrastructure/`；
- 为保持异常边界一致而确有必要的 Application 或 CLI 文件；
- 对应的 `src/test/`；
- 本计划的进度和决策日志。

如果需要改变错误文本的公共含义、架构分层或现有门禁，必须报告，不能自行决定。

## 非目标

- 不自动修复、备份或覆盖损坏文件；
- 不新增恢复、导入或迁移命令；
- 不处理文件权限、磁盘耗尽或跨进程并发；
- 不改变 `add`、`list`、`complete` 的成功行为；
- 不引入新的技术栈或依赖；
- 不把错误堆栈展示给普通 CLI 用户。

## 可执行验收

给定损坏文件 `tmp/tasks.json`，分别执行：

```powershell
java -jar target/study-track.jar --data-file .\tmp\tasks.json add "新任务"
java -jar target/study-track.jar --data-file .\tmp\tasks.json list
java -jar target/study-track.jar --data-file .\tmp\tasks.json complete 1
```

每条命令都必须：

- 返回退出码 `1`；
- 标准输出为空；
- 标准错误以 `Data file error:` 开头；
- 标准错误不包含 Java 堆栈；
- 执行前后的文件字节完全一致。

Repository 层必须把无法读取为有效任务库的输入统一转换为
`TaskPersistenceException`，不能让 `NullPointerException`、Jackson 异常或文件系统
细节越过 Application/CLI 边界。

完整门禁：

```powershell
.\mvnw.cmd verify
```

本机 Maven Central TLS 不可用时，只可在本地命令中传入用户已有的 Maven
`settings.xml`，不得提交机器专属镜像。

## 实现原则

- 先为损坏输入建立失败测试并观察当前错误；
- 在最靠近数据解释的 Infrastructure 层统一异常；
- 读取校验失败时绝不进入写入路径；
- 用字节数组或哈希验证原文件未变化，不能只检查时间戳；
- 测试使用 `@TempDir`，不接触用户数据；
- 现有 29 个测试和所有门禁必须保持通过。

## 进度

- [x] 人类授权继续主线
- [x] 主智能体定义失败契约与测试矩阵
- [x] 新子智能体仅凭仓库上下文完成阅读
- [x] 失败测试已建立并观察
- [x] 最小修复完成
- [x] 完整 `verify` 通过
- [x] 主智能体审查完成

## 决策日志

- 2026-07-27：把语法错误、`null` 根值和缺失必需字段纳入“损坏 JSON”；它们虽然
  处于不同解析阶段，但都不能形成规格定义的任务库，也必须遵循同一失败契约。
- 2026-07-27：本切片要求比较文件字节而不是只观察退出码，因为失败安全的关键是
  用户原始数据没有被覆盖。
- 2026-07-27：失败测试首先观察到 `null` 根值越过 Repository 的异常边界；
  Picocli 返回退出码 `1`，但标准错误不是 `Data file error:`。语法错误、缺失
  `nextId` 和 `tasks: null` 已由现有读取路径转换为 `TaskPersistenceException`。
- 2026-07-27：实现子智能体仅在 `JsonTaskRepository.read()` 增加空根校验，
  使异常进入既有持久化边界；跨三个命令、四类损坏数据的矩阵全部通过。
- 2026-07-27：主智能体独立运行完整门禁，30 个测试通过；随后使用 `null` 根文件
  运行真实 `add`、`list`、`complete`，三个退出码均为 `1`，文件 SHA-256 始终
  不变，审查通过。
- 2026-07-27：子智能体未发现父对话知识缺口；逐任务字段、重复 ID 和数据迁移等
  未定义约束保持在本计划范围之外，没有被实现自行扩张。
- 2026-07-27：最小修复放在 Infrastructure 的 JSON 根值校验处；不改变
  Application/CLI、成功路径、架构分层或构建门禁。`add`、`list`、`complete`
  共享的回归矩阵按原始字节验证失败安全。
- 2026-07-27：使用 JDK 21 运行 `.\mvnw.cmd verify`，30 个测试全部通过；
  本机缓存可用，未使用机器专属 Maven 镜像配置。
