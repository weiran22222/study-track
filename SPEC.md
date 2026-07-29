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

按已保存标题搜索任务：

```powershell
java -jar target/study-track.jar list --contains "Harness"
```

`--contains` 的查询文本先使用 Java `String.strip()` 去除首尾空白，处理后必须包含
`1..200` 个 Unicode 码点。有效查询对每个已保存标题执行区分大小写、非正则、非模糊的
字面子串匹配；正则表达式元字符没有特殊含义。

查询文本无效时：

- 向标准错误精确写入 `Search text must contain between 1 and 200 characters.`，标准输出
  不输出内容；
- 退出码为 `2`；
- 在读取任务数据前失败，不读取、创建或修改数据文件。

缺少 `--contains` 的参数时，由命令行解析器报告为参数错误并返回退出码 `2`，同样不得
读取、创建或修改数据文件。

标题搜索可以与状态筛选同时使用：

```powershell
java -jar target/study-track.jar list --status pending --contains "Harness"
```

同时使用时两个条件按 AND 组合。所有成功的 `list` 查询都按 ID 从小到大输出，并保持
本节既有的单行格式；没有匹配任务时输出 `No tasks.`。成功查询只读、退出码为 `0`，不得
创建或修改数据文件。数据文件不存在时视为空任务库；JSON 损坏时沿用第 3 节的数据文件
错误格式和失败安全规则，退出码为 `1`。

第一版标题搜索不提供忽略大小写、正则表达式、模糊搜索、查询文件或结果高亮。

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

### 2.4 查看单个任务

```powershell
java -jar target/study-track.jar show 1
```

任务存在时，使用与 `list` 相同的单行格式输出：

```text
[ ] 1 阅读 Harness Engineering 概念总览
```

已完成任务使用 `[x]`。命令成功时退出码为 `0`，不得修改数据文件。

任务不存在时：

- 向标准错误写入 `Task 99 not found.`；
- 退出码为 `2`；
- 不创建或修改数据文件。

非整数 ID 由命令行解析器报告为参数错误并返回退出码 `2`。JSON 损坏时继续遵守
第 3 节的失败安全规则。

### 2.5 任务统计

```powershell
java -jar target/study-track.jar summary
```

固定输出三行：

```text
Total: 3
Pending: 2
Completed: 1
```

空任务库输出：

```text
Total: 0
Pending: 0
Completed: 0
```

命令成功时退出码为 `0`，不得创建或修改数据文件。JSON 损坏时继续遵守第 3 节的失败
安全规则。

### 2.6 删除任务

永久删除指定任务：

```powershell
java -jar target/study-track.jar delete 1
```

任务存在时：

- 从任务集合中永久移除该任务；
- 未完成和已完成任务都可以删除；
- `nextId` 保持不变，已删除的 ID 永不复用；
- 向标准输出写入 `Deleted task 1.`；
- 退出码为 `0`。

任务不存在时：

- 向标准错误写入 `Task 99 not found.`；
- 退出码为 `2`；
- 不创建或修改数据文件。

非整数 ID 由命令行解析器报告为参数错误并返回退出码 `2`。数据文件不存在时按任务不存在
处理且不创建文件。JSON 损坏或持久化失败时返回退出码 `1`，不得覆盖或部分修改原文件。

第一版删除不进行交互确认，也不接受 `--force`。调用者通过显式提供 `delete <id>` 表达
永久删除意图。

### 2.7 重命名任务

只修改指定任务的标题。标题必须恰好使用以下一种来源。

内联标题：

```powershell
java -jar target/study-track.jar rename 1 "深入学习 Harness Engineering"
```

UTF-8 标题文件：

```powershell
java -jar target/study-track.jar rename 1 --title-file title.txt
```

`--title-file` 用于避免完整 Unicode 标题经过 Shell 和操作系统原生命令行编码。文件路径
仍作为普通命令行参数传递；跨平台验收使用 ASCII 路径，不承诺当前原生命令行无法表示的
路径字符。

参数按以下顺序处理：

1. Picocli 解析任务 ID 和标题来源；非整数 ID、没有标题来源或同时提供内联标题与
   `--title-file` 时，由 Picocli 报告为参数错误并返回退出码 `2`；
2. 使用 `--title-file` 时，在 JVM 内严格按 UTF-8 读取完整文件；允许并忽略文件开头的
   一个 UTF-8 BOM；
3. 新标题先使用 Java `String.strip()` 去除首尾空白，再校验为 `1..200` 个 Unicode
   码点；
4. 标题有效后才读取任务数据。

参数解析失败时不得读取标题文件或任务数据，也不得创建或修改数据文件。

标题文件不存在、是目录、不可读或包含非法 UTF-8 时：

- 向标准错误写入
  `Title file error: Unable to read UTF-8 title file: <path>`；
- 退出码为 `1`；
- 不读取、创建或修改任务数据文件。

标题无效时：

- 向标准错误写入 `Task title must contain between 1 and 200 characters.`；
- 退出码为 `2`；
- 不读取、创建或修改数据文件。

任务存在且归一化后的新标题与原标题不同时：

