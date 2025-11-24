import { useAuthStore } from '@/stores/auth'

/**
 * Admin Guard - Bảo vệ routes ADMIN
 * Chỉ cho phép truy cập nếu ĐÃ đăng nhập ADMIN với ADMIN role
 */
export function adminGuard(to, from, next) {
  const authStore = useAuthStore()

  const adminToken = localStorage.getItem('adminToken')

  // CHỈ CHECK adminToken - không quan tâm userToken
  if (!adminToken) {
    console.log('adminGuard: No admin token, redirect to admin login')
    return next({
      path: '/admin/login',
      query: { redirect: to.fullPath }
    })
  }

  // Sync state nếu cần
  if (!authStore.isAdminAuthenticated || !authStore.admin) {
    console.log('adminGuard: Syncing admin auth state')
    authStore.checkAuth('ADMIN')
  }

  // Validate admin data
  const admin = JSON.parse(localStorage.getItem('admin') || '{}')
  if (admin.role !== 'ADMIN') {
    console.log('adminGuard: Not admin role, redirect to admin login')
    // Clear invalid data
    localStorage.removeItem('adminToken')
    localStorage.removeItem('admin')
    authStore.adminToken = null
    authStore.admin = null
    authStore.isAdminAuthenticated = false
    return next('/admin/login')
  }

  next()
}
