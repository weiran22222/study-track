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

## 3. 计划目录

```text
study-track/
├── AGENTS.md
├── SPEC.md
├── ARCHITECTURE.md
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
├── src/main/java/com/example/studytrack/
│   ├── bootstrap/
│   │   └── StudyTrackApplication.java
│   ├── cli/
│   │   ├── StudyTrackCommand.java
│   │   ├── AddCommand.java
│   │   ├── ListCommand.java
│   │   ├── CompleteCommand.java
│   │   ├── ShowCommand.java
│   │   ├── SummaryCommand.java
│   │   └── StudyTaskServiceFactory.java
│   ├── application/
│   │   ├── StudyTaskService.java
│   │   ├── CompleteTaskResult.java
│   │   ├── TaskSummary.java
│   │   ├── InvalidTaskTitleException.java
│   │   ├── TaskNotFoundException.java
│   │   ├── TaskPersistenceException.java
│   │   └── port/
│   │       └── TaskRepository.java
│   ├── domain/
│   │   └── StudyTask.java
│   └── infrastructure/
│       └── persistence/
│           └── JsonTaskRepository.java
└── src/test/java/com/example/studytrack/
    ├── architecture/
    │   ├── ArchitectureTest.java
    │   ├── BuildConfigurationTest.java
    │   └── EnvironmentBootstrapTest.java
    ├── bootstrap/
    │   └── StudyTrackApplicationTest.java
    ├── application/
    │   └── StudyTaskServiceTest.java
    └── infrastructure/
        └── JsonTaskRepositoryTest.java
```

该目录是新智能体的定位地图，不是第二份架构规则。增加、删除或移动主要入口类时，应同步
更新本节；依赖合法性仍以第 4、5 节和 ArchUnit 测试为准。

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

包含用例服务和 `TaskRepository` 端口。它可以依赖 Domain，但不能依赖 CLI、Infrastructure、Picocli、Jackson 或文件系统 API。

### Infrastructure

实现 `TaskRepository`，负责读取和原子写入 JSON。它可以依赖 Domain 和 Application 中的端口，不能包含命令行展示逻辑。

### CLI

负责参数解析、调用 Application 服务以及把结果转换为标准输出、标准错误和退出码。它不得直接使用 Jackson 或 `java.nio.file`。

### Bootstrap

是唯一允许同时依赖 CLI 和 Infrastructure 的位置，负责组合对象并启动 Picocli。

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

唯一的完整验证入口是：

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

CI 和本地开发使用相同命令，避免出现两套验收逻辑。

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
