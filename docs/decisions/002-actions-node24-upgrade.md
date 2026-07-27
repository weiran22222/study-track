# 决策卡 002：升级 GitHub Actions Node.js 24 运行时

状态：已实现并通过远程验证

日期：2026-07-27

## 问题

首次远程 `verify` 成功，但 GitHub 报告 `actions/checkout@v4` 和
`actions/setup-java@v4` 使用的 Node.js 20 运行时已弃用，目前由平台强制运行在
Node.js 24。继续保留旧主版本会让非阻塞警告累积，并可能在平台停止兼容后变成阻塞失败。

## 证据

- [远程 `verify #1`](https://github.com/weiran22222/study-track/actions/runs/30268171485)
  成功，但包含一条 Node.js 20 弃用警告；
- GitHub 官方 `actions/checkout` 当前文档使用 `v6`；
- GitHub 官方 `actions/setup-java` 当前文档使用 `v5`，该主版本升级到 Node.js 24；
- 当前使用 GitHub 托管的 `ubuntu-latest`，不涉及自托管 Runner 版本升级。

## 人类决策

2026-07-27，学习者选择方案 A：

- 将 `actions/checkout@v4` 升级为 `actions/checkout@v6`；
- 将 `actions/setup-java@v4` 升级为 `actions/setup-java@v5`；
- 保留现有 Temurin JDK 21、Maven 缓存和统一验证命令；
- 增加低成本回归保护，防止工作流退回已弃用主版本。

## 收益

- 消除已观测到的 Node.js 20 弃用警告；
- 让工作流使用官方当前文档所示的 Node.js 24 运行时版本；
- 在平台停止兼容旧运行时前主动消除风险；
- 把本次远程反馈固化为可自动检查的不变量。

## 成本与风险

- 两个 Action 都跨越主版本，必须通过真实远程运行验证；
- `checkout@v6` 的凭据持久化实现与旧版不同，但当前工作流不使用容器 Action 或后续
  认证 Git 操作，风险较低；
- 静态回归测试只能保护版本声明，不能替代 GitHub 托管 Runner 上的实际执行。

## 非目标

- 不改变 Java、Maven 或产品代码；
- 不改变缓存策略；
- 不引入自托管 Runner；
- 不顺手升级其他依赖或处理 Maven Shade 警告；
- 不把一次远程成功描述为所有 Runner 环境均兼容。

## 验收

- 工作流使用 `actions/checkout@v6` 和 `actions/setup-java@v5`；
- 环境自检仍在完整 `verify` 前执行；
- 低成本回归测试能发现两个 Action 退回旧主版本；
- JDK 21 下本地完整 `verify` 通过；
- 推送后的 GitHub Actions 成功，环境自检和 33 个测试通过；
- 远程运行不再报告本决策针对的 Node.js 20 弃用警告。

## 验证结果

- JDK 21 下本地完整 `verify` 通过：33 个测试、0 个失败；
- [远程 `verify #3`](https://github.com/weiran22222/study-track/actions/runs/30269042845)
  对提交 `3792c08` 运行成功；
- 远程环境自检报告 Java 21 和 Maven Wrapper 3.9.12；
- 远程完整门禁报告 33 个测试、0 个失败；
- 远程 Check Run 注解数为 0，Node.js 20 弃用警告消失。
