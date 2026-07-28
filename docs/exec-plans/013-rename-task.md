# 执行计划 013：重命名单个任务

状态：进行中（本地实施与子智能体验证已完成，主智能体审查及远程门禁待验证）

## 目标

实现决策卡 010、`SPEC.md` 2.7 节和 AC-15，使用户可以只修改一个已有任务的标题，同时
保持分层、现有任务状态、Repository 元数据和持久化失败安全。

本计划的代码与测试实施明确交给规划合并后的后续子智能体。本阶段只固化产品协议、验收
边界和实施顺序，不新增生产代码或测试实现，也不声称 `rename` 已经可用。

## 范围

- 在 Application 中复用 `add` 的标题规范化和校验规则，实现重命名用例及幂等结果；
- 复用现有 `TaskRepository.update(StudyTask)` 写端口和 JSON 原子写入路径；
- 新增并注册 `RenameCommand`；
- 添加 Application、Repository、CLI 和 Bootstrap 集成测试；
- 把 `rename` 纳入损坏 JSON 的跨命令失败安全矩阵；
- 使用真实 JAR 验证成功、幂等和失败无副作用；
- 创建 `docs/evidence/004-rename-task.md` 保存实际验证结果；
- 实施过程中持续更新本计划的实现决策、验证结果和进度，完成后移入
  `docs/exec-plans/completed/`。

## 非目标

- 不修改任务 ID 或通过 `rename` 修改完成状态；
- 不实现通用字段编辑、批量重命名或交互式编辑器；
- 不保存标题历史，也不实现撤销；
- 不修改 JSON 结构或执行数据迁移；
- 不改变现有命令协议；
- 不修改 ArchUnit 规则、CI、依赖、分支保护或远程设置；
- 不为减少局部重复而进行本规格之外的重构。

## 行为验收

1. 命令形式为 `rename <id> <new-title>`；
2. Picocli 先解析 ID，Application 使用 `String.strip()` 规范化标题，再按 `1..200` 个
   Unicode 码点校验，标题有效后才读取 Repository；
3. 标题无效时 stderr 输出
   `Task title must contain between 1 and 200 characters.`、退出 `2`，Repository
   读写调用均为零；
4. 未完成和已完成任务重命名均输出 `Renamed task <id>.` 并退出 `0`；
5. 成功路径只改变目标任务 `title`，其 `id` 和 `completed` 保持不变；
6. 成功路径保持 `nextId`、其他任务内容和相对顺序不变；
7. 归一化后的新标题等于原标题时输出
   `Task <id> already has that title.`、退出 `0`，不得调用写端口，数据文件字节不变；
8. 不存在任务输出 `Task <id> not found.`、退出 `2`，不得调用写端口，数据文件字节
   不变；
9. 数据文件不存在时按任务不存在处理，不创建文件；
10. 非整数 ID 由 Picocli 返回参数错误和退出码 `2`，不得访问数据；
11. 标题无效且任务不存在时先返回标题错误，不读取数据；
12. JSON 损坏时返回退出码 `1`，原始文件字节不变；
13. 持久化写入失败时返回退出码 `1`，原始文件完整保留并清理本次临时文件；
14. 重命名后 `list`、`show` 和 `summary` 读取到一致状态；
15. JDK 21 下完整 `verify` 通过。

## 预计代码位置

- `application/StudyTaskService.java`：标题规范化、校验、查找、幂等判断和更新编排；
- `application/RenameTaskResult.java`：如实现选择显式结果类型，用于区分已修改与标题
  未变化；具体命名由实施子智能体在不改变协议的前提下决定；
- `application/port/TaskRepository.java`：预计复用现有 `update(StudyTask)`，不新增重命名
  专用端口；
- `infrastructure/persistence/JsonTaskRepository.java`：预计无需改变生产实现；以测试证明
  现有更新路径保留元数据、顺序并遵守失败安全；
- `cli/RenameCommand.java`：解析 ID 与新标题，调用 Application，映射 stdout、stderr
  和退出码；
- `cli/StudyTrackCommand.java`：注册 `rename` 子命令；
- 对应的 Application、Repository、CLI 和 Bootstrap 测试文件。

