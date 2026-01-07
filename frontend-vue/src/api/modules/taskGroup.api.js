// src/api/modules/taskGroup.api.js
import apiClient from '../config'

/**
 * TaskGroup API - Generic cho tất cả module types (Grammar, Listening, Reading)
 */
export const taskGroupAPI = {
  /**
   * Lấy tất cả TaskGroups của một lesson
   * @param {string} parentType - 'GRAMMAR_LESSON' | 'LISTENING_LESSON' | 'READING_LESSON'
   * @param {number} lessonId
   * @returns {Promise}
   */
  getTaskGroupsByLesson: (parentType, lessonId) =>
    apiClient.get(`/api/admin/task-groups/${parentType}/${lessonId}`),

  /**
   * Lấy chi tiết một TaskGroup
   * @param {number} taskGroupId
   * @returns {Promise}
   */
  getTaskGroupById: (taskGroupId) =>
    apiClient.get(`/api/admin/task-groups/${taskGroupId}`),

  /**
   * Tạo TaskGroup mới
   * @param {string} parentType
   * @param {number} lessonId
   * @param {object} data - { taskName, instruction, orderIndex }
   * @returns {Promise}
   */
  createTaskGroup: (parentType, lessonId, data) =>
    apiClient.post(`/api/admin/task-groups/${parentType}/${lessonId}`, data),

  /**
   * Cập nhật TaskGroup
   * @param {number} taskGroupId
   * @param {object} data - { taskName, instruction, orderIndex }
   * @returns {Promise}
   */
  updateTaskGroup: (taskGroupId, data) =>
    apiClient.put(`/api/admin/task-groups/${taskGroupId}`, data),

  /**
   * Xóa TaskGroup (cascade delete questions)
   * @param {number} taskGroupId
   * @returns {Promise}
   */
  deleteTaskGroup: (taskGroupId) =>
    apiClient.delete(`/api/admin/task-groups/${taskGroupId}`),
}
