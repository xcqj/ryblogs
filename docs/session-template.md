# 会话上下文模板（Agent 指令书）

> **用途**：本文件是三个仓库（RuoYi-Vue-Plus / plus-ui / blog-web）新会话的**上下文组织规范**。
> 各仓 `AGENTS.md` §10 均有指针指向本文件；新会话中 agent 应按本模板自动补齐上下文，
> 用户通常**只需给任务描述 + 涉及仓库**。
>
> **维护约定**：本文件只放"长期不变"的上下文（红线/环境/协作方式）。任务清单、切片状态等
> 易变内容不在这里，按第 2 节指向对应权威文档。修改本文件 = 改全局会话行为，需小步 commit 并说明。

---

## 0. 用户最小输入（缺了才问，能推断的不问）

agent 只向用户要求三样：

1. **任务描述**——做什么、为什么/期望效果（一句话起，可后续补充）
2. **涉及仓库**——`blog-web` / `plus-ui` / `RuoYi-Vue-Plus` / 多仓分步（跨仓须说明拆分顺序）
3. **性质**——新功能 / Bug 修复 / 样式质感 / 文档 / 重构（从任务描述推断不出再问）

其余一切（必读清单、红线、环境事实、Git 基线、协作方式）由 agent 按本模板补齐。
**信息不足以定位修改目标时，先澄清再动手，不要臆测。**

## 1. 动手第一步：基线自查

- `git log --oneline -1` + `git status --short` 确认涉及仓库 HEAD 与工作区状态（**不要假设基线**，以实测为准）
- 三仓布局（同级 clone，多根工作区）：`RuoYi-Vue-Plus`（后端）、`plus-ui`（管理后台）、`blog-web`（Nuxt 4 前台）
- **同一开发切片内可多仓同会话修改**，但必须每仓独立 commit、独立可回滚（D45 修订 D32）；跨切片禁止混做，两个 Agent 不得同时改同一工作区

## 2. 必读清单（按任务性质选读，不相关的行删掉）

**通用（任何任务）**：

- 涉及仓库的 `AGENTS.md`（红线与工作流权威；与本文件冲突时以 AGENTS.md 为准）

**按性质追加**：

| 任务性质 | 追加必读 |
|---|---|
| 设计/样式质感（blog-web） | `blog-web/.impeccable.md`（设计上下文单一来源）+ `use_skill` 加载 `design-taste-frontend`（taste-skill v2；本机注册名，目录名 taste-skill，按注册名调用） |
| blog-web 页面/SSR/Nitro/样式 | `blog-web/.codex/skills/nuxt-frontend-coding/SKILL.md`（先读其 references/nuxt.md） |
| plus-ui 后台 CRUD | `plus-ui/.codex/skills/frontend-crud-coding/SKILL.md` |
| 后端业务/接口 | `RuoYi-Vue-Plus/.codex/skills/ruoyi-plus-ai-coding/SKILL.md` + `references/backend.md` |

**按任务对象追加**：

- 涉及接口：`RuoYi-Vue-Plus/docs/blog-api.md`（契约权威；实现与契约冲突时先报告再修订，不得默默偏离）
- 排期/切片类：`RuoYi-Vue-Plus/docs/ROADMAP.md`（切片状态与完成标准权威；决策记录看 §6）
- blog-web 页面/组件粒度：`blog-web/docs/plan.md` 对应小节
- plus-ui 页面/组件粒度：`plus-ui/docs/frontend-plan.md` 对应小节（派生自 ROADMAP S10，状态以 ROADMAP 为准）
- 部署类：`RuoYi-Vue-Plus/docs/cloud-deployment.md`
- 目标目录**最近似的真实代码**（同模块/同目录优先；信息源优先级：仓内真实代码 → 权威文档 → skill 资产 → 通用框架知识，禁止照搬通用模板）

## 3. 项目红线（任何任务生效）

