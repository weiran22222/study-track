# 决策卡 016：为 CI 增加 PR 完整差异空白门禁

状态：已批准

日期：2026-07-28

## 问题

决策卡 015 建立了提交前 `git diff --cached --check` 约定，但该检查依赖提交者在本地正确
执行，现有 GitHub Actions `verify` Job 只检查环境并运行 Maven `verify`。因此，受保护
`main` 的必需检查尚未机械验证 PR 相对目标分支的完整最终差异是否包含 Git 能识别的
空白错误。

这不是产品行为或 Java 架构问题，而是合并验收门禁的缺口。门禁必须检查 PR 的最终
`base...head` 差异，而不是只检查最后一个提交、工作树或某一次暂存区。

## 候选方案与取舍

### A. 在 GitHub Actions workflow 中内联 `git diff --check`

- 收益：文件改动最少，能够直接使用 PR 上下文中的 base/head SHA；
- 成本：诊断、参数处理和本地复验逻辑会埋在 YAML 中，六要素失败反馈难以独立测试和
  复用。

### B. 使用仓库 POSIX 脚本，并只在 PR 事件中由现有 `verify` Job 调用

- 收益：把 `base...head` 范围和失败反馈集中为可审查、可本地复验的仓库契约；workflow
  只负责提供可信的 PR base/head SHA；仍由现有受保护 `verify` Job 承担门禁；
- 成本：需要维护一个小型 shell 脚本、完整 checkout 历史和相应静态契约测试。

选择本方案。

### C. 把差异检查绑定到 Maven 生命周期

- 收益：表面上继续只有一个本地命令；
- 成本：Maven 本身没有 PR base/head 上下文，会把 Git 与 POSIX shell 环境耦合进所有
  本地构建，并可能使 Windows `verify` 依赖系统 `sh`。这会扩大工具链影响，不采用。

## 人类决定

学习者于 2026-07-28 明确批准：把“检查 PR 完整差异中的 Git 空白错误”加入现有受保护
`main` 的必需 `verify` Job，采用仓库 POSIX 脚本与 GitHub Actions PR-only 调用，并使用
PR 的 `base...head` 范围。

## 决定

- checkout 获取完整历史，使 PR base 与 head 及其合并基点可用于三点差异；
- 新增仓库 POSIX 脚本，接受 base SHA 与 head SHA，并执行
  `git diff --check "$base...$head"`；
- workflow 仅在 `pull_request` 事件调用该脚本，并从事件上下文传入 base/head SHA；
- `push` 事件继续运行既有环境自检和 Maven `verify`，不运行没有 PR 语义的差异门禁；
- 脚本失败时保留 Git 的具体位置诊断，并按 `AGENTS.md` 要求补全被违反的不变量、原因、
  修复方向、复验命令和权威文档链接；
- 用静态机械契约测试保护 workflow、脚本和范围接线；Windows Maven `verify` 不执行
  POSIX 脚本，也不依赖系统 `sh`；
- 通过功能 PR 的临时负向探针实际观察门禁先失败、修复后通过，再检查最终 `main` 的
  `verify`。

## 风险等级

这是第三级 Harness 变更。虽然不改变产品、架构分层、依赖或远程权限，但它改变现有
受保护 `main` 的必需 CI/验收门禁语义：此前 Maven `verify` 成功即可满足该 Job，本决定
增加了 PR 完整差异必须无 Git 空白错误的前置条件。人类已明确批准，按“规划 PR → 功能
PR”保存决策、计划和阶段证据。

## 不变边界

- 不改变 `SPEC.md`、产品行为、Java 分层、依赖、JSON 格式或数据；
- 不增加或修改 GitHub 权限、分支保护规则、Job 名称或新的外部服务；
- 不把差异检查扩展为格式化器、lint、秘密扫描或其他未批准门禁；
- 不让 `push` 事件伪造 PR 范围，也不削弱其既有环境自检与 Maven `verify`；
- 不以浅历史、最后一个提交或暂存区替代 PR 的完整 `base...head` 差异；
- 如果实施需要改变上述范围、远程权限或受保护检查身份，停止并重新获得人类决定。

## 验收

- 规划 PR 只包含本决策、执行计划、本地 POC 证据和文档索引；
- 功能 PR 按执行计划实现脚本、workflow 接线、当前验证地图和机械契约测试；
- 本地测试与 JDK 21 完整 `verify` 通过，且 Windows `verify` 不依赖系统 `sh`；
- 功能 PR 的临时空白错误使必需 `verify` 按预期失败，并给出完整诊断；
- 删除临时错误后，同一 PR 的 `verify` 成功；
- 功能合并后最终 `main` 的远程 `verify` 成功。
