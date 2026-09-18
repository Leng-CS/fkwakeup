# fkwakeup

Android 课程表 App。把教务系统里那张难用的课表图片，30 秒变成一个可查询、可放桌面小组件的课表。

## 它是怎么工作的

```
教务系统截图 → App 内一键复制提示词 → 交给任意 AI → 复制 AI 回复 → 回 App 粘贴
    → 自动新建课表 → 预览纠偏 → 落库 → 桌面小组件随时可看
```

用户不需要手动录入，也不需要把文本存成文件。整个链路里 App 会自动剥离 AI 输出的代码块围栏与前后正文，只取 JSON 部分。

## 功能

| 状态 | 功能 |
|---|---|
| 规划中 | 导入建课表（粘贴 / 文件 / 从 AI App 分享进来，三条路径） |
| 规划中 | 手动建课表、多学期切换、课程增删改、一门课多个时间段 |
| 规划中 | 周视图网格、跨节次课程合并、当前节次高亮 |
| 规划中 | 桌面小组件（Glance，小 / 中 / 大三种尺寸） |
| 规划中 | 导出备份、导入失败可让 AI 重新生成 |
| P1 | 上课前提醒、时间冲突检测 |
| P2 | 节假日调休、课程颜色与笔记 |

## 技术栈

原生 Android，离线优先，无账号体系、无云同步、不抓取教务系统。

- Kotlin + Jetpack Compose + Material3
- Room / DataStore / kotlinx.serialization
- Hilt + Coroutines + Flow
- **Glance**（`androidx.glance:glance-appwidget`）—— 桌面小组件
- WorkManager —— 小组件刷新调度

## 文档

| 文档 | 内容 |
|---|---|
| [`docs/project-blueprint.md`](docs/project-blueprint.md) | **主文档**。数据模型、导入格式规范、实现方案、难度评估、里程碑 |
| [`docs/recognition-prompt.md`](docs/recognition-prompt.md) | App 内置提示词与全部 UI 文案，是 `strings.xml` 与 `res/raw` 的唯一来源 |
| [`docs/timetable-sample.json`](docs/timetable-sample.json) | 导入格式示例，覆盖单双周、跳周、跨节次、空字段 |
| [`AGENTS.md`](AGENTS.md) | 给 AI 编码助手的项目约定，开工前必读 |

## 开发路线

M0 工程骨架 → M1 数据层 → M2 导入解析 → M3 导入 UI → M4 周视图 → M5 课程与学期管理 → M6 小组件 → M7 导出打磨。

每个里程碑的验收标准见主文档第 9 章。

## 许可

MIT
