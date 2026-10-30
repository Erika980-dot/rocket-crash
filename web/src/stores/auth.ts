import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { http } from '@/api/http'

interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export const useAuthStore = defineStore('auth', () => {
  // Start with whatever was saved last time (survives page refresh)  页面刷新也不会丢失
  const accessToken = ref(localStorage.getItem('accessToken') ?? '')
  const refreshToken = ref(localStorage.getItem('refreshToken') ?? '')

  const isLoggedIn = computed(() => accessToken.value !== '')

  function setTokens(t: TokenResponse) {
    accessToken.value = t.accessToken
    refreshToken.value = t.refreshToken
    localStorage.setItem('accessToken', t.accessToken)
    localStorage.setItem('refreshToken', t.refreshToken)
  }

  function clear() {
    accessToken.value = ''
    refreshToken.value = ''
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  async function login(username: string, password: string) {
    const { data } = await http.post<TokenResponse>('/auth/login', { username, password })
    setTokens(data)
  }

  async function register(username: string, password: string) {
    await http.post('/auth/register', { username, password })
    await login(username, password)   // log in straight away after registering
  }

  async function logout() {
    try {
      if (refreshToken.value) {
        await http.post('/auth/logout', { refreshToken: refreshToken.value })   // end the server session
      }
    } catch {
      // even if the server call fails, still log out locally
    }
    clear()
  }

    // Another tab changed the tokens (refresh, login or logout) → follow it.
  // The "storage" event fires in every OTHER tab of the same site.
  // 其他分页改了 token(刷新/登录/登出)→ 跟着同步。"storage" 事件只会在"其他"分页触发。
  window.addEventListener('storage', (e) => {
    if (e.key === 'accessToken') accessToken.value = e.newValue ?? ''
    if (e.key === 'refreshToken') refreshToken.value = e.newValue ?? ''
  })
  
  return { accessToken, refreshToken, isLoggedIn, setTokens, clear, login, register, logout }
})