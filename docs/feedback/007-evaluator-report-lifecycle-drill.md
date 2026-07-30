# PR evaluator 报告生命周期远程负路径演练

状态：已完成

日期：2026-07-30

## 1. 基线与观察边界

本演练的精确 `develop` 基线是
`2d41e676d5fa78f2fa9588a21abcd13f4318e824`。该 SHA 只固定演练分支的仓库起点，不是
效果对照组。六个演练步骤的已发生事实见第 4 节；该基线本身不证明步骤结果，也不表示
本次最终记录提交及其后的远程动作已经发生。

演练依据[决策 031](../decisions/031-pr-evaluator-report-lifecycle.md)和当前
[WORKFLOW.md](../../WORKFLOW.md)验证 PR evaluator 报告门禁的 fail-closed 操作语义：
正确且与 head 绑定的 v1 `PASS` 报告应允许同一 `verify` 继续完成；错误 SHA 或陈旧报告
应使对应事件的 `verify` 失败，恢复为当前 head 的真实独立 evaluator `PASS` 后才应重新
成功。

目标仅是验证上述特定远程事件与门禁的操作结果是否符合预注册预期。即使全部步骤符合
预期，也不证明 evaluator 身份或报告内容真实，不证明验证完整性，也不构成 Harness
正确性、效率、自治性或其他维度的正向因果效果。

## 2. 不变条件

- 使用现有唯一 required context `verify`，不修改或绕过分支保护；
- 每个有效 body 报告都必须来自对应冻结 Subject SHA 的不同 evaluator 完整 `PASS`，
  不得把 generator 自检或协调者判断改写成 evaluator 报告；
- 错误报告只把 `Subject SHA` 替换为格式合法且不可能等于实际提交的 40 位值
  `0000000000000000000000000000000000000000`；不得删除其他必需字段或同时制造第二种
  失败原因；
- 除预注册的 body 编辑、关闭/重新打开和后续已验证提交外，不改变 workflow、报告脚本、
  required context、PR base 或其他门禁输入；
- 每一步只以对应 GitHub PR event、Actions run、Check Run 与必要评论为远程权威记录，
  不用本地推断补写结论。

## 3. 预注册顺序与预期

下表保留执行前注册的顺序、受控输入与预期；六个步骤的实际结果另见第 4 节：

| 顺序 | 待执行动作 | 受控输入 | 预期结果 |
|---|---|---|---|
| 1 | 创建 PR，触发 `opened` | body 从创建时起包含当前 PR head 的唯一完整 v1 `PASS` 报告 | 同一 `verify` 成功 |
| 2 | 编辑 body，触发 `edited` | 只把报告 Subject SHA 改为 `0000000000000000000000000000000000000000`，其他字段保持不变 | 同一 `verify` 因 SHA 不匹配失败 |
| 3 | 再次编辑 body，触发 `edited` | 恢复当前 PR head 的原始完整独立 evaluator `PASS` 报告 | 同一 `verify` 恢复成功 |
| 4 | 关闭后重新打开 PR，触发 `reopened` | head 与正确 body 均不改变 | 同一 `verify` 成功 |
| 5 | 推送后续新提交，触发 `synchronize` | 新 SHA 在推送前先取得不同 evaluator 完整 `PASS`，但 PR body 暂时保留旧 head 的报告 | 同一 `verify` 因旧 body SHA 与新 head 不匹配失败 |
| 6 | 编辑 body，触发 `edited` | 使用步骤 5 已取得的新 SHA 独立 evaluator `PASS` 替换旧报告 | 同一 `verify` 恢复成功 |

步骤 5 保留 evaluator-before-PR：新提交将在推送前先完成冻结 SHA 独立验证；演练只故意
延迟 body 更新，以验证 `synchronize` 对陈旧报告 fail closed。不得为了制造负路径而推送
未经独立 `PASS` 的新 head。

每一步的实际记录包含 event activity、PR head SHA、body 中的 Subject SHA、Actions run
链接、`verify` conclusion 以及与预期是否一致。

## 4. 步骤 1～6 的实际结果

