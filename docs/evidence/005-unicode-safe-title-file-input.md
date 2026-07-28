# Unicode 安全的标题文件输入实施证据

状态：本地实施完成，待功能 PR 与最终 `main` 验证

日期：2026-07-28

## 验证目标

证明 `rename <id> --title-file <path>` 按决策卡 012、`SPEC.md` 2.7 节与 AC-16 在 JVM
内严格读取 UTF-8 标题，同时保持参数互斥、错误优先级、零任务数据访问、AC-15 行为和
既有分层。

## 无父对话实施与分层

实施子智能体从 `main` 提交 `02f327c00f847a08caf74419295172e5b658097f` 的干净工作树
创建 `codex/title-file-input`，并从仓库文档恢复全部产品与架构语义。实现后的依赖链为：

```text
RenameCommand -> TitleFileReader <- Utf8TitleFileReader
       |
       +--------> StudyTaskService -> TaskRepository <- JsonTaskRepository

StudyTrackApplication 组合上述组件
```

Application 定义不暴露 `Path` 或字符集细节的 `TitleFileReader` 端口及稳定
`TitleFileException`；Infrastructure 使用 `java.nio.file`、UTF-8 `CharsetDecoder` 和
`CodingErrorAction.REPORT` 实现严格解码，并只忽略开头的一个 UTF-8 BOM。CLI 通过
Picocli 命令级参数预处理器在绑定和转换前验证标题来源形状，读取文件成功后才创建任务
服务并调用既有 `renameTask`。CLI 和 Application 均未直接访问文件系统，Bootstrap
仍是 CLI 与 Infrastructure 的唯一组合入口。

没有新增依赖、修改 JSON 格式、CI、远程设置或数据迁移。

## 自动验证

JDK 固定为 `D:\work\jdk\jdk-21.0.11`。环境自检结果为 Java 21、Maven Wrapper
3.9.12。定向命令覆盖 Application、Architecture、Bootstrap、CLI、UTF-8 Reader 和
Java 子进程测试：

```powershell
.\mvnw.cmd `
  -Dtest=RenameCommandTest,Utf8TitleFileReaderTest,StudyTrackApplicationTest,UnicodeTitleFileProcessTest,StudyTaskServiceTest,ArchitectureTest `
  test
```

最终结果为 79 项通过、0 失败、0 错误。完整命令：

```powershell
.\mvnw.cmd verify
```

最终结果为 97 项通过、0 失败、0 错误；Checkstyle 与 ArchUnit 通过，并生成
`target/study-track.jar`。相对既有 78 项测试，本轮新增 19 项，自动覆盖：

- 完整 UTF-8 的 ASCII、BMP 与补充平面字符，开头可选 BOM 和空文件；
- 缺失文件、目录及非法 UTF-8 的稳定错误，不泄露底层异常或堆栈；
- 没有标题源、非整数 ID、内联标题与 `--title-file` 冲突时不读取标题文件或任务数据；
- 负整数 ID 继续进入任务查询并返回任务不存在，`--` 后负号开头的内联标题保持可用；
- 标题文件读取失败时不创建任务服务，空白或 201 码点标题时 Repository 读取为零；
- 标题文件读取、标题校验、Repository 读取和更新的调用顺序；
- AC-15 的内联成功、幂等、不存在、损坏 JSON 与持久化失败回归；
- 真实 UTF-8 实现下的 BOM、错误优先级和数据文件无副作用；
- 只把 ASCII 数据和标题路径传入真实 Java 子进程的 200/201 个 emoji 边界。

`UnicodeTitleFileProcessTest` 使用 `ProcessBuilder` 启动新的 Java 21 进程并调用真实
`StudyTrackApplication` 主类。它从 UTF-8 文件传入标题，证明 200 个补充平面码点成功，
201 个返回 `2` 且数据 SHA-256 不变。

## 真实 JAR 验收

完整 `verify` 生成的 `target/study-track.jar` 在经过以下检查的系统临时目录运行：

```text
C:\Users\weiran\AppData\Local\Temp\study-track-title-9876ac0fb9544bc582ab2621d74334ff
```

脚本先用 `Path.GetFullPath` 确认目录的直接父目录是系统临时目录，并确认完整路径只含
ASCII 字符。成功场景观察到：

```text
add.exit=0
add.stdout=Created task 1: Seed
rename200.exit=0
rename200.stdout=Renamed task 1.
rename200.persistedCodePoints=200
```

201 个 emoji 场景观察到：

```text
rename201.exit=2
rename201.stderr=Task title must contain between 1 and 200 characters.
rename201.hash.before=94F45ACEBAE26825F1D05A7C2D01FEC0CF1DC04A413CDF5344BEF99350EF4E2B
rename201.hash.after=94F45ACEBAE26825F1D05A7C2D01FEC0CF1DC04A413CDF5344BEF99350EF4E2B
```

同一真实 JAR 还得到以下结果：

- 缺失标题文件退出 `1`，输出批准的 `Title file error`；
- 非法 UTF-8 标题文件退出 `1`，输出相同稳定错误格式；
- 同时提供两个标题源退出 `2`，首行输出
  `NEW-TITLE and --title-file are mutually exclusive.`；
- 有效标题文件配合损坏任务 JSON 时退出 `1` 并输出 `Data file error`。

损坏任务 JSON 的前后 SHA-256 均为：

```text
3D6E9D06D4C4F4936F8CEC161700FEBC706471E86A3B7E165F7A1B599B62E121
```

验收完成后再次校验目标仍是系统临时目录的直接子目录，再递归清理；最终
`cleanup.exists=False`。

主智能体独立审查发现参数预处理器最初把 `-1` 误分类为选项。修复后使用最终 JAR
回归验证：

```text
negativeId.exit=2
negativeId.stderr=Task -1 not found.
negativeId.hash.before=41F7E9BDFC818F9F0D1C4096DC1F79F2CEDAC580E40258F9E89940A868E2E371
negativeId.hash.after=41F7E9BDFC818F9F0D1C4096DC1F79F2CEDAC580E40258F9E89940A868E2E371
dashTitle.exit=0
dashTitle.stdout=Renamed task 1.
cleanup.exists=False
```

## 证据边界

Maven Surefire 的 `test` 阶段发生在 `package` 之前，因此自动子进程测试使用当前测试
类路径启动真实 Java 进程，不把它虚报为 JAR 自动测试。真实可执行 JAR 由上述独立本地
验收覆盖；本轮没有为了在测试阶段提前生成 JAR 而改变 Maven 生命周期或增加插件。

文件权限在 Windows、POSIX 与特权执行环境中的可控性不同；自动测试确定性覆盖缺失、
目录和非法 UTF-8，并通过注入异常证明所有标题读取失败都保持零任务数据访问。它没有
伪装一个权限语义不稳定的“不可读普通文件”场景。

本地 Maven 使用已有依赖缓存，不能证明干净机器无缓存冷启动。真实 JAR 的文件系统结论
只直接适用于本次 Windows 系统临时目录。尚未创建或推送功能 PR，未观察功能分支远程
CI、功能合并或最终 `main` 验证；这些不能由本证据推断。
