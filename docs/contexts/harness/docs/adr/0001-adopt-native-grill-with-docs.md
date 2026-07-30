---
status: accepted
date: 2026-07-30
---

# Adopt native grill-with-docs

StudyTrack 将采用上游原生 `grill-with-docs` 组合来帮助复杂设计先形成共享理解，再进入
实施。该决定已经接受；本规划 PR 只建立词汇、ADR 与迁移计划，不激活工作流权限。

## Decision

### Native mode and explicit opt-in

采用原生模式：`grill-with-docs` 组合 `grilling` 的逐问访谈与 `domain-modeling` 的即时
词汇表/ADR 记录，不在仓库中重新实现包装器。它只能由人类显式调用，不自动触发，也不
成为普通任务、简单任务或所有变更的通用门禁。

会话一次只提出一个需要人类决定的问题，并附推荐答案；能从仓库或工具查明的事实由
facilitator 自行查明。只有人类明确确认已经形成共享理解，才能退出访谈并进入实施交接。
未解决的决定分支、词汇冲突或未记录的关键决定都会阻止退出。

### Pinned upstream inputs

学习与运行语义固定到 `mattpocock/skills` commit
`2ab958093e83e0ec752e6c1c5932da465bf23e0c` 的以下路径：

- [`skills/engineering/grill-with-docs/SKILL.md`](https://github.com/mattpocock/skills/blob/2ab958093e83e0ec752e6c1c5932da465bf23e0c/skills/engineering/grill-with-docs/SKILL.md)；
- [`skills/productivity/grilling/SKILL.md`](https://github.com/mattpocock/skills/blob/2ab958093e83e0ec752e6c1c5932da465bf23e0c/skills/productivity/grilling/SKILL.md)；
- [`skills/engineering/domain-modeling/SKILL.md`](https://github.com/mattpocock/skills/blob/2ab958093e83e0ec752e6c1c5932da465bf23e0c/skills/engineering/domain-modeling/SKILL.md)。

这些上游技能是固定学习输入，不自动成为本仓库权威。仓库不 vendoring、复制或改写上游
skill 文件；升级快照必须经过新的本地 Harness 决定和受保护 PR。技能是否可用属于运行
环境前提，不由本仓库静默安装或修复。

### Decision cards and ADRs coexist

现有 `docs/decisions/` 决策卡继续解释跨上下文 Harness 与仓库决策的历史理由。context
ADR 只记录所属上下文中难以逆转、缺少背景会令人意外且经过真实取舍的决定。两者并存，
不互相替代；已有 28 张决策卡保持原路径与内容，不迁移、不重编号，也不批量改写为 ADR。

词汇表仍然只定义术语。产品行为留在 `SPEC.md`，当前 Harness 协议留在 `HARNESS.md` 与
`WORKFLOW.md`，实施步骤留在执行计划，避免 context 文档成为第二套规格或工作流。

### Planned permissions and exit gate

第二个迁移 PR 落地后，coordinator 的新增分支自主权仅限于：从已经远程验证并安全更新的
干净 `develop` 创建并切换到干净的 `codex/*` 分支。该权限不包括从其他基线创建分支，
不允许在脏工作树中切换，也不扩大 stage、commit、push、PR、merge、rebase、
cherry-pick、GitHub 或发布权限。

在一次由人类显式调用的 grilling 会话中，facilitator 的写权限仅限于相关
`CONTEXT-MAP.md`、context `CONTEXT.md` 和 context `docs/adr/*.md`。它不得修改产品代码、
规格、架构、Harness 当前协议、工作流、CI、脚本、构建或远程配置；超出该范围必须停止并
取得新的明确授权。写入只保存已经由人类解决的术语和决定，不能把推荐答案冒充为批准。

共享理解是显式退出门禁，而不是 facilitator 的主观判断。人类负责确认：目标与非目标、
关键术语、决定分支、取舍和待实施范围已经足够清楚；确认前不得开始实现、创建实现交接或
宣称规划完成。

### Two-PR delivery and responsibilities

采用两个受保护 PR：

1. 规划 PR 建立 context map、两个最小词汇表、本 ADR、进行中的迁移计划和索引，不改变
   当前权限或工作流；
2. 迁移 PR 才能更新稳定 Harness/工作流导航、激活上述窄权限、增加机械文档检查，并按
   generator/evaluator、冻结 Subject SHA 和 required `verify` 门禁交付。

人类决定是否显式调用 grilling、回答真正的决定问题、确认共享理解并掌握产品边界、重大
Harness 取舍与发布。facilitator 负责逐问澄清、先自行查明事实、维护获准的词汇表/ADR，
并在退出门禁前停止实施。coordinator 负责经允许的分支准备、最小交接、冻结 Subject SHA
和失败回流。generator 只实施批准的迁移范围并报告自检；不同 evaluator 对冻结 SHA 做
只读独立验证，不能由 facilitator、coordinator 或 generator 的判断替代。

## Effect hypothesis

在不把 grilling 变成通用强制门禁的前提下，原生词汇表和即时 ADR 预计能减少复杂设计中
的术语歧义、重复澄清和决策理由丢失；新增文档与访谈成本不能明显增加简单任务负担。

落地后的前三次显式 grilling 会话是前瞻观察单元：至少一次 StudyTrack 产品主题、至少
一次 Harness 主题，第三次主题不限。观察人类澄清往返、术语冲突发现、后续 generator
是否仍需重新询问、ADR 是否真正被复用，以及文档熵。当前没有可靠量化基线，不倒推历史
耗时或节省比例。结论仅使用 `正向 | 混合 | 无明显效果 | 负向 | 证据不足`；落地、
`verify`、evaluator `PASS` 或 PR 合并不能单独证明正向效果。

## Considered options

- **继续只进行不留记录的 grilling**：访谈能澄清当次思路，但共享语言和决定理由会随
  会话消失；
- **在仓库 vendoring 或重写三个技能**：能把实现固定在仓库，却引入重复来源、升级负担
  和本地漂移；
- **把 grilling 设为所有工作的强制门禁**：覆盖最广，但会给简单任务增加无关访谈和
  文档成本；
- **迁移现有决策卡为 ADR**：形式统一，但会制造大范围历史重写、链接漂移与无新增信息
  的文档 churn。

选择显式 opt-in 的原生组合、固定上游快照、Decision/ADR 共存和前瞻效果观察，以保留
共享理解收益，同时限制权限、熵与持续维护成本。

## Consequences

复杂设计多一个由人类控制的澄清入口，并能把稳定词汇和重大取舍留在对应 context；简单
任务保持原流程。仓库需要维护 context 导航、少量 ADR 和固定上游引用，但不承担 skill
源码副本。若迁移 PR 尚未落地，本 ADR 中的分支自主权、facilitator 写权限与退出门禁均
只是计划语义，不得按已激活规则执行。
