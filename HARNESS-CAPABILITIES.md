# StudyTrack Harness 能力与信任边界图

本文是当前 Harness 表面的稳定第二层地图：按任务触发导航到权威来源，并标出机械检查能
证明什么、不能证明什么。它只描述与导航，不授予权限、不新增规则或能力，也不能替代
[WORKFLOW.md](WORKFLOW.md)、[HARNESS.md](HARNESS.md)、[SPEC.md](SPEC.md) 或
[ARCHITECTURE.md](ARCHITECTURE.md)。

## 权威来源与触发条件

| 关注点 | 触发条件 | 第一权威来源 |
|---|---|---|
| 项目目的与 Harness 效果 | 评估或调整 Harness | [HARNESS.md](HARNESS.md) |
| 产品行为与验收 | 改变或判断 CLI 范围、输入输出或完成标准 | [SPEC.md](SPEC.md) |
| 技术结构与构建流水线 | 改代码、依赖、分层或架构 | [ARCHITECTURE.md](ARCHITECTURE.md) |
| 权限、角色、门禁与命令 | 任何仓库修改 | [WORKFLOW.md](WORKFLOW.md) |
| 默认导航与根本原则 | 每项任务开始 | [AGENTS.md](AGENTS.md) |
| 跨上下文术语与复杂设计 | 术语冲突或复杂跨上下文设计 | [CONTEXT-MAP.md](CONTEXT-MAP.md) |
| 历史理由、实施与观察 | 需要解释当前事实的形成原因或核对历史证据 | [docs/README.md](docs/README.md) |

历史记录可能被后续决定取代；它们解释理由和已发生事实，不能凌驾于当前权威来源。

## 当前能力表面

| 类别 | 仓库当前提供的表面 | 使用触发 | 信任边界 |
|---|---|---|---|
| 上下文与指导 | `AGENTS.md` 提供第一跳；`CONTEXT-MAP.md` 导航上下文与通用语言；`docs/README.md` 渐进披露历史 | 任务冷启动；出现术语或历史理由问题时继续下钻 | 导航帮助定位规则，本身不创造权限或产品语义 |
| 机械反馈 | JDK 21 环境自检、Maven Wrapper `verify`、自动测试、架构/风格检查、仓库 Markdown 本地链接一致性门禁、验证对象 guard、PR 当前 evaluator 报告门禁与适用 CI 门禁 | 首次进入或环境变化；修改后；冻结 SHA 独立验证；PR 与长期分支更新 | 每次结果只覆盖实际命令、对象、环境和时间；链接门禁只证明已覆盖相对本地目标存在；报告门禁只证明 v1 envelope 存在、结构、SHA 绑定与自述 `PASS`；检查通过不证明验收范围外没有缺陷 |
| 编排与状态 | 风险分级、人类/generator/evaluator/协调者职责、`FROZEN(<Subject SHA>)` 状态与失败回流、分支流 | 仓库变更分级；实现交接；独立验证；合并或发布判断 | 协议和审计记录约束协作；Git guard 不认证参与者身份，也不替代 required `verify` |
| 工具与运行时 | 仓库内 Wrapper 与诊断脚本；原生 `grill-with-docs` 的固定学习输入和只读运行前诊断 | 构建环境检查；人类显式调用复杂设计会话 | 仓库只规定入口和诊断边界，不安装、启用或认证外部工具、插件或技能 |

机械反馈、CI 触发、分支规则和角色权限的完整当前协议只见
[WORKFLOW.md](WORKFLOW.md)；构建与运行时诊断见
[docs/environment.md](docs/environment.md)。

## 证据边界

- 仓库文档说明的是当前约定，不是某次运行已经发生的证据。
- 本地环境检查或 `verify` 结果只绑定实际执行时的机器、checkout 和命令；环境检查通过
  不等于完整门禁通过。
- Subject SHA 与前后 guard 可以证明 Git 对象、`HEAD`、工作树和暂存区满足检查条件，
  不能证明 generator/evaluator 的真实身份或独立性。
- PR evaluator 报告门禁可以证明 body 中唯一 v1 区的结构、非空字段、Subject SHA 与
  event head 绑定及自述 `Verdict: PASS`；它不能证明身份独立性、报告内容真实性、命令
  实际执行、场景完整或结论正确。
