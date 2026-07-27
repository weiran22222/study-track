# 执行计划 007：升级 GitHub Actions Node.js 24 运行时

状态：已完成

## 背景

本计划执行
[决策卡 002](../../decisions/002-actions-node24-upgrade.md)。它修复首次远程 CI 暴露的
Harness 警告，不改变 StudyTrack 产品行为。

## 范围

- 升级 `.github/workflows/verify.yml` 中的两个官方 Action；
- 扩展现有环境契约回归测试；
- 本地验证工作流结构和完整门禁；
- 推送后验证真实 GitHub Actions 运行与警告状态；
- 更新决策卡、计划、复盘和 `AGENTS.md`。

## 实现约束

- `checkout` 固定为 `actions/checkout@v6`；
- `setup-java` 固定为 `actions/setup-java@v5`；
- 保持 Temurin JDK 21、Maven 缓存、环境自检和统一 `verify` 命令不变；
- 不修改产品代码、Java/Maven 版本或其他依赖；
- 回归失败信息必须指出位置、不变量、原因、修复方法、验证命令和权威文档。

## 验收场景

1. 结构测试确认两个 Action 使用批准的主版本；
2. 环境自检仍排在完整 `verify` 之前；
3. JDK 21 下完整 `verify` 通过；
4. GitHub Actions 远程运行成功；
5. 远程环境步骤继续报告 Java 21 和 Maven Wrapper 3.9.12；
6. 远程完整门禁继续报告 33 个测试、0 个失败；
7. Node.js 20 弃用警告消失。

## 进度

- [x] 人类批准 Actions 主版本升级
- [x] 无父对话子智能体完成实现
- [x] 本地完整 `verify` 通过
- [x] 远程 GitHub Actions 通过
- [x] Node.js 20 弃用警告消失
- [x] 反馈证据写回仓库
- [x] 计划关闭

## 交接要求

实现子智能体只能从 `AGENTS.md`、本决策卡、本计划和仓库现状恢复任务。不得依赖父对话，
不得自行扩大升级范围，也不得创建 Git commit 或推送远程。

## 实现与验证记录

- 2026-07-27：无父对话子智能体从仓库恢复批准范围，将 `checkout` 升级到 `v6`、
  `setup-java` 升级到 `v5`，并扩展环境契约测试；没有修改产品、Java/Maven 版本、
  缓存策略或其他依赖。
- 2026-07-27：主智能体审查发现测试错误信息引用会移动的活动计划路径，将权威链接修正
  为稳定的决策卡路径。
- 2026-07-27：JDK 21 下本地完整 `verify` 通过，33 个测试、0 个失败。
- 2026-07-27：[远程 `verify #3`](https://github.com/weiran22222/study-track/actions/runs/30269042845)
  对提交 `3792c08` 运行成功；环境自检、完整门禁及全部收尾步骤均成功。
- 2026-07-27：远程日志确认 Java 21、Maven Wrapper 3.9.12 和 33 个测试通过；
  Check Run 注解数为 0，Node.js 20 弃用警告消失。
