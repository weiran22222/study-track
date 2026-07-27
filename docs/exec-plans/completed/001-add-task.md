# 执行计划 001：实现添加学习任务

状态：已完成

## 目标

实现 `SPEC.md` 的“2.1 添加任务”，打通 CLI → Application → Repository → JSON
的第一个可运行垂直切片。

本计划覆盖：

- AC-01：生成递增 ID，并持久化处理后的标题；
- AC-05：拒绝空标题和超过 200 个 Unicode 码点的标题；
- AC-08：CLI 不直接访问文件系统或 JSON；
- AC-09：Application 不依赖具体持久化实现；
- AC-10：全部机械检查通过；
- AC-11：可执行 JAR 能运行 `add` 命令。

## 输入与权威来源

- 产品行为：[../../../SPEC.md](../../../SPEC.md#21-添加任务)
- 数据格式：[../../../SPEC.md](../../../SPEC.md#3-数据格式)
- 分层规则：[../../../ARCHITECTURE.md](../../../ARCHITECTURE.md#4-分层与依赖)
- 持久化策略：[../../../ARCHITECTURE.md](../../../ARCHITECTURE.md#6-持久化策略)

冲突时以 `SPEC.md` 的产品行为和 `ARCHITECTURE.md` 的依赖边界为准，不在本计划中
另造规则。

## 允许修改的范围

- `pom.xml`：仅在实现确实需要时调整；
- `src/main/java/com/example/studytrack/domain/`；
- `src/main/java/com/example/studytrack/application/`；
- `src/main/java/com/example/studytrack/infrastructure/`；
- `src/main/java/com/example/studytrack/cli/`；
- `src/main/java/com/example/studytrack/bootstrap/`；
- 对应的 `src/test/`；
- 本计划的进度与决策日志。

## 非目标

- 不实现 `list`；
- 不实现 `complete`；
- 不增加账号、网络、数据库、Spring Boot 或其他规格外功能；
- 不降低或绕过 Checkstyle、ArchUnit、Java 版本和测试门禁；
- 不修改 AC-01/AC-05 的产品含义来迎合实现。

## 可执行验收

使用临时数据文件完成以下验证：

```powershell
java -jar target/study-track.jar --data-file .\tmp\tasks.json add "  阅读 Harness  "
```

必须输出：

```text
Created task 1: 阅读 Harness
```

再次添加必须生成 ID `2`。数据文件必须符合 `SPEC.md` 定义的 JSON 格式。

以下输入必须返回退出码 `2`，向标准错误输出规格中的错误信息，且不修改数据：

- 空字符串；
- 只有空白的字符串；
- 超过 200 个 Unicode 码点的标题。

完整门禁：

```powershell
.\mvnw.cmd verify
```

本机 Maven Central TLS 不可用时，可以只为本地验证传入用户已有的 Maven
`settings.xml`；不得把机器专属镜像提交到仓库。

## 实现原则

- 先写或补齐会失败的测试，再实现最小代码；
- CLI 只负责参数、输出和退出码映射；
- Application 负责标题规则和新增用例；
- Repository 端口位于 Application；
- Infrastructure 负责 JSON 与原子写入；
- 测试必须使用临时目录；
- 失败信息应能指向约束和修复方向。

## 进度

- [x] 规格和架构已建立
- [x] Maven、Checkstyle、ArchUnit 和 CI 基线已建立
- [x] 添加任务测试已建立并观察到预期失败
- [x] 最小实现完成
- [x] `verify` 通过
- [x] 主智能体审查完成

## 决策日志

- 2026-07-27：选择 `add` 作为第一个垂直切片，因为它能最小化地贯通全部四层，
  并产生可观察的持久化结果。
- 2026-07-27：Application 使用 `String.codePointCount` 校验标题长度，确保规格中的
  “Unicode 码点”不会被 UTF-16 代码单元数量替代。
- 2026-07-27：CLI 通过由 Bootstrap 注入的服务工厂获取 Application 服务，使全局
  `--data-file` 仍由 CLI 解析，同时将 `Path` 和具体 JSON Repository 留在组合入口。
- 2026-07-27：JSON Repository 在目标目录写入 UTF-8 临时文件后使用原子替换；
  相关测试使用 JUnit `@TempDir`，不访问用户数据。
- 2026-07-27：主智能体审查发现连续运行 `verify` 会让 Shade 重复处理上一次的
  聚合 JAR；通过固定 Maven Jar Plugin 并启用 `forceCreation`，恢复门禁幂等性。
- 2026-07-27：主智能体连续运行两次完整 `verify`，13 个测试均通过；随后用可执行
  JAR 验证连续 ID、标题 `strip()` 和无效输入不修改数据，计划关闭。
