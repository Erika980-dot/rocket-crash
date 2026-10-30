<script setup lang="ts">
import { computed, ref } from 'vue'
import { fmtX, type Bet, type Round } from '@/api/types'

// ↓ data IN from the parent  父组件传进来的数据
const props = defineProps<{
  round: Round | null
  myBet: Bet | null
  busy: boolean
}>()

// ↑ events OUT to the parent  通知父组件的事件
const emit = defineEmits<{
  bet: [amount: number]
  cashout: []
}>()

const amount = ref(100)
const chips = [10, 50, 100, 500]

// What I'd win if I cashed out right now  现在提现可以拿多少
const potential = computed(() =>
  props.myBet && props.round ? Math.floor((props.myBet.amount * props.round.multiplier) / 100) : 0,
)

const canBet = computed(() => props.round?.status === 'BETTING' && !props.myBet && !props.busy)
const canCashOut = computed(
  () => props.round?.status === 'RUNNING' && props.myBet?.status === 'PLACED' && !props.busy,
)
</script>

<template>
  <section class="mt-6 rounded-2xl bg-slate-800 p-6">
    <!-- STATE 1: no bet this round  这局还没下注 -->
    <div v-if="!myBet">
      <div class="mb-3 flex flex-wrap gap-2">
        <button
          v-for="c in chips"
          :key="c"
          @click="amount = c"
          class="rounded-lg px-4 py-1 text-sm"
          :class="amount === c ? 'bg-amber-400 text-slate-900' : 'bg-slate-700 hover:bg-slate-600'"
        >
          {{ c }}
        </button>
      </div>

      <div class="flex flex-col gap-3 sm:flex-row">
        <input
          v-model.number="amount"
          type="number"
          min="10"
          max="10000"
          class="flex-1 rounded-lg bg-slate-700 px-4 py-3 outline-none focus:ring-2 focus:ring-amber-400"
        />
        <button
          :disabled="!canBet"
          @click="emit('bet', amount)"
          class="rounded-lg bg-amber-400 px-8 py-3 font-bold text-slate-900 hover:bg-amber-300 disabled:opacity-40"
        >
          {{ round?.status === 'BETTING' ? `Bet ${amount}` : 'Wait for next round' }}
        </button>
      </div>
    </div>

    <!-- STATE 2: bet placed, not settled  已下注,还没结算 -->
    <div v-else-if="myBet.status === 'PLACED'" class="text-center">
      <p class="mb-3 text-slate-400">Your bet: {{ myBet.amount }} pts</p>
      <button
        :disabled="!canCashOut"
        @click="emit('cashout')"
        class="w-full rounded-xl bg-emerald-500 py-4 text-xl font-bold text-slate-900 hover:bg-emerald-400 disabled:opacity-40"
      >
        {{ round?.status === 'RUNNING' ? `Cash out  +${potential}` : 'Waiting for launch…' }}
      </button>
    </div>

    <!-- STATE 3: settled  已结算 -->
    <div v-else class="text-center text-lg">
      <p v-if="myBet.status === 'WON'" class="text-emerald-400">
        ✅ Cashed out at {{ fmtX(myBet.cashoutAt ?? 0) }} → +{{ myBet.payout }} pts
      </p>
      <p v-else-if="myBet.status === 'LOST'" class="text-red-400">💥 Lost {{ myBet.amount }} pts</p>
      <p v-else class="text-slate-300">↩ Refunded {{ myBet.amount }} pts</p>
      <p class="mt-1 text-sm text-slate-400">Next round starting soon…</p>
    </div>
  </section>
</template>