# 执行计划 004：完成学习任务

状态：已完成

## 学习目标

使用第三个功能切片验证 Harness 对状态变化、错误路径和幂等操作的约束能力。
实现继续交给一个不继承父对话的新子智能体，以检验仓库交接能力是否能够持续复用。

## 产品目标

实现 `SPEC.md` 的“2.3 完成任务”，覆盖：

- AC-04：完成状态被持久化，重复执行具有幂等行为；
- AC-06：任务不存在时返回退出码 `2`，且不修改数据；
- AC-08：CLI 不直接访问文件系统或 JSON；
- AC-09：Application 不依赖具体持久化实现；
- AC-10：全部机械检查通过；
- AC-11：可执行 JAR 能运行 `complete`。

## 权威来源

- 产品行为：[../../../SPEC.md](../../../SPEC.md#23-完成任务)
- 数据格式：[../../../SPEC.md](../../../SPEC.md#3-数据格式)
- 分层规则：[../../../ARCHITECTURE.md](../../../ARCHITECTURE.md#4-分层与依赖)
- 工作约定：[../../../AGENTS.md](../../../AGENTS.md)

## 允许修改的范围

- `src/main/java/com/example/studytrack/domain/`；
- `src/main/java/com/example/studytrack/application/`；
- `src/main/java/com/example/studytrack/infrastructure/`；
- `src/main/java/com/example/studytrack/cli/`；
- `src/main/java/com/example/studytrack/bootstrap/`；
- 对应的 `src/test/`；
- 本计划的进度与决策日志。

如果现有规格、架构或门禁阻碍实现，子智能体必须报告冲突，不得自行改变产品
含义或放宽约束。

## 非目标

- 不实现编辑、删除或恢复任务；
- 不改变 `add`、`list` 的产品行为；
- 不处理并发进程同时修改 JSON；
- 不把 AC-07 的损坏 JSON 完整行为扩展为本切片目标；
- 不改变技术栈、架构分层或强制门禁；
- 不增加完成时间、历史记录或其他规格外字段。

## 可执行验收

先添加任务：

```powershell
java -jar target/study-track.jar --data-file .\tmp\tasks.json add "学习幂等性"
```

首次完成：

```powershell
java -jar target/study-track.jar --data-file .\tmp\tasks.json complete 1
```

必须输出 `Completed task 1.`，返回退出码 `0`，并持久化
`"completed": true`。新进程执行 `list --status completed` 必须能看到该任务。

再次执行同一命令时：

- 输出 `Task 1 is already completed.`；
- 返回退出码 `0`；
- 数据文件内容不得发生变化。

完成不存在的任务：

```powershell
java -jar target/study-track.jar --data-file .\tmp\tasks.json complete 99
```

必须向标准错误输出 `Task 99 not found.`，返回退出码 `2`，且数据文件内容不得变化。
当数据文件原本不存在时，失败也不得创建空文件。

非整数 ID 由 Picocli 作为参数错误处理并返回退出码 `2`。

完整门禁：

```powershell
.\mvnw.cmd verify
```

本机 Maven Central TLS 不可用时，只可在本地命令中传入用户已有的 Maven
`settings.xml`，不得提交机器专属镜像。

## 实现原则

- 先建立可观察的失败测试，再实现最小代码；
- Application 负责“找到任务、判断状态、决定状态转换”的用例语义；
- Repository 负责读取和安全持久化，但不决定用户输出；
- CLI 负责参数协议、输出和退出码映射；
- 已完成与不存在两条不写入路径必须由测试验证数据未变化；
- 测试使用 `@TempDir` 或内存替身，不接触用户真实数据；
- 现有 `add`、`list`、构建幂等性和架构门禁必须保持通过。

## 进度

- [x] 人类授权继续主线
- [x] 主智能体定义范围与验收
- [x] 新子智能体仅凭仓库上下文完成阅读
- [x] 失败测试已建立并观察
- [x] 最小实现完成
- [x] 完整 `verify` 通过
- [x] 主智能体审查完成

## 决策日志

- 2026-07-27：把“重复完成不修改文件”与“不存在任务不修改文件”作为显式验收，
  因为退出码正确并不能证明状态变化是幂等且无副作用。
- 2026-07-27：实现子智能体仅凭根 `AGENTS.md`、`SPEC.md`、`ARCHITECTURE.md`、
  本计划与现有 add/list 实现恢复了完整约束；未发现需要父对话补充的产品或架构
  知识。
- 2026-07-27：先添加 Application、Repository 与 CLI 集成测试，并使用 JDK 21
  观察到测试编译因完成用例、结果类型、未找到异常和 Repository 更新端口缺失而
  失败；确认红灯后才添加产品实现。
- 2026-07-27：Application 使用 `CompleteTaskResult` 区分首次完成与已完成，并用
  `TaskNotFoundException` 表达不存在；Application 在调用 Repository 更新端口前
  决定三条用例分支，使已完成与不存在分支不会触发写入。Repository 仅在首次完成
  时原子替换任务，保留 `nextId` 与任务顺序；CLI 只映射输出和退出码。
- 2026-07-27：使用 JDK 21 运行 `.\mvnw.cmd verify`，29 个测试、Checkstyle、
  ArchUnit、Enforcer 与可执行 JAR 打包全部通过，未使用机器专属 Maven 配置。
  随后直接运行 JAR 验证首次完成、重复完成、已有文件中不存在任务、不存在数据
  文件、完成状态跨进程读取；两条已有文件无写入路径的 SHA-256 保持不变，不存在
  的数据文件未被创建。
- 2026-07-27：主智能体独立运行完整门禁并通过；随后用真实 JAR 再次验证首次
  完成、跨进程读取、重复完成与不存在任务。重复完成和不存在任务前后文件哈希
  一致，缺失数据文件未被创建，审查通过。
- 2026-07-27：新子智能体未请求父对话中的产品或架构信息，第三次无会话交接成功。
