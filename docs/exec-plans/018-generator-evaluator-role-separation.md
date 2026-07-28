# 执行计划 018：实施 generator/evaluator 职责分离

状态：实施中，generator 本地实现与自检完成，待冻结与独立验证

## 目标与权限边界

本计划执行
[决策卡 021](../decisions/021-generator-evaluator-role-separation.md)。目标是把同一任务的
实现和最终本地验证强制交给不同子智能体，并把 evaluator 结论绑定到精确、不可变的提交。

这是第三级 Harness 变更。学习者已于 2026-07-29 明确批准决策卡 021、规划 PR #35
合并及按本计划实施；规划 PR 已合并，合并后的 `develop` push `verify` run
30379095072 已成功。当前实施阶段可以更新 `AGENTS.md`、两个 guard、对应测试和本计划
事实，但不得改变 CI 身份、产品、架构、数据格式、远程权限或决策卡 020/计划 017。

规划 PR 与实施 PR 都必须实际试运行本协议；不得把尚未发生的 evaluator 结论、实施 PR、
远端 CI、合并或最终 `develop` 验证写成已经发生。

## 已知前置与独立待办

- 实施基线必须是 GitHub `origin/develop` 的精确、成功验证提交，实际工作分支仍为
  `codex/*`；
- 本次实施分支从 `origin/develop` 的
  `bb1d4af4bca0f1bf449d00095866dadd91737aec` 创建；generator 开始时已确认本地
  `HEAD` 与该 base 相同；
- 决策卡 020 与计划 017 的状态陈述已经落后于实际发布闭环。这是独立文档收尾待办，
  不是本计划范围；本计划不得顺手修复，也不得把历史状态文字当作当前远端事实；
- 远端分支、PR、评论、保护和 Actions 状态必须从 GitHub 权威记录回读，不能由计划推断。

## 全程不变量

1. 分支只由主智能体/协调者从精确 `origin/develop` 创建；generator/evaluator 不创建或
   切换分支；
2. 第一版串行复用共享工作树：generator 停止、主智能体冻结提交后，evaluator 才开始；
3. generator 与 evaluator 必须是不同子智能体；evaluator 以无父对话上下文启动；
4. evaluator 只依赖仓库、handoff manifest 和 Subject SHA，且不修改文件、暂存区、
   Git 历史、分支、worktree 或 GitHub；
5. evaluator 前后都机械确认 branch/HEAD/Subject SHA、干净工作树和空暂存区；
6. 任意新提交使旧 evaluator 结论失效；`FAIL` 必须回 generator，`INCONCLUSIVE`
   不得合并；
7. evaluator `PASS` 与现有 required `verify` 都必须绑定 PR 的同一 head SHA；
8. 不手工运行 `git worktree add/remove/prune`；需要隔离时只能另行批准
   Codex-managed Worktree/Handoff；
9. 不创建无法认证不同智能体身份的 `independent-verification` required check；
10. 不把未发生的本地检查、PR 评论、CI、合并或最终 `develop` 验证写成事实。

## 角色和交接契约

### 主智能体/协调者

- 回读并记录精确 `origin/develop` SHA；
- 从该 SHA 创建 `codex/*`，随后把仓库路径、分支、base SHA 和批准范围交给 generator；
- generator 停止后审查差异，只暂存预期文件，提交并记录 Subject SHA；
- 在 evaluator 开始前确认当前分支、`HEAD`、工作树和暂存区；
- 以不继承父对话的方式启动一个不同 evaluator，并传递完整 handoff manifest；
- evaluator 结束后复查 Git 状态，处理 verdict，把报告写入 PR 评论；
- `FAIL` 时只把报告交回 generator；修复后提交新 SHA 并从冻结步骤重新开始。

### Generator

- 只实现批准的代码、测试和文档范围；
- 可以运行快速测试和完整 `verify`，但只能报告为 generator 自检；
- 完成后停止写入并交还修改摘要、命令结果、风险和未决项；
- 不创建或切换分支，不 stage/commit/push，不操作 GitHub，不给最终 `PASS`。

### Evaluator

- 必须与 generator 是不同子智能体，且启动时不继承父对话；
- 先读取仓库权威文档和 manifest，再独立选择规格场景与验证命令；
- 开始前和结束后运行验证对象检查；
- 只读审查、运行不会持久修改仓库的验证命令并输出统一报告；
- 不修改、stage、commit、push、切换分支、管理 worktree、操作 GitHub或修复发现。

如果验证工具会生成受忽略的构建产物，evaluator 必须在报告中声明；任何 tracked 或
untracked 工作树变化、暂存区变化、`HEAD`/分支变化都使本轮结果为 `INCONCLUSIVE`。

