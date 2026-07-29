# 执行计划 017：迁移 develop 集成与 main 生产发布分支模型

状态：已完成并归档（S6 模型闭环）

## 目标与权限边界

本计划执行
[决策卡 020](../../decisions/020-develop-production-branch-model.md)。目标是在始终保留一个
受保护默认分支的前提下，把日常集成迁移到 `develop`，把 `main` 保留为需要人工批准的
生产发布基线，并建立普通、release 和 hotfix 三类机械分支流。

规划 PR 只包含决策、计划和文档索引。规划合并前不得创建远程 `develop`，不得改变
GitHub 默认分支、Branch Protection、Ruleset、远程权限或 CI。实施阶段也只能执行本文
列出的远程变化；遇到能力、API 或仓库状态偏差必须停止，不能扩大权限或绕过保护。

## 全程不变量

1. 默认分支在每个可观察状态下都受 PR、严格 `verify`、管理员不可绕过、禁止强推和禁止
   删除的保护；
2. `main` 的保护在迁移期间保持有效，不先降级再迁移；
3. `develop` 未完成保护配置与 GitHub 回读前，绝不设为默认分支；
4. 所有长期分支变化经 PR；不直接推送、不强推、不使用管理员绕过；
5. `main` 是发布基线而非部署记录，任何文档和 PR 都不得声称当前存在真实部署；
6. GitHub API、PR 和 Actions 是远程事实权威；本地 refs 与历史文档不能替代回读；
7. 分支流检查失败时修复根因，不增加永久迁移白名单或关闭必需检查。

## 迁移状态机

| 状态 | 默认分支 | develop | main | 允许的下一步 |
|---|---|---|---|---|
| S0 当前 | `main` | 不假定存在 | 已保护 | 规划 PR 合入当前 `main` |
| S1 规划已合并 | `main` | 尚未创建或未启用 | 已保护 | 从精确最新 `main` 创建 `develop` |
| S2 develop 已创建 | `main` | 非默认，等待保护 | 已保护 | 只配置并回读 `develop` 保护 |
| S3 双分支已保护 | `main` | 已保护、非默认 | 已保护 | 把默认分支切换为 `develop` |
| S4 新模型入口就绪 | `develop` | 已保护 | 已保护 | 从 `develop` 建实施分支 |
| S5 检查进入 develop | `develop` | 已保护且含分支流检查 | 已保护 | 首次 `develop → main` release PR |
| S6 模型闭环 | `develop` | 已保护且已验证 | 已保护且含同一检查 | 正常开发、发布和 hotfix |

任何状态验证失败都停留在最近一个已验证状态；不得跳过状态、交换 S2/S3/S4 的顺序，或
让未保护 `develop` 成为默认分支。

## 已发生的远程事实与完成结果

2026-07-28，GitHub 权威记录已经确认：

- `develop` 从 `main` SHA `3620e6b2dfc12911c93075f126e09141a1623ed3` 精确创建；
- `develop` initial push `verify` run `#114` 成功；
- `develop` 和 `main` 的保护均为 require PR、required `verify`、strict/up-to-date、
  no bypass、禁止强推和禁止删除，approvals 未要求；
- 远端 `HEAD` 指向 `refs/heads/develop`。

以上事实使远程迁移达到 S4。随后发生的 GitHub 权威事实完成了 S4 → S6：

