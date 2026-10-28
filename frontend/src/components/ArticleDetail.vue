<script setup>
import { onMounted, ref } from 'vue'
import { request } from '../api'
import { dateText } from '../state'
const props = defineProps({ kind: String, id: String })
const article = ref(null), error = ref('')
onMounted(async () => {
  try { article.value = await request(`/api/public/${props.kind}/${props.id}`) }
  catch (e) { error.value = e.message }
})
</script>
<template>
  <a class="back-link" :href="`#/${kind}`">← 返回{{ kind === 'news' ? '新闻列表' : '通知列表' }}</a>
  <p v-if="error" class="empty error" role="alert">{{ error }}</p>
  <article v-else-if="article" class="article-detail">
    <span class="tag">{{ article.categoryName || article.publisherUnit }}</span>
    <h1>{{ article.title }}</h1><div class="article-meta">{{ article.authorName || article.publisherUnit }} · {{ dateText(article.publishedAt) }}</div>
    <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
    <div class="article-body">{{ article.body }}</div>
  </article>
  <p v-else class="empty">正在加载正文…</p>
</template>
