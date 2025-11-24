import { defineStore } from 'pinia'
import { authAPI } from '@/api/modules/auth.api.js'
import { useToast } from 'vue-toastification'

/**
 * Helper an toàn để parse JSON từ localStorage
 */
function safeJSONParse(value) {
  if (value === null || value === undefined) return null
  if (value === 'undefined' || value === 'null') return null
  try {
    return JSON.parse(value)
  } catch (e) {
    console.warn('safeJSONParse: invalid JSON value in localStorage', value, e)
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    userToken: localStorage.getItem('userToken') || null,
    adminToken: localStorage.getItem('adminToken') || null,
    user: safeJSONParse(localStorage.getItem('user')) || null,
    admin: safeJSONParse(localStorage.getItem('admin')) || null,
    isUserAuthenticated: !!localStorage.getItem('userToken'),
    isAdminAuthenticated: !!localStorage.getItem('adminToken'),
    isLoggingOut: false,
  }),

  getters: {
    // ✅ FIX: Getter cho user login
    isUserLoggedIn: (state) => state.isUserAuthenticated && !!state.user,

    // ✅ FIX: Getter cho admin login
    isAdminLoggedIn: (state) => state.isAdminAuthenticated && !!state.admin,

    currentUser: (state) => state.user,
    currentAdmin: (state) => state.admin,

    // ✅ FIX: Check role ADMIN từ cả user VÀ admin object
    isAdmin: (state) => {
      // Nếu đăng nhập qua admin endpoint
      if (state.admin?.role === 'ADMIN') return true
      // Hoặc nếu user có role ADMIN (case đăng nhập nhầm)
      if (state.user?.role === 'ADMIN') return true
      return false
    },
  },

  actions: {
    // Clear all auth data
    clearLocalAuth() {
      localStorage.removeItem('userToken')
      localStorage.removeItem('user')
      localStorage.removeItem('adminToken')
      localStorage.removeItem('admin')

      this.userToken = null
      this.user = null
      this.adminToken = null
      this.admin = null
      this.isUserAuthenticated = false
      this.isAdminAuthenticated = false

      console.log('Local auth data cleared')
    },

    // ==================== USER ACTIONS ====================

    async register(userData) {
      const toast = useToast()
      try {
        const response = await authAPI.registerUser(userData)
        toast.success('Đăng ký thành công! Vui lòng kiểm tra email để lấy mã xác thực.')
        return { success: true, data: response.data }
      } catch (error) {
        const message = error.response?.data?.message || 'Đăng ký thất bại! Vui lòng thử lại.'
        toast.error(message)
        throw error
      }
    },

    async login(credentials) {
      const toast = useToast()
      try {
        const response = await authAPI.loginUser(credentials)
        const authData = response.data.data
        const token = authData.token
        const user = {
          id: authData.id,
          username: authData.username,
          email: authData.email,
          fullName: authData.fullName,
          role: authData.role,
          totalPoints: authData.totalPoints || 0,
          streakDays: authData.streakDays || 0,
        }

        if (user.role === 'ADMIN') {
          toast.error('Vui lòng đăng nhập qua trang quản trị viên!')
          throw new Error('Admin should use admin login page')
        }

        localStorage.setItem('userToken', token)
        localStorage.setItem('user', JSON.stringify(user))

        this.userToken = token
        this.user = user
        this.isUserAuthenticated = true

        toast.success('Đăng nhập thành công!')
        return { success: true, user }
      } catch (error) {
        const message = error.response?.data?.message || 'Đăng nhập thất bại! Vui lòng thử lại.'
        if (!error.message?.includes('Admin should use')) {
          toast.error(message)
        }
        throw error
      }
    },

    async verifyEmail(email, otp) {
      const toast = useToast()
      try {
        const response = await authAPI.verifyEmail({ email, otp })
        toast.success('Xác thực email thành công! Bạn có thể đăng nhập ngay bây giờ.')
        return { success: true, data: response.data }
      } catch (error) {
        const message =
          error.response?.data?.message || 'Xác thực email thất bại! Vui lòng thử lại.'
        toast.error(message)
        throw error
      }
    },

    async resendVerifyEmail(email) {
      const toast = useToast()
      try {
        const response = await authAPI.resendVerifyEmail({ email })
        toast.success('Gửi lại mã xác thực thành công! Vui lòng kiểm tra email.')
        return { success: true, data: response.data }
      } catch (error) {
        const message =
          error.response?.data?.message || 'Gửi lại mã xác thực thất bại! Vui lòng thử lại.'
        toast.error(message)
        throw error
      }
    },

    async forgotPassword(email) {
      const toast = useToast()
      try {
        const response = await authAPI.forgotPassword({ email })
        toast.success('Mã OTP đặt lại mật khẩu đã được gửi đến email!')
        return { success: true, data: response.data }
      } catch (error) {
        const message = error.response?.data?.message || 'Gửi OTP thất bại'
        toast.error(message)
        throw error
      }
    },

    async verifyResetPassword(email, otp) {
      const toast = useToast()
      try {
        const response = await authAPI.verifyResetPassword({ email, otp })
        toast.success('Xác thực OTP thành công!')
        return { success: true, data: response.data }
      } catch (error) {
        const message = error.response?.data?.message || 'Xác thực OTP thất bại'
        toast.error(message)
        throw error
      }
    },

    async resetPassword(email, newPassword) {
      const toast = useToast()
      try {
        const response = await authAPI.resetPassword({ email, newPassword })
        toast.success('Đặt lại mật khẩu thành công! Bạn có thể đăng nhập với mật khẩu mới.')
        return { success: true, data: response.data }
      } catch (error) {
        const message = error.response?.data?.message || 'Đặt lại mật khẩu thất bại'
        toast.error(message)
        throw error
      }
    },

    async logoutUser() {
      const toast = useToast()
      if (this.isLoggingOut) return
      this.isLoggingOut = true
      try {
        await authAPI.logoutUser().catch((e) => {
          console.error('API logoutUser failed:', e)
        })
      } finally {
        this.clearLocalAuth()
        toast.success('Đăng xuất thành công!')
        this.isLoggingOut = false
      }
    },

    async logoutUserAll() {
      const toast = useToast()
      if (this.isLoggingOut) return
      this.isLoggingOut = true
      try {
        await authAPI.logoutUserAll().catch(() => {})
      } finally {
        this.clearLocalAuth()
        toast.success('Đã đăng xuất tất cả thiết bị!')
        this.isLoggingOut = false
      }
    },

    // ==================== ADMIN ACTIONS ====================

    async loginAdmin(credentials) {
      const toast = useToast()
      try {
        const response = await authAPI.loginAdmin(credentials)
        const authData = response.data.data
        const token = authData.token
        const admin = {
          id: authData.userId,
          username: authData.username,
          email: authData.email,
          fullName: authData.fullName,
          role: authData.role,
        }

        if (admin.role !== 'ADMIN') {
          toast.error('Tài khoản không có quyền quản trị!')
          throw new Error('Not an admin account')
        }

        localStorage.setItem('adminToken', token)
        localStorage.setItem('admin', JSON.stringify(admin))

        this.adminToken = token
        this.admin = admin
        this.isAdminAuthenticated = true

        toast.success('Đăng nhập quản trị thành công!')
        return { success: true, admin }
      } catch (error) {
        const message = error.response?.data?.message || 'Đăng nhập thất bại! Vui lòng thử lại.'
        if (!error.message?.includes('Not an admin')) {
          toast.error(message)
        }
        throw error
      }
    },

    async createAdmin(adminData) {
      const toast = useToast()
      try {
        const response = await authAPI.createAdmin(adminData)
        toast.success('Tạo tài khoản quản trị thành công!')
        return { success: true, data: response.data }
      } catch (error) {
        const message = error.response?.data?.message || 'Tạo tài khoản thất bại'
        toast.error(message)
        throw error
      }
    },

    async forgotPasswordAdmin(email) {
      const toast = useToast()
      try {
        const response = await authAPI.forgotPasswordAdmin({ email })
        toast.success('Mã OTP đặt lại mật khẩu đã được gửi đến email!')
        return { success: true, data: response.data }
      } catch (error) {
        const message = error.response?.data?.message || 'Gửi OTP thất bại'
        toast.error(message)
        throw error
      }
    },

    async verifyResetPasswordAdmin(email, otp) {
      const toast = useToast()
      try {
        const response = await authAPI.verifyResetPasswordAdmin({ email, otp })
        toast.success('Xác thực OTP thành công!')
        return { success: true, data: response.data }
      } catch (error) {
        const message = error.response?.data?.message || 'Xác thực OTP thất bại'
        toast.error(message)
        throw error
      }
    },

    async resetPasswordAdmin(email, newPassword) {
      const toast = useToast()
      try {
        const response = await authAPI.resetPasswordAdmin({ email, newPassword })
        toast.success('Đặt lại mật khẩu thành công!')
        return { success: true, data: response.data }
      } catch (error) {
        const message = error.response?.data?.message || 'Đặt lại mật khẩu thất bại'
        toast.error(message)
        throw error
      }
    },

    async logoutAdmin() {
      const toast = useToast()
      if (this.isLoggingOut) return
      this.isLoggingOut = true
      try {
        await authAPI.logoutAdmin().catch((e) => {
          console.warn('logoutAdmin api error (ignored):', e?.response?.status, e?.message)
        })
      } finally {
        this.clearLocalAuth()
        toast.success('Đăng xuất thành công!')
        this.isLoggingOut = false
      }
    },

    async logoutAdminAll() {
      const toast = useToast()
      if (this.isLoggingOut) return
      this.isLoggingOut = true
      try {
        await authAPI.logoutAdminAll().catch(() => {})
      } finally {
        this.clearLocalAuth()
        toast.success('Đã đăng xuất tất cả thiết bị!')
        this.isLoggingOut = false
      }
    },

    // ==================== COMMON METHODS ====================

    async logout(type = 'USER') {
      if (type === 'ADMIN') return this.logoutAdmin()
      return this.logoutUser()
    },

    async logoutAll(type = 'USER') {
      if (type === 'ADMIN') return this.logoutAdminAll()
      return this.logoutUserAll()
    },

    checkAuth(type = 'USER') {
      if (type === 'ADMIN') {
        const token = localStorage.getItem('adminToken')
        const admin = safeJSONParse(localStorage.getItem('admin'))
        if (token && admin) {
          this.adminToken = token
          this.admin = admin
          this.isAdminAuthenticated = true
          return true
        }
        this.isAdminAuthenticated = false
        return false
      } else {
        const token = localStorage.getItem('userToken')
        const user = safeJSONParse(localStorage.getItem('user'))
        if (token && user) {
          this.userToken = token
          this.user = user
          this.isUserAuthenticated = true
          return true
        }
        this.isUserAuthenticated = false
        return false
      }
    },
  },
})
