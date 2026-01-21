// src/api/modules/taskGroup.api.js
import api from '../config'

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
    api.get(`/api/admin/task-groups/${parentType}/${lessonId}`),

  /**
   * Lấy chi tiết một TaskGroup
   * @param {number} taskGroupId
   * @returns {Promise}
   */
  getTaskGroupById: (taskGroupId) =>
    api.get(`/api/admin/task-groups/${taskGroupId}`),

  /**
   * Tạo TaskGroup mới
   * @param {string} parentType
   * @param {number} lessonId
   * @param {object} data - { taskName, instruction, orderIndex }
   * @returns {Promise}
   */
  createTaskGroup: (parentType, lessonId, data) =>
    api.post(`/api/admin/task-groups/${parentType}/${lessonId}`, data),

  /**
   * Cập nhật TaskGroup
   * @param {number} taskGroupId
   * @param {object} data - { taskName, instruction, orderIndex }
   * @returns {Promise}
   */
  updateTaskGroup: (taskGroupId, data) =>
    api.put(`/api/admin/task-groups/${taskGroupId}`, data),

  /**
   * Xóa TaskGroup (cascade delete questions)
   * @param {number} taskGroupId
   * @returns {Promise}
   */
  deleteTaskGroup: (taskGroupId) =>
    api.delete(`/api/admin/task-groups/${taskGroupId}`),
}
