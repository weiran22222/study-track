# 执行计划 014：Unicode 安全的标题文件输入

状态：待实施

## 目标

实施决策卡 012、`SPEC.md` 2.7 节与 AC-16，为 `rename` 增加 UTF-8 标题文件输入，使完整
Unicode 标题不经过 Windows 原生命令行，同时保持现有标题规则、失败安全和分层约束。

本计划只描述实现步骤、风险与验证方法。完整产品行为以 `SPEC.md` 为唯一权威。

## 范围

- 为 `rename` 增加与内联标题互斥的 `--title-file <path>` 输入；
- 在 Application 定义标题文件读取端口，在 Infrastructure 提供严格 UTF-8 实现；
- 由 Bootstrap 注入实现，CLI 不直接访问文件系统；
- 复用现有 `StudyTaskService.renameTask` 的规范化、Unicode 码点校验和持久化行为；
- 增加端口实现、CLI、Bootstrap、架构和真实子进程自动测试；
- 使用构建后的真实 JAR 保存 POC 边界的正式实施证据。

## 非目标

- 不移除或改变 `rename <id> <new-title>`；
- 不增加标准输入、Base64 参数、启动器包装脚本或 PowerShell 7 要求；
- 不改变标题长度、`String.strip()`、输出、幂等或任务持久化协议；
- 不修改 JSON 格式、执行数据迁移或增加外部依赖；
- 不承诺原生命令行无法表示的标题文件路径字符；
- 不顺便扩展 `add`、通用编辑、批处理、导入或交互模式。

## 目标设计

- `application/port/TitleFileReader.java`：以字符串位置读取标题的端口，不暴露
  `Path`、字符集或 Infrastructure 类型；
- `application/TitleFileException.java`：表示标题文件无法读取或无法严格解码；
- `infrastructure/input/Utf8TitleFileReader.java`：使用 `java.nio.file` 严格读取 UTF-8，
  并忽略文件开头至多一个 UTF-8 BOM；
- `cli/RenameCommand.java`：用 Picocli 表达两个互斥标题来源，调用注入端口后再调用现有
  Application 用例，并映射规格定义的输出和退出码；
- `bootstrap/StudyTrackApplication.java`：组合 CLI、标题读取实现和现有 Repository。

如果实施发现必须改变核心依赖方向、数据格式、CI、远程权限或引入新的构建插件，子智能体
必须停止并报告范围扩大，不得自行修改本计划或降低风险等级。

## 实施阶段

### 阶段 1：输入端口和 UTF-8 实现

1. 定义最小标题文件读取端口和稳定异常；
2. 实现严格 UTF-8 解码，覆盖 ASCII、BMP 字符、补充平面字符、可选 BOM、空文件、
   不存在、目录、不可读和非法 UTF-8；
3. 文件错误只返回规格中的稳定消息，不泄露堆栈或操作系统错误文本；
4. 保持 Application 与 CLI 均不依赖 `java.nio.file`。

### 阶段 2：CLI 与组合入口

1. 用 Picocli 表达“内联标题”和“`--title-file`”恰好二选一；
2. 保证非整数 ID、缺少标题源或同时提供两种来源时不调用标题读取端口和 Repository；
3. 标题文件成功读取后，把原始字符串交给现有 `renameTask`；
4. 映射标题文件异常为 stderr、退出码 `1`，且不读取任务数据；
5. 通过 Bootstrap 注入真实 UTF-8 实现，不在 CLI 中创建 Infrastructure 对象。

### 阶段 3：行为、顺序和失败安全测试

1. 用记录型端口与 Repository 证明“参数解析 → 标题文件读取 → 标题校验 → Repository
   读取”的顺序；
2. 验证空白、超过 200 码点、缺失、不可读和非法 UTF-8 路径均不访问任务数据；
3. 验证成功、已完成任务、幂等、不存在任务、损坏 JSON 和持久化失败保持 AC-15 行为；
4. 验证成功路径只修改标题，并保持 `id`、`completed`、`nextId`、其他任务和相对顺序；
5. 使用 Java `ProcessBuilder` 只传 ASCII 路径，自动覆盖 200/201 个 emoji 的真实子进程
   边界，避免再次依赖人工发现。

### 阶段 4：真实 JAR 与完整门禁

1. 在 JDK 21 下打包真实可执行 JAR；
2. 在经过校验的系统临时目录创建 UTF-8 标题文件和任务数据；
3. 验证 200 个 emoji 成功持久化为 200 个码点；
4. 验证 201 个 emoji 返回 `2` 且数据文件 SHA-256 不变；
5. 验证缺失文件、非法 UTF-8、参数冲突和损坏任务 JSON 的输出、退出码与无副作用；
6. 运行 JDK 21 完整 `verify`，把实际命令、结果、哈希和证据边界写入证据 005；
7. 由主智能体独立审查和复验后，通过受保护功能 PR 合并并回查最终 `main`。

## 风险与控制

- 风险：把文件读取直接放入 CLI 或 Application，绕过现有分层。
  控制：使用 Application 端口、Infrastructure 实现、Bootstrap 装配和 ArchUnit。
- 风险：只测 Java 字符串，遗漏原先的真实进程缺口。
  控制：保留 Java 子进程测试和真实 JAR 验收，标题内容只通过 UTF-8 文件进入。
- 风险：文件错误发生后仍读取损坏的任务 JSON，导致错误优先级漂移。
  控制：记录型端口断言标题输入失败时 Repository 读写均为零。
- 风险：测试把 emoji 放回命令行，重现 Shell 损失并误判实现。
  控制：命令行只传 ASCII 临时路径，测试在进程内生成并从文件读取 Unicode。
- 风险：标题文件过大造成内存压力。
  控制：本地单用户版本暂不增加独立文件大小协议；标题仍按 200 码点校验。若真实使用暴露
  持续成本，再通过新的产品决策增加流式上限。

## 成功标准

- AC-16 的参数互斥、UTF-8、BOM、文件错误、顺序和 200/201 码点边界均有自动测试；
- AC-15 的现有行为和全部既有测试保持通过；
- CLI 与 Application 不依赖 `java.nio.file`，Infrastructure 不依赖 CLI；
- 失败路径不读取、创建或修改任务数据；
- JDK 21 下 Checkstyle、JUnit、ArchUnit、子进程测试和可执行 JAR 打包全部通过；
- 功能 PR 与最终 `main` 的远程 `verify` 成功后，才把 AC-16 标记为已实现。

## 进度

- [x] 人类批准在 POC 成功后采用 UTF-8 标题文件方案
- [x] PowerShell 7 与 Java `@argfile` POC 已否证
- [x] UTF-8 标题文件隔离 POC 已通过
- [x] 决策卡 012、规格、目标架构与本计划已起草
- [ ] 规划 PR 通过并合并
- [ ] 无父对话子智能体实施功能与自动测试
- [ ] 主智能体独立审查、真实 JAR 复验和完整 `verify`
- [ ] 功能 PR 通过并合并
- [ ] 最终 `main` 远程 `verify` 成功
- [ ] 证据 005 保存实际结果，计划移入 `completed/`
