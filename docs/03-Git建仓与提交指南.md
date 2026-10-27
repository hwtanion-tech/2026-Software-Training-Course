# Git建仓与提交指南

已准备项目目录、源代码、文档、.gitignore和.gitattributes。未执行任何Git指令，未生成.git，未创建远程仓库。以下命令全部由你在PowerShell中手动执行。先完成本地提交即可；GitHub/Gitee推送是可选步骤。

## 1 进入项目目录

```powershell
Set-Location -LiteralPath 'G:\file\grade 4 first semester\PRACTICE\campus-news-system'
```

Set-Location是PowerShell切换目录命令。后续所有Git命令都在此目录执行，避免把整个实训文件夹及无关文档纳入仓库。

## 2 初始化仓库

```powershell
git --version
git init -b main
git status
```

| 命令 | 功能 |
| --- | --- |
| git --version | 检查Git是否安装并显示版本 |
| git init -b main | 在当前目录创建.git仓库数据，初始分支名为main；只执行一次 |
| git status | 查看所在分支、未跟踪文件和暂存情况，不会修改代码 |

## 3 设置本仓库的提交者信息

如果没有设置过身份，按下例执行。邮箱请替换成你自己的邮箱或GitHub提供的隐私邮箱，不要照抄占位文字。

```powershell
git config user.name '韩文涛'
git config user.email '请替换成你的邮箱'
```

这两条命令只配置当前仓库的提交者身份，不包含--global，不改变其他仓库。它们不负责登录GitHub/Gitee。

## 4 检查并提交第一周代码

在运行下列命令前，确认数据库密码没有写入源码、SQL、README或截图。项目默认从环境变量取密码；.gitignore会排除本地.env及application-local配置文件，但不会自动识别其他文件中的密码。

```powershell
git status --short
git add .
git diff --cached --stat
git diff --cached
git commit -m 'feat: 完成第一周项目骨架与需求分析'
git status
git log --oneline -5
```

| 命令 | 功能 |
| --- | --- |
| git status --short | 简洁显示新增、修改和未跟踪文件 |
| git add . | 把当前目录中未被忽略的新增、修改和删除内容放入暂存区，不会提交或上传 |
| git diff --cached --stat | 汇总将要提交的文件及行数变化 |
| git diff --cached | 查看暂存区的实际改动，确认内容后再提交 |
| git commit -m '…' | 将暂存区保存为本地历史中的一个提交，-m指定说明 |
| git status | 提交后检查工作区是否干净 |
| git log --oneline -5 | 查看最近5次提交的简洁记录 |

首次提交建议包含backend源码、frontend源码和package-lock.json、database、docs、README及根目录配置文件。不要提交node_modules、target、dist、真实密码或.local验证日志。

## 5 可选 上传到GitHub或Gitee

先在网站上手动创建空仓库，名称建议campus-news-system。不要勾选自动生成README、.gitignore或License，以便与本地首次提交衔接。复制自己的HTTPS或SSH仓库地址，再执行：

```powershell
git remote add origin '请替换成你的实际仓库地址'
git remote -v
git push -u origin main
```

| 命令 | 功能 |
| --- | --- |
| git remote add origin '…' | 给远程仓库地址设置名称origin，不会立即上传 |
| git remote -v | 检查已配置的拉取和推送地址 |
| git push -u origin main | 将本地main分支提交上传，并设置上游跟踪关系；可能要求你登录 |

不要把令牌或密码直接写在远程URL中。如果origin已经存在，先用git remote -v确认，确需修改地址时使用git remote set-url origin '实际仓库地址'，无需重复add。

## 6 第二周以后的日常提交

```powershell
git status
git diff
git add .
git diff --cached
git commit -m 'feat: 完成系统设计与数据库设计'
git push
```

git diff查看尚未暂存的修改；git push在设置上游后把新的本地提交上传。如果尚未配置远程，跳过git push。发生推送冲突时先查看远程与本地差异，不要直接强制推送。
