param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [string]$Username = 'admin',
    [string]$Password = $env:SMOKE_PASSWORD
)
# 仅对本地课程验收数据库运行：会新增一个验收分类，创建并清理自己的测试新闻、通知。
$ErrorActionPreference = 'Stop'
if (-not $Password) {
    $inputSecret = Read-Host '请输入管理员登录密码' -AsSecureString
    $Password = [System.Net.NetworkCredential]::new('', $inputSecret).Password
}
$session = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
function Request([string]$Method, [string]$Path, $Payload = $null) {
    $args = @{ Uri = "$BaseUrl$Path"; Method = $Method; WebSession = $session }
    if ($Method -ne 'GET') {
        $csrf = (Invoke-RestMethod -Uri "$BaseUrl/api/auth/csrf" -WebSession $session).data
        $args.Headers = @{ ($csrf.headerName) = $csrf.token }
    }
    if ($null -ne $Payload) {
        $args.ContentType = 'application/json; charset=utf-8'
        $args.Body = [Text.Encoding]::UTF8.GetBytes(($Payload | ConvertTo-Json -Depth 5))
    }
    return (Invoke-RestMethod @args).data
}
function Assert($Condition, [string]$Message) {
    if (-not $Condition) { throw "验收失败：$Message" }
    Write-Output "PASS $Message"
}
function AssertHidden([string]$Path) {
    try { Request GET $Path | Out-Null; throw '公开接口泄漏了未发布内容' }
    catch { if ([int]$_.Exception.Response.StatusCode -ne 404) { throw } }
    Write-Output 'PASS 未发布内容返回404'
}
$csrf = Request GET '/api/auth/csrf'
Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -WebSession $session `
    -Headers @{ ($csrf.headerName) = $csrf.token } -Body @{ username=$Username; password=$Password } | Out-Null
$me = Request GET '/api/auth/me'
Assert ($me.authenticated -and $me.user.role -eq 'ADMIN') '管理员真实会话登录'
$db = Request GET '/api/health/database'
Assert ($db.status -eq 'UP') "数据库连接正常（$($db.mode)）"
$tag = '验收-' + [DateTime]::Now.ToString('yyyyMMddHHmmss')
$categories = Request POST '/api/admin/categories' @{ name=$tag; sortOrder=99; enabled=$true }
$category = $categories | Where-Object name -eq $tag
Assert ($null -ne $category) '创建分类'
foreach ($kind in @('news','notices')) {
    $payload = @{ title="$tag-$kind"; body='中文正文：验证持久化、发布和撤回。'; summary='验收摘要'; categoryId=$category.id; authorName='验收编辑'; publisherUnit='实训中心' }
    $item = Request POST "/api/manage/$kind" $payload
    $id = $item.id
    Assert ($item.status -eq 'DRAFT') "$kind 新建为草稿"
    AssertHidden "/api/public/$kind/$id"
    $payload.body = '修改后的中文正文。'
    $updated = Request PUT "/api/manage/$kind/$id" $payload
    Assert ($updated.body -eq $payload.body) "$kind 编辑持久化"
    Request POST "/api/manage/$kind/$id/publish" | Out-Null
    $public = Request GET "/api/public/$kind/$id"
    Assert ($public.status -eq 'PUBLISHED' -and $public.body -eq $payload.body) "$kind 发布后公开可见"
    $encoded = [Uri]::EscapeDataString($tag)
    $list = Request GET "/api/public/${kind}?keyword=$encoded&page=1&size=1"
    Assert ($list.total -eq 1 -and $list.items.Count -eq 1) "$kind 关键词分页"
    Request POST "/api/manage/$kind/$id/withdraw" | Out-Null
    AssertHidden "/api/public/$kind/$id"
    Request DELETE "/api/manage/$kind/$id" | Out-Null
    Write-Output "PASS $kind 清理本脚本创建的内容"
}
Request PUT "/api/admin/categories/$($category.id)" @{name=$tag;sortOrder=99;enabled=$false} | Out-Null
Request POST '/api/auth/logout' | Out-Null
Assert (-not (Request GET '/api/auth/me').authenticated) '退出后会话失效'
Write-Output '验收通过；验收分类已停用保留。'
