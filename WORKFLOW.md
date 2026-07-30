# StudyTrack 工作流与操作约定

任何仓库修改前必须先读取 [AGENTS.md](AGENTS.md) 的目标、文档地图和根本原则，并完整读取
本文。本文承接仓库当前有效的操作性规则；`AGENTS.md` 只保留稳定的第一跳导航与不可妥协
原则。

## 权威事实与机械验收

当前产品行为与验收标准以 [SPEC.md](SPEC.md) 为唯一权威。本文件不维护 Harness 版本号、
逐项验收状态或功能完成清单；需要了解历史理由时，从 [文档索引](docs/README.md) 按需查阅。

`verify` 是仓库的机械验收入口，完整命令和执行内容见下方“验证命令”及
[ARCHITECTURE.md](ARCHITECTURE.md#7-验证流水线)。

## 关键边界

- 不得实现 [SPEC.md](SPEC.md)“本版本不包含”中的功能；
- CLI 不得直接访问文件系统或 Jackson；
- Application 不得依赖具体持久化实现；
- Bootstrap 是 CLI 与 Infrastructure 的组合入口；
- 修改架构前必须先更新 [ARCHITECTURE.md](ARCHITECTURE.md)；
- 修改产品行为前必须先更新 [SPEC.md](SPEC.md) 及验收标准；
- 不得把启动骨架能够运行描述为产品功能已经完成，产品进度以验收标准为准；
- 不得声称最终原子替换失败已经被直接故障注入验证；相关风险的接受边界和重评条件从
  [文档索引](docs/README.md) 查阅。

完整规则及原因见 [ARCHITECTURE.md](ARCHITECTURE.md#4-分层与依赖)。

## Harness 变更权限与风险分级

智能体可以自主修复违反现有规格、架构或测试的实现问题。修改 Harness 时，按语义、
权限、外部状态、回滚成本、持续时间和协作复杂度选择以下流程；改动文件少不自动等于
低风险，无法可靠判断为第一或第二级时必须使用第三级。

### 第一级：事实修正

适用于错别字、失效链接、错误文件名或与仓库现状不符但不改变既定语义的修正：

- 使用一个受保护 PR；
- 在 PR 中描述问题、修正和验证；
- 不要求独立决策卡或执行计划；
- 仍须通过适合本仓库的必需门禁。

### 第二级：小型 Harness 决策

适用于可逆、范围局部，且不改变产品目标、核心架构边界、验收语义、智能体权限、
工具链、远程权限或显著持续成本的 Harness 调整：

- 人类知情；需要判断时先获得明确同意；
- 创建简短决策卡，并与实现放在同一个受保护 PR；
- 默认不创建详细执行计划、独立证据文件或归档 PR；
- PR diff、审查记录和 CI 结果共同作为完成证据。

### 第三级：重大或多步骤变更

改变产品目标、核心架构边界、验收语义、智能体权限、工具链、远程仓库权限、数据格式或
迁移策略，引入显著持续维护或外部服务成本，或者包含多个独立阶段、外部状态变化、并行
智能体协作或高回滚成本时，必须使用完整流程：

- 先获得人类明确批准；
- 创建决策卡和执行计划；
- 按阶段保存验证证据；
- 默认使用“规划 PR → 功能 PR”：功能 PR 可以根据已经发生的本地实施事实更新证据并
  归档计划，但不得声称尚未发生的合并、最终 `main` 提交或远程 CI 结果；
- 功能 PR 合并后必须检查最终 `main` 的 `verify`；GitHub PR 与 Actions 是远程 CI
  结果的权威记录，默认不把这些结果重复抄回仓库；
- 仅当必须记录合并后才出现的外部状态、发生部署/迁移/远程权限变化、实际结果偏离预期，
  或仍有风险与未完成事项时，使用独立收尾 PR。

### 工件单一职责

- 决策卡只记录为什么改变、候选方案、取舍和人类选择；产品协议引用 `SPEC.md`，不重复
  保存完整协议；
- `SPEC.md` 是当前完整产品行为与验收标准的唯一权威；
- 执行计划引用 `SPEC.md`，只记录实施步骤、风险和验证方法，不复制完整产品协议；
- 证据只记录实际执行的命令、结果、哈希与证据边界，不记录预测或尚未发生的远程事实。

### 所有级别的不变边界

- 不得绕过受保护 `main`、必需 PR 或 `verify`；
- 智能体不得自行把第三级变更降级；
- 发现实际范围扩大时必须停止并升级流程；
- 不得把未执行的检查写成已通过；
- 不得因为精简工件而削弱人类产品决策、规格先行、自动测试、受保护 PR、必需 `verify`
  或最终 `main` 验证；
- 不得仅因为技术上可行就扩大 Harness 范围。

## 标准工作流

需要修改仓库的任务按
[决策卡 022](docs/decisions/022-simplify-agent-handoff.md) 分离实现与最终本地验证：

1. 人类批准目标、验收标准以及是否创建或切换分支；未收到明确指令时，协调者保持当前
   分支不变；
2. 协调者使用下方最小 generator 交接委派实现；generator 以最小改动实现并增加自动
   测试，运行相关测试和完整 `verify`，但只能把结果报告为 generator 自检；
3. generator 停止写入后，协调者审查、提交并冻结 `FROZEN(<Subject SHA>)`；
4. 不同且不继承 generator 对话的 evaluator 运行前置 guard、只读独立验证和后置
   guard，给出 `PASS`、`FAIL` 或 `INCONCLUSIVE`；
5. `FAIL` 由协调者交回 generator；任何修复产生新 SHA 后，旧报告立即失效并重新交接；
6. `INCONCLUSIVE` 不得视为通过；只有 evaluator `PASS` 与 required `verify` 同时覆盖
   同一 Subject SHA，协调者才可进入正常 PR 合并判断；
7. evaluator 必须在 PR 创建或把新 head 推入既有 PR 前完成；当前完整 `PASS` 报告进入
   PR body 的 v1 marker 区，旧 `FAIL`/`INCONCLUSIVE` 在 PR 存在后追加为评论；
8. 如果检查失败，根据错误修复根因并重新验证，不绕过受保护 PR 或门禁。

### 原生 grill-with-docs 显式会话

`grill-with-docs` 是复杂设计的可选澄清入口，只能由人类显式调用；它不自动触发，也不
是普通任务、简单任务或所有变更的通用门禁。运行环境使用
[HARNESS.md](HARNESS.md#原生-grill-with-docs-学习输入)固定的原生组合，不在仓库中
vendoring 或重新实现包装器。

会话由 facilitator 一次只提出一个需要人类决定的问题，并附推荐答案。能从仓库或只读
工具查明的事实由 facilitator 先自行查明，不把事实查询转交给人类，也不把推荐答案冒充
为人类批准。facilitator 的写权限仅限于与本次主题相关的
`CONTEXT-MAP.md`、对应 context 的 `CONTEXT.md` 和该 context 的 `docs/adr/*.md`，且
只记录人类已经解决的术语与决定。不得修改产品代码、`SPEC.md`、`ARCHITECTURE.md`、
`HARNESS.md`、本文、CI、脚本、构建或远程配置；需要越界时必须停止并取得新的明确授权。

共享理解是人类显式确认的退出门禁，不是 facilitator 的主观判断。只有人类确认目标与
非目标、关键术语、决定分支、取舍和待实施范围已足够清楚，才可退出会话并进入实施交接；
确认前不得开始实现、创建实现交接或宣称规划完成。未解决的决定分支、词汇冲突或未记录
的关键决定都会阻止退出。

### 角色与冻结 SHA 交接

- **generator**：只实现批准的代码、测试和文档并运行自检；不得创建或切换分支，不得
  stage、commit、push、操作 GitHub、给出最终 `PASS` 或替代 evaluator；
- **evaluator**：只依赖仓库、最小交接和精确 Subject SHA 进行只读独立验证；不得修改或
  修复文件，不得 stage、commit、push、切换分支或操作 GitHub；
- **协调者**：审查和提交、冻结 SHA、检查交接状态、保存 evaluator 报告并协调失败回流；
  只有收到人类明确指令后才能创建或切换分支；唯一例外是完成“本地 develop 安全更新”
  后按该节的窄权限准备新的 `codex/*` 分支；不得用自身判断替代独立 evaluator、复用
  失效报告或绕过门禁；
- **人类**：决定产品目标、验收标准、重大 Harness 变更、是否创建或切换分支以及是否
  发布。

协调者可按任务需要自行决定是否使用额外子智能体以及是否并行；同一修改任务的
generator 写入、协调者冻结 Subject SHA、不同 evaluator 只读验证仍必须依次进行。
状态依次为 `SPEC_READY → IMPLEMENTING → FROZEN(<Subject SHA>) → VERIFYING`，验证只可
进入 `PASS`、`FAIL → IMPLEMENTING` 或 `INCONCLUSIVE`。

协调者交给 generator 的最小任务交接只包含：

```text
Task:
Acceptance criteria:
Allowed scope:
Prohibitions:
```

协调者交给 evaluator 的最小交接只包含：

```text
Task:
Acceptance criteria:
Subject SHA:
Generator:
Evaluator:
Mutation allowed: no
```

evaluator 报告至少包含：

```text
Subject SHA:
Generator:
Evaluator:
Commands executed:
Independent scenarios:
Findings:
Residual gaps:
Verdict: PASS | FAIL | INCONCLUSIVE
```

报告不得写入被验证提交。只有覆盖当前 Subject SHA 的完整 `PASS` 可以放入 PR body；
协调者不得把 generator 自检改写成 evaluator 报告，也不得用简短 envelope 代替上述
完整字段。协调者在 evaluator 前后分别运行
`scripts/check-verification-subject.ps1`（Windows）或
`scripts/check-verification-subject.sh`（macOS/Linux），只传一个完整 Subject SHA，
检查该 SHA 解析为提交、`HEAD` 精确相等、工作树干净且暂存区为空。完整协议与实施步骤
见 [决策卡 022](docs/decisions/022-simplify-agent-handoff.md) 和
[文档索引](docs/README.md)中的已归档执行计划 019。

不同智能体身份仍由协调记录、最小交接与 evaluator 报告审计；不得通过新增名为
`independent-verification` 的普通 CI Job 或 required check 伪造身份保证。

### PR evaluator 报告生命周期

当前 PR head 的完整 evaluator `PASS` 必须位于 PR body 中唯一的
`studytrack-evaluator-report:v1` marker 区，按固定顺序包含 `Subject SHA`、
`Generator`、`Evaluator`、`Commands executed`、`Independent scenarios`、`Findings`、
`Residual gaps` 与 `Verdict` 八个非空字段。`Subject SHA` 必须精确等于 PR head SHA，
`Verdict` 必须精确为 `PASS`。完整格式与历史理由见
[决策卡 031](docs/decisions/031-pr-evaluator-report-lifecycle.md)。

- 新 PR 创建时，body 必须已经包含该 head 的完整当前 `PASS`；
- 既有 PR 的 head SHA 改变后，旧 body 报告立即失效，`synchronize` 门禁必须因 SHA
  不匹配而失败；
- 新 SHA 重新取得不同 evaluator `PASS` 后，协调者更新 body；`edited` 事件重新运行同一
  required `verify`；
- `reopened` 重新检查 body 与当前 head；历史真实 `FAIL`/`INCONCLUSIVE` 在 PR 存在后
  按原报告追加评论，不覆盖 body 中的当前 `PASS`；
- 不得捏造、摘要替代或批量回填历史报告；评论不作为当前 head 的机械门禁输入。

PR-only 检查只证明 body 中存在唯一、结构符合 v1、SHA 与当前 head 相同且自述
`Verdict: PASS` 的报告。它不证明 generator/evaluator 的真实身份或独立性、报告内容
真实性、命令实际执行、场景完整、结论正确或 Harness 正向因果效果。

## 提交前暂存检查

1. 只暂存本次变更精确预期的文件；
2. 运行 `git status --short`，确认所有预期文件都已进入暂存区，且没有意外暂存内容；
3. 运行 `git diff --cached --check`，检查将要提交的完整暂存内容。

普通 `git diff --check` 不包含未跟踪新文件，不能把它当成提交内容的完整检查。

## 验证范围

- 提交前检查使用 `git diff --cached --check`，只检查将要提交的完整暂存内容；
- 本地 `.\mvnw.cmd verify`（Windows）或 `./mvnw verify`（macOS/Linux）是产品代码、
  架构和构建产物的完整机械验收入口；
- GitHub Actions 保留同名 `jobs.verify`：`pull_request` 未按目标分支过滤，覆盖所有
  PR 目标分支；activity types 明确为 `opened`、`synchronize`、`reopened` 与 `edited`。
  每次实际触发先使用事件提供的 base/head ref 检查分支流，再使用 base/head SHA 检查
  完整 `base...head` 差异，随后从 event file 安全落盘 body 并检查当前 evaluator
  `PASS` 报告，最后运行 JDK 21 环境自检和 Maven `verify`；
- `push` 只匹配 `develop` 与 `main`，作为长期分支更新后的最终非 PR 验证，继续运行
  JDK 21 环境自检和 Maven `verify`，但不运行没有 PR 语义的分支流或差异门禁；
- 普通 `codex/*`、`hotfix/*` 等工作分支 push 不触发 CI。generator 仍须在 JDK 21 下
  本地运行相关测试与完整 `verify`；创建或更新 PR 后由 `pull_request` 事件运行完整
  required 门禁。Windows Maven 验证不执行 POSIX 脚本，也不依赖系统 `sh`。

## 分支工作流

- `develop` 是 GitHub 默认分支和日常集成基线；普通 `codex/*` 从最新 `develop`
  建立，并通过 PR 合回 `develop`；
- `main` 是需要人工发布批准的生产发布基线，不表示已经部署；正常发布只使用
  `develop → main` PR；
- 紧急修复使用从最新 `main` 建立的 `hotfix/* → main` PR，合并后必须通过
  `main → develop` PR 回流；
- 普通工作分支 push 不运行 CI；所有 PR 运行完整门禁，`develop` 与 `main` 的 push
  运行合并或发布基线更新后的最终非 PR `verify`；
- `develop` 与 `main` 都要求 PR、严格 `verify`、管理员不可绕过，并禁止强推和删除；
- 分支流检查只允许上述普通、release、hotfix 和 hotfix 回流四种 PR 拓扑。

### 本地 develop 安全更新

- 禁止在本地 `develop` 上 merge、rebase 或 cherry-pick feature 或其他本地分支；任何
  进入远端 `develop` 的变更都必须通过受保护 GitHub PR 合并；
- 每次经人类批准的 PR 在 GitHub 合入 `develop` 后，协调者无需再次询问是否安全回到
  本地 `develop`；这项长期授权不替代人类的 PR 合并决定，也不扩大其他 Git/GitHub
  权限；
- GitHub 合并后，先回读最终 `origin/develop` 的精确 SHA，并确认该 SHA 的 required push `verify` 成功；
  只有完成这项远端验证后，才可在当前工作树和暂存区干净时运行 `git switch develop`。
  若远端事实、SHA、push `verify`、干净状态或分支占用无法确认，或切换失败，必须停止且
  不得修复；
- Windows 使用 `.\scripts\update-local-develop.ps1 "<verified-develop-sha>"`，
  macOS/Linux 使用
  `sh ./scripts/update-local-develop.sh "<verified-develop-sha>"`；
- 两个入口只接受一个已验证的完整 40 位 SHA，唯一更新源为 `origin/develop`。入口要求
  当前分支精确为 `develop`、工作树和暂存区干净、本地 `HEAD` 不领先且不分叉，并只执行
  no-op 或 fast-forward-only 更新；
- 任一前置、fetch、SHA、提交关系、更新或后置检查失败时必须停止；不得通过 reset、
  clean、merge、rebase、cherry-pick、stash、push、GitHub 操作或自动清理来修复。

完成上述远程验证和安全更新后，若本地 `develop`、工作树与暂存区仍然干净且未分叉，
coordinator 可自行从这个精确 `develop` 创建并切换到一个新建、干净的 `codex/*`
分支。该有限权限不得用于其他基线、已有或非 `codex/*` 分支，也不得在脏工作树中创建或
切换分支；它不扩大 stage、commit、push、PR、merge、rebase、cherry-pick、GitHub、
远程权限或发布权限。任一条件无法证明时停止并请求人类明确指令。

## 验证命令

首次进入仓库或构建环境变化后，先阅读
[环境说明](docs/environment.md)，并运行快速自检。

Windows：

```powershell
.\scripts\check-environment.ps1
```

自检通过后运行本地完整产品、架构与构建验证：

```powershell
.\mvnw.cmd verify
```

macOS/Linux：

```bash
sh ./scripts/check-environment.sh
```

自检通过后运行本地完整产品、架构与构建验证：

```bash
./mvnw verify
```

构建环境必须使用 JDK 21。Maven Enforcer 会在版本不符合时给出明确错误，不能绕过
版本门禁或改用系统 Maven。环境自检只诊断，不安装 JDK、不修改环境变量或 Maven 配置。

## 完成定义

任务只有同时满足以下条件才算完成：

- 行为符合 [SPEC.md](SPEC.md)；
- 依赖符合 [ARCHITECTURE.md](ARCHITECTURE.md)；
- 新增或修改的行为有自动测试；
- `verify` 全部通过；
- 不同 evaluator 对冻结的 Subject SHA 给出 `PASS`，且 required `verify` 覆盖同一 SHA；
- PR body 中的当前 v1 `PASS` 报告与同一 PR head SHA 绑定；
- evaluator 前后 guard 均确认 Subject SHA、HEAD、工作树和暂存区没有改变；
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
