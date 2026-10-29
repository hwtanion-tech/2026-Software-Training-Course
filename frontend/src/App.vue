<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { request, logout } from './api'
import { user, project, route, go } from './state'
import HomePage from './components/HomePage.vue'
import PublicList from './components/PublicList.vue'
import ArticleDetail from './components/ArticleDetail.vue'
import LoginPage from './components/LoginPage.vue'
import ManageContent from './components/ManageContent.vue'
import CategoryPage from './components/CategoryPage.vue'
import AccountPage from './components/AccountPage.vue'
const ready = ref(false), error = ref(''), leaving = ref(false)
const parts = computed(() => route.value.split('/').filter(Boolean))
const managed = computed(() => ['manage', 'categories', 'accounts'].includes(parts.value[0]))
function guard() { if (ready.value && managed.value && !user.value) go('/login') }
watch([route, user, ready], guard)
watch(route, () => { error.value = ''; window.scrollTo(0, 0) })
async function signOut() {
  leaving.value = true
  try { await logout(); user.value = null; go('/home') } catch (e) { error.value = e.message } finally { leaving.value = false }
}
onMounted(async () => {
  try {
    const [info, session] = await Promise.all([request('/api/project'), request('/api/auth/me')])
    project.value = info; user.value = session.user
  } catch (e) { error.value = '无法连接后端，请确认服务已启动。' } finally { ready.value = true }
})
</script>
<template>
  <header class="topbar"><a class="brand" href="#/home"><span class="brand-icon">闻</span><span>校园新闻<span class="brand-sub">CAMPUS NEWS</span></span></a>
    <nav class="main-nav" aria-label="主导航"><a href="#/home" :class="{ active: parts[0] === 'home' }">首页</a><a href="#/news" :class="{ active: parts[0] === 'news' }">校园新闻</a><a href="#/notices" :class="{ active: parts[0] === 'notices' }">通知公告</a></nav>
    <div class="account"><template v-if="user"><a href="#/manage/news" class="manage-link">管理后台</a><span>{{ user.displayName }}</span><button :disabled="leaving" @click="signOut">退出</button></template><a v-else class="login-link" href="#/login">管理登录 ↗</a></div>
  </header>
  <div v-if="project?.mode === 'demo'" class="demo-strip">本机演示模式 · 内容为实训样例 · 修改会保存在本机数据库</div>
  <main><p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="!ready" class="empty">正在连接校园新闻系统…</p>
    <template v-else>
      <HomePage v-if="parts[0] === 'home'" />
      <template v-else-if="['news', 'notices'].includes(parts[0])"><ArticleDetail v-if="parts[1]" :key="route" :kind="parts[0]" :id="parts[1]" /><PublicList v-else :key="route" :kind="parts[0]" /></template>
      <LoginPage v-else-if="parts[0] === 'login'" />
      <ManageContent v-else-if="parts[0] === 'manage' && user && ['news', 'notices'].includes(parts[1])" :key="route" :kind="parts[1]" />
      <CategoryPage v-else-if="parts[0] === 'categories' && user?.role === 'ADMIN'" />
      <AccountPage v-else-if="parts[0] === 'accounts' && user?.role === 'ADMIN'" />
      <p v-else-if="['categories', 'accounts'].includes(parts[0]) && user" class="empty error">仅管理员可以进入此页面。</p>
      <p v-else class="empty">页面不存在，<a href="#/home">返回首页</a>。</p>
    </template>
  </main>
  <footer><a class="brand" href="#/home"><span class="brand-icon">闻</span>校园新闻</a><span>校园新闻及通知发布系统 · 软件综合开发实训</span><span>关注校园 · 连接你我</span></footer>
</template>
