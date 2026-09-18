# AGENTS.md

给 AI 编码助手的项目约定。**开工前必须完整读完本文件与 `docs/project-blueprint.md`。**

> 如果是接手已有仓库、需要了解环境与工程约定，先读 `docs/HANDOFF.md`。
> 本文专注编码约定。

## 一句话项目

Android 原生课程表 App：用户把教务系统课表截图交给 AI，AI 输出结构化 JSON，App 导入即建课表，并支持桌面小组件。

## 开工前必读（按顺序）

1. `docs/project-blueprint.md` **第 4 章** —— 数据模型。这是全部代码的契约。
2. `docs/project-blueprint.md` **第 5 章** —— 导入格式 v1.0。这是与外部 AI 的接口契约。
3. `docs/project-blueprint.md` **第 9、10 章** —— 里程碑与验收标准。确认当前处于哪个 M。
4. `docs/recognition-prompt.md` —— UI 文案与 `res/raw` 提示词内容的唯一来源。

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
core/common/            WeekSpecParser、时间工具
core/database/          Room entities / dao
core/importer/          ★ 导入解析：Extractor / Normalizer / Validator
core/exporter/
feature/schedule/       周视图主页
feature/course/         课程详情与编辑
feature/importexport/   导入页 + 预览纠偏页 + 导出
feature/settings/       学期管理 / 节次时间表 / 设置
widget/glance/          Glance 小组件 + 更新调度
```

## 编码规范

- 单向数据流：`StateFlow` + `UiState` 密封类，Composable 不直接访问数据库。
- Composable 命名用名词：`ScheduleScreen`、`CourseCard`，不写 `DrawCourse`。
- 提交信息：`feat(schedule): 支持跨节次课程合并渲染`。
- **每完成一个逻辑单元必须补单元测试**，无测试视为未完成。必测项见主文档 6.5 节。
- 单个任务只改一个文件（或一个文件内的一组相邻函数），产出 diff 化改动。

## 明令禁止

- 禁止修改第 4、5 章定义的数据契约而不先询问。
- 禁止引入技术栈之外的依赖。
- 禁止跳过单元测试。
- 禁止一次性生成超过 300 行的新文件，超长就拆分任务。
- 禁止在小组件里使用 Glance 不支持的 API（Canvas、自定义 Layout、Compose 动画）。
- 禁止硬编码提示词文本，一律放 `res/raw`。
- 禁止硬编码节次时间（各校不同），一律走节次时间表配置。

## 遇到这些情况必须停下来问

- 需求冲突，或文档没覆盖的交互细节
- 需要新增数据表字段
- 需要申请新的系统权限
- 性能与体验的权衡（例如小组件刷新频率）

## 当前状态

工程尚未开始，处于 **M0：工程骨架**。开始编码前先确认当前里程碑。
