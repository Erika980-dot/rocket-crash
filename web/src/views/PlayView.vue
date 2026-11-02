<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { http, refreshNow } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { Wallet } from '@/api/types'

const auth = useAuthStore()
const router = useRouter()
const frame = ref<HTMLIFrameElement | null>(null)
const balance = ref<number | null>(null)

async function loadWallet() {
  balance.value = (await http.get<Wallet>('/wallet/me')).data.balance
}

/** Give the game the current token  把当前 token 交给游戏 */
function sendToken() {
  frame.value?.contentWindow?.postMessage(
    { type: 'TOKEN', token: auth.accessToken },
    window.location.origin,              // only deliver to OUR origin  只送给自己的网站
  )
}

async function onMessage(e: MessageEvent) {
  // Must come from our site AND from our iframe  必须来自自己的网站,而且是我们的 iframe
  if (e.origin !== window.location.origin || e.source !== frame.value?.contentWindow) return

  switch (e.data?.type) {
    case 'GAME_READY':
      sendToken()
      break
    case 'TOKEN_EXPIRED':
      try {
        await refreshNow()               // the watch below sends the new token
      } catch {
        auth.clear()
        router.push({ path: '/login', query: { reason: 'TOKEN_EXPIRED' } })
      }
      break
    case 'BALANCE_CHANGED':
      loadWallet()
      break
    case 'AUTH_FAILED':
      auth.clear()
      router.push({ path: '/login', query: { reason: e.data.code } })
      break
  }
}

// Whenever the token changes (refresh here, or in another tab), pass it on
// token 一变(本分页刷新或其他分页),就交给游戏
watch(() => auth.accessToken, (t) => { if (t) sendToken() })

onMounted(() => {
  window.addEventListener('message', onMessage)
  loadWallet()
})
onUnmounted(() => window.removeEventListener('message', onMessage))
</script>

<template>
  <div class="mx-auto max-w-6xl p-4">
    <header class="mb-4 flex items-center justify-between gap-4 rounded-2xl bg-slate-800 p-4">
      <RouterLink to="/" class="rounded-lg bg-slate-700 px-4 py-2 text-sm hover:bg-slate-600">← Lobby</RouterLink>
      <p class="text-lg font-bold text-amber-300">{{ balance?.toLocaleString() ?? '…' }} pts</p>
    </header>

    <iframe
      ref="frame"
      src="/game/index.html"
      title="Rocket Crash game"
      class="aspect-video w-full rounded-2xl border-0 bg-slate-950"
    />
  </div>
</template>