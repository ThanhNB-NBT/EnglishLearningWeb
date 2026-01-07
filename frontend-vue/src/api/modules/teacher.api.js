// src/api/modules/teacher.api.js - COMPLETE VERSION
import apiClient from '@/api/config'

/**
 * ✅ COMPLETE: Separate APIs for ADMIN and TEACHER
 */

// ==================== ADMIN OPERATIONS (Admin Portal Only) ====================
export const teacherAdminAPI = {
  /**
   * ✅ Assign teacher to topic (ADMIN ONLY)
   * POST /api/admin/teachers/assign
   */
  assignTeacher: (data) => {
    return apiClient.post('/api/admin/teachers/assign', {
      teacherId: data.teacherId,
      topicId: data.topicId,
    })
  },

  /**
   * ✅ Revoke assignment (ADMIN ONLY)
   * DELETE /api/admin/teachers/assignments/{assignmentId}
   */
  revokeAssignment: (assignmentId) => {
    return apiClient.delete(`/api/admin/teachers/assignments/${assignmentId}`)
  },

  /**
   * ✅ Hard delete assignment (ADMIN ONLY)
   * DELETE /api/admin/teachers/assignments/{teacherId}/{topicId}/hard-delete
   */
  hardDeleteAssignment: (teacherId, topicId) => {
    return apiClient.delete(
      `/api/admin/teachers/assignments/${teacherId}/${topicId}/hard-delete`,
    )
  },

  /**
   * ✅ Get assignments for a teacher (ADMIN ONLY)
   * GET /api/admin/teachers/{teacherId}/assignments
   */
  getTeacherAssignments: (teacherId) => {
    return apiClient.get(`/api/admin/teachers/${teacherId}/assignments`)
  },

  /**
   * ✅ Get teachers assigned to a topic (ADMIN ONLY)
   * GET /api/admin/teachers/topics/{topicId}/teachers
   */
  getTopicTeachers: (topicId) => {
    return apiClient.get(`/api/admin/teachers/topics/${topicId}/teachers`)
  },

  /**
   * ✅ Get all active assignments (ADMIN ONLY)
   * GET /api/admin/teachers/assignments
   */
  getAllAssignments: () => {
    return apiClient.get('/api/admin/teachers/assignments')
  },

  /**
   * ✅ Revoke all assignments for a teacher (ADMIN ONLY)
   * DELETE /api/admin/teachers/{teacherId}/assignments
   */
  revokeAllTeacherAssignments: (teacherId) => {
    return apiClient.delete(`/api/admin/teachers/${teacherId}/assignments`)
  },

  /**
   * ✅ Revoke all assignments for a topic (ADMIN ONLY)
   * DELETE /api/admin/teachers/topics/{topicId}/assignments
   */
  revokeAllTopicAssignments: (topicId) => {
    return apiClient.delete(`/api/admin/teachers/topics/${topicId}/assignments`)
  },
}

// ==================== TEACHER OPERATIONS (Teacher Portal) ====================
export const teacherAPI = {
  /**
   * ✅ Get current teacher's assignments
   * GET /api/teacher/my-assignments
   */
  getMyAssignments: () => {
    return apiClient.get('/api/teacher/my-assignments')
  },

  /**
   * ✅ Get topics assigned to current teacher (by module type)
   * GET /api/teacher/my-topics?moduleType=GRAMMAR
   */
  getMyTopics: (moduleType) => {
    const params = moduleType ? { moduleType } : {}
    return apiClient.get('/api/teacher/my-topics', { params })
  },

  /**
   * ✅ Get topics assigned to teacher with pagination
   * GET /api/teacher/my-topics/paginated?moduleType=GRAMMAR&page=0&size=10
   */
  getMyTopicsPaginated: (moduleType, page = 0, size = 10) => {
    return apiClient.get('/api/teacher/my-topics/paginated', {
      params: {
        moduleType,
        page,
        size,
      },
    })
  },

  /**
   * ✅ Get teacher profile
   * GET /api/teacher/profile
   */
  getProfile: () => {
    return apiClient.get('/api/teacher/profile')
  },

  /**
   * ✅ Check if teacher is assigned to a specific topic
   * GET /api/teacher/check-assignment/{topicId}
   */
  checkAssignment: (topicId) => {
    return apiClient.get(`/api/teacher/check-assignment/${topicId}`)
  },

  /**
   * ✅ Get statistics for teacher dashboard
   * GET /api/teacher/stats
   */
  getTeacherStats: () => {
    return apiClient.get('/api/teacher/stats')
  },
}

// ==================== BACKWARD COMPATIBILITY ====================
// Keep old exports for existing admin code
export default teacherAdminAPI
