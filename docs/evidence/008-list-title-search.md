# `list --contains` 本地 generator 实施证据

日期：2026-07-29

角色：generator

范围：`SPEC.md` 2.2 节与 AC-18 的本地实现、自测和真实 JAR 代表场景

本文只记录本轮实际发生的本地命令、结果、文件状态、哈希和证据边界。它不是 evaluator
报告，不给出 `PASS`，也不证明 PR、required CI、合并或最终 `develop` 状态。

## 环境与失败/修复轮次

首次在继承的 shell 中运行：

```powershell
.\scripts\check-environment.ps1
```

实际退出 `1`。输出定位为 `java --version`，检测到 Java 17，而仓库要求 Java 21。脚本
没有修改环境或仓库。确认本机已有 `D:\work\jdk\jdk-21.0.11` 后，只为后续命令进程设置
`JAVA_HOME` 和 `PATH`，重新运行同一脚本；实际退出 `0`：

```text
StudyTrack environment check passed.
Java: 21
Maven Wrapper: Apache Maven 3.9.12
Next: .\mvnw.cmd verify
```

第一次定向测试命令实际成功，但 Checkstyle 报告了本轮新增测试的 5 条
`VariableDeclarationUsageDistance` 告警：

```powershell
.\mvnw.cmd "-Dtest=StudyTaskServiceTest,StudyTrackApplicationTest" test
```

该次结果为 68 项测试、0 失败、0 错误，构建退出 `0`。随后只把相应测试快照和结果变量
声明为 `final`，重跑同一命令；实际仍为 68 项测试、0 失败、0 错误、退出 `0`，且
Checkstyle 输出 `You have 0 Checkstyle violations.`，不再出现上述 5 条告警。

真实 JAR 的补充平面参数手工尝试暴露的是 Windows 原生命令行传输边界，而不是自动测试
中的码点校验失败：

- 通过 Windows PowerShell `Start-Process -ArgumentList`、直接经过 `rtk` 以及由 JDK 21
  JShell 中的 Java `ProcessBuilder` 启动 `target/study-track.jar`，尝试把 200 个
  U+1F600 作为内联原生命令行参数传给 `add`；
- 调用端构造值的 `codePointCount` 为 200，但子进程收到的参数已被当前 Windows 原生
  启动链转换，命令实际退出 `2` 并输出标题长度错误；
- `unicode.json`、`unicode-direct.json`、`unicode-java.json`、`unicode-java2.json`
  和 `unicode-java3.json` 均不存在，未留下任务数据。

本轮没有增加查询文件或修改原生命令行协议。200/201 个补充平面码点的产品语义由下方
Application 与完整组合 CLI 自动测试在 JVM 内覆盖；真实 JAR 只选择当前原生命令行可
可靠表达的代表场景。

## 自动测试与完整门禁

Application 测试实际覆盖：

- 查询先 `strip()`，Java `String.contains` 的区分大小写字面匹配和 ID 排序；
- 200 个补充平面码点有效，201 个及空白查询无效；
- 无效查询时 `TaskRepository.findAll/create/update/delete` 调用次数均为 `0`。

组合 CLI 测试实际覆盖：

- 字面元字符、大小写、`--status` AND、排序和既有单行格式；
- 无匹配、无数据文件、损坏 JSON、缺少 option 参数；
- 精确错误、退出码和数据文件不存在或字节不变；
- 200/201 个补充平面码点边界。

完整命令：

```powershell
.\mvnw.cmd verify
```

在 JDK 21 下先后实际运行两次，均退出 `0`，均为 139 项测试、0 失败、0 错误、0 跳过；
Checkstyle 0 violation，ArchUnit 测试通过。第一次完整构建生成的 JAR SHA-256 为
`c17fd39914996daf6dbf70fe4378855363467070fd314b8139fbffcd3bf1b938`，用于第一轮真实 JAR
场景。补强缺参数自动断言和更新文档后运行最终完整构建，当前产物为：

```text
target/study-track.jar
size: 2786457 bytes
sha256: fc60386b95561f3c4d90a8b3241af6580c458dcb690faa4de901bda93626d250
```

最终构建后，又对当前哈希产物重跑字面元字符、状态 AND、无效查询优先于损坏 JSON 和
有效查询报告损坏 JSON 的场景，结果与下节记录一致。这是 generator 自检产物哈希；后续
任何代码、测试或构建变化都可能使它失效。

## 真实 JAR 代表场景

所有场景使用 JDK 21 和系统临时目录：

```text
C:\Users\weiran\AppData\Local\Temp\study-track-list-title-search-20260729-1954
```

通过真实 JAR 创建 3 条数据并完成第 1 条后，`tasks.json` 的基线 SHA-256 为：

```text
f3511dc7c16a15187b67a1bb0078ade2fadf1a58fb4e9a5ab04f174b78fe5a63
```

字面元字符和 `strip()`：

```powershell
java -jar target/study-track.jar --data-file <temp>\tasks.json list --contains "  .*  "
```

实际退出 `0`：

```text
[x] 1 Harness .* completed
[ ] 3 harness .* pending
```

状态 AND 与大小写：

```powershell
java -jar target/study-track.jar --data-file <temp>\tasks.json list --status pending --contains Harness
```

实际退出 `0`：

```text
[ ] 2 Harness plain pending
```

无匹配查询实际退出 `0` 并输出 `No tasks.`。三次成功查询后 `tasks.json` 的 SHA-256 仍为
上述基线值。

对同一个空的损坏 JSON 文件运行有效查询，实际退出 `1`，stdout 为空，stderr 为：

```text
Data file error: Unable to read data file <temp>\corrupt.json.
```

运行 201 个 ASCII 码点的无效查询时，实际退出 `2`，stdout 为空，stderr 精确为：

```text
Search text must contain between 1 and 200 characters.
```

两次运行前后 `corrupt.json` 的 SHA-256 都是空文件哈希：

```text
e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
```

缺少 option 参数：

```powershell
java -jar target/study-track.jar --data-file <temp>\missing-argument.json list --contains
```

实际退出 `2`，Picocli stderr 以
`Missing required parameter for option '--contains' (TEXT)` 开头，数据文件不存在。
对另一个不存在的数据文件运行有效查询，实际退出 `0`、输出 `No tasks.`，运行后文件仍
不存在。

## 证据边界

- 上述结果只覆盖当前工作树和本机已有依赖缓存下的 generator 自检，不是干净机器冷启动
  证明；
- 未 stage、commit、push、创建或修改 PR，也未操作 GitHub；
- 未冻结 Subject SHA，未运行 evaluator 前后 guard，未执行独立 evaluator；
- 未检查或声明 required CI、PR、合并、最终 `develop` SHA 或 Harness 效果结论；
- Windows 原生命令行无法在本轮手工场景中可靠传递补充平面内联参数；对应 200/201 码点
  语义由自动测试覆盖，该边界不授权增加查询文件或其他搜索功能。
