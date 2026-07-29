# 执行计划 021：建立 Harness 目的与落地效果验证框架

状态：进行中（规划 PR）

## 目标与权威边界

实施[决策卡 024](../decisions/024-harness-effect-validation-goal.md)：把学习、验证和演进
Harness Engineering 实践确立为项目终极目的，以 StudyTrack CLI 作为受控实验载体，
建立一套最小、可审计且不以流程数量冒充成功的 Harness 落地效果框架。

`SPEC.md` 继续是完整产品行为与验收标准的唯一权威，`ARCHITECTURE.md` 继续定义技术分层
与依赖。本计划只规划 Harness 目的、导航和文档机械检查，不修改产品协议、架构、CLI
行为、数据格式、依赖、脚本、CI 身份、远程权限、分支保护或部署。

## 全程不变量

- `deusyu/harness-engineering` 是固定提交文件的学习输入，不自动凌驾于本仓库权威文档；
- “进入 `develop`”只是 Harness 落地前提，不等于已经产生效果；不得用 PR、提交、代码、
  文档、检查或智能体数量替代结果判断；
- 不补造历史基线、耗时、人工干预、缺陷率或因果归因；证据不足必须明确报告；
- 优先复用 PR、Actions、冻结 Subject SHA、generator/evaluator 和已有反馈记录，不要求
  每次评估都复制为新的 evidence 文件；
- generator 写入、协调者提交并冻结 Subject SHA、不同且不继承 generator 对话的
  evaluator 只读验证依次进行；任何新 SHA 使旧报告失效；
- evaluator `PASS`、required `verify` 和 PR head 必须绑定同一精确 SHA；
- 规划与功能变更只通过受保护 GitHub `codex/* → develop` PR 集成；不得在本地
  `develop` merge、rebase 或 cherry-pick，也不得直接 push 或管理员绕过；
- GitHub 合并后必须确认最终 `origin/develop` 精确 SHA 的 push `verify` 成功，之后才
  能通过仓库 updater 纯快进本地 `develop`；
- 所有结果只按实际发生的事实记录。规划不预写 evaluator、CI、PR、合并、远端分支、
  本地更新、Harness 效果或部署成功。

## 精确变更范围

### 规划 PR

只包含：

- `docs/decisions/024-harness-effect-validation-goal.md`；
- `docs/exec-plans/021-harness-effect-validation-goal.md`；
- `docs/README.md`。

规划 PR 不修改活跃 Harness 行为、`AGENTS.md`、`SPEC.md`、`ARCHITECTURE.md`、脚本、
测试、CI、产品代码、依赖、权限、分支保护或部署。

### 功能 PR

预计只包含以下逻辑范围：

- 新增根级 `HARNESS.md`，作为稳定的 Harness 目的与效果评估文档；
- `AGENTS.md`：只增加到 `HARNESS.md` 的简短导航及“终极目的/受控载体”边界，不复制
  评估手册；
- `docs/README.md`：把 `HARNESS.md` 加入当前事实导航，并在实施完成时把计划 021 的索引
  更新到归档路径；
- `src/test/java/com/example/studytrack/architecture/DocumentationNavigationTest.java`：
  扩展机械文档检查；
- `docs/decisions/024-harness-effect-validation-goal.md`：只按已经发生的本地实施事实更新
  状态与实施边界；
- 将本计划原样迁移到
  `docs/exec-plans/completed/021-harness-effect-validation-goal.md`，并只记录已发生的
  本地实施与 generator 自检。

若实施需要修改 `SPEC.md`、`ARCHITECTURE.md`、其他测试、脚本、workflow、产品代码、
依赖、GitHub 设置、权限、分支保护、数据格式或部署，立即停止并重新取得人类批准。

## `HARNESS.md` 最小内容契约

稳定文档只保存未来任务都需要的低变信息：

1. 明确终极目标、上游学习输入和 StudyTrack 受控载体三层边界；
2. 保留“人类掌舵、智能体执行”、仓库即记录系统、地图而非手册、机械化执行、智能体
   可读性、反馈回路和熵管理的本地采用方式；
3. 定义落地与效果的区别，以及结果正确性、自主性/人类掌舵负担、反馈回路有效性、
   可复现性/可追踪性、交付效率、熵/维护成本六个维度；
4. 提供最小评估声明：变化与假设、观察单元、适用维度、基线或无基线声明、实际结果与
   证据定位、反例/残余缺口、维护成本、结论；
5. 结论只允许表达正向、混合、无明显效果、负向或证据不足，不生成强制综合分数；
6. 明确优先复用既有记录、不得补造测量、流程体量不是成功、单次绿灯或合并不证明效果；
7. 链接 `SPEC.md`、`ARCHITECTURE.md`、`AGENTS.md`、决策 024 和固定提交的上游文件，
   不复制当前产品协议或上游全文。

