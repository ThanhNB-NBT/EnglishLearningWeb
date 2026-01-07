import apiClient from '@/api/config'

export const authAPI = {
  // ==================== USER ENDPOINTS ====================

  /**
   * Register new user
   * POST /api/auth/user/register
   */
  registerUser: (data) => {
    return apiClient.post('/api/auth/user/register', {
      username: data.username,
      email: data.email,
      password: data.password,
      fullName: data.fullName || data.username
    })
  },

  /**
   * Login user
   * POST /api/auth/user/login
   */
  loginUser: (credentials) => {
    return apiClient.post('/api/auth/user/login', {
      usernameOrEmail: credentials.usernameOrEmail,
      password: credentials.password
    })
  },

  /**
   * Verify email with OTP
   * POST /api/auth/user/verify-email
   */
  verifyEmail: (data) => {
    return apiClient.post('/api/auth/user/verify-email', {
      email: data.email,
      otp: data.otp
    })
  },

  /**
   * Resend verification OTP
   * POST /api/auth/user/resend-otp
   */
  resendVerifyEmail: (data) => {
    return apiClient.post('/api/auth/user/resend-verify-email', {
      email: data.email
    })
  },

  /**
   * Send password reset OTP
   * POST /api/auth/user/forgot-password
   */
  forgotPassword: (data) => {
    return apiClient.post('/api/auth/user/forgot-password', {
      email: data.email
    })
  },

  /**
   * Verify password reset OTP
   * POST /api/auth/user/verify-reset-password
   */
  verifyResetPassword: (data) => {
    return apiClient.post('/api/auth/user/verify-reset-password', {
      email: data.email,
      otp: data.otp
    })
  },

  /**
   * Reset password
   * POST /api/auth/user/reset-password
   */
  resetPassword: (data) => {
    return apiClient.post('/api/auth/user/reset-password', {
      email: data.email,
      newPassword: data.newPassword
    })
  },

  /**
   * Logout user
   * POST /api/auth/user/logout
   */
  logoutUser: () => {
    return apiClient.post('/api/auth/user/logout')
  },

  /**
   * Logout all user devices
   * POST /api/auth/user/logout-all
   */
  logoutUserAll: () => {
    return apiClient.post('/api/auth/user/logout-all')
  },

  // ==================== ADMIN ENDPOINTS ====================

  /**
   * Login admin
   * POST /api/auth/admin/login
   */
  loginAdmin: (credentials) => {
    return apiClient.post('/api/auth/admin/login', {
      usernameOrEmail: credentials.usernameOrEmail,
      password: credentials.password
    })
  },

  /**
   * Create new admin account
   * POST /api/auth/admin/create
   * Requires admin token
   */
  createAdmin: (data) => {
    return apiClient.post('/api/auth/admin/create', {
      username: data.username,
      email: data.email,
      password: data.password,
      fullName: data.fullName
    })
  },

  /**
   * Send password reset OTP (admin)
   * POST /api/auth/admin/forgot-password
   */
  forgotPasswordAdmin: (data) => {
    return apiClient.post('/api/auth/admin/forgot-password', {
      email: data.email
    })
  },

  /**
   * Verify password reset OTP (admin)
   * POST /api/auth/admin/verify-reset-password
   */
  verifyResetPasswordAdmin: (data) => {
    return apiClient.post('/api/auth/admin/verify-reset-password', {
      email: data.email,
      otp: data.otp
    })
  },

  /**
   * Reset password (admin)
   * POST /api/auth/admin/reset-password
   */
  resetPasswordAdmin: (data) => {
    return apiClient.post('/api/auth/admin/reset-password', {
      email: data.email,
      newPassword: data.newPassword
    })
  },

  /**
   * Logout admin
   * POST /api/auth/admin/logout
   */
  logoutAdmin: () => {
    return apiClient.post('/api/auth/admin/logout')
  },

  /**
   * Logout all admin devices
   * POST /api/auth/admin/logout-all
   */
  logoutAdminAll: () => {
    return apiClient.post('/api/auth/admin/logout-all')
  },

  // ==================== TEACHER ENDPOINTS ====================
  /**
   * Login teacher
   * POST /api/auth/teacher/login
   */
  loginTeacher: (credentials) => {
    return apiClient.post('/api/auth/teacher/login', {
      usernameOrEmail: credentials.usernameOrEmail,
      password: credentials.password
    })
  },

  /**
   * Create new teacher account (Admin only)
   * POST /api/auth/teacher/create
   */
  createTeacher: (data) => {
    return apiClient.post('/api/auth/teacher/create', {
      username: data.username,
      email: data.email,
      password: data.password,
      fullName: data.fullName
    })
  },

  /**
   * Logout teacher
   * POST /api/auth/teacher/logout
   */
  logoutTeacher:  () => {
    return apiClient.post('/api/auth/teacher/logout')
  },

  /**
   * Logout all teacher devices
   * POST /api/auth/teacher/logout-all
   */
  logoutTeacherAll: () => {
    return apiClient.post('/api/auth/teacher/logout-all')
  },

  // Password reset cho teacher (giống admin)
  forgotPasswordTeacher: (data) => {
    return apiClient.post('/api/auth/teacher/forgot-password', {
      email: data.email
    })
  },

  verifyResetPasswordTeacher: (data) => {
    return apiClient.post('/api/auth/teacher/verify-reset-password', {
      email:  data.email,
      otp: data.otp
    })
  },

  resetPasswordTeacher: (data) => {
    return apiClient.post('/api/auth/teacher/reset-password', {
      email:  data.email,
      newPassword: data.newPassword
    })
  },

  // ==================== LEGACY COMPATIBILITY ====================

  /**
   * @deprecated Use loginUser or loginAdmin instead
   */
  login: (credentials) => {
    console.warn('auth.login() is deprecated. Use loginUser() or loginAdmin()')
    return authAPI.loginUser(credentials)
  },

  /**
   * @deprecated Use logoutUser or logoutAdmin instead
   */
  logout: () => {
    const isAdmin = window.location.pathname.startsWith('/admin')
    return isAdmin ? authAPI.logoutAdmin() : authAPI.logoutUser()
  },

  /**
   * @deprecated Use registerUser instead (admin không có register endpoint)
   */
  register: (data) => {
    console.warn('auth.register() is deprecated. Use registerUser()')
    return authAPI.registerUser(data)
  },

  // ==================== ADMIN CLEANUP ====================
  /**
   * Get unverified users statistics
   * GET /api/admin/cleanup/stats
   */
  getCleanupStats: () => {
    return apiClient.get('/api/admin/cleanup/stats')
  },

  /**
   * Get list of unverified users
   * GET /api/admin/cleanup/unverified-users
   */
  getUnverifiedUsers: (limit = 50) => {
    return apiClient.get(`/api/admin/cleanup/unverified-users?limit=${limit}`)
  },


  /**
   * Preview cleanup - see how many accounts will be deleted
   * GET /api/admin/cleanup/preview
   */
  previewCleanup: () => {
    return apiClient.get('/api/admin/cleanup/preview')
  },

  /**
   * Run cleanup manually
   * POST /api/admin/cleanup/run-now
   */
  runCleanup: () => {
    return apiClient.post('/api/admin/cleanup/run-now')
  },
}
