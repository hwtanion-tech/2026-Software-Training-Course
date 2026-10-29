<script setup>
import { onMounted, reactive, ref } from 'vue'
import { request } from '../api'
import { user, statusName, dateText } from '../state'
const props = defineProps({ kind: String })
const result = ref({ items: [], total: 0, pages: 0 }), categories = ref([])
const keyword = ref(''), status = ref(''), page = ref(1), busy = ref(false), loading = ref(false)
const error = ref(''), message = ref(''), editing = ref(false), editingId = ref(null), oldStatus = ref(''), deleting = ref(null)
const form = reactive({ title: '', summary: '', body: '', categoryId: '', authorName: '', publisherUnit: '' })
async function load(reset = false) {
  if (reset) page.value = 1
  loading.value = true; error.value = ''
  const params = new URLSearchParams({ keyword: keyword.value, page: page.value, size: 8 })
  if (status.value) params.set('status', status.value)
  try { result.value = await request(`/api/manage/${props.kind}?${params}`) }
  catch (e) { error.value = e.message } finally { loading.value = false }
}
function start(item = null) {
  editingId.value = item?.id || null; oldStatus.value = item?.status || ''; editing.value = true; error.value = ''; message.value = ''
  Object.assign(form, { title: item?.title || '', summary: item?.summary || '', body: item?.body || '', categoryId: item?.categoryId || '', authorName: item?.authorName || user.value?.displayName || '', publisherUnit: item?.publisherUnit || '' })
}
async function save() {
  busy.value = true; error.value = ''; message.value = ''
  try {
    const body = { ...form, categoryId: props.kind === 'news' ? Number(form.categoryId) : null }
    await request(`/api/manage/${props.kind}${editingId.value ? `/${editingId.value}` : ''}`, { method: editingId.value ? 'PUT' : 'POST', body })
    editing.value = false; message.value = oldStatus.value === 'PUBLISHED' ? '修改已保存为草稿，前台已下架。确认后可重新发布。' : '内容已保存。'
    await load(true)
  } catch (e) { error.value = e.message } finally { busy.value = false }
}
async function action(item, operation) {
  busy.value = true; error.value = ''; message.value = ''
  try {
    await request(`/api/manage/${props.kind}/${item.id}${operation === 'delete' ? '' : `/${operation}`}`, { method: operation === 'delete' ? 'DELETE' : 'POST' })
    deleting.value = null; message.value = { publish: '已发布，可前往公开页面查看。', withdraw: '已撤回，公开列表和详情均已隐藏。', delete: '内容已删除。' }[operation]
    await load(true)
  } catch (e) { error.value = e.message } finally { busy.value = false }
}
async function turn(delta) { page.value += delta; await load() }
onMounted(async () => {
  load()
  try { categories.value = await request('/api/manage/categories') } catch (e) { error.value = e.message }
})
</script>
<template>
  <section class="page-head compact"><p class="eyebrow">CONTENT MANAGEMENT</p><div class="section-heading"><div><h1>{{ kind === 'news' ? '新闻管理' : '通知管理' }}</h1><p>{{ user?.role === 'ADMIN' ? '管理全部发布人员的内容。' : '此处仅显示你创建的内容。' }}</p></div><button class="primary" :disabled="busy" @click="start()">＋ 新建{{ kind === 'news' ? '新闻' : '通知' }}</button></div></section>
  <nav class="admin-tabs"><a href="#/manage/news" :class="{ active: kind === 'news' }">新闻管理</a><a href="#/manage/notices" :class="{ active: kind === 'notices' }">通知管理</a><a v-if="user?.role === 'ADMIN'" href="#/categories">分类管理</a><a v-if="user?.role === 'ADMIN'" href="#/accounts">账号管理</a></nav>
  <p v-if="error" role="alert" class="error">{{ error }}</p><p v-if="message" role="status" class="success">{{ message }}</p>
  <form v-if="editing" class="editor-form" @submit.prevent="save"><div class="section-heading"><h2>{{ editingId ? '编辑内容' : '新建内容' }}</h2><button type="button" :disabled="busy" @click="editing = false">取消编辑</button></div>
    <p v-if="oldStatus === 'PUBLISHED'" class="form-note">修改已发布内容后会自动保存为草稿，需再次发布。</p>
    <label>标题<input v-model="form.title" required maxlength="200" placeholder="请输入标题，最多200字" /></label>
    <div class="form-grid" v-if="kind === 'news'"><label>新闻分类<select v-model="form.categoryId" required><option value="" disabled>请选择分类</option><option v-for="category in categories" :key="category.id" :value="category.id" :disabled="!category.enabled">{{ category.name }}{{ category.enabled ? '' : '（已停用）' }}</option></select></label><label>作者<input v-model="form.authorName" required maxlength="50" /></label></div>
    <label v-else>发布单位<input v-model="form.publisherUnit" required maxlength="100" placeholder="例如：教务办公室" /></label>
    <label v-if="kind === 'news'">摘要<textarea v-model="form.summary" rows="2" maxlength="500" placeholder="可选，最多500字"></textarea></label>
    <label>正文<textarea v-model="form.body" required rows="9" maxlength="50000" placeholder="请输入正文，支持换行，按纯文本展示"></textarea></label>
    <div class="editor-actions"><span class="muted">{{ form.body.length }} / 50000 字</span><button class="primary" :disabled="busy">{{ busy ? '保存中…' : '保存内容' }}</button></div>
  </form>
  <form class="filters" @submit.prevent="load(true)"><label class="search-field"><span class="sr-only">搜索标题</span><input v-model="keyword" maxlength="200" placeholder="搜索标题关键词" /></label><label><span class="sr-only">状态筛选</span><select v-model="status"><option value="">全部状态</option><option v-for="(label, value) in statusName" :key="value" :value="value">{{ label }}</option></select></label><button :disabled="busy || loading">查询</button></form>
  <p v-if="loading" class="empty">正在加载…</p>
  <template v-else><div class="table-wrap"><table><thead><tr><th>标题</th><th>状态</th><th>创建者</th><th>更新时间</th><th>操作</th></tr></thead><tbody>
    <tr v-for="item in result.items" :key="item.id"><td><strong>{{ item.title }}</strong><small>#{{ item.id }} · {{ item.categoryName || item.publisherUnit }}</small></td><td><span class="status-pill" :class="item.status">{{ statusName[item.status] }}</span></td><td>{{ item.creatorName }}</td><td>{{ dateText(item.updatedAt) }}</td><td class="row-actions"><button :disabled="busy" @click="start(item)">编辑</button><button v-if="item.status !== 'PUBLISHED'" :disabled="busy" @click="action(item, 'publish')">发布</button><button v-else :disabled="busy" @click="action(item, 'withdraw')">撤回</button><button v-if="item.status !== 'PUBLISHED'" class="danger" :disabled="busy" @click="deleting = item">删除</button><a v-if="item.status === 'PUBLISHED'" :href="`#/${kind}/${item.id}`">查看</a></td></tr>
    <tr v-if="!result.items.length"><td colspan="5" class="empty">暂无内容，点击“新建”创建第一条内容。</td></tr>
  </tbody></table></div><nav class="pagination" aria-label="管理列表分页"><span>共 {{ result.total }} 条</span><button :disabled="page <= 1 || busy" @click="turn(-1)">上一页</button><span>{{ page }} / {{ result.pages || 1 }}</span><button :disabled="page >= result.pages || busy" @click="turn(1)">下一页</button></nav></template>
  <div v-if="deleting" class="dialog-backdrop"><section class="dialog" role="dialog" aria-modal="true" aria-labelledby="delete-title"><h2 id="delete-title">确认删除内容</h2><p>将删除“{{ deleting.title }}”，此操作无法在页面内撤销。</p><div class="dialog-actions"><button :disabled="busy" @click="deleting = null">取消</button><button class="danger" :disabled="busy" @click="action(deleting, 'delete')">确认删除</button></div></section></div>
</template>

