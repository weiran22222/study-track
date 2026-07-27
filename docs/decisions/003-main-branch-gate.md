# 决策卡 003：公开仓库并建立 main 合并门禁

状态：已批准，待实施

日期：2026-07-27

## 问题

GitHub Actions 已能在 `push` 和 `pull_request` 上执行环境自检及完整 `verify`，但当前
`main` 允许管理员直接推送。CI 因此只能在提交进入主分支后报告结果，不能在合并前机械
阻止不合格变更。

仓库当前为 Private，账号使用 GitHub Free。远程 API 对 Branch Protection 和 Rulesets
均返回 403；GitHub 官方能力边界要求将仓库设为 Public，或升级到支持私有仓库保护的套餐。

## 公开前审计

2026-07-27，主智能体完成只读审计：

- 本地 `main` 与 `origin/main` 同步，工作区干净；
- 跟踪内容仅包含源码、测试、构建配置和 Harness 文档；
- 完整 Git 历史没有命中常见 API 密钥、访问令牌、客户端密钥或私钥标记；
- `MVNW_PASSWORD` 命中来自官方 Maven Wrapper 的通用环境变量支持，不包含真实凭据；
- 未发现机器专属绝对路径；`settings.xml` 命中均为“禁止提交机器配置”的说明。

该审计降低公开风险，但不能保证任何自动扫描能够发现所有敏感语义。仓库公开后，已经被
他人获取的历史无法通过重新设为 Private 撤回。

## 人类决策

2026-07-27，学习者明确选择方案 A：

- 将 `weiran22222/study-track` 从 Private 改为 Public；
- 为 `main` 建立 Branch Protection；
- 使用单人学习项目参数，不要求人工审批，但必须通过 PR；
- 要求状态检查 `verify` 成功，并要求分支与最新 `main` 同步；
- 管理员也不得绕过；
- 禁止强制推送和删除 `main`。

## 保护参数

| 参数 | 值 | 原因 |
|---|---|---|
| Require pull request | 是 | 阻止直接推送进入 `main` |
| Required approvals | 0 | 单人项目无法要求他人审批，但仍保留 PR 边界 |
| Required status check | `verify` | 复用已验证的统一门禁 |
| Require up-to-date branch | 是 | 验证结果必须对应最新主分支 |
| Enforce administrators | 是 | 仓库所有者也遵守相同规则 |
| Allow force pushes | 否 | 防止改写受保护历史 |
| Allow deletions | 否 | 防止删除主分支 |
| Signed commits / linear history / merge queue | 否 | 当前学习目标不需要额外复杂度 |

## 成本与风险

- 完整 Git 历史将公开；
- 所有后续变更必须建立分支和 PR，增加少量操作成本；
- `verify` 名称变化会阻塞合并，改名必须作为 Harness 迁移处理；
- 严格同步要求在 `main` 更新后重新同步并运行检查；
- 管理员不绕过可能导致错误配置时暂时无法合并，因此必须保留可审计的恢复方法。

## 非目标

- 不要求代码所有者或人工审批；
- 不启用签名提交、线性历史、部署门禁或合并队列；
- 不修改产品行为；
- 不用本地 Hook 冒充远程强制门禁；
- 不自动合并未验证的 PR。

## 验收

- 仓库 API 报告 `visibility=public`；
- `main` 保护要求 PR、`verify`、严格同步并对管理员生效；
- API 报告强推和删除均禁用；
- 测试分支能够推送并创建 PR；
- PR 的 `verify` 成功前不可合并，成功后变为可合并；
- 尝试直接推送 `main` 被远程拒绝；
- 证据写回仓库，并通过受保护的 PR 合并。
