# 课程表 App 项目开发文档（面向 AI 编码助手）

> 文档版本：v1.0
> 目标平台：Android（原生 Kotlin）
> 技术栈：Kotlin + Jetpack Compose + Material3 + Room + Glance
> 阅读对象：执行编码任务的 AI 助手 / 开发者

---

## 0. 如何使用本文档

本文档是**唯一事实来源（Single Source of Truth）**。执行编码任务时必须遵守：

1. **先读第 4、5 章**（数据模型与导入格式），再动手写任何代码。这两个章节定义了全部契约。
2. **任务按第 10 章的里程碑顺序推进**，不要跳序。每个里程碑末尾有硬性验收标准，未通过不得进入下一个。
3. **不要一次性重写整个模块**。每个任务限定在单个文件或单个函数级别，产出 diff 化的改动。
4. **遇到本文档未覆盖的决策**，不要自行发明。停下来向用户提问，并把结论回写进本文档的对应章节。
5. **禁止引入未在第 7.1 节列出的第三方库**。

---

## 1. 项目概述

### 1.1 定位

一款**离线优先、本地存储**的 Android 大学生课程表应用。核心价值是：**把教务系统里那张难用的课表图片，用 30 秒变成一个可查询、可放桌面小组件的课表。**

明确不做：账号体系、云同步、社交功能、教务系统自动抓取（登录教务系统涉及模拟登录与合规风险，一律不做）。

### 1.2 三条主流程

| 编号 | 流程 | 步骤 |
|---|---|---|
| **P1（最重要）** | 导入建课表 | 用户截图教务课表 → App 内一键复制提示词 → 发给任意 AI → AI 输出 JSON → 复制 AI 回复 → 回 App 粘贴（或选文件 / 从 AI App 分享进来）→ 自动新建课表 → 预览纠偏页 → 落库 → 课表主页可见 |
| **P2** | 手动建课表 | 新建学期 → 设置学期起始日与周数 → 配置节次时间表 → 逐门添加课程（可设多个时间段） → 保存 |
| **P3** | 桌面看课表 | 桌面添加小组件 → 自动读取当前学期数据 → 显示今日课程 / 本周网格 → 点击跳转 App |

### 1.3 范围界定

| 优先级 | 功能 | 说明 |
|---|---|---|
| **P0** | FR-01 导入建课表 | 本项目第一优先级功能 |
| **P0** | FR-02 多课表（学期）管理 | 新建/切换/重命名/归档/删除 |
| **P0** | FR-03 课程 CRUD + 多时间段 | 名称、教师、地点、周次、周几、节次 |
| **P0** | FR-04 周视图课表展示 | 网格布局、跨节次课程、当前节次高亮 |
| **P0** | FR-05 桌面小组件（Glance） | 小/中/大三种尺寸 |
| **P0** | FR-06 导出备份 | 导出为同规格 JSON，可再导入 |
| **P0** | FR-07 当前周计算 | 依赖：导入后必须知道"今天是第几周"，否则无法判断今天有没有课 |
| **P0** | FR-08 节次时间表可配置 | 不同学校节次时间不同，禁止硬编码 |
| **P1** | FR-09 上课前提醒通知 | 体验加分项 |
| **P1** | FR-10 时间/教室冲突检测 | 保存时提示 |
| **P1** | FR-11 单双周与自定义周次 | 数据模型 P0 已支持，UI 精细化放 P1 |
| **P2** | FR-12 节假日调休 | 手动指定某周按某周的课表走 |
| **P2** | FR-13 课程颜色、笔记、附件 | — |
| **P2** | FR-14 考试倒计时 / 作业待办 | — |

---

## 2. 术语与领域概念

| 术语 | 英文/代码标识 | 定义 |
|---|---|---|
| 课表 / 学期 | `Term` | 一个学期的完整课表容器，含起始日期、总周数、节次时间表 |
| 课程 | `Course` | 一门课，如"高等数学A"，含名称、教师、颜色 |
| 上课时间段 | `CourseSession` | 课程的**一次**上课安排：周几 + 第几节到第几节 + 周次 + 地点 |
| 周次表达式 | `weekSpec` | 描述"第几周上课"的字符串，如 `"1-16"`、`"1-9,11-18"`、`"2-16双"` |
| 节次 | `Section` | 第 N 节课（1 开始），具体起止时间由节次时间表定义 |
| 节次时间表 | `SectionTemplate` | 学期级配置：`第1节 08:00-08:45`、`第2节 08:55-09:40` … |
| 当前周 | `currentWeek` | 由学期起始周一推算出的今天是第几教学周 |

**关键建模决策：Course 与 CourseSession 是一对多。** 一门课可以有多个时间段（例如"高等数学A"周一 1-2 节、周三 3-4 节）。这是需求 2 中"同一门课程可以设置多个时间段"的直接映射。

---

## 3. 功能需求详述

### FR-01 导入建课表（P0，最高优先级）

**输入**：用户选择本地 `.json` 文件（或通过分享/粘贴文本导入）。

**处理**：走第 5.6 节的五级容错流水线。

**输出**：一个新的 `Term`，直接写入数据库，并自动切换为当前课表。

**UI 流程**：
1. 导入页两个 Tab：**「AI 识别建课表」为默认页（粘贴入口）**，「从文件导入」为兜底。
2. 粘贴后自动触发解析；也提供「从剪贴板粘贴」「清空」「开始解析」三个按钮。
3. **预览纠偏页（关键）**：
   - 顶部：识别摘要"共 24 个时间段 → 归并为 14 门课程"。
   - 中部：课程卡片列表，每门课展示其所有时间段，支持展开编辑。
   - 提供操作：**拆分为新课**、**合并到其它课**、**删除**、**就地编辑任意字段**。
   - 底部：学期信息卡（名称/起始日/总周数），可修改。
4. 确认后落库，跳转课表主页。

**失败处理**：解析失败必须给出**可定位的错误**，例如：`第 3 条记录：dayOfWeek 值为 9，应为 1-7`。禁止只弹"解析失败"。失败页必须提供让 AI 重生成的出口，见 FR-01B。

### FR-01A 提示词分发与使用引导（P0）

