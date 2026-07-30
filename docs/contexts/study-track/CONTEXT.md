# StudyTrack

StudyTrack 上下文描述个人记录和跟踪学习事项时使用的产品语言。这里只定义术语，不定义
产品行为、验收规则或实现方式。

## Language

**学习任务（learning task）**：
个人希望记录和跟踪的一项学习事项。
_Avoid_：Codex task、未限定的“任务”

**任务 ID（task ID）**：
用于持续指称同一项学习任务的稳定身份。
_Avoid_：Codex task ID、记录序号

**标题（title）**：
命名一项学习任务的文本。
_Avoid_：描述、任务名

**状态（status）**：
学习任务所处的进度分类，其词汇为“未完成”和“已完成”。
_Avoid_：阶段、结果

**任务库（task store）**：
StudyTrack 所管理的学习任务及其身份分配信息的概念集合。
_Avoid_：Codex task 列表、JSON 文件、Repository
