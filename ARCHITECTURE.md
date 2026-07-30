# StudyTrack 架构

## 1. 设计目标

架构优先保证：

1. 产品行为可以独立测试；
2. 命令行框架和 JSON 库不会渗透到业务逻辑；
3. 依赖方向可以由 ArchUnit 机械检查；
4. 新的智能体只阅读少量入口文档就能定位修改位置。

产品行为以 [SPEC.md](SPEC.md) 为准。

## 2. 技术栈

- Java 21；
- Maven Wrapper；
- Picocli：命令行解析；
- Jackson Databind：JSON 序列化；
- JUnit 5：单元测试和集成测试；
- ArchUnit：架构边界检查；
- Maven Checkstyle Plugin：代码规范检查。

依赖版本必须集中锁定在 `pom.xml`，构建和验证必须使用仓库中的 Maven Wrapper。

## 3. 包级导航与关键入口

```text
src/main/java/com/example/studytrack/
├── bootstrap/       # 组合 CLI 与 Infrastructure 并启动应用
├── cli/             # 解析命令、调用用例并映射输出和退出码
├── application/     # 编排用例服务并定义输入与持久化端口
├── domain/          # 保存任务实体及领域状态
└── infrastructure/  # 实现外部输入与持久化端口
```

关键入口：

- `bootstrap/StudyTrackApplication.java`：应用启动与组件组合入口；
- `cli/StudyTrackCommand.java`：Picocli 根命令入口；
- `application/StudyTaskService.java`：应用用例服务入口；
- `application/port/TitleFileReader.java`：标题文件输入端口；
- `application/port/TaskRepository.java`：持久化端口；
- `infrastructure/input/Utf8TitleFileReader.java`：严格 UTF-8 标题文件读取实现；
- `infrastructure/persistence/JsonTaskRepository.java`：当前 JSON 持久化实现。

本节是包级导航，不是完整文件清单，也不展开测试目录。查询完整文件集合时，使用 Git、
IDE 或在仓库根目录运行 `rg --files`。增加、删除或移动上述关键入口时，应同步更新本节；
依赖合法性仍以第 4、5 节和 ArchUnit 测试为准。

## 4. 分层与依赖

```text
CLI ──────────────> Application ──────────────> Domain
                           ^
                           |
Infrastructure ────────────┘

Bootstrap 负责实例化和连接上述组件。
```

### Domain

包含任务实体及领域状态，不依赖项目内其他包，也不依赖 Picocli、Jackson 或文件系统。

### Application

包含用例服务，并定义由外部适配器实现的输入和持久化端口。它可以依赖 Domain，但不能
依赖 CLI、Infrastructure、Picocli、Jackson 或文件系统 API。输入端口必须使用不依赖
文件系统 API 的值，不得把 `Path` 或字符集实现细节带入 Application。

### Infrastructure

实现 Application 端口：`persistence` 负责读取和原子写入 JSON，`input` 负责严格读取
UTF-8 标题文件。它可以依赖 Domain 和 Application 中的端口，不能包含命令行展示逻辑。

### CLI

负责参数解析、通过注入的 Application 端口解析标题来源、调用 Application 服务，以及
把结果转换为标准输出、标准错误和退出码。它不得直接使用 Jackson 或 `java.nio.file`。

### Bootstrap

是唯一允许同时依赖 CLI 和 Infrastructure 的位置，负责组合标题输入、任务持久化与 CLI
并启动 Picocli。

## 5. 机械化架构规则

`src/test/java/com/example/studytrack/architecture/ArchitectureTest.java` 必须通过 ArchUnit 检查：

1. `domain..` 不依赖其他项目包；
2. `application..` 不依赖 `cli..` 或 `infrastructure..`；
3. `application..` 不依赖 Picocli、Jackson 或 `java.nio.file..`；
4. `cli..` 不依赖 `infrastructure..`、Jackson 或 `java.nio.file..`；
5. `infrastructure..` 不依赖 `cli..`；
6. 只有 `bootstrap..` 可以同时依赖 CLI 和 Infrastructure。

错误信息应指出违反规则的类、依赖目标、修复方向，并链接本节。