承载演练的 PR 是
[PR #57](https://github.com/weiran22222/study-track/pull/57)。步骤 1～4 的 head 为
`8f707dced5348ddcff9026eb33c2b08c56ea186b`，步骤 5～6 的新 head 为
`f0d1818d2af9398c697d2c33519d6d11968eeecb`；新 head 在推送前已取得不同 evaluator
`PASS`：

| 顺序 | 已发生 event 与输入 | 权威 run 与结果 | 与预期比较 |
|---|---|---|---|
| 1 | `opened`；body 为 head `8f707dced5348ddcff9026eb33c2b08c56ea186b` 的正确完整报告 | [`30535318232`](https://github.com/weiran22222/study-track/actions/runs/30535318232)：`success` | 符合预期 |
| 2 | `edited`；body Subject SHA 仅改为 `0000000000000000000000000000000000000000` | [`30535404918`](https://github.com/weiran22222/study-track/actions/runs/30535404918)：`failure`；失败步骤为 `Check the current evaluator report` | 符合预期 |
| 3 | `edited`；body 恢复为当前 head 的正确完整报告 | [`30535455060`](https://github.com/weiran22222/study-track/actions/runs/30535455060)：`success` | 符合预期 |
| 4 | 关闭后重新打开，触发 `reopened`；head 与正确 body 均未改变 | [`30535503775`](https://github.com/weiran22222/study-track/actions/runs/30535503775)：`success` | 符合预期 |
| 5 | 推送已独立 `PASS` 的新 head，触发 `synchronize`；body 仍引用旧 head `8f707dced5348ddcff9026eb33c2b08c56ea186b` | [`30536062704`](https://github.com/weiran22222/study-track/actions/runs/30536062704)：`failure`；失败步骤为 `Check the current evaluator report` | 符合预期 |
| 6 | `edited`；body 更新为新 head `f0d1818d2af9398c697d2c33519d6d11968eeecb` 的完整 evaluator `PASS` | [`30536126659`](https://github.com/weiran22222/study-track/actions/runs/30536126659)：`success` | 符合预期 |

步骤 2 的失败诊断中，`Location` 为
`Subject SHA in PR body: 0000000000000000000000000000000000000000`，`Reason` 明确为
报告 Subject SHA 与 PR head `8f707dced5348ddcff9026eb33c2b08c56ea186b` 不匹配；输出
包含 `Location`、`Invariant`、`Reason`、`Fix`、`Recheck` 与 `Authority` 六个字段。失败 run
`30535404918` 保持为独立失败证据；步骤 3 是由恢复正确 body 后的新 `edited` event
产生的 run `30535455060`，不是对步骤 2 失败 run 的重跑。

步骤 5 的失败诊断中，`Location` 为
`Subject SHA in PR body: 8f707dced5348ddcff9026eb33c2b08c56ea186b`，`Reason` 明确为
旧报告 SHA 与新 PR head `f0d1818d2af9398c697d2c33519d6d11968eeecb` 不匹配；输出
同样包含 `Location`、`Invariant`、`Reason`、`Fix`、`Recheck` 与 `Authority` 六个
字段。失败 run `30536062704` 保持为独立失败证据；步骤 6 是更新 body 后由新的
`edited` event 产生的 run `30536126659`，不是对步骤 5 失败 run 的重跑。

本次六步 fail-closed 远程操作演练符合预注册预期：正确且与 head 绑定的报告在
`opened`、恢复后的 `edited` 与 `reopened` 中成功，全零或陈旧 SHA 分别在独立
`edited` 与 `synchronize` 中失败，更新为新 head 的独立 evaluator `PASS` 后再由独立
`edited` 恢复成功。这个窄结论只描述 PR #57 上六次 event/run 的操作事实，不证明
evaluator 身份、报告真实性、验证完整性，也不构成 Harness 正向因果效果。

本次最终记录提交尚未推送；它及其之后可能产生的 `synchronize`、`edited`、其他远程
run、评论、合并或最终 `develop` 结果均尚未发生，本文不预写其 head、run 或结论。

## 5. 停止条件与反误报规则

出现以下任一情况将立即停止后续演练并报告实际状态：

- PR base/head、body Subject SHA、事件 activity 或对应 Actions run 无法精确绑定；
- 步骤 2 或 5 的错误/陈旧 SHA 意外通过 `verify`；
- 正确且完整的当前报告在步骤 1、3、4 或 6 被报告门禁拒绝；
- 需要管理员绕过、修改 branch protection、创建替代 required status、禁用检查或修改
  workflow/脚本才能继续；
- 新提交无法在推送前取得不同 evaluator `PASS`；
- 发生与受控输入无关的 CI、平台、网络或仓库故障，导致本轮不能区分报告门禁语义。

失败 run 必须保留为失败证据。不得通过重跑同一 workflow、改正输入后重跑，或引用另一
次成功 run，把预期失败冒充为成功；恢复步骤必须由预注册的后续 `edited` 事件产生独立
run 并分别记录。平台或瞬时故障可以按其真实原因记录，但不能替代任何预注册事件，也不能
改写为报告门禁的预期结果。

不得在远程动作发生前填写 run URL、conclusion、评论、PR 状态或“符合预期”结论。实际
结果偏离预期时将原样保留，不通过删除评论、重写历史或增加未注册步骤展示成功。

## 6. 最终记录与自引用边界

本文件所在 PR 承载演练及其最终记录。本次提交只把已经发生且可定位的步骤 1～6 结果写入
本文；它本身会产生新的 PR head，并可能继续触发 `synchronize`、`edited` 或其他远程
运行。

为避免“记录新运行又制造新运行”的无限自引用，最终记录提交及其之后产生的 PR event、
Actions run、评论、合并或最终 `develop` push 结果不会再通过新的提交回写本文。这些
后续事实只以 GitHub PR、Actions、Check Run 和评论为权威。本文最终结论只覆盖明确写出的
演练截止点，不把承载记录本身的后续远程状态推断为已验证。

六个预注册步骤已由上述权威记录精确定位，本文只得出“本次 fail-closed 远程操作演练符合
预期”的窄结论，不据此给出 Harness 正向因果效果结论。
