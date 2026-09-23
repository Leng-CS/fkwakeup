# fkwakeup

**把一张教务课表截图，变成随手可查的课程表。** fkwakeup 是一款轻松、柔和风格的 Android 课程表 App：借助你常用的 AI 识别课表，再由 App 导入、校对和管理课程，也可以手动创建课表。

## 主要功能

- **AI 辅助导入**：复制 App 提供的提示词，把教务课表截图交给 ChatGPT、Claude、豆包、通义或其他 AI，再粘贴识别结果。导入器支持线下课、固定节次的直播网课，以及按日期开放的异步网课；不完整信息会留在预览页供你补全。
- **周视图与课程管理**：查看课程、周次、节次和教室，编辑课程与多个上课时间段，切换学期，并配置各节课的时间。
- **桌面小组件**：提供 `4×1`、`4×2`、`4×4` 三种显示模式，可纵向浏览课程，并为每个组件单独设置范围、标记样式和背景。
- **课程提醒与冲突检测**：配置课程提醒和单次课程例外；保存有时间重叠的课程前会提示冲突。
- **离线数据与备份**：课表保存在设备本地，可导出备份并重新导入。

## 获取应用

从 [GitHub Releases](https://github.com/Leng-CS/fkwakeup/releases/latest) 下载最新 APK。首个正式版本为 **v1.0.0**，支持 Android 8.0（API 26）及以上版本。

## 从截图到课表

1. 在教务系统截取完整课表，尽量包含星期、节次和周视图之外的网课区域。
2. 在 App 导入页复制识别提示词，连同截图发给你常用的 AI。
3. 将 AI 返回的 JSON 整段粘贴回 App。
4. 在预览页核对学期、课程、时间段和网课安排，补齐提示的字段后确认导入。

AI 只负责识别截图；课程数据由 App 在本地解析和保存。应用不要求注册账号，不提供云同步，也不会登录或抓取教务系统。

## 开发

项目使用 Kotlin、Jetpack Compose、Material 3、Room、DataStore、kotlinx.serialization、Hilt、Glance 和 WorkManager。

在 Windows 上运行单元测试并构建调试 APK：

```powershell
.\gradlew.bat testDebugUnitTest :app:assembleDebug
```

APK 输出在 `app/build/outputs/apk/debug/app-debug.apk`。更多开发约定、数据模型和导入 JSON 格式见 [项目开发文档](docs/project-blueprint.md)；AI 提示词的维护来源见 [识别提示词文档](docs/recognition-prompt.md)。

## 文档

- [项目开发文档](docs/project-blueprint.md)：功能需求、数据模型、导入格式与验收标准
- [课表识别提示词](docs/recognition-prompt.md)：App 使用的提示词与导入文案
- [导入格式示例](docs/timetable-sample.json)：`campus-timetable` JSON 示例
- [交接文档](docs/HANDOFF.md)：项目结构与工程注意事项
- [变更记录](docs/CHANGELOG.md)：版本与迭代记录

## 许可

[Apache License 2.0](LICENSE)