## Handoff manifest

主智能体必须在委派消息中直接提供下列字段，不把父对话当作隐式输入：

```text
Task:
Repository:
Source branch:
Expected base ref:
Expected base SHA:
Subject SHA:
Generator agent/task id:
Evaluator agent/task id:
Specification / acceptance criteria:
Required repository documents:
Working-tree mode: serial shared
Mutation allowed: no
Required pre/post guard:
```

若未来经新决策启用受管隔离，`Working-tree mode` 可改为 `managed detached`，evaluator
必须验证 fixed/detached Subject SHA。不得让 generator 和 evaluator 的两个 worktree
同时检出同一 `codex/*` 分支。

## 状态机

| 状态 | 所有者与入口条件 | 允许的下一步 |
|---|---|---|
| `SPEC_READY` | 人类已批准目标与 AC，主智能体已固定 base | 主智能体创建分支并委派 generator |
| `IMPLEMENTING` | generator 独占可写阶段 | generator 停止并交还，主智能体审查 |
| `FROZEN(SHA)` | 主智能体已提交且交接 Git 状态满足不变量 | 启动不同 evaluator |
| `VERIFYING` | evaluator 已通过前置 guard，只读验证 | `PASS`、`FAIL` 或 `INCONCLUSIVE` |
| `PASS` | 报告完整且后置 guard 通过 | 同 SHA 推送、PR、required `verify` 与人类合并判断 |
| `FAIL` | 发现可复现的规格、实现或 Harness 缺陷 | evaluator 停止，主智能体把报告交回 generator |
| `INCONCLUSIVE` | 环境、证据或只读边界不足 | 不合并；修复交接/环境后重新冻结和验证 |

`FAIL -> IMPLEMENTING` 后任何修复都必须形成新提交；新 Subject SHA 自动废止旧报告。
远端 rebase、merge、amend 或其他 head SHA 变化同样要求重新独立验证。

## 最小机械入口与静态契约测试

实施阶段新增跨平台薄入口：

- Windows：`scripts/check-verification-subject.ps1`；
- macOS/Linux：`scripts/check-verification-subject.sh`。

两个入口接收 `expected SHA` 与 `expected branch`（受管隔离模式可显式要求 detached），
并以非零退出码拒绝：

1. 当前分支与 manifest 不同；
2. `git rev-parse HEAD` 不等于 Subject SHA；
3. `git status --porcelain` 非空；
4. `git diff --cached --quiet` 失败。

错误必须满足 `AGENTS.md` 六字段反馈要求：位置、不变量、原因、修复方向、复验命令和权威
文档。入口不得 checkout、reset、clean、stash 或以任何方式“修复”工作树。

新增静态契约测试，至少检查：

- 两个脚本入口存在并由稳定导航引用；
- 同一组 SHA、branch、clean/index 不变量出现在两个入口；
- 两个入口不包含 checkout/switch/reset/clean/stash/worktree 或远程写入命令；
- 错误反馈包含六字段；
- `AGENTS.md` 的标准工作流、完成定义和临时工作树规则引用决策卡 021 的角色分离边界；
- `.github/workflows/verify.yml` 的 required Job 身份仍为 `verify`，没有伪造
  `independent-verification` Job。

测试只验证静态契约和可重复的本地 Git guard 场景，不声称能认证两个子智能体身份。

## 阶段 0：规划 PR 试运行

规划 PR #35 已按人类授权合并，合并后的 `develop` push `verify` run 30379095072 已成功。
远端 PR 评论、Check Run 和 Actions 保留该阶段的详细权威证据；本计划不复制报告全文。

本规划 PR 本身必须按新协议试运行，以尽早暴露交接缺口：

1. 主智能体从精确 `origin/develop` 创建 `codex/*` 规划分支，记录 base SHA；
2. generator 只生成决策卡 021、计划 018 和索引，运行文档导航测试与差异检查，不给
   最终 `PASS`；
3. generator 停止后，主智能体审查、暂存、提交并冻结规划 Subject SHA；
4. 主智能体确认分支、SHA、干净工作树和空暂存区；
5. 一个不同且无父对话上下文的 evaluator 只读验证 Subject SHA，输出统一报告；
6. `FAIL` 回原 generator 修复；主智能体提交新 SHA，旧报告失效并重新验证；
7. `PASS` 后才以同一 SHA 推送并创建 `codex/* → develop` 规划 PR；
8. 主智能体把报告写入 PR 评论，确认评论 Subject SHA 等于 PR head SHA；
9. 等待现有 required `verify` 对同一 SHA 成功，然后停止并把决策卡、diff、evaluator
   报告和 CI 结果交给人类；
