# Context Map

本仓库包含 StudyTrack 产品与 Harness 工程两个上下文。这里仅导航各自的通用语言与关系；
产品行为以 [SPEC.md](SPEC.md) 为准，Harness 当前规则与效果协议以
[HARNESS.md](HARNESS.md) 和 [WORKFLOW.md](WORKFLOW.md) 为准。

## Contexts

- [StudyTrack](docs/contexts/study-track/CONTEXT.md)：描述个人学习事项的产品语言；
- [Harness](docs/contexts/harness/CONTEXT.md)：描述人类掌舵、智能体交付与效果观察的协作
  语言。

## Relationships

- **Harness → StudyTrack**：Harness 把 StudyTrack 作为受控实验载体，但不拥有或改写
  StudyTrack 的产品语义；
- **StudyTrack → Harness**：StudyTrack 提供可机械验收的变更主题，Harness 负责交付与
  观察这些变更时使用的协作语义；
- **术语隔离**：StudyTrack 中使用“学习任务”，Harness 中使用“Codex task”；不得用未
  限定的“任务”混指两者。
