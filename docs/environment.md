# StudyTrack 构建环境

## 支持的环境契约

StudyTrack 只支持以下构建入口：

- JDK 21；
- 仓库内的 Maven Wrapper；
- 完整验收命令 `.\mvnw.cmd verify`（Windows）或 `./mvnw verify`
  （macOS/Linux）。

不使用系统 Maven，也不绕过 `pom.xml` 中的 Java 版本门禁。

## 环境自检

克隆仓库后，在仓库根目录先运行对应平台的自检。

Windows PowerShell：

```powershell
.\scripts\check-environment.ps1
```

macOS/Linux：

```sh
sh ./scripts/check-environment.sh
```

自检只读取当前环境并调用 Wrapper 的 `--version`，不会安装 JDK、修改 `PATH`、
修改 `JAVA_HOME`，也不会写入 Maven 配置。通过后会明确给出下一条完整验收命令。

## Java 版本不符

如果报告的 Java 主版本不是 21，请先安装或选择一个 JDK 21，并仅在当前 shell 或机器的
正常环境管理机制中更新 `JAVA_HOME` 和 `PATH`。不要把本机 JDK 的绝对路径提交到仓库。

PowerShell 可在当前会话中按本机实际安装位置设置：

```powershell
$env:JAVA_HOME = "实际的 JDK 21 安装目录"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

POSIX shell 可在当前会话中按本机实际安装位置设置：

```sh
export JAVA_HOME="/actual/path/to/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"
```

修改后重新运行环境自检。自检同时确认 `java` 命令和 Maven Wrapper 实际使用的运行时
都是 Java 21，避免 `PATH` 与 `JAVA_HOME` 指向不同版本。

## Wrapper 或网络失败

自检必须通过仓库内的 `mvnw.cmd` 或 `mvnw` 调用 Maven。Wrapper 文件缺失时，从仓库
恢复它们；不要改用系统 Maven。

Wrapper 第一次运行可能需要下载固定版本的 Maven。代理、证书、镜像或网络策略导致的
下载失败属于机器或组织环境问题。本 Harness 不会自动修改全局/用户 Maven 配置，也不
提供机器专属 settings。应使用所在环境批准的配置解决后，再运行相同自检。

## 证据边界

自检通过只证明当前机器上 JDK 21 与 Maven Wrapper 可以启动；完整 `verify` 通过才证明
当前代码满足仓库门禁。若依赖来自已有本地缓存，这两项成功都不能被描述为“干净机器冷启动
已经验证”。CI 只有在远程工作流实际成功后，才能作为远程环境的运行证据。
