param([string]$BaseUrl='http://127.0.0.1:8080',[string]$Username='admin',[string]$Password=$env:SMOKE_PASSWORD)
# 本地验收脚本：新增一个时间标记的测试账号，结束后停用保留，不删除业务账号。
$ErrorActionPreference='Stop'
if(-not $Password) { $secret=Read-Host '管理员登录密码' -AsSecureString; $Password=[Net.NetworkCredential]::new('', $secret).Password }
function CallApi($Session,[string]$Method,[string]$Path,$Payload=$null) {
    $args=@{Uri="$BaseUrl$Path";Method=$Method;WebSession=$Session}
    if($Method -ne 'GET') {
        $csrf=(Invoke-RestMethod "$BaseUrl/api/auth/csrf" -WebSession $Session).data
        $args.Headers=@{($csrf.headerName)=$csrf.token}
    }
    if($null -ne $Payload) { $args.ContentType='application/json; charset=utf-8'; $args.Body=[Text.Encoding]::UTF8.GetBytes(($Payload|ConvertTo-Json)) }
    (Invoke-RestMethod @args).data
}
function SignIn([string]$Name,[string]$Value) {
    $session=[Microsoft.PowerShell.Commands.WebRequestSession]::new()
    $csrf=CallApi $session GET '/api/auth/csrf'
    Invoke-RestMethod "$BaseUrl/api/auth/login" -Method Post -WebSession $session -Headers @{($csrf.headerName)=$csrf.token} -Body @{username=$Name;password=$Value} | Out-Null
    return $session
}
function Check($Condition,[string]$Message) { if(-not $Condition) {throw "失败：$Message"}; Write-Output "PASS $Message" }
function Reject([scriptblock]$Action,[int]$Status,[string]$Message) {
    try { & $Action | Out-Null; throw "未拒绝请求：$Message" }
    catch { if([int]$_.Exception.Response.StatusCode -ne $Status) {throw} }
    Write-Output "PASS $Message ($Status)"
}
$admin=SignIn $Username $Password
$testName='verify_'+[DateTime]::Now.ToString('yyyyMMddHHmmss')
$firstPassword='Verify!'+[Guid]::NewGuid().ToString('N').Substring(0,16)
$nextPassword='Reset!'+[Guid]::NewGuid().ToString('N').Substring(0,16)
$item=CallApi $admin POST '/api/admin/accounts' @{username=$testName;displayName='综合验收账号';role='PUBLISHER';enabled=$true;password=$firstPassword}
$id=$item.id
Check ($id -gt 0) '管理员创建账号'
$editor=SignIn $testName $firstPassword
Reject {CallApi $editor GET '/api/admin/accounts'} 403 '发布人员不能管理账号'
Reject {CallApi $admin POST '/api/admin/accounts' @{username=$testName;displayName='重复';role='PUBLISHER';enabled=$true;password=$firstPassword}} 409 '用户名不能重复'
CallApi $admin PUT "/api/admin/accounts/$id" @{displayName='改名后的验收账号';role='PUBLISHER';enabled=$true;password=''} | Out-Null
Check ((CallApi $editor GET '/api/auth/me').user.displayName -eq '改名后的验收账号') '改名不撤销会话且密码保持不变'
CallApi $admin PUT "/api/admin/accounts/$id" @{displayName='验收账号';role='ADMIN';enabled=$true;password=''} | Out-Null
Reject {CallApi $editor GET '/api/manage/news'} 401 '变更角色撤销旧会话'
$promoted=SignIn $testName $firstPassword
Check ((CallApi $promoted GET '/api/admin/accounts').total -gt 0) '重新登录后取得新权限'
CallApi $admin PUT "/api/admin/accounts/$id" @{displayName='验收账号';role='PUBLISHER';enabled=$true;password=$nextPassword} | Out-Null
Reject {CallApi $promoted GET '/api/admin/accounts'} 401 '降权和重设密码撤销管理员旧会话'
Reject {SignIn $testName $firstPassword} 401 '旧密码失效'
$changed=SignIn $testName $nextPassword
Check ((CallApi $changed GET '/api/auth/me').authenticated) '新密码登录成功'
CallApi $admin PUT "/api/admin/accounts/$id" @{displayName='验收账号';role='PUBLISHER';enabled=$false;password=''} | Out-Null
Reject {SignIn $testName $nextPassword} 401 '停用后拒绝登录'
CallApi $admin PUT "/api/admin/accounts/$id" @{displayName='验收账号';role='PUBLISHER';enabled=$true;password=''} | Out-Null
Reject {CallApi $changed GET '/api/manage/news'} 401 '重新启用后不能复用停用前的会话'
$me=CallApi $admin GET '/api/auth/me'
Reject {CallApi $admin PUT "/api/admin/accounts/$($me.user.id)" @{displayName=$me.user.displayName;role='ADMIN';enabled=$false;password=''}} 409 '不能停用当前管理员'
CallApi $admin PUT "/api/admin/accounts/$id" @{displayName='已停用的综合验收账号';role='PUBLISHER';enabled=$false;password=''} | Out-Null
CallApi $admin POST '/api/auth/logout' | Out-Null
Write-Output '账号验收通过；测试账号已停用，随机密码不输出。'
