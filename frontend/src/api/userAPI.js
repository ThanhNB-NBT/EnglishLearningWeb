import { api } from './index';

export const userAPI = {
  getAllUsers: () => api.get('/users'),
  getUserById: (id) => api.get(`/users/${id}`),
  getUserByUsername: (username) => api.get(`/users/username/${username}`),
  getUserByEmail: (email) => api.get(`/users/email/${email}`),
  createUser: (userData) => api.post('/users', userData),
  updateUser: (id, userData) => api.put(`/users/${id}`, userData),
  deleteUser: (id) => api.delete(`/users/${id}`),
  blockUser: (id) => api.put(`/users/${id}/block`),
  unblockUser: (id) => api.put(`/users/${id}/unblock`),
  addPoints: (id, points) => api.put(`/users/${id}/points?points=${points}`),
  getActiveUsers: () => api.get('/users/active'),
  getTopUsersByPoints: (minPoints = 0) => api.get(`/users/top-points?minPoints=${minPoints}`),
  getTopUsersByStreak: (minStreakDays = 0) => api.get(`/users/top-streak?minStreakDays=${minStreakDays}`),
  updateLastLogin: (username) => api.put(`/users/${username}/last-login`),
  updateStreakDays: (id, streakDays) => api.put(`/users/${id}/streak?streakDays=${streakDays}`),
};