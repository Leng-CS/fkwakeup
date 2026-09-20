# AGENTS.md

给 AI 编码助手的项目约定。**开工前必须完整读完本文件与 `docs/project-blueprint.md`。**

> 如果是接手已有仓库、需要了解环境与工程约定，先读 `docs/HANDOFF.md`。
> 本文专注编码约定。

## 一句话项目

Android 原生课程表 App：用户把教务系统课表截图交给 AI，AI 输出结构化 JSON，App 导入即建课表，并支持桌面小组件。

## 开工前必读（按顺序）

1. `docs/project-blueprint.md` **第 4 章** —— 数据模型。这是全部代码的契约。
2. `docs/project-blueprint.md` **第 5 章** —— 导入格式 v1.0。这是与外部 AI 的接口契约。
3. `docs/project-blueprint.md` **第 3、9、10 章** —— 功能需求（FR 列表）、里程碑与验收标准、AI 协作与变更流程。
4. `docs/CHANGELOG.md` —— 最近改了什么（微调 / Bug / 需求）。
5. `docs/recognition-prompt.md` —— UI 文案与 `res/raw` 提示词内容的唯一来源。

**不要跳过第 1、2 步直接写 UI。** 数据契约一变，UI 全部返工。

## 技术栈锁定

Kotlin + Jetpack Compose + Material3 + Room + DataStore + kotlinx.serialization + Hilt + Glance + WorkManager。

**禁止引入上面列表之外的第三方库。** 尤其禁止用 Gson / Moshi 替换 kotlinx.serialization。

## 目录约定

```
app/                    Application + MainActivity + 导航宿主
  src/main/res/raw/
    prompt_import.txt   导入提示词，对应《recognition-prompt.md》第二节
    prompt_repair.txt   修复版提示词，对应第三节，含 {ERRORS} / {ORIGINAL}
  ShareImportActivity   ACTION_SEND text/plain 分享入口
core/model/             领域模型（纯 Kotlin，无 Android 依赖）
core/common/            WeekSpecParser / WeekSpecFormatter（周次表达式 ⇄ 周集合）
                        ScheduleLayout（网格布局算法）、CourseColorPalette（色板，纯整数）
                        WheelScrollGuard（★ 滚轮回写的首帧防护，纯逻辑，见 CHANGELOG #17）
                        CurrentTermPick（★「选哪个学期」的唯一规则，见 CHANGELOG #26）
                        BlockPhase / BlockPhaseCalculator / MutedBlockColor（★ 课块时间状态与灰化，见 CHANGELOG #29）
core/database/          Room entities / dao / repository
  repository/CurrentTermProvider   ★「当前学期」的响应式来源，页面切换学期靠它刷新
core/importer/          ★ 导入解析：Extractor / Normalizer / Validator
core/exporter/          导出为 campus-timetable v1.0（要求可往返）
core/designsystem/      配色、通用组件与**共享文案**
  picker/WheelPicker    滚轮选择器（周几 / 起止节）
  picker/WeekPicker     周次点选网格
  editor/               ★课程/时间段编辑表单（CourseSessionEditForm / SheetTopBar / CourseColorPicker），
                        feature:schedule 与 feature:importexport 共用；sheet_* / weekday_* / action_* 文案也在这
feature/schedule/       周视图主页 + 课程块编辑抽屉（CourseBlockEditSheet）
feature/course/         课程管理与编辑 + 学期管理 + 节次时间表
feature/importexport/   导入页 + 预览纠偏页
feature/settings/       预留
widget/glance/          Glance 小组件 + 更新调度
```

## 变更流程（微调 / Bug / 新需求一律走这套）

改动无论多小，都必须完成五步：

1. **建 Issue** —— 一个工作项一个 Issue，标题前缀 `[微调]` / `[Bug]` / `[需求]`
2. **归 Milestone** —— M0–M8 归对应里程碑；MVP 之后的迭代统一归 **M9**
3. **走独立分支** —— `chore/*`、`fix/*`、`feat/*`，**不直接提交到 main**
4. **更新文档** —— 涉及功能/交互的必须更新 `docs/project-blueprint.md`（需求类补进第 3 章 FR 列表）
5. **记入 CHANGELOG** —— 在 `docs/CHANGELOG.md` 追加一条（Issue 号 + 分支 + commit + 验证结论）

合入 main 需**用户确认**，不要自行合并。Issue 正文至少四段：背景 / 做了什么 / 验证 / 对应分支与提交。

详见 `project-blueprint.md` 第 10.5 节。

## 编码规范

- 单向数据流：`StateFlow` + `UiState` 密封类，Composable 不直接访问数据库。
- Composable 命名用名词：`ScheduleScreen`、`CourseCard`，不写 `DrawCourse`。
- 提交信息：`feat(schedule): 支持跨节次课程合并渲染`。
- **每完成一个逻辑单元必须补单元测试**，无测试视为未完成。必测项见主文档 6.5 节。
- 单个任务只改一个文件（或一个文件内的一组相邻函数），产出 diff 化改动。
- **通用 UI 文案统一放 `core:designsystem`**（`action_*` 确定/取消/保存/删除、`weekday_*`、`sheet_*` 等），
  **不要在 feature 模块里重复定义同名 string** —— 同名资源跨模块重复是 Android 资源合并的隐患。
  跨模块引用时用 `import com.lengcs.fkwakeup.core.designsystem.R as DsR` 区分（同模块也有 `R`）。
  已收口两次（`sheet_*` / `weekday_*` 见 #20，`action_*` 见 #24），**别再造第三份**。

## 明令禁止

- 禁止修改第 4、5 章定义的数据契约而不先询问。
- 禁止引入技术栈之外的依赖。
- 禁止跳过单元测试。
- 禁止一次性生成超过 300 行的新文件，超长就拆分任务。
- 禁止在小组件里使用 Glance 不支持的 API（Canvas、自定义 Layout、Compose 动画）。
- 禁止在 Glance 小组件**根节点**放 `GlanceModifier.clickable` —— 它会吞掉 launcher 的
  长按菜单，导致小组件无法移除/调整大小。点击行为挂在内层内容区（CHANGELOG #30）。
- 禁止硬编码提示词文本，一律放 `res/raw`。
- 禁止硬编码节次时间（各校不同），一律走节次时间表配置。

## 遇到这些情况必须停下来问

- 需求冲突，或文档没覆盖的交互细节
- 需要新增数据表字段
- 需要申请新的系统权限
- 性能与体验的权衡（例如小组件刷新频率）

## 当前状态

- **M0–M7 已完成并合入 main**：工程骨架、数据层、导入链路、导入 UI、周视图、课程与学期管理、小组件、导出备份
- **M8（P1，提醒通知 + 冲突检测）暂缓**，未开始
- **M9（MVP 后打磨与问题修复）进行中**
  - #10–#29 已合入 main
  - **#30–#31 在分支 `feat/widget-next-lessons` 上待合入**（小组件重构为「最近课程列表」双入口 + 课程边界精确刷新）
- **demo 已发布**：[`v0.1.0-demo`](https://github.com/Leng-CS/fkwakeup/releases/tag/v0.1.0-demo)，附调试卷 APK
- 单元测试 **176 个**，全绿

开始编码前先看 `docs/CHANGELOG.md` 了解最近改了什么，再确认当前该做哪个 Issue。
