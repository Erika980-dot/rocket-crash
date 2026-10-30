<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { errorMessage, http } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { fmtX, type Bet, type Me, type Round, type Wallet } from '@/api/types'
import BetPanel from '@/components/BetPanel.vue'

const auth = useAuthStore()
const router = useRouter()

const me = ref<Me | null>(null)
const balance = ref<number | null>(null)
const round = ref<Round | null>(null)
const myBet = ref<Bet | null>(null)   // my bet in the CURRENT round only  只记录"当前回合"的注单
const recent = ref<number[]>([])   // crash points, newest first
const busy = ref(false)
const error = ref('')
let timer: number | undefined

const phase = computed(() => {
  switch (round.value?.status) {
    case 'BETTING': return { label: 'Place your bets!', color: 'text-amber-300' }
    case 'RUNNING': return { label: 'Flying… 🚀', color: 'text-emerald-400' }
    case 'CRASHED': return { label: '💥 Crashed', color: 'text-red-400' }
    default:        return { label: 'Waiting…', color: 'text-slate-400' }
  }
})

// ---------- loading data from the server  从服务器读取 ----------
async function loadWallet() {
  balance.value = (await http.get<Wallet>('/wallet/me')).data.balance
}

async function loadRecent() {
  const { data } = await http.get<{ crashPoint: number }[]>('/rounds/recent')
  recent.value = data.map((r) => r.crashPoint)
}

async function pollRound() {
  try {
    round.value = (await http.get<Round>('/rounds/current')).data
  } catch {
    /* one failed poll is fine */
  }
}

/** Ask the server: do I have a bet in this round, and what's its status?  问服务器我这局的注单 */
async function loadMyBet(roundId: number) {
  const { data } = await http.get<Bet[]>('/bets/me')
  myBet.value = data.find((b) => b.roundId === roundId) ?? null
}

// ---------- actions (called by BetPanel's emits)  动作(由 BetPanel 的事件触发) ----------
async function placeBet(amount: number) {
  error.value = ''
  busy.value = true
  try {
    const { data } = await http.post<Bet>('/bets', {
      amount,
      requestId: crypto.randomUUID(),   // a NEW id per click → idempotency  每次点击一个新 ID
    })
    myBet.value = data
    await loadWallet()
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    busy.value = false
  }
}

async function cashOut() {
  error.value = ''
  busy.value = true
  try {
    myBet.value = (await http.post<Bet>('/bets/cashout')).data
    await loadWallet()
  } catch (e) {
    error.value = errorMessage(e)   // e.g. "The rocket already crashed"
  } finally {
    busy.value = false
  }
}

async function logout() {
  await auth.logout()
  router.push('/login')
}

// in http.ts: replace refreshTokens()
async function refreshTokens(usedToken: string) {
  // "rocket-token-refresh" is a lock shared by ALL tabs of this site
  // 这个锁由同一网站的"所有分页"共用
  await navigator.locks.request('rocket-token-refresh', async () => {
    const auth = useAuthStore()

    // While we waited for the lock, did another tab already refresh?
    // 等锁的时候,是不是别的分页已经刷新好了?
    const latest = localStorage.getItem('accessToken')
    if (latest && latest !== usedToken) {
      auth.accessToken = latest
      auth.refreshToken = localStorage.getItem('refreshToken') ?? ''
      return   // nothing to do, just use the new token  直接用新 token
    }

    const { data } = await http.post('/auth/refresh', { refreshToken: auth.refreshToken })
    auth.setTokens(data)
  })
}

// ---------- react to the round changing  回合变化时的反应 ----------
// A new round started → forget the old bet  新回合开始 → 清掉上一局的注单
watch(
  () => round.value?.id,
  (id, oldId) => {
    if (oldId !== undefined && id !== oldId) {
      myBet.value = null
      error.value = ''
    }
  },
)

// The rocket crashed → get the REAL result from the server (don't guess!)
// 火箭爆炸 → 向服务器拿真正的结果(不要自己猜!)
watch(
  () => round.value?.status,
  async (status) => {
    if (status === 'CRASHED') loadRecent()

    if (status === 'CRASHED' && round.value && myBet.value?.status === 'PLACED') {
      await loadMyBet(round.value.id)
      await loadWallet()
    }
  },
)

// ---------- lifecycle  生命周期 ----------
onMounted(async () => {
  me.value = (await http.get<Me>('/users/me')).data
  await loadWallet()
  await loadRecent()
  await pollRound()
  // After a page refresh mid-round: did I already bet?  刷新页面后:这局我已经下注了吗?
  if (round.value) await loadMyBet(round.value.id)
  timer = window.setInterval(pollRound, 500)
})

onUnmounted(() => window.clearInterval(timer))

</script>

<template>
  <div class="mx-auto max-w-3xl p-4">
    <header class="mb-6 flex flex-wrap items-center justify-between gap-4 rounded-2xl bg-slate-800 p-4">
      <div>
        <p class="text-sm text-slate-400">Player</p>
        <p class="text-lg font-semibold">{{ me?.username ?? '…' }}</p>
      </div>
      <div class="text-right">
        <p class="text-sm text-slate-400">Balance</p>
        <p class="text-lg font-bold text-amber-300">{{ balance?.toLocaleString() ?? '…' }} pts</p>
      </div>
      <div class="flex gap-2">
        <RouterLink to="/history" class="rounded-lg bg-slate-700 px-4 py-2 text-sm hover:bg-slate-600">
          History
        </RouterLink>
        <button @click="logout" class="rounded-lg bg-slate-700 px-4 py-2 text-sm hover:bg-slate-600">
          Log out
        </button>
      </div>
    </header>

        <div class="mb-3 flex flex-wrap gap-2">
      <span
        v-for="(c, i) in recent"
        :key="i"
        class="rounded-full px-3 py-1 text-sm font-semibold tabular-nums"
        :class="c >= 200 ? 'bg-emerald-500/20 text-emerald-300' : 'bg-red-500/20 text-red-300'"
      >
        {{ fmtX(c) }}
      </span>
    </div>

    <section class="rounded-2xl bg-slate-800 p-8 text-center">
      <p class="text-sm text-slate-400">Round #{{ round?.id ?? '–' }}</p>
      <p class="my-4 text-6xl font-black tabular-nums md:text-8xl" :class="phase.color">
        {{ round ? fmtX(round.multiplier) : '–' }}
      </p>
      <p class="text-lg" :class="phase.color">{{ phase.label }}</p>
    </section>

    <!-- props DOWN, events UP  数据往下传,事件往上报 -->
    <BetPanel :round="round" :my-bet="myBet" :busy="busy" @bet="placeBet" @cashout="cashOut" />

    <p v-if="error" class="mt-3 text-center text-sm text-red-400">{{ error }}</p>
  </div>
</template>