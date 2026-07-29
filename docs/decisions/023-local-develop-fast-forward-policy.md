# 决策卡 023：本地 develop 只从 origin/develop 安全纯快进

状态：已实施（仓库内规则、入口与自动测试）

日期：2026-07-29

## 问题

GitHub 对远端 `develop` 的 PR 和 required `verify` 保护，不能阻止操作者在本地把 feature
或其他分支 merge、rebase 或 cherry-pick 到本地 `develop`。即使后续直接 push 会被远端
保护拒绝，本地 `develop` 仍可能混入未经 PR 审查的提交，导致后续建分支、验证或状态判断
使用错误基线。

仅约定“合并后运行 `git pull --ff-only`”也不完整：当本地 `develop` 已领先
`origin/develop` 时，命令可能不消除领先状态；如果没有先确认当前分支、工作树、暂存区和
提交关系，操作者仍可能把受污染的本地分支当成已同步基线。

## 候选方案与取舍

### A. 只保留文字规则，由操作者手工运行 Git 命令

- 收益：不增加仓库脚本或测试；
- 成本：每次都要人工拼接并解释状态检查，容易漏掉本地领先、分叉、错误分支或脏工作树，
  跨平台命令也可能漂移。

### B. 不再维护本地 develop，只使用临时 clone 或 detached HEAD

- 收益：减少对本地长期分支的直接修改；
- 成本：改变现有分支工作方式，增加 clone、目录和凭据管理成本，也不能替代 GitHub PR
  与最终远端验证。

### C. 稳定规则加仓库自有的跨平台安全更新入口

- 收益：把允许的唯一更新源、前置状态、精确 SHA 和纯快进后置条件机械化；PowerShell
  与 POSIX 使用同一契约，失败可给出一致、可操作的诊断；
- 成本：需要维护两个脚本及行为/静态契约测试；脚本只能保护本地更新，GitHub PR、合并和
  Actions 结果仍需独立回读。

选择本方案。

## 人类决定

学习者于 2026-07-29 明确要求：

- 本地其他分支不能直接合并到本地 `develop`；
- 变更必须在 GitHub 中提交合并请求并在 GitHub 完成合并；
- GitHub 合并后，才在本地更新 `develop`。

本决策把“直接合并”明确为不得在本地 `develop` 上 merge、rebase 或 cherry-pick feature
及其他本地分支；hotfix 回流也必须遵守[决策卡 020](020-develop-production-branch-model.md)
的 `main → develop` GitHub PR 路径，不能在本地复制提交。

## 决定与理由

- 任何进入远端 `develop` 的变更都必须通过受保护 GitHub PR；不得直接 push，也不得先在
  本地 `develop` 合并、rebase 或 cherry-pick 其他分支再尝试推送；
- PR 在 GitHub 合并后，先回读最终远端 `develop` SHA，并确认该精确 SHA 的 push
  `verify` 成功；只有完成这项外部验证后，才允许更新本地 `develop`；
- 仓库提供 `scripts/update-local-develop.ps1` 和 `scripts/update-local-develop.sh`。
  两个入口只接收一个完整的、已经通过上述 GitHub 回读确认的 40 位 SHA；该参数是本地
  更新的精确目标锚点，不由脚本证明 CI 成功；
- 更新源硬编码为 `origin/develop`，不接受 remote、branch、ref 或其他更新源参数。入口
  只 fetch 远端 `origin` 的 `develop` 到 `refs/remotes/origin/develop`，再从该
  remote-tracking ref 纯快进；
- 入口在任何修改本地 `develop` 前必须 fail closed：当前目录是 Git 仓库，当前分支精确
  等于 `develop`，工作树干净，暂存区为空，fetch 成功，`origin/develop` 精确等于调用者
  提供的完整 SHA，且本地 `HEAD` 相对 `origin/develop` 既不领先也不分叉；
- 满足前置条件后，只允许以 `--ff-only` 把本地 `develop` 前移到
  `refs/remotes/origin/develop`。已经同步时成功且不创建提交；落后时只能纯快进；领先、
  分叉、错误 SHA、错误分支、脏工作树、非空暂存区或 Git/网络错误均非零退出且不修复；
