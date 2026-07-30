# StudyTrack Harness 目的与效果评估

## 权威边界

本仓库的终极目标是学习、验证并演进 Harness Engineering 实践，判断 Harness 变化落地后
是否产生可审计的实际效果。StudyTrack CLI 是边界稳定、结果可机械验收的受控实验载体，
不是项目终极目的，也不因 Harness 实验而自动扩大产品范围。

- [SPEC.md](SPEC.md) 是当前完整产品行为与验收标准的唯一权威；
- [ARCHITECTURE.md](ARCHITECTURE.md) 定义技术选择、分层和依赖；
- [AGENTS.md](AGENTS.md) 是智能体使用的稳定导航地图与工作约定；
- 本文是项目 Harness 终极目的与效果评估协议的稳定仓库级权威；
- [决策卡 024](docs/decisions/024-harness-effect-validation-goal.md)记录本协议的历史理由。

本文不复制产品协议。Harness 实验需要改变 CLI 行为时，必须先由人类批准并更新
`SPEC.md` 及其验收标准。

## 上游学习输入与本地采用

主要学习输入是 `deusyu/harness-engineering` 的固定提交
`90208d60687e47eb350606a584837e4cce7ab403`。固定文件链接保留本协议形成时的学习快照：

- [总览与人类掌舵、智能体执行](https://github.com/deusyu/harness-engineering/blob/90208d60687e47eb350606a584837e4cce7ab403/README.md)；
- [核心概念与反馈回路](https://github.com/deusyu/harness-engineering/blob/90208d60687e47eb350606a584837e4cce7ab403/concepts/00-overview.md)；
- [仓库即记录系统](https://github.com/deusyu/harness-engineering/blob/90208d60687e47eb350606a584837e4cce7ab403/concepts/01-repo-as-source-of-truth.md)；
- [机械化执行](https://github.com/deusyu/harness-engineering/blob/90208d60687e47eb350606a584837e4cce7ab403/concepts/02-mechanical-enforcement.md)；
- [智能体可读性](https://github.com/deusyu/harness-engineering/blob/90208d60687e47eb350606a584837e4cce7ab403/concepts/04-agent-readability.md)；
- [熵管理](https://github.com/deusyu/harness-engineering/blob/90208d60687e47eb350606a584837e4cce7ab403/concepts/03-entropy-and-garbage-collection.md)。

这些材料是学习输入，不自动成为本仓库权威。任何实践只有经过本地人类决定、风险分级、
受保护 PR 和验证后才成为本仓库规则；冲突时由本仓库当前权威文档决定。不得直接复制上游
的结构、指标、流程或数字来替代本地验证。

本仓库采用以下方向，而不照抄上游实现：

- **人类掌舵、智能体执行**：人类决定目的、产品边界、重大取舍与发布，智能体在批准边界
  内实施和反馈；
- **仓库即记录系统**：长期有效且需要被智能体使用的事实进入版本化仓库，远程事实保留在
  对应 GitHub 权威记录；
- **地图而非手册**：`AGENTS.md` 只导航到按需读取的权威材料，不堆叠完整协议；
- **机械化执行**：对高价值不变量使用测试、lint、guard 和 CI，错误提供可行动的修复
  反馈；
- **智能体可读性**：使用明确边界、稳定入口、可复现命令和易诊断结果降低推理歧义；
- **反馈回路**：失败、审查和 evaluator 发现必须能定位根因并进入下一轮改进；
- **熵管理**：新增规则也要检查重复、漂移、脆弱性、认知负担和持续维护成本。

## 原生 grill-with-docs 学习输入

复杂设计的澄清入口采用 `mattpocock/skills` 固定提交
`2ab958093e83e0ec752e6c1c5932da465bf23e0c` 的原生 `grill-with-docs` 组合：

- [`skills/engineering/grill-with-docs/SKILL.md`](https://github.com/mattpocock/skills/blob/2ab958093e83e0ec752e6c1c5932da465bf23e0c/skills/engineering/grill-with-docs/SKILL.md)；
- [`skills/productivity/grilling/SKILL.md`](https://github.com/mattpocock/skills/blob/2ab958093e83e0ec752e6c1c5932da465bf23e0c/skills/productivity/grilling/SKILL.md)；
- [`skills/engineering/domain-modeling/SKILL.md`](https://github.com/mattpocock/skills/blob/2ab958093e83e0ec752e6c1c5932da465bf23e0c/skills/engineering/domain-modeling/SKILL.md)。

它原生组合 `grilling` 的逐问访谈与 `domain-modeling` 的即时词汇表/ADR 记录，只能由
人类显式调用，是复杂设计的 opt-in 工具，不自动触发，也不是普通任务、简单任务或所有
变更的通用门禁。固定快照是学习与运行语义输入，不自动成为本仓库权威；当前协议仍由
本文与 [WORKFLOW.md](WORKFLOW.md) 决定。

仓库不 vendoring、复制、包装或改写三个上游 skill 文件。用户显式管理运行环境中的技能
安装与版本；智能体不得静默安装或更新。固定快照升级必须经过新的本地 Harness 决定和
受保护 PR。

落地后的前三次由人类显式调用的 grilling 会话是前瞻观察单元：至少一次 StudyTrack
产品主题、至少一次 Harness 主题，第三次主题不限。观察人类澄清往返、术语冲突发现、
后续 generator 是否仍需重新询问、ADR 是否真正被复用，以及文档熵。当前无可靠量化
基线，不倒推历史耗时或节省比例；保留反例、维护成本与无法归因之处，结论只使用本文
规定的五种值。落地、绿色 `verify`、evaluator `PASS` 或 PR 合并都不能单独证明正向
效果。

## 落地与效果

**落地**表示 Harness 变化已经通过适用门禁进入目标分支。**效果**表示它在预先说明的
观察单元中带来了可追踪结果。落地是评估前提，不是正向效果证明。

评估按与假设相关的维度观察，不强制合成总分：

1. **结果正确性**：产出是否符合批准的产品、架构、权限和验收边界，是否避免漏测、
   越界、假成功和回归；
2. **自主性与人类掌舵负担**：目标明确后，是否减少重复澄清、人工补救、微观指挥和审查
   往返，同时保留必须由人类作出的决定；
3. **反馈回路有效性**：检查是否及时发现真实问题，说明位置、不变量、原因和修复方向，
   并帮助后续尝试收敛；
4. **可复现性与可追踪性**：不同 evaluator 是否能从仓库、精确 Subject SHA、命令和
   权威记录重建验证对象、结果与结论边界；
5. **交付效率**：在相同正确性和风险边界下，历时、修复轮次、等待及人工操作是否改善；
6. **熵与维护成本**：是否增加重复协议、文档漂移、脆弱检查、持续运行成本、认知负担或
   后续清理工作。

某一维度改善不自动抵消其他维度退化。结论必须说明实际权衡和适用边界。

## 最小评估声明

评估 Harness 变化时，使用以下最小字段；字段可记录在能稳定定位的既有工件中，不要求
创建新的表格或文件：

1. **变化与假设**：改变了什么，预期通过什么机制影响什么结果；
2. **观察单元**：哪项任务、变更、时间段或可比较场景承载观察；
3. **适用维度**：六个维度中哪些与本次假设有关，哪些不评估；
4. **基线状态**：写明可靠基线及来源；没有可靠基线时明确记录“无基线”，只建立现状，
   不倒推历史；
5. **实际结果与证据**：记录观察结果及可定位的 SHA、命令、PR、Actions、报告或反馈；
6. **反例与残余缺口**：记录不支持假设的结果、覆盖缺口和无法归因之处；
7. **维护成本**：记录新增规则、运行、认知和后续维护成本，以及已知权衡；
8. **结论**：只使用 `正向 | 混合 | 无明显效果 | 负向 | 证据不足`。

“证据不足”是有效结论，不能被当作通过；没有基线或可归因证据时，不得补造耗时、干预
次数、缺陷率、节省比例或其他历史测量。

## 证据复用与反指标

默认复用 PR diff/review、GitHub Actions、冻结 Subject SHA、generator/evaluator 报告和
现有反馈记录。它们已覆盖评估字段时，不强制创建重复 evidence 文件。只有跨多个 PR 的
长期观察、结果偏差、需要保留的新知识或现有记录无法表达的外部状态，才增加专门工件。
本协议不要求新增服务、遥测平台、远程权限或持续数据采集。

反指标：PR 数、提交数、代码量、文档数量、检查数量、智能体数量和流程步骤数量均不能
单独作为效果证明。一次绿色 `verify`、一次 evaluator `PASS` 或一次 PR 合并证明的是对应
交付门禁事实，也不能单独证明 Harness 产生正向效果。

评估应比较达到同等结果与风险边界时的质量、负担和成本，并保留负面结果与证据不足；
不得为了展示成功而扩大产品范围、增加无关流程或删除反例。
