# 执行计划 016：实施 CI PR 完整差异空白门禁

状态：远程负向与恢复验证完成，待功能 PR 合并与最终 main 验证

## 目标

实施[决策卡 016](../../decisions/016-ci-pr-diff-whitespace-gate.md)：在现有受保护 `main`
所要求的 `verify` Job 中，对每个 PR 的完整 `base...head` 差异运行 Git 空白错误检查，
同时保持 `push` 事件的既有环境自检和 Maven `verify`，并保持 Windows 本地完整验证
不依赖系统 `sh`。

本计划只记录实施步骤、风险、验证与停止条件，不改变产品规格或架构分层。

## 预计变更范围

- `.github/workflows/verify.yml`：checkout 使用完整历史；只在 `pull_request` 事件传入
  base/head SHA 并调用仓库脚本；
- `scripts/check-pr-diff.sh`：接受 base/head SHA，运行
  `git diff --check "$base...$head"`，失败时给出符合导航约定的诊断；
- `AGENTS.md` 与 `ARCHITECTURE.md`：更新当前有效的验证地图，说明 Maven 是本地完整
  产品/架构验证入口，PR 的同名必需 Job 还会检查完整差异；
- `src/test/java/com/example/studytrack/architecture/` 下的适当静态契约测试：读取
  workflow 与脚本文本，保护关键接线，不执行 shell；
- 本计划与[证据 007](../../evidence/007-ci-pr-diff-whitespace-poc.md)：在功能实施阶段只根据
  已发生事实更新状态和证据边界。

不修改 `SPEC.md`、产品代码、产品测试、依赖、JSON、分支保护、远程权限或 Job 名称。

## 实施步骤

### 阶段 1：脚本与完整历史

1. 将 `actions/checkout` 配置为获取完整历史，保证 PR base、head 与合并基点均可达；
2. 新增最小 POSIX 脚本，要求恰好两个参数：base SHA 与 head SHA；
3. 以引用安全的参数构造 `"$base...$head"`，执行 Git 原生命令
   `git diff --check "$base...$head"`；
4. 成功时退出 `0`；参数无效、Git 无法解析范围或发现空白错误时非零退出，不吞掉 Git
   给出的文件与行号；
5. 空白错误反馈同时包含以下六项：
   - 错误位置：Git 原始诊断中的文件与行号；
   - 被违反的不变量：PR 完整 `base...head` 差异不得包含 Git 空白错误；
   - 原因：明确说明 `git diff --check` 检测到的错误；
   - 修复方向：删除行尾空白或多余文件尾空行等对应内容；
   - 复验命令：使用相同 base/head 重新运行仓库脚本；
   - 权威链接：链接本决策以及 Git `diff --check` 官方文档。

### 阶段 2：PR-only workflow 接线

1. 在现有 `verify` Job 中增加带事件条件的步骤，仅当
   `github.event_name == 'pull_request'` 时运行；
2. 从 `github.event.pull_request.base.sha` 与 `.head.sha` 取得不可由仓库内容控制的
   SHA，并作为两个独立参数传给脚本；
3. 不使用 `HEAD^`、最后一次提交、工作树或暂存区替代完整 PR 范围；
4. 保持环境自检和 Maven `verify` 在 `push` 与 `pull_request` 两种事件下都运行；
5. 不改变 workflow 名、Job 名、触发器、权限或受保护分支设置。

### 阶段 3：当前验证地图与机械契约

1. 更新 `AGENTS.md`，区分提交前 cached diff 检查、本地 Maven `verify` 和 PR-only
   完整差异门禁，并保持稳定导航而不追加历史流水账；
2. 更新 `ARCHITECTURE.md` 第 7 节当前验证流水线，描述同一 `verify` Job 的事件语义，
   不把 Harness 规则误写成产品架构依赖；
3. 添加 Java 静态契约测试，至少验证完整 checkout、PR-only 条件、base/head 上下文、
   `base...head` 命令和六要素反馈标记存在；
4. 静态测试只用 Java 读取仓库文件，不启动 `sh`。因此 Windows
   `.\mvnw.cmd verify` 不以系统安装 POSIX shell 为前提；
5. 保留文档导航测试对新工件索引和本地链接的检查。

### 阶段 4：本地验证

1. 在 JDK 21 下运行环境自检；
2. 运行新增静态契约测试与 `DocumentationNavigationTest`；
3. 使用临时 Git 对象或临时仓库构造一个干净范围和一个新增文件尾空行的范围，在可用的
   POSIX 执行环境中直接调用脚本，分别确认退出 `0` 与非零，并核对六要素诊断；
