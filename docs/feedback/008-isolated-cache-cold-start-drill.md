# 隔离 Maven 缓存的新克隆冷启动演练

状态：已完成（证据不足）

日期：2026-07-30

本文记录一次已经发生的 Windows 冷启动演练。它只建立“新远程克隆可以从隔离且为空的
Maven Wrapper/依赖缓存起点完成仓库验证”这一窄操作事实，不把单次成功写成干净机器证明
或 Harness 正向因果效果。

## 1. 变化与假设

被观察的是仓库既有的 JDK 21 环境自检、Maven Wrapper 和完整 `verify` 入口，没有新增
Harness 机制。假设是：在同一 Windows 主机上，从 GitHub 远程新克隆精确 Subject，并把
Wrapper 用户目录与 Maven 本地依赖仓库同时指向运行前不存在的隔离目录，现有入口能够
下载所需内容并完成验证。

演练不改变产品、架构、构建、脚本、CI 或远程配置。即使操作成功，也只能说明这一次受控
环境中的入口可运行，不能单独证明现有 Harness 导致正确性、效率或可复现性改善。

## 2. 观察单元

观察对象是从 `https://github.com/weiran22222/study-track.git` 新克隆到
`C:\Users\weiran\AppData\Local\Temp\studytrack-cold-start-d8262164` 的仓库。基线与
Subject SHA 均精确为
`d8262164b3ba910a9f4b08d0ec30f02699d443dc`。

主机是既有 Windows 主机，已有 JDK 21，网络可用，GitHub 远程克隆所需的宿主机状态与
凭据也已存在。正式观察使用第二个 Maven 用户目录
`C:\Users\weiran\AppData\Local\Temp\studytrack-cold-start-d8262164-maven-home-v2`；
该目录在运行前不存在，因此以空目录状态为起点。运行同时设置：

- `MAVEN_USER_HOME` 为上述隔离目录；
- `-Dmaven.repo.local` 为上述隔离目录下的 `repository`；
- `JAVA_HOME` 为宿主机既有的 JDK 21。

实际入口依次为 `.\scripts\check-environment.ps1` 和
`.\mvnw.cmd "-Dmaven.repo.local=<隔离目录>\repository" verify`。冷启动标准输出与
错误输出分别保存在临时 `...-out.log` 与 `...-err.log`；随后同一隔离目录的 warm replay
分别保存在 `...-warm-out.log` 与 `...-warm-err.log`。

正式观察前的第一次编排只给前台调用 10 秒等待时间。等待超时前 Maven 已经向第一个
`...-maven-home` 写入内容，使它不再满足空缓存起点；因此该目录和该次调用被排除，不参与
正式观察。10 秒编排超时只说明调用方没有在窗口内取得结果，既不能写成仓库失败，也不能
写成仓库成功。

## 3. 适用维度

- **结果正确性**：观察既有自检与 `verify` 在本次精确对象和环境中的结果，但不推断验收
  范围外没有缺陷；
- **反馈回路有效性**：观察日志是否给出 Java、Wrapper、测试汇总、跳过和构建结果；
- **可复现性与可追踪性**：记录精确 SHA、隔离变量、日志、退出码边界和 Git 状态；
- **交付效率**：只记录两次 Maven 自报历时，不用它们推断相对效率；
- **自主性与人类掌舵负担**不评估；本次演练没有可靠的人工负担对照；
- **熵与维护成本**只记录本反馈、索引和稳定锚点的新增维护表面，不作效果判断。

## 4. 基线状态

仓库对象有可靠基线：新克隆的 `HEAD`、本次 Subject 和当前记录起点都是
`d8262164b3ba910a9f4b08d0ec30f02699d443dc`。正式冷启动的 Maven 基线是运行前不存在的
第二隔离目录，而不是宿主机默认 Maven 缓存。

