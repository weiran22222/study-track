# 执行计划 020：实施本地 develop 安全纯快进策略

状态：进行中（规划 PR 阶段，尚未实施）

## 目标与权威边界

实施[决策卡 023](../decisions/023-local-develop-fast-forward-policy.md)：其他本地分支不得
直接 merge、rebase 或 cherry-pick 到本地 `develop`；变更只通过受保护 GitHub PR 进入
远端 `develop`。GitHub 合并且最终远端 `develop` 精确 SHA 的 push `verify` 成功后，
本地 `develop` 才能通过仓库自有入口从 `origin/develop` 纯快进。

[决策卡 020](../decisions/020-develop-production-branch-model.md)继续定义远端分支拓扑，
[决策卡 022](../decisions/022-simplify-agent-handoff.md)继续定义 generator/evaluator
同 SHA 交接。本计划不改变 `SPEC.md`、`ARCHITECTURE.md`、产品行为、数据格式、依赖、
CI Job 身份、GitHub 分支保护、远程权限或部署状态。

## 全程不变量

- 规划 PR 与功能 PR 都只能在 GitHub 通过受保护 `codex/* → develop` PR 合并；不得在
  本地把规划/功能分支 merge、rebase 或 cherry-pick 到本地 `develop`，也不得直接 push；
- 本地 `develop` 的唯一更新源是 `refs/remotes/origin/develop`，不得接受本地分支、
  其他 remote、其他 remote-tracking ref、任意 SHA 或工作树内容作为更新源；
- 只有 GitHub 权威记录确认最终远端 `develop` 的精确 SHA 及其 push `verify` 成功后，
  才能把该 40 位 SHA 交给本地更新入口；脚本本身不证明或模拟 CI；
- 本地更新前必须精确位于 `develop`，工作树和暂存区均干净，且本地 `HEAD` 相对
  `origin/develop` 不领先、不分叉；任一状态不明确即停止；
- 更新只能是 `origin/develop` 到本地 `develop` 的 fast-forward-only，不能产生 merge
  commit，也不能用 reset、rebase、cherry-pick、强推、自动清理或切换分支修复状态；
- generator 写入、协调者冻结 Subject SHA、不同 evaluator 只读验证依次进行。任何新
  SHA 使旧报告失效；evaluator `PASS` 与 required `verify` 必须覆盖同一 PR head SHA；
- 计划只记录实际发生的本地事实；GitHub PR、Actions、合并和最终远端 SHA 以 GitHub
  记录为准，不预写成功结果，也不把任何结果描述为部署。

## 预计变更范围

### 规划 PR

只包含：

- `docs/decisions/023-local-develop-fast-forward-policy.md`；
- `docs/exec-plans/020-local-develop-fast-forward-policy.md`；
- `docs/README.md`。

规划 PR 不修改 `AGENTS.md`、脚本、测试、workflow、`SPEC.md`、`ARCHITECTURE.md`、
产品代码、依赖、分支保护或远程权限，不创建证据文件。

### 功能 PR

预计只修改：

- `AGENTS.md`：增加稳定 Git 操作规则、两个入口和 GitHub 合并后更新顺序；
- `scripts/update-local-develop.ps1`；
- `scripts/update-local-develop.sh`；
- `src/test/java/com/example/studytrack/architecture/LocalDevelopUpdateTest.java`；
- 本计划、决策状态与 `docs/README.md`：只记录已经发生的本地实施事实并在完成时归档。

若需要修改 workflow、现有 guard、`SPEC.md`、`ARCHITECTURE.md`、产品代码、依赖、
GitHub 设置或超出上述范围，立即停止并重新取得批准。

## 安全更新入口契约

Windows 与 POSIX 入口分别为：

```powershell
.\scripts\update-local-develop.ps1 "<verified-develop-sha>"
```

```bash
sh ./scripts/update-local-develop.sh "<verified-develop-sha>"
```

两个入口采用相同算法和结果：