**目标**：用户在 App 内一键拿到提示词，并清楚知道每一步该做什么。核心矛盾是**第 2、3 步发生在 App 外**，用户最容易在这里流失，所以引导必须具体到"截图要截什么"这个级别。

**入口**：导入页 Tab「AI 识别建课表」；首次启动的空状态卡片直达此处。

**页面结构（自上而下）**

| 区块 | 内容 |
|---|---|
| 分步引导 | 4 步：截图 → 复制提示词发给 AI → 复制 AI 回复 → 回来粘贴。纯展示，不强制打勾 |
| 截图指引 | 3 条：要能看到星期表头和节次列 / 太长就分两次截 / 避免模糊反光 |
| 复制区 | 主按钮「复制提示词」+ 折叠区「查看完整提示词（可手动复制）」 |
| 粘贴区 | 多行输入框 + 「从剪贴板粘贴」/「清空」/「开始解析」 |

**实现约束**

- 提示词文本存 `res/raw/prompt_import.txt`，运行时读取。**禁止硬编码在 Kotlin 字符串里**——后续要迭代提示词，改 raw 资源比改代码安全。
- 复制必须用 `ClipData.newPlainText(label, text)`，**禁止富文本**，否则粘贴进部分 AI 输入框会带样式或被截断。
- 复制成功后 Snackbar：`提示词已复制，去粘贴给 AI 吧`。
- 折叠区默认收起，展开后为只读文本 + 「复制」按钮（供特殊场景手动取用）。
- **本期不做**跳转或分享到外部 AI 的按钮。原因：各 AI App 对「图片 + 文字」的 `ACTION_SEND` 处理不一致，深链包名/URL 又易失效。后续若要补，用 `ACTION_SEND` + `FileProvider` 带 `EXTRA_STREAM`（截图）与 `EXTRA_TEXT`（提示词）。

**回传路径（三条，优先级递减）**

| # | 路径 | 实现 | 说明 |
|---|---|---|---|
| 1 | 粘贴文本（默认） | 多行输入框 + 剪贴板读取 | 主路径。手机上把文本存成 `.json` 对用户几乎不可行，所以粘贴必须是默认页 |
| 2 | 文件导入（兜底） | `Intent.ACTION_OPEN_DOCUMENT`，mime `application/json` 与 `*/*` | 面向桌面端 AI 生成的 `.json`。用 SAF，**无需申请存储权限** |
| 3 | 分享到本 App（加分项） | Manifest 注册 `ACTION_SEND` / `text/plain` 的 Activity | 用户在 AI App 里点分享直达导入页 |

分享入口 Activity 需 `android:exported="true"`，并做防御：空 `EXTRA_TEXT` 直接返回主页；文本超过 1 MB 拒绝并提示。

**文案来源**：全部取自《recognition-prompt.md》第一节，该文件是文案的唯一来源。

### FR-01B 导入失败闭环（P0）

导入失败不能是死胡同。失败页提供一键出口，让用户把错误报告贴回 AI 重生成。

**错误报告 = 修复版提示词模板 + 错误列表 + 用户原始文本**

- 模板存 `res/raw/prompt_repair.txt`，含占位符 `{ERRORS}` 与 `{ORIGINAL}`，运行时替换。
- 模板全文见《recognition-prompt.md》第三节。

**UI**：失败页三个按钮 —— `复制错误报告，让 AI 重新生成` / `手动修改` / `换一张截图重来`。复制后 Snackbar：`已复制，粘贴给 AI 让它重新生成`。

**约束**：原始文本超过 8 KB 时截断并追加 `...（内容过长已截断）`，避免剪贴板过大被系统拒绝。

### FR-02 多课表管理（P0）

- 顶部下拉可切换当前学期。
- 支持新建空课表、复制已有课表、重命名、归档、删除（二次确认）。
- 每学期独立配置：名称、起始周一、总周数（默认 18）、节次时间表。

### FR-03 课程 CRUD 与多时间段（P0）

课程字段：

| 字段 | 必填 | 说明 |
|---|---|---|
| 名称 | 是 | 如"高等数学A" |
| 教师 | 否 | — |
| 颜色 | 否 | 未指定时按名称哈希自动分配 |
| 备注 | 否 | — |

时间段字段：

| 字段 | 必填 | 说明 |
|---|---|---|
| 星期几 | 是 | 1=周一 … 7=周日 |
| 起始节次 | 是 | 1 开始 |
| 结束节次 | 是 | 含端点，必须 ≥ 起始节次 |
| 周次表达式 | 是 | 见 4.3 |
| 上课地点 | 否 | — |
| 备注 | 否 | — |

交互：课程详情页内用"+ 添加时间段"按钮增删时间段列表。删除课程时二次确认。

### FR-04 周视图展示（P0）

- 横向 7 列（或 5 列，由设置控制是否显示周末），纵向为节次行。
- 左侧固定节次列（显示节次号 + 起止时间）。
- 顶部日期栏显示日期，当天列高亮。
- **当前时刻高亮**：当前正在上的课加边框高亮；已结束的课程降透明度。
- 支持左右滑动切换周次，顶部显示"第 N 周"。
- 点击课程块进入课程详情。
- 跨多节的课程绘制为一个连续长块。

### FR-05 桌面小组件（P0）

三种尺寸，用 Glance 实现：

| 尺寸 | 目标 dp | 内容 |
|---|---|---|
| 小 2×2 | ~110×110 | 下一节课：课程名 + 时间 + 地点；无课则显示"今天没有更多课" |
| 中 4×2 | ~250×110 | 今日剩余课程列表（最多 3 条） |
| 大 4×4 / 5×4 | ~250×250 | 本周网格缩略图 + 今日列高亮 |

- 点击任意区域 → `actionStartActivity` 打开 App 主页。
- 数据为空时显示"点击导入课表"引导。
- 必须跟随系统深色模式（`ColorProvider`）。

### FR-06 导出备份（P0）

导出为与导入格式**完全一致**的 JSON（第 5 章 schema），保证导出文件可重新导入。通过系统分享面板输出。

### FR-07 当前周计算（P0）

见 8.2 节算法。

### FR-08 节次时间表（P0）

