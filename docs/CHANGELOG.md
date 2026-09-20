# 变更记录

MVP（M0–M8）之后的**微调、Bug 修复与新增需求**都记在这里。

记录规则见 `project-blueprint.md` 第 10.5 节：每个工作项都要有 Issue、独立分支、文档更新，最后追加一条到这里。

格式：`[类型] 标题` + Issue 链接 + 分支/commit + 做了什么 + 验证结论。

---

## M9 · MVP 后打磨与问题修复

### #10 [微调] 周视图顶栏紧凑化，课程块完整展示时间/课名/地点

- 分支：`chore/schedule-ui-polish`，commit `126d035` → 已合入 main（`918d018`）
- 顶栏：学期名与周次改上下两行，按钮区固定宽度；三个切换按钮缩到 36dp；本周时隐藏「回到本周」
- 课程块：改为三行（开始时间 / 课程名 / 地点），课程名最多 3 行不截断；`ROW_HEIGHT` 56dp → 64dp
- `ScheduleBlock` 新增 `startMinutes` / `endMinutes`
- 补 8 个边界单测（`ScheduleLayoutTest` 12 → 20）
- 验证：本周态与第 3 周态均正常

### #11 [Bug] 切换学期后课程管理与周视图不刷新，必须重启应用

- 分支：`fix/term-switch-not-refreshing`，commit `d050a1c` → 已合入 main（`918d018`）
- 根因：两个 ViewModel 在 `init` 里只读一次当前学期 ID，没有监听 DataStore 变化
- 修复：新增 `CurrentTermProvider` 把「当前学期」做成 Flow，ViewModel 用 `flatMapLatest` 重新订阅
- 附带：学期管理页「当前」标记改为持续监听；切换学期后刷新小组件
- 验证：切到旧学期、切到空学期 TermB，两处都立即刷新

### #12 [需求] 周视图点击课程块编辑 + 自定义课块颜色

- 分支：`feat/course-block-edit-and-color`，commit `6fb9412`
- 新增 FR-09（课程块快捷编辑）、FR-10（自定义课块颜色），已写入主文档第 3 章
- 点击课程块弹出底部抽屉：上半课程信息（名称/教师/颜色），下半本节课（周几/节次/周次/地点）+ 删除本节
- 12 色预设色板 + 「自动」；颜色按课程粒度；同步到桌面小组件
- 色板下沉到 `core:common`（纯整数），`ScheduleBlock` 携带解析后色值；`CourseTextColor` 按亮度切黑/白文字
- **过程中修掉**：① 色板扩容导致自动色全部错位（改用固定 `AUTO_COLOR_COUNT` 常量）② 色板一行放不下被截断（改 `FlowRow` 两行）
- 验证：改色后两个时间段同时变色并落库；改回自动后 `color_argb` 回到 null；小组件颜色同步

### #13 [需求] 编辑抽屉的操作按钮移到右上角并改为图标

- 分支：`feat/course-block-edit-and-color`，commit `5211366`
- 抽屉操作区移到右上角，改为三个图标：保存（软盘）/ 删除（红色垃圾桶）/ 取消（叉）
- 图标用**自绘 VectorDrawable**（`core:designsystem/res/drawable`），**未引入 material-icons-extended**（遵守技术栈锁定）
- 每个图标带 `contentDescription`，读屏可用
- **过程中修掉**：`Row + Text(weight(1f)) + IconButton` 的组合实测会把按钮压成 0 宽（UI 树里能查到节点却完全不可见）→ 改为 `Box + align(CenterStart/CenterEnd)` 嵌套 Row。**不要用 weight 做「左标题右按钮」布局**
- 验证：抽屉右上角三图标齐全；UI 树 `保存` / `删除本节课` / `取消` 三个 content-desc 均存在；三个按钮实测 **126×126 px**（=48dp，未被压扁）；截图目视确认蓝色软盘 / 红色垃圾桶 / 叉号

### #14 [需求] 本节课信息改用滚轮选择器

- 分支：`feat/course-block-edit-and-color`，commit `5211366`
- 新增 `core:designsystem/picker/WheelPicker`（`LazyColumn` + `rememberSnapFlingBehavior`，**无第三方库**）
- 三个滚轮并排：周几 / 起始节 / 结束节；中间高亮条 + 上下淡出；`LabeledWheel` 便于表单复用
- 节次取值范围取自当前学期的节次时间表长度
- 验证：滚轮停在 `周三 / 3 / 4`，与「高等数学A 周三第3-4节」完全一致

