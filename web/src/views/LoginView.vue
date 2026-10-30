<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { errorMessage } from '@/api/http'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

// Why were we sent here?  为什么被送回登录页?
const notice = computed(() => {
  if (route.query.reason === 'KICKED') return 'Your account was logged in on another device.'
  if (route.query.reason === 'TOKEN_EXPIRED') return 'Your session expired. Please log in again.'
  return ''
})

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    router.push('/')
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center p-4">
    <form @submit.prevent="submit" class="w-full max-w-sm space-y-4 rounded-2xl bg-slate-800 p-6 shadow-xl">
      <h1 class="text-center text-2xl font-bold">🚀 Rocket Crash</h1>

      <p v-if="notice" class="rounded-lg bg-amber-500/15 p-3 text-sm text-amber-300">{{ notice }}</p>

      <input
        v-model="username"
        placeholder="Username"
        autocomplete="username"
        class="w-full rounded-lg bg-slate-700 px-4 py-3 outline-none focus:ring-2 focus:ring-amber-400"
      />
      <input
        v-model="password"
        type="password"
        placeholder="Password"
        autocomplete="current-password"
        class="w-full rounded-lg bg-slate-700 px-4 py-3 outline-none focus:ring-2 focus:ring-amber-400"
      />

      <p v-if="error" class="text-sm text-red-400">{{ error }}</p>

      <button
        :disabled="loading"
        class="w-full rounded-lg bg-amber-400 py-3 font-semibold text-slate-900 hover:bg-amber-300 disabled:opacity-50"
      >
        {{ loading ? 'Logging in…' : 'Log in' }}
      </button>

      <p class="text-center text-sm text-slate-400">
        No account?
        <RouterLink to="/register" class="text-amber-400 hover:underline">Register</RouterLink>
      </p>
    </form>
  </div>
</template>