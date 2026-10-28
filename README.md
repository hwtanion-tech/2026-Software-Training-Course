# 校园新闻及通知发布系统

第二周在第一周骨架上实现了可运行的登录、权限控制、新闻与通知维护、发布撤回、分类管理、公开查询和分页。项目采用 Spring Boot + Vue + MySQL；另提供 H2 本机演示模式，方便先验收页面和业务。

## 快速验收

使用 Java 21、Maven 3.9、Node.js 20.19+ 或 22.12+。沿用仓库锁定的 Spring Boot 3.5.16、Vue 3.5.44、Vite 7.3.7。

第一个 PowerShell 终端启动后端：

```powershell
Set-Location -LiteralPath 'G:\file\grade 4 first semester\PRACTICE\campus-news-system\backend'
mvn spring-boot:run
```

第二个 PowerShell 终端启动前端：

```powershell
Set-Location -LiteralPath 'G:\file\grade 4 first semester\PRACTICE\campus-news-system\frontend'
npm.cmd ci
npm.cmd run dev
```

打开 http://127.0.0.1:5173 。首次下载依赖需要网络；已有服务运行时直接访问，不必重复启动。两个终端分别按 Ctrl+C 停止。前端通过 Vite 代理访问 8080 后端。

默认 `demo` 模式首次创建本机 H2 文件数据库与样例。页面顶部明确提示演示模式；不是 MySQL 连接成功的证明。从 backend 目录启动时，数据保存在 `backend/.local/campus-news-demo.mv.db`，重启后保留，已加入忽略规则。请始终从同一 backend 目录启动，否则相对路径会指向另一份数据。

| 演示账号 | 密码 | 权限 |
| --- | --- | --- |
| admin | DemoAdmin!2026 | 管理全部新闻、通知及分类 |
| editor | DemoEditor!2026 | 只维护自己的新闻与通知 |
| other | DemoOther!2026 | 第二位发布人员，用于验证作者隔离 |

账号仅用于本机课程演示。MySQL 模式不会创建这些账号，默认服务仅监听 127.0.0.1。

## 已实现内容

- 访客：首页、新闻列表与详情、通知列表与详情；标题关键词、新闻分类筛选、分页；仅能读取已发布内容。
- 发布人员：真实会话登录、退出；新增、修改、删除本人新闻与通知；草稿、发布、撤回及重新发布。
- 管理员：管理所有作者内容，新增/修改/停用分类，调整分类排序。
- 后端：BCrypt 密码校验、CSRF、角色与创建者校验、参数校验、参数化 SQL、事务及状态转换。
- 新建内容强制草稿；修改已发布内容自动回草稿；已发布内容必须先撤回再删除。

目前正文为纯文本，未实现图片/附件上传、富文本、注册、找回密码、账号管理页面、审核流和生产部署。这些不应作为已完成功能填写进报告。

## 文件入口

| 位置 | 说明 |
| --- | --- |
| backend/src/main/java/cn/edu/campus/news | Controller、Service、Repository、认证及初始化 |
| frontend/src/components | 首页、列表、详情、登录、内容后台及分类页面 |
| database/schema.sql | MySQL 四表结构，手动导入；沿用第一周结构 |
| scripts/smoke-test.ps1 | 可重复执行的本地管理员接口验收脚本 |
| [第二周系统设计](docs/05-第二周系统设计.md) | 架构、类图、流程、接口契约 |
| [第二周修改与验收指南](docs/06-第二周修改与验收指南.md) | 具体变更、MySQL 配置、页面验收和排错 |
| [第二周验证记录](docs/07-第二周验证记录.md) | 实际测试范围、结果与限制 |
| [Git 提交指南](docs/03-Git建仓与提交指南.md) | 由本人执行的命令与用途 |

## 构建与测试

```powershell
# backend 目录：运行测试并打包
mvn -B package
# frontend 目录：生产构建
npm.cmd run build
```

后端当前有 19 项集成测试，使用独立内存数据库，不修改你的演示数据。MySQL 模式的配置步骤及管理员初始化见验收指南；旧版 `DB_ENABLED` 开关已废弃，现使用 `SPRING_PROFILES_ACTIVE=mysql`。`/api/health/database` 现只允许管理员访问。
