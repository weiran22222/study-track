# 执行计划 003：查看学习任务

状态：已完成

## 学习目标

使用一个不继承当前聊天上下文的新子智能体实现第二个功能切片，验证仓库中的
`AGENTS.md`、规格、架构和历史计划是否足以支持正确交付。

## 产品目标

实现 `SPEC.md` 的“2.2 查看任务”，覆盖：

- AC-02：新进程能够读取之前保存的任务；
- AC-03：按 ID 排序，支持全部、未完成和已完成筛选；
- AC-08：CLI 不直接访问文件系统或 JSON；
- AC-09：Application 不依赖具体持久化实现；
- AC-10：全部机械检查通过；
- AC-11：可执行 JAR 能运行 `list`。

## 权威来源

- 产品行为：[../../../SPEC.md](../../../SPEC.md#22-查看任务)
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

只有实现确实需要且不改变既有 Harness 含义时才可修改 `pom.xml`。如果现有规格、
架构或门禁阻碍实现，子智能体必须报告冲突，不得自行放宽。

## 非目标

- 不实现 `complete`；
- 不修改 `add` 的产品行为；
- 不处理并发进程写入；
- 不把 AC-07 的损坏 JSON 完整行为扩展为本切片目标；
- 不改变技术栈、架构分层或强制门禁；
- 不增加规格外的分页、搜索、标签或排序选项。

## 可执行验收

给定包含以下任务的数据文件：

```text
[ ] 1 阅读 Harness
[x] 2 完成 add 实验
[ ] 3 编写 list 验收测试
```

执行：

```powershell
java -jar target/study-track.jar --data-file .\tmp\tasks.json list
```

必须按 ID 输出全部三项。执行：

```powershell
java -jar target/study-track.jar --data-file .\tmp\tasks.json list --status pending
```

只能输出 ID `1`、`3`。`--status completed` 只能输出 ID `2`。

空仓库或筛选后无匹配项时输出：

```text
No tasks.
```

未知状态必须由 Picocli 返回退出码 `2`。所有展示内容写入标准输出，成功时标准错误
为空。

完整门禁：

```powershell
.\mvnw.cmd verify
```

本机 Maven Central TLS 不可用时，只可在本地命令中传入用户已有的 Maven
`settings.xml`，不得提交机器专属镜像。

## 实现原则

- 先建立可观察的失败测试，再实现最小代码；
- Repository 负责读取持久化任务，但不负责展示；
- Application 负责用例行为，CLI 负责筛选参数映射与文本展示；
- 排序和筛选的职责选择必须在决策日志说明；
- 测试使用 `@TempDir` 或内存替身，不接触用户真实数据；
- 现有 `add`、构建幂等性及架构门禁必须保持通过。

## 进度

- [x] 人类授权继续主线
- [x] 主智能体定义范围与验收
- [x] 新子智能体仅凭仓库上下文完成阅读
- [x] 失败测试已建立并观察
- [x] 最小实现完成
- [x] 完整 `verify` 通过
- [x] 主智能体审查完成

## 决策日志

- 2026-07-27：刻意不给实现子智能体继承聊天上下文，用第二个切片检验“仓库即
  记录系统”是否真的成立，而不只是在同一会话中看起来成立。
- 2026-07-27：实现子智能体仅凭仓库入口文档、规格、架构、活动计划和现有代码
  恢复了本切片的范围与验收；没有发现必须依赖父对话才能补齐的产品或架构信息。
- 2026-07-27：先添加 Application、Repository 和 CLI 测试，并在 JDK 21 门禁通过后
  观察到测试编译因 `StudyTaskService.listTasks()` 与 `JsonTaskRepository.findAll()`
  缺失而失败，再开始产品实现。
- 2026-07-27：排序由 Application 的 list 用例负责，使所有调用适配器获得一致的
  ID 升序结果；`pending`、`completed` 字符串到领域完成状态的筛选由 CLI 负责，
  避免命令行协议渗入 Application。Repository 只返回持久化任务，不承担展示职责。
- 2026-07-27：使用 JDK 21 运行 `.\mvnw.cmd verify`，20 个测试、Checkstyle、
  ArchUnit、Enforcer 与可执行 JAR 打包全部通过；随后直接运行 JAR 验证全部任务、
  两种状态筛选和非法状态退出码 `2`。未使用机器专属 Maven 配置。
- 2026-07-27：直接 JAR 验收发现 Picocli 的默认枚举转换还会接受大写
  `PENDING`；补充失败测试后改为 CLI 层严格转换，仅接受规格定义的小写值。最终
  再次运行完整 `verify` 并通过，且最终 JAR 对大写值返回退出码 `2`。
- 2026-07-27：主智能体独立运行完整门禁，20 个测试通过；随后使用乱序、混合状态
  JSON 直接验证全部、`pending`、`completed` 输出及大写状态退出码，审查通过。
- 2026-07-27：新子智能体未请求任何父对话中的产品或架构信息，说明当前仓库入口
  足以支持本切片的无会话交接。