- 未完成和已完成任务都可以重命名；
- 只修改指定任务的 `title`；
- 任务的 `id` 和 `completed` 保持不变；
- `nextId`、其他任务的内容和相对顺序保持不变；
- 向标准输出写入 `Renamed task 1.`；
- 退出码为 `0`。

归一化后的新标题与原标题相同时：

- 不写入数据文件；
- 向标准输出写入 `Task 1 already has that title.`；
- 退出码为 `0`。

任务不存在时：

- 向标准错误写入 `Task 99 not found.`；
- 退出码为 `2`；
- 不创建或修改数据文件。

数据文件不存在时按任务不存在处理且不创建文件。JSON 损坏或持久化失败时返回退出码
`1`，不得覆盖或部分修改原文件。标题文件读取和标题校验均先于任务读取：标题文件失败
时先报告标题文件错误；标题无效且任务不存在时先报告标题错误；两种情况都不读取任务
数据。

第一版重命名不修改任务 ID 或完成状态，不提供通用字段编辑、批量重命名、交互式编辑器、
标题历史或撤销功能。

### 2.8 重新打开任务

把一个已完成任务重新设为未完成：

```powershell
java -jar target/study-track.jar reopen 1
```

任务已完成时：

- 只把指定任务的 `completed` 从 `true` 改为 `false` 并持久化；
- 任务的 `id` 和 `title` 保持不变，`nextId`、其他任务的内容和相对顺序保持不变；
- 向标准输出写入 `Reopened task 1.`，标准错误不输出内容；
- 退出码为 `0`。

任务已经是未完成状态时：

- 不写入数据文件；
- 向标准输出写入 `Task 1 is already pending.`，标准错误不输出内容；
- 退出码为 `0`。

任务不存在时：

- 向标准错误写入 `Task 99 not found.`，标准输出不输出内容；
- 退出码为 `2`；
- 不创建或修改数据文件。

非整数 ID 由 Picocli 报告为参数错误并返回退出码 `2`，不得访问、创建或修改数据文件。
数据文件不存在时按任务不存在处理且不创建文件。

JSON 损坏或持久化失败时：

- 向标准错误写入现有数据错误格式 `Data file error: <message>`，标准输出不输出内容；
- 退出码为 `1`；
- 不得覆盖或部分修改原文件。

第一版重新打开不切换未完成任务的状态，不恢复已删除任务，也不提供通用状态编辑、批量
重新打开、状态历史或撤销功能。

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
- `nextId` 只在成功添加任务后递增；完成、重新打开、删除或重命名任务不会改变它，已有
  ID 永不复用；
- 写入必须采用临时文件加替换的方式，避免留下半写入的 JSON；
- JSON 无法解析时，向标准错误报告数据文件错误，返回退出码 `1`，不得覆盖原文件；
- 自动测试必须使用临时目录，不得读写用户的真实数据文件。

## 4. 本版本不包含

- 用户账号和多人协作；
- 网络同步、HTTP 服务和数据库；
- 图形界面；
- 截止日期、提醒、标签和优先级；
- 除第 2.7 节明确授权的单任务标题重命名外，其他通用或批量编辑能力；
- 软删除、回收站、恢复已删除任务和批量删除；
- 导入、导出及数据迁移。
- 忽略大小写、正则表达式或模糊标题搜索、从文件读取搜索文本及搜索结果高亮。

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
| AC-12 | `show` 精确返回指定任务；成功、不存在和无数据文件路径均不修改数据。 |
| AC-13 | `summary` 正确统计总数、未完成数和已完成数；空库和成功路径均保持只读。 |
| AC-14 | `delete` 永久删除指定任务并保持 `nextId` 不变；成功、不存在、无数据文件、损坏 JSON 和持久化失败路径均符合退出码及失败安全约定。 |
| AC-15 | `rename` 在读取数据前按 `add` 规则校验标题；成功时只修改标题并保留 `id`、`completed`、`nextId` 和其他任务，标题相同则不写入；无效标题、不存在、非整数 ID、无数据文件、损坏 JSON 和持久化失败路径均符合输出、退出码及失败安全约定。 |
| AC-16 | `rename --title-file` 在 JVM 内严格读取 UTF-8 标题并与内联标题互斥；参数、读取、解码或标题校验失败时不得访问任务数据，200 个补充平面 Unicode 码点成功且 201 个失败不修改数据。 |
| AC-17 | `reopen` 只把指定已完成任务持久化为未完成且保留其他数据；已未完成时幂等成功且不写入；不存在、非整数 ID、无数据文件、损坏 JSON 和持久化失败路径均符合输出、退出码、无副作用及失败安全约定。 |
| AC-18 | `list --contains <text>` 先 `strip()` 并校验查询为 1..200 个 Unicode 码点，再对已保存标题执行区分大小写、非正则、非模糊的字面子串匹配；无效查询精确报错、退出 `2` 且不访问数据，与 `--status` 按 AND 组合；成功结果保持 ID 升序、既有单行格式、空结果、只读及退出 `0` 语义，无数据文件视为空库，损坏 JSON 沿用数据错误与退出 `1`。 |

统一验收入口：

```powershell
.\mvnw.cmd verify
```
