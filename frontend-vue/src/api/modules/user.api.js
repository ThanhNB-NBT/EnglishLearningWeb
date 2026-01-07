import axiosClient from '@/api/config'

export const userAPI = {
  // ==================== 1. UNIFIED USER ENDPOINTS (/me) ====================
  // Dành cho User, Admin, Teacher tự thao tác trên tài khoản của chính mình

  /**
   * Lấy thông tin cá nhân cơ bản (Profile)
   * GET /api/users/me
   */
  getProfile() {
    return axiosClient.get('/api/users/me')
  },

  /**
   * Lấy thống kê (Điểm, Level...)
   * GET /api/users/me/stats
   */
  getStats() {
    return axiosClient.get('/api/users/me/stats')
  },

  /**
   * Lấy lịch sử hoạt động (Lần đăng nhập cuối...)
   * GET /api/users/me/activity
   */
  getActivity() {
    return axiosClient.get('/api/users/me/activity')
  },

  /**
   * Lấy thông tin Streak chi tiết
   * GET /api/users/me/streak
   */
  getStreakInfo() {
    return axiosClient.get('/api/users/me/streak')
  },

  /**
   * Cập nhật thông tin cá nhân (Chỉ cho phép sửa FullName)
   * PUT /api/users/me
   * Payload: { fullName: "..." }
   */
  updateProfile(data) {
    return axiosClient.put('/api/users/me', data)
  },

  /**
   * Đổi mật khẩu
   * PUT /api/users/me/change-password
   * Payload: { oldPassword, newPassword, confirmPassword }
   */
  changePassword(data) {
    return axiosClient.put('/api/users/me/change-password', data)
  },

  // ==================== 2. PUBLIC / COMMON ENDPOINTS ====================

  /**
   * Lấy thông tin public của user khác qua username
   * GET /api/users/username/{username}
   */
  getUserByUsername(username) {
    return axiosClient.get(`/api/users/username/${username}`)
  },

  // ==================== 3. ADMIN ONLY ENDPOINTS ====================

  /**
   * Lấy danh sách tất cả user
   * GET /api/users
   */
  getAllUsers() {
    return axiosClient.get('/api/users')
  },

  /**
   * Lấy user theo ID
   * GET /api/users/{id}
   */
  getUserById(id) {
    return axiosClient.get(`/api/users/${id}`)
  },

  /**
   * Lấy user theo Email
   * GET /api/users/email/{email}
   */
  getUserByEmail(email) {
    return axiosClient.get(`/api/users/email/${email}`)
  },

  /**
   * Xóa user
   * DELETE /api/users/{id}
   */
  deleteUser(id) {
    return axiosClient.delete(`/api/users/${id}`)
  },

  /**
   * Khóa user
   * PUT /api/users/{id}/block
   */
  blockUser(id) {
    return axiosClient.put(`/api/users/${id}/block`)
  },

  /**
   * Mở khóa user
   * PUT /api/users/{id}/unblock
   */
  unblockUser(id) {
    return axiosClient.put(`/api/users/${id}/unblock`)
  },

  /**
   * Cộng điểm cho user (Admin cheat/reward)
   * PUT /api/users/{id}/points?points=...
   */
  addPoints(id, points) {
    return axiosClient.put(`/api/users/${id}/points`, null, {
      params: { points }
    })
  },

  /**
   * Chỉnh sửa streak (Admin fix)
   * PUT /api/users/{id}/streak?streakDays=...
   */
  updateStreakDays(id, streakDays) {
    return axiosClient.put(`/api/users/${id}/streak`, null, {
      params: { streakDays }
    })
  },

  /**
   * Lấy danh sách user đang online/active
   * GET /api/users/active
   */
  getActiveUsers() {
    return axiosClient.get('/api/users/active')
  },

  /**
   * Lấy Top user theo điểm
   * GET /api/users/top-points
   */
  getTopUsersByPoints(minPoints = 0) {
    return axiosClient.get('/api/users/top-points', {
      params: { minPoints }
    })
  },

  /**
   * Lấy Top user theo chuỗi streak
   * GET /api/users/top-streak
   */
  getTopUsersByStreak(minStreakDays = 0) {
    return axiosClient.get('/api/users/top-streak', {
      params: { minStreakDays }
    })
  }
}