如果实施发现必须改变核心架构边界、数据格式、依赖、CI 或远程权限，子智能体必须停止并
报告范围扩大，不得自行改变本计划或降低风险级别。

## 实际实现决策

- 在 `StudyTaskService` 中提取私有 `normalizeTitle`，由 `addTask` 和 `renameTask` 共同复用
  `String.strip()`、Unicode 码点计数和同一异常；这避免两份校验行为漂移，没有扩大为
  产品范围之外的重构；
- 使用 `RenameTaskResult` 枚举显式区分 `RENAMED` 与 `ALREADY_NAMED`，让 CLI 只负责
  输出和退出码映射；
- `renameTask` 在规范化和校验成功后才调用一次 `findAll`；任务存在且标题变化时构造只
  改 `title` 的新 `StudyTask` 并调用一次现有 `update`，幂等与不存在路径不调用写端口；
- `RenameCommand` 沿用现有命令的异常映射，注册到 `StudyTrackCommand`；Picocli 在调用
  Application 前拒绝非整数 ID 和缺少标题；
- `TaskRepository`、`JsonTaskRepository` 生产实现、JSON 格式、依赖、ArchUnit、CI 和
  远程设置均未改变。Repository 测试直接证明现有 `update` 路径保留元数据与相对顺序。

## 实施阶段

### 阶段 1：Application 行为

1. 提取或复用 `addTask` 的标题规范化与 Unicode 码点校验，确保错误信息完全一致；
2. 在任何 Repository 读取前完成标题校验；
3. 读取并定位任务，不存在时复用 `TaskNotFoundException`；
4. 比较归一化标题与持久化原标题，相同则返回幂等结果且不调用写端口；
5. 构造只改变标题的新 `StudyTask` 并调用一次现有 `update` 端口；
6. 用记录型 Repository 测试调用顺序、零读写、零写和单次写不变量。

### 阶段 2：Repository 与失败安全

1. 用 JSON Repository 测试证明更新保留 `id`、`completed`、`nextId`、其他任务和相对
   顺序；
2. 复用现有临时文件加原子替换写入路径，不增加第二套写入协议；
3. 用确定性写入失败测试证明原始字节不变且临时文件被清理；
4. 记录最终 `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)` 失败仍未被直接故障注入
   的既有证据边界，不把它误写成已验证。

### 阶段 3：CLI 与组合入口

1. 新增 `RenameCommand` 并注册到根命令；
2. 复用现有标题无效、任务不存在和持久化异常到退出码 `2`、`2`、`1` 的映射；
3. 验证 Picocli 非整数 ID 和参数数量错误不会创建服务侧数据访问；
4. 添加真实组合入口测试，覆盖成功、已完成任务、幂等、无文件和无副作用路径。

### 阶段 4：跨命令和真实 JAR 验收

1. 把 `rename` 加入损坏 JSON 跨命令矩阵；
2. 构建真实 JAR，在临时目录创建任务、完成必要状态、重命名并使用 `show`、`list` 和
   `summary` 复核；
3. 对幂等、不存在、标题无效和损坏 JSON 路径比较执行前后文件 SHA-256；
4. 验证无数据文件路径不会创建文件；
5. 所有手工场景只使用临时目录，不访问用户真实数据。

### 阶段 5：完整验证、证据与受保护 PR

1. 运行 Application、Repository 和 CLI 定向测试；
2. 在 JDK 21 下运行完整 `.\mvnw.cmd verify`；
3. 将实际命令、测试数量、输出、退出码、哈希和已知边界写入
   `docs/evidence/004-rename-task.md`；
4. 由主智能体独立审查 diff 并复跑完整门禁和真实 JAR 场景；
5. 通过受保护功能 PR 合并，在最终 `main` 回查远程 `verify`；
6. 将决策状态更新为已验证、计划移入 completed，并在文档索引登记证据和归档计划；
7. 必要时使用独立归档 PR，不提前填写尚未发生的 PR、提交或 CI 结果。

## 成功标准