- 每个学期独立，默认提供一套常见时间表（12 节：上午 4、下午 4、晚上 4）。
- 支持增删改节次、修改起止时间。
- 导入文件时若 JSON 携带 `sectionTemplates`，覆盖默认值。

### FR-09 课程块快捷编辑（P0）

在周视图上**直接点击课程块**，弹出底部抽屉编辑，免去「先进管理页再找课」的路径。抽屉分两段：

1. **课程信息**（名称 / 教师 / 颜色）—— 修改后影响这门课的**所有**时间段
2. **本节课**（周几 / 起始节 / 结束节 / 周次 / 地点）+ 删除本节 —— 只影响当前这一节

约束：
- 删除的是**时间段**，不是整门课，需二次确认；课程本身保留
- 校验不通过（名称为空、周次无法解析）时只提示、**不改数据**
- 起止节次写反了自动纠正，比报错更省事
- **操作区固定在抽屉右上角，用图标而非文字**：保存（软盘）/ 删除（红色垃圾桶）/ 取消（叉）。
  图标为**自绘 VectorDrawable**，不引入 `material-icons-extended`；每个图标必须有 `contentDescription`

> **布局教训**：抽屉操作区**不要**用 `Row` + `Text(Modifier.weight(1f))` + `IconButton` 做「左标题右按钮」——
> 实测 `weight` 不会把剩余宽度让出来，`Row` 被压成标题文字宽度，`IconButton` 变 0 宽，
> UI 树里能查到节点却完全不可见。改用 `Box` + `align(CenterStart)` / `align(CenterEnd)` 嵌套 `Row`。

### FR-10 自定义课块颜色（P0）

用户可以给课程指定颜色，不指定则按课名哈希自动取色。

约束：
- **颜色粒度是课程，不是时间段** —— 同一门课的所有时间段共用一个颜色
- 提供 12 个预设色 + **「自动」**（按课名哈希，可随时改回自动）
- 自定义色必须同步到桌面小组件，两处颜色**不允许出现色差**
- 自定义色不一定是深色，文字颜色需按背景**感知亮度**自动切换黑 / 白，保证可读

> **实现约束（务必遵守）**：色板定义在 `core:common`（纯 ARGB 整数、不依赖 Compose），Compose 层只做 `Color(argb)` 包装；`ScheduleBlock` 直接携带解析后的色值。这样周视图与小组件天然拿到同一色值。
>
> **历史教训**：自动取色的模数**不能**跟着 `colors.size` 走，否则往色板里加一个颜色会让所有已有课表的自动色整体错位。用固定的 `AUTO_COLOR_COUNT` 常量（当前为 10）。

### FR-11 统一的滚轮与周次点选输入（P0）

「周几 / 起始节 / 结束节」与「周次」这两类字段，在**所有**编辑入口（周视图抽屉、课程编辑页）
必须用同一种输入方式，避免「这里要点选、那里要手填」的困惑。

- **滚轮选择器**（`core:designsystem/picker/WheelPicker`）：`LazyColumn` + `rememberSnapFlingBehavior`
  实现惯性吸附，可见 3 项、中间高亮；`LabeledWheel` 用于表单内复用。**不引入第三方滚轮库**
- **周次点选器**（`WeekPicker`）：`1..totalWeeks` 网格点选，附「整学期 / 单周 / 双周 / 清空」快捷
- 承载 `WeekPicker` 的两个 feature 模块都依赖 `core:designsystem`，组件放这里即可共用

**存储格式不变（重要）**：`CourseSession.weekSpec` 仍是**字符串**，它是导入格式 v1.0 的契约字段。
点选只是**输入方式**的变化，落库前由 `WeekSpecFormatter.format(weeks, totalWeeks)` 转回表达式，
读出时由 `WeekSpecParser.parse()` 反解。因此**不需要改数据契约、不需要数据库迁移**。

> **实现约束**：滚轮「滚动结束后回写选中值」的判定必须走 `core:common/WheelScrollGuard`，
> **不能**直接判断 `!isScrollInProgress` —— 它初始即 false，首次组合会被误当成一次「滚动结束」，
> 从而把外部传入的选中值重置成第一项。详见 `docs/CHANGELOG.md` 的 #17。

---

## 4. 数据模型

### 4.1 领域模型（Kotlin）

```kotlin
// core/model/Term.kt
data class Term(
    val id: Long = 0,
    val name: String,               // "2026-2027 秋季学期"
    val startMonday: LocalDate,     // 学期第一周的周一，必须是周一
    val totalWeeks: Int,            // 默认 18
    val isArchived: Boolean = false,
    val createdAt: Instant,
)

// core/model/SectionTemplate.kt
data class SectionTemplate(
    val termId: Long,
    val index: Int,                 // 第几节，1 开始
    val startMinutes: Int,          // 自 00:00 起的分钟数，如 8*60=480
    val endMinutes: Int,
)

// core/model/Course.kt
data class Course(
    val id: Long = 0,
    val termId: Long,
    val name: String,
    val teacher: String? = null,
    val colorArgb: Int? = null,     // null 时按 name 哈希生成
    val note: String? = null,
)

// core/model/CourseSession.kt
data class CourseSession(
    val id: Long = 0,
    val courseId: Long,
    val dayOfWeek: Int,             // 1..7
    val startSection: Int,          // 1..N，含
    val endSection: Int,            // 含，>= startSection
    val weekSpec: String,           // 原始表达式，如 "1-16"
    val location: String? = null,
    val note: String? = null,
)

// 派生：weekSpec 解析后的周集合，不落库，读取时计算
data class CourseSessionResolved(
    val session: CourseSession,
    val weeks: Set<Int>,
)
```

### 4.2 Room 表结构

