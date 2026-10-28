import { ref } from 'vue'

export const user = ref(null)
export const project = ref(null)
export const route = ref(window.location.hash.slice(1) || '/home')
window.addEventListener('hashchange', () => { route.value = window.location.hash.slice(1) || '/home' })
window.addEventListener('session-expired', () => { user.value = null })
export function go(path) {
  // 同步路由状态，避免退出时守卫仍依据旧后台路径跳回登录页。
  route.value = path
  window.location.hash = path
}
export const statusName = { DRAFT: '草稿', PUBLISHED: '已发布', WITHDRAWN: '已撤回' }
export function dateText(value) { return value ? value.replace('T', ' ').slice(0, 16) : '尚未发布' }
