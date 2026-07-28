# CI PR 完整差异空白门禁本地 POC 证据

状态：远程负向探针已验证，恢复验证尚未发生

日期：2026-07-28

## 验证目标

在不保留工作树改动、不创建长期分支和不推送远程的前提下，验证 Git 原生
`git diff --check <base>...<head>` 能区分干净的完整差异与新增文件尾空行。

## 干净范围

对以下既有提交范围运行：

```powershell
git diff --check a39416dd47b0eae7618291202adbc38843ef604e...6b32b34154223167139f7f4e779642d9b3bd8f4f
```

实际退出码为 `0`，没有空白错误输出。

## 负向范围

POC 使用临时索引树创建了未挂到任何分支或标签的提交：

```text
commit: 4a60881c69f6e26e51e58ecd29337b718d56a4fd
parent: 3fe3dc20115dd5dabf4b0b413be400ddc3e6fbfa
file: poc-ci-diff.txt
defect: 文件尾新增空行
```

父提交是规划分支起点。对该范围运行：

```powershell
git diff --check HEAD...4a60881c69f6e26e51e58ecd29337b718d56a4fd
```

实际退出码为 `1`，Git 报告：

```text
poc-ci-diff.txt:2: new blank line at EOF.
```

这直接证明三点差异可以在 PR 形状的完整 base/head 范围中定位该空白错误。

## 清理与对象边界

- `4a60881c69f6e26e51e58ecd29337b718d56a4fd` 是悬空 Git 对象，未挂到分支或标签；
- 该对象及 `poc-ci-diff.txt` 均未推送；
- POC 文件已从工作树删除；
- 临时索引已恢复；
- POC 完成后规划分支工作树干净。

悬空对象可由 Git 后续垃圾回收，不属于交付内容，不应为了保留证据而创建引用或推送。

## 功能实施阶段的脚本 POC

在 Git for Windows 的 POSIX shell 中，通过仓库 `target/pr-diff-poc` 下的一次性临时仓库
直接调用已实施的 `scripts/check-pr-diff.sh`。该临时仓库产生以下提交：

```text
base:       2d5677cde41b9e32512624415d8d2582e3be3bc5
clean head: 190a5b1341acb376398b3b61d0fb673236710480
bad head:   4a931cc99a39a3e7877b8bbb5fff79534eed2156
```

脚本对 `base...clean head` 退出 `0`，没有诊断输出；对 `base...bad head` 最终退出 `1`。
失败范围在 `sample.txt` 文件尾新增空行，Git 原始位置诊断为：

```text
sample.txt:3: new blank line at EOF.
```

脚本随后报告 `Location`、`Invariant`、`Reason`、`Fix`、`Recheck` 和 `Authority` 六个字段。
诊断中的 Git `diff --check` 退出码为 `2`；脚本的统一失败出口为 `1`。POC 同时断言诊断
包含决策卡 016 和稳定的 Git 官方
[`diff --check`](https://git-scm.com/docs/git-diff#Documentation/git-diff.txt---check)
链接，所有断言通过。

POC 后通过精确绝对路径删除临时仓库和 runner，并再次检查两者均不存在。因此上述三个
提交、临时工作树、临时索引和 `sample.txt` 已一并清理，没有引用、文件或对象留在交付
仓库中。

## JDK 21 本地验证

使用 `D:\work\jdk\jdk-21.0.11` 作为当前进程的 `JAVA_HOME`。Windows 环境自检实际通过，
报告 Java 21 与 Maven Wrapper 3.9.12。

定向命令：

```powershell
.\mvnw.cmd -Dtest=PullRequestDiffGuardTest,DocumentationNavigationTest test
```

实际运行 6 个测试，失败 `0`、错误 `0`。首次定向运行提示新增测试有一处 102 字符的
Checkstyle 行长警告；实施中已缩短该行。随后运行完整命令：

```powershell
.\mvnw.cmd verify
```

实际结果为 `BUILD SUCCESS`：Checkstyle 违规 `0`，运行 112 个测试，失败 `0`、错误 `0`、
跳过 `0`，并完成可执行 JAR 打包。该 Windows Maven 命令没有调用
`scripts/check-pr-diff.sh` 或系统 `sh`；新增 Java 契约测试只以 UTF-8 读取 workflow、
脚本、POM 和 Windows 环境检查文本。

## PR #28 远程负向探针

PR #28 在初始功能提交 `8922a26` 上显示 `2/2` 检查成功。随后推送专用负向探针提交
`07f977715bfbd964d69e8bc8bb0404c3ca325060`，其中
`poc-ci-whitespace.txt` 的文件尾包含新增空行。

该提交触发的 `push` 检查成功，证明没有 PR 上下文的 `push` 事件仍只运行既有环境与
Maven 验证。对应的
[`pull_request` run](https://github.com/weiran22222/study-track/actions/runs/30354267196)
在 4 秒后失败，PR 合并被禁用。失败
[Job 日志](https://github.com/weiran22222/study-track/actions/runs/30354267196/job/90258688563?pr=28)
保留 Git 的 `poc-ci-whitespace.txt:2: new blank line at EOF.` 位置诊断，并包含：

- `Location`：直接包含 `poc-ci-whitespace.txt:2: new blank line at EOF.` 原始 Git 诊断；
- `Invariant`：完整 PR `base...head` 差异不得包含 Git 空白错误；
- `Reason`：`git diff --check` 检测到空白错误并以非零状态退出；
- `Fix`：删除报告的行尾空白或文件尾多余空行；
- `Recheck`：使用相同 base/head SHA 重新运行仓库脚本；
- `Authority`：决策卡 016 与 Git 官方 `diff --check` 文档链接。

这证明 PR-only 门禁会拒绝包含 Git 空白错误的完整 PR 差异，同时 `push` 路径保持既有
语义。恢复结果尚未发生：当前只在本地工作树删除了探针文件，尚未提交、推送或观察恢复
后的远程 `verify`。

## 证据边界

本证据记录两个规划阶段 Git 范围以及功能实施阶段仓库脚本 POC、静态测试、JDK 21 完整
`verify` 和清理事实。它证明本地脚本能区分干净与空白错误范围，且当前 Windows Maven
验证不依赖系统 `sh`。它还记录 PR #28 已经发生的负向提交、`push` 成功、
`pull_request` 失败、六字段诊断和合并被禁用事实。

GitHub PR 与 Actions 是上述远程事实的权威记录。恢复后的远程门禁成功、功能合并及最终
`main` 验证均尚未发生或观察，不得从本地删除探针文件推断。