- [PR #33](https://github.com/weiran22222/study-track/pull/33) 实施分支流门禁，使检查进入
  `develop`，完成 S5；
- [PR #34](https://github.com/weiran22222/study-track/pull/34) 完成第一次
  `develop → main` release；
- `main` 合并提交 `280b74e5ca04f6e2a2b2e85c2a01882fe2d20e91` 的
  [push verify run #119](https://github.com/weiran22222/study-track/actions/runs/30372083458)
  成功；
- GitHub 默认分支仍为 `develop`，`develop` 与 `main` 均保持受保护并要求 required
  `verify`。

因此迁移状态已达到 S6，实施计划完成。仓库没有发生部署；第一次 release、`main`
合并提交与成功 CI 只证明发布基线和分支模型闭环，不是部署证据。

## 阶段 0：规划 PR 走当前 main 流程

### 前置

- 本地规划分支从干净、与 `origin/main` 同步的 `main` 建立；
- 只包含决策 020、计划 017 和文档索引；
- 当前 `main` 仍是默认与受保护分支；该事实在实施前由 GitHub 回读确认。

### 操作

1. 本地运行 DocumentationNavigationTest、完整 JDK 21 `verify` 和差异检查；
2. 按当前流程创建 `codex/* → main` 规划 PR；
3. 等待精确 head SHA 的必需 `verify` 成功并保持与最新 `main` 同步；
4. 不使用管理员绕过，正常合并规划 PR。

### 验证

- GitHub PR 显示 base 为 `main`，规划 diff 没有实现或远程配置变化；
- GitHub Actions 记录该 head SHA 的 `verify` 成功；
- 合并后 GitHub `main` ref 与 push 事件的最终 `verify` 成功。

### 回滚

- PR 合并前可关闭规划 PR，远程分支模型保持 S0；
- 若规划 diff 扩大到实现或远程状态，撤回额外内容并重新审查；
- 规划合并后不回写历史；后续迁移若取消，以新决策取代本计划。

## 阶段 1：实施前远程盘点与冻结

### 前置

- 规划 PR 已合并，且精确最新 `main` SHA 的最终 `verify` 成功；
- 获得执行远程分支与保护变化所需的既有权限，但不扩大权限；
- 没有正在合并的 PR 或自动化会与默认分支切换竞争。

### 操作

1. 通过 GitHub 回读仓库默认分支、`main` ref、`main` Branch Protection、必需检查和
   Actions 状态，记录精确 `main` SHA `M`；
2. 枚举所有开放 PR 及其 base。若仍有指向 `main` 的非规划 PR，停止并由人类决定是先
   完成、关闭还是在 `develop` 受保护后重定向；
3. 回读 `develop` ref 是否已存在。不得假定本地缺失等于远端缺失；
4. 保存 `main` 保护和默认分支的回读结果，作为后续逐步比对与回滚基线。

### 验证

- GitHub 报告默认分支仍为受保护 `main`；
- `M` 对应成功的最终 `verify`；
- 开放 PR 已无迁移歧义；
- 若远端已有 `develop`，其 SHA、历史、保护和所有权已由人类审查。

### 回滚

- 本阶段只读；任何不符合预期的事实都停止在 S1；
- 不删除、覆盖或强推意外存在的 `develop`，必须另行获得人类决定。

## 阶段 2：从精确最新 main 创建 develop

### 前置

- 阶段 1 全部通过，`M` 未因新的 `main` 合并而失效；
- 再次回读 `main` ref 等于 `M`；
- GitHub 确认目标 `develop` ref 不存在，或人类已另行批准现有分支处理方案。

### 操作

- 使用 GitHub 的 ref 创建能力，把 `refs/heads/develop` 精确创建在 `M`；
- 不从可能陈旧的本地分支推导起点，不提交额外内容。

### 验证

- GitHub 回读 `develop` ref 精确等于 `M`；
- 默认分支仍为 `main`；
- `main` 保护未改变。

### 回滚

- 创建失败时保持 S1，不重试为不同 SHA；
- 创建成功但后续保护失败时，不把 `develop` 设为默认、不向其合并工作，也不删除或强推
  该分支；保留 `main` 为受保护默认分支，修正保护方案后从 S2 继续。

## 阶段 3：先保护 develop，再切换默认分支

### 前置

- GitHub 回读 `develop == M`，默认分支为受保护 `main`；
- 已从 GitHub 回读当前 `main` 保护的真实参数，检查名仍为 `verify`。

### 操作

1. 为 `develop` 配置 PR 必需、required status check `verify`、strict/up-to-date、
   enforce administrators、禁止强推和禁止删除；
2. 逐字段回读 `develop` 保护，不以 API 写入成功响应代替回读；
3. 再次确认 `main` 保护仍满足同样的不变量；
4. 只有以上全部通过，才把 GitHub 默认分支设置为 `develop`；
5. 回读默认分支与两条保护，确认进入 S4。

### 验证

- 默认切换前，`develop` 已受完整保护；
- 切换后 GitHub 报告 `default_branch=develop`；
- `develop` 和 `main` 均要求 PR、严格 `verify`、管理员不可绕过，且禁强推、禁删除；
- 新 PR 的默认 base 是 `develop`，但不以 UI 展示替代 API 回读。

### 回滚

- `develop` 保护写入或回读失败：停留在 S2，`main` 继续作为受保护默认分支；
- 默认分支写入失败：停留在 S3，不降低任何保护；
- 默认切换后发现严重问题：只有先回读确认 `main` 保护完整，才可把默认分支恢复为
  `main`；不得通过解除 `develop` 或 `main` 保护恢复服务。

## 阶段 4：从 develop 实施机械分支流检查

### 前置

- 已进入 S4，GitHub 确认 `develop` 为受保护默认分支，`main` 仍受保护；
- 实施分支从精确最新、`verify` 成功的 `develop` SHA 建立；
- 实施不改变产品、Java 架构、远程权限或保护参数。

### 分支流检查契约

仅在 `pull_request` 事件使用 GitHub 事件提供的 base/head ref：

- `base=develop` 时，普通 head 必须是 `codex/*`；
- hotfix 合入 `main` 后，允许 `head=main, base=develop` 作为明确的回流路径；
- `base=main` 时，只允许 `head=develop` 的 release PR 或 `head=hotfix/*` 的紧急修复；
- 其他 base/head 组合失败，并按 `AGENTS.md` 输出位置、不变量、原因、修复方向、精确
  复验命令和权威文档；
- 检查只判断 PR 拓扑。分支是否与最新 base 同步继续由 strict required check 保护；
- 检查必须位于现有 required `verify` Job 内，不能新建一个未被保护规则要求的旁路 Job。

### CI 事件语义

- `pull_request`：运行分支流检查、既有完整 PR diff 检查、环境自检和 Maven `verify`；
- `push`：没有可信 PR base/head，不运行分支流或 PR diff 检查，但继续运行环境自检和
  Maven `verify`；
- 两类事件继续使用同名 `verify` Job，不能改变保护规则依赖的检查身份；
- 自动测试使用静态契约与本地参数化场景覆盖允许和拒绝组合，不伪造远程 CI 已通过。

### 自举与迁移例外

按本计划顺序，实施 PR 自然是 `codex/* → develop`，已经属于目标模型允许的普通路径，
因此不需要迁移例外。规划 PR 在检查出现前已按旧模型合入 `main`，也不需要追溯例外。

如果 GitHub 的真实事件或保护语义显示实施 PR 无法在无例外的情况下进入 `develop`，
必须停止并重新审查，优先调整实施顺序。只有获得新的明确批准后，才可设计绑定精确 PR
编号和精确 head SHA、且在第一次 release PR 前删除的一次性例外。禁止分支名通配、
管理员绕过、关闭 required check，或把迁移例外永久留在 `develop`/`main`。

### 验证与回滚

- 本地运行允许/拒绝矩阵、相关静态测试和完整 `verify`；
- 实施 PR 必须明确显示 `base=develop, head=codex/*`，远程 `verify` 日志必须证明分支流
  步骤实际执行并通过；
- 实施 PR 失败时只在实施分支修复，不直推 `develop`；
- 若检查错误阻止合法流，保持现有保护，通过新的 `codex/* → develop` PR 修复；不绕过。

## 阶段 5：实施 PR 合入 develop

### 前置

- 实施 PR 的精确 head SHA 已通过 required `verify`；
- PR 与最新 `develop` 同步；
- diff 只包含批准的分支模型实现、测试、当前文档和计划的实际进度。

### 操作

- 不使用管理员绕过，正常合入 `develop`；
- 观察合并后 `develop` push 事件的环境自检和 Maven `verify`。

### 验证

- GitHub `develop` ref 指向合并结果；
- push `verify` 对精确 `develop` SHA 成功；
- GitHub 回读 `develop` 保护与默认分支未漂移；
- 分支流检查和测试现已存在于 `develop`。

### 回滚

- 合并前可修复或关闭实施 PR；
- 合并后若代码有误，通过受保护 `codex/* → develop` 修复或 revert PR；
- 不直接改写 `develop`，不把默认分支切到未确认保护的分支。

## 阶段 6：第一次 develop → main release PR 的自举验证

第一次 release PR 会把分支流检查本身从 `develop` 带入尚未包含该检查的 `main`。这是
迁移的关键自举边界，不能仅依赖本地测试或假定 GitHub 会采用预期 workflow 版本。

### 前置

- 阶段 5 的 `develop` push `verify` 成功；
- `develop` 与 `main` 都保持完整保护；
- 人类明确批准本次 release PR，且 PR 说明 `main` 只是发布基线、没有部署；
- release PR 的 base 精确为 `main`，head 精确为 `develop`。

### 操作

1. 创建 `develop → main` release PR；
2. 不合并，先检查 GitHub Actions 对该 PR 精确 head SHA 的日志；
3. 确认 required `verify` Job 中分支流步骤实际出现，并读取事件为
   `base=main, head=develop` 后通过；
4. 确认既有 PR diff 检查、环境自检和 Maven `verify` 同样通过；
5. 确认 PR 与最新 `main` 同步，并再次取得人工发布批准；
6. 不使用管理员绕过，正常合并。

### 验证

- Actions 日志而非推测证明首次 release PR 执行了随 PR 引入的检查；
- required `verify` 绑定 release PR 精确 head SHA 并成功；
- GitHub 合并记录显示唯一方向为 `develop → main`；
- 合并后 `main` push 事件的完整 `verify` 对最终 SHA 成功；
- `main` 现包含与 `develop` 相同的分支流检查，两条保护和默认分支均无漂移；
- 记录“发布基线已更新”，不得记录“已部署”。

### 回滚

- 若 PR workflow 没有实际执行新检查，禁止合并；先根据 GitHub 真实语义调整计划或实现；
- 若检查执行但错误拒绝合法 release，回到 `develop` 通过受保护 PR 修复，再重建或更新
  release PR；
- 合并后若需撤回发布基线，使用新的、人工批准的受保护 PR；禁止 reset、强推或绕过。

## 阶段 7：常规开发与 release 流程

### 普通变更

1. 从精确最新且 `verify` 成功的 `develop` 创建 `codex/*`；
2. 本地验证后创建 `codex/* → develop` PR；
3. required `verify`、分支流检查和严格同步全部通过后合并；
4. 检查合并后 `develop` push `verify`。

### 正常发布

1. 人类审查 `develop` 累积差异并明确批准发布；
2. 创建 `develop → main` release PR，在 PR 中记录批准与发布基线范围；
3. required `verify` 和严格同步通过后合并；
4. 检查最终 `main` push `verify`；
5. 除非未来另有部署系统与证据，不得声称已部署。

## 阶段 8：hotfix 与 develop 回流

### 前置

- 人类确认问题需要直接修复当前 `main` 发布基线；
- `hotfix/*` 从精确最新且 `verify` 成功的 `main` SHA 建立；
- `main` 和 `develop` 保护保持完整。

### 操作

1. 创建 `hotfix/* → main` PR；
2. required `verify`、分支流检查、严格同步与人工批准全部通过后合并；
3. 检查最终 `main` push `verify`；
4. 随即创建 `main → develop` 回流 PR，不直接推送或手工复制提交；
5. 解决与 `develop` 的冲突，并等待 required `verify`、回流分支流规则与严格同步通过；
6. 合并后检查最终 `develop` push `verify`。

### 验证

- GitHub PR 记录证明 hotfix 源自/指向 `main`，随后有明确 `main → develop` 回流；
- 两个合并结果的 push `verify` 均成功；
- 回流完成前不得把 hotfix 标记为完整关闭；
- 默认分支仍为 `develop`，两条保护无漂移。

### 回滚

- hotfix PR 合并前可修复或关闭，不改变长期分支；
- hotfix 已进 `main` 但回流冲突时，保留保护并在回流 PR 中解决，不能跳过回流；
- 若机械规则无法表达必要回流，停止并通过新的 Harness 决策修正规则，不使用绕过。

## 阶段证据、计划归档与最终检查

- 每次远程写入前后保存 GitHub API 回读的 ref、default branch 和保护摘要；
- PR 与 Actions 页面是 base/head、required check、合并和最终 push CI 的权威记录；
- 仓库内只记录实际发生的本地命令、SHA、偏差和未完成风险，不预测远程结果；
- 实施 PR 可以更新本计划的实际进度并在完成后移入 `exec-plans/completed/`；
- 只有发生部署/迁移外部状态偏差、必须记录合并后事实或仍有风险时，才创建独立收尾 PR；
- 完成定义已经满足：
  - GitHub 默认分支为 `develop`，且 `develop`、`main` 的保护回读均要求 required
    `verify`；
  - [PR #33](https://github.com/weiran22222/study-track/pull/33) 完成实施，分支流门禁及其
    普通、release、hotfix 和 hotfix 回流机械测试进入 `develop`，没有永久迁移绕过；
  - [PR #34](https://github.com/weiran22222/study-track/pull/34) 完成第一次 release
    自举验证；
  - 最终 `main` SHA `280b74e5ca04f6e2a2b2e85c2a01882fe2d20e91` 的
    [push verify run #119](https://github.com/weiran22222/study-track/actions/runs/30372083458)
    成功；
  - 没有部署，完成状态只表示分支模型与发布基线闭环。

## 停止条件

出现以下任一情况立即停止，不继续远程变化：

- GitHub 回读与预期状态不一致，或无法确认精确 ref/保护/default；
- `verify` 检查名或事件语义与计划不同；
- 无法在保护 `develop` 后再切换默认分支；
- 需要临时解除 PR、严格检查、管理员执行、禁强推或禁删除；
- 需要永久迁移白名单、直接推送、强推或管理员绕过；
- 需要新增远程权限、付费能力、部署系统或超出决策 020 的分支类型；
- 实际范围扩大到产品、架构、数据格式或未经批准的 CI 语义。
