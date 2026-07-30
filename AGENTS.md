# StudyTrack 智能体导航

## 项目目标

项目终极目标是验证并演进 Harness 落地效果，详见
[HARNESS.md](HARNESS.md)。StudyTrack CLI 是受控实验载体；其产品行为仍只由
[SPEC.md](SPEC.md) 定义。

## 开始工作前：文档地图

按任务需要选择第一跳，不默认加载全部文档：

| 分类 | 第一跳 | 职责与读取时机 |
|---|---|---|
| 导航与原则 | [AGENTS.md](AGENTS.md) | 项目目标、文档地图和根本原则；每项任务先读。 |
| 工作约定 | [WORKFLOW.md](WORKFLOW.md) | 权限、角色、门禁和命令；任何仓库修改前必须读取。 |
| 当前权威事实 | [HARNESS.md](HARNESS.md) | 项目目的与 Harness 效果评估协议；评估或调整 Harness 时读。 |
| 当前权威事实 | [SPEC.md](SPEC.md) | 产品行为、输入输出和验收标准；涉及产品范围或行为时读。 |
| 当前权威事实 | [ARCHITECTURE.md](ARCHITECTURE.md) | 技术选择、分层、依赖与验证流水线；改代码、依赖或架构时读。 |
| 上下文导航 | [CONTEXT-MAP.md](CONTEXT-MAP.md) | 上下文关系与通用语言入口；处理跨上下文术语或复杂设计时按需读取。 |
| 辅助说明 | [docs/environment.md](docs/environment.md) | JDK 21、Wrapper 和环境诊断；首次进入或构建环境变化时读。 |
| 历史入口 | [docs/README.md](docs/README.md) | 历史工件的中央索引；需要理解现状形成原因时从这里渐进查阅。 |
| 历史工件 | `docs/decisions/` | 为什么选择及取舍；从索引选择与当前问题相关的决策。 |
| 历史工件 | `docs/exec-plans/` | 实施步骤、风险和验证方法；执行或复核相关多步骤工作时读。 |
| 历史工件 | `docs/evidence/` | 已执行命令、结果、哈希和证据边界；核对历史实施事实时读。 |
| 历史工件 | `docs/feedback/` | 复盘、效果观察和残余缺口；评估 Harness 结果时读。 |

当前事实优先读取根级稳定文档；历史工件可能被后续决定取代，不能凌驾于当前权威事实。
本地图只提供第一跳，不复制各文档协议、不逐项列出历史工件，也不形成完整文件清单。

## 根本原则

- 人类掌舵并决定目标、产品边界、重大取舍与发布；智能体只在批准范围内执行，不因技术
  可行就扩大范围。
- 当前事实以 `HARNESS.md`、`SPEC.md` 和 `ARCHITECTURE.md` 为权威；历史工件只解释
  理由，冲突时不得凌驾于当前权威文档。
- 产品行为与架构变化必须分别先更新 `SPEC.md` 与 `ARCHITECTURE.md`，再实施和验收。
- 受保护 PR、required `verify` 与适用门禁不得绕过；本地 `develop` 不得直接 merge、
  rebase 或 cherry-pick 功能分支。
- 只记录真实执行的证据，不把未发生的检查或远程状态写成成功；generator 与不同
  evaluator 必须围绕同一冻结 Subject SHA 依次实施和独立验证。
- 任何仓库修改前必须完整读取 [WORKFLOW.md](WORKFLOW.md)，其操作性规则不得由本摘要
  放松或重解释。
