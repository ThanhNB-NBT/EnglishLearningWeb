// File: src/api/interceptors.js - ENHANCED VERSION
import apiClient from './config'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'vue-toastification'
import router from '@/router'

const MAX_RETRIES = 3
const RETRY_DELAY_MS = 200

// Helper to check if error is optimistic lock related
function isOptimisticLockError(error) {
  if (error.response?.status === 409) return true

  const message = error.response?.data?.message || ''
  const lowerMessage = message.toLowerCase()

  return (
    lowerMessage.includes('optimistic') ||
    lowerMessage.includes('updated or deleted by another transaction') ||
    lowerMessage.includes('objectoptimisticlockingfailureexception') ||
    lowerMessage.includes('row was updated or deleted')
  )
}

// Delay function for retry
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

export function setupInterceptors() {
  // ==================== REQUEST INTERCEPTOR ====================
  apiClient.interceptors.request.use(
    (config) => {
      const authStore = useAuthStore()

      const isLogoutEndpoint = config.url?.includes('/logout')
      const isAuthEndpoint = config.url?.includes('/auth/') && !isLogoutEndpoint
      const isAdminCreate = config.url?.includes('/auth/admin/create')

      // Skip token cho public auth endpoints
      if (isAuthEndpoint && !isAdminCreate) {
        return config
      }

      // Nếu đang logout, không thêm token
      if (authStore.isLoggingOut && !isLogoutEndpoint) {
        return config
      }

      // Initialize retry count
      if (!config.retryCount) {
        config.retryCount = 0
      }

      // Determine which token to use
      const isAdminRoute = router.currentRoute.value.path.startsWith('/admin')
      const token = isAdminRoute ? authStore.adminToken : authStore.userToken

      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }

      return config
    },
    (error) => {
      console.error('Request interceptor error:', error)
      return Promise.reject(error)
    },
  )

  // ==================== RESPONSE INTERCEPTOR ====================
  apiClient.interceptors.response.use(
    (response) => {
      return response
    },
    async (error) => {
      const authStore = useAuthStore()
      const toast = useToast()
      const originalRequest = error.config || {}

      const status = error.response?.status
      const errorData = error.response?.data || {}
      const errorMessage = errorData?.message || ''

      // Handle Optimistic Lock Errors (409 or specific messages)
      if (isOptimisticLockError(error) && originalRequest.retryCount < MAX_RETRIES) {
        originalRequest.retryCount += 1
        const delayMs = RETRY_DELAY_MS * originalRequest.retryCount // exponential backoff

        console.warn(
          `⚠️ Optimistic lock detected, retrying request (${originalRequest.retryCount}/${MAX_RETRIES})...`,
          originalRequest.url,
        )

        // Show subtle notification on first retry
        if (originalRequest.retryCount === 1) {
          toast.info('Đang xử lý yêu cầu, vui lòng đợi...', {
            timeout: 1500,
            hideProgressBar: true,
          })
        }

        await delay(delayMs)
        return apiClient(originalRequest)
      }

      // Show error if retries exhausted
      if (isOptimisticLockError(error) && originalRequest.retryCount >= MAX_RETRIES) {
        console.error('❌ Max retries reached for optimistic lock error')
        toast.error('Yêu cầu thất bại sau nhiều lần thử. Vui lòng thử lại sau.')
        return Promise.reject(error)
      }

      // HANDLE 401: Unauthorized
      if (status === 401) {
        const requestUrl = (originalRequest.url || '').toLowerCase()

        // CASE 1: Login endpoint failed
        if (requestUrl.includes('/login')) {
          console.log('❌ LOGIN FAILED - do not clear auth')
          return Promise.reject(error)
        }

        // CASE 2: Logout failed or retry
        if (requestUrl.includes('/logout') || originalRequest.__isRetry) {
          console.log('⚠️ LOGOUT FAILED or RETRY')
          try {
            authStore.clearLocalAuth()
            console.log('✅ Auth cleared')
          } catch (e) {
            console.error('Error clearing auth:', e)
          }

          toast.error(getErrorMessage(errorMessage))
          redirectToLogin()
          return Promise.reject(error)
        }

        // Mark as retried
        originalRequest.__isRetry = true

        // CASE 3: During logout
        if (authStore.isLoggingOut) {
          console.log('⚠️ DURING LOGOUT')
          try {
            authStore.clearLocalAuth()
          } catch (e) {
            console.error('Error clearing auth:', e)
          }
          toast.error('Phiên đăng nhập đã hết hạn')
          redirectToLogin()
          return Promise.reject(error)
        }

        // CASE 4: Normal 401
        console.log('⚠️ NORMAL 401 - clearing auth')

        try {
          authStore.clearLocalAuth()
          console.log('✅ Auth cleared successfully')
        } catch (e) {
          console.error('Error clearing auth:', e)
        }

        const message = getErrorMessage(errorMessage)
        toast.error(message)

        redirectToLogin()
        return Promise.reject(error)
      }

      // HANDLE 403
      if (status === 403) {
        toast.error(errorMessage || 'Bạn không có quyền thực hiện hành động này', {
          timeout: 5000,
        })
        console.error('❌ 403 Permission denied:', errorMessage)
        return Promise.reject(error)
      }

      // HANDLE 500+ (Server errors)
      if (status >= 500) {
        console.error('❌ Server error:', status, errorMessage)
        toast.error('Lỗi server. Vui lòng thử lại sau.')
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
