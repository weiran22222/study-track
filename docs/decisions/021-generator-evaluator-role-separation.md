# 决策卡 021：分离实现 generator 与独立验证 evaluator

状态：已批准并实施；分支/worktree 与交接细节已由决策卡 022 部分取代

日期：2026-07-29

## 问题

当前标准工作流允许同一个实现智能体编写代码、运行测试并把自检结果作为主要本地完成依据。
主智能体可以复验，远端 `verify` 也会机械检查提交，但 Harness 尚未要求“实现者”和
“独立验证者”必须是两个不同的子智能体，也没有把独立验证绑定到不可变的精确提交。

这会让实现假设、测试盲点和完成判断集中在同一上下文中。把最终本地验收交给不同子智能体
会改变智能体权限、交接协议和完成定义，因此属于第三级 Harness 变更。

## 候选方案与取舍

### A. 保持实现者自检与主智能体复验

- 收益：流程最短，不增加角色和交接工件；
- 成本：没有稳定的实现者与验证者身份分离，验证容易继承实现上下文和假设。

### B. 串行共享工作树上的 generator/evaluator 分离

- 收益：先冻结精确提交，再让无父对话上下文的不同子智能体只读验证；不需要引入新的
  worktree 生命周期或远程服务；
- 成本：增加一次交接、独立验证和失败回流，身份分离主要依赖协调协议与审计证据。

选择本方案作为第一版。

### C. 两个隔离 worktree 并行工作或新增独立 required check

- 收益：文件系统隔离更强，远端状态看起来更显式；
- 成本：验证对象在实现完成前仍会变化，并行没有必要；仓库和现有 CI 也无法认证两个
  Codex 子智能体的真实身份。给普通 CI Job 命名为 `independent-verification` 会制造
  无法兑现的身份保证。

本次不选择。未来若确有隔离需求，只能使用 Codex-managed Worktree/Handoff，并另行评估
可认证的远端证明机制。

## 人类批准与已发生事实