`AGENTS.md` 只告诉智能体何时读取 `HARNESS.md`；具体字段留在稳定文档中，保持地图而非
手册。

## 机械文档检查设计

只扩展现有 `DocumentationNavigationTest`，不新建第二套测试类：

- `HARNESS.md` 必须存在，且能从 `AGENTS.md` 和 `docs/README.md` 发现；
- `HARNESS.md` 必须链接 `SPEC.md`，并明确 `SPEC.md` 是产品行为与验收标准的唯一权威、
  StudyTrack 是受控实验载体；
- 稳定文档必须保留六个效果维度、无基线/证据不足边界、既有记录复用和“流程体量不等于
  成功”的语义；
- 稳定文档必须保留固定上游学习链接及“学习输入不自动成为本仓库权威”的边界；
- 失败继续使用仓库既有 Location、Invariant、Reason、Fix、Recheck、Authority 六字段
  诊断，并指向 `HARNESS.md` 与决策 024。

测试只守护稳定语义和导航，不锁定整段措辞、历史计数、测量值或未来可演进的排版。

## 阶段 0：规划 PR

1. generator 只起草决策 024、活跃计划 021 和索引，不改变活跃 Harness 行为；
2. 使用 JDK 21 运行环境自检、`DocumentationNavigationTest` 和完整
   `.\mvnw.cmd verify`，结果只报告为 generator 自检；
3. generator 停止写入后，协调者审查精确 diff，只暂存三个规划文件，运行
   `git status --short` 与 `git diff --cached --check`，提交并冻结
   `FROZEN(<Subject SHA>)`；
4. 协调者在 evaluator 前运行
   `.\scripts\check-verification-subject.ps1 "<Subject SHA>"`；
5. 不同且不继承 generator 对话的 evaluator 使用最小交接，只读检查范围、目标层级、
   上游永久链接、未预写事实、导航测试和 JDK 21 完整 `verify`；
6. evaluator 后再次对同一 SHA 运行 guard。`FAIL` 回到 generator；任何修复提交都产生
   新 SHA 并使旧报告失效；`INCONCLUSIVE` 不得视为通过；
7. 只有 evaluator `PASS` 与 required `verify` 覆盖同一规划 PR head SHA 后，才可按
   人类批准在 GitHub 合并；不得本地合入 `develop`；
8. 合并后回读最终 `origin/develop` 精确 SHA 并确认该 SHA 的 push `verify` 成功，再在
   本地 `develop` 运行
   `.\scripts\update-local-develop.ps1 "<verified-develop-sha>"`。任一状态不明确即停止。

规划 PR 合入只批准后续实现，不表示 `HARNESS.md` 已存在或任何效果已经被观察。

## 阶段 1：功能实现与 generator 自检

1. 人类明确授权创建/切换从最新且已验证 `develop` 建立的 `codex/*` 分支后，generator
   按功能 PR 精确范围实施；
2. 先写最小 `HARNESS.md`，再更新两个导航入口和机械检查；不得修改产品或架构协议；
3. generator 运行：

```powershell
.\scripts\check-environment.ps1
.\mvnw.cmd -Dtest=DocumentationNavigationTest test
.\mvnw.cmd verify
```

4. 环境自检必须确认 JDK 21，完整验证必须使用 Maven Wrapper；失败时修复根因并重跑，
   不绕过 Enforcer、测试、Checkstyle 或架构门禁；
5. generator 检查工作树和范围后停止写入，只报告自检；不得 stage、commit、push、
   操作 GitHub、给出最终 `PASS` 或声称效果已验证。

## 阶段 2：冻结 Subject SHA 与独立 evaluator

协调者审查并只暂存功能 PR 的精确预期文件，运行 `git status --short` 和
`git diff --cached --check`，提交后冻结 Subject SHA。给 evaluator 的最小交接为：

```text
Task:
Acceptance criteria:
Subject SHA:
Generator:
Evaluator:
Mutation allowed: no
```

协调者在 evaluator 前后分别运行 verification subject guard。evaluator 必须与 generator
不同且不继承其对话，只依赖仓库、交接与精确 SHA，并至少：

- 核对功能 diff 没有改变产品、架构、脚本、CI 或权限边界；
- 独立检查 `HARNESS.md` 的三层目标、效果六维、最小声明、证据和反指标边界；
- 运行环境自检、针对性测试和 JDK 21 完整 `verify`；
- 报告 Subject SHA、Generator、Evaluator、命令、独立场景、发现、残余缺口和
  `PASS | FAIL | INCONCLUSIVE`。