```kotlin
@Entity(tableName = "terms", indices = [Index("name")])
data class TermEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startMondayEpochDay: Long,   // LocalDate.toEpochDay()
    val totalWeeks: Int,
    val isArchived: Boolean,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "section_templates",
    primaryKeys = ["termId", "index"],
    foreignKeys = [ForeignKey(
        entity = TermEntity::class,
        parentColumns = ["id"],
        childColumns = ["termId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class SectionTemplateEntity(
    val termId: Long,
    val index: Int,
    val startMinutes: Int,
    val endMinutes: Int,
)

@Entity(
    tableName = "courses",
    indices = [Index("termId")],
    foreignKeys = [ForeignKey(
        entity = TermEntity::class,
        parentColumns = ["id"],
        childColumns = ["termId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val termId: Long,
    val name: String,
    val teacher: String?,
    val colorArgb: Int?,
    val note: String?,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "course_sessions",
    indices = [Index("courseId"), Index("dayOfWeek")],
    foreignKeys = [ForeignKey(
        entity = CourseEntity::class,
        parentColumns = ["id"],
        childColumns = ["courseId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class CourseSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
    val weekSpec: String,
    val location: String?,
    val note: String?,
)
```

**数据库初始化必须调用** `.enableMultiInstanceInvalidation()`（小组件更新与主 App 写库可能并发）。

DataStore（`settings.preferences_pb`）存储：`currentTermId: Long`、`showWeekend: Boolean`、`showNonCurrentWeekCourses: Boolean`、`reminderMinutesBefore: Int`、`themeMode: String`。

### 4.3 周次表达式规范

**语法（EBNF）**

```
weekSpec   := segment ("," segment)*
segment    := range | single
range      := INT "-" INT [pattern]
single     := INT [pattern]
pattern    := "单" | "双" | "odd" | "even"
```

**语义**

| 表达式 | 含义 |
|---|---|
| `1-16` | 第 1 至 16 周，全部 |
| `1-16单` | 第 1 至 16 周中的奇数周 |
| `2-16双` | 第 2 至 16 周中的偶数周 |
| `1-9,11-18` | 1-9 周 + 11-18 周（第 10 周停课） |
| `1,3,5,7` | 离散周 |
| `1-8,10-16双` | 混合（每段独立应用单双规则） |

**解析算法**

```
fun parseWeekSpec(spec: String, totalWeeks: Int): Set<Int>
  1. 去除所有空白字符
  2. 按 "," 切分为 segments
  3. 对每个 segment：
     a. 正则匹配 ^(\d+)(?:-(\d+))?(单|双|odd|even)?$
     b. 不匹配 → 抛 WeekSpecParseException(segment)
     c. start > end → 抛异常
     d. end > totalWeeks → 钳制到 totalWeeks（不报错，仅 warn）
     e. 遍历 start..end，按 pattern 过滤奇偶
  4. 合并所有 segment 的周，去重，排序
  5. 空结果 → 抛异常
```

**必须有单元测试覆盖**：上述表格每一个表达式 + 非法输入（`"abc"`、`"5-2"`、`""`、`"1-"`、`"16-1双"`）。

### 4.4 节次时间表默认值

```kotlin
val DEFAULT_SECTIONS = listOf(
    1 to ("08:00" to "08:45"),
    2 to ("08:55" to "09:40"),
    3 to ("10:00" to "10:45"),
    4 to ("10:55" to "11:40"),
    5 to ("14:00" to "14:45"),
    6 to ("14:55" to "15:40"),
    7 to ("16:00" to "16:45"),
    8 to ("16:55" to "17:40"),
    9 to ("19:00" to "19:45"),
    10 to ("19:55" to "20:40"),
    11 to ("20:50" to "21:35"),
    12 to ("21:45" to "22:30"),
)
```

---

## 5. 导入文件格式规范 v1.0

### 5.1 三条设计原则

1. **扁平化优先**：顶层是 `sessions` 数组，**一行 = 一个时间段**。不要求 AI 做课程归并（归并是 App 的职责，且 App 归并错了用户能在预览页纠正，AI 归并错了无法察觉）。
2. **字符串化数值敏感项**：周次用表达式字符串、时间用 `"HH:mm"`，避免 AI 输出时间戳或错误数值类型。
3. **可往返（round-trip）**：App 导出的文件必须能被 App 原样导入且结果一致。

### 5.2 字段表

**顶层**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `format` | string | 是 | 固定 `"campus-timetable"` |
| `version` | string | 是 | 固定 `"1.0"` |
| `term` | object | 是 | 学期信息 |
| `sectionTemplates` | array | 否 | 节次时间表，缺省用默认值 |
| `sessions` | array | 是 | 时间段列表，可为空数组 |

**term**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | string | 是 | 学期名，如 `"2026-2027 秋季学期"` |
| `startMonday` | string | 是 | `"YYYY-MM-DD"`，必须是周一 |
| `totalWeeks` | int | 是 | 1..30 |

**sectionTemplates[i]**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `index` | int | 是 | 第几节，1 开始 |
| `start` | string | 是 | `"HH:mm"` |
| `end` | string | 是 | `"HH:mm"`，必须晚于 start |

**sessions[i]**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | string | 是 | 课程名 |
| `teacher` | string\|null | 否 | 未知填 `null` |
| `location` | string\|null | 否 | 未知填 `null` |
| `dayOfWeek` | int | 是 | 1=周一 … 7=周日 |
| `startSection` | int | 是 | ≥1 |
| `endSection` | int | 是 | ≥ startSection |
| `weeks` | string | 是 | 周次表达式，见 4.3 |
| `note` | string\|null | 否 | AI 的不确定说明会放这里 |