### #15 [需求] 周次编辑改为点选式

- 分支：`feat/course-block-edit-and-color`，commit `5211366`
- 新增 `core:common/WeekSpecFormatter`（周集合 ⇄ 表达式）与 `WeekPicker` 点选 UI（网格 + 整学期/单周/双周/清空）
- **存储仍是字符串**：`weekSpec` 是导入格式契约，未改数据契约、未做数据库迁移
- 验证：1–18 网格，1–16 已选 / 17–18 未选，显示「已选 16 周」；生成的表达式可被 `WeekSpecParser` 正确解析

### #16 [需求] 课程管理的编辑课程页同步滚轮与周次选择器

- 分支：`feat/course-block-edit-and-color`，commit `5211366`
- `EditableSession.weekSpec` 由字符串改为 `weeks: Set<Int>`，UI 换成滚轮 + 周次点选器，与抽屉表现一致
- `feature:course` 新增依赖 `core:designsystem`
- 验证：课程管理 → 编辑课程页每个时间段显示三个滚轮 + 周次网格

### #17 [Bug] WheelPicker 首帧可能回写选中值，把用户已选值重置为第一项

- 分支：`feat/course-block-edit-and-color`
- 根因：`LaunchedEffect(state.isScrollInProgress)` 判断的是「**状态为 false**」而不是「**发生过滚动并停止**」。`isScrollInProgress` 初始即 false，首次组合就被误当成一次「滚动结束」；而此刻视口中心恰好落在第 0 项，于是回调 `onSelectedChange(0)`，**把外部传入的选中值静默重置成「周一 / 第 1 节」**。属于竞态，依赖两个 effect 的调度顺序，时好时坏
- 修复：抽出 `core:common/WheelScrollGuard`（纯逻辑）记录「是否真的滚动过」，只有滚动过、且已停止才允许回写；顺带过滤「落点未变」与「拿不到布局信息」两类无意义回调
- 补 **9 个单测**（`WheelScrollGuardTest`），其中「连续多次首帧式调用都不回写」「回写后守卫复位」两条专门守住修复本身不退化成永久放行
- 说明：Compose 生命周期竞态无法用 JVM 单测直接覆盖，故把判定逻辑**下沉为纯函数**再测，实机行为另行验证
- 验证：core:common 82 个用例全绿；实机打开抽屉，滚轮稳定停在与数据一致的项上，不再自跳

### #18 [微调] 清理 CourseEditScreen 中改造后残留的死代码

- 分支：`feat/course-block-edit-and-color`
- 删除 `CourseEditScreen.kt` 的 `NumberField(...)`（#16 改造后已无调用方）
- **勘误**：本 Issue 初稿曾把 `sectionItems` 也列为死代码，动手前 grep 复核发现 `SessionEditor` 的「起 / 止」两个 `LabeledWheel` 仍在调用它（`:376`、`:383`），**实际保留**；`WEEKDAY_ITEMS` 同理
- 另清理 `WheelPicker.kt` 中三个从未使用的导入（`getValue` / `setValue` / `mutableIntStateOf`）
- 教训：同名私有函数在多个文件里各有一份（`CourseBlockEditSheet.kt` 也有 `sectionItems`），「有没有人用」必须逐文件 grep，不能凭记忆判断

### #19 [微调] 补 WeekSpecFormatter 边界单测

- 分支：`feat/course-block-edit-and-color`
- 新增 `WeekSpecFormatterEdgeTest`（**5 个用例**）：空集合 → 空串且 `parseOrNull` 为 null；全选 → `1-18`；单周集合往返等价；真实用例矩阵（含 `1-9,11-18` 及其单周形式）；越界防护
- 空集合那条把「保存逻辑必须先拦住未选周次」这一前提**钉成测试**
- 往返断言用「**周集合等价**」而非字符串相等 —— 规范化会改变字面量（`{1,3..15}` → `1-15单`）
- 验证：全绿，计入 core:common

### #20 [重构] 抽出共享的课程/时间段编辑表单到 core:designsystem

- 分支：`feat/import-preview-edit`
- 新增 `core:designsystem/editor/`：`CourseSessionEditState`（纯数据）+ `CourseSessionEditForm`（表单主体）
  + `SheetTopBar`（顶部操作条）+ `CourseColorPicker`
- 表单**接收纯参数**、不含动作按钮与抽屉语义，因此 `feature:schedule`（周视图抽屉）与
  `feature:importexport`（导入预览抽屉）都能用。周视图传 3 个按钮（含删除），预览页传 2 个
