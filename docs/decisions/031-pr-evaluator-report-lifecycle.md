# 决策卡 031：机械保护 PR evaluator 报告与合并后安全回流

状态：已批准，仓库内实施与 generator 自检已完成

日期：2026-07-30

## 问题与当前证据

[决策卡 021](021-generator-evaluator-role-separation.md)已经要求协调者把 evaluator 报告
持久化到 PR 评论，并把报告 Subject SHA、PR head 与 required `verify` 绑定到同一提交；
[决策卡 022](022-simplify-agent-handoff.md)继续保留统一报告字段、新 SHA 使旧报告失效及
`PASS | FAIL | INCONCLUSIVE` 语义。当前 [WORKFLOW.md](../../WORKFLOW.md)也要求协调者
保存报告，但 guard 只检查本地 Git 对象和工作树，唯一 `verify` 也不检查 PR 中是否真的
存在可审计报告。

已有远端记录显示这一缺口不是纯理论问题：

- [PR #36](https://github.com/weiran22222/study-track/pull/36)保留了真实 evaluator
  `FAIL`、修复后 `PASS` 与集成说明评论；
- [PR #54](https://github.com/weiran22222/study-track/pull/54)没有 PR 评论，尽管该次本地
  协调实际经历了两次 `FAIL` 和最终一次 `PASS`。

因此，“流程要求保存”目前不等于“每个 PR 都机械具备当前、完整且与 head SHA 绑定的
`PASS` 报告”。同时，[决策卡 023](023-local-develop-fast-forward-policy.md)已经提供
本地 `develop` 的安全纯快进入口，但每次 PR 合并后仍需要协调者依次回读精确远端 SHA、
确认对应 push `verify`、切回 `develop` 并调用入口；当前协议尚未记录用户对这段重复
协调动作的长期授权。

## 人类决定

用户明确批准把本变更按第三级 Harness 变更规划，并决定：

- 选择 evaluator-before-PR：只有冻结 Subject SHA 已经取得不同 evaluator 的完整
  `PASS`，才创建 PR 或把新的 head SHA 推入既有 PR；
- 当前完整 `PASS` 报告将放在 PR body 的 v1 机器可读区；旧的 `FAIL` 或
  `INCONCLUSIVE` 将在 PR 创建后追加为评论，保留失败回流历史；
- 唯一 required `verify` 的名称与保护规则保持不变，不新增 required status；
- 每次 GitHub PR 合入 `develop` 后，协调者无需再次询问是否回到本地 `develop`。协调者
  将先确认精确 `origin/develop` SHA 及其 push `verify` 成功，再在当前干净工作树中
  `git switch develop` 并调用既有安全纯快进入口；
- dirty、`develop` 被其他 worktree 占用、分叉、SHA 不确定或 push `verify` 失败时必须
  停止；不得用 reset、clean、merge、rebase 或 cherry-pick 恢复流程；
- 先通过规划 PR 交付本决定与执行计划，规划 PR 仍由人类决定是否合并；只有规划 PR
  实际合并并完成远端验证与本地安全更新后，功能 PR 才会实施。

这项长期授权只消除同一安全回流动作的重复询问，不授权自动合并 PR、不扩大 push、
GitHub 写入、发布、管理员绕过或异常恢复权限。

## 候选方案与取舍

### A. 只把 evaluator 报告写入 PR 评论

- 收益：保留按时间追加的自然审计流；
- 成本：required `verify` 很难在不访问 GitHub API 的情况下确定哪条评论代表当前 head
  的最终报告，也容易让旧 `PASS` 与新 head 并存。

不选择。评论只保存旧 `FAIL`/`INCONCLUSIVE` 与集成上下文，不作为当前合并门禁输入。

### B. 为 evaluator 报告增加新的 required status

- 收益：报告门禁在分支保护中拥有独立名称；
- 成本：新增 required check 和保护规则会扩大远端配置与维护表面，而且普通 CI 仍不能
  认证 evaluator 身份。

不选择。现有唯一 required `verify` 将继续承载 PR 机械门禁。

### C. 在 PR body 保存完整当前 `PASS`，由现有 `verify` 的 PR-only 步骤检查

- 收益：PR event 已包含 body 与 head SHA；检查不需要网络或额外身份服务，新 SHA 会让
  旧 body 立即因 SHA 不匹配而失败；`edited` 事件可以在重新评估后重跑同一 required
  Job；
- 成本：需要维护一个版本化文本 envelope、POSIX 检查脚本、workflow 步骤和回归测试；
  PR body 编辑也会触发一次 required `verify`。

选择本方案。

### D. 只保存 Subject SHA、Evaluator 与 Verdict 的简短 envelope

- 收益：解析最简单、PR body 更短；
- 成本：丢失命令、独立场景、发现与残余缺口，不能满足决策 021/022 的完整报告审计
  目的。

不选择。

## 决定与 v1 报告协议

当前有效的 evaluator 报告将完整出现在 PR body 中，且只允许一对精确 marker：

```text
<!-- studytrack-evaluator-report:v1:start -->
Subject SHA: <40 位 PR head SHA>
Generator: <非空标识>
Evaluator: <非空标识>
Commands executed:
<至少一行实际命令与结果>
Independent scenarios:
<至少一行独立场景>
Findings:
<至少一行发现；无发现时明确写 none>
Residual gaps:
<至少一行残余缺口；无缺口时明确写 none>
Verdict: PASS
<!-- studytrack-evaluator-report:v1:end -->
```

v1 将采用逐行、固定 marker 与固定字段顺序的窄协议，不把 PR body 当作完整 CommonMark
解析。PR body 中 begin/end marker 必须各出现且只出现一次，顺序正确；八个字段必须在
marker 内各出现一次并具有非空内容。`Subject SHA` 必须精确等于 event 的
`pull_request.head.sha`，`Verdict` 必须精确为 `PASS`。缺失或重复 marker、缺失字段、
SHA 不匹配和非 `PASS` 都将使 `verify` 失败。

报告内容只能来自已经完成的 evaluator 输出。协调者不得根据测试日志自行补写
`PASS`，不得把 generator 自检改写成 evaluator 报告，也不得缩短为仅含结论的 envelope。

## PR 生命周期与机械门禁

仓库内实现保持唯一 `jobs.verify`，并把 `pull_request` activity types 明确限制为
`opened`、`synchronize`、`reopened` 与 `edited`：

1. evaluator 将在 PR 创建或新 head 推送前，对冻结 SHA 完成只读验证；
2. 新 PR 的 body 将从一开始包含该 SHA 的完整 v1 `PASS` 报告，`opened` 将运行
   `verify`；
3. 已有 PR 的 head SHA 变化时，旧 body 将因 SHA 不匹配而使 `synchronize` 运行失败；
4. 新 SHA 重新取得 evaluator `PASS` 后，协调者将更新 body；`edited` 将重跑并允许同一
   required `verify` 对新 head 与新报告共同验收；
5. 旧 `FAIL`/`INCONCLUSIVE` 将在 PR 已存在后按实际发生顺序追加评论，不覆盖 body 中
   唯一的当前 `PASS`；
6. `reopened` 将重新检查 body 与当前 head，防止关闭期间发生的报告漂移被忽略。

workflow 不会把未受信任的 PR body 直接插值到 shell。它将从只读
`$GITHUB_EVENT_PATH` 安全提取 body 与 head SHA，把 body 写入 runner 临时文件，再把
临时文件路径和期望 SHA 传给仓库内 POSIX 脚本。脚本只读取参数与本地文件，不访问网络、
GitHub API 或 Git 状态，也不修改 PR、仓库或 runner 外部状态。

## 历史报告与试点边界

不批量回填历史 PR，也不根据摘要捏造原始 evaluator 报告。PR #54 将继续作为“有真实
本地验证历史但没有 PR 评论”的缺口证据，本决定不修改它。

功能 PR 将作为 v1 协议的前瞻试点：它的当前 `PASS` 将进入 PR body，实施期间真实发生的
`FAIL`/`INCONCLUSIVE` 才会追加评论。若未来人类另行要求回填 PR #54，只能根据可定位的
两份真实 `FAIL` 和最终 `PASS` 原文逐份回填，并明确标记为历史补录；本计划不预先授权
该动作。

## 合并后安全回到 develop

当前 `WORKFLOW.md` 已把以下 coordinator 顺序写入当前工作流；既有
`scripts/update-local-develop.ps1` 和 `.sh` 未修改：

1. 只在 GitHub 权威记录确认 PR 已合入 `develop` 后开始；
2. 回读精确 `origin/develop` SHA，并确认该同一 SHA 的 push `verify` 成功；
3. 确认当前工作树与暂存区干净；随后运行 `git switch develop`。若 `develop` 被其他
   worktree 占用或切换失败，将停止而不修复；
4. 把已验证的完整 SHA 作为唯一参数交给现有安全纯快进入口；
5. 后置确认本地 `develop`、`origin/develop` 和已验证 SHA 精确一致，工作树与暂存区
   仍干净。

任一远端事实、SHA、分支占用、提交关系或 Git 状态无法证明时都会停止并报告，不会自动
reset、clean、merge、rebase、cherry-pick、stash、删分支或搬运提交。

## 证明边界与非目标

v1 门禁只将证明：

- PR body 存在唯一、结构符合协议的报告区；
- 所需字段存在且非空；
- 报告自述 `Subject SHA` 等于当前 PR head；
- 报告自述 `Verdict: PASS`。

它不会证明 generator/evaluator 的真实身份或独立性、报告内容真实性、命令确实执行、
场景完整、结论正确、完整 CommonMark 语义、外部状态或 Harness 正向因果效果。身份与
内容仍由协调记录、审查和人类判断审计；required `verify` 继续只证明实际 Job 对对应
SHA 的机械结果。

本决定不会改变产品行为、规格、Java 分层、数据格式、依赖、required Job 名称、分支保护
规则、发布语义或部署状态；不会新增 comments-only 门禁、自定义 required status、外部
身份服务、网络调用、定时自动化或历史批量回填。

## 仓库内实施事实与未发生边界

2026-07-30，本功能工作树已按“权威文档先行”更新 `WORKFLOW.md`、
`ARCHITECTURE.md` 与 `HARNESS-CAPABILITIES.md`，随后完成：

- 在同一 `jobs.verify` 中把 `pull_request` activity 明确为 `opened`、`synchronize`、
  `reopened`、`edited`，并在 branch-flow、完整 diff 后增加 PR-only 当前报告检查；
- 从 `$GITHUB_EVENT_PATH` 使用 runner 本地 `jq` 安全提取 body/head，把 body 写入
  `$RUNNER_TEMP`，未直接把 PR body 插值进 shell；
- 新增只读本地参数/文件的 `scripts/check-pr-evaluator-report.sh`，检查唯一 v1 marker、
  八字段固定顺序与非空、完整 SHA 精确匹配和 `Verdict: PASS`，所有失败共用六字段诊断；
- 更新 workflow 契约测试、文档导航测试，并增加缺少/重复 marker、缺少/空
  scalar/section、字段乱序、SHA 不匹配、`FAIL`/`INCONCLUSIVE` 与有效报告行为场景；
- 保持唯一 required Job、无条件 JDK/环境/Maven 验证和既有 develop 安全更新脚本/
  测试不变；未修改产品、规格、依赖、分支保护或远程权限。

generator 使用显式 Git for Windows `sh.exe` 运行 POSIX focused matrix。前两次真实运行
分别发现 Git awk 不接受两处多行语法，修复为兼容写法后 25 项定向测试全部通过、无跳过。
第一次完整 `verify` 的 157 项测试中有 1 项失败：`WORKFLOW.md` 改写丢失了既有 develop
更新测试保护的稳定字面锚点；恢复该锚点后，相关 16 项定向测试与完整 157 项测试均通过。

以上仅是仓库内实现与 generator 本地自检事实，不表示不同 evaluator 已给出 `PASS`、
required CI 已成功、功能 PR 已创建或合并、远端 `develop` 已变化，或合并后安全回流已经
发生。

## 风险等级与交付

这是第三级 Harness 变更：它改变 evaluator 报告的持久化协议、PR 事件门禁、coordinator
合并后权限和跨本地/远端状态顺序，并引入持续维护脚本。交付将使用“规划 PR → 功能 PR”：

1. 规划 PR 只新增本决策、执行计划 025 和文档索引，不改变当前规则、workflow、脚本、
   测试或远端状态；
2. 规划 PR 将先完成 generator 自检、不同 evaluator 验证和同 SHA required `verify`，
   再由人类决定是否合并；
3. 只有规划 PR 人工合并、精确 `develop` push `verify` 成功且本地安全回流完成后，功能
   PR 才会按计划实施；
4. 功能 PR 也将经过冻结 SHA、不同 evaluator、同 SHA required `verify`、人类合并判断
   和合并后安全回流。

规划文字不表示上述实现、测试、PR、CI、合并、远端验证或本地更新已经发生。
