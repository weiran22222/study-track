# 执行计划 025：实施 PR evaluator 报告生命周期

状态：仓库内实施与 generator 自检已完成（2026-07-30）

## 目标与权威边界

本计划实施
[决策卡 031](../../decisions/031-pr-evaluator-report-lifecycle.md)：把完整当前 evaluator
`PASS` 放入 PR body 的 v1 marker 区，由现有唯一 required `verify` 在 PR event 中检查
结构、head SHA 绑定和 `PASS`；旧 `FAIL`/`INCONCLUSIVE` 将保留为评论。每次 PR 合入
`develop` 后，coordinator 将依据用户长期授权，在确认精确远端 SHA 及其 push
`verify` 后安全切回并纯快进本地 `develop`。

本计划属于第三级 Harness 变更，将严格使用“规划 PR → 功能 PR”。规划 PR 不会激活新
协议；功能 PR 将先更新当前权威文档，再实现 workflow、POSIX 脚本和测试。现有
[WORKFLOW.md](../../../WORKFLOW.md)、[ARCHITECTURE.md](../../../ARCHITECTURE.md)、
[决策卡 022](../../decisions/022-simplify-agent-handoff.md)和
[决策卡 023](../../decisions/023-local-develop-fast-forward-policy.md)在功能 PR 合并前
继续定义当前有效行为。

## 全程不变量

- generator 写入、协调者冻结 Subject SHA、不同 evaluator 只读验证将依次进行；任何
  新 SHA 都会使旧报告失效；
- evaluator-before-PR 将保持不变：新 PR 创建或既有 PR 推送新 head 前，将先取得该
  冻结 SHA 的完整 evaluator `PASS`；
- 唯一 required Job 将继续名为 `verify`，不会新增 required status 或修改保护规则；
- PR body 只保存当前 head 的完整 `PASS`；旧 `FAIL`/`INCONCLUSIVE` 将在 PR 已创建后
  追加评论，不会作为当前门禁输入；
- 不会捏造、摘要替代或批量回填历史 evaluator 报告；功能 PR 将作为 v1 前瞻试点，
  PR #54 不会在本计划中回填；
- workflow 将安全落盘 event body；POSIX 检查脚本不会访问网络、GitHub API、Git 状态
  或修改任何文件；
- PR 合并、远端 SHA 与 Actions 结果只会从 GitHub 权威记录回读，不会由计划或本地命令
  推断；
- 合并后安全回流遇到 dirty、staged、`develop` 占用、分叉、SHA 不确定或 push
  `verify` 失败时将停止，不会 reset、clean、merge、rebase、cherry-pick、stash 或自动
  恢复；
- 本计划不会改变产品行为、规格、数据格式、依赖、分支拓扑、发布语义、部署状态或
  evaluator 身份信任边界。

## 规划 PR 精确范围

规划 PR 将只包含：

- `docs/decisions/031-pr-evaluator-report-lifecycle.md`；
- `docs/exec-plans/025-pr-evaluator-report-lifecycle.md`；
- `docs/README.md`。

规划 PR 不会修改 `WORKFLOW.md`、`ARCHITECTURE.md`、`HARNESS-CAPABILITIES.md`、
workflow、脚本、测试、`SPEC.md`、产品代码、依赖、GitHub 设置或远端状态。

## 功能 PR 预计范围

功能 PR 预计只修改：

- `WORKFLOW.md`：激活 evaluator-before-PR、body/current PASS、历史评论、新 SHA 失效、
  四种 PR 事件和合并后长期授权的安全回流顺序；
- `ARCHITECTURE.md`：在验证流水线中描述现有 `jobs.verify` 的 PR-only 报告检查、
  event/body/head 数据边界与 push 路径不运行该检查；
- `HARNESS-CAPABILITIES.md`：把报告存在性与 SHA 绑定列入机械反馈，同时保留身份、
  内容真实性与因果边界；
- `.github/workflows/verify.yml`：显式使用
  `opened, synchronize, reopened, edited`，在同一个 `jobs.verify` 内安全落盘 PR body
  并调用报告脚本；push 路径将跳过该 PR-only 步骤；
- `scripts/check-pr-evaluator-report.sh`：实现 v1 marker、字段、SHA 与 `PASS` 的纯本地
  fail-closed 检查；
