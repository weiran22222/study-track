# `list --contains` 与精简导航后的 Harness 前瞻观察

状态：已完成（证据不足）

日期：2026-07-29

本文回填 `list --contains <text>` 规划阶段预注册的观察口径，覆盖规划 PR、功能 PR 和功能
合并后的最终 `develop` 验证。观察单元已经结束；本次观察收尾内容、其受保护 PR、CI、
合并和后续最终 `develop` 验证均不属于观察单元；本文不记录或声称承载自身的 PR、CI、
合并或最终 `develop` 远程事实。

## 1. 变化与假设

被观察的 Harness 变化是[决策 026](../decisions/026-slim-agent-navigation.md)落地后的精简
入口：`AGENTS.md` 只保留目标、文档地图和根本原则，仓库修改者必须从地图跳转并完整读取
`WORKFLOW.md`；当前标准流程继续使用规格先行、最小 generator 交接、冻结 Subject SHA、
不同 evaluator 和受保护 `develop` PR。

`list --contains` 是该变化后的首个前瞻真实产品样本，而不是为了观察而扩大的 Harness 或
产品能力。预注册假设是：精简入口和明确的权威分工可能让参与者在较少非计划人工补救下
找到正确边界、完成可机械验收的实现，并留下可复现、可追踪的反馈链。观察也主动寻找漏读
`WORKFLOW.md`、协议漂移、越界实现、无效检查、重复工件和新增认知成本等反例。

## 2. 观察单元

观察单元从人类批准产品规格与边界开始，到功能合并后的精确最终 `develop` push
`verify` 成功为止：