学习者于 2026-07-29 明确批准本卡方案、规划 PR #35 合并及按执行计划 018 实施。
[规划 PR #35](https://github.com/weiran22222/study-track/pull/35) 随后已经合并；
合并后的 `develop` push
[`verify` run 30379095072](https://github.com/weiran22222/study-track/actions/runs/30379095072)
已经成功。

实施过程随后冻结第一版 Subject SHA
`a551365b9a9d8a7cdc8598a274dd23df48e3ca30`；不同 evaluator 发现 PowerShell guard
的分支比较不区分大小写并给出 `FAIL`。该报告回流同一 generator 后，修复提交
`063392c9c77f64445ea725e85fdec6c2c16dfbdd` 改为精确区分大小写并增加回归场景；旧报告
随新 SHA 失效，同一 evaluator 对新 SHA 完整复验后给出 `PASS`，相关测试 16/16、完整
测试 122/122。实施
[PR #36](https://github.com/weiran22222/study-track/pull/36) 的 head 与 evaluator 报告及
required `verify` 均绑定该新 SHA；PR 评论保留第一次 `FAIL` 与修复后 `PASS`。学习者明确
批准合并后，PR #36 合入 `develop`，合并提交为
`75c87f1620985c8a4af6981439345fd176eda2e2`，最终 `develop` push
[`verify` run 30382996038](https://github.com/weiran22222/study-track/actions/runs/30382996038)
成功。`main` 未改变，也没有发生部署。

这些记录证明了精确 SHA、失败回流、同 SHA required `verify` 和合并后验证闭环；它们不
证明 generator/evaluator 的密码学身份或平台级身份认证。

批准的最小协议为：

- generator 可以修改代码、测试和文档并运行自检，但无权给出最终 `PASS`；
- evaluator 必须是不同子智能体，以无父对话上下文启动，只依赖仓库、handoff manifest
  和精确 Subject SHA，只读给出 `PASS`、`FAIL` 或 `INCONCLUSIVE`；
- 主智能体/协调者管理分支、冻结提交、检查交接状态、协调失败回流并持久化验证报告；
- 人类继续保留产品目标、重大 Harness 变更和发布决定。

## 决定

### 分支与第一版工作树模型

主智能体/协调者从精确 `origin/develop` 创建实际开发分支，分支名继续使用
`codex/*`。generator 和 evaluator 都不得创建或切换分支。

第一版使用串行共享工作树/Handoff：

1. generator 完成实现与自检后停止；
2. 主智能体审查、提交并冻结精确 Subject SHA；
3. 主智能体确认分支、`HEAD`、工作树和暂存区满足交接条件后，evaluator 才开始；
4. evaluator 结束后再次检查相同条件，确认验证没有改变被验证对象。

所有智能体继续遵守决策卡 019：不得手工执行 `git worktree add`、`remove` 或 `prune`。
未来若升级为隔离验证，只能使用 Codex-managed Worktree/Handoff；evaluator 应在
detached/fixed SHA 上验证，不能让两个 worktree 同时检出同一个可变分支。

### 角色权限

| 角色 | 必须负责 | 禁止 |
|---|---|---|
| generator | 实现批准范围，增加测试，运行自检，报告修改和已执行结果 | 创建/切换分支，给最终 `PASS`，替代 evaluator，提交、推送、操作 GitHub |
| evaluator | 对精确 SHA 做规格审查、机械验证和独立场景，输出统一报告 | 修改文件、stage、commit、push、切换分支、管理 worktree、操作 GitHub、修复发现 |
| 主智能体/协调者 | 创建分支，审查和提交，冻结 SHA，检查交接条件，保存报告，协调 FAIL 回流 | 用自身判断替代独立 evaluator，复用已失效结论，绕过 PR 或 required `verify` |
| 人类 | 决定目标、验收标准、重大 Harness 变化和是否发布 | 不变 |

generator 的测试结果是自检，不是最终独立验收。evaluator 的 `PASS` 也不替代现有
required `verify`：两者必须针对同一 Subject SHA 成立，PR 才可进入合并判断。

### 状态机与结论失效

```text
SPEC_READY -> IMPLEMENTING -> FROZEN(<Subject SHA>) -> VERIFYING
                                                        |-> PASS
                                                        |-> FAIL -> IMPLEMENTING
                                                        `-> INCONCLUSIVE
```

- 只有主智能体可以把已审查提交标记为 `FROZEN(<Subject SHA>)`；
- `FAIL` 只把报告交回 generator；evaluator 不直接修复；
- 修复产生任何新 SHA 后，旧报告和旧结论立即失效，必须重新交接和独立验证；
- `INCONCLUSIVE` 表示证据不足或验证环境不可信，不能视为通过或允许合并；
- `PASS` 只覆盖报告中的精确 Subject SHA、规格/AC、命令和证据边界。

### Handoff 与验证报告

主智能体交给 evaluator 的 manifest 至少包含：

```text
Task:
Repository:
Source branch:
Expected base ref and SHA:
Subject SHA:
Generator:
Evaluator:
Specification / acceptance criteria:
Working-tree mode: serial shared | managed detached
Mutation allowed: no
```

evaluator 的统一报告至少包含：

```text
Subject SHA:
Generator:
Evaluator:
Specification / acceptance criteria:
Commands executed:
Independent scenarios:
Findings:
Residual gaps:
Verdict: PASS | FAIL | INCONCLUSIVE
```

报告不得由 evaluator 写入被验证提交，因为保存报告会改变 SHA 并使结论失效。主智能体
在 PR 创建后把报告持久化到 PR 评论，并确认评论中的 Subject SHA 与 PR head SHA 相同。
PR、评论、Check Run 和 Actions 是远端审计证据的权威来源。

### 机械边界与身份边界

实施已经提供一个最小的验证对象检查入口，在 evaluator 开始前和结束后检查：

- 当前分支符合 manifest，或在受管隔离模式下为明确的 detached 状态；
- `HEAD` 精确等于 Subject SHA；
- 工作树干净；
- 暂存区为空。

脚本只能证明 Git 对象和工作区未变，不能证明两个 Codex 身份不同。仓库/CI 目前没有可信
机制机械认证 generator 与 evaluator 身份；第一版由主智能体记录不同的子智能体标识、
handoff manifest 和 PR 评论来提供可审计证据。不得新增一个无法认证身份的
`independent-verification` required check。

## 风险与回退

- 协调者可能错误复用旧报告：所有报告必须显示 Subject SHA，新 SHA 强制重新验证；
- evaluator 可能意外写入：前后检查失败时结论自动降为 `INCONCLUSIVE`，先恢复干净状态，
  再由新的受控交接验证；
- 身份证据可被流程误报：不得把协调记录描述为密码学或平台级认证；
- 串行流程增加等待时间：这是第一版有意接受的成本，不用手工 worktree 优化；
- 若角色边界导致无法完成合法任务，回退只能通过新的受保护 Harness 变更恢复旧流程；
  不得在单个任务中静默跳过 evaluator。

## 非目标

- 不改变产品行为、Java 架构、数据格式、构建工具链或 GitHub 权限；
- 不改变 `develop`/`main` 分支模型或现有 required `verify` 的名称与语义；
- 不引入真实部署、外部身份服务、审批人数或新的 required check；
- 不允许 generator、evaluator 或主智能体手工管理临时 worktree；
- 不在本规划 PR 实施 `AGENTS.md`、脚本、测试或 CI 变更。

具体实施、试运行、停止条件和验收证据见已归档的
[执行计划 018](../exec-plans/completed/018-generator-evaluator-role-separation.md)。