10. 只有人类明确批准决策卡 021 并授权合并与实施后，才可正常合并规划 PR；未获批准时
    保持 PR 未合并或关闭，不进入阶段 1。

本阶段尚未实现 guard 脚本，因此主智能体和 evaluator 使用等价的只读 Git 命令完成前后
检查，并在报告中明确这是规划阶段的协议试运行，不声称脚本已经存在。

## 阶段 1：实施角色协议和机械 guard

人类明确批准决策卡 021、授权合并与实施，且规划 PR 已合入并确认 `develop` 最终
`verify` 成功后：

1. 主智能体从新的精确 `origin/develop` 创建实施 `codex/*` 分支；
2. 不继承父对话的 generator 更新 `AGENTS.md`、两个 guard 脚本和静态契约测试；
3. `AGENTS.md` 明确角色权限、状态机、handoff/report 格式、SHA 失效规则，以及 evaluator
   与 required `verify` 缺一不可；
4. 保持 `.github/workflows/verify.yml`、required check 身份、产品、架构和远程权限不变；
5. generator 运行相关测试和完整 JDK 21 `verify`，只作为自检报告；
6. 若实施发现需要 CI 身份服务、新 required check、手工 worktree 或远程权限，立即停止。

实施 PR 在冻结 Subject SHA 前，可根据已发生的本地事实更新并归档本计划，但不得写入尚未
发生的 evaluator 结论、PR、CI、合并或最终 `develop` 结果。远端证据保留在 PR 评论、
Check Run 和 Actions，不为复制结果而产生一个会使验证失效的新提交。

## 阶段 2：冻结、独立验证与 FAIL 回流

1. generator 停止后，主智能体审查 diff、测试和权限边界；
2. 主智能体只暂存预期文件，执行 cached diff 检查并提交，记录 Subject SHA；
3. 主智能体运行 guard，确认 source branch、Subject SHA、clean worktree、empty index；
4. 启动与 generator 不同、无父对话上下文的 evaluator，提供完整 manifest；
5. evaluator 前置 guard 通过后，独立审查规范、运行 guard 场景、静态契约测试与完整
   `verify`，再运行后置 guard；
6. evaluator 输出报告，不写入仓库：

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

7. `FAIL` 时 evaluator 停止，主智能体把原报告交回 generator；generator 修复并停止后，
   主智能体提交新 SHA，回到本阶段第 3 步；
8. `INCONCLUSIVE` 时不得推进入合并判断；先恢复可信交接或验证环境，再重新验证；
9. 只有 `PASS` 才进入远端 PR 阶段。

## 阶段 1 generator 已发生本地事实

2026-07-29，generator 在
`codex/generator-evaluator-separation` 的未提交实施工作树中完成第一版实现与自检：

- 使用 `D:\work\jdk\jdk-21.0.11` 运行 `.\scripts\check-environment.ps1`，报告 Java 21
  与 Maven Wrapper 3.9.12；
- `.\mvnw.cmd -Dtest=VerificationSubjectGuardTest,DocumentationNavigationTest,BranchFlowGuardTest,PullRequestDiffGuardTest test`
  通过，16 个相关测试无失败；
- `.\mvnw.cmd verify` 通过，122 个测试无失败；
- `VerificationSubjectGuardTest` 在临时 Git 仓库中覆盖正确 SHA/branch/clean/index，
  并受控拒绝旧 SHA、错误分支、dirty worktree、staged index、缺少参数和非 Git 目录；
- 当前 Windows `PATH` 没有系统 `sh`；使用 Git for Windows 的明确 `sh.exe` 路径运行
  `sh -n ./scripts/check-verification-subject.sh` 已通过。POSIX 行为场景仍需后续
  evaluator/远端 Linux `verify` 按计划独立复验。

以上仅是 generator 自检，不是 evaluator `PASS`，也不表示实施 PR、远端 CI、合并或最终
`develop` 验证已经发生。

协调者随后冻结 Subject SHA
`a551365b9a9d8a7cdc8598a274dd23df48e3ca30`。独立 evaluator 对该 SHA 给出真实 `FAIL`：
PowerShell guard 使用默认大小写不敏感的分支比较，错误接受了仅字母大小写不同的 expected
branch，而 POSIX guard 正确拒绝。该报告已回流 generator；旧 SHA 与旧结论已经失效，
generator 改用明确大小写敏感的精确比较并补充同场景回归。修复后的新 SHA 尚未冻结或
独立验证。

## 阶段 3：同一 SHA 的 PR、CI 与合并