- `src/test/java/com/example/studytrack/architecture/VerifyWorkflowTriggerTest.java`：
  把 trigger 断言更新为四种明确 activity，把 PR-only 条件数量从既有两个更新为新增
  报告门禁后的三个，并固定 branch-flow、完整差异、evaluator 报告与 canonical
  verification 的顺序；JDK setup、环境自检和 Maven `verify` 将继续对每个已触发的 PR
  及 `develop`/`main` push 无条件执行；
- `src/test/java/com/example/studytrack/architecture/PullRequestEvaluatorReportTest.java`：
  覆盖脚本行为、workflow 静态契约、禁止网络和六字段失败诊断；
- `src/test/java/com/example/studytrack/architecture/DocumentationNavigationTest.java`：
  仅在现有导航测试需要识别决策、计划或新当前协议时做最小更新；
- 本决策、本计划和 `docs/README.md`：只根据实际本地实施更新状态，并在完成时把计划
  移入 `exec-plans/completed/`。

除非行为测试证明决策 023 的现有入口无法执行获批顺序，功能 PR 将不修改
`scripts/update-local-develop.ps1`、`scripts/update-local-develop.sh` 或
`LocalDevelopUpdateTest`。若需要修改其他文件、依赖、required check、分支保护或远程
权限，将立即停止并重新取得人类批准。

## v1 脚本与 workflow 设计

`scripts/check-pr-evaluator-report.sh` 将只接收两个参数：

```text
check-pr-evaluator-report.sh <pr-body-file> <expected-head-sha>
```

脚本将要求：

1. 参数数量精确为二，body 文件可读，expected SHA 为完整 40 位十六进制；
2. begin/end marker 各出现且只出现一次，并按正确顺序包围报告；
3. `Subject SHA`、`Generator`、`Evaluator`、`Commands executed`、
   `Independent scenarios`、`Findings`、`Residual gaps`、`Verdict` 八个字段在 marker
   内各出现一次且内容非空；
4. 字段顺序符合 v1，避免歧义解析；
5. `Subject SHA` 精确等于 expected PR head SHA；
6. `Verdict` 精确等于 `PASS`。

失败将输出 Location、Invariant、Reason、Fix、Recheck 与 Authority 六字段，并使用非零
退出。脚本不会解释完整 CommonMark，不会查询评论、网络、GitHub、Git 或 evaluator
身份，也不会修改 body 文件。

workflow 将从 `$GITHUB_EVENT_PATH` 读取 event JSON，而不会把
`${{ github.event.pull_request.body }}` 直接插值到 shell。PR-only 步骤将把
`.pull_request.body // ""` 安全写到 `$RUNNER_TEMP` 下的文件，并从同一 event 读取
`.pull_request.head.sha`，然后把两个值交给脚本。临时路径将由 runner 管理；脚本不会把
PR body 当作命令执行。

现有 `pull_request` 触发将显式使用：

```yaml
types: [opened, synchronize, reopened, edited]
```

`jobs.verify` 名称、JDK 21 环境检查、PR 分支流、完整差异检查和 Maven `verify` 将继续
存在。push 到 `develop`/`main` 将继续运行环境检查与 Maven `verify`，但不会运行没有
PR body/head 语义的 evaluator 报告步骤。

## 阶段 0：规划 PR 自检、独立验证与人工合并

1. generator 将只起草决策 031、活跃计划 025 和索引；
2. generator 将使用 command-local JDK 21 运行文档导航测试、Markdown 本地链接门禁、
   完整 Maven `verify` 和规划 diff 范围/空白检查，并只报告为 generator 自检；
3. coordinator 将审查精确范围，只暂存三个规划文件，运行
   `git diff --cached --check`，提交并冻结规划 Subject SHA；
4. 不同 evaluator 将在前后 guard 之间只读验证规划内容、未来时态、权限边界、文档
   导航和完整 `verify`，给出 `PASS | FAIL | INCONCLUSIVE`；
5. 规划 PR 将按当前协议创建；evaluator 报告仍将使用当前可用持久化方式，因为 v1 尚未
   实施；
6. 只有不同 evaluator `PASS` 与 required `verify` 覆盖同一规划 head SHA 后，才会
   交由人类决定是否合并；
7. 人类实际批准并在 GitHub 合并后，coordinator 将回读精确 `origin/develop` SHA，
   等待该 SHA 的 push `verify` 成功，再按当前规则安全更新本地 `develop`；
