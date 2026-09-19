# fkwakeup 项目交接文档

> 面向接手开发的 AI 模型。**读完本文 + `AGENTS.md` + `docs/project-blueprint.md` 第 4、5 章即可开工。**
> 本文只做导航与环境说明，**技术细节一律以 `docs/project-blueprint.md` 为准**，两者冲突时以主文档为准。

---

## 1. 项目是什么

Android 原生课程表 App。核心价值：**把教务系统里那张难用的课表图片，30 秒变成一个可查询、可放桌面小组件的课表。**

主流程：

```
用户在教务系统截图
  → 在 App 内一键复制提示词
  → 连同截图发给任意 AI（ChatGPT / Claude / 豆包 / 通义 / Gemini 均可）
  → AI 输出结构化 JSON
  → 用户复制回复，回 App 粘贴
  → App 自动新建课表，进入预览纠偏页
  → 落库，课表主页可见，桌面小组件可看
```

明确不做：账号体系、云同步、社交、教务系统自动抓取（模拟登录有合规风险）。

---

## 2. 仓库

| 项 | 值 |
|---|---|
| 地址 | https://github.com/Leng-CS/fkwakeup |
| 可见性 | 公开 |
| 默认分支 | `main` |
| 分支保护 | 已开启：禁 force push、禁删除，**且对管理员生效** |
| 内容 | 10 files · 9 Issues · 9 Milestones · 17 Labels |

```
.gitattributes
.gitignore
.github/ISSUE_TEMPLATE/bug_report.md
.github/pull_request_template.md
AGENTS.md                      给 AI 的项目约定，开工前必读
LICENSE                        Apache-2.0
README.md
docs/HANDOFF.md                本文
docs/project-blueprint.md      主开发文档（权威）
docs/recognition-prompt.md     App 内置提示词与 UI 文案（权威）
docs/timetable-sample.json     导入格式示例
```

**Issue #1–#9 对应 Milestone M0–M8**，每个 Issue 里写了目标 / 范围 / 验收标准 / 参考章节。开始某个里程碑前先读对应 Issue。

> 之后的迭代（微调 / Bug / 新需求）归 **M9 里程碑**，见 Issue #10 起。所有变更汇总在 `docs/CHANGELOG.md`。

---

## 3. 必读顺序

1. **`AGENTS.md`** —— 项目约定、目录结构、编码规范、禁止清单
2. **`docs/project-blueprint.md` 第 4 章** —— 数据模型（Kotlin 领域模型 + Room 表结构 + 周次表达式语法）
3. **第 5 章** —— 导入格式 v1.0（JSON Schema、字段表、校验、五级容错流水线）
4. **第 9、10 章** —— 难度评估、M0–M9 里程碑与验收标准、AI 协作与变更流程（10.5 节）
5. `docs/CHANGELOG.md` —— 最近改了什么（微调 / Bug / 需求）
5. **`docs/recognition-prompt.md`** —— App 内置提示词正文 + 全部 UI 文案（`strings.xml` 与 `res/raw` 的唯一来源）

**不要跳过第 2、3 步直接写 UI。** 数据契约一旦改动，UI 全部返工。

---

## 4. 已定决策（不要重新讨论）

| 决策 | 结论 | 原因 |
|---|---|---|
| 技术栈 | 原生 Kotlin + Compose + Material3 + Room + Hilt + Glance | Glance 是官方唯一的 Compose 风格小组件方案 |
| 导入格式 | JSON，格式 `campus-timetable` v1.0 | AI 输出最稳定 |
| **输出结构** | **扁平化：一行 = 一个时间段** | AI 把同一门课的多个时间段正确归并最容易出错；改为 App 侧按 (name+teacher) 归并，用户可在预览页纠正 |
| 回传路径 | 粘贴文本（默认）+ 文件导入（兜底）+ 分享到本 App（加分项） | 手机上让用户把文本存成 `.json` 几乎不可行 |
| 跳转外部 AI | **本期不做** | 各 AI App 对「图+文字」分享处理不一致，深链包名易失效 |
| 导入失败 | **必须做闭环**：可复制「修复版提示词 + 错误列表 + 原文」给 AI 重生成 | 把死胡同变成可恢复流程 |
| 节次时间表 | 每学期独立可配置，**禁止硬编码** | 各校「第 3 节课」时间完全不同 |
| 小组件刷新 | 写库主动 update + WorkManager 15 分钟 + 日期/时间/时区/开机/升级广播 | 单一机制都不够；`ACTION_TIME_TICK` 无法静态注册，不能用 |
| 提醒通知 | P1，不在 MVP 范围 | 国内 ROM 省电策略可靠性差，需白名单引导 |
| 提示词存储 | `res/raw/prompt_import.txt`，禁止硬编码进 Kotlin | 提示词一定会迭代 |
| 数据契约 | 第 4、5 章内容**未经批准不得修改** | 全项目契约 |

