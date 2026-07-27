# StudyTrack 智能体导航

## 项目目标

构建一个本地 Java 命令行学习任务管理器。人类负责产品边界和验收标准，智能体在架构约束内实现并通过自动检查。

## 开始工作前

按任务需要阅读：

1. 产品行为、输入输出和完成标准：[SPEC.md](SPEC.md)
2. 技术选择、分层与依赖规则：[ARCHITECTURE.md](ARCHITECTURE.md)

本文件只提供地图和工作约定，不复制两份文档的全部内容。

## 当前阶段

当前是 Harness v4：规格、架构、Maven Wrapper、Checkstyle、ArchUnit、CI 和可执行
JAR 已经建立；`add`、`list`、`complete` 三个功能切片已完成。AC-07 损坏 JSON
的完整验收与可靠性加固尚未完成。

不得把启动骨架能够运行描述为产品功能已经完成；产品进度以
[SPEC.md](SPEC.md#5-验收标准) 中的验收标准为准。

最近完成的 complete 功能计划：
[docs/exec-plans/completed/004-complete-task.md](docs/exec-plans/completed/004-complete-task.md)。

最近完成的 list 功能计划：
[docs/exec-plans/completed/003-list-tasks.md](docs/exec-plans/completed/003-list-tasks.md)。

最近完成的 Harness 计划：
[docs/exec-plans/completed/002-build-idempotency-guard.md](docs/exec-plans/completed/002-build-idempotency-guard.md)。

最近完成的功能计划：
[docs/exec-plans/completed/001-add-task.md](docs/exec-plans/completed/001-add-task.md)。

## 关键边界

- 不得实现 [SPEC.md](SPEC.md)“本版本不包含”中的功能；
- CLI 不得直接访问文件系统或 Jackson；
- Application 不得依赖具体持久化实现；
- Bootstrap 是 CLI 与 Infrastructure 的组合入口；
- 修改架构前必须先更新 [ARCHITECTURE.md](ARCHITECTURE.md)；
- 修改产品行为前必须先更新 [SPEC.md](SPEC.md) 及验收标准。

完整规则及原因见 [ARCHITECTURE.md](ARCHITECTURE.md#4-分层与依赖)。

## 标准工作流

1. 阅读与任务相关的规格和架构章节；
2. 检查现有代码与测试，不猜测仓库状态；
3. 以最小改动实现一个可验证增量；
4. 添加或更新对应自动测试；
5. 运行快速的相关测试；
6. 完成前运行统一验收命令；
7. 如果检查失败，根据错误修复根因并重新验证。

## 验证命令

Windows：

```powershell
.\mvnw.cmd verify
```

macOS/Linux：

```bash
./mvnw verify
```

构建环境必须使用 JDK 21。Maven Enforcer 会在版本不符合时给出明确错误，不能绕过
版本门禁或改用系统 Maven。

## 完成定义

任务只有同时满足以下条件才算完成：

- 行为符合 [SPEC.md](SPEC.md)；
- 依赖符合 [ARCHITECTURE.md](ARCHITECTURE.md)；
- 新增或修改的行为有自动测试；
- `verify` 全部通过；
- 文档与实际命令、目录和行为保持一致；
- 没有未经规格授权的顺手功能。

## 失败反馈要求

自定义检查的错误信息必须包含：

1. 错误位置；
2. 被违反的不变量；
3. 违反原因；
4. 具体修复方向；
5. 修改后的验证命令；
6. 权威文档链接。