- `sheet_*` / `weekday_*` 文案从 `feature/schedule` 搬到 `core:designsystem`
- **`CourseBlockEditSheet` 改为使用共享表单**，只保留「填状态 / 回传结果」
- 顺手给周视图抽屉补上**备注**字段（DB 早有 `CourseSession.note`，课程管理页也能改，此前抽屉漏了）
- 验证：`:app:assembleDebug` 通过；周视图抽屉的字段、取值、滚轮定位、周次回填、颜色保存行为不变

> **踩坑**：文案搬家后 `feature/schedule` 与 `core:designsystem` 会**同时定义** `sheet_title` 等名字，
> 造成同名资源重复。已把搬走的从 `feature/schedule` 删除，并在该文件里留了注释防止后来者重新定义。
> 另外两个模块都有 R 类，`CourseBlockEditSheet` 里用 `import ... R as DsR` 区分，避免指错模块。

### #21 [需求] 导入预览页支持编辑时间段的全部字段（含颜色）

- 分支：`feat/import-preview-edit`
- 课程卡片改为**只读**（课名 / 教师 / 颜色圆点 / 时间段列表），点时间段行弹出编辑抽屉
- 抽屉两段：**课程信息**（名称 / 教师 / 颜色，作用于该课全部时间段）、
  **本节课**（周几 / 起止节 / 周次点选 / 地点 / 备注，只作用于当前这一条）
- 卡片上保留「拆分为新课」与「删除」；抽屉里**不放**删除
- `MergedCourse` 新增 `colorArgb: Int? = null`，`confirmImport()` 传给 `Course`
  （`Course.colorArgb` 早已存在，**未改第 4/5 章数据契约、无需迁移**）
- `ImportViewModel` 新增 `updateSession` / `updateCourseColor`；`defaultSectionCount()` 改为
  「默认节次表长度」与「草稿里已用到的最大节次」取大者 —— 否则 AI 给的更大节次会被滚轮夹掉
- **为什么周次用点选网格而非「起止周」**：起止表达不了 `1-16单`（单周）与 `1-9,11-18`（跳周），
  而这两种在示例课表里都存在。用起止会导致「打开单周课、什么都不改直接保存 → `1-16单` 变成 `1-16`」
  的静默数据损坏
- 附加价值：`SessionDraft` 字段全 nullable，AI 漏给周几/节次的记录原会被静默丢弃，
  现在可在预览页补齐
- 验证：见文末实机验证

### #22 [需求] 导入预览页支持修改学期起始日

- 分支：`feat/import-preview-edit`
- 学期信息卡新增「修改」→ Material3 `DatePicker`；选定后**对齐到周一**
- **为什么必须对齐**：`startMonday` 的语义是「第一周的周一」，`CurrentWeekCalculator` 依赖它算周次。
  存入周中日期会让「今天第几周」出现半周边界错误，进而影响周视图高亮与桌面小组件
- DatePicker 返回的是 **UTC 毫秒**，按 `ZoneOffset.UTC` 还原日期，避免时区把日期挪一天
- 验证：改起始日 → 落库 → 学期管理页 `startMonday` 正确且为周一

### #23 [Bug] 未选周次时会写入空 weekSpec，导致该课程永久不显示

- 分支：`feat/import-preview-edit`
- 根因：`confirmImport()` 写的是 `draft.weeks ?: "1-$totalWeeks"`，**只判 null**。
  但周次点选器有「清空」，清空后是**空字符串** → 兜底不生效 → 库里写入 `week_spec = ""`
  → 周视图 `parseOrNull("")` 返回 null → `?: continue` 静默跳过 → **整门课永久不显示且无任何报错**
- 此前预览页不能改周次，所以碰不到；#21 放开编辑后一点「清空」保存就会踩中
- 修复：新增 `core:common/WeekSpecFallback.orFullTerm()` 判 null **与空白**，落库改用它；
  两个编辑抽屉在保存前**拦住未选周次**并给出错误提示，不写库
- 补 **8 个单测**（`WeekSpecFallbackTest`），其中「返回值一定可以被解析」一条把
  「兜底必须产出合法表达式」钉成测试
- 验证：全绿，计入 core:common

---

## 模板（下次新增时复制）

```
### #N [类型] 标题

- 分支：`xxx`，commit `xxx` → 状态
- 做了什么（要点列举）
- 验证：结论
```
