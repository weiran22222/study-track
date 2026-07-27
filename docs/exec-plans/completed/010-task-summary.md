# 执行计划 010：summary 任务统计

状态：已完成

## 目标

实现 `SPEC.md` 2.5 节和 AC-13，输出任务总数、未完成数和已完成数，同时保持所有路径只读。

## 范围

- Application 计算不可变的任务统计结果；
- 新增 `summary` CLI 子命令并注册到根命令；
- 固定输出 `Total`、`Pending`、`Completed` 三行；
- 添加 Application、CLI 和文件副作用测试；
- 更新本计划进度和实现决策。

## 非目标

- 不实现 `show`；
- 不增加筛选参数或其他统计维度；
- 不修改 JSON 格式或持久化写入逻辑；
- 不修改 SPEC、架构、CI、依赖或远程设置；
- 不解决另一个并行分支的冲突。

## 行为验收

1. 混合状态任务的三个计数正确；
2. 空任务库固定输出三个 `0`；
3. 命令成功退出 `0`，stderr 为空；
4. 数据文件不存在时不创建文件；
5. 已有数据文件在统计前后字节不变；
6. 损坏 JSON 返回退出码 `1`，不覆盖原文件；
7. JDK 21 下完整 `verify` 通过。

## 允许的共享热点

该切片可以最小修改 `StudyTaskService`、`StudyTrackCommand` 和现有 CLI 集成测试。统计逻辑
属于 Application，不应放入 CLI。不要为了避免未来冲突而预先实现或重构 `show`。

## 进度

- [x] 无父对话子智能体开始
- [x] Application 行为与测试完成
- [x] CLI 行为与测试完成
- [x] 只读副作用验证完成
- [x] 完整 `verify` 通过
- [x] 主智能体审查（JDK 21 下独立复跑 `verify`：37 项测试通过，Checkstyle 0 违规）
- [x] PR #5 同步最新 `main` 后通过并合并（最终 `main` 提交 `cb0b930`）

## 实现决策

- Application 使用不可变 `TaskSummary` 记录承载三个 `long` 计数，CLI 只负责固定格式输出；
- `summarizeTasks()` 只调用一次 `TaskRepository.findAll()`，由总数减去已完成数得到未完成数，
  不调用任何持久化写入方法；
- CLI 回归测试同时覆盖已有文件字节不变、无数据文件不创建，以及损坏 JSON 原字节保留。

## 合并 main 冲突记录

将包含 `show` 的 `main` 提交 `6d421307ce07a03123ea911f3248d758479045bf` 合并到本分支时，
实际冲突及解决方式如下：

- `StudyTaskService`：保留 `summarizeTasks()` 与 `showTask(long)` 两个独立只读用例；
- `StudyTrackCommand`：根命令同时注册 `ShowCommand` 和 `SummaryCommand`；
- `StudyTaskServiceTest`：保留双方全部 Application 行为与无写入断言；
- `StudyTrackApplicationTest`：保留双方 CLI 集成场景，并让损坏 JSON 参数化测试同时覆盖
  `show` 和 `summary`。

冲突解决只取两个已审查切片的行为并集，没有重构共享逻辑、修改规格、架构、持久化或工具链。
合并后在 JDK 21 下运行完整 `.\mvnw.cmd verify`，43 项测试全部通过，Checkstyle 0 违规。
