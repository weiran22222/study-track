# 决策卡 012：使用 UTF-8 标题文件绕过 Windows 命令行编码边界

状态：已批准，本地实施完成，待功能 PR 与最终 `main` 验证

日期：2026-07-28

## 问题

`SPEC.md` 按 Unicode 码点定义标题长度，但 Windows 上从 PowerShell 启动 Java 进程时，
补充平面字符可能在进入 JVM 前被系统原生编码替换。黑盒评估因此观察到：恰好 200 个
emoji 本应有效，却在真实进程中被错误拒绝。

现有 Application 已使用 `String.codePointCount` 正确校验；缺陷位于 Shell、Windows
原生参数和 Java 启动器之间，不能通过修改业务校验修复。

## 候选方案与 POC

### A. 接受 Windows 命令行限制

- 收益：不增加产品协议；
- 成本：规格承诺的 Unicode 边界无法在受支持的 Windows 调用链上兑现。

### B. 把 PowerShell 7 规定为受支持环境

使用便携版 PowerShell 7.6.3 分别验证 `Windows`、`Standard` 和 `Legacy` 三种原生命令
参数模式，200 个 emoji 在进入 Java 后仍被破坏。本方案 POC 失败。

### C. 使用 Java `@argfile`

JDK 21 的参数文件按系统原生编码解释。本机 `native.encoding` 为 GBK，而 GBK 无法表示
emoji；UTF-8 参数文件仍错误拒绝 200 个 emoji。本方案 POC 失败。

### D. 增加 UTF-8 标题文件输入

命令行只传递文件路径，Infrastructure 在 JVM 内按 UTF-8 读取标题，再复用现有重命名
用例。隔离 POC 使用临时 shim 和真实 CLI、Application、JSON Repository 验证：

- 200 个 emoji 被准确持久化为 200 个 Unicode 码点；
- 201 个 emoji 返回退出码 `2`，操作前后数据文件 SHA-256 相同；
- 临时 shim、数据和标题文件全部清理，仓库没有改动。

本方案 POC 通过。

## 人类决定

学习者于 2026-07-28 批准增加 UTF-8 标题文件输入，但要求先做 POC；在 PowerShell 7 和
`@argfile` 方案被否证、UTF-8 文件通道 POC 通过后，批准条件满足，选择方案 D。

## 决定与取舍

- 保留现有内联标题形式，增加互斥的 `--title-file` 形式；
- UTF-8 文件通道是跨 Shell 传递完整 Unicode 标题的受支持方式；
- 完整产品协议、错误输出、处理顺序和验收标准以 `SPEC.md` 2.7 节及 AC-16 为准；
- 标题文件路径自身仍通过原生命令行传递，因此跨环境验收使用 ASCII 路径；本决定不承诺
  原生命令行无法表示的路径字符；
- 不引入 PowerShell 7 安装要求、启动器包装脚本、Base64 参数、标准输入模式或新的
  Maven 依赖；
- 不修改 JSON 格式、已有任务数据或标题的业务校验规则。

## Harness 影响

这是改变 CLI 产品协议、错误语义和目标架构的第三级变更。按决策卡 006 与 011：

1. 规划 PR 先固化本决策、`SPEC.md`、`ARCHITECTURE.md` 和执行计划 014；
2. 规划合并后，由无父对话子智能体实施功能、自动测试与证据；
3. 功能必须通过受保护 PR、必需 `verify` 和最终 `main` 验证；
4. GitHub PR 与 Actions 保存远程结果；只有出现决策卡 011 规定的情况才增加收尾 PR。

## 不变边界

- CLI 和 Application 不得直接使用 `java.nio.file`；
- 标题文件读取必须发生在任务 Repository 读取之前；
- 读取、解码或标题校验失败不得读取、创建或修改任务数据；
- `rename` 的成功、幂等、任务不存在和持久化失败语义保持不变；
- 不扩展为通用编辑、导入、批处理、交互式编辑器或标题历史功能。

## 本地实施状态

2026-07-28，功能分支已按本决定完成本地实现、自动测试、JDK 21 完整 `verify` 和真实
JAR 验收。实际命令、退出码、SHA-256、自动子进程与真实 JAR 的证据边界记录在
[证据 005](../evidence/005-unicode-safe-title-file-input.md)。

这些本地事实不代表功能 PR 已创建、远程 CI 已通过、功能已合并或最终 `main` 已验证。
