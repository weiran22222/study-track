# main 合并门禁验证证据

状态：验证中

日期：2026-07-27

## 验证目标

证明 GitHub 上的 `main` 不再只是运行 CI，而是会在变更进入主分支前强制要求 PR 和
`verify` 成功。

## 远程配置回查

对 `weiran22222/study-track` 的 GitHub API 回查结果：

| 项目 | 实际值 |
|---|---|
| 可见性 | `public` |
| 目标分支 | `main` |
| 必须通过 PR | 是 |
| 审批数 | `0` |
| 必需状态检查 | `verify` |
| 要求分支为最新 | `true` |
| 管理员必须遵守 | `true` |
| 允许强制推送 | `false` |
| 允许删除 | `false` |
| 线性历史 | `false` |
| 会话解决要求 | `false` |

## 待验证行为

- [x] 当前证据提交直接推送到 `main` 被拒绝；
- [x] 测试分支可以推送并创建 PR；
- [x] `verify` 完成前 PR 不可合并；
- [x] `verify` 成功后 PR 可以合并；
- [ ] 证据通过 PR 合并进入 `main`；
- [ ] 合并后的 `main` 远程 CI 成功。

## 证据边界

本文件记录 GitHub 返回的配置和行为结果，不把配置声明本身视为有效门禁。只有拒绝直接推送
和真实 PR 状态转换均被观察到后，才可以关闭执行计划。

## 直接推送拒绝

在分支提交 `b087a21` 上执行非强制推送 `git push origin HEAD:main`，远程返回：

```text
GH006: Protected branch update failed for refs/heads/main.
Changes must be made through a pull request.
Required status check "verify" is expected.
```

该提交只包含本证据文件和活动计划更新，因此即使保护配置意外失效，也不会改变产品行为。
实际结果为远程拒绝，`main` 没有移动。

## PR 初次成功状态

[PR #1](https://github.com/weiran22222/study-track/pull/1) 已从
`codex/verify-main-gate` 指向 `main`。首次查询时，两次 `verify`（分别由 `push` 和
`pull_request` 触发）都已经成功，GitHub 返回：

```text
mergeable=true
mergeable_state=clean
```

这证明检查成功后 PR 可以合并，但查询发生得太晚，没有捕获检查运行中的阻塞状态。为避免
把缺失证据写成成功，本 PR 将增加本段说明性提交，并在推送后立即重新观察 pending 状态。

## PR 检查运行中状态

说明性提交 `7f00b23` 推送后立即回查 PR #1：

```text
mergeable=true
mergeable_state=blocked
verify (push)=queued
verify (pull_request)=in_progress
```

Git 内容本身可以合并，但在必需检查尚未完成时，GitHub 将 PR 状态标记为 `blocked`。
这证明 `verify` 已成为合并前条件，而不是合并后的通知。

随后，同一提交的两个 `verify` 均返回 `completed/success`，PR 状态变为：

```text
mergeable=true
mergeable_state=clean
```

由此形成同一提交上的状态转换证据：`blocked`（检查未完成）→ `clean`（检查成功）。
