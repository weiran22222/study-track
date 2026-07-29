# 执行计划 019：简化 generator/evaluator 交接

状态：已完成（仓库内实施与 generator 自检，2026-07-29）

## 目标与范围

实施[决策卡 022](../../decisions/022-simplify-agent-handoff.md)：保留不同 generator/evaluator、
冻结 Subject SHA、只读验证和同 SHA required `verify`，移除活跃 Harness 对分支及
worktree 运行模式的耦合。

只修改 `AGENTS.md`、决策 019/021 状态、文档索引、本计划与决策卡 022、两个 verification
subject guard 及其契约测试。不修改 `SPEC.md`、`ARCHITECTURE.md`、产品代码、CI 身份、
GitHub 权限或分支保护，不创建额外证据文件。

## 实施步骤

1. 把 `AGENTS.md` 的工作流、角色、轻量交接、guard 命令和完成定义改为精确 SHA 语义，
   删除活跃的 worktree 模式规则；
2. 将两个 guard 收敛为单一完整 Subject SHA 参数，只读检查 commit 解析、`HEAD`、干净
   `git status --porcelain` 和空 `git diff --cached`，保留六字段错误；
3. 更新 `VerificationSubjectGuardTest`，覆盖单参数、旧 SHA、dirty、staged、参数错误、
   非仓库、脚本只读、AGENTS 角色/轻量交接/同 SHA 完成语义，同时确认 required Job 仍为
   `verify` 且没有伪造身份 check；
4. 更新索引和历史状态标记，运行针对性测试及 JDK 21 完整 `verify`；
5. generator 停止写入后，由协调者冻结 SHA，再由不同 evaluator 按最小交接独立验证。

## 风险与停止条件

- 轻量交接可能遗漏实现所需背景：背景应从 Task、Acceptance criteria 和仓库权威文档
  获取，不重新扩张强制 manifest；
- guard 不能认证智能体身份：身份仍由不同 agent/task 标识和报告审计，不新增
  `independent-verification` check；
- 任意修复或提交产生新 SHA 时，旧 evaluator 报告立即失效；
- 若实施需要改变产品、架构、CI 身份、远程权限、分支保护或扩大预期文件范围，立即停止。

## 验收与验证

- 两个 guard 对相同单参数契约给出一致结果，失败均含 Location、Invariant、Reason、Fix、
  Recheck、Authority；
- evaluator 前后 guard 与 required `verify` 覆盖同一 Subject SHA；
- `VerificationSubjectGuardTest` 针对性测试通过；
- 使用 JDK 21 运行完整 `.\mvnw.cmd verify` 通过；
- 最终独立结论由不同 evaluator 给出，generator 自检不作为最终 `PASS`。

## 已发生本地事实

generator 在未提交工作树中完成仓库内实施并执行以下自检：

- `.\scripts\check-environment.ps1` 成功，Java 21，Maven Wrapper 3.9.12；
- 针对性测试成功，10/10；
- `sh -n ./scripts/check-verification-subject.sh` 成功；
- JDK 21 完整 `.\mvnw.cmd verify` 成功，122/122，0 Checkstyle violations；
- `git diff --check` 成功。

以上只证明 generator 自检。归档本计划不表示 evaluator 已给出 `PASS`、required CI 已
成功、PR 已合并，也不表示任何其他远端事实已经发生。
