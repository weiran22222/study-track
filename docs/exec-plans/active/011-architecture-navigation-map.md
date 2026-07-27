# 执行计划 011：架构导航地图降粒度

状态：主智能体审查通过，待 PR 门禁

## 目标

实施决策卡 005，把 `ARCHITECTURE.md` 的完整文件树改为稳定的包级导航和少量关键入口，
同时完整保留真正的架构约束。

## 范围

- 修改 `ARCHITECTURE.md` 第 3 节标题、地图与说明；
- 展示五个生产包及其职责；
- 列出五个关键入口及其定位作用；
- 更新本计划的实现记录和进度。

## 非目标

- 不修改 Java 源码或测试；
- 不增加目录一致性测试或生成脚本；
- 不修改 ArchUnit、Checkstyle、Maven 或 GitHub Actions；
- 不改变产品规格、分层职责或依赖方向；
- 不把测试目录重新展开成完整文件清单。

## 实施约束

1. 包级地图必须包含 `bootstrap`、`cli`、`application`、`domain`、`infrastructure`；
2. 关键入口只包含：
   - `StudyTrackApplication`；
   - `StudyTrackCommand`；
   - `StudyTaskService`；
   - `TaskRepository`；
   - `JsonTaskRepository`；
3. 文档必须明确“非完整文件清单”；
4. 必须给出 Git、IDE 或 `rg --files` 的完整文件查询方式；
5. 第 1、2、4、5、6、7 节不得发生语义变化。

## 验证

- 审查 diff，确认只修改授权文档；
- 搜索五个包和五个关键入口均存在；
- 搜索不存在完整命令类列表；
- `git diff --check` 通过；
- JDK 21 下完整 `verify` 通过。

## 进度

- [x] 无父对话子智能体开始
- [x] 第 3 节改为包级导航
- [x] 非完整清单边界写明
- [x] 架构规则完整保留
- [x] 完整 `verify` 通过
- [x] 主智能体审查（第 3 节外无语义变化；JDK 21 独立复跑 43 项测试通过）
- [ ] PR 通过并合并

## 实现记录

- 2026-07-27：将 `ARCHITECTURE.md` 第 3 节替换为五个生产包的职责导航，并只保留
  `StudyTrackApplication`、`StudyTrackCommand`、`StudyTaskService`、`TaskRepository`
  和 `JsonTaskRepository` 五个关键入口；
- 明确该节不是完整文件清单、不展开测试目录，完整文件集合使用 Git、IDE 或
  `rg --files` 查询；
- diff 只涉及 `ARCHITECTURE.md` 第 3 节和本计划记录，第 1、2、4、5、6、7 节未修改，
  Java 源码、测试、构建和 CI 配置均未修改；
- JDK 21 环境自检通过；五个包与五个关键入口搜索通过，完整命令类列表搜索无结果，
  `git diff --check` 通过，完整 `.\mvnw.cmd verify` 通过（43 个测试）。