- PR、Check Run、Actions、分支保护和远端 SHA 等远程事实只以实际远端权威记录为准；
  本图不证明当前远程状态。
- 运行环境公开的插件、技能、连接、权限和沙箱信息只能在当前会话按可用诊断核对；仓库
  文件和本图都不证明其可用性或隔离强度。
- 计划、建议、重新评估条件和未完成事项不是已发生事实；本图不证明未来工作，也不授权
  执行它。

因此，本图不证明任何插件或技能在当前运行时可用，不证明智能体身份、沙箱状态、远程
状态或未来工作。

## 效果观察边界

[当前 Harness 效果基线](docs/feedback/005-current-harness-effect-baseline.md)和
[`list --contains` 前瞻观察](docs/feedback/006-list-title-search-observation.md)保留了
可追踪的操作信号、反例、成本与“证据不足”结论。缺少可靠同类基线限制了这些观察能得出
的因果结论，但不是尚待补齐的 Harness 能力缺口，也不形成未来工作要求。

按 [HARNESS.md](HARNESS.md#落地与效果)，正向因果证明不是 Harness 交付的完成条件；
落地门禁与效果结论承担不同职责，本图不要求把既有观察改写为正向结论。

## 已知部分覆盖与未覆盖

| 区域 | 当前边界 | 定位 |
|---|---|---|
| 最终原子替换失败 | 已直接覆盖临时写入失败、原文件保护与清理；最终 `Files.move` 失败没有确定性故障注入 | [决策 008](docs/decisions/008-accept-atomic-replacement-test-gap.md) |
| generator/evaluator 身份 | guard 和记录绑定精确 SHA 与工作树状态；仓库与 CI 没有密码学或平台级身份认证 | [决策 021](docs/decisions/021-generator-evaluator-role-separation.md#机械边界与身份边界) |
| 验证中状态与报告留存 | PR body 的 v1 门禁机械检查当前 `PASS` 报告存在、结构与 head SHA 绑定；历史 `FAIL`/`INCONCLUSIVE` 依赖协调者按真实报告追加评论，门禁不读取评论，也不认证身份或内容真实性 | [WORKFLOW 的报告生命周期](WORKFLOW.md#pr-evaluator-报告生命周期)、[决策 031](docs/decisions/031-pr-evaluator-report-lifecycle.md)、[决策 021 的报告边界](docs/decisions/021-generator-evaluator-role-separation.md#handoff-与验证报告) |
| 熵管理外循环 | 导航测试、覆盖根级 `*.md` 与 `docs/**/*.md` 的相对本地链接一致性门禁、人工反馈和计划归档提供反应式控制；首次人工 Harness 文档与协议熵审计只修正当前根级协议与导航断言，不批量改写历史工件；仓库不提供定时漂移扫描、质量评分或自动维护 PR 机制 | [决策 009](docs/decisions/009-documentation-entropy-control.md)、[决策 030](docs/decisions/030-repository-markdown-link-consistency.md)、[决策 032](docs/decisions/032-harness-entropy-audit.md)、[状态漂移复盘](docs/feedback/004-unicode-boundary-harness-retrospective.md#4-状态漂移的两级处理)、[历史索引](docs/README.md) |
| 干净机器冷启动 | 隔离 Maven 缓存的新 GitHub 克隆已在同一 Windows 主机成功完成自检与 `verify`；这仍非干净机器证明，隔离边界不包括 OS、JDK、网络、Git 凭据或宿主机状态 | [隔离 Maven 缓存冷启动演练](docs/feedback/008-isolated-cache-cold-start-drill.md)、[环境说明的证据边界](docs/environment.md#证据边界) |
| 原生技能运行时 | 固定学习输入和只读诊断已定义；仓库不保证技能已安装、启用或在后续会话可见 | [环境说明的运行前提](docs/environment.md#原生-grill-with-docs-运行前提) |
| Windows Unicode 手工链路 | JVM 自动测试覆盖 200/201 码点语义；原生命令行未可靠传递 200 个补充平面内联字符 | [`list --contains` 前瞻观察](docs/feedback/006-list-title-search-observation.md#6-反例与残余缺口) |

这些条目是已知边界的导航，不是待办清单、未来承诺或越过现有权限的修复授权。
