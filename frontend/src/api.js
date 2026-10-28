let csrf = null

export async function refreshCsrf() {
  csrf = await request('/api/auth/csrf')
  return csrf
}

export async function request(path, options = {}) {
  const method = options.method || 'GET'
  const headers = { ...options.headers }
  if (!['GET', 'HEAD'].includes(method)) {
    const token = csrf || await refreshCsrf()
    headers[token.headerName] = token.token
  }
  let body = options.body
  if (body && !(body instanceof URLSearchParams)) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(body)
  }
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 8000)
  try {
    const response = await fetch(path, { method, body, headers, credentials: 'same-origin', signal: controller.signal })
    const contentType = response.headers.get('content-type') || ''
    if (!contentType.includes('application/json')) {
      throw new Error('未收到 JSON 响应，请确认后端已在 8080 端口启动。')
    }
    const data = await response.json()
    if (!response.ok) {
      if (response.status === 401) window.dispatchEvent(new Event('session-expired'))
      if (response.status === 403) csrf = null
      const failure = new Error(data.message || `请求失败（${response.status}）`)
      failure.status = response.status
      throw failure
    }
    return data.code === 'OK' ? data.data : data
  } catch (error) {
    if (error.name === 'AbortError') throw new Error('请求超时，请检查后端服务。')
    throw error
  } finally {
    clearTimeout(timeout)
  }
}

export const getJson = request
export async function login(username, password) {
  await refreshCsrf()
  await request('/api/auth/login', { method: 'POST', body: new URLSearchParams({ username, password }) })
  csrf = null
  await refreshCsrf()
  return request('/api/auth/me')
}
export async function logout() {
  await request('/api/auth/logout', { method: 'POST' })
  csrf = null
}
