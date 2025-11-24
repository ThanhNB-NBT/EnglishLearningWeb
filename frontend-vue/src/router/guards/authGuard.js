import { useAuthStore } from '@/stores/auth'

/**
 * Auth Guard - Bảo vệ routes USER
 * Chỉ cho phép truy cập nếu ĐÃ đăng nhập USER
 */
export function authGuard(to, from, next) {
  const authStore = useAuthStore()

  const userToken = localStorage.getItem('userToken')

  // CHỈ CHECK userToken - không quan tâm adminToken
  if (!userToken) {
    console.log('authGuard: No user token, redirect to login')
    return next({
      path: '/auth/login',
      query: { redirect: to.fullPath }
    })
  }

  // Sync state nếu cần
  if (!authStore.isUserAuthenticated || !authStore.user) {
    console.log('authGuard: Syncing user auth state')
    authStore.checkAuth('USER')
  }

  // Validate user data
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  if (user.role === 'ADMIN') {
    console.log('authGuard: User token belongs to ADMIN role, invalid state')
    // Clear invalid user token
    localStorage.removeItem('userToken')
    localStorage.removeItem('user')
    authStore.userToken = null
    authStore.user = null
    authStore.isUserAuthenticated = false
    return next('/auth/login')
  }

  next()
}