8. 任一远端或本地前置不成立时将停止，功能 PR 不会开始。

## 阶段 1：先更新权威协议，再实现功能

1. coordinator 将从已远程验证并安全更新的干净 `develop` 准备经授权的功能分支；
2. generator 将先更新 `WORKFLOW.md`、`ARCHITECTURE.md` 与
   `HARNESS-CAPABILITIES.md`，明确当前语义、证明边界和不变权限；
3. generator 将随后实现 POSIX 脚本和同一个 `jobs.verify` 内的 PR-only workflow
   步骤，再增加行为/静态测试；
4. generator 将把功能 PR 定为 v1 前瞻试点，不会修改 PR #54；只有功能实施中真实发生
   的 evaluator `FAIL`/`INCONCLUSIVE` 才会成为评论；
5. generator 将按已经发生的本地实施事实更新决策与计划状态；不会预写 evaluator、
   required CI、PR、合并、push `verify`、本地安全回流或效果结论；
6. 如果实现需要 comments-only 门禁、新 required status、GitHub API、网络访问、完整
   CommonMark 解析、外部身份服务或 develop 更新脚本扩权，将立即停止。

## 阶段 2：focused cases 与 generator 自检

脚本行为测试将至少覆盖：

- body 缺少 marker；
- begin/end marker 重复；
- 任一必需字段缺失或为空；
- 报告 Subject SHA 与 expected PR head SHA 不同；
- `Verdict` 为 `FAIL`、`INCONCLUSIVE` 或其他非 `PASS`；
- 唯一 marker、全部字段、相同 SHA 且 `Verdict: PASS` 的有效报告。

workflow 静态测试将确认：

- `pull_request` 只显式包含 `opened`、`synchronize`、`reopened`、`edited`；
- 仍只有一个 `jobs.verify`，没有 `independent-verification` 或其他 required Job；
- `VerifyWorkflowTriggerTest` 将把 PR-only 条件的精确数量更新为三个，并按
  branch-flow → 完整差异 → evaluator 报告 → JDK setup → 环境自检 → Maven `verify`
  检查顺序；
- body 从 `$GITHUB_EVENT_PATH` 安全落盘，不直接插值到 shell；
- expected SHA 来自 `pull_request.head.sha`，不是 merge commit `$GITHUB_SHA`；
- 报告脚本只在 PR 路径运行，push 路径继续执行既有环境检查和 Maven `verify`；
- canonical JDK setup、环境自检和 Maven `verify` 将保持无 `if:` 条件，对四种已触发的
  PR activity 以及 `develop`/`main` push 无条件执行；
- 脚本没有 `curl`、`wget`、`gh`、HTTP、GitHub API 或 Git 写操作；
- 文档与 workflow、脚本、失败六字段保持一致。

generator 将在 JDK 21 下运行：

```powershell
.\scripts\check-environment.ps1
.\mvnw.cmd "-Dtest=PullRequestEvaluatorReportTest,DocumentationNavigationTest,DocumentationConsistencyTest,VerifyWorkflowTriggerTest" test
.\mvnw.cmd verify
git diff --check
```

POSIX 原生行为将由可用的明确 `sh` 入口或 Linux required `verify` 执行。Windows 若没有
系统 `sh`，generator 将明确报告本地未执行的 POSIX 边界，不会冒充双平台通过。

## 已发生的仓库内实施与 generator 自检

2026-07-30，generator 在获准功能工作树中先更新 `WORKFLOW.md`、`ARCHITECTURE.md` 与
`HARNESS-CAPABILITIES.md`，再实现 workflow、POSIX guard 与测试。实际范围为计划内
文件；`DocumentationNavigationTest` 因旧能力边界锚点和计划归档路径变化做了最小更新。
没有修改 `SPEC.md`、产品代码、`pom.xml`、依赖、develop 更新脚本/
`LocalDevelopUpdateTest`、分支保护或远程权限。

实际实现结果：

- `pull_request` 已明确使用 `opened`、`synchronize`、`reopened`、`edited`；同一个
  `jobs.verify` 按 branch-flow、完整 diff、evaluator report 顺序运行三个 PR-only
  步骤，随后无条件运行 JDK setup、环境自检和 Maven `verify`；
- workflow 从 `$GITHUB_EVENT_PATH` 使用 runner 本地 `jq` 提取 body/head，body 只落入
  `$RUNNER_TEMP`；新脚本不访问网络、GitHub API 或 Git，也不修改状态；