前后 guard、evaluator 结论或 required `verify` 不能绑定同一 SHA 时停止。任何修复形成
新提交后，回到本阶段重新冻结和验证。

## 阶段 3：功能 PR、GitHub-only 集成与本地更新

1. 只推送 evaluator `PASS` 的精确 Subject SHA，创建 `codex/* → develop` GitHub PR；
2. 回读 PR base/head，确认 evaluator、PR head 与 required `verify` 绑定同一 SHA；
3. base 同步、修复或任何新提交改变 head 后，旧报告立即失效并回到阶段 2；
4. required `verify` 成功且 PR 与最新 `develop` 同步后，由人类决定是否在 GitHub 正常
   合并；不得管理员绕过，也不得在本地合并；
5. 合并后回读最终 `origin/develop` 精确 SHA，等待该 SHA 的 push `verify` 成功；失败或
   无法绑定精确 SHA 时停止；
6. 确认本地当前分支精确为 `develop` 后运行：

```powershell
.\scripts\update-local-develop.ps1 "<verified-develop-sha>"
```

7. 只读确认本地 `develop`、`origin/develop` 和已验证 SHA 一致，工作树干净且暂存区
   为空；
8. PR、Actions、evaluator 记录已足以覆盖交付事实时不创建重复 evidence 文件。只有发生
   偏差、外部状态或需要长期保留的效果观察时，再按决策 024 选择反馈/证据工件。

功能 PR 合并与 push `verify` 只能证明框架已按门禁落地。正向 Harness 效果仍需后续明确
观察单元的实际证据，不能在本阶段自动宣告。

## 风险与控制

- **目标再次被产品功能覆盖**：稳定文档和机械检查同时守护三层目标，`SPEC.md` 仍独占
  产品协议；
- **上游变成影子权威**：只引用固定提交文件，并明确任何采用都要经过本地决定和门禁；
- **评估沦为流程计数**：六维框架要求结果、负担和维护成本，明确排除 PR/检查数量代理；
- **无基线时制造改善故事**：允许“无基线”和“证据不足”，禁止回填未观察数字；
- **评估工件膨胀**：默认复用现有 PR/Actions/evaluator，只有现有记录不能表达时才新增
  专门工件；
- **机械检查过度锁死文案**：只检查稳定链接和语义锚点，不锁定全文、数字或排版；
- **同一任务自证成功**：generator 只自检，不给最终结论；不同 evaluator 验证冻结 SHA；
- **落地冒充效果**：PR 合并、CI 和 evaluator 只证明交付门禁，不自动证明后续效果；
- **本地 develop 污染**：所有集成在 GitHub 完成，本地仅在最终 push `verify` 后通过
  updater 从 `origin/develop` 纯快进。

## 停止与回退条件

出现以下任一情况立即停止并请求人类决定：

- 需要改变产品行为、架构、脚本、CI、依赖、权限、分支保护、远程设置或部署；
- 无法让稳定文档在不复制 `SPEC.md` 或上游全文的前提下表达目标；
- 需要引入遥测服务、付费平台、敏感数据或新的远程权限才能评估；
- 评估要求无法由可审计事实支持，只能依赖补造数字或主观成功叙事；
- generator/evaluator、前后 guard 和 required `verify` 无法绑定同一 Subject SHA；
- GitHub 无法确认最终 `develop` SHA 的 push `verify`，或本地 `develop` 不是 updater
  接受的安全状态。

规划或功能 PR 合并前可以关闭。功能合入后如框架错误，通过新的受保护 Harness PR 修正或
revert `HARNESS.md`、导航和对应测试；不得本地改写 `develop`、删除真实历史记录、关闭
required 门禁或把 `INCONCLUSIVE` 当作通过。

## 验收标准

- [ ] 规划 PR 只包含决策 024、活跃计划 021 和索引；
- [ ] `HARNESS.md` 稳定区分终极目标、上游学习来源和受控产品载体；
- [ ] `HARNESS.md` 定义六个效果维度、最小评估声明、证据复用、无基线和反指标边界；
- [ ] `AGENTS.md` 与 `docs/README.md` 提供简洁导航，不复制评估手册；
- [ ] `SPEC.md`、`ARCHITECTURE.md` 和 StudyTrack 产品行为未改变；
- [ ] 文档机械检查覆盖稳定目的、权威边界、效果维度和上游学习输入边界；
- [ ] generator 自检、不同 evaluator `PASS` 与 required `verify` 覆盖同一功能 Subject
  SHA；
- [ ] 规划与功能 PR 都只在 GitHub 合入，最终 `develop` 精确 SHA 的 push `verify`
  成功后，本地 `develop` 才通过仓库 updater 纯快进；
- [ ] 没有补造历史测量、重复强制证据文件、管理员绕过、远程权限变化或部署声明。
