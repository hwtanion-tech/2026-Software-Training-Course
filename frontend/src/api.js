export async function getJson(path) {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 8000)
  try {
    const response = await fetch(path, { signal: controller.signal })
    const contentType = response.headers.get('content-type') || ''
    if (!contentType.includes('application/json')) {
      throw new Error('未收到 JSON 响应，请确认后端已在 8080 端口启动。')
    }
    const data = await response.json()
    if (!response.ok) throw new Error(data.message || `请求失败（${response.status}）`)
    return data
  } catch (error) {
    if (error.name === 'AbortError') throw new Error('请求超时，请检查后端服务。')
    throw error
  } finally {
    clearTimeout(timeout)
  }
}
