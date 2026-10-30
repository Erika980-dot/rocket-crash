<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { http } from '@/api/http'
import { fmtX, type Bet } from '@/api/types'

const bets = ref<Bet[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    bets.value = (await http.get<Bet[]>('/bets/me')).data
  } finally {
    loading.value = false
  }
})

function result(b: Bet): { text: string; color: string } {
  switch (b.status) {
    case 'WON':      return { text: `+${b.payout - b.amount} @ ${fmtX(b.cashoutAt ?? 0)}`, color: 'text-emerald-400' }
    case 'LOST':     return { text: `−${b.amount}`, color: 'text-red-400' }
    case 'REFUNDED': return { text: 'Refunded', color: 'text-slate-300' }
    default:         return { text: 'In play…', color: 'text-amber-300' }
  }
}
</script>

<template>
  <div class="mx-auto max-w-3xl p-4">
    <div class="mb-4 flex items-center justify-between">
      <h1 class="text-2xl font-bold">My bets</h1>
      <RouterLink to="/" class="rounded-lg bg-slate-700 px-4 py-2 text-sm hover:bg-slate-600">← Lobby</RouterLink>
    </div>

    <p v-if="loading" class="text-slate-400">Loading…</p>
    <p v-else-if="bets.length === 0" class="rounded-2xl bg-slate-800 p-6 text-center text-slate-400">
      No bets yet. Go play a round! 🚀
    </p>

    <ul v-else class="space-y-2">
      <li
        v-for="b in bets"
        :key="b.id"
        class="flex flex-col gap-1 rounded-xl bg-slate-800 p-4 md:flex-row md:items-center md:justify-between"
      >
        <span class="text-slate-300">Round #{{ b.roundId }} · bet {{ b.amount }}</span>
        <span class="font-semibold" :class="result(b).color">{{ result(b).text }}</span>
      </li>
    </ul>
  </div>
</template>