1. 精确接收一个 40 位十六进制 SHA；不接收 remote、branch、ref 或额外参数；
2. 确认当前目录是 Git 仓库，当前分支以大小写敏感方式精确等于 `develop`；
3. 分别确认 `git status --porcelain` 为空且暂存差异为空；dirty、untracked 或 staged
   状态均非零退出；
4. 只执行等价于
   `git fetch origin refs/heads/develop:refs/remotes/origin/develop` 的精确 fetch；
   不 fetch 或选择其他更新源；
5. 确认 `refs/remotes/origin/develop` 解析为完整 commit，且精确等于调用者提供的
   `verified-develop-sha`；不接受缩写、祖先 SHA 或 fetch 后更新出的不同 SHA；
6. 使用 `git rev-list --left-right --count
   HEAD...refs/remotes/origin/develop` 判断关系。左侧计数非零表示本地领先或分叉，必须
   停止；右侧为零表示已经同步；只有左侧为零、右侧为正才允许更新；
7. 只执行
   `git merge --ff-only refs/remotes/origin/develop`。这里允许的 source 仅是
   `origin/develop` 的 remote-tracking ref，不能把参数直接传给 Git 作为 source；
8. 后置确认仍位于 `develop`、`HEAD` 精确等于已验证 SHA、工作树干净且暂存区为空；
9. 任何 Git 命令错误或状态解析歧义均 fail closed，不尝试修复。

所有失败统一输出 Location、Invariant、Reason、Fix、Recheck、Authority。`Fix` 只能建议
停止并人工检查、清理自己的未提交工作、回到正确分支或核对 GitHub SHA；不得建议通过
reset、强推、绕过 PR、合并本地 feature 或删除未知提交来满足入口。

## 自动测试设计

`LocalDevelopUpdateTest` 同时承担稳定导航、静态契约和临时 Git 仓库行为测试：

- 确认 `AGENTS.md` 明确 GitHub-only 合并、最终远端 push `verify`、禁止本地
  merge/rebase/cherry-pick、两个入口和 fast-forward-only 更新顺序；
- 确认两个脚本都存在、只接收一个完整 SHA、硬编码 `origin/develop` 与
  `refs/remotes/origin/develop`、使用提交关系检查和 `--ff-only`，且不包含任意 source
  参数、checkout/switch、reset、rebase、cherry-pick、push、`gh` 或 HTTP 调用；
- 确认两个脚本都包含六字段诊断和决策 023/计划 020 权威链接；
- 在临时本地仓库与 bare `origin` fixture 中，按运行平台调用原生入口：Windows 运行
  PowerShell，Linux/macOS 运行 POSIX，覆盖“本地落后后成功纯快进”和“已经同步时成功
  no-op”，并确认没有 merge commit、最终 HEAD 精确等于已验证 SHA；
- 受控拒绝缺少/多余/非完整 SHA、非 Git 目录、非 `develop` 分支、tracked dirty、
  untracked、staged、本地仅领先、已分叉、缺少 `origin/develop`、fetch 失败和期望 SHA
  与 fetch 后 ref 不一致；每个失败都核对非零退出、六字段诊断和本地 HEAD/用户内容未变；
- Windows 完整 Maven 验证不依赖系统 `sh`；Ubuntu required `verify` 直接执行 POSIX
  行为场景。静态契约测试在每个平台同时比较两个脚本，防止跨平台语义漂移；
- `DocumentationNavigationTest` 确认决策、活跃计划和后续归档路径可发现。

不得通过 mock Git 成功输出替代关键提交关系行为测试，也不得为探针污染真实仓库或远端。

## 阶段 0：规划 PR

1. generator 只起草决策 023、活跃计划 020 和索引，不修改 Harness 行为；
2. 使用 JDK 21 运行 `DocumentationNavigationTest` 和完整 `.\mvnw.cmd verify`，结果只
   报告为 generator 自检；