4. 临时对象不得挂到长期分支、不得推送；临时文件、索引和工作树必须恢复；
5. 运行 JDK 21 完整 `.\mvnw.cmd verify`，确认 Windows Maven 门禁不依赖系统 `sh`；
6. 只把实际命令、提交哈希、退出码与清理结果追加到证据，不把预计结果写成事实。

### 阶段 5：功能 PR 远程负向探针

1. 通过受保护流程创建功能 PR，确认实现本身的静态测试与 Maven `verify` 正常；
2. 在功能分支提交一个专用临时文件，其文件尾包含 Git 会报告的新增空行，并推送该提交；
3. 观察 PR 的必需 `verify` 失败，保存 Actions 权威记录，核对诊断包含位置、不变量、
   原因、修复、复验和权威链接；
4. 删除该临时文件并提交修复，使 PR 相对 base 的最终完整差异不再包含探针；
5. 推送后确认同一 PR 的必需 `verify` 成功；不得用重跑旧 Job、管理员绕过或修改门禁
   代替修复差异；
6. GitHub PR 与 Actions 保存远程失败和恢复的权威证据；功能 PR 在合并前只把已经发生
   的本地实施和探针事实写入证据，并把计划移入 `completed/`，不得预填合并提交或最终
   `main` 结果。

### 阶段 6：合并与最终 main

1. 审查功能 PR 的最终差异，确认临时探针文件不存在且未扩大范围；
2. 在必需 `verify` 成功后通过受保护 PR 合并；
3. 检查最终 `main` 的远程 `verify` 成功；
4. 默认不创建独立收尾 PR；仅在决策卡 011 规定的外部状态、结果偏差或遗留风险需要
   仓库记录时升级。

## 风险与控制

- 风险：浅 checkout 找不到合并基点，导致误报或漏检。
  控制：完整历史 checkout，并由静态契约测试保护。
- 风险：检查最后一个提交而漏掉 PR 前序提交中的空白错误。
  控制：只使用事件中的 base/head SHA 和三点范围。
- 风险：PR 之外的 `push` 事件没有 base/head，却错误运行脚本。
  控制：步骤级 PR-only 条件；push 保持原有环境与 Maven 验证。
- 风险：脚本只返回 Git 片段，未满足可操作诊断。
  控制：保留 Git 位置并补齐六要素，静态测试和远程负向探针共同验证。
- 风险：为了测试 POSIX 脚本让 Windows Maven 构建依赖 `sh`。
  控制：Maven 内只运行文本级 Java 契约测试；脚本行为由显式本地探针和 Linux Actions
  验证。
- 风险：负向探针污染最终 PR。
  控制：使用专用临时文件，后续提交删除，并在合并前检查完整 PR 差异。

## 停止条件

出现以下任一情况时停止实施并报告，不得自行扩大：

- 需要改变分支保护、远程权限、workflow/Job 身份或增加外部服务；
- 需要把门禁扩展到空白错误之外的格式化、lint、秘密或安全策略；
- 需要让 `push` 事件模拟 PR base/head，或削弱既有环境自检和 Maven `verify`；
- 需要修改产品、`SPEC.md`、核心架构分层、依赖、数据格式或迁移；
- GitHub PR SHA、checkout 历史或三点范围的实际语义与计划不符；
- Windows 完整 `verify` 无法在不依赖系统 `sh` 的情况下保持通过。

## 进度

- [x] 人类批准第三级门禁语义变更与推荐方案
- [x] 本地 `git diff --check` 干净/失败 POC 完成并清理
- [x] 决策卡、执行计划和 POC 证据起草
- [x] 规划 PR 通过并合并
- [x] 功能脚本、workflow、验证地图和静态契约测试完成
- [x] 本地脚本探针与 JDK 21 完整 `verify` 通过
- [x] 功能 PR 远程负向探针按预期失败并给出完整诊断
- [x] 删除探针后功能 PR 必需 `verify` 通过
- [ ] 功能 PR 通过并合并
- [ ] 最终 `main` 远程 `verify` 成功

## 当前证据边界

[证据 007](../../evidence/007-ci-pr-diff-whitespace-poc.md)记录了规划阶段 Git 原生 POC，
功能实施阶段已经发生的本地脚本探针、静态契约测试、JDK 21 完整 `verify` 和临时仓库
清理事实，以及 PR #28 已发生的远程负向失败、六字段诊断和删除探针后的远程恢复成功。

功能 PR 合并与最终 `main` 均尚未观察，不能由当前可合并状态推断。按照决策卡 011 的
精简三级流程，本计划在远程恢复实际完成后、功能 PR 合并前归档；其余远程事实以实际
GitHub PR 与 Actions 记录为权威。
