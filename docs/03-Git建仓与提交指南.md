# Git 建仓与第二周提交指南

当前目录已有 `.git`，本地 main 与本次读取的远程 main 均以 `e084f00ea2a3856303f7cc8902776b31873b1dfc` 为基线。以下命令由你本人执行。本轮没有运行 Git 命令、创建提交或推送远程。不要再次初始化仓库。

## 检查并提交第二周代码

在 PowerShell 中逐条执行，先看输出再进行下一步：

```powershell
Set-Location -LiteralPath 'G:\file\grade 4 first semester\PRACTICE\campus-news-system'
git status
git diff --stat
git diff
```

| 命令 | 功能 |
| --- | --- |
| Set-Location | 切换到项目根目录；这是 PowerShell 命令 |
| git status | 查看分支、已修改和未跟踪文件；新文件不会出现在普通 diff 中 |
| git diff --stat | 汇总已跟踪文件的改动量 |
| git diff | 查看尚未暂存的具体代码变化 |

完成 README 中的构建测试并按验收指南确认功能后：

```powershell
git add .gitignore README.md backend frontend database docs scripts
git diff --cached --stat
git diff --cached
git status
git commit -m "feat: implement week 2 campus news workflows"
git push origin main
```

| 命令 | 功能 |
| --- | --- |
| git add ... | 把列出的源码、文档和截图加入暂存区；忽略规则排除本地库、依赖、日志与构建目录 |
| git diff --cached --stat | 查看本次准备提交的文件范围 |
| git diff --cached | 检查暂存区的实际内容，包括本周新增文件 |
| git status | 提交前再次确认分支和文件，特别注意没有密码、.local、target、node_modules |
| git commit -m ... | 在本地创建第二周提交，此时尚未上传 |
| git push origin main | 将本地 main 提交上传已配置的 origin；先确认 status 显示你位于 main |

如暂存了不想提交的文件，可执行 `git restore --staged -- 文件路径`，它只取消暂存，不删除工作文件。这里没有让你执行会丢弃源码的 restore 或 reset 命令。

如果推送提示远程已有更新，先执行 `git fetch origin` 查看远程状态，再执行 `git log --oneline --left-right main...origin/main` 比较双方提交。确认并处理冲突后再推送，不要强制推送。不要把占位密码写入配置文件或 commit 信息。

## 已有项目的更新原则

第一周的 `git init`、首次 `remote add` 已完成，本周通常只需检查、暂存、提交和推送。需要核对远程时由你执行 `git remote -v`；预期 origin 指向 `https://github.com/hwtanion-tech/2026-Software-Training-Course`。

## 第三周提交

第三周新增账号管理、会话撤销、综合测试及验收材料。沿用上文检查和暂存步骤，本周提交说明可用：

```powershell
git commit -m "feat: complete week 3 account management and acceptance"
git push origin main
```

项目外的成果文件包含Word报告、验收指南和源码压缩包，按课程要求单独提交；需要加入仓库时由本人选择。此次没有代为执行任何Git指令。
