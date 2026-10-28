<script setup>
import { onMounted, ref } from 'vue'
import { request } from '../api'
import { dateText } from '../state'
const props = defineProps({ kind: { type: String, default: 'news' } })
const result = ref({ items: [], total: 0, pages: 0 })
const page = ref(1), keyword = ref(''), categoryId = ref(''), categories = ref([])
const loading = ref(false), error = ref('')
let requestId = 0
async function load(reset = false) {
  if (reset) page.value = 1
  const id = ++requestId
  loading.value = true; error.value = ''
  const params = new URLSearchParams({ page: page.value, size: 6, keyword: keyword.value })
  if (props.kind === 'news' && categoryId.value) params.set('categoryId', categoryId.value)
  try { const data = await request(`/api/public/${props.kind}?${params}`); if (id === requestId) result.value = data }
  catch (e) { if (id === requestId) error.value = e.message }
  finally { if (id === requestId) loading.value = false }
}
async function turn(delta) { page.value += delta; await load() }
onMounted(async () => {
  load()
  try { categories.value = await request('/api/public/categories') } catch (e) { error.value = e.message }
})
</script>
<template>
  <section class="page-head"><p class="eyebrow">CAMPUS / {{ kind === 'news' ? 'NEWS' : 'NOTICES' }}</p>
    <h1>{{ kind === 'news' ? '校园新闻' : '通知公告' }}</h1><p>及时了解校园里的新动态与重要安排。</p></section>
  <form class="filters" @submit.prevent="load(true)">
    <label class="search-field"><span class="sr-only">搜索标题</span><input v-model="keyword" maxlength="200" placeholder="搜索标题关键词" /></label>
    <label v-if="kind === 'news'"><span class="sr-only">新闻分类</span><select v-model="categoryId"><option value="">全部分类</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
    <button class="primary" :disabled="loading">搜索</button>
  </form>
  <p v-if="error" role="alert" class="error">{{ error }}</p>
  <p v-if="loading" class="empty" aria-live="polite">正在加载内容…</p>
  <template v-else-if="!error">
    <div class="list-meta">共 {{ result.total }} 条{{ kind === 'news' ? '新闻' : '通知' }}</div>
    <div v-if="result.items.length" :class="kind === 'news' ? 'news-grid' : 'notice-list'">
      <a v-for="item in result.items" :key="item.id" class="story" :href="`#/${kind}/${item.id}`">
        <div class="story-meta"><span class="tag">{{ item.categoryName || item.publisherUnit }}</span><time>{{ dateText(item.publishedAt) }}</time></div>
        <h2>{{ item.title }}</h2><p>{{ item.summary || item.body.slice(0, 100) }}</p>
        <div class="story-bottom"><span>{{ item.authorName || item.publisherUnit }}</span><span>阅读全文 ↗</span></div>
      </a>
    </div>
    <div v-else class="empty">暂无匹配内容，请调整关键词或分类。</div>
    <nav class="pagination" aria-label="分页"><button :disabled="page <= 1" @click="turn(-1)">上一页</button><span>第 {{ page }} 页 / 共 {{ result.pages || 1 }} 页</span><button :disabled="page >= result.pages" @click="turn(1)">下一页</button></nav>
  </template>
</template>