- 新脚本实现唯一 marker、八字段固定顺序/非空、40 位 SHA 精确匹配、`Verdict: PASS`
  和统一六字段失败诊断；
- `PullRequestEvaluatorReportTest` 覆盖 missing/duplicate marker、missing/empty
  scalar/section、字段乱序、SHA mismatch、`FAIL`/`INCONCLUSIVE` 和 valid；
  `VerifyWorkflowTriggerTest` 覆盖四种
  activity、三个 PR-only 条件、顺序与无条件 canonical verification。

generator 的实际验证反馈形成了两轮收敛：

1. Windows 默认定向 Maven 首次运行 25 项通过、1 项按既有边界跳过；随后显式设置
   `STUDYTRACK_POSIX_SHELL=C:\Program Files\Git\bin\sh.exe` 运行真实 POSIX 场景；
2. 显式 POSIX 场景前两次均在合法报告处失败，分别定位到 Git awk 不接受两处多行语法；
   改成兼容语法后，同一 25 项定向测试全部通过，0 失败、0 错误、0 跳过；
3. 第一次完整 JDK 21 `verify` 运行 157 项测试，其中 1 项
   `LocalDevelopUpdateTest` 失败，原因是 `WORKFLOW.md` 改写破坏既有
   `push `verify` 成功` 稳定锚点；恢复原锚点而不修改测试后，相关 16 项定向测试通过；
4. 随后完整 JDK 21 `verify` 运行 157 项测试，0 失败、0 错误、0 跳过，并成功打包。

这些结果只属于 generator 本地自检。不同 evaluator、required CI、功能 PR、合并、最终
远端 push `verify` 与合并后安全回流尚未发生，不能由本节推断。

## 阶段 3：冻结、新 SHA 生命周期与功能 PR 试点

1. coordinator 将审查功能 diff、脚本输入边界、workflow 权限和测试，只暂存精确预期
   文件并运行 `git diff --cached --check`；
2. coordinator 将提交并冻结 `FROZEN(<Subject SHA>)`，在不同 evaluator 前后运行 guard；
3. evaluator 将独立复验 focused cases、workflow 静态契约、文档导航、完整
   `verify` 与证明边界；
4. `FAIL` 将回流 generator；修复形成新 SHA 后，旧报告将立即失效并从头验证；
5. 只有完整 `PASS` 才会进入功能 PR body 的 v1 marker 区；如果 PR 尚未创建，历史
   `FAIL`/`INCONCLUSIVE` 将在创建后按真实原文追加评论；
6. 功能 PR `opened` 将试运行 v1 门禁。若后续新 head 推送，旧 body 将使
   `synchronize` 失败；新 SHA 重新 evaluator `PASS` 后，coordinator 将更新 body，
   `edited` 将触发同一 required `verify` 重跑；
7. 只有 body/current head/evaluator `PASS` 与 required `verify` 覆盖同一 SHA，才会
   进入人类合并判断。

## 阶段 4：人工合并与自动安全回 develop

1. 人类将继续决定功能 PR 是否合并；长期授权不会替代这项决定；
2. GitHub 实际合入 `develop` 后，coordinator 将无需再次询问安全回流授权；
3. coordinator 将从 GitHub 权威记录回读最终 `origin/develop` 的完整 SHA，并等待该
   精确 SHA 的 push `verify` 成功；
4. coordinator 将确认当前工作树与暂存区干净，再运行 `git switch develop`；若
   `develop` 被占用、切换失败或状态不干净，将停止；
5. coordinator 将把已验证 SHA 传给
   `.\scripts\update-local-develop.ps1 "<verified-develop-sha>"` 或 POSIX 对应入口；
6. coordinator 将后置确认本地 `develop`、`origin/develop` 与已验证 SHA 相同，且
   工作树和暂存区干净；
7. 任一 SHA、push `verify`、占用、分叉、dirty/staged 或后置条件失败时将停止并报告，
   不会 reset、clean、merge、rebase、cherry-pick、stash、删除或搬运提交；
8. 只有必须记录合并后新出现的偏差、外部状态或残余风险时，才会另建收尾 PR；不会为了
   复制 GitHub 已保存的成功记录而创建收尾工件。

## 验收状态

