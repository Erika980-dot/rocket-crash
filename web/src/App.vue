<script setup lang="ts">
import { watch } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

// Logged out (in this tab or another one) while on a protected page → go to login
// 在需要登录的页面时被登出(本分页或其他分页)→ 回到登录页
watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (!loggedIn && route.meta.requiresAuth) router.push('/login')
  },
)
</script>

<template>
  <RouterView />
</template>