---

## 5. 当前进度与下一步

> 本节只记录**接手时该看什么**。每次改动的详情在 `docs/CHANGELOG.md`，按 Issue 号追踪。

### 状态

| 里程碑 | 状态 |
|---|---|
| M0–M7 | ✅ 已完成并合入 main（工程骨架 → 数据层 → 导入链路 → 导入 UI → 周视图 → 课程与学期管理 → 小组件 → 导出备份） |
| M8（P1 提醒通知 + 冲突检测） | ⏸ 暂缓，未开始 |
| M9（MVP 后打磨与问题修复） | 🔄 进行中 |

M9 当前工作项：

- **#10** [微调] 周视图顶栏与课程块信息展示 → 已合入 main
- **#11** [Bug] 切换学期后不刷新 → 已合入 main
- **#12** [需求] 课程块点击编辑 + 自定义颜色 → **待合入 main**

### 接手后先看这三件事

1. `docs/CHANGELOG.md` —— 最近改了什么
2. 对应 Issue 的正文 —— 背景 / 做了什么 / 验证 / 分支与 commit
3. 主文档第 10.5 节 —— 变更流程（建 Issue → 归 Milestone → 分支 → 更新文档 → 记 CHANGELOG）

### 新增需求或改动的流程

一律走五步，详见 `AGENTS.md`「变更流程」与主文档 10.5 节。**不要直接提交到 main。**

---

## 6. 工程环境注意事项

这些是本机实测出来的坑，照做能省很多时间。

### 推送代码

本机 git 走 HTTPS 时**无法使用凭据管理器**（git 调不起 `sh`），必须用 token 内嵌 URL：

```bash
git -c credential.helper= push "https://<token>@github.com/Leng-CS/fkwakeup.git" main
```

`-c credential.helper=` 是关键，用来彻底禁用凭据管理器。

### 分支保护的副作用

`enforce_admins=true`，**force push 会被拦截**（对 owner 也生效）。所以：

- 提交前先确认，避免事后 `--amend` / `rebase`
- 确实需要改历史时，先去 Settings → Branches 关掉保护，改完再开回来

### 中文编码

- `git commit -m "中文"` 是安全的，实测提交信息不会乱码
- 但 PowerShell 用 GBK 解码 git 的 UTF-8 输出，日志里会显示成「鍒濆」之类乱码。**这是显示假象，不是真乱码**。复核用 `[Console]::OutputEncoding = [System.Text.Encoding]::UTF8`，或直接调 GitHub API 读回

### GitHub API 调用

本环境 PowerShell 直连即可，**不需要代理**（git 配了 `127.0.0.1:7897`，但 `Invoke-RestMethod` 不走它）。

---

## 7. 禁止清单（速查）

完整版见 `AGENTS.md`，这里列最容易踩的：

- ❌ 修改第 4、5 章的数据契约而不先询问
- ❌ 引入技术栈之外的第三方库（尤其禁止 Gson / Moshi 替换 kotlinx.serialization）
- ❌ 跳过单元测试
- ❌ 一次性生成超过 300 行的新文件
- ❌ 小组件里用 Canvas / 自定义 Layout / Compose 动画（Glance 不支持）
- ❌ 硬编码提示词文本
- ❌ 硬编码节次时间

必须写单元测试的模块：`WeekSpecParser`、导入 `Extractor` / `Normalizer`、当前周计算、课程归并。

---

## 8. 未决事项

| 项 | 状态 |
|---|---|
| 平板 / 大屏布局适配 | 待定，P1 |
| 学期中途调休的表达方式 | 暂定 P2 用「指定日期按某周课表执行」 |
| GitHub Actions CI | 用户明确暂不需要 |
| 连接器对仓库的写权限 | 当前只读；推代码走 token，不影响开发 |

---

## 9. 一句话给接手的模型

**先把第 4、5 章的数据契约落成代码，再动 UI。** 这个项目的难点不在界面，而在导入解析的鲁棒性和 Glance 小组件的诸多限制。数据契约稳了，后面每一步都能独立验证。
