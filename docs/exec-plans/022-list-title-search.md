# 执行计划 022：按标题字面子串筛选任务列表

状态：实施中（功能 generator 本地自检已完成）

## 目标与权威边界

实施[决策卡 027](../decisions/027-list-title-search.md)和
[`SPEC.md` 2.2 节及 AC-18](../../SPEC.md#22-查看任务)，为 `list` 增加已批准的标题筛选
能力。本计划只记录实施阶段、风险、验证方法、停止条件和进度；完整产品协议只以
`SPEC.md` 为权威。

本任务使用“规划 PR → 功能 PR → 观察收尾 PR”。最后一个 PR 是本次跨 PR 前瞻观察的
必要收尾：它只能在功能合并及精确最终 `develop` push `verify` 已有可定位记录后，回填
实际证据、反例、成本与结论并归档本计划。

## 阶段 0：规划 PR

1. 只修改 `SPEC.md`、决策 027、本活动计划、前瞻观察 006 和 `docs/README.md`；
2. 审查五份工件的单一职责、本地链接、未发生事实边界和第三级流程一致性；
3. 运行 JDK 21 环境自检和完整 `.\mvnw.cmd verify`，只报告实际本地结果，不把它写成
   evaluator、required CI、PR 或合并事实；
4. generator 停止写入后，由协调者在获授权的正常流程中审查、提交和冻结规划 Subject
   SHA，再交给不同 evaluator 只读验证；
5. 只有规划 evaluator 与 required `verify` 覆盖同一规划 PR head，且人类决定合并后，
   才进入功能阶段。

## 阶段 1：功能实现与 generator 自检

1. 从已合入规划的最新且已验证 `develop` 开始，在人类授权的功能分支上检查既有列表
   Application、CLI 组合和测试；
2. 以最小改动实现 `SPEC.md` 已批准的查询归一化、校验、标题匹配及与状态条件组合，不
   改变持久化模型或写入路径；
3. 增加 Application、CLI 和端到端自动测试，覆盖 Unicode 边界、字面与大小写语义、
   组合筛选、排序/格式、空结果、无数据文件、损坏 JSON 和无副作用；
4. 使用系统临时目录构建并运行真实 JAR 代表场景，比较必要路径的数据文件存在性与
   操作前后字节或 SHA-256；
5. 运行 JDK 21 环境自检、定向测试和完整 `.\mvnw.cmd verify`。失败时修复根因并重跑，
   记录实际 generator 修复轮次，不绕过门禁；
6. generator 只报告自检，不 stage、commit、push、操作 GitHub、给出最终 `PASS` 或
   回填尚未发生的远程事实。

## 阶段 2：冻结 SHA、独立 evaluator 与功能 PR

1. 协调者审查功能范围，只提交批准的实现、测试和必要文档更新，冻结精确 Subject SHA；
2. 在 evaluator 前后对同一 SHA 运行 verification subject guard；
3. 不同且不继承 generator 对话的 evaluator 只读核对规格边界、自动测试、真实 JAR
   场景和 JDK 21 完整 `verify`，报告 findings、残余缺口与
   `PASS | FAIL | INCONCLUSIVE`；
4. `FAIL` 回到 generator；任何修复产生新 SHA 后重新冻结和验证。`INCONCLUSIVE` 不得
   视为通过；
5. 只有 evaluator 与 required `verify` 覆盖同一功能 PR head，才可由人类决定是否通过
   受保护 PR 合入 `develop`；
6. 合并后回读精确最终 `develop` SHA，等待并确认该 SHA 的 push `verify`。无法精确绑定
   SHA、运行失败或状态不明时停止，不更新为成功。

## 阶段 3：观察收尾 PR

1. 从规划 PR 记录、功能 PR diff/review、冻结 SHA、generator/evaluator 报告、Actions
   和精确最终 `develop` push `verify` 提取可定位的实际事实；
2. 更新前瞻观察 006，记录规格批准后的额外澄清、generator 修复轮次、evaluator
   findings、CI 失败原因、人工操作、等待边界、反例、残余缺口和维护成本；
3. 必需人类产品决策与规格批准后的非计划人工补救分别记录；不得使用 PR
   created-to-merged 时间推断效率，不补造有效工作时间或节省比例；
4. 按 `HARNESS.md` 允许的枚举填写结论。首个样本即使交付成功也不能单独证明正向效果；
5. 将本计划移入 `docs/exec-plans/completed/`，同步索引，并通过新的受保护收尾 PR 和
   适用门禁。收尾 PR 自身及其最终 `develop` 验证不在被观察的产品交付单元内，避免
   无限延伸观察边界。

## 风险与控制

- **把查询当作正则或忽略大小写处理**：用元字符、大小写差异和普通 Unicode 文本的定向
  测试保护 `SPEC.md` 的字面语义；
- **使用 UTF-16 长度代替 Unicode 码点**：覆盖补充平面字符的 200/201 码点边界，并验证
  `strip()` 后长度；
- **无效查询仍访问数据**：使用会暴露读取的 Repository 测试替身，并以损坏或不存在的
  数据文件验证校验优先级；
- **组合筛选变成 OR 或改变顺序/格式**：构造交叉状态与标题数据，精确断言 AND、ID
  升序、单行格式和 `No tasks.`；
- **只读命令产生副作用**：对成功、无匹配、无数据文件、无效查询和损坏数据路径检查
  文件存在性及操作前后字节或哈希；
- **为搜索顺便增加功能或依赖**：只实施 `SPEC.md` 已批准边界，触发停止条件时回到人类
  决策；
- **把门禁成功冒充 Harness 效果**：观察保留无基线、首样本和反例边界，只在收尾阶段
  使用实际跨 PR 证据；
- **文档与观察成本超过收益**：收尾时记录新增协议、测试、运行、等待和认知表面，不删除
  负面证据。

## 验证方法

- 环境：`.\scripts\check-environment.ps1` 必须确认 `java` 与 Maven Wrapper 使用 JDK 21；
- Application：验证归一化、Unicode 码点边界、标题匹配、状态 AND、排序及 Repository
  访问/写入次数；
- CLI：精确验证 stdout、stderr、退出码、参数错误优先级和既有格式；
- 持久化与端到端：使用临时目录覆盖存在/不存在/损坏数据文件，并检查只读与失败安全；
- 真实 JAR：通过 `java -jar target/study-track.jar --data-file <temp> list ...` 验证代表
  成功、组合、空结果、无效查询、无文件和损坏 JSON 场景；
- 完整门禁：在 JDK 21 下运行仓库唯一完整入口 `.\mvnw.cmd verify`；
- 独立验证：不同 evaluator 对冻结 Subject SHA 重做关键黑盒场景和完整门禁，前后 guard
  保证验证对象未变。

## 停止条件

出现以下任一情况立即停止并请求人类决定，不得自行扩大范围：

- 必须改变 JSON 格式、迁移数据、增加 Repository 写协议或修改架构依赖方向；
- 必须增加外部依赖、服务、遥测、脚本、CI、权限、分支保护、远程设置或部署；
- 规格中的匹配、错误优先级、组合、输出、退出码或只读语义无法按现有边界实现；
- 实际需求扩展到忽略大小写、正则、模糊搜索、查询文件、高亮、批量编辑或其他命令；
- generator/evaluator、前后 guard、功能 PR head 和 required `verify` 无法绑定同一 SHA；
- 无法定位精确最终 `develop` SHA 的 push `verify`，或观察只能依赖补造测量和因果叙事。

## 进度

- [x] 人类批准产品目标、最小边界和第三级规划
- [x] 五个规划工件已起草
- [ ] 规划 PR 已创建并通过 evaluator 与同 SHA required `verify`
- [ ] 规划 PR 已由人类决定合入 `develop`
- [x] 功能 generator 已完成实现、自动测试、真实 JAR 与完整 `verify` 自检
- [ ] 功能 Subject SHA 已由不同 evaluator 独立验证
- [ ] 功能 PR required `verify` 与 evaluator 覆盖同一 head 并由人类决定合并
- [ ] 精确最终 `develop` SHA 的 push `verify` 已成功
- [ ] 观察收尾 PR 已回填实际结果并归档本计划

当前勾选项只表示已经发生的人类批准、文档起草和本地 generator 自检，不表示任何 PR、
远程 CI、evaluator 结论、合并、最终 `develop` 验证或 Harness 效果已经发生。

### 2026-07-29 功能 generator 本地结果

- 在既有 Application/CLI 分层内实现查询 `strip()`、Unicode 码点校验、Java
  `String.contains` 字面匹配及与状态条件的 AND 组合；未改变 Repository 协议、JSON
  格式或写入路径；
- 增加 Application 与组合 CLI 自动测试，机械检查无效查询时 Repository 零调用，并
  覆盖 200/201 个补充平面码点、大小写、字面元字符、排序/格式、无匹配、无文件、损坏
  JSON、缺参数和文件无副作用；
- 初始环境自检因继承的 JDK 17 失败；只为命令进程选择本机 JDK 21 后通过。第一次定向
  测试成功但出现 5 条新增 Checkstyle 使用距离告警，声明相应测试变量为 `final` 后重跑
  不再出现；
- JDK 21 定向测试实际为 68 项、0 失败；完整 `.\mvnw.cmd verify` 实际为 139 项、
  0 失败，并生成真实 `target/study-track.jar`；
- 在系统临时目录运行真实 JAR 的字面元字符、大小写、状态 AND、无匹配、无效查询、
  缺参数、无文件和损坏 JSON 代表场景，核对退出码、输出、文件存在性和 SHA-256；
- Windows 原生命令行未能可靠承载 200 个补充平面字符的手工内联参数；调用端测试值为
  200 个码点，但原生启动链传给子进程的参数已转换。未扩大为查询文件功能，200/201
  码点语义由 JVM 内 Application 与组合 CLI 自动测试覆盖；
- 完整实际命令、结果、哈希与未发生事实边界见
  [证据 008](../evidence/008-list-title-search.md)。本节只是 generator 本地进度，不是
  evaluator `PASS`，不表示任何 PR、CI、合并或最终 `develop` 事实。