3. generator 停止写入后，协调者审查精确 diff，只暂存三个规划文件，运行
   `git diff --cached --check`，提交并冻结 Subject SHA；
4. 不同 evaluator 在前后 guard 之间只读检查规划范围、未预写事实、导航测试和完整
   `verify`，给出 `PASS | FAIL | INCONCLUSIVE`；
5. 只有 evaluator `PASS` 与 required `verify` 覆盖同一规划 PR head SHA 后，才可按
   人类批准在 GitHub 合并；不得本地合入 `develop`；
6. 合并后回读精确远端 `develop` SHA 并等待该 SHA 的 push `verify` 成功。由于功能入口
   尚未实施，如需在功能 PR 前更新本地 `develop`，协调者必须手工执行与本计划入口等价的
   exact-branch、clean/index、not-ahead/not-diverged 和 `origin/develop`-only 检查，再
   进行 `--ff-only` 更新；任一条件不明确即停止。

本阶段完成后才进入功能实现。规划文档合入不代表安全更新入口已存在或活跃规则已改变。

## 阶段 1：功能实现与 generator 自检

1. 在经人类明确授权、从最新且已验证 `develop` 建立的 `codex/*` 分支上修改预计范围；
2. 先更新 `AGENTS.md` 稳定规则，再实现两个最小脚本和自动测试；不修改 CI 或远端设置；
3. generator 运行原生成功/失败 fixture、`LocalDevelopUpdateTest`、
   `DocumentationNavigationTest`、相关架构测试和 JDK 21 完整 `verify`；
4. POSIX 脚本另做语法检查；Windows 不以 POSIX 行为未执行冒充双平台通过，Linux
   required `verify` 将覆盖 POSIX 原生场景；
5. generator 检查 Git diff 和范围后停止写入，只报告 generator 自检，不给出最终
   `PASS`，不 stage、commit、push 或操作 GitHub。

## 阶段 2：冻结与独立验证

1. 协调者审查实现、测试、禁止命令和文档边界，只暂存功能 PR 的精确预期文件，运行
   `git status --short` 与 `git diff --cached --check`；
2. 协调者提交并冻结 `FROZEN(<Subject SHA>)`，在 evaluator 前运行
   `scripts/check-verification-subject.ps1` 或 `.sh`；
3. 与 generator 不同且不继承其对话的 evaluator 按最小交接只读复验：
   - 静态检查两个入口和 `AGENTS.md` 契约；
   - 独立建立临时 Git fixture，覆盖成功纯快进/no-op 及关键拒绝矩阵；
   - 运行针对性测试和 JDK 21 完整 `verify`；
   - 明确 Windows/POSIX 当前平台覆盖与残余缺口；
4. evaluator 完成后再次运行同一 Subject SHA guard；工作树、暂存区、HEAD 或 SHA 任一
   变化都使验证无效；
5. `FAIL` 交回 generator 修复并产生新 SHA 后从头验证；`INCONCLUSIVE` 不得视为通过。

## 阶段 3：功能 PR、GitHub 合并与本地更新

1. 只推送 evaluator `PASS` 的精确 Subject SHA，创建 `codex/* → develop` GitHub PR；
2. 回读 PR base/head，确认 evaluator 报告、PR head 与 required `verify` 绑定同一 SHA；
3. 若同步 base、修复或其他操作改变 head SHA，旧报告立即失效并回到阶段 2；
4. required `verify` 成功且 PR 与最新 `develop` 同步后，取得人类合并批准，在 GitHub
   正常合并；不使用管理员绕过，不在本地合入；
5. 回读 GitHub 最终 `develop` SHA，等待该精确 SHA 的 push `verify` 成功。失败或无法
   绑定精确 SHA 时停止，不更新本地 `develop`；
6. 在本地确认当前分支为 `develop` 后，把该精确 SHA 作为唯一参数调用新入口。入口必须
   从 `origin/develop` 纯快进，并报告最终 HEAD；
