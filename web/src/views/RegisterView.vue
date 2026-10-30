<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { errorMessage } from '@/api/http'

const auth = useAuthStore()
const router = useRouter()

const username = ref('')
const password = ref('')
const confirm = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''

  // Quick checks in the browser: instant feedback, no round trip.
  // The backend checks again, because the browser can't be trusted.
  // 浏览器先快速检查(马上提示),后端会再检查一次(因为浏览器不可信)
  if (password.value.length < 8) {
    error.value = 'Password must be at least 8 characters'
    return
  }
  if (password.value !== confirm.value) {
    error.value = 'Passwords do not match'
    return
  }

  loading.value = true
  try {
    await auth.register(username.value, password.value)
    router.push('/')
  } catch (e) {
    error.value = errorMessage(e)   // e.g. "Username already taken"
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center p-4">
    <form @submit.prevent="submit" class="w-full max-w-sm space-y-4 rounded-2xl bg-slate-800 p-6 shadow-xl">
      <h1 class="text-center text-2xl font-bold">Create account</h1>
      <p class="text-center text-sm text-slate-400">You get 1,000 points to start 🎁</p>

      <input v-model="username" placeholder="Username (letters, numbers, _)" autocomplete="username"
        class="w-full rounded-lg bg-slate-700 px-4 py-3 outline-none focus:ring-2 focus:ring-amber-400" />
      <input v-model="password" type="password" placeholder="Password (min 8)" autocomplete="new-password"
        class="w-full rounded-lg bg-slate-700 px-4 py-3 outline-none focus:ring-2 focus:ring-amber-400" />
      <input v-model="confirm" type="password" placeholder="Confirm password" autocomplete="new-password"
        class="w-full rounded-lg bg-slate-700 px-4 py-3 outline-none focus:ring-2 focus:ring-amber-400" />

      <p v-if="error" class="text-sm text-red-400">{{ error }}</p>

      <button :disabled="loading"
        class="w-full rounded-lg bg-amber-400 py-3 font-semibold text-slate-900 hover:bg-amber-300 disabled:opacity-50">
        {{ loading ? 'Creating…' : 'Register' }}
      </button>

      <p class="text-center text-sm text-slate-400">
        Have an account?
        <RouterLink to="/login" class="text-amber-400 hover:underline">Log in</RouterLink>
      </p>
    </form>
  </div>
</template>