### 5.3 JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "CampusTimetable",
  "type": "object",
  "required": ["format", "version", "term", "sessions"],
  "properties": {
    "format": { "const": "campus-timetable" },
    "version": { "const": "1.0" },
    "term": {
      "type": "object",
      "required": ["name", "startMonday", "totalWeeks"],
      "properties": {
        "name": { "type": "string", "minLength": 1 },
        "startMonday": { "type": "string", "pattern": "^\\d{4}-\\d{2}-\\d{2}$" },
        "totalWeeks": { "type": "integer", "minimum": 1, "maximum": 30 }
      }
    },
    "sectionTemplates": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["index", "start", "end"],
        "properties": {
          "index": { "type": "integer", "minimum": 1 },
          "start": { "type": "string", "pattern": "^([01]\\d|2[0-3]):[0-5]\\d$" },
          "end":   { "type": "string", "pattern": "^([01]\\d|2[0-3]):[0-5]\\d$" }
        }
      }
    },
    "sessions": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["name", "dayOfWeek", "startSection", "endSection", "weeks"],
        "properties": {
          "name": { "type": "string", "minLength": 1 },
          "teacher": { "type": ["string", "null"] },
          "location": { "type": ["string", "null"] },
          "dayOfWeek": { "type": "integer", "minimum": 1, "maximum": 7 },
          "startSection": { "type": "integer", "minimum": 1 },
          "endSection": { "type": "integer", "minimum": 1 },
          "weeks": { "type": "string", "minLength": 1 },
          "note": { "type": ["string", "null"] }
        }
      }
    }
  }
}
```

### 5.4 示例

见同目录 `timetable-sample.json`。核心片段：

```json
{
  "format": "campus-timetable",
  "version": "1.0",
  "term": { "name": "2026-2027 秋季学期", "startMonday": "2026-09-07", "totalWeeks": 18 },
  "sessions": [
    { "name": "高等数学A", "teacher": "张伟", "location": "教三-301",
      "dayOfWeek": 1, "startSection": 1, "endSection": 2, "weeks": "1-16" },
    { "name": "高等数学A", "teacher": "张伟", "location": "教三-301",
      "dayOfWeek": 3, "startSection": 3, "endSection": 4, "weeks": "1-16" },
    { "name": "大学英语", "teacher": "李娜", "location": "外语楼-202",
      "dayOfWeek": 2, "startSection": 3, "endSection": 4, "weeks": "1-16单" }
  ]
}
```

### 5.5 校验规则与错误码

| 错误码 | 触发条件 | 用户提示 |
|---|---|---|
| `E_FORMAT` | `format` 不等于 `campus-timetable` | 这不是课表文件 |
| `E_VERSION` | `version` 不是 `1.0` | 文件版本不受支持 |
| `E_NO_JSON` | 文本中找不到合法 JSON 对象 | 未找到课表数据 |
| `E_TERM_DATE` | `startMonday` 格式错误或不是周一 | 学期起始日必须是周一 |
| `E_DOW` | `dayOfWeek` 不在 1..7 | 第 N 条：星期值应为 1-7 |
| `E_SECTION_RANGE` | `endSection < startSection` | 第 N 条：结束节次小于开始节次 |
| `E_SECTION_OOB` | 节次超出节次时间表范围 | 第 N 条：第 X 节未在节次时间表中定义 |
| `E_WEEKSPEC` | 周次表达式无法解析 | 第 N 条：周次"xxx"无法识别 |
| `E_TIME` | 节次起止时间格式错误或倒序 | 第 X 节时间设置有误 |

**错误必须携带记录序号**，让用户能在预览页定位并修复。

### 5.6 五级容错流水线（核心实现）

```
L0 提示词契约      （在 App 外，由提示词模板保证；见 recognition-prompt.md）
L1 结构化提取      App 侧：剥离 ```json 围栏、前后正文，取第一个完整 JSON 对象
L2 Schema 校验+归一化  App 侧：字段别名映射、类型强转、周次解析、越界钳制
L3 安全自动修复    App 侧：补全缺失的可推断字段
L4 人工纠偏        UI：预览页，支持拆分/合并/编辑后落库
```

**L1 结构化提取实现要点**

```
1. 若存在 ``` 围栏：取围栏内内容（优先 ```json，其次任意围栏）
2. 否则：从文本中定位第一个 '{' 与最后一个 '}'，取子串
3. 括号计数扫描，截取第一个配平的 JSON 对象（防止尾部多余内容）
4. 用 kotlinx.serialization 以 lenient 模式解析为 JsonElement，再手工映射
   ——不要用 @Serializable data class 直接反序列化，否则字段漂移会整体失败
5. 尝试 JSON5 风格修复：去除尾随逗号、把单引号换双引号（仅做这两项，不做更多）
```

**L2 字段别名映射表**（AI 常见的同义字段名，全部映射到标准字段）

| 标准字段 | 可接受的别名 |
|---|---|
| `name` | `courseName`, `course`, `课程名`, `课程`, `className`, `title` |
| `teacher` | `instructor`, `lecturer`, `教师`, `老师` |
| `location` | `classroom`, `room`, `place`, `地点`, `教室` |
| `dayOfWeek` | `day`, `weekday`, `dow`, `星期`, `周几` |
| `startSection` | `startPeriod`, `from`, `开始节次` |
| `endSection` | `endPeriod`, `to`, `结束节次` |
| `weeks` | `weekSpec`, `weekList`, `周次`, `上课周` |

**dayOfWeek 归一化**

| 输入 | 归一化为 |
|---|---|
| `1` / `"1"` / `"周一"` / `"星期一"` / `"Mon"` / `"Monday"` | 1 |
| `7` / `"周日"` / `"星期天"` / `"Sun"` / `"Sunday"` | 7 |
| `0`（部分模型用 0 表示周日） | 7 |

**节次归一化**：`"1-2"` / `"第1-2节"` / `"1,2"` / `"1"`（单节时 end=start）都能解析。`"1-2节"` 需剥离"节"字。

**L3 可安全自动修复项**

- `teacher` / `location` 为 `""`、`"-"`、`"未知"`、`"无"` → 归一为 `null`
- 缺失 `sectionTemplates` → 填入 4.4 默认值
- 缺失课程颜色 → 按 `name.hashCode()` 取调色板颜色
- `endSection` 缺失 → 等于 `startSection`
- `weeks` 缺失 → `"1-{totalWeeks}"`
- `term.name` 缺失 → 按 `startMonday` 推导（如 `2026-09` → `"2026-2027 秋季学期"`）

**L4 预览页归并规则**

按 `name.trim()` + `teacher?.trim()` 归一化后作为归并键（大小写不敏感、去除空格）。归并后展示课程卡片，卡片下列出所有时间段。用户可手动拆分或跨课合并。

---

## 6. 技术选型与工程规范

### 6.1 技术栈与版本

