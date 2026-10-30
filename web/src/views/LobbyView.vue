<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

interface Me { id: number; username: string }
interface Wallet { userId: number; balance: number }
interface Round {
  id: number
  status: 'BETTING' | 'RUNNING' | 'CRASHED' | 'CANCELLED'
  multiplier: number
  crashPoint: number | null
}

const auth = useAuthStore()
const router = useRouter()

const me = ref<Me | null>(null)
const balance = ref<number | null>(null)
const round = ref<Round | null>(null)
let timer: number | undefined

const multiplierText = computed(() =>
  round.value ? (round.value.multiplier / 100).toFixed(2) + 'x' : '–',
)

// Different colour and label per phase  每个阶段不同颜色和文字
const phase = computed(() => {
  switch (round.value?.status) {
    case 'BETTING': return { label: 'Place your bets!', color: 'text-amber-300' }
    case 'RUNNING': return { label: 'Flying… 🚀', color: 'text-emerald-400' }
    case 'CRASHED': return { label: '💥 Crashed', color: 'text-red-400' }
    default:        return { label: 'Waiting…', color: 'text-slate-400' }
  }
})

async function loadAccount() {
  // Two requests at the same time  同时发两个请求
  const [u, w] = await Promise.all([
    http.get<Me>('/users/me'),
    http.get<Wallet>('/wallet/me'),
  ])
  me.value = u.data
  balance.value = w.data.balance
}

async function pollRound() {
  try {
    round.value = (await http.get<Round>('/rounds/current')).data
  } catch {
    // one failed poll is fine; the next one comes in 0.5s
  }
}

async function logout() {
  await auth.logout()
  router.push('/login')
}

// Runs when the page appears  页面出现时执行
onMounted(() => {
  loadAccount()
  pollRound()
  timer = window.setInterval(pollRound, 500)   // ask every 0.5 seconds  每 0.5 秒问一次
})

// Runs when the page disappears: ALWAYS clean up timers!  页面消失时:一定要清掉定时器!
onUnmounted(() => window.clearInterval(timer))
</script>

<template>
  <div class="mx-auto max-w-3xl p-4">
    <!-- Top bar  顶部 -->
    <header class="mb-6 flex items-center justify-between gap-4 rounded-2xl bg-slate-800 p-4">
      <div>
        <p class="text-sm text-slate-400">Player</p>
        <p class="text-lg font-semibold">{{ me?.username ?? '…' }}</p>
      </div>
      <div class="text-right">
        <p class="text-sm text-slate-400">Balance</p>
        <p class="text-lg font-bold text-amber-300">{{ balance?.toLocaleString() ?? '…' }} pts</p>
      </div>
      <button @click="logout" class="rounded-lg bg-slate-700 px-4 py-2 text-sm hover:bg-slate-600">
        Log out
      </button>
    </header>

    <!-- Live round  实时回合 -->
    <section class="rounded-2xl bg-slate-800 p-8 text-center">
      <p class="text-sm text-slate-400">Round #{{ round?.id ?? '–' }}</p>
      <p class="my-4 text-6xl font-black tabular-nums md:text-8xl" :class="phase.color">
        {{ multiplierText }}
      </p>
      <p class="text-lg" :class="phase.color">{{ phase.label }}</p>
    </section>
  </div>
</template>