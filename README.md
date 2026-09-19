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
| ✅ | 导入建课表（粘贴 / 文件 / 从 AI App 分享进来，三条路径） |
| ✅ | 手动建课表、多学期切换、课程增删改、一门课多个时间段 |
| ✅ | 周视图网格、跨节次课程合并、当前节次与今日列高亮 |
| ✅ | 点击课程块直接编辑（名称 / 教师 / 时间 / 地点 / 周次） |
| ✅ | 自定义课块颜色（12 色预设 + 自动），同步到桌面小组件 |
| ✅ | 桌面小组件（Glance，小 / 中 / 大三种尺寸） |
| ✅ | 导出备份（可重新导入）、导入失败可让 AI 重新生成 |
| ✅ | 节次时间表按学期配置 |
| P1 | 上课前提醒、时间冲突检测 |
| P2 | 节假日调休 |

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
| [`docs/HANDOFF.md`](docs/HANDOFF.md) | 交接文档：项目导航、已定决策、工程环境注意事项。**换人/换模型接手时先读这个** |
| [`docs/CHANGELOG.md`](docs/CHANGELOG.md) | 变更记录：M7 之后的微调、Bug 修复与新增需求 |
| [`AGENTS.md`](AGENTS.md) | 给 AI 编码助手的项目约定，开工前必读 |

## 开发路线

M0 工程骨架 → M1 数据层 → M2 导入解析 → M3 导入 UI → M4 周视图 → M5 课程与学期管理 → M6 小组件 → M7 导出打磨。

**M0–M7 已完成。** M8（提醒通知）暂缓；M9 是 MVP 之后的打磨与问题修复，长期进行中，记录见 `docs/CHANGELOG.md`。

每个里程碑的验收标准见主文档第 9 章。

## 许可

Apache-2.0
