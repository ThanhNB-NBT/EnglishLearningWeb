// File: src/api/interceptors.js - DEBUG VERSION
import apiClient from './config'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'vue-toastification'
import router from '@/router'

export function setupInterceptors() {
  // ==================== REQUEST INTERCEPTOR ====================
  apiClient.interceptors.request.use(
    (config) => {
      const authStore = useAuthStore()

      console.group('📤 REQUEST INTERCEPTOR')
      console.log('URL:', config.url)
      console.log('Method:', config.method)
      console.log('Current route:', router.currentRoute.value.path)

      const isLogoutEndpoint = config.url?.includes('/logout')
      const isAuthEndpoint = config.url?.includes('/auth/') && !isLogoutEndpoint
      const isAdminCreate = config.url?.includes('/auth/admin/create')

      // Skip token cho public auth endpoints
      if (isAuthEndpoint && !isAdminCreate) {
        console.log('⚪ Auth endpoint - SKIP TOKEN')
        console.groupEnd()
        return config
      }

      // Nếu đang logout, không thêm token
      if (authStore.isLoggingOut && !isLogoutEndpoint) {
        console.log('⚠️ Logging out - SKIP TOKEN')
        console.groupEnd()
        return config
      }

      // Determine which token to use
      const isAdminRoute = router.currentRoute.value.path.startsWith('/admin')
      const token = isAdminRoute ? authStore.adminToken : authStore.userToken

      if (token) {
        config.headers.Authorization = `Bearer ${token}`
        console.log('✅ TOKEN ADDED')
      } else {
        console.error('❌ NO TOKEN AVAILABLE!')
      }

      console.groupEnd()
      return config
    },
    (error) => {
      console.error('❌ Request interceptor error:', error)
      return Promise.reject(error)
    },
  )

  // ==================== RESPONSE INTERCEPTOR ====================
  apiClient.interceptors.response.use(
    (response) => {
      console.log('✅ Response OK:', response.config.url, response.status)
      return response
    },
    async (error) => {
      const authStore = useAuthStore()
      const toast = useToast()
      const originalRequest = error.config || {}

      const status = error.response?.status
      const errorData = error.response?.data || {}
      const errorMessage = errorData?.message || ''

      console.group('❌ RESPONSE ERROR')
      console.log('Status:', status)
      console.log('URL:', originalRequest.url)
      console.log('Message:', errorMessage)
      console.log('Full error data:', errorData)
      console.log('Is retry:', originalRequest.__isRetry)
      console.groupEnd()

      // ✅ HANDLE 401: Unauthorized
      if (status === 401) {
        const requestUrl = (originalRequest.url || '').toLowerCase()

        // ✅ CASE 1: Login endpoint failed
        if (requestUrl.includes('/login')) {
          console.log('Case: LOGIN FAILED - do not clear auth')
          console.groupEnd()
          return Promise.reject(error)
        }

        // ✅ CASE 2: Logout failed or retry
        if (requestUrl.includes('/logout') || originalRequest.__isRetry) {
          console.log('Case: LOGOUT FAILED or RETRY')
          try {
            authStore.clearLocalAuth()
            console.log('✅ Auth cleared')
          } catch (e) {
            console.error('❌ Error clearing auth:', e)
          }

          toast.error(getErrorMessage(errorMessage))
          redirectToLogin()
          console.groupEnd()
          return Promise.reject(error)
        }

        // Mark as retried
        originalRequest.__isRetry = true

        // ✅ CASE 3: During logout
        if (authStore.isLoggingOut) {
          console.log('Case: DURING LOGOUT')
          try {
            authStore.clearLocalAuth()
          } catch (e) {
            console.error('Error clearing auth:', e)
          }
          toast.error('Phiên đăng nhập đã hết hạn')
          redirectToLogin()
          console.groupEnd()
          return Promise.reject(error)
        }

        // ✅ CASE 4: Normal 401
        console.log('Case: NORMAL 401 - clearing auth')

        try {
          authStore.clearLocalAuth()
          console.log('✅ Auth cleared successfully')
        } catch (e) {
          console.error('❌ Error clearing auth:', e)
        }

        const message = getErrorMessage(errorMessage)
        console.log('Toast message:', message)
        toast.error(message)

        redirectToLogin()
        console.groupEnd()
        return Promise.reject(error)
      }

      // ✅ HANDLE 403
      if (status === 403) {
        console.warn('🚫 403 Forbidden')
        toast.error('Bạn không có quyền truy cập')
        router.push('/')
        return Promise.reject(error)
      }

      return Promise.reject(error)
    },
  )
}

function getErrorMessage(backendMessage) {
  if (!backendMessage) {
    return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
  }

  const messageMap = {
    'Tài khoản đã bị khóa': 'Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.',
    'Tài khoản không tồn tại': 'Tài khoản không tồn tại trong hệ thống.',
    'Tài khoản chưa được xác thực': 'Vui lòng xác thực email trước khi đăng nhập.',
    'Phiên đăng nhập đã hết hạn': 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
    'Token đã bị vô hiệu hóa': 'Phiên đăng nhập không còn hợp lệ.',
    'Token không hợp lệ': 'Phiên đăng nhập không hợp lệ.',
    'Thiếu token xác thực': 'Vui lòng đăng nhập để tiếp tục.',
    'Invalid or expired token': 'Phiên đăng nhập đã hết hạn.',
    'Token has been revoked': 'Phiên đăng nhập đã bị thu hồi.',
    'Missing authentication token': 'Vui lòng đăng nhập để tiếp tục.',
  }

  for (const [key, value] of Object.entries(messageMap)) {
    if (backendMessage.includes(key)) {
      return value
    }
  }

  return backendMessage
}

function redirectToLogin() {
  const isAdminRoute = router.currentRoute.value.path.startsWith('/admin')
  const loginPath = isAdminRoute ? '/admin/login' : '/auth/login'

  if (router.currentRoute.value.path !== loginPath) {
    router.push(loginPath)
  }
}
