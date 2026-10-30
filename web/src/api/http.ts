import axios, { type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

export const http = axios.create({ baseURL: '/api', timeout: 8000 })

type RetryConfig = InternalAxiosRequestConfig & { _retried?: boolean }

// BEFORE every request: attach the CURRENT token  每个请求前:带上"最新"的 token
http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) config.headers.Authorization = `Bearer ${auth.accessToken}`
  return config
})

// ONE shared refresh for everyone who's waiting  所有人共用"同一次"刷新
let refreshing: Promise<void> | null = null

async function refreshTokens() {
  const auth = useAuthStore()
  const { data } = await http.post('/auth/refresh', { refreshToken: auth.refreshToken })
  auth.setTokens(data)
}

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const auth = useAuthStore()
    const original = error.config as RetryConfig | undefined
    const status = error.response?.status
    const code = error.response?.data?.code
    const isAuthCall = original?.url?.startsWith('/auth/')

    // --- Expired token: refresh once, then retry  过期:刷新一次,再重试 ---
    if (status === 401 && code === 'TOKEN_EXPIRED' && original && !isAuthCall && !original._retried) {
      original._retried = true   // never retry the same request twice  同一请求只重试一次

      // Did someone already get a new token while this request was in flight?
      // (another request in this tab, or another tab via the storage event)
      // 这个请求发出后,是否已经有别人拿到新 token 了?
      const usedToken = String(original.headers.Authorization ?? '').replace('Bearer ', '')
      if (auth.accessToken && auth.accessToken !== usedToken) {
        return http(original)
      }

      try {
        // First failing request starts the refresh; the others just await the same promise
        // 第一个失败的请求负责刷新,其他请求只是等待同一个 Promise
        refreshing ??= refreshTokens().finally(() => { refreshing = null })
        await refreshing
        return http(original)   // the request interceptor attaches the NEW token
      } catch {
        // refresh failed → fall through to "log out" below  刷新失败 → 往下走登出
      }
    }

    // --- Any other 401: log out  其他 401:登出 ---
    if (status === 401 && !isAuthCall) {
      auth.clear()
      router.push({ path: '/login', query: { reason: code } })
    }
    return Promise.reject(error)
  },
)

export function errorMessage(e: unknown): string {
  if (axios.isAxiosError(e)) {
    const message = (e.response?.data as { message?: string } | undefined)?.message
    if (message) return message
    if (e.code === 'ECONNABORTED') return 'Request timed out, please try again'
    if (!e.response) return 'Cannot reach the server'
  }
  return 'Something went wrong'
}