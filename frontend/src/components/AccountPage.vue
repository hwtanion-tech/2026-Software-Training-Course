<script setup>
import { onMounted, reactive, ref } from 'vue'
import { request, logout } from '../api'
import { user, go } from '../state'
const result = ref({ items: [], total: 0, pages: 0 }), page = ref(1), keyword = ref('')
const busy = ref(false), loading = ref(false), error = ref(''), message = ref(''), id = ref(null)
const form = reactive({ username: '', displayName: '', role: 'PUBLISHER', enabled: true, password: '' })
function reset() { id.value = null; Object.assign(form, { username: '', displayName: '', role: 'PUBLISHER', enabled: true, password: '' }) }
function edit(item) { id.value = item.id; Object.assign(form, item, { password: '' }); error.value = ''; message.value = '' }
async function load(resetPage = false) {
  if (resetPage) page.value = 1
  loading.value = true; error.value = ''
  try { result.value = await request(`/api/admin/accounts?${new URLSearchParams({ keyword: keyword.value, page: page.value, size: 8 })}`) }
  catch (e) { error.value = e.message } finally { loading.value = false }
}
async function save() {
  busy.value = true; error.value = ''; message.value = ''
  const self = id.value === user.value?.id, changedPassword = !!form.password
  try {
    await request(`/api/admin/accounts${id.value ? `/${id.value}` : ''}`, { method: id.value ? 'PUT' : 'POST', body: { ...form } })
    if (self && changedPassword) {
      try { await logout() } catch { /* 后端已撤销当前会话，转到登录页。 */ }
      user.value = null; go('/login'); return
    }
    if (self) user.value.displayName = form.displayName.trim()
    reset(); await load(true); message.value = '账号已保存。启停、角色或密码变更后，该账号需重新登录。'
  } catch (e) { error.value = e.message } finally { busy.value = false; form.password = '' }
}
async function turn(delta) { page.value += delta; await load() }
onMounted(() => load())
</script>
<template>
  <section class="page-head compact"><p class="eyebrow">ACCOUNTS</p><h1>账号管理</h1><p>管理后台账号与使用权限。账号保留历史内容关联，可停用。</p></section>
  <nav class="admin-tabs"><a href="#/manage/news">新闻管理</a><a href="#/manage/notices">通知管理</a><a href="#/categories">分类管理</a><a class="active" href="#/accounts">账号管理</a></nav>
  <p v-if="error" role="alert" class="error">{{ error }}</p><p v-if="message" role="status" class="success">{{ message }}</p>
  <form class="editor-form" @submit.prevent="save"><div class="section-heading"><h2>{{ id ? '编辑账号' : '新增账号' }}</h2><button v-if="id" type="button" :disabled="busy" @click="reset">取消编辑</button></div>
    <div class="form-grid"><label>账号<input v-model="form.username" :disabled="!!id || busy" required pattern="[A-Za-z0-9_]{3,50}" maxlength="50" autocomplete="off" placeholder="3至50位字母数字或下划线" /></label><label>显示名称<input v-model="form.displayName" :disabled="busy" required maxlength="50" /></label></div>
    <div class="form-grid"><label>角色<select v-model="form.role" :disabled="busy || id === user?.id"><option value="PUBLISHER">发布人员</option><option value="ADMIN">管理员</option></select></label><label>{{ id ? '重设密码' : '初始密码' }}<input v-model="form.password" :disabled="busy" type="password" :required="!id" minlength="12" maxlength="64" autocomplete="new-password" :placeholder="id ? '留空保留原密码' : '请输入12至64位密码'" /></label></div>
    <p class="form-note">密码按 UTF-8 编码最多72字节。不能停用自己或修改自己的管理员角色；修改自己的密码后需重新登录。</p>
    <label class="checkbox-label"><input type="checkbox" v-model="form.enabled" :disabled="busy || id === user?.id" />启用账号</label><div class="dialog-actions"><button class="primary" :disabled="busy || loading">保存账号</button></div>
  </form>
  <form class="filters" @submit.prevent="load(true)"><label class="search-field"><span class="sr-only">搜索账号</span><input v-model="keyword" maxlength="200" placeholder="搜索账号或显示名称" /></label><button :disabled="loading || busy">查询</button></form>
  <p v-if="loading" class="empty">正在加载账号…</p>
  <template v-else><div class="table-wrap"><table><thead><tr><th>账号</th><th>显示名称</th><th>角色</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in result.items" :key="item.id"><td>{{ item.username }}{{ item.id === user?.id ? '（当前）' : '' }}</td><td>{{ item.displayName }}</td><td>{{ item.role === 'ADMIN' ? '管理员' : '发布人员' }}</td><td>{{ item.enabled ? '启用' : '停用' }}</td><td><button :disabled="busy" @click="edit(item)">编辑账号</button></td></tr><tr v-if="!result.items.length"><td colspan="5" class="empty">没有匹配的账号。</td></tr></tbody></table></div>
    <nav class="pagination" aria-label="账号分页"><span>共 {{ result.total }} 个账号</span><button :disabled="page <= 1 || busy" @click="turn(-1)">上一页</button><span>{{ page }} / {{ result.pages || 1 }}</span><button :disabled="page >= result.pages || busy" @click="turn(1)">下一页</button></nav>
  </template>
</template>