- AC-15 的每个成功、幂等、输入错误、读取失败和写入失败分支都有自动验证；
- 标题校验确实发生在 Repository 读取之前；
- 成功修改只发生一次持久化写入，幂等和所有使用错误路径不写入；
- 产品输出、错误流和退出码与 `SPEC.md` 完全一致；
- 分层规则、Checkstyle、全部 JUnit 测试、ArchUnit 和可执行 JAR 打包全部通过；
- 真实 JAR 验证与自动测试结论一致；
- 证据明确区分“直接验证”“继承既有保障”和“尚未直接故障注入”的边界；
- 受保护 PR 与最终 `main` 的远程 `verify` 均成功后，才把 AC-15 标记为已实现。

## 已知与未知边界

- 已知：现有 `TaskRepository.update(StudyTask)` 和 JSON 原子写入路径足以表达“只改标题”，
  预计不需要新增持久化端口或数据迁移；
- 已知：Application 当前在 `addTask` 内直接实现标题校验，实施时需要复用规则并避免两份
  行为漂移，但不得借机进行无关重构；
- 已知：标题校验先于读取，因此无效标题会遮蔽不存在任务或损坏 JSON，并且这是已批准
  的产品顺序；
- 边界：比较对象是“归一化后的新标题”和“持久化的原标题”的 Java 字符串精确相等；
  不进行大小写折叠、Unicode 归一化或内部空白压缩；
- 边界：现有生产代码没有可注入的最终原子替换边界，本次沿用决策卡 008 已接受的直接
  测试缺口；如果出现重评触发条件，应先重新决策；
- 未知：实施时是否需要独立的 `RenameTaskResult` 类型属于局部设计选择，由后续子智能体
  根据可读性和测试反馈决定，不得改变产品协议；
- 未知：并发进程在 Application 读取后、Repository 更新前修改或删除同一任务的行为仍
  受现有单文件 Repository 的竞态边界约束，本次不新增锁或并发协议。

## 验证顺序

1. `.\mvnw.cmd -Dtest=StudyTaskServiceTest,JsonTaskRepositoryTest test`
2. `.\mvnw.cmd -Dtest=StudyTrackApplicationTest,RenameCommandTest test`
3. 损坏数据、标题校验顺序和写入失败的定向测试
4. `.\mvnw.cmd package` 后执行真实 JAR 临时目录场景
5. JDK 21 下执行完整 `.\mvnw.cmd verify`
6. 主智能体独立审查与复验
7. 受保护 PR 合并后回查最终 `main` 的远程 `verify`

## 本地验证

- JDK 21 环境自检：通过；默认 Java 17 不满足门禁，因此所有 Maven 与真实 JAR 命令均
  显式使用本机既有 `D:\work\jdk\jdk-21.0.11`，没有修改仓库或机器级配置；
- Application 与 Repository 定向测试：31 项通过；
- CLI 与 Bootstrap 定向测试：35 项通过；
- JDK 21 打包：78 项通过并生成 `target/study-track.jar`；
- 真实 JAR 临时目录验收：成功重命名已完成任务，`show`、`list`、`summary` 一致；
  幂等、不存在、无效标题和损坏 JSON 场景的原文件 SHA-256 均不变，无数据文件场景
  未创建文件；
- JDK 21 完整 `verify`：78 项通过，Checkstyle、JUnit、ArchUnit 和可执行 JAR 打包全部
  成功；
- 详细输出、哈希与证据边界见
  [`docs/evidence/004-rename-task.md`](../evidence/004-rename-task.md)；
- 主智能体审查、功能分支 push、功能 PR、远程 CI、合并和最终 `main` 验证均待验证。

## 进度

- [x] 人类批准产品语义
- [x] 决策卡 010、`SPEC.md` 2.7 节和 AC-15 已起草
- [x] 执行计划 013 已起草
- [ ] 规划 PR 通过并合并
- [x] 后续子智能体开始实施
- [x] Application 与 Repository 行为完成
- [x] CLI 与组合入口行为完成
- [x] 失败安全与真实 JAR 验收完成
- [x] 子智能体完整 `verify` 通过
- [ ] 主智能体审查与独立复验
- [ ] 功能 PR 通过并合并
- [ ] 最终 `main` 远程 `verify` 成功，证据和计划归档
