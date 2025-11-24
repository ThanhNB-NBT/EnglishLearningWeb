// Sửa file interceptors.js theo nội dung này
import apiClient from './config'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'vue-toastification'
import router from '@/router'

export function setupInterceptors() {
  // Request interceptor: thêm token header
  apiClient.interceptors.request.use(
    (config) => {
      const authStore = useAuthStore()

      const isLogoutEndpoint = config.url?.includes('/logout')
      //Không gửi token cho các endpoint AUTH (login, register, forgot-password...)
      const isAuthEndpoint = config.url?.includes('/auth/') && !isLogoutEndpoint

      const isAdminCreate = config.url?.includes('/auth/admin/create')

      if (isAuthEndpoint && !isAdminCreate) {
        console.log('⚪ Auth endpoint detected, skipping token:', config.url)
        return config // Không thêm token cho login/register
      }

      // Nếu đang logout, không thêm token
      if (authStore.isLoggingOut && !isLogoutEndpoint) {
        return config
      }

      const isAdminRoute = router.currentRoute.value.path.startsWith('/admin')
      const token = isAdminRoute ? authStore.adminToken : authStore.userToken

      if (token) {
        config.headers.Authorization = `Bearer ${token}`
        console.log('Token added to request:', config.url)
      } else {
        console.log('No token available for:', config.url)
      }

      return config
    },
    (error) => Promise.reject(error),
  )

  // Response interceptor
  apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
      const authStore = useAuthStore()
      const toast = useToast()
      const originalRequest = error.config || {}

      const status = error.response?.status

      // Nếu là 401 Unauthorized
      if (status === 401) {
        const requestUrl = (originalRequest.url || '').toLowerCase()

        // Nếu là login endpoint bị 401, KHÔNG clear auth (đó là lỗi credentials)
        if (requestUrl.includes('/login')) {
          console.error('Login failed - invalid credentials')
          return Promise.reject(error) // Để LoginView xử lý
        }

        // If logout endpoint itself failed (or we've already retried), just clear local state and redirect
        if (requestUrl.includes('/logout') || originalRequest.__isRetry) {
          try {
            authStore.clearLocalAuth()
          } catch (e) {
            console.error('clearLocalAuth error:', e)
          }
          toast.error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
          const isAdmin = router.currentRoute.value.path.startsWith('/admin')
          router.push(isAdmin ? '/admin/login' : '/auth/login')
          return Promise.reject(error)
        }

        // Mark as retried to avoid loops
        originalRequest.__isRetry = true

        // Nếu đang logout (đã set cờ) thì chỉ clear
        if (authStore.isLoggingOut) {
          try {
            authStore.clearLocalAuth()
          } catch (e) {
            console.error('Error clearing auth on 401 during logout:', e)
          }
          toast.error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
          const isAdmin = router.currentRoute.value.path.startsWith('/admin')
          router.push(isAdmin ? '/admin/login' : '/auth/login')
          return Promise.reject(error)
        }

        // Bình thường: clear local state (không gọi logout API để tránh gây thêm 401)
        try {
          authStore.clearLocalAuth()
        } catch (e) {
          console.error('Error clearing auth on 401:', e)
        }

        toast.error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
        const isAdmin = router.currentRoute.value.path.startsWith('/admin')
        router.push(isAdmin ? '/admin/login' : '/auth/login')

        return Promise.reject(error)
      }

      // 403 Forbidden
      if (status === 403) {
        toast.error('Bạn không có quyền truy cập tài nguyên này.')
        router.push('/')
      }

      // Các lỗi khác
      return Promise.reject(error)
    },
  )
}
