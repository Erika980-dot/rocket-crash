import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import LobbyView from '@/views/LobbyView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', component: LobbyView, meta: { requiresAuth: true } },
    { path: '/login', component: LoginView },
    { path: '/register', component: RegisterView },
    { path: '/:pathMatch(.*)*', redirect: '/' },   // any unknown URL → lobby
  ],
})

// The "route guard": runs before every page change  路由守卫:每次换页之前执行
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) return '/login'                       // not logged in → login
  if ((to.path === '/login' || to.path === '/register') && auth.isLoggedIn) return '/' // already in → lobby
})

export default router