# StudyTrack 产品规格

## 1. 产品目标

StudyTrack 是一个本地命令行工具，帮助个人记录、查看和完成学习任务。

本版本的目标是验证一套最小 Harness：产品行为可描述、完成标准可执行、错误能够自动反馈。

## 2. 命令约定

构建后通过可执行 JAR 使用：

```powershell
java -jar target/study-track.jar [全局选项] <命令> [命令选项]
```

默认数据文件是当前工作目录中的 `study-tasks.json`。所有命令都支持通过全局选项指定其他文件：

```powershell
java -jar target/study-track.jar --data-file .\tmp\tasks.json <命令>
```

### 2.1 添加任务

```powershell
java -jar target/study-track.jar add "阅读 Harness Engineering 概念总览"
```

成功时：

- 生成从 `1` 开始、单调递增且不复用的整数 ID；
- 持久化任务；
- 向标准输出写入：

```text
Created task 1: 阅读 Harness Engineering 概念总览
```

标题在校验和保存前使用 Java `String.strip()` 去除首尾空白。处理后的标题必须包含 `1..200` 个 Unicode 码点。

标题无效时：

- 向标准错误写入 `Task title must contain between 1 and 200 characters.`；
- 退出码为 `2`；
- 不创建或修改数据文件。

### 2.2 查看任务

查看全部任务：

```powershell
java -jar target/study-track.jar list
```

按 ID 从小到大输出：

```text
[ ] 1 阅读 Harness Engineering 概念总览
[x] 2 完成第一次 Harness 实验
```

其中 `[ ]` 表示未完成，`[x]` 表示已完成。没有匹配任务时输出：

```text
No tasks.
```

只查看未完成或已完成任务：

```powershell
java -jar target/study-track.jar list --status pending
java -jar target/study-track.jar list --status completed
```

`--status` 只接受 `pending` 和 `completed`。其他值由命令行解析器报告为参数错误并返回退出码 `2`。

### 2.3 完成任务

```powershell
java -jar target/study-track.jar complete 1
```

首次完成任务时：

- 将任务状态持久化为已完成；
- 向标准输出写入 `Completed task 1.`；
- 退出码为 `0`。

重复完成已经完成的任务时：

- 不修改数据文件；
- 向标准输出写入 `Task 1 is already completed.`；
- 退出码为 `0`。

任务不存在时：

- 向标准错误写入 `Task 99 not found.`；
- 退出码为 `2`；
- 不修改数据文件。

## 3. 数据格式

数据以 UTF-8 JSON 保存：

```json
{
  "nextId": 2,
  "tasks": [
    {
      "id": 1,
      "title": "阅读 Harness Engineering 概念总览",
      "completed": false
    }
  ]
}
```

规则：

- 数据文件不存在时，将其视为空任务库；
- `nextId` 是下一条任务的 ID；
- 删除功能不在本版本范围，因此已有 ID 不会被复用；
- 写入必须采用临时文件加替换的方式，避免留下半写入的 JSON；
- JSON 无法解析时，向标准错误报告数据文件错误，返回退出码 `1`，不得覆盖原文件；
- 自动测试必须使用临时目录，不得读写用户的真实数据文件。

## 4. 本版本不包含

- 用户账号和多人协作；
- 网络同步、HTTP 服务和数据库；
- 图形界面；
- 截止日期、提醒、标签和优先级；
- 编辑和删除任务；
- 导入、导出及数据迁移。

增加上述能力前，必须先修改本规格及相应验收标准。

## 5. 验收标准

| ID | 可验证的完成条件 |
|---|---|
| AC-01 | 添加任务后生成递增 ID，并将处理后的标题持久化到 JSON。 |
| AC-02 | 进程重新启动后能够读取之前保存的任务。 |
| AC-03 | `list` 按 ID 排序，并能筛选全部、未完成和已完成任务。 |
| AC-04 | `complete` 能持久化完成状态，重复执行具有幂等行为。 |
| AC-05 | 空标题或超过 200 个 Unicode 码点的标题返回退出码 `2`，且不修改数据。 |
| AC-06 | 不存在的任务 ID 返回退出码 `2`，且不修改数据。 |
| AC-07 | JSON 损坏时返回退出码 `1`，保留原始文件。 |
| AC-08 | CLI 不直接访问文件系统或 JSON 库。 |
| AC-09 | Application 层不依赖具体持久化实现。 |
| AC-10 | Checkstyle、ArchUnit 和全部自动测试通过。 |
| AC-11 | 构建生成可通过 `java -jar target/study-track.jar` 运行的 JAR。 |

统一验收入口：

```powershell
.\mvnw.cmd verify
```
