# Harness

Harness 上下文描述人类掌舵下，智能体交付仓库变更并观察其效果时使用的协作语言。这里只
定义术语，不保存工作流、权限、验证协议或实现细节。

## Language

**Harness**：
帮助智能体在经人类决定的边界内可靠工作的仓库级约束、指引与反馈回路。
_Avoid_：产品规格、自动驾驶

**Codex task**：
承载一次 Codex 协作目标及其上下文的工作单元。
_Avoid_：学习任务、未限定的“任务”

**generator**：
在批准范围内形成变更并执行自检的智能体角色。
_Avoid_：evaluator、最终验收者

**evaluator**：
独立检查冻结验证对象并给出结论的只读智能体角色。
_Avoid_：generator、修复者

**coordinator**：
编排角色交接、冻结验证对象并处理门禁结果的智能体角色。
_Avoid_：人类决策者、generator、evaluator

**facilitator**：
引导发现对话以形成共享理解的智能体角色。
_Avoid_：coordinator、generator、人类决策者

**Subject SHA**：
唯一标识某次冻结验证对象的完整 Git commit SHA。
_Avoid_：分支名、HEAD、短 SHA

**门禁（gate）**：
继续交付流程前必须满足的明确条件。
_Avoid_：建议、效果

**落地（landing）**：
Harness 变化通过适用门禁后进入目标分支的状态。
_Avoid_：效果、发布

**效果（effect）**：
Harness 变化落地后，在预先声明的观察单元中出现的可追踪结果。
_Avoid_：落地、单次检查通过

**观察单元（observation unit）**：
承载一次 Harness 效果观察的特定任务、变更、会话、时间段或可比较场景。
_Avoid_：指标、结论