**无可靠干净机器或同类效果比较基线。** 本次仍复用同一主机的 OS、既有 JDK、网络、
Git 凭据和其他宿主机状态；此前启用缓存的 CI 与本机默认缓存运行也不是可比较的无缓存
对照。因此只建立当前窄操作事实，不倒推历史冷启动耗时或改善比例。

## 5. 实际结果与证据

冷启动日志记录了环境自检通过、`Java: 21` 和
`Maven Wrapper: Apache Maven 3.9.12`。同一日志包含从 Maven Central 下载 Wrapper/
构建依赖后的完整 Maven 汇总：

- `Tests run: 157, Failures: 0, Errors: 0, Skipped: 1`；
- 唯一跳过是 Windows 上未显式提供 `STUDYTRACK_POSIX_SHELL` 时的 POSIX guard 场景；
- `BUILD SUCCESS`；
- Maven 自报 `Total time: 04:59 min`。

该冷启动由后台启动方式运行并重定向日志；这种启动方式没有保留该 Maven 进程的 OS 退出
码。因此这里不把日志中的 `BUILD SUCCESS` 补写为退出码 `0`，也不声称取得了未保留的
退出状态。

随后在同一 `...-maven-home-v2` 隔离目录执行 warm replay。持久日志再次记录环境自检
通过、157 个测试、0 failures、0 errors、1 个相同的 Windows skip、`BUILD SUCCESS`、
`Total time: 48.080 s` 和 `PROCESS_EXIT_CODE=0`；外层 `Start-Process` 也返回退出码
`0`。这次复验使用的已是冷启动填充后的缓存，只证明同一隔离目录可直接复验，不能替代
空缓存观察。

运行后只读 Git 核对得到：

- `git rev-parse HEAD` 精确为
  `d8262164b3ba910a9f4b08d0ec30f02699d443dc`；
- `git status --short --branch` 显示 `develop...origin/develop` 且工作树干净；
- `origin` 仍为上述 GitHub URL。

这些是 generator 自检与观察记录，不是独立 evaluator `PASS`。

## 6. 反例与残余缺口

- 第一次 10 秒编排超时污染了第一个 Maven home，说明调用编排本身会破坏预定的空缓存
  起点；本次通过排除该目录并使用运行前不存在的第二目录恢复了观察边界；
- 冷启动后台运行没有保留 OS 退出码，不能把 Maven 日志结论扩写为进程退出码事实；
- 这不是全新机器，也没有隔离 OS、JDK、网络、Git 凭据或其他宿主机状态；
- 本次不证明离线构建、跨平台结果、无缓存 CI 或零跳过；实际结果明确有 1 个 Windows
  skip；
- 单次新克隆和一次 warm replay 没有反事实，不能隔离任务对象、网络、主机、缓存策略与
  Harness 机制的影响，也不能证明 Harness 的正向因果效果。

## 7. 维护成本

本次新增一份反馈记录，在历史索引和能力图增加一个稳定入口，并在现有文档导航测试中保护
该链接及“非干净机器证明/隔离边界”的核心语义。这增加少量文档、链接和断言维护成本，
但不新增脚本、门禁、决策卡、执行计划、服务、定时机制或持续遥测。

临时克隆、隔离 Maven 目录和日志属于本机演练证据，不进入仓库；未来若临时目录被清理，
仓库仍只保留本文记录的已发生结果与证据边界，不把临时文件存在性写成长期保证。

## 8. 结论

**证据不足。**

窄操作事实是：精确 Subject 的新 GitHub 克隆在同一 Windows 主机上，以运行前不存在的
第二 Maven 用户目录同时隔离 `MAVEN_USER_HOME` 与 `maven.repo.local` 后，环境自检和
Maven `verify` 日志成功完成；同一隔离目录的 warm replay 又以退出码 `0` 成功。由于
主机、OS、JDK、网络、Git 凭据和其他宿主机状态均未隔离，且没有可比较基线或因果对照，
这仍非干净机器证明，也不能得出 Harness 正向效果结论。
