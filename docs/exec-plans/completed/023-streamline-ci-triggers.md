# 执行计划 023：精简 CI verify 触发

状态：已完成（仓库内实施与 generator 自检，2026-07-29）

## 目标与边界

实施[决策卡 028](../../decisions/028-streamline-ci-triggers.md)：保留未按目标分支过滤的
`pull_request` 完整 `verify` 门禁，把 push 触发精确限制为 `develop` 与 `main`，并保持
`jobs.verify`、PR-only branch-flow/完整差异门禁、JDK 21 环境自检和 Maven `verify`
不变。`pull_request` activity types 采用 GitHub 对未配置 `types` 的默认集合。

本计划不修改 `SPEC.md`、`HARNESS.md`、产品代码、依赖、分支保护、required check、
远程权限或 GitHub 配置，也不创建独立 evidence/feedback 文件。这里的权限边界是
generator 不创建或切换分支；本功能分支已由协调者依据人类授权创建，不因此扩大
generator 权限。

## 实施步骤

1. 先更新 `ARCHITECTURE.md` 的当前验证流水线事件矩阵；
2. 新增决策卡与本短计划，再修改 `verify.yml` 和 `WORKFLOW.md`；
3. 增加单一职责的 workflow 触发契约测试，并调整既有分支流与文档导航测试；
4. 更新 `docs/README.md`，运行 JDK 21 环境自检、相关测试和完整
   `.\mvnw.cmd verify`；
5. 只根据真实本地实施与 generator 自检结果更新本计划，并移入 `completed/`。

## 风险与控制

- **误删 PR 门禁**：测试精确保护 `pull_request`、branch-flow 在完整差异之前运行且两步
  都仅限 PR；
- **误删长期分支最终验证**：测试要求 push 分支集合精确等于 `develop`、`main`，并要求
  环境自检和 Maven `verify` 保持无事件条件；
- **required check 身份漂移**：测试继续保护唯一既有 `jobs.verify` 身份；
- **文档与行为漂移**：架构和操作约定同时说明工作分支、PR 与长期分支 push 三种情况；
- **把本地结果冒充远程事实**：计划只记录实际 generator 命令与结果，不声称 evaluator、
  PR、CI、合并、远程 push 或最终效果结论。

## 验证与证据边界

计划内 generator 自检为：

```powershell
.\scripts\check-environment.ps1
.\mvnw.cmd "-Dtest=VerifyWorkflowTriggerTest,BranchFlowGuardTest,PullRequestDiffGuardTest,EnvironmentBootstrapTest,VerificationSubjectGuardTest,DocumentationNavigationTest" test
.\mvnw.cmd verify
```

## 已发生的 generator 本地事实

- 初始环境自检准确报告继承的 Java 17 不满足门禁；只为后续命令进程选择本机
  `D:\work\jdk\jdk-21.0.11` 后，重新运行同一入口通过，报告 Java 21 与 Maven Wrapper
  3.9.12；
- 相关测试命令实际运行
  `VerifyWorkflowTriggerTest`、`BranchFlowGuardTest`、`PullRequestDiffGuardTest`、
  `EnvironmentBootstrapTest`、`VerificationSubjectGuardTest` 与
  `DocumentationNavigationTest`，共 26 项测试，0 失败、0 错误、0 跳过，Checkstyle
  0 违规；
- 完整 `.\mvnw.cmd verify` 实际运行 142 项测试，0 失败、0 错误、0 跳过，Checkstyle
  0 违规，并成功完成可执行 JAR 打包；
- workflow 触发范围、当前文档与测试已形成；本计划按上述实际本地状态归档。

这些结果仅是 generator 自检，不是 evaluator `PASS`、GitHub workflow 解析、required
CI、PR、合并、`develop`/`main` push、最终远端验证或 Harness 效果证据。相关事实尚未由
本计划观察，也没有创建独立 evidence/feedback 文件。
