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

- 分支：`feat/course-block-edit-and-color`，commit `6fb9412` → **待合入 main**
- 新增 FR-09（课程块快捷编辑）、FR-10（自定义课块颜色），已写入主文档第 3 章
- 点击课程块弹出底部抽屉：上半课程信息（名称/教师/颜色），下半本节课（周几/节次/周次/地点）+ 删除本节
- 12 色预设色板 + 「自动」；颜色按课程粒度；同步到桌面小组件
- 色板下沉到 `core:common`（纯整数），`ScheduleBlock` 携带解析后色值；`CourseTextColor` 按亮度切黑/白文字
- **过程中修掉**：① 色板扩容导致自动色全部错位（改用固定 `AUTO_COLOR_COUNT` 常量）② 色板一行放不下被截断（改 `FlowRow` 两行）
- 验证：改色后两个时间段同时变色并落库；改回自动后 `color_argb` 回到 null；小组件颜色同步

---

## 模板（下次新增时复制）

```
### #N [类型] 标题

- 分支：`xxx`，commit `xxx` → 状态
- 做了什么（要点列举）
- 验证：结论
```
