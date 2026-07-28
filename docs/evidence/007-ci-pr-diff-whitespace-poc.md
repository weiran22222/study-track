# CI PR 完整差异空白门禁本地 POC 证据

状态：本地 POC 完成，远程门禁尚未验证

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

## 证据边界

本证据只记录上述两个已经发生的本地 Git 命令范围、实际退出码、诊断和清理事实。它没有
执行尚未创建的仓库脚本，没有修改或运行 GitHub Actions，也没有观察功能 PR 的必需
`verify` 失败、修复后成功、合并或最终 `main` 验证。

远程负向探针及恢复结果必须在功能 PR 实际发生后以 GitHub PR 与 Actions 为权威记录，
不得从本地 POC 预填或推断。