1. 主智能体推送已独立 `PASS` 的精确 Subject SHA，创建 `codex/* → develop` PR；
2. 把 evaluator 原始报告持久化为 PR 评论，显示 generator/evaluator 标识与 Subject SHA；
3. 回读 PR head SHA 等于报告 Subject SHA；
4. 等待现有 required `verify` 成功，确认没有新增或冒充身份的 required check；
5. 如果同步 base、修复或其他操作改变 head SHA，旧 `PASS` 立即失效，回到阶段 2；
6. required `verify` 和 evaluator `PASS` 同时覆盖同一 SHA 后，才交由人类/正常保护流程
   决定是否合并；
7. 合并后检查最终 `develop` push `verify`。该远端结果由 GitHub 保存，不提前回写。

## 风险、回滚与证据边界

### 风险

- **身份不可机械认证**：仓库只能检查 Git 状态；不同 agent 身份由协调日志、manifest 和
  PR 评论审计，不描述为平台级保证；
- **构建副作用**：Maven 可能更新被忽略的 `target/`；guard 关注 Git 可见变化，报告仍需
  声明构建产物边界；
- **状态竞争**：generator/evaluator 并发会破坏只读保证，因此第一版严格串行；
- **报告使 SHA 改变**：报告只写 PR 评论，不提交进被验证分支；
- **旧结论误用**：任何 head SHA 变化都重新独立验证；
- **流程成本**：小任务也增加 handoff 时间；先收集真实数据，未经新决策不降级。

### 回滚

- 规划或实施 PR 合并前可以关闭 PR，现有工作流保持不变；
- 实施合入后发现协议阻塞合法工作，通过新的受保护 `codex/* → develop` Harness PR
  回退或修正，不直接改写长期分支；
- 不通过删除证据、关闭 required `verify`、让同一智能体兼任两角色或手工 worktree
  来临时恢复速度。

### 证据边界

- 仓库：决策、计划、稳定协议、guard 和静态契约测试；
- evaluator 报告：精确 Subject SHA 上的本地独立验证及 residual gaps；
- GitHub PR 评论：角色标识、manifest 摘要和 evaluator 报告的持久化审计；
- GitHub PR/Check Run/Actions：base/head、required `verify`、合并与最终 push CI；
- 以上都不能证明密码学身份，也不能把 `main` 发布基线描述为真实部署。

## 停止条件

出现以下任一情况立即停止，不扩大范围：

- 无法确认精确 `origin/develop` base、Subject SHA、当前分支或干净 Git 状态；
- 找不到与 generator 不同且能以无父对话上下文启动的 evaluator；
- evaluator 需要修改仓库、切分支、手工管理 worktree 或操作 GitHub才能完成验证；
- 需要同一智能体兼任 generator/evaluator，或需要把 `INCONCLUSIVE` 当作通过；
- required `verify` 无法与 evaluator 报告绑定同一 PR head SHA；
- 需要改变 CI Job 身份、GitHub 权限、分支保护、产品、架构或数据格式；
- 实际发现仅靠协调证据不足，必须引入外部身份认证或付费服务；
- 决策卡 020/计划 017 的陈旧状态干扰实施并要求顺手扩大本计划。

## 验收标准

- [ ] 规划 PR 留有不同 generator/evaluator 对精确 SHA 的试运行报告，并在 required
  `verify` 成功后取得人类对决策卡 021、规划 PR 合并及实施的明确批准；
- [x] 决策卡 021 与计划 018 只在上述明确批准后经规划 PR 合入；
- [x] `AGENTS.md` 明确四方职责、串行 handoff、状态机、SHA 失效和完成定义；
- [x] generator 与 evaluator 的禁止权限清晰且与 Codex-managed Worktree 决策一致；
- [x] 两个跨平台 guard 入口实现 branch/HEAD/SHA/clean/index 前后检查，失败反馈满足
  六字段要求；
- [x] 静态契约和本地场景覆盖 guard、角色边界及 required `verify` 不变；
- [ ] 实施由 generator 子智能体完成，主智能体冻结 SHA，不同 evaluator 只读验证；
- [x] 至少试验并记录一次可控的 `FAIL -> IMPLEMENTING -> 新 SHA -> VERIFYING` 回流，
  或在不伪造产品缺陷的前提下用 guard/测试 fixture 证明旧结论失效路径；
- [ ] evaluator 报告使用统一格式，未写入被验证提交，由主智能体保存到 PR 评论；
- [ ] evaluator `PASS` 与 required `verify` 对同一 PR head SHA 成功；
- [x] 没有新增伪造身份的 `independent-verification` check，没有手工 worktree；
- [ ] JDK 21 下相关测试与完整 `verify` 通过，最终 `develop` push `verify` 由 GitHub
  权威记录确认；
- [ ] 没有修改产品、架构、数据格式、远程权限或计划 017/决策 020 的陈旧状态。
