<script setup>
import { onMounted, reactive, ref } from 'vue'
import { request } from '../api'
import { user } from '../state'
const categories = ref([]), error = ref(''), message = ref(''), busy = ref(false), id = ref(null)
const form = reactive({ name: '', sortOrder: 0, enabled: true })
async function load() { categories.value = await request('/api/manage/categories') }
function edit(item) { id.value = item.id; Object.assign(form, item); message.value = '' }
function reset() { id.value = null; Object.assign(form, { name: '', sortOrder: 0, enabled: true }) }
async function save() {
  error.value = ''; message.value = ''; busy.value = true
  try {
    await request(`/api/admin/categories${id.value ? `/${id.value}` : ''}`, { method: id.value ? 'PUT' : 'POST', body: { name: form.name, sortOrder: Number(form.sortOrder), enabled: form.enabled } })
    reset(); await load(); message.value = '分类已保存。'
  } catch (e) { error.value = e.message } finally { busy.value = false }
}
onMounted(async () => { try { await load() } catch (e) { error.value = e.message } })
</script>
<template>
  <section class="page-head compact"><p class="eyebrow">CATEGORIES</p><h1>新闻分类管理</h1><p>停用后不再用于新内容，已有新闻的分类信息保留。</p></section>
  <nav class="admin-tabs"><a href="#/manage/news">新闻管理</a><a href="#/manage/notices">通知管理</a><a class="active" href="#/categories">分类管理</a><a v-if="user?.role === 'ADMIN'" href="#/accounts">账号管理</a></nav>
  <p v-if="error" role="alert" class="error">{{ error }}</p><p v-if="message" role="status" class="success">{{ message }}</p>
  <form class="editor-form" @submit.prevent="save"><h2>{{ id ? '编辑分类' : '新建分类' }}</h2><div class="form-grid"><label>分类名称<input v-model="form.name" required maxlength="50" /></label><label>排序<input v-model="form.sortOrder" type="number" min="0" max="9999" required /></label></div><label class="checkbox-label"><input type="checkbox" v-model="form.enabled" />启用分类</label><div class="dialog-actions"><button v-if="id" type="button" @click="reset">取消编辑</button><button class="primary" :disabled="busy">保存分类</button></div></form>
  <div class="table-wrap"><table><thead><tr><th>名称</th><th>排序</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in categories" :key="item.id"><td>{{ item.name }}</td><td>{{ item.sortOrder }}</td><td>{{ item.enabled ? '启用' : '停用' }}</td><td><button @click="edit(item)">编辑分类</button></td></tr></tbody></table></div>
</template>

