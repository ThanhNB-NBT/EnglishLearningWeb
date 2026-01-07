import axios from 'axios'
import router from '@/router'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8980',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ✅ Retry configuration for optimistic lock errors
const MAX_RETRIES = 3
const RETRY_DELAY_MS = 200

// Helper: Check if error is optimistic lock related
function isOptimisticLockError(error) {
  if (error.response?.status === 409) return true

  const message = error.response?.data?.message || ''
  return (
    message.includes('optimistic') ||
    message.includes('updated or deleted by another transaction') ||
    message.includes('ObjectOptimisticLockingFailureException')
  )
}

// Helper: Delay function for retry
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

// ✅ Convert frontend pagination (1-indexed) to backend (0-indexed)
function convertPaginationParams(config) {
  if (config.params && config.params.page !== undefined) {
    const page = parseInt(config.params.page) || 1
    config.params.page = page > 0 ? page - 1 : 0
  }

  if (config.params) {
    const { sortBy, sortDir, ...restParams } = config.params

    if (sortBy) {
      const direction = sortDir || 'asc'
      config.params = {
        ...restParams,
        sort: `${sortBy}:${direction}`,
      }
    }
  }

  return config
}

// ==================== REQUEST INTERCEPTOR ====================
apiClient.interceptors.request.use(
  (config) => {
    convertPaginationParams(config)

    let token = null
    const currentPath = window.location.pathname
    const requestUrl = config.url || ''

    // ✅ Skip token for login/register endpoints
    const isAuthEndpoint =
      requestUrl.includes('/auth/user/login') ||
      requestUrl.includes('/auth/user/register') ||
      requestUrl.includes('/auth/admin/login') ||
      requestUrl.includes('/auth/teacher/login') ||
      requestUrl.includes('/auth/user/verify-email') ||
      requestUrl.includes('/auth/user/resend-verify-email') ||
      requestUrl.includes('/auth/user/forgot-password') ||
      requestUrl.includes('/auth/user/verify-reset-password') ||
      requestUrl.includes('/auth/user/reset-password') ||
      requestUrl.includes('/auth/admin/forgot-password') ||
      requestUrl.includes('/auth/admin/verify-reset-password') ||
      requestUrl.includes('/auth/admin/reset-password') ||
      requestUrl.includes('/auth/teacher/forgot-password') ||
      requestUrl.includes('/auth/teacher/verify-reset-password') ||
      requestUrl.includes('/auth/teacher/reset-password')

    if (isAuthEndpoint) {
      if (!config.retryCount) {
        config.retryCount = 0
      }
      return config
    }

    // ✅ Token selection based on CURRENT PATH
    if (currentPath.startsWith('/teacher')) {
      token = localStorage.getItem('teacherToken')
    } else if (currentPath.startsWith('/admin')) {
      token = localStorage.getItem('adminToken')
    } else {
      token = localStorage.getItem('userToken')
    }

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    if (!config.retryCount) {
      config.retryCount = 0
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// ==================== RESPONSE INTERCEPTOR ====================
apiClient.interceptors.response.use(
  (response) => {
    // ✅ Convert backend pagination (0-indexed) back to frontend (1-indexed)
    if (response.data?.data?.page !== undefined) {
      response.data.data.page = response.data.data.page + 1
    }

    return response
  },
  async (error) => {
    const originalRequest = error.config

    // ✅ Retry logic for optimistic lock errors
    if (isOptimisticLockError(error) && originalRequest.retryCount < MAX_RETRIES) {
      originalRequest.retryCount += 1
      const delayMs = RETRY_DELAY_MS * originalRequest.retryCount

      console.warn(
        `⚠️ Optimistic lock detected, retrying (${originalRequest.retryCount}/${MAX_RETRIES})...`,
        originalRequest.url,
      )

      await delay(delayMs)
      return apiClient(originalRequest)
    }

    // ✅ Handle 401 Unauthorized
    if (error.response?.status === 401) {
      const currentPath = window.location.pathname
      let loginPath = '/auth/login'

      if (currentPath.startsWith('/teacher')) {
        console.log('❌ Teacher auth failed, clearing teacher data')
        localStorage.removeItem('teacherToken')
        localStorage.removeItem('teacher')
        loginPath = '/teacher/login'
      } else if (currentPath.startsWith('/admin')) {
        console.log('❌ Admin auth failed, clearing admin data')
        localStorage.removeItem('adminToken')
        localStorage.removeItem('admin')
        loginPath = '/admin/login'
      } else {
        console.log('❌ User auth failed, clearing user data')
        localStorage.removeItem('userToken')
        localStorage.removeItem('user')
        loginPath = '/auth/login'
      }

      if (window.location.pathname !== loginPath) {
        router.push({
          path: loginPath,
          query: { redirect: currentPath },
        })
      }
    }

    // ✅ ENHANCED: Handle 403 Forbidden with role-specific messages
    if (error.response?.status === 403) {
      const currentPath = window.location.pathname
      const errorMessage = error.response.data?.message || 'Bạn không có quyền truy cập'

      console.error('❌ 403 Forbidden:', {
        path: currentPath,
        url: error.config.url,
        message: errorMessage,
      })

      // ✅ Show user-friendly message
      if (currentPath.startsWith('/teacher')) {
        // Teacher-specific error messages
        if (errorMessage.includes('topic') || errorMessage.includes('assignment') || errorMessage.includes('phân công')) {
          alert('⚠️ Bạn chưa được phân công quản lý nội dung này.\n\nVui lòng liên hệ Admin để được cấp quyền.')
        } else if (errorMessage.includes('permission') || errorMessage.includes('quyền')) {
          alert('⚠️ Chức năng này chỉ dành cho Admin.\n\nVui lòng liên hệ Admin nếu bạn cần truy cập.')
        } else {
          alert('⚠️ ' + errorMessage)
        }
      } else if (currentPath.startsWith('/admin')) {
        // Admin shouldn't see 403, but just in case
        alert('⚠️ Lỗi phân quyền: ' + errorMessage)
      } else {
        // User portal
        alert('⚠️ Bạn không có quyền truy cập chức năng này.')
      }
    }

    // Handle network errors
    if (!error.response) {
      console.error('❌ Network error:', error.message)
    }

    return Promise.reject(error)
  },
)

export default apiClient