- [x] 当前权威文档先于脚本和 workflow 更新，并准确区分报告机械存在性与身份/内容
  真实性边界；
- [x] v1 脚本 focused cases 覆盖 missing/duplicate marker、missing/empty
  scalar/section、字段乱序、SHA mismatch、`FAIL`/`INCONCLUSIVE` 和 valid report，
  并提供六字段诊断；
- [x] `VerifyWorkflowTriggerTest` 与新增静态测试覆盖四种 PR activity、安全 body
  落盘、PR head SHA、三个 PR-only 步骤的数量与顺序、无条件 canonical verification、
  唯一 `jobs.verify` 与无网络边界；
- [x] 功能实现保持前瞻试点边界，没有捏造或批量回填历史 PR，PR #54 保持不变；
- [x] focused tests、文档导航、Markdown 本地链接与完整 JDK 21 `verify` 已通过；
- [ ] 新 SHA 将使旧 body 门禁失败；重新 evaluator `PASS` 并更新 body 后，
  `edited` 将重跑同一 required `verify`；
- [ ] 不同 evaluator、同 SHA required `verify` 与功能 PR body 试点尚待实际发生；
- [ ] 人类仍将决定功能 PR 是否合并；合并后将确认精确 `origin/develop` SHA 的 push
  `verify` 成功，并按长期授权自动切回和安全更新本地 `develop`；
- [ ] dirty、占用、分叉、SHA 不确定或 verify 失败路径将停止，且不会发生 reset、
  clean、merge、rebase、cherry-pick 或其他恢复性 mutation；
- [ ] 不会新增 comments-only 门禁、自定义 required status、保护规则变化、外部身份
  服务、完整 CommonMark 声明、产品/依赖/发布/部署变化或 Harness 正向因果结论。

## 风险、回退与停止条件

- **body 被当作代码执行**：workflow 将只从 event JSON 安全落盘，再把文件路径交给
  脚本；任何直接 shell 插值都会阻止验收；
- **旧 `PASS` 误绑定新 SHA**：脚本将精确比较 PR head；任何新 SHA 都会先让旧 body
  失败，直到新报告更新；
- **评论历史冒充当前结论**：脚本将完全忽略评论，只接受 body 中唯一 v1 当前报告；
- **身份保证被夸大**：文档和测试将明确门禁只验证存在、结构、SHA 与自述 `PASS`；
- **edited 造成额外 CI 成本**：这是获得当前 body/head 一致性所接受的持续成本；不会再
  增加独立 Job；
- **历史回填制造伪证据**：本计划选择 feature PR 前瞻试点，不修改 #54；
- **合并后本地状态异常**：安全回流将 fail closed，长期授权不会扩张异常恢复权限；
- **workflow 事件漂移**：静态测试将固定四种 activity 与 push/PR 分支语义；
- **脚本解析范围扩大**：v1 将保持窄逐行协议，不演进为 CommonMark parser。

规划或功能 PR 合并前都可以关闭对应 PR。功能合入后若门禁错误拒绝合法报告，将通过新的
受保护 PR 修复或 revert，不会编辑分支保护、删除历史报告或管理员绕过。出现以下任一
情况将立即停止并请求新决定：

- 实现必须访问 GitHub API/评论、引入新 required status、修改保护规则或增加依赖；
- 无法安全从 event file 落盘 body，或 expected SHA 不能可靠绑定 PR head；
- v1 窄协议不能给出确定解析与六字段错误；
- 需要回填缺少原始证据的历史 PR，或修改 #54 才能证明功能；
- 需要修改 develop 更新入口、执行 reset/clean/merge/rebase/cherry-pick，或自动解决
  worktree 占用/分叉；
- evaluator、required `verify`、PR head 与 body Subject SHA 无法绑定同一提交；
- 实际范围扩大到产品、依赖、分支保护、远程权限、发布、部署或外部服务。

## 证据边界

本计划记录获批意图、实际仓库内实施与 generator 自检，但不预写 evaluator、required
CI、功能 PR、合并、最终 push `verify` 或安全回流成功。GitHub PR、评论、Check Run、
Actions 与远端 refs 将继续是对应远端事实的权威来源。

即使 v1 门禁与完整 `verify` 都成功，也只会证明当前 event/body/head 的机械条件和实际
构建结果；不会证明 evaluator 身份独立性、报告真实性、验证完整性、完整 CommonMark、
部署或 Harness 正向因果效果。
