import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

// One shared axios instance for the whole app  全站共用一个 axios
export const http = axios.create({
  baseURL: '/api',      // so we write http.get('/wallet/me'), not the full URL
  timeout: 8000,        // give up after 8 seconds  8 秒没回应就放弃
})

// BEFORE every request: attach the token  每个请求发出前:自动带上 token
http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

// AFTER every response: handle 401 in ONE place  每个回应回来后:统一处理 401
http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const code = error.response?.data?.code
    const isAuthCall = error.config?.url?.startsWith('/auth/')

    if (status === 401 && !isAuthCall) {
      useAuthStore().clear()
      router.push({ path: '/login', query: { reason: code } })   // e.g. reason=KICKED
    }
    return Promise.reject(error)
  },
)

// Turn any error into a friendly sentence for the user  把错误变成用户看得懂的话
export function errorMessage(e: unknown): string {
  if (axios.isAxiosError(e)) {
    const message = (e.response?.data as { message?: string } | undefined)?.message
    if (message) return message                                 // from our GlobalErrorHandler
    if (e.code === 'ECONNABORTED') return 'Request timed out, please try again'
    if (!e.response) return 'Cannot reach the server'
  }
  return 'Something went wrong'
}