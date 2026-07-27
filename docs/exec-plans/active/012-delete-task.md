# 执行计划 012：永久删除单个任务

状态：实现完成，待主智能体审查

## 目标

实现决策卡 007、`SPEC.md` 2.6 节和 AC-14，使用户可以永久删除一个任务，同时保持
分层、单调 ID 和持久化失败安全。

## 范围

- 为 `TaskRepository` 增加删除端口能力；
- 在 `StudyTaskService` 中实现按 ID 删除用例；
- 在 JSON Repository 中使用现有原子写入路径删除任务；
- 新增并注册 `DeleteCommand`；
- 添加 Application、Repository 和 CLI 自动测试；
- 把 `delete` 纳入损坏 JSON 的跨命令失败安全矩阵；
- 更新本计划的实现决策、验证结果和进度。

## 非目标

- 不实现编辑、软删除、恢复、回收站或批量删除；
- 不增加交互确认或 `--force`；
- 不修改 JSON 结构或执行迁移；
- 不复用已删除 ID；
- 不改变现有命令协议；
- 不修改 ArchUnit 规则、CI、依赖、分支保护或远程设置。

## 行为验收

1. 删除未完成或已完成任务均输出 `Deleted task <id>.` 并退出 `0`；
2. 只移除指定任务，其他任务内容和相对顺序不变；
3. 成功删除前后 `nextId` 完全不变；
4. 删除最后一个任务后保存合法空数组，`nextId` 不变；
5. 不存在任务输出 `Task <id> not found.`、退出 `2`，文件字节不变；
6. 数据文件不存在时返回不存在，不创建文件；
7. 非整数 ID 返回 Picocli 参数错误和退出码 `2`；
8. 未定义的 `--force` 选项返回 Picocli 参数错误和退出码 `2`，数据不变；
9. JSON 损坏返回退出码 `1`，原始字节不变；
10. 持久化写入或替换失败时返回退出码 `1`，原文件完整保留；
11. 删除后 `list`、`show` 和 `summary` 读取到一致的新状态；
12. 删除后再次添加任务使用原 `nextId`，不复用被删除 ID；
13. JDK 21 下完整 `verify` 通过。

## 实施约束

- Application 必须先通过 Repository 只读端口确认目标存在；不存在时不得调用写端口；
- Repository 删除操作接收明确的任务 ID，并以原 `nextId` 写回剩余任务；
- CLI 复用现有 `TaskNotFoundException` 和 `TaskPersistenceException` 映射；
- 成功路径只能写入一次持久化结果；
- 测试不得操作用户真实数据文件；
- 不为减少代码重复而进行规格外重构。

## 验证顺序

1. 运行 Application 与 Repository 相关测试；
2. 运行 CLI 集成测试；
3. 运行损坏数据和写入失败测试；
4. 使用构建后的真实 JAR进行最小成功与不存在场景验收；
5. 在 JDK 21 下运行完整 `.\mvnw.cmd verify`；
6. 由主智能体独立审查 diff 并复跑完整门禁；
7. 通过受保护 PR 合并，在最终 `main` 回查远程 `verify`。

## 实现决策

- `TaskRepository` 增加接收明确 ID 的 `delete(long taskId)` 写端口；Application 先通过
  `findAll()` 确认任务存在，不存在时抛出现有 `TaskNotFoundException` 且不调用删除端口。
- JSON Repository 在删除端口内重新读取当前集合，只移除匹配 ID，并把原 `nextId` 和剩余
  任务按原相对顺序交给现有原子 `write` 路径；成功路径只调用一次 `write`。若两次读取之间
  目标消失，Repository 把该竞态视为持久化失败，不写回无变化的数据。
- `DeleteCommand` 只解析一个 `long` ID、调用 Application，并复用现有不存在与持久化异常
  到退出码 `2`、`1` 的映射；命令未声明任何删除选项，因此 `--force` 由 Picocli 拒绝。
- 写入失败测试使用测试专用 `JsonFactory`，在 Jackson 为临时文件创建 `JsonGenerator` 时
  确定性抛出 `IOException`。该方式跨平台直接验证写入失败会保留原文件全部字节并清理
  临时文件，不为测试增加生产抽象。

## 实际验证

- 环境自检：默认 Java 17 按预期被拒绝；仅在命令进程中选择本机已安装的 JDK 21 后，
  自检通过并报告 Java 21、Maven Wrapper 3.9.12。
- Application 与 Repository：
  `.\mvnw.cmd -Dtest=StudyTaskServiceTest,JsonTaskRepositoryTest test`，22 个测试通过。
- CLI：
  `.\mvnw.cmd -Dtest=StudyTrackApplicationTest,DeleteCommandTest test`，24 个测试通过。
- 损坏数据与写入失败定向复验：损坏 JSON 跨命令矩阵、Repository 写入失败和 CLI
  持久化异常映射共 3 个测试通过；矩阵已包含 `delete`。
- `.\mvnw.cmd package`：54 个测试通过，并生成 `target/study-track.jar`。
- 真实 JAR：先添加 ID 1，再执行 `delete 1`，输出 `Deleted task 1.`、退出 `0`；
  数据变为 `nextId: 2` 和合法空数组。随后执行 `delete 99`，输出
  `Task 99 not found.`、退出 `2`；前后文件 SHA-256 均为
  `54909EC6DCB8E9D152D38209B0B23F64E5FB2703110BA08AABB391875A9D9F16`。
- JDK 21 完整 `.\mvnw.cmd verify`：54 个测试通过，Checkstyle 0 违规，
  ArchUnit 4 个测试通过，可执行 JAR 成功打包。

## Harness 知识缺口

现有生产代码没有围绕 `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)` 的可注入边界，
因此在不增加规格外生产抽象、不依赖平台权限或文件锁偶然性的前提下，无法跨平台确定性
直接注入“临时文件已完整写入、最终原子替换失败”。本实现直接故障注入并验证的是替换前的
写入失败；原子替换仍复用既有 `write` 路径和架构约束，不把它描述为已直接故障注入验证。

## 进度

- [ ] 规划 PR 合并
- [x] 无父对话子智能体开始
- [x] Application 与 Repository 行为完成
- [x] CLI 行为完成
- [x] 失败安全与真实 JAR 验收完成
- [x] 子智能体完整 `verify` 通过
- [ ] 主智能体审查与独立复验
- [ ] 功能 PR 通过并合并
- [ ] 最终 `main` 远程验证与证据归档
