// src/api/modules/teacher.api.js - FIXED VERSION
import api from '@/api/config'

// ==================== DEFINITIONS ====================

const adminOperations = {
  /**
   * ✅ Assign teacher to topic (ADMIN ONLY)
   * POST /api/admin/teachers/assign
   */
  assignTeacher: (data) => {
    return api.post('/api/admin/teachers/assign', {
      teacherId: data.teacherId,
      topicId: data.topicId,
    })
  },

  /**
   * ✅ Revoke assignment (ADMIN ONLY)
   * DELETE /api/admin/teachers/assignments/{assignmentId}
   */
  revokeAssignment: (assignmentId) => {
    return api.delete(`/api/admin/teachers/assignments/${assignmentId}`)
  },

  /**
   * ✅ Hard delete assignment (ADMIN ONLY)
   * DELETE /api/admin/teachers/assignments/{teacherId}/{topicId}/hard-delete
   */
  hardDeleteAssignment: (teacherId, topicId) => {
    return api.delete(
      `/api/admin/teachers/assignments/${teacherId}/${topicId}/hard-delete`,
    )
  },

  /**
   * ✅ Get assignments for a teacher (ADMIN ONLY)
   * GET /api/admin/teachers/{teacherId}/assignments
   */
  getTeacherAssignments: (teacherId) => {
    return api.get(`/api/admin/teachers/${teacherId}/assignments`)
  },

  /**
   * ✅ Get teachers assigned to a topic (ADMIN ONLY)
   * GET /api/admin/teachers/topics/{topicId}/teachers
   */
  getTopicTeachers: (topicId) => {
    return api.get(`/api/admin/teachers/topics/${topicId}/teachers`)
  },

  /**
   * ✅ Get all active assignments (ADMIN ONLY)
   * GET /api/admin/teachers/assignments
   */
  getAllAssignments: () => {
    return api.get('/api/admin/teachers/assignments')
  },

  /**
   * ✅ Revoke all assignments for a teacher (ADMIN ONLY)
   * DELETE /api/admin/teachers/{teacherId}/assignments
   */
  revokeAllTeacherAssignments: (teacherId) => {
    return api.delete(`/api/admin/teachers/${teacherId}/assignments`)
  },

  /**
   * ✅ Revoke all assignments for a topic (ADMIN ONLY)
   * DELETE /api/admin/teachers/topics/{topicId}/assignments
   */
  revokeAllTopicAssignments: (topicId) => {
    return api.delete(`/api/admin/teachers/topics/${topicId}/assignments`)
  }
}

const teacherOperations = {
  /**
   * ✅ Get current teacher's assignments
   * GET /api/teacher/my-assignments
   */
  getMyAssignments: () => {
    return api.get('/api/teacher/my-assignments')
  },

  /**
   * ✅ Get teacher profile
   * GET /api/teacher/profile
   */
  getProfile: () => {
    return api.get('/api/teacher/profile')
  },

  /**
   * ✅ Check if teacher is assigned to a specific topic
   * GET /api/teacher/check-assignment/{topicId}
   */
  checkAssignment: (topicId) => {
    return api.get(`/api/teacher/check-assignment/${topicId}`)
  },

  /**
   * ✅ Get statistics for teacher dashboard
   * GET /api/teacher/stats
   */
  getTeacherStats: () => {
    return api.get('/api/teacher/stats')
  },
}

// ==================== EXPORTS ====================

// 1. Export dành riêng cho Admin (nếu code mới dùng cái này)
export const teacherAdminAPI = adminOperations

// 2. Export gộp chung (FIX LỖI: code cũ import teacherAPI nhưng gọi hàm Admin)
export const teacherAPI = {
  ...adminOperations,
  ...teacherOperations
}

// 3. Default export cũng là bản gộp chung
export default teacherAPI