| 类别 | 选型 | 版本建议 |
|---|---|---|
| 语言 | Kotlin | 2.0+ |
| AGP / Gradle | Android Gradle Plugin | 8.5+ / 8.7 |
| minSdk | 26（Android 8.0） | — |
| targetSdk | 35+ | — |
| UI | Jetpack Compose + Material3 | BOM 2024.12+ |
| 导航 | Navigation Compose | 2.8+ |
| 数据库 | Room | 2.6+ |
| JSON | kotlinx.serialization | 1.7+ |
| 偏好存储 | DataStore Preferences | 1.1+ |
| DI | Hilt | 2.52+ |
| 异步 | Kotlin Coroutines + Flow | — |
| 小组件 | **Glance**（`androidx.glance:glance-appwidget`） | 1.1+ |
| 后台调度 | WorkManager | 2.9+ |
| 测试 | JUnit4 + Truth + Turbine | — |

### 6.2 模块与目录结构

```
app/                                # Application + MainActivity + 导航宿主
  src/main/res/raw/
    prompt_import.txt               # 导入提示词，对应《recognition-prompt.md》第二节
    prompt_repair.txt               # 修复版提示词，对应第三节，含 {ERRORS} / {ORIGINAL}
  ShareImportActivity.kt            # ACTION_SEND text/plain 分享入口
core/
  model/                            # 领域模型（纯 Kotlin，无 Android 依赖）
  common/                           # WeekSpecParser, DateTimeUtils, Result 封装
  database/                         # Room entities / dao / DatabaseModule
  datastore/                        # SettingsRepository
  designsystem/                     # Theme, Color, Typography, 课程调色板
  importer/                         # ★ 导入解析：Extractor / Normalizer / Validator / Importer
  exporter/                         # 导出
feature/
  schedule/                         # 周视图主页
  course/                           # 课程详情 / 编辑
  importexport/                     # 导入页 + 预览纠偏页 + 导出
  settings/                         # 学期管理 / 节次时间表 / 通用设置
widget/
  glance/                           # GlanceAppWidget 实现 + 更新调度
```

### 6.3 状态管理

- 统一使用 `StateFlow` + `UiState` 密封类的单向数据流（MVI 风格）。
- ViewModel 通过 `viewModelScope` 启动，UI 用 `collectAsStateWithLifecycle()`。
- 禁止在 Composable 中直接访问数据库，一律经 Repository。

### 6.4 命名与提交规范

- 包名全小写，无下划线：`com.xxx.timetable.feature.schedule`
- 类名 UpperCamelCase；函数名 lowerCamelCase；常量 UPPER_SNAKE_CASE
- Composable 名必须是名词：`ScheduleScreen` / `CourseCard`，不写 `DrawCourse`
- 提交信息：`feat(schedule): 支持跨节次课程合并渲染`

### 6.5 测试要求

**必须写单元测试**（无测试视为未完成）：

- `WeekSpecParserTest`：覆盖 4.3 全部用例
- `SessionExtractorTest`：带围栏 / 带正文 / 尾随逗号 / 单引号 / 无 JSON
- `NormalizerTest`：字段别名、dayOfWeek 归一化、节次归一化
- `CurrentWeekCalculatorTest`：跨学期、跨年、学期前、学期后
- `CourseMergerTest`：归并键、大小写、空格

---

## 7. 关键实现方案

### 7.1 课表网格布局算法

不要用手写 `Layout`，用 **`LazyColumn` + 每行一个 `Row(Modifier.weight(1f))`** 的结构即可满足需求，可维护性强得多。

跨节次课程的处理（关键）：

```
输入：某学期的全部 CourseSession，当前显示的 weekIndex
1. 过滤：只保留 weeks.contains(weekIndex) 的 session
2. 按 dayOfWeek 分桶（7 个桶）
3. 对每个桶内的 sessions 按 startSection 排序，做区间扫描：
   - 维护 occupied: IntArray(节次数)，记录每个节次被哪个 session 占用
   - 若某节次已被占用 → 判定为冲突，标记 conflictFlag，渲染时并排显示（各占 1/2 宽）
4. 连续相同 session 的节次合并为一个渲染单元：
   CourseBlock(startSection, endSection, session, columnSpan = 1 或 0.5)
5. 行渲染：对每一节次行，找出该行起始的 block，高度 = (end-start+1) * rowHeight
```

为避免"一个课程块被相邻行重复绘制"，采用策略：**在节次行内只绘制 `startSection == 当前行` 的块**，块高度跨多行用 `Modifier.height(rowHeight * span)` 实现，行容器 `height(IntrinsicSize.Min)` 会被撑开——实际推荐用**绝对定位的 `Box` 叠加层**：

```
Box {
  节次列 + 网格背景（7 列 × N 行的横线竖线）
  CourseBlockLayer（用 offset/absoluteOffset 精确定位每个课程块）
}
```
课程块坐标：`x = 日期列宽 + (dayOfWeek-1) * 列宽`，`y = 节次表头高 + (startSection-1) * 行高`。列宽与行高通过 `BoxWithConstraints` 计算。这是本方案推荐的最终实现，能同时解决跨节次、重叠、对齐三个问题。

### 7.2 当前周计算

```kotlin
fun currentWeek(term: Term, today: LocalDate = LocalDate.now()): Int {
    val todayMonday = today.with(DayOfWeek.MONDAY, 1)  // 或用 with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val diffWeeks = ChronoUnit.WEEKS.between(term.startMonday, todayMonday).toInt()
    return diffWeeks + 1   // 1-based
}
```

处理规则：
- 结果 `< 1` → 学期未开始，UI 显示"假期中"，默认展示第 1 周
- 结果 `> totalWeeks` → 学期已结束，UI 显示"学期已结束"，默认展示最后一周
- `startMonday` 若不是周一，导入时自动向前对齐到最近的周一，并在预览页提示用户
- 一律使用 `ZoneId.systemDefault()`，禁止硬编码时区

### 7.3 小组件实现（Glance）

**必须知道的 Glance 硬约束**

