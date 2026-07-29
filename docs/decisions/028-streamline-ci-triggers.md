# 决策卡 028：精简 CI verify 触发

状态：已批准

日期：2026-07-29

## 问题与基线

现有 `.github/workflows/verify.yml` 对每次 `push` 和 `pull_request` 都运行同名 `verify`
Job。普通 `codex/*`、`hotfix/*` 等工作分支先在 push 后运行一次完整环境自检和 Maven
`verify`，创建或更新 PR 后又运行包含分支流、完整 `base...head` 差异门禁和同一 Maven
入口的完整检查。工作分支 push 没有 PR base/head 语义，前一次运行不能替代后一次 PR
门禁。

可靠基线来自当前 workflow，以及已归档执行计划 022 中同一功能 PR 分别出现分支 push
`verify` 与 PR `verify` 的可定位记录。该记录只证明重复触发形态确实出现过；没有可归因
的耗时、等待、失败率或成本基线，不据此倒推节省比例。

## 候选方案与取舍

### A. 保持所有分支 push 与 PR 双重触发

- 收益：每次远端 push 都立即得到完整 Maven 反馈；
- 成本：同一工作分支更新在 PR 事件上重复运行构建，而 push 运行又不能执行 PR-only
  分支流和完整差异门禁。

### B. 取消所有 push，只保留 PR

- 收益：触发矩阵最简单，工作分支不重复运行；
- 成本：`develop` 与 `main` 经 PR 合并形成的新提交不再有最终非 PR 验证，会削弱本地
  `develop` 安全更新与最终发布基线验证，不接受。

### C. PR 保留完整门禁，push 只匹配 develop 与 main

- 收益：普通工作分支只在 PR 上运行一次有完整语义的门禁；两条长期分支更新后仍有最终
  环境自检和 Maven `verify`；
- 成本：未创建 PR 的工作分支不再获得远端 CI 反馈，generator 必须依赖本地 JDK 21
  自检和完整 `verify`，PR 创建后才获得 required `verify`。

选择本方案。

## 人类决定

学习者于 2026-07-29 明确批准：

- `pull_request` 不按目标分支过滤，继续在 GitHub 默认 activity types 实际触发时运行
  完整 `verify`；
- `push` 只匹配 `develop` 与 `main`，普通 `codex/*`、`hotfix/*` 等工作分支 push 不再
  运行 CI；
- PR 继续先运行分支流与完整 `base...head` 差异门禁，所有实际触发事件继续运行 JDK 21
  环境自检和 Maven `verify`；
- `jobs.verify` 身份、受保护 PR、required check、generator/evaluator、冻结 SHA 和最终
  `origin/develop` push 验证边界保持不变。

## 决定与效果假设

`.github/workflows/verify.yml` 的事件矩阵改为：

- `pull_request` 不配置目标分支过滤或显式 `types`；它覆盖所有 PR 目标分支，并在
  GitHub 默认 activity types 实际触发时运行分支流、完整差异、JDK 21 环境自检和
  Maven `verify`；
- 仅 `develop` 与 `main` 的 `push` 运行最终非 PR 环境自检和 Maven `verify`；
- 工作分支 push 不触发 workflow。

效果假设是：在保持结果正确性、反馈回路和可追踪边界不变的前提下，移除工作分支 push
与 PR 的重复完整构建，可以降低 CI 运行与等待成本。观察单元是本决定落地后的普通、
hotfix 与长期分支更新；适用维度为结果正确性、反馈回路、交付效率及熵与维护成本。
自主性负担和跨 evaluator 可复现性本次不单独评估。当前无足够落地后样本，不形成效果
结论；后续只使用实际 PR/Actions 记录观察反例、残余缺口和维护成本。

## 单 PR 交付理由

这是改变 CI 工具链触发语义的第三级 Harness 变更，已获得人类明确批准并使用决策卡和
短执行计划。范围已经冻结，不包含远程权限、分支保护、数据迁移或其他外部状态，workflow
改动局部且可由同一受保护 PR 回滚。依据[决策卡 011](011-lean-tier3-artifacts.md)的精简
原则，拆出规划 PR 不会增加新的有效证据，因此在同一受保护 PR 中交付架构更新、决策、
短计划、实现和测试，不创建重复 evidence/feedback 文件。

功能 diff 只可根据已经发生的本地 generator 自检归档计划；evaluator、required CI、
PR、合并与远程 push 结果仍须按既有流程在实际发生后由对应权威记录承载，不在仓库中
预写。

## 不变边界

- 不修改 `SPEC.md`、`HARNESS.md`、产品行为、产品代码、依赖或数据格式；
- 不修改 GitHub 分支保护、required check、权限、远程配置或 `jobs.verify` 身份；
- 不删除或弱化 `pull_request` verify、PR branch-flow、完整 `base...head` 差异门禁、
  `develop`/`main` push verify、JDK 21 环境自检或 Maven `verify`；
- 不改变受保护 PR、generator/evaluator、冻结 Subject SHA、同 SHA required
  `verify`、最终 `main` 验证或本地 `develop` 更新前最终 `origin/develop` push
  `verify` 规则；
- 不把减少运行次数单独当作 Harness 正向效果，也不补造远程结果、成本或效率数据。

本决定只取代决策卡 016 中“所有 push 继续运行”的旧触发范围；其 PR 完整差异门禁及其他
不变边界继续有效。决策卡 020 与 023 定义的分支模型和长期分支最终验证继续有效。
