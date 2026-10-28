<script setup>
import { onMounted, ref } from 'vue'
import { request } from '../api'
import { dateText } from '../state'
const news = ref([]), notices = ref([]), error = ref(''), loading = ref(true)
onMounted(async () => {
  try {
    const [n, t] = await Promise.all([request('/api/public/news?size=4'), request('/api/public/notices?size=3')])
    news.value = n.items; notices.value = t.items
  } catch (e) { error.value = e.message } finally { loading.value = false }
})
</script>
<template>
  <section class="hero"><div><p class="eyebrow">CAMPUS NEWSROOM</p><h1>校园里的每一天，<br />都有新的发现。</h1><p>连接校园动态、学术交流与日常生活。<br />在这里，看见身边正在发生的事。</p><a class="primary button-link" href="#/news">浏览校园新闻 <span>↗</span></a></div><div class="hero-art" aria-hidden="true"><span class="art-word">闻</span><span class="art-caption">记录 · 分享 · 连接</span><div class="art-line"></div></div></section>
  <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="loading" class="empty">正在加载校园资讯…</p>
  <div v-else class="home-columns">
    <section><div class="section-heading"><h2>最新新闻</h2><a href="#/news">查看全部 →</a></div>
      <div v-if="news.length" class="home-news"><a v-for="(item, index) in news" :key="item.id" :href="`#/news/${item.id}`" class="home-story"><span class="item-number">0{{ index + 1 }}</span><div><div class="story-meta"><span>{{ item.categoryName }}</span><time>{{ dateText(item.publishedAt) }}</time></div><h3>{{ item.title }}</h3><p>{{ item.summary }}</p></div></a></div><p v-else class="empty">暂无已发布新闻</p>
    </section>
    <aside class="notices-panel"><div class="section-heading"><h2>通知公告</h2><a href="#/notices">更多 →</a></div>
      <a v-for="item in notices" :key="item.id" class="notice-link" :href="`#/notices/${item.id}`"><time>{{ dateText(item.publishedAt).slice(0, 10) }}</time><h3>{{ item.title }}</h3><span>{{ item.publisherUnit }}</span></a><p v-if="!notices.length" class="empty">暂无已发布通知</p>
      <div class="aside-note">及时关注通知，<br />让重要安排不被错过。</div>
    </aside>
  </div>
</template>