7. 只读确认本地 `develop` 与 `origin/develop` 都等于已验证 SHA，工作树干净且暂存区
   为空；远程 feature 分支删除与否不属于本计划；
8. 根据已经发生的本地实施事实更新并归档本计划；GitHub PR/Actions 保留远端权威事实，
   除非出现偏差、外部状态或遗留风险，否则不创建独立收尾 PR。

## 风险与控制

- **本地仅领先被误判为同步**：显式检查 left/right count，左侧非零即拒绝，不只依赖
  `pull --ff-only`；
- **CI 回读后远端再次前移**：入口要求完整已验证 SHA，并在 fetch 后与
  `origin/develop` 精确比较；
- **参数变成任意 source**：参数只作期望 SHA；fetch 和 fast-forward source 均硬编码；
- **脚本破坏用户提交**：失败路径禁止 reset、clean、stash、rebase 或自动建分支，测试
  核对 HEAD 与用户内容未变；
- **跨平台漂移**：原生行为测试分别由 Windows 本地与 Ubuntu CI 执行，静态契约始终比较
  两个入口；
- **网络或 Git 状态竞争**：fetch/解析/后置检查任一失败均非零退出；不宣称入口提供事务
  或锁，发生竞争时停止并重新读取状态；
- **本地结果冒充远端证据**：脚本不访问 GitHub；PR、合并和 Actions 结果必须单独回读；
- **流程成本增加**：入口只处理一个具体更新动作，不扩张为通用 Git 包装器。

## 回滚与停止条件

规划或功能 PR 合并前可关闭 PR。功能合入后通过新的受保护 PR 修正或 revert，不直接
改写 `develop`。出现以下任一情况立即停止：

- 当前分支、工作树、暂存区、`origin/develop` 或提交关系无法精确确认；
- 本地 `develop` 领先或分叉，需要 reset、丢弃、stash、自动建分支或复制提交才能继续；
- GitHub 无法证明 PR 合并结果的精确 `develop` SHA 或该 SHA 的最终 push `verify` 成功；
- 入口需要接受其他 source、切换分支、访问 GitHub、push 或修改远端；
- PowerShell/POSIX 不能保持相同 fail-closed 语义；
- evaluator 与 required `verify` 不能绑定同一 Subject SHA；
- 实际范围扩张到产品、架构、CI、依赖、远程权限、分支保护、部署或付费服务。

停止后报告精确状态和所需人类决定，不使用管理员绕过、强推、本地 feature 合并或把
`INCONCLUSIVE` 当作通过。

## 验收标准

- [ ] 规划 PR 只包含决策 023、活跃计划 020 和索引，并通过独立 evaluator 与同 SHA
  required `verify` 后在 GitHub 合入；
- [ ] `AGENTS.md` 明确禁止把其他本地分支 merge/rebase/cherry-pick 到本地 `develop`，
  并明确 GitHub PR → 最终远端 push `verify` → 本地安全更新顺序；
- [ ] PowerShell 与 POSIX 入口只接受完整已验证 SHA，唯一 source 为
  `origin/develop`，并执行 exact branch、clean/index、not-ahead/not-diverged 和
  fast-forward-only 前后检查；
- [ ] 所有失败非零退出、无修复性 mutation，并包含六字段诊断；
- [ ] 行为/静态测试覆盖成功、no-op、输入错误、错误分支、dirty/staged、本地领先、
  分叉、fetch/ref/SHA 错误和跨平台契约；
- [ ] generator 自检、不同 evaluator `PASS` 与 required `verify` 覆盖同一功能 PR
  Subject SHA；
- [ ] 功能 PR 只在 GitHub 合入，最终远端 `develop` 精确 SHA 的 push `verify` 成功后，
  本地 `develop` 才通过新入口从 `origin/develop` 纯快进到该 SHA；
- [ ] 没有本地 feature 合并、直接 push、管理员绕过、CI/远程权限变化或部署声明。