| 阶段 | 冻结 head | generator 与 evaluator | 受保护 PR、合并与最终验证 |
|---|---|---|---|
| 规划 | [`e030cab7a6ac6bc3be93a8b9aeed151d3f6ee1f5`](https://github.com/weiran22222/study-track/commit/e030cab7a6ac6bc3be93a8b9aeed151d3f6ee1f5) | generator 在 JDK 21 下完整 `verify` 为 132/132；不同 evaluator `PASS`，定向 7/7、完整 132/132，无规划 finding；前后 guard 均绑定该 SHA | [PR #47](https://github.com/weiran22222/study-track/pull/47) 的分支 push [run 30448735930](https://github.com/weiran22222/study-track/actions/runs/30448735930) 与 PR [run 30448751053](https://github.com/weiran22222/study-track/actions/runs/30448751053) 均为 `verify/success`；人类决定合并为 `383dbd2cd4d00641a389148f766bdf4a9e3a3409`，最终 develop push [run 30448960186](https://github.com/weiran22222/study-track/actions/runs/30448960186) 为 `verify/success` |
| 功能 | [`c5bd758e6bc0c57aaa7493d9f00e31153efe7855`](https://github.com/weiran22222/study-track/commit/c5bd758e6bc0c57aaa7493d9f00e31153efe7855) | generator 定向 68/68、完整 139/139，并有一次由 5 条 Checkstyle 使用距离告警触发的测试代码修复轮次；不同 evaluator `PASS`，定向 68/68、完整 139/139，无缺陷 finding；前后 guard 均绑定该 SHA | [PR #48](https://github.com/weiran22222/study-track/pull/48) 的分支 push [run 30450528279](https://github.com/weiran22222/study-track/actions/runs/30450528279) 与 PR [run 30450546994](https://github.com/weiran22222/study-track/actions/runs/30450546994) 均为 `verify/success`；人类决定合并为 `d9f6f08cc8c785e14989a2c93d410d3b06f4a209`，最终 develop push [run 30453206695](https://github.com/weiran22222/study-track/actions/runs/30453206695) 为 `verify/success` |

上述六次 workflow run 是观察单元的全部远程运行计数。承载本收尾工件的 PR 及其运行、
合并或最终 `develop` 状态不纳入观察单元，本文不记录或声称这些自身远程事实。

## 3. 适用维度

本观察适用 `HARNESS.md` 的全部六个维度：结果正确性、自主性与人类掌舵负担、反馈回路
有效性、可复现性与可追踪性、交付效率、熵与维护成本。各维度均在第 5 节分别记录实际
信号和因果边界；没有任何单一绿灯、`PASS`、合并或计数被当作效果证明。

## 4. 基线状态

**无可靠可比较基线。** [当前 Harness 效果基线](005-current-harness-effect-baseline.md)中的
PR #38～#43 都是异质的 Harness、流程或文档任务，不是相同复杂度和风险边界的标题搜索
产品任务；更早产品任务又使用不同流程和导航，无法构成可靠对照。

本观察只建立精简导航后首个前瞻产品样本的现状。现有记录没有提供同等正确性和风险边界下
可比较的有效工作时间、人工等待、人工操作或缺陷率，因此不能倒推节省比例、效率或正确性
改善。

## 5. 实际结果与证据

规格批准后没有发生额外产品语义澄清，也没有发生人工微观指挥。人类选择产品边界、当前
分支、是否合并规划 PR 以及是否合并功能 PR，都是必须保留的治理决定，不计为非计划补救。
generator 在冷启动时没有需要人类提示第一跳或指出权威文档，实施范围保持在批准边界内。

本地 generator 的功能命令、结果、哈希和 Windows Unicode 边界见
[证据 008](../evidence/008-list-title-search.md)。PR、Actions、冻结 SHA 与 evaluator
报告分别提供远程和独立验证定位；本文只是汇总已发生事实，不重新执行或替代 evaluator。

六个维度的实际信号与因果边界如下：

| 维度 | 实际信号 | 因果边界 |
|---|---|---|
| 结果正确性 | 规划与功能 generator 自检、不同 evaluator、前后 guard、两个受保护 PR 的检查和两个最终 develop push 运行均可绑定各自冻结 SHA；功能 evaluator 没有缺陷 finding。 | 这些事实证明样本通过既定门禁；无同类基线，不能证明精简导航或当前 Harness 提高了正确性，也不能排除验收范围外缺陷。 |
| 自主性与人类掌舵负担 | generator 冷启动未需人工导航提示，范围正确；规格批准后无额外产品语义澄清、人工微观指挥或非计划补救。 | 单样本没有可比较的澄清、补救、审查往返或人工操作基线；必需的人类边界、分支和合并决定不能被误计为负担改善。 |
| 反馈回路有效性 | 功能 generator 自检暴露 5 条 Checkstyle 使用距离告警，并在一次测试代码修复轮次后收敛；两个 evaluator 和六次 CI 运行均给出可定位结果。 | Checkstyle 修复是本样本的操作信号；evaluator 无 finding 和 CI 绿色不提供同类缺陷捕获率、漏检率或对照，不能证明反馈回路相对改善。 |
| 可复现性与可追踪性 | 规划和功能各自的 frozen head、前后 guard、generator/evaluator 测试数、PR、push/PR run、merge SHA 与最终 develop run 可连续定位。 | 这是操作上积极的可追踪信号；本文没有跨环境重做验证，也没有历史对照，不能作因果改善结论。 |
| 交付效率 | 功能修复轮次可计为一次，两个受保护 PR 完成相应门禁。 | 没有可比有效工作时间、缺陷率或可分离的人类等待；created-to-merged 混入未知等待，不用于效率推断。 |
| 熵与维护成本 | 精简导航下未发生导航补救，计划在观察结束后归档；同时新增规格、历史工件、实现、测试和运行表面。 | 尚无长期漂移、认知负担、维护频率或同类导航对照；不能判断精简收益是否抵消新增成本。 |

三个过程问题必须保留在正确边界内：

- generator 首次环境自检使用继承的默认 Java 17；随后只为命令进程选择 JDK 21 并完成
  验证。这是本机环境选择，不是产品缺陷，也不能冒充 Harness 改善；
- 规划 evaluator 的 PowerShell Maven 定向调用需要修正引号，使属性作为完整参数传入后
  才得到 7/7 结果。这是验证工具调用边界，不是规划 finding、产品缺陷或改善证明；
- Windows 原生命令行不能在本轮手工 JAR 场景中可靠传递 200 个补充平面内联字符；
  200/201 码点语义由 JVM 内自动测试覆盖。这是已知证据边界，不授权扩大产品范围，也不
  能写成 Harness 改善。

## 6. 反例与残余缺口

- 默认 Java 17、规划 evaluator 的 PowerShell 引号修正和 Windows 补充平面原生命令行
  传输限制都表明环境与验证工具仍有摩擦；
- 功能 evaluator 无缺陷 finding，只说明独立场景没有发现规格内缺陷，不证明不存在漏检；
- 六次成功 workflow run 与两个成功合并是门禁事实，不是 Harness 正向效果的反事实；
- 没有同类基线、可比有效工作时间、等待拆分、人工负担测量或缺陷率；
- 单样本无法隔离精简导航、任务难度、操作者、工具状态和既有测试基础的影响；
- 承载本收尾工件的 PR、CI、合并或最终 `develop` 远程事实不由本文记录或声称，也不回扩
  观察单元。

## 7. 维护成本

观察单元包含 **2 个受保护 PR** 和 **6 次 workflow run**：每个 PR 各有一次分支 push
`verify`、一次 `pull_request` `verify` 和一次合并后最终 develop push `verify`。这些是
明确的运行、协调和等待成本，不能作为效果证明。

仓库表面新增或修改了规格、决策、计划、反馈、证据和文档索引；产品实现修改了
`StudyTaskService` 与 `ListCommand` 两个生产行为文件，并新增一个专用异常类型；两个
测试文件新增或修改，共新增 7 个自动测试。Windows Unicode 残余边界还要求保留自动测试
与证据说明，构成持续认知和维护成本。

created-to-merged 历时不用于效率推断。可明确的人类操作是产品边界、当前分支、规划合并
和功能合并决定；其中混入的人类审查与等待时长没有被可靠拆分，未知部分明确为未知，不
补造有效工作时间或等待数字。

## 8. 结论

**证据不足。**

本样本提供了操作上积极的信号：generator 无需人工导航补救且范围正确，Checkstyle
自检让一次测试代码修复轮次收敛，evaluator、CI 与最终 SHA 链条可追踪。它也保留了环境、
PowerShell 调用和 Windows Unicode 的真实边界，以及 2 个 PR、6 次运行和新增维护表面。
由于没有同类基线、可比较有效工作时间、人工等待或缺陷率，不能把这些操作信号写成
Harness 的正向因果效果。
