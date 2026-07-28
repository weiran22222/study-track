# 决策卡 017：增强 reopen 幂等失败诊断

状态：已批准

日期：2026-07-28

## 问题

一次已观察的变异在 `StudyTaskService.reopenTask` 的 already-pending 分支错误调用
`repository.update`，把未完成任务写成已完成。Application、CLI 和 Bootstrap 三层回归
测试都捕获了错误，但原始失败只显示调用次数或字节数组不同，不能在单个 JUnit 失败输出
中直接说明位置、不变量、原因、修复方向、精确复验命令和权威规格。

## 人类决定

学习者于 2026-07-28 明确批准：只增强这三层 already-pending/no-write 回归断言的失败
消息，使每个独立失败都包含 `Location`、`Invariant`、`Reason`、`Fix`、`Recheck` 和
`Authority` 六个标签，并继续由 JUnit 提供 expected/actual。

## 风险等级

这是第二级、局部且可逆的 Harness 改进。它不改变产品行为、验收语义、架构边界、工具链、
远程权限或持续成本；只提高现有 AC-17 回归失败的可操作性。决策卡与测试消息放在同一
受保护 PR，不创建执行计划或独立证据文件。

## 范围

- 增强 `StudyTaskServiceTest` 的 already-pending `updateCalls` 断言消息；
- 增强 `ReopenCommandTest` 的对应 `updateCalls` 断言消息；
- 增强 `StudyTrackApplicationTest` 的数据文件字节不变断言消息；
- 使用同一单行生产代码变异，确认三层失败分别输出六个标签；
- 移除变异后运行 JDK 21 完整 `verify`。

## 非目标

- 不修改 `SPEC.md`、产品行为、生产代码、架构、依赖、POM 或 workflow；
- 不改变断言条件、测试覆盖范围或 Maven 验收语义；
- 不把六标签扩展到其他测试或引入共享诊断框架；
- 不保留用于负向验证的生产代码变异。

权威产品行为仍以 [`SPEC.md` 2.8 节及 AC-17](../../SPEC.md#28-重新打开任务)为准。
