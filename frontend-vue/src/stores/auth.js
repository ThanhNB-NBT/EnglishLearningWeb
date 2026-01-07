import { defineStore } from 'pinia'
import { authAPI } from '@/api/modules/auth.api.js'
import { userAPI } from '@/api/modules/user.api.js'
import { useToast } from 'vue-toastification'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    // Tokens
    userToken: localStorage.getItem('userToken'),
    adminToken: localStorage.getItem('adminToken'),
    teacherToken: localStorage.getItem('teacherToken'),

    // User Data objects
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    admin: JSON.parse(localStorage.getItem('admin') || 'null'),
    teacher: JSON.parse(localStorage.getItem('teacher') || 'null'),

    // Authenticated Flags
    isUserAuthenticated: !!localStorage.getItem('userToken'),
    isAdminAuthenticated: !!localStorage.getItem('adminToken'),
    isTeacherAuthenticated: !!localStorage.getItem('teacherToken'),

    isLoggingOut: false,
  }),

  getters: {
    // Helper xác định role hiện tại dựa trên URL
    currentRole() {
      const path = window.location.pathname
      if (path.startsWith('/admin')) return 'ADMIN'
      if (path.startsWith('/teacher')) return 'TEACHER'
      return 'USER'
    },

    currentUser() {
      if (this.currentRole === 'ADMIN') return this.admin
      if (this.currentRole === 'TEACHER') return this.teacher
      return this.user
    },

    isLoggedIn() {
      if (this.currentRole === 'ADMIN') return this.isAdminAuthenticated
      if (this.currentRole === 'TEACHER') return this.isTeacherAuthenticated
      return this.isUserAuthenticated
    },

    hasRole: (state) => (role) => state.currentUser?.role === role,

    userLevel: (state) => state.currentUser?.englishLevel || null,

    isLevelSufficient: (state) => (requiredLevel) => {
      if (!requiredLevel) return true
      if (!state.currentUser?.englishLevel) return false

      const levels = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']
      const userLevelIndex = levels.indexOf(state.currentUser.englishLevel)
      const requiredLevelIndex = levels.indexOf(requiredLevel)

      return userLevelIndex >= requiredLevelIndex
    }
  },

  actions: {
    // ==================== 1. INITIALIZATION & SYNC ====================

    // Được gọi bởi roleGuard để đồng bộ state từ localStorage
    checkAuth() {
      if (localStorage.getItem('userToken')) {
        this.isUserAuthenticated = true
        try {
          this.user = JSON.parse(localStorage.getItem('user'))
        } catch (e) {
          console.error('Error parsing user from localStorage', e)
        }
      }
      if (localStorage.getItem('adminToken')) {
        this.isAdminAuthenticated = true
        try {
          this.admin = JSON.parse(localStorage.getItem('admin'))
        } catch (e) {
          console.error('Error parsing admin from localStorage', e)
        }
      }
      if (localStorage.getItem('teacherToken')) {
        this.isTeacherAuthenticated = true
        try {
          this.teacher = JSON.parse(localStorage.getItem('teacher'))
        } catch (e) {
          console.error('Error parsing teacher from localStorage', e)
        }
      }
    },

    // ==================== 2. CLEAR DATA ACTIONS (Fix lỗi của bạn tại đây) ====================
    // RoleGuard gọi các hàm này khi token hết hạn hoặc lỗi data

    clearUser() {
      this.user = null
      this.userToken = null
      this.isUserAuthenticated = false
      localStorage.removeItem('user')
      localStorage.removeItem('userToken')
    },

    clearAdmin() {
      this.admin = null
      this.adminToken = null
      this.isAdminAuthenticated = false
      localStorage.removeItem('admin')
      localStorage.removeItem('adminToken')
    },

    clearTeacher() {
      this.teacher = null
      this.teacherToken = null
      this.isTeacherAuthenticated = false
      localStorage.removeItem('teacher')
      localStorage.removeItem('teacherToken')
    },

    // ==================== 3. PROFILE MANAGEMENT ====================

    async fetchProfile() {
      try {
        // Gọi song song 3 API để lấy full data
        const [profileRes, statsRes, activityRes] = await Promise.allSettled([
          userAPI.getProfile(),
          userAPI.getStats(),
          userAPI.getActivity(),
        ])

        if (profileRes.status === 'rejected') throw profileRes.reason
        const baseProfile = profileRes.value.data.data

        // Stats & Activity có thể null nếu backend chưa tạo record
        const stats = statsRes.status === 'fulfilled' ? statsRes.value.data.data : {}
        const activity = activityRes.status === 'fulfilled' ? activityRes.value.data.data : {}

        const fullProfile = {
          ...baseProfile,
          stats: stats,
          activity: activity,
        }

        this.updateLocalState(fullProfile)
        return fullProfile
      } catch (error) {
        console.error('Fetch profile error:', error)
        if (error.response?.status === 401) {
          // Token hỏng -> Clear đúng role
          if (this.currentRole === 'ADMIN') this.clearAdmin()
          else if (this.currentRole === 'TEACHER') this.clearTeacher()
          else this.clearUser()
        }
        return null
      }
    },

    async updateProfile(data) {
      const toast = useToast()
      try {
        const response = await userAPI.updateProfile(data)
        const updatedBase = response.data.data

        // Merge data mới vào data cũ để không mất stats/activity
        const currentData = this.currentUser || {}
        const newProfile = { ...currentData, ...updatedBase }

        this.updateLocalState(newProfile)

        toast.success('Cập nhật thông tin thành công!')
        return newProfile
      } catch (error) {
        toast.error(error.response?.data?.message || 'Cập nhật thất bại')
        throw error
      }
    },

    async changePassword(data) {
      const toast = useToast()
      try {
        await userAPI.changePassword(data)
        toast.success('Đổi mật khẩu thành công!')
        return true
      } catch (error) {
        toast.error(error.response?.data?.message || 'Đổi mật khẩu thất bại')
        throw error
      }
    },

    // Helper update state
    updateLocalState(profileData) {
      const role = profileData.role
      if (role === 'ADMIN') {
        this.admin = profileData
        localStorage.setItem('admin', JSON.stringify(profileData))
      } else if (role === 'TEACHER') {
        this.teacher = profileData
        localStorage.setItem('teacher', JSON.stringify(profileData))
      } else {
        this.user = profileData
        localStorage.setItem('user', JSON.stringify(profileData))
      }
    },

    // ==================== 4. LOGIN ACTIONS ====================

    async loginUser(credentials) {
      return this.handleLogin(authAPI.loginUser, credentials, 'user')
    },
    async loginAdmin(credentials) {
      return this.handleLogin(authAPI.loginAdmin, credentials, 'admin')
    },
    async loginTeacher(credentials) {
      return this.handleLogin(authAPI.loginTeacher, credentials, 'teacher')
    },

    async handleLogin(apiFunc, credentials, type) {
      const toast = useToast()
      try {
        const response = await apiFunc(credentials)
        const data = response.data.data

        const tokenKey = `${type}Token`
        this[tokenKey] = data.token
        localStorage.setItem(tokenKey, data.token)

        if (type === 'user') this.isUserAuthenticated = true
        if (type === 'admin') this.isAdminAuthenticated = true
        if (type === 'teacher') this.isTeacherAuthenticated = true

        // Lấy profile ngay lập tức để có role check
        await this.fetchProfile()

        toast.success(`Đăng nhập thành công!`)
        return response
      } catch (error) {
        const msg = error.response?.data?.message || 'Đăng nhập thất bại'
        // Chỉ toast lỗi nếu không phải lỗi quyền (vì roleGuard hoặc view sẽ handle lỗi quyền riêng)
        if (!msg.includes('quyền')) toast.error(msg)
        throw error
      }
    },

    // ==================== 5. REGISTER & AUTH ACTIONS ====================

    async register(data) {
      const toast = useToast()
      try {
        const res = await authAPI.registerUser(data)
        toast.success('Đăng ký thành công! Vui lòng kiểm tra email.')
        return res
      } catch (error) {
        toast.error(error.response?.data?.message || 'Đăng ký thất bại')
        throw error
      }
    },

    async verifyEmail(email, otp) {
      return await authAPI.verifyEmail({ email, otp })
    },

    async resendVerifyEmail(email) {
      const toast = useToast()
      try {
        await authAPI.resendVerifyEmail({ email })
        toast.success('Đã gửi lại mã OTP!')
      } catch (error) {
        toast.error(error.response?.data?.message || 'Gửi lại mã thất bại')
        throw error
      }
    },

    async forgotPassword(email) {
      const toast = useToast()
      try {
        const res = await authAPI.forgotPassword({ email })
        toast.success('Đã gửi OTP khôi phục mật khẩu!')
        return res
      } catch (error) {
        toast.error(error.response?.data?.message || 'Gửi yêu cầu thất bại')
        throw error
      }
    },

    async verifyResetPassword(email, otp) {
      return await authAPI.verifyResetPassword({ email, otp })
    },

    async resetPassword(email, newPassword) {
      const toast = useToast()
      try {
        const res = await authAPI.resetPassword({ email, newPassword })
        toast.success('Đặt lại mật khẩu thành công!')
        return res
      } catch (error) {
        toast.error(error.response?.data?.message || 'Đặt lại mật khẩu thất bại')
        throw error
      }
    },

    // ==================== 6. LOGOUT ====================

    async logout(role) {
      this.isLoggingOut = true
      const toast = useToast()
      try {
        if (role === 'ADMIN') await authAPI.logoutAdmin().catch(() => {})
        else if (role === 'TEACHER') await authAPI.logoutTeacher().catch(() => {})
        else await authAPI.logoutUser().catch(() => {})
      } finally {
        // Clear local state
        if (role === 'ADMIN') this.clearAdmin()
        else if (role === 'TEACHER') this.clearTeacher()
        else this.clearUser() // Default clear user

        this.isLoggingOut = false
        toast.info('Đã đăng xuất')

        // Chuyển hướng về trang login tương ứng
        const loginPath = role === 'ADMIN' ? '/admin/login' : '/auth/login'
        window.location.href = loginPath
      }
    },
  },
})