- 更新后再次确认当前分支仍为 `develop`、`HEAD` 精确等于已验证 SHA、工作树干净且暂存
  区为空。脚本不得切换分支、reset、rebase、cherry-pick、强推或清理用户文件；
- 所有失败按 `AGENTS.md` 输出 Location、Invariant、Reason、Fix、Recheck、Authority
  六字段诊断。

完整 SHA 参数防止“远端在 CI 回读后又前移”时静默更新到未经确认的新提交；本地提交关系
检查防止 `--ff-only` 对“本地仅领先”场景给出表面成功。GitHub PR、合并记录和 Actions
仍是远端事实的权威来源，本地脚本不访问 GitHub，也不把本地成功描述为 PR 或 CI 证据。

## 边界

- 本决策只约束本地 `develop` 的更新路径，不改变决策 020 的普通、release、hotfix 或
  hotfix 回流 PR 拓扑；
- 不改变 `SPEC.md`、`ARCHITECTURE.md`、产品行为、数据格式、依赖、CI Job 身份、
  GitHub 分支保护或远程权限；
- 不授权脚本创建/切换分支、提交、push、创建/更新/合并 PR，或读取/修改 GitHub 状态；
- 不禁止在 feature 分支上为同步工作而进行经任务授权的操作；禁止项针对把 feature 或
  其他分支内容直接纳入本地 `develop`；
- `origin/develop` 是本地更新的唯一来源，但它不自行证明远端 CI 成功；操作者必须先用
  GitHub 权威记录确认精确 SHA 的最终 push `verify`；
- StudyTrack 仍没有真实部署；PR 合并、`develop` 更新和成功 CI 都不得描述为部署。

## 风险等级与交付

这是第三级 Harness 变更：它收紧智能体 Git 权限和长期分支操作语义，引入两个会修改本地
分支 ref 的跨平台入口，并包含 GitHub PR、最终远端验证和本地状态变化等有序阶段。按现有
Harness 使用“规划 PR → 功能 PR”：

1. 规划 PR 只交付本决策、执行计划 020 和文档索引，不改变活跃规则、脚本、测试、CI 或
   远程状态；
2. 规划 PR 经 GitHub 合入 `develop` 且最终远端 push `verify` 成功后，功能 PR 才实现
   `AGENTS.md` 规则、两个安全入口和自动测试；
3. 两个 PR 都必须遵守 generator/evaluator 同一冻结 SHA 验证、required `verify` 和
   GitHub-only 合并；功能 PR 合并后才使用新入口更新本地 `develop`。

本决策批准时，上述阶段只是待执行计划，不曾据此预写实现、测试、PR、CI、合并或远端
变化；当前实际边界见下节。

## 仓库内实施边界

2026-07-29，功能工作树已经实现 `AGENTS.md` 稳定规则、PowerShell/POSIX 安全更新入口及
`LocalDevelopUpdateTest`，执行计划 020 随功能 diff 归档。入口实现不读取 GitHub；调用者
仍必须先回读精确最终 `develop` SHA 的 push `verify` 成功记录，再把该 SHA 作为唯一参数。

本状态只说明仓库内实现已经形成，不表示独立 evaluator 已给出 `PASS`、required CI 已
成功、功能 PR 已创建或合并、远端 `develop` 已改变、本地 `develop` 已运行新入口更新，
也不表示发生部署。上述事实只能在实际发生后由对应权威记录证明。

## 回退

- 规划或功能 PR 合并前，可关闭对应 PR；远端 `develop` 和现有 Harness 行为不变；
- 功能合入后若入口错误拒绝合法纯快进，通过新的受保护 `codex/* → develop` Harness PR
  修复或 revert `AGENTS.md`、脚本和测试，不直接改写本地或远端 `develop`；
- 若本地 `develop` 已领先或分叉，安全入口必须停止。先保留提交并报告状态，由人类决定
  新建保全分支、重新 clone 或其他恢复方案；脚本不得自动 reset、丢弃或搬运提交；
- 回退不得关闭 PR/required `verify` 保护，不得使用本地 feature 合并、管理员绕过、
  强推或删除证据来恢复速度。
