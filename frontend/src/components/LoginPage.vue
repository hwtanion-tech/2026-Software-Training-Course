<script setup>
import { ref } from 'vue'
import { login } from '../api'
import { user, project, go } from '../state'
const username = ref(''), password = ref(''), error = ref(''), busy = ref(false)
async function submit() {
  busy.value = true; error.value = ''
  try { const session = await login(username.value.trim(), password.value); user.value = session.user; password.value = ''; go('/manage/news') }
  catch (e) { error.value = e.message } finally { busy.value = false }
}
</script>
<template>
  <section class="login-layout"><div class="login-copy"><p class="eyebrow">EDITORIAL DESK</p><h1>让有价值的消息，<br />被更多人看见。</h1><p>登录内容管理后台，维护新闻与通知。</p><ul><li>发布人员维护自己创建的内容</li><li>管理员管理全部内容与新闻分类</li><li>草稿与撤回内容不会公开展示</li></ul></div>
    <form class="login-card" @submit.prevent="submit"><h2>登录管理后台</h2><p class="muted">请输入你的账号和密码</p>
      <label>账号<input v-model="username" required maxlength="50" autocomplete="username" placeholder="请输入账号" /></label>
      <label>密码<input v-model="password" type="password" required maxlength="64" autocomplete="current-password" placeholder="请输入密码" /></label>
      <p v-if="error" class="error" role="alert">{{ error }}</p><button class="primary full-width" :disabled="busy">{{ busy ? '正在登录…' : '登录' }}</button>
      <div v-if="project?.mode === 'demo'" class="demo-help"><strong>本机演示账号</strong><p>管理员：admin / DemoAdmin!2026</p><p>发布人员：editor / DemoEditor!2026</p><p>另一编辑：other / DemoOther!2026</p></div>
    </form>
  </section>
</template>
