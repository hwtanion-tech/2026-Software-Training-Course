<script setup>
import { onMounted, ref } from 'vue'
import { getJson } from './api'

const project = ref(null)
const backend = ref(null)
const database = ref(null)
const loading = ref(false)
const error = ref('')
const databaseError = ref('')

async function refresh() {
  loading.value = true
  error.value = ''
  databaseError.value = ''
  backend.value = null
  project.value = null
  database.value = null
  const results = await Promise.allSettled([
    getJson('/api/health'), getJson('/api/project'), getJson('/api/health/database'),
  ])
  if (results[0].status === 'fulfilled') backend.value = results[0].value
  else error.value = '后端暂未连接。请在 backend 目录运行 mvn spring-boot:run 后重试。'
  if (results[1].status === 'fulfilled') project.value = results[1].value
  else error.value ||= '项目信息加载失败，请检查后端服务。'
  if (results[2].status === 'fulfilled') database.value = results[2].value
  else databaseError.value = results[2].reason.message
  loading.value = false
}

onMounted(refresh)
</script>

<template>
  <div class="workspace">
    <header class="topbar">
      <a class="brand" href="#main"><span class="brand-icon">闻</span>校园新闻系统</a>
      <span class="stage-label">软件综合开发实训 / WEEK 01</span>
    </header>
    <main id="main">
      <section class="intro">
        <p class="eyebrow">第一周 · 需求分析与环境准备</p>
        <h1>从一条校园消息开始</h1>
        <p class="description">搭建校园新闻与通知的信息入口。本周先明确需求、划分角色，并打通前后端开发环境。</p>
        <div class="stack"><span>Spring Boot</span><span>Vue 3</span><span>MySQL</span></div>
      </section>

      <section class="environment" aria-labelledby="environment-title">
        <div class="section-heading"><h2 id="environment-title">开发环境</h2>
          <button type="button" :disabled="loading" @click="refresh">{{ loading ? '检查中…' : '重新检查' }}</button>
        </div>
        <div class="status-grid" aria-live="polite">
          <article><span class="card-label">01 / 前端</span><h3>Vue 页面已启动</h3><p><i class="dot good"></i>页面正常渲染</p></article>
          <article><span class="card-label">02 / 后端</span><h3>Spring Boot 服务</h3>
            <p><i class="dot" :class="{ good: backend?.status === 'UP' }"></i>{{ loading ? '正在检查' : backend?.message || '等待后端连接' }}</p></article>
          <article><span class="card-label">03 / 数据库</span><h3>MySQL 连接</h3>
            <p><i class="dot" :class="{ good: database?.status === 'UP' }"></i>{{ loading ? '正在检查' : database?.status === 'UP' ? '连接检查成功' : databaseError ? '检查失败' : '尚未配置连接' }}</p></article>
        </div>
        <p v-if="error" class="message error" role="alert">{{ error }}</p>
        <p class="message" :class="{ error: databaseError }">{{ databaseError || database?.message || 'MySQL 状态将在检查后显示。' }}</p>
      </section>

      <template v-if="project">
        <section class="roles" aria-labelledby="roles-title">
          <div class="section-heading"><h2 id="roles-title">谁会使用这个系统</h2><span class="muted">需求定义 · 尚未实现鉴权</span></div>
          <div class="role-grid"><article v-for="(role, index) in project.roles" :key="role.name">
            <span class="role-number">0{{ index + 1 }}</span><h3>{{ role.name }}</h3><p>{{ role.description }}</p>
          </article></div>
        </section>
        <div class="two-columns">
          <section><h2>本周交付</h2><ul><li v-for="item in project.weekOneDeliverables" :key="item">{{ item }}</li></ul></section>
          <section><h2>后续开发</h2><ul><li v-for="item in project.plannedFeatures" :key="item">{{ item }}<span class="pending">待实现</span></li></ul></section>
        </div>
        <section class="workflow"><h2>内容状态约定</h2><div class="states"><span v-for="state in project.contentStatuses" :key="state">{{ state }}</span></div>
          <p class="muted">前台只展示已发布内容。状态转换和权限校验将在后续业务开发中实现。</p></section>
      </template>
      <footer>校园新闻及通知发布系统设计及实现 · 第一周工程骨架</footer>
    </main>
  </div>
</template>