| 约束 | 说明与应对 |
|---|---|
| 组件受限 | 只能用 `Box/Row/Column/Text/Image/Icon/Button/Spacer/LazyColumn`。**不能用 Canvas、自定义 Layout、Compose 动画** |
| 字体 | 不支持自定义字体文件，只能用系统字体。中文显示正常，但不要做字体定制设计 |
| 颜色 | 必须用 `ColorProvider(day, night)` 或 `GlanceTheme.colors`，不能直接传 `Color` |
| 尺寸 | 必须在 `provideGlance` 中用 `LocalSize.current` 动态分支，**禁止假设固定尺寸**（各 launcher 差异极大） |
| 点击 | `actionStartActivity` / `actionRunCallback`；`ActionCallback` 在后台执行，不能做耗时操作 |
| 进程 | UI 组合发生在 App 进程，但渲染在 launcher 进程；数据库需启用 `enableMultiInstanceInvalidation()` |
| 调试 | 每次改动需重新编译安装，无法热重载；建议先在普通 Composable 里写静态预览再迁移 |

**刷新调度（关键难点）**

单一机制都不够，必须组合：

| 触发源 | 机制 | 覆盖场景 |
|---|---|---|
| 数据变更 | 写库后主动调用 `GlanceAppWidget.update()` | 增删改课程后立即刷新 |
| 周期性 | WorkManager `PeriodicWorkRequest`，间隔 15 分钟 | 当前节次高亮推进 |
| 日期变更 | `BroadcastReceiver` 监听 `Intent.ACTION_DATE_CHANGED` | 跨天更新今日课程 |
| 时间/时区变更 | `ACTION_TIME_CHANGED` / `ACTION_TIMEZONE_CHANGED` | 用户改时间/换时区 |
| 开机 | `ACTION_BOOT_COMPLETED` | 重启后小组件恢复 |
| 应用升级 | `ACTION_MY_PACKAGE_REPLACED` | 升级后重新注册任务 |

> 注意：`Intent.ACTION_TIME_TICK` **无法静态注册**，不要依赖它做分钟级刷新。15 分钟粒度的当前节次高亮是权衡后的可接受方案，需在文档中向用户说明。

**尺寸分支代码骨架**

```kotlin
class TimetableGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val size = LocalSize.current
            when {
                size.width < 180.dp && size.height < 180.dp -> SmallWidget()
                size.height < 180.dp                        -> MediumWidget()
                else                                        -> LargeWidget()
            }
        }
    }
}
```

**AppWidgetProviderInfo XML** 需声明 `targetCellWidth/Height`、`minWidth/minHeight`，并启用 `android:resizeMode="horizontal|vertical"`、`widgetFeatures="reconfigurable"`。

### 7.4 导入向导交互流程

```
导入页（文件/文本双 Tab）
   ↓ 触发解析
解析中（loading）
   ↓ 成功
预览纠偏页 ── 编辑/拆分/合并/删除 ──→ 确认导入 ──→ 落库 + 设为当前学期 ──→ 课表主页
   ↓ 失败
错误页（列出可定位错误 + "复制原文给我修" 按钮 + 重新选择）
```

错误页提供的"复制原文"必须把用户原始文本带上，方便直接贴回 AI 让它重生成。

---

## 8. 难度评估

| # | 模块 | 难度 | 主要风险 |
|---|---|---|---|
| 1 | 导入解析（L1-L3） | ★★★☆ | AI 输出漂移、字段别名、周次写法多样；靠单元测试覆盖兜住 |
| 1b | 提示词分发与引导（FR-01A/01B） | ★☆☆☆ | 无技术难点，是纯 UI 与文案工作；真正的挑战在引导文案打磨和 raw 资源与文档同步 |
| 2 | 课表网格布局 | ★★★☆ | 跨节次合并、冲突课程并排、不同屏幕高度自适应 |
| 3 | 周次表达式与当前周 | ★★☆☆ | 规则明确，测试覆盖即可 |
| 4 | 课程 CRUD | ★☆☆☆ | 常规表单 |
| 5 | Room 数据层与迁移 | ★★☆☆ | 外键级联、学期删除 |
| 6 | **桌面小组件（Glance）** | ★★★★ | 组件受限、调试慢、刷新时机组合、各 launcher 尺寸差异、深色模式 |
| 7 | 上课提醒通知（P1） | ★★★★ | 国内 ROM 省电策略会杀后台，需引导加白名单；精确闹钟权限 |
| 8 | 导出备份 | ★☆☆☆ | 复用导入模型 |
| 9 | 整体 | ★★★☆ | 中等偏上，主体可控，小组件是唯一硬骨头 |

**结论**：这是一个**体量中等但细节密集**的项目。对有 Android 经验的开发者约 3-4 周可完成 P0；若交给 AI 编码助手逐模块生成，关键在于**数据契约先行**（第 4、5 章），否则 AI 会在模型上反复返工。

---

## 9. 开发里程碑

| 里程碑 | 内容 | 验收标准 |
|---|---|---|
| **M0** | 工程骨架：Gradle、模块划分、Hilt、Room 建表、主题 | 编译通过；数据库导出器可看到 4 张表；空白主页可启动 |
| **M1** | 数据层完成：`WeekSpecParser` + Repository + 单元测试 | 4.3 表格全部用例通过；`WeekSpecParserTest` 全绿 |
| **M2** | 导入链路：`Extractor` → `Normalizer` → `Validator` → `Importer` | 用 `timetable-sample.json` 导入成功；带围栏/带正文的文本也能导入；错误输入能定位到行 |
| **M3** | 导入 UI：导入页（AI 识别 Tab + 文件 Tab）+ 提示词复制 + 预览纠偏页 + 失败闭环 | 一键复制提示词可用且复制出来是纯文本；粘贴 / 文件 / 分享三条回传路径均走通；拆分、合并、编辑可用；导入后课表落库；失败页能复制错误报告 |
| **M4** | 课表主页：周视图网格 + 当前周 + 切换周 | 跨节次课程渲染正确；今日列高亮；左右切换正常 |
| **M5** | 课程 CRUD + 学期管理 + 节次时间表配置 | 增删改查全部可用；多时间段可增删 |
| **M6** | 小组件：三种尺寸 + 刷新调度 | 冷启动后可显示；跨天自动更新；改课后立即更新；深色模式正常 |
| **M7** | 导出备份 + 打磨 | 导出文件可重新导入且数据一致 |
| **M8（P1）** | 提醒通知 + 冲突检测 | 通知能触发；冲突保存时提示 |
| **M9** | MVP 后的**打磨 / Bug 修复 / 新增需求**（迭代性质，非事先规划） | 每个工作项一个 Issue，走独立分支，验证通过后合入 main |

