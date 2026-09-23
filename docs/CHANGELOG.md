# 变更记录

MVP（M0–M8）之后的**微调、Bug 修复与新增需求**都记在这里。

记录规则见 `project-blueprint.md` 第 10.5 节：每个工作项都要有 Issue、独立分支、文档更新，最后追加一条到这里。

格式：`[类型] 标题` + Issue 链接 + 分支/commit + 做了什么 + 验证结论。

---

## v0.2.0-demo · 2026-09-22

- 重构桌面小组件：统一单入口，支持 `4×1 / 4×2 / 4×4`、纵向课程列表、按实例配置、课程范围与多种背景样式。
- 节次时间表新增全局单节时长和课间时长、滚轮编辑、断点递推，以及确认前的相邻节次冲突校验。
- 全面应用轻松、柔和的卡通视觉语言，并统一周视图「导入 / 管理」操作按钮。
- 发布版本升级为 `versionCode 2` / `versionName 0.2.0-demo`；完整实现和验证记录见下方 #32–#34。

## M9 · MVP 后打磨与问题修复

### #42 [需求] 节次时间表离开时提醒保存未提交修改

- Issue：[#42](https://github.com/Leng-CS/fkwakeup/issues/42)；分支：`feat/section-timing-unsaved-prompt`；实现 commit `751f03d`。
- 页面返回和系统返回共用未保存判断：草稿变化时弹出「保存 / 放弃更改 / 继续编辑」，保存成功才离开，失败留在页面；无效草稿禁用保存。未修改、修改后恢复原值、或已点击页面保存时直接返回。
- 比较进入页面时的编辑状态与当前状态，避免导入的非标准节次表在首次打开时误报；「恢复默认」仍为立即保存。
- 验证：206 项单元测试及 3 项真实 SQLite 仪器测试通过，Debug APK 构建通过；模拟器实测两种返回方式及弹窗三项选择、保存后重进仍可见更改、页面底部保存后直接返回。测试前备份并恢复模拟器数据。

### #41 [Bug] 节次修复 APK 与 v2 数据库不兼容导致启动崩溃

- Issue：[#41](https://github.com/Leng-CS/fkwakeup/issues/41)；分支：`fix/section-timing-v2-compat`；节次修复移植 commit `bb23697`。
- 根因：模拟器已由网课分支升级到 Room v2；覆盖安装来自 v1 分支的节次修复 APK 后，启动报 `A migration from 2 to 1 was required but not found`。同一 `versionCode 2` 不代表内部数据库 schema 相同。
- 在保留 v2 schema 和已有 1→2 迁移的网课分支上集成 #38 节次修复并重新构建 APK，不做破坏性降级或清除数据。
- 验证：全量单测、3 项真实 SQLite 仪器测试及 Debug APK 构建通过；模拟器冷启动正常，周视图显示网课。覆盖安装前后数据库均为 v2，8 个学期、96 条节次、39 门课程、44 条时间段、1 条网课安排数量一致，无新增启动崩溃。

### #38 [Bug] 节次全局规则更新异常及保存崩溃导致时间表丢失

- Issue：[#38](https://github.com/Leng-CS/fkwakeup/issues/38)；原分支：`fix/section-timing-save`，实现 commit `497a116`；v2 兼容分支移植 commit `bb23697`。
- 保存时绑定目标学期并以事务替换节次表，失败回滚旧数据；只在开始时间真正变化时新增断点；冲突提示与滚轮分开显示。
- 验证与旧版已丢失时间表的恢复方式见 [调试记录](section-timing-debug-38.md)。

### #39 [需求] 支持异步网课与直播网课

- Issue：[#39](https://github.com/Leng-CS/fkwakeup/issues/39)；分支：`feat/online-course-support`；实现 commits `970051a`、`9f6decd`、`4fa50df`、`0b2bc00`、`2173724`
- 数据契约升级为 `campus-timetable v1.1`，新增直播授课方式和可重复的异步网课开放期，同时继续兼容 v1.0 导入；导出统一生成 v1.1。
- Room 数据库升级到 v2 并提供显式迁移；课程编辑页支持线下／直播切换、平台和链接，以及按日期配置多个异步开放期。完全位于学期外的开放期禁止保存，跨学期边界时提示但允许保存。
- 周视图增加可折叠网课区，按进行中、未开始、已结束排序；已结束课程灰化并默认折叠。网课卡片进入 App 内课程详情，链接从详情页打开，提醒按钮进入 M8 占位页。
- 直播时间段和异步开放期均不进入桌面小组件；导入预览、实际落库、课程管理计数与识别提示词均已接入新结构。
- 验证：`gradlew testDebugUnitTest :app:assembleDebug`、`gradlew :app:lintDebug :widget:glance:lintDebug` 全绿；Debug APK 在 `emulator-5554` 覆盖安装后数据库迁移成功，并实测新建异步网课、保存及周视图展示无崩溃。

### #36 [需求] 建立自动化 APK 发布流程

- Issue：[#36](https://github.com/Leng-CS/fkwakeup/issues/36)；分支：`feat/automated-apk-release`；实现 commits `fb44305`、`1bb4845`、`98fc0f6`
- 新增标签触发的 GitHub Actions 工作流：先运行全量单元测试并构建 Debug APK，再使用仓库内置的最小 `contents: write` 权限创建预发布版本。
- APK 与 SHA-256 校验文件会一并上传；对应版本的发布说明保存在 `docs/releases/`，不存在时自动使用 GitHub 生成说明。
- 验证：工作流配置已通过 `git diff --check`；最终以 `v0.2.0-demo` 标签的实际 Actions 运行和 Release 资产为准。

### #34 [需求] 统一轻松柔和的卡通视觉风格

- Issue：[#34](https://github.com/Leng-CS/fkwakeup/issues/34)；分支：`feat/soft-cartoon-ui`；实现 commit `73a498c`
- 建立「云朵课表」视觉语言：奶油纸张底色、蓝莓主色、蜜桃与薄荷点缀；补齐浅色/深色 ColorScheme、全局圆角和字体层级，并默认关闭会覆盖产品配色的系统动态取色。
- 周视图改为分层主信息卡、圆形周切换、薄荷色今天胶囊与贴纸式课程块；保留时间、课名、地点、灰化和当前课程边框等全部信息与状态逻辑。
- 后续视觉细化：周视图的「导入 / 管理」改为同尺寸、同圆角、同底色的胶囊按钮组，不再以悬空文字链接呈现。
- 课程管理把拥挤在顶栏的导出/学期/节次/小组件入口整理成两行工具区；导入流程用三组柔和容器区分步骤、AI 操作和文件操作。
- 新增 `docs/ui-style-guide.md` 与 2 个主题对比度测试，核心文字/背景组合均达到 `4.5:1`；未修改第 4、5 章数据契约。
- 验证：`gradlew testDebugUnitTest :app:assembleDebug` 全绿；Debug APK 已安装到 `Pixel_8_API_35`，使用 5 门课程实测浅色/深色周视图、课程管理、导入输入与导入预览，无遮挡或崩溃。

### #33 [需求] 节次时间表支持滚轮编辑与连续时段自动递推

- Issue：[\#33](https://github.com/Leng-CS/fkwakeup/issues/33)；分支：`feat/section-time-smart-schedule`；修正 commit `eda9d78`
- 节次保留原有的第 1 节至第 12 节连续列表；顶部只配置一组全局单节时长与课间时长，均使用滚轮，不再输入 `HH:mm`。
- 点击某节的时间打开开始／结束滚轮。改结束时间会更新全局单节时长；改开始时间则将该节作为断点，之后节次按全局规则递推；断点可恢复自动递推。
- 保存前同时校验单节起止顺序、相邻节次不能重叠、结果不得跨越当天；最终仍只保存既有 `SectionTemplate` 起止时间，未修改第 4、5 章数据契约。
- 修正 commit `786645d`：确认前先预演完整时间表。若与下一节重叠或跨越当天，滚轮弹窗保留并直接显示原因，不写入导致列表失去修正入口的无效状态。
- 验证：`gradlew :core:common:test`、`gradlew :feature:course:compileDebugKotlin`、`gradlew testDebugUnitTest :app:assembleDebug` 全绿；Debug APK 已安装并在 `emulator-5554` 成功打开列表与时间滚轮弹窗。

### #32 [需求] 小组件改为可缩放、可滚动、按实例配置的课程列表

- 分支：`feat/widget-customizable-list`，实现 commits `5307f96`、`a696f80`
- 移除旧的「紧凑版 / 详细版」双入口，保留单一小组件；默认四列宽并支持 `4×1 / 4×2 / 4×4` 的垂直尺寸。
- 每个桌面实例按 `AppWidgetId` 单独持久化范围、左栏、背景、透明度、文字透明度、深色预设与相册 URI；App 内「课程管理 → 小组件」自动列出实例为「课表小组件 1 / 2 …」。
- 课程列表支持内部纵向滚动；每个时间段独立两行显示，`今日＋明天 / 本周` 有日期分组，范围为空时展示下一次课程。
- 左栏支持课程颜色、节次、两者；背景支持透明、纯色、双色渐变、三种内置预设及相册图片。闹钟按钮只进入 M8 占位页。
- 合并前审查修正 commit `bfb6284`：图片与渐变背景改用 Glance 公开的 `ImageProvider(Bitmap)` 接口，消除 3 个 `RestrictedApi` Lint 错误。
- 点课程、列表空白区或下一次课程会打开对应日期的周视图；课程开始/结束的单次刷新与 15 分钟兜底、广播刷新均保留。
- 验证：`gradlew :widget:glance:lintDebug testDebugUnitTest :app:assembleDebug` 全绿；Debug APK 已安装到 `Pixel_8_API_35` 并成功启动。
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

- 分支：`feat/import-preview-edit`，commit `d000353` → 已合入 main（`d000353`）
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

- 分支：`feat/import-preview-edit`，commit `d000353` → 已合入 main（`d000353`）
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

- 分支：`feat/import-preview-edit`，commit `d000353` → 已合入 main（`d000353`）
- 学期信息卡新增「修改」→ Material3 `DatePicker`；选定后**对齐到周一**
- **为什么必须对齐**：`startMonday` 的语义是「第一周的周一」，`CurrentWeekCalculator` 依赖它算周次。
  存入周中日期会让「今天第几周」出现半周边界错误，进而影响周视图高亮与桌面小组件
- DatePicker 返回的是 **UTC 毫秒**，按 `ZoneOffset.UTC` 还原日期，避免时区把日期挪一天
- 验证：改起始日 → 落库 → 学期管理页 `startMonday` 正确且为周一

### #23 [Bug] 未选周次时会写入空 weekSpec，导致该课程永久不显示

- 分支：`feat/import-preview-edit`，commit `d000353` → 已合入 main（`d000353`）
- 根因：`confirmImport()` 写的是 `draft.weeks ?: "1-$totalWeeks"`，**只判 null**。
  但周次点选器有「清空」，清空后是**空字符串** → 兜底不生效 → 库里写入 `week_spec = ""`
  → 周视图 `parseOrNull("")` 返回 null → `?: continue` 静默跳过 → **整门课永久不显示且无任何报错**
- 此前预览页不能改周次，所以碰不到；#21 放开编辑后一点「清空」保存就会踩中
- 修复：新增 `core:common/WeekSpecFallback.orFullTerm()` 判 null **与空白**，落库改用它；
  两个编辑抽屉在保存前**拦住未选周次**并给出错误提示，不写库
- 补 **8 个单测**（`WeekSpecFallbackTest`），其中「返回值一定可以被解析」一条把
  「兜底必须产出合法表达式」钉成测试
- 验证：全绿，计入 core:common

### #24 [重构] 统一 action_* 通用按钮文案到 core:designsystem

- 分支：`feat/shared-action-strings`
- `action_confirm`(确定) / `action_cancel`(取消) / `action_save`(保存) / `action_remove`(删除)
  此前在 `feature/course`、`feature/schedule`、`feature/importexport` **各存一份**，语义完全相同；
  改文案要同时改 3 处，极易漂移
- 收敛到 `core:designsystem`，**13 处引用**改指 `com.lengcs.fkwakeup.core.designsystem.R`
  （用 `as DsR` 别名区分，因为同模块也有 `R`）
- 三个 feature 的 `strings.xml` 中删除同名定义并留注释，防止后来者重新定义
- **纯资源搬迁，无行为变更**；这是 #20 那次「同名资源跨模块重复」踩坑的同类收口
- 验证：全量单测 133 全绿 + `:app:assembleDebug` 通过

### #25 [需求] 周视图顶栏加常驻「导入课表」入口

- 分支：`feat/schedule-import-entry`，commit `5cb57a9` → 已合入 main（`bb5ea6f`）
- **问题**：导入页此前只有两个入口 —— 周视图「**还没有课表**」时的空态按钮，
  和从其它 App 分享文本进来（`ShareImportActivity`）。**已有一份课表后，
  应用内就再也找不到导入入口了**。学期结束要导入新学期、第一份认错了想重来、
  想再导一份对照 —— 三种常见情况都会撞上
- 顶栏 `ScheduleTopBar` 加**常驻**的「导入」按钮，置于「管理」左侧
- 沿用 #10 的紧凑样式；文案用短词「导入」而非复用 `schedule_empty_action` 的
  「导入课表」，避免挤占学期名
- **空课表时不显示**（`onImport` 传 `null`）—— 那时页面中央已有主 CTA，
  顶栏再来一个是噪音
- 顺带把顶栏「管理」的**硬编码中文**改为 `stringResource`：新按钮与它相邻，
  一个用资源一个硬编码会不一致（同属一个函数内的相邻代码）
- **记录一个既有行为**：「再导入」会**新建一个学期并切换过去**，旧学期课程完整保留
  （多学期共存是既定设计）。本次不改，但用户点完导入若发现「课表变空了」，
  实际是切到了新学期的空课表
- 验证：空课表时顶栏无该按钮、中央大按钮正常；导入一份后顶栏出现「导入」（容器 152×126 px，
  未被压成 0 宽）；点击后进入导入页且是**干净的输入态**（不是上次的 Done 页）；
  顶栏布局不挤，学期名完整未截断

### #26 [Bug] 课程管理里新增/修改课程用错学期，导致新课程看不到、已有课程被搬走

- 分支：`fix/course-edit-wrong-term`
- **现象**（用户报告）：多学期时，① 课程管理页添加课程保存后看不到 ② 修改已有课程保存后，
  该课程在周视图和课程管理页都消失（像被删了）
- **根因**：`CourseEditViewModel` 用 `termRepository.observeTerms().first()` 当作当前学期，
  但 `TermDao.observeActive()` 是 `ORDER BY start_monday_epoch_day DESC` ——
  **`first()` 是「开学日期最晚的学期」**，不是当前学期。两者只在**单学期**时恰好相同，
  所以此前一直没暴露
  - 新增：课程被写进那个学期 → 当前学期的列表里看不到
  - 修改：`updateCourse` 用错误的 `termId` 覆盖 `course.term_id` → 课程被**搬到另一个学期**，
    从当前学期彻底消失。**这不只是显示问题，是数据被改了**
- **实测证据**（5 个学期；`observeTerms().first()` = `term_id=2`，当前学期 = `term_id=5`）：

  | 操作 | 结果 |
  |---|---|
  | 新增 `TestCourseA` | 写入 `term_id=2`（应为 5）→ 管理页看不到 |
  | 打开「体育（篮球）」**不改任何字段**直接保存 | `term_id` 由 **5 → 2**，课程从当前学期消失 |

- **修复**：
  1. `CourseEditViewModel` 改用 `CurrentTermProvider.observeCurrentTerm()`（真正的当前学期）
  2. `save()` 不再静默失败 —— 没有学期时提示「请先在学期管理里新建一个」
  3. 「选哪个学期」的规则抽成 `core:common/CurrentTermPick`（纯逻辑、可单测），
     `CurrentTermProvider` / `SectionTemplateViewModel` / `TermManageViewModel` /
     `WidgetDataProvider` 四处**复用同一套规则**，不再各自复刻
  4. `TermRepository.observeTerms()` 补 KDoc 警示：`first()` 不是当前学期
  5. `TermManageViewModel.delete()` 的兜底显式传 `configuredTermId = 0` ——
     此刻 settings 里还是刚被删掉的 id，走「配置优先」会拿到已不存在的学期
- 补 **7 个单测**（`CurrentTermPickTest`），把「配置优先 / 兜底靠后 / 兜底结果 ≠ 当前学期」钉住
- 验证：修复后同样操作 —— 新增 `FixCheckA` 写入 `term_id=5` 且管理页可见；
  「线性代数」保存后 `term_id` 仍为 5，管理页与周视图都在。全量单测 **140 全绿**

### #27 [Bug] 导入页没有返回按钮

- 分支：`feat/ui-polish-and-past-block`
- **现象**（用户报告）：从周视图顶栏点「导入」进去后，没有任何返回入口，只能按系统返回键
- **根因**：`ImportFlow` 的 `Scaffold` **没有 topBar**，且导航处压根没传 `onBack`
  （`ImportFlow(initialText=..., onImported=...)`）
- **缺陷早于 #25 就存在**（从空课表的「导入课表」进去也一样），但 #25 之后更容易撞上 ——
  已有一份课表的人点进来了，想回去却找不到按钮
- 修复：`ImportFlow` 加 `onBack` 参数与顶栏；`MainActivity` 传 `navController.popBackStack()`。
  **只在 `Input` / `Failure` 两个阶段显示顶栏** —— 预览页自带 `TopAppBar`（它自己也有 `Scaffold`），
  再套一层会出现两条标题栏
- 分享入口同样受益：`ShareImportActivity` 走的是 `HOME → IMPORT` 的正常返回栈，`popBackStack` 有效
- 验证：顶栏出现「< 导入课表」；点 `<` 回到周视图（顶栏显示「第 2 周 · 本周」）

### #28 [微调] 课程管理页「新增课程」缺按钮样式

- 分支：`feat/ui-polish-and-past-block`
- **现象**（用户报告）：`新增课程` 是个 `TextButton`（`labelSmall` 字号），视觉上像一行文字链接
- 修复：改成填充样式的 `Button`，放在顶栏下方的独立一行，与课程列表拉开层次
- 验证：截图确认是圆角实心主色按钮，不再是文字链接

### #29 [需求] 已上完的课块变灰，正在上的突出

- 分支：`feat/ui-polish-and-past-block`
- **改动前的行为**：只有「今天这一列」且节次已经过去的课块降到 `alpha 0.5`。
  周一到周五哪怕早就过完了仍是全彩
- **新规则：只按时间判定** —— 课块的结束时刻早于此刻就是「已上完」，
  **不关心它属于第几周**。于是切成上一周会整周变灰，切到下一周全彩，切到过去的学期也全灰
- 三态：`Upcoming`（全彩）/ `Ongoing`（全彩 **+ 主色边框**突出）/ `Past`（去饱和变灰）
- **为什么用「去饱和」而不是继续用半透明**：半透明只是变淡，颜色还在，看着是「淡」不是「灰」。
  `MutedBlockColor` 把颜色朝它自身的 Rec.601 亮度收敛（保留 15% 色相），
  整体一眼是灰的，但不同课程仍留有明度差异，不会糊成一坨
- **文字颜色按灰化后的颜色算** —— 灰化会改变亮度，沿用原色判断可能让浅灰底配白字看不清
- **缺起止时间时一律判 `Upcoming`**（宁可不灰，不要误灰）：节次表没配全时 `ScheduleLayout`
  给不出时间，此时把课涂灰会让人以为课已经上完了
- **桌面小组件同步**：`WidgetCell` 带上 `phase`。小组件是「每格标课名」的缩略形式，
  一节课跨几节就出现几次，所以相位**按这一格所在的节次单独判**，
  不能整块共用一个结果 —— 否则正在上的那节课会被它后面那节连累成灰
- 核心逻辑做成 `core:common` 的纯函数并补 **21 个单测**
  （`BlockPhaseTest` 14 + `MutedBlockColorTest` 7），
  其中「上一周的课在当下全部是 Past」一条把「只看时间、不看周次」这个**有意为之**
  的行为钉成测试，免得以后被当成 bug 改掉
- 验证：周六晚上（设备时钟）看周视图 —— 周一至周六全灰、周日（今天）未开始的课保持全彩；
  再用导入构造「08:55-14:45 跨 4 节且此刻正在进行」的课，确认它**全彩且带边框**，
  同一列「08:00-08:45 已结束」的课则是灰的

---

## 模板（下次新增时复制）

```
### #N [类型] 标题

- 分支：`xxx`，commit `xxx` → 状态
- 做了什么（要点列举）
- 验证：结论
```