- **不信任前端**：`authorId`、权限字段、关键状态字段一律后端强制校验；隔离靠服务端过滤而非前端隐藏按钮；公共接口不得泄露草稿 / 用户隐藏 / 管理员下线 / 逻辑删除数据
- **API 三层边界不得混用**：`/blog/public/**`（匿名）、`/blog/article|user/**`（登录作者/读者）、`/blog/admin/**`（仅 plus-ui 管理后台可调；**blog-web 禁止调用**，也不得给前台分配治理入口）
- **token 纪律**：业务代码不手写 `Authorization` / `clientid` 头；blog-web 走 httpOnly cookie + Nitro server route 代理；token 不落 localStorage；不提交 `.env`（只提交 `.env.example`）
- **SSR 安全（blog-web）**：组件渲染期不得裸用 `window` / `document` / `localStorage` / `navigator`；用 `import.meta.client`、`<ClientOnly>` 或客户端 composable 守卫
- **表现层纪律**：视觉量走 token 体系不写魔法数字（tokens.css 单一来源）；blog-web 无组件库（薄自研组件 + UnoCSS + prose）；**不新增运行时依赖，确需新增先问用户**
- **D38 有效**：不引 three.js / GSAP / 视差重特效；质感来自细节（缓动/留白/字号落差/微交互/排版精度），不为"显得做了事"加戏
- **发现冲突先报告**：设计与现有代码冲突、文档之间冲突时，先明确指出，由最新明确决定裁决，不默默改设计
- **部署链不编译**：CI 产不可变镜像 → 部署机只 pull；部署机不跑 `mvn` / `npm` / `docker build`、不持源码（S29.1，见 AGENTS.md §8）
- **红线人工复核**：关键权限/删除逻辑改完后列清单给用户复核

## 4. 环境事实（本机，长期有效）

- **构建目录纪律**：只在对应仓库目录内执行构建（后端 `mvn`/`./mvnw`，前端 `npm`/`pnpm`），**父目录禁跑任何构建**（会落游离 `target/`/`node_modules` 噪音）
- blog-web 仅有 `build/dev/generate/preview/start` 脚本，**无 typecheck/lint 脚本**——验证以 `npm run build` 为准，勿臆造命令
- **本地服务常驻**：后端 `8081`（java）、blog-web dev `3000`；容器 `blog-mysql` / `blog-redis` / `blog-minio` / `blog-es`；起生产预览用 `NITRO_PORT=3100 node .output/server/index.mjs`（3000 被 dev 占用时）
- **本机 WSL 无 CJK 字体**：截图中文方框属环境问题非页面问题；视觉验收以用户 Windows 侧浏览器为准；agent 侧自动化只做冒烟（页面加载 / 控制台无错 / 构建通过）
- **blog-web 截图/冒烟工具**：`blog-web/tooling/shot/`（playwright，`.libs` + `LD_LIBRARY_PATH` + `.fonts` fontconfig 兜底；`shot.mjs` 出现 console error / 5xx 即退出码 1，可当回归门禁）
- **已知截图伪影（勿当缺陷修）**：fullPage 截图中视口外 `v-reveal` 元素不显现（如 CTA band"消失"）；`background-attachment: fixed` 长页拼接出现色带
- **后端 mapstruct-plus** 转换器编译期生成，运行时报 `ConvertException: cannot find converter` 先 `mvn clean` 全量重编再排查源码
- **blog-web Nuxt 4.6 两个已实证的 transform 坑**：① `app/directives/` 不会被自动注册，需 `app/plugins/*.ts` 显式挂载；② plugins 下 bare 包的静态默认导入会被 unimport 剥离（运行时报 not defined 白屏），用动态 `import()`

## 5. 协作方式（用户既定偏好）

- **动手前先澄清范围并给实施计划，确认后再执行**；不要默默改设计或偏离契约
- 改代码前建立 Git checkpoint；**小步 commit**（信息带切片号或性质），一个切片一个闭环：实现 → 构建/自测 → 冒烟 → Review
- **人工事项列清单给用户，不自动代跑**（浏览器视觉验收、Apifox 补录、跨仓文档状态更新、生产验证等）
- 交付时说明**实际运行过**哪些验证命令及结果；无法验证的说明原因，不谎报
- **完成本任务后不要自动进入下一个切片**；用户说"继续"再开下一刀
- 用户无 UI/UX 设计经验：样式问题由 agent 按专业标准评审驱动，产出具体清单让用户做选择题，不让用户用设计词汇描述需求

## 6. 收尾检查清单

- [ ] 构建/自测通过（命令与结果写进交付说明）
- [ ] 冒烟通过（涉及页面：加载 / 控制台 / 5xx / 深浅主题 / 移动视口，按任务范围裁剪）
- [ ] 文档同步：涉及切片状态的更新 ROADMAP；blog-web 页面粒度更新 `docs/plan.md`；plus-ui 页面粒度更新 `docs/frontend-plan.md`；跨仓更新列人工事项
- [ ] commit 信息含切片号/性质，工作区干净
- [ ] 人工事项清单交付给用户（含验收方式）
