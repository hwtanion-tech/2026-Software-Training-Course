# 校园新闻及通知发布系统

本阶段完成需求分析与开发环境准备。前端是第一周开发工作台，后端提供运行状态、MySQL连接检查和项目信息接口。登录、内容管理、发布撤回、分类查询和分页尚未实现，页面明确列为后续开发。

## 1 项目结构

```text
campus-news-system/
├── backend/                  Spring Boot后端与基础接口测试
├── frontend/                 Vue开发工作台及Vite代理
├── database/
│   ├── schema.sql            MySQL表结构草案，手动导入
│   └── samples.json          需求分析样例，不自动导入
├── docs/
│   ├── 01-需求分析与核心用例.md
│   ├── 02-数据清单与状态规则.md
│   ├── 03-Git建仓与提交指南.md
│   └── 04-第一周验证记录.md
├── .gitignore
├── .gitattributes
└── .editorconfig
```

## 2 开发环境

使用Java 21、Maven 3.9、Node.js 20.19+或22.12+及MySQL 8.0.16+。本机检测到Java 21、Maven 3.9.16、Node.js 24.11.1及MySQL 8.0.33客户端。依赖锁定Spring Boot 3.5.16、Vue 3.5.44、Vite 7.3.7及Vue插件6.0.10；前端锁文件随仓库提交。

官方参考：[Spring Boot环境要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)、[Vue快速入门](https://vuejs.org/guide/quick-start.html)、[Vite 7发布说明与Node要求](https://vite.dev/blog/announcing-vite7)。本工程固定使用Vite 7，应以其引擎要求为准。

## 3 不配置数据库也能启动

打开第一个PowerShell终端：

```powershell
Set-Location -LiteralPath 'G:\file\grade 4 first semester\PRACTICE\campus-news-system\backend'
mvn spring-boot:run
```

后端默认只监听127.0.0.1:8080。打开第二个PowerShell终端：

```powershell
Set-Location -LiteralPath 'G:\file\grade 4 first semester\PRACTICE\campus-news-system\frontend'
npm.cmd ci
npm.cmd run dev
```

浏览器访问<http://127.0.0.1:5173>。前端通过Vite的/api代理访问8080端口；使用npm.cmd可避免PowerShell对npm.ps1的执行策略限制。初次安装和Maven构建需要网络下载依赖。终端按Ctrl+C停止对应服务。

未设置数据库时，后端正常启动，页面明确显示数据库“尚未配置连接”，不会伪报连接成功。数据库状态独立于后端服务状态。

## 4 手动初始化并验证MySQL

项目不会自动连接、建库或导入SQL。请先确认本机MySQL管理员账号，再在PowerShell执行：

```powershell
Set-Location -LiteralPath 'G:\file\grade 4 first semester\PRACTICE\campus-news-system\database'
mysql -u root -p --default-character-set=utf8mb4
```

输入你自己的MySQL密码，在MySQL命令行内运行：

```sql
SOURCE schema.sql;
SHOW TABLES FROM campus_news;
```

schema.sql创建campus_news库和四张表，不插入账号、不删除现有表，也不会迁移已存在的表结构。

在MySQL中创建本项目的专用账号（将占位密码换成你自己的本地密码，不要把真实密码保存到代码）：

```sql
CREATE USER 'campus_app'@'127.0.0.1' IDENTIFIED BY '请替换为你的本地密码';
GRANT SELECT, INSERT, UPDATE, DELETE ON campus_news.* TO 'campus_app'@'127.0.0.1';
```

若账号已存在，先核对已有配置，不要盲目重复CREATE USER。然后停止后端，在后端PowerShell终端设置以下环境变量：

```powershell
$env:DB_ENABLED = 'true'
$env:DB_USERNAME = 'campus_app'
$secret = Read-Host '请输入数据库密码' -AsSecureString
$credential = [System.Net.NetworkCredential]::new('', $secret)
$env:DB_PASSWORD = $credential.Password
Remove-Variable secret, credential
mvn spring-boot:run
```

Read-Host避免把明文密码写入命令历史；应用通过当前终端进程环境读取密码。若不是默认端口或库名，同时设置DB_URL。检查完成并停止后端后，可清理当前终端变量：

```powershell
Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
Remove-Item Env:DB_USERNAME -ErrorAction SilentlyContinue
Remove-Item Env:DB_ENABLED -ErrorAction SilentlyContinue
```

浏览器点击“重新检查”，或在第三个终端执行：

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/health/database
```

返回UP表示建立连接并成功执行SELECT 1；它不等于已验证表结构或完成业务功能。未启用返回NOT_CONFIGURED，连接失败返回HTTP 503及DOWN。默认仅本机访问，后续部署前应把诊断接口限制为管理员可用。

## 5 本周接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | /api/health | 检查后端是否运行 |
| GET | /api/project | 返回需求阶段的角色、交付范围和待开发清单 |
| GET | /api/health/database | 可选MySQL只读连接检查 |

## 6 构建与验证

后端目录运行：

```powershell
mvn test
mvn package
java -jar target/campus-news-backend-0.1.0-SNAPSHOT.jar
```

前端目录运行：

```powershell
npm.cmd ci
npm.cmd run build
npm.cmd run preview
```

preview默认访问<http://127.0.0.1:4173>，仍需后端在8080运行。若修改SERVER_PORT，需同步修改frontend/vite.config.js中的两个代理目标。

本周自动化测试覆盖无数据库启动、未配置数据库的真实状态、计划功能标记及数据库异常信息屏蔽。实际验证结果见[第一周验证记录](docs/04-第一周验证记录.md)。

## 7 手动建立Git仓库

本目录尚未初始化Git。按[Git建仓与提交指南](docs/03-Git建仓与提交指南.md)自行初始化、检查、提交及可选推送，指南逐条解释命令功能。

## 8 AI协作与后续工作

本阶段使用Codex辅助生成项目骨架、需求文档和检查代码；需本人阅读、运行和理解后再提交。后续开发中的AI使用情况应继续按实际情况记录。

下一周完善架构、数据库、接口和页面设计，再实现登录、权限和新闻通知管理。不能把本周的角色需求展示页面当作已经完成的登录鉴权或新闻发布系统。
