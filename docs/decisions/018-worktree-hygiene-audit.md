# 决策卡 018：只读审计本地 worktree

状态：已被 [决策卡 019](019-codex-managed-worktree-lifecycle.md) 取代

日期：2026-07-28

## 问题

长期保留的 linked worktree 会增加本地状态噪声，但仅凭“目录存在”或“工作树干净”不能
判断其任务与 PR 是否已经完成，也不能安全地自动删除。人工逐个拼接 Git 查询容易漏查
dirty、detached、无 upstream 或相对 upstream 不同步等风险。

## 人类决定

学习者于 2026-07-28 明确批准：增加一个兼容当前 Windows PowerShell 5.1 的只读发现
脚本，从仓库根审计所有 worktree，并把输出分为 `PRIMARY`、`ATTENTION` 和
`REVIEW-CANDIDATE`。候选仍必须由人类确认任务与 PR 已完成后才能处理。

## 风险等级

这是第二级、局部且可逆的 Harness 改进。它不改变产品、架构、验收语义、工具链、远程
权限或分支状态，只增加本地只读诊断与静态机械契约。决策卡和实现放在同一个受保护 PR，
不创建执行计划或独立证据文件。

## 决定

- 主 worktree 始终标为 `PRIMARY`，不作为自动清理候选；
- linked worktree 只有在干净、有本地分支、有 upstream，且相对 upstream
  `ahead=0 behind=0` 时才标为 `REVIEW-CANDIDATE`；
- dirty、detached、无本地分支、无 upstream、upstream ref 缺失或不同步均标为
  `ATTENTION`；
- 候选不会改变脚本退出码，也不会触发任何删除、清理、推送或其他写操作；
- Git 调用或 porcelain 解析失败时退出非零，并按 `AGENTS.md` 输出六字段诊断；
- `AGENTS.md` 保存稳定发现命令，不登记易变的 worktree 清单或清理进度。

## 验证边界

跨平台 Maven CI 不执行 Windows PowerShell 脚本；Java 测试只静态保护脚本存在、入口可
发现、只读不变量、六字段失败反馈和关键分类条件。Windows 本地 POC 使用一个与远端同步
的历史分支验证 clean linked worktree 为 `REVIEW-CANDIDATE`，加入未跟踪文件后为
`ATTENTION`。该 POC 不证明未来所有 Git 输出或文件系统边界。

## 非目标

- 不自动移除或 prune worktree，不删除分支，不修改文件，不 push；
- 不查询或改变 GitHub、PR、分支保护或远端权限；
- 不安装或要求当前不存在的 `pwsh`；
- 不把静态契约测试描述为 PowerShell 行为级跨平台验证。

使用方式与失败反馈边界以 [`AGENTS.md`](../../AGENTS.md) 为准。