## 6. 持久化策略

保存数据时：

1. 在目标文件所在目录创建临时文件；
2. 使用 UTF-8 写入完整 JSON；
3. 成功后替换目标文件；
4. 写入或替换失败时保留原始数据；
5. 清理本次操作产生的临时文件。

Repository 将存储异常转换为项目定义的持久化异常；CLI 只负责把该异常映射为退出码 `1`，不泄露堆栈信息给普通用户。

## 7. 验证流水线

本地产品代码、架构与构建产物的唯一完整验证入口是：

```powershell
.\mvnw.cmd verify
```

该命令必须执行：

1. Java 编译；
2. Checkstyle；
3. JUnit 单元测试；
4. CLI 集成测试；
5. ArchUnit 架构测试；
6. 可执行 JAR 打包。

GitHub Actions 保留唯一且同名的 `jobs.verify`。`pull_request` 未按目标分支过滤，因此
覆盖所有 PR 目标分支；activity types 明确为 `opened`、`synchronize`、`reopened` 与
`edited`。每次实际触发都按固定顺序运行完整 PR 门禁：

1. 使用事件提供的 base/head ref 检查分支流；
2. 使用 base/head SHA 调用 `sh ./scripts/check-pr-diff.sh` 检查完整 `base...head`
   差异；checkout 必须获取完整历史，使两个端点及合并基点可达；
3. 从只读 `$GITHUB_EVENT_PATH` 提取 PR body 与 `pull_request.head.sha`，把 body 安全
   写入 runner 临时文件，再调用
   `sh ./scripts/check-pr-evaluator-report.sh <body-file> <expected-head-sha>`；
4. 无条件执行 JDK 21 环境自检和同一 Maven 命令。

第三步只解析决策 031 定义的窄 v1 逐行协议：唯一 marker、八个固定顺序非空字段、完整
40 位 Subject SHA 与 event head 精确相等、`Verdict: PASS`。workflow 不把 PR body
直接插值进 shell；脚本只读取本地参数与文件，不访问网络、GitHub API 或 Git，也不修改
状态。

`push` 事件只匹配 `develop` 与 `main`，用于长期分支更新后的最终非 PR 验证；普通
`codex/*`、`hotfix/*` 等工作分支 push 不触发 CI。`develop` 与 `main` 的 push 没有 PR
语义，不运行分支流、差异或 evaluator 报告门禁，但仍无条件执行同一 JDK 21 环境自检和
Maven `verify`。因此 PR 门禁、长期分支最终验证和本地 Maven 验收共用产品与架构验证
入口，又不会为工作分支 push 重复运行一次完整构建。

PR 差异与 evaluator 报告门禁是 CI 合并验收，不绑定 Maven 生命周期。本地 Windows
`.\mvnw.cmd verify` 不执行 POSIX 脚本，也不依赖系统 `sh`。提交前的
`git diff --cached --check`、本地 Maven `verify`、PR-only 完整差异门禁和报告门禁分别
保护暂存内容、产品/架构构建、最终 PR 差异及当前报告 envelope，不能互相替代。

报告门禁只证明 PR body 存在唯一、结构符合 v1、Subject SHA 等于 event head 且自述
`Verdict: PASS` 的报告。它不证明 generator/evaluator 身份或独立性、报告真实性、
验证完整性、完整 CommonMark 语义或 Harness 正向因果效果。

### 构建幂等性

统一验证命令必须能够在不执行 `clean` 的情况下重复运行。打包阶段不得把上一次
生成的聚合 JAR 作为下一次 Maven Shade 的输入，否则同一份代码会因为执行历史
不同而产生不同的构建告警或产物。

当前通过以下机制保护这一不变量：

- `maven-jar-plugin` 在每次打包前使用 `forceCreation` 重新创建原始项目 JAR；
- `BuildConfigurationTest` 检查该配置存在且为 `true`；
- 修改打包策略时，必须同步更新本节和相应回归测试。

这项结构测试保护已经发现的根因，但不是对所有构建幂等性问题的完备证明。如果
后续再次出现与执行历史有关的构建差异，应考虑升级为连续执行两次构建的行为级
CI 检查。