### M9 的定位

M0–M8 是事先规划好的功能块；**M9 是「用起来之后」才发现的问题与想法**，属于持续迭代。它不会关闭，是长期存在的收口。

M9 的工作项必须遵守第 10.5 节的变更流程。已完成的记录见 `docs/CHANGELOG.md`。

---

## 10. AI 协作执行规范

### 10.1 给 AI 的任务拆分原则

- 每个任务**只改一个文件**，或**一个文件内的一组相邻函数**。
- 任务描述必须包含：目标文件路径、要改什么、验收方式。
- 例：
  > 在 `core/common/WeekSpecParser.kt` 中实现 `parseWeekSpec(spec: String, totalWeeks: Int): Set<Int>`，遵循文档 4.3 语法与解析算法。同时创建 `WeekSpecParserTest.kt` 覆盖 4.3 表格全部用例与 5 个非法输入。运行测试直到全绿。

### 10.2 每次会话开始时注入的上下文

```
1. 本文档第 4 章（数据模型）+ 第 5 章（导入格式）
2. 当前里程碑编号（如 M2）及其验收标准
3. 待修改文件的现有代码
```

### 10.3 明令禁止

- 禁止在未询问的情况下修改第 4、5 章的数据契约。
- 禁止引入第 6.1 节之外的依赖（尤其禁止为了"方便"引入 Gson/Moshi 替换 kotlinx.serialization）。
- 禁止跳过单元测试。
- 禁止一次性生成超过 300 行的新文件；超长需拆分任务。
- 禁止在小组件里使用 `Glance` 不支持的 API（Canvas、自定义 Layout、动画）。

### 10.4 遇到不确定时的处理

遇到以下情况**必须停下来问用户**，不要猜：
- 需求冲突或本文档未覆盖的交互细节
- 需要新增数据表字段
- 需要申请新的系统权限
- 性能与体验的权衡（如小组件刷新频率）

### 10.5 变更流程约定（微调 / Bug / 新需求一律走这套）

**任何**对项目的改动——无论多小——都必须完成以下五步，缺一不可：

| 步骤 | 动作 | 说明 |
|---|---|---|
| 1 | **建 Issue** | 一个具体工作项 = 一个 Issue。标题前缀用 `[微调]` / `[Bug]` / `[需求]` |
| 2 | **归 Milestone** | M0–M8 的事项归对应里程碑；MVP 之后的迭代统一归 **M9** |
| 3 | **走独立分支** | 分支名 `chore/*`（微调）、`fix/*`（Bug）、`feat/*`（需求）。**不直接提交到 main** |
| 4 | **更新文档** | 改动涉及功能/交互的，必须更新本文档对应章节（需求类要补进第 3 章 FR 列表） |
| 5 | **记入 CHANGELOG** | 在 `docs/CHANGELOG.md` 追加一条，注明 Issue 号、分支、commit、验证结论 |

合并时机：**验证通过后**由用户确认再合入 main，不要自行合并。

Issue 正文至少包含四段：**背景 / 做了什么 / 验证 / 对应分支与提交**。

---

## 11. 测试清单（人工验收）

- [ ] 用 3 个不同 AI 模型生成课表 JSON，都能成功导入
- [ ] AI 输出带 ```json 围栏 → 能导入
- [ ] AI 输出带"好的，以下是识别结果："前言 → 能导入
- [ ] 点「复制提示词」→ 粘贴到系统备忘录中是无样式的纯文本
- [ ] 复制提示词 → 贴给 AI → 复制 AI 回复 → 回 App 粘贴 → 全程一次走通
- [ ] 在 AI App 里点分享 → 能选到本 App 并进入导入页
- [ ] 分享进来超过 1 MB 的文本 → 被拒绝且有明确提示
- [ ] 导入失败 → 点「复制错误报告」→ 粘出的内容含错误列表与原始文本
- [ ] 原始文本超长时，错误报告被截断且带「已截断」标记
- [ ] 首次导入成功 → 出现添加小组件的引导卡片
- [ ] 手动把 `dayOfWeek` 改成 9 → 报错并提示"第 N 条"
- [ ] 手动删掉一个字段 → 能导入且不崩溃
- [ ] 单双周课程在对应周显示、在非对应周不显示
- [ ] 跨 3 节的课程渲染为连续长块
- [ ] 同一时段两门课 → 并排显示
- [ ] 添加桌面小组件 → 正确显示；杀掉 App 进程后仍显示
- [ ] 系统切换深色模式 → 小组件与 App 均正常
- [ ] 跨过午夜 → 小组件次日自动更新
- [ ] 修改课程 → 小组件立即更新
- [ ] 导出文件 → 重新导入 → 数据一致
- [ ] 学期起始日填非周一 → 自动对齐并提示
- [ ] 删除学期 → 课程与时间段级联删除

---

## 12. 风险与开放问题

| 编号 | 问题 | 现状 / 建议 |
|---|---|---|
| R1 | 不同 AI 模型输出一致性 | 已用提示词 + 别名映射 + 归一化缓解，仍需多模型实测 |
| R2 | 截图质量差导致识别错误 | 提示词要求 AI 不确定时写 `note`；预览页允许用户纠正 |
| R3 | 小组件刷新不够实时 | 15 分钟粒度，已在文档与 UI 说明中告知用户 |
| R4 | 国内 ROM 后台限制 | 提醒通知（P1）需引导用户加入电池优化白名单 |
| R5 | 课表图片含合并单元格/跨周标注 | 需实测后补充提示词规则 |
| R6 | 提示词随模型迭代而失效 | 提示词存 raw 资源、支持随版本更新；失败闭环兜底，用户仍可自助修复 |
| R7 | 截图质量差导致识别错误 | 引导页明确教截图要点；失败页提供「换一张截图重来」出口 |
| Q1 | 是否需要支持一天多校区往返提示 | 待定，P2 |
| Q2 | 是否需要 iPad/平板布局适配 | 待定，P1 |
| Q3 | 学期中途调休如何表达 | 暂定 P2 用"指定日期按某周课表执行"实现 |
