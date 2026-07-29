# 决策卡 022：简化 generator/evaluator 交接

状态：已批准

日期：2026-07-29

## 问题

决策卡 021 正确地要求不同 generator 实现、不同 evaluator 验证同一冻结提交，但第一版
又把活跃协议绑定到分支名、共享或隔离方式以及特定 worktree 生命周期。这些是运行环境
选择，不是验证对象或智能体身份；把它们写进 guard 和 handoff 增加了机械约束与交接噪音。

## 人类决定

学习者明确决定：

- 活跃 Harness 不再保留 worktree 相关内容；
- generator 与不同 evaluator 仍是需要修改仓库任务的必要角色；
- 是否新建或切换 feature 分支由人类操作或明确指示，协调者不得主动决定；
- 保持流程精简，不把交接和验证做得更复杂。

这些决定确定了目标和权限边界，不表示学习者逐项指定了 Harness 风险等级、PR 拆分或工件
数量。

## 决定与理由

- 继续要求需要修改仓库的任务由一个 generator 实现、不同 evaluator 独立验证；只有精确
  Subject SHA 才稳定标识被验证内容，分支名和文件系统布局不能证明角色身份；
- guard 只接收一个完整 Subject SHA，并只读确认它解析为提交、`HEAD` 精确相等、工作树
  干净、暂存区为空，不检查或修复分支和运行模式；
- generator 最小交接只保留 Task、Acceptance criteria、Allowed scope、Prohibitions；
  evaluator 最小交接只保留 Task、Acceptance criteria、Subject SHA、Generator、
  Evaluator、Mutation allowed: no；
- evaluator 报告继续记录精确 SHA、命令、独立场景、发现、残余缺口和
  `PASS | FAIL | INCONCLUSIVE`；任何新 SHA 都使旧报告失效；
- 是否创建或切换分支由人类决定。协调者只有收到明确指令后才能操作；一个对话线程对应
  一个分支仍可作为默认用法，但不成为 Harness 的机械验证条件；
- 协调者可自主选择额外子智能体和并行方式，但 generator 写入、冻结 Subject SHA、
  evaluator 只读验证必须串行，避免验证对象在验证中变化。

保留 SHA 绑定是为了让 evaluator 结论、required `verify` 和 PR head 指向同一不可变
内容；保留角色分离是为了减少实现假设进入最终本地判断。两者均不依赖分支或 worktree
模式，也不声称能提供密码学或平台级身份认证。

## 交付与边界

协调者依据现有 Harness 风险分级，将这项改变智能体权限与交接语义的工作按第三级处理；
再依据[决策卡 011](011-lean-tier3-artifacts.md) 的精简原则，选择在一个受保护 PR 中交付
本卡、短执行计划和实现，不创建额外证据文件。这是协调者对现有流程的应用，不是对学习者
决定内容的扩张，也不声称学习者逐项批准了该 PR 与工件安排。

决策、范围与验收已经明确，拆成规划 PR 与功能 PR 只会重复交接，不增加有效证据。按
决策卡 011，功能 diff 可以归档已经发生的本地实施与 generator 自检；evaluator、
required CI、PR 合并及其他远端事实仍以实际发生后的 GitHub 记录为准，不在仓库中预写。

历史决策 004、018、019、021 与已完成计划 018 保留；019 由本卡取代，021 中角色分离和
同 SHA 语义继续有效，分支/worktree 与冗长交接部分由本卡取代。本次不改变 `SPEC.md`、
`ARCHITECTURE.md`、产品代码、CI Job 身份、GitHub 权限、分支保护或分支流。

## 回退

若轻量交接无法明确任务或精确 SHA guard 不能可靠保护验证对象，应通过新的受保护 Harness
变更补充必要字段或检查；不得让同一智能体兼任两角色、复用旧 SHA 报告、绕过 required
`verify`，也不得用分支名或运行环境冒充验证身份。
