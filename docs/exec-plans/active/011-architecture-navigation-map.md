# 执行计划 011：架构导航地图降粒度

状态：待实现

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

- [ ] 无父对话子智能体开始
- [ ] 第 3 节改为包级导航
- [ ] 非完整清单边界写明
- [ ] 架构规则完整保留
- [ ] 完整 `verify` 通过
- [ ] 主智能体审查
- [ ] PR 通过并合并
