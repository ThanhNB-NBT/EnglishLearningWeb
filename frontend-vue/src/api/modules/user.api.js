import apiClient from '../config'

export const userAPI = {
  // ==================== USER SELF-SERVICE (AUTHENTICATED) ====================

  /**
   * Get current user info (from token)
   * GET /api/users/me
   */
  getCurrentUser: () => apiClient.get('/api/users/me'),

  /**
   * Update current user info (only self)
   * PUT /api/users/me
   */
  updateCurrentUser: (userData) => apiClient.put('/api/users/me', userData),

  /**
   * Change password (only self)
   * PUT /api/users/me/change-password
   * Body: { oldPassword, newPassword, confirmPassword }
   */
  changePassword: (passwordData) => {
    return apiClient.put('/api/users/me/change-password', {
      oldPassword: passwordData.oldPassword,
      newPassword: passwordData.newPassword,
      confirmPassword: passwordData.confirmPassword,
    })
  },

  /**
   * Get user by username (public/authenticated)
   * GET /api/users/username/{username}
   */
  getUserByUsername: (username) => apiClient.get(`/api/users/username/${username}`),

  // ==================== ADMIN ONLY ENDPOINTS ====================

  /**
   * Get all users (Admin only)
   * GET /api/users
   */
  getAllUsers: () => apiClient.get('/api/users'),

  /**
   * Get user by ID (Admin only)
   * GET /api/users/{id}
   */
  getUserById: (id) => apiClient.get(`/api/users/${id}`),

  /**
   * Get user by email (Admin only)
   * GET /api/users/email/{email}
   */
  getUserByEmail: (email) => apiClient.get(`/api/users/email/${email}`),

  /**
   * Delete user (Admin only)
   * DELETE /api/users/{id}
   */
  deleteUser: (id) => apiClient.delete(`/api/users/${id}`),

  /**
   * Block user (Admin only)
   * PUT /api/users/{id}/block
   */
  blockUser: (id) => apiClient.put(`/api/users/${id}/block`),

  /**
   * Unblock user (Admin only)
   * PUT /api/users/{id}/unblock
   */
  unblockUser: (id) => apiClient.put(`/api/users/${id}/unblock`),

  /**
   * Add points to user (Admin only)
   * PUT /api/users/{id}/points?points={points}
   */
  addPoints: (id, points) => apiClient.put(`/api/users/${id}/points?points=${points}`),

  /**
   * Update user streak days (Admin only)
   * PUT /api/users/{id}/streak?streakDays={streakDays}
   */
  updateStreakDays: (id, streakDays) => apiClient.put(`/api/users/${id}/streak?streakDays=${streakDays}`),

  /**
   * Update last login (Admin only - internal use)
   * PUT /api/users/{id}/last-login
   */
  updateLastLogin: (id) => apiClient.put(`/api/users/${id}/last-login`),

  // ==================== QUERIES (ADMIN ONLY) ====================

  /**
   * Get active users (Admin only)
   * GET /api/users/active
   */
  getActiveUsers: () => apiClient.get('/api/users/active'),

  /**
   * Get top users by points (Admin only)
   * GET /api/users/top-points?minPoints={minPoints}
   */
  getTopUsersByPoints: (minPoints = 0) => apiClient.get(`/api/users/top-points?minPoints=${minPoints}`),

  /**
   * Get top users by streak (Admin only)
   * GET /api/users/top-streak?minStreakDays={minStreakDays}
   */
  getTopUsersByStreak: (minStreakDays = 0) => apiClient.get(`/api/users/top-streak?minStreakDays=${minStreakDays}`),
}
