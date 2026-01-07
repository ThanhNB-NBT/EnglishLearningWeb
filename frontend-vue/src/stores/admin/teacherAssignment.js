// src/stores/admin/teacherAssignment.js
import { defineStore } from 'pinia'
import { teacherAPI } from '@/api'
import { ElMessage } from 'element-plus'

export const useTeacherAssignmentStore = defineStore('teacherAssignment', {
  state: () => ({
    // Assignments
    assignments: [],
    currentAssignment: null,
    assignmentsLoading: false,

    // Teachers by topic
    teachersByTopic: {},

    // Topics by teacher
    topicsByTeacher: {},

    // Statistics
    stats: null,
  }),

  getters: {
    activeAssignments: (state) => state.assignments.filter((a) => a.isActive),
    inactiveAssignments: (state) => state.assignments.filter((a) => !a.isActive),

    getAssignmentById: (state) => (id) => state.assignments.find((a) => a.id === id),

    getTeachersByTopicId: (state) => (topicId) => state.teachersByTopic[topicId] || [],

    getTopicsByTeacherId: (state) => (teacherId) => state.topicsByTeacher[teacherId] || [],
  },

  actions: {
    // ═══════════════════════════════════════════════════════════════════
    // ASSIGN TEACHER
    // ═══════════════════════════════════════════════════════════════════

    async assignTeacher(teacherId, topicId) {
      try {
        const response = await teacherAPI.assignTeacher({ teacherId, topicId })

        if (response.data.success) {
          ElMessage.success('✅ Phân công giáo viên thành công!')
          return response.data.data
        }
      } catch (error) {
        console.error('❌ Error assigning teacher:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể phân công')
        throw error
      }
    },

    // ═══════════════════════════════════════════════════════════════════
    // REVOKE ASSIGNMENT
    // ═══════════════════════════════════════════════════════════════════

    async revokeAssignment(teacherId, topicId) {
      try {
        const response = await teacherAPI.revokeAssignment(teacherId, topicId)

        if (response.data.success) {
          ElMessage.success('✅ Đã thu hồi quyền!')

          // Remove from local state
          this.assignments = this.assignments.filter(
            (a) => !(a.teacherId === teacherId && a.topicId === topicId)
          )
        }
      } catch (error) {
        console.error('❌ Error revoking assignment:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể thu hồi')
        throw error
      }
    },

    async deactivateAssignment(assignmentId) {
      try {
        const response = await teacherAPI.deactivateAssignment(assignmentId)

        if (response.data.success) {
          ElMessage.success('✅ Đã vô hiệu hóa phân công!')

          // Update local state
          const assignment = this.assignments.find((a) => a.id === assignmentId)
          if (assignment) {
            assignment.isActive = false
          }
        }
      } catch (error) {
        console.error('❌ Error deactivating assignment:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể vô hiệu hóa')
        throw error
      }
    },

    // ═══════════════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════════════

    async fetchAssignmentsByTeacher(teacherId) {
      this.assignmentsLoading = true
      try {
        const response = await teacherAPI.getAssignmentsByTeacher(teacherId)

        if (response.data.success) {
          const assignments = response.data.data
          this.topicsByTeacher[teacherId] = assignments
          console.log(`✅ Fetched ${assignments.length} assignments for teacher ${teacherId}`)
        }
      } catch (error) {
        console.error('❌ Error fetching assignments:', error)
        ElMessage.error('Không thể tải danh sách phân công')
        throw error
      } finally {
        this.assignmentsLoading = false
      }
    },

    async fetchAssignmentsByTopic(topicId) {
      this.assignmentsLoading = true
      try {
        const response = await teacherAPI.getAssignmentsByTopic(topicId)

        if (response.data.success) {
          const assignments = response.data.data
          this.teachersByTopic[topicId] = assignments
          console.log(`✅ Fetched ${assignments.length} teachers for topic ${topicId}`)
        }
      } catch (error) {
        console.error('❌ Error fetching assignments:', error)
        ElMessage.error('Không thể tải danh sách giáo viên')
        throw error
      } finally {
        this.assignmentsLoading = false
      }
    },

    async fetchAllActiveAssignments() {
      this.assignmentsLoading = true
      try {
        const response = await teacherAPI.getAllActiveAssignments()

        if (response.data.success) {
          this.assignments = response.data.data
          console.log(`✅ Fetched ${this.assignments.length} active assignments`)
        }
      } catch (error) {
        console.error('❌ Error fetching assignments:', error)
        ElMessage.error('Không thể tải danh sách phân công')
        throw error
      } finally {
        this.assignmentsLoading = false
      }
    },

    async fetchAssignmentById(assignmentId) {
      this.assignmentsLoading = true
      try {
        const response = await teacherAPI.getAssignmentById(assignmentId)

        if (response.data.success) {
          this.currentAssignment = response.data.data
          return this.currentAssignment
        }
      } catch (error) {
        console.error('❌ Error fetching assignment:', error)
        ElMessage.error('Không thể tải chi tiết phân công')
        throw error
      } finally {
        this.assignmentsLoading = false
      }
    },

    // ═══════════════════════════════════════════════════════════════════
    // BATCH OPERATIONS
    // ═══════════════════════════════════════════════════════════════════

    async revokeAllByTeacher(teacherId) {
      try {
        const response = await teacherAPI.revokeAllByTeacher(teacherId)

        if (response.data.success) {
          ElMessage.success('✅ Đã thu hồi tất cả quyền!')

          // Remove from local state
          this.assignments = this.assignments.filter((a) => a.teacherId !== teacherId)
          delete this.topicsByTeacher[teacherId]
        }
      } catch (error) {
        console.error('❌ Error revoking all assignments:', error)
        ElMessage.error('Không thể thu hồi tất cả')
        throw error
      }
    },

    async revokeAllByTopic(topicId) {
      try {
        const response = await teacherAPI.revokeAllByTopic(topicId)

        if (response.data.success) {
          ElMessage.success('✅ Đã thu hồi tất cả giáo viên!')

          // Remove from local state
          this.assignments = this.assignments. filter((a) => a.topicId !== topicId)
          delete this.teachersByTopic[topicId]
        }
      } catch (error) {
        console.error('❌ Error revoking all assignments:', error)
        ElMessage.error('Không thể thu hồi tất cả')
        throw error
      }
    },

    // ═══════════════════════════════════════════════════════════════════
    // VALIDATION
    // ═══════════════════════════════════════════════════════════════════

    async checkAssignment(teacherId, topicId) {
      try {
        const response = await teacherAPI.checkAssignment(teacherId, topicId)

        if (response.data.success) {
          return response.data.data
        }

        return false
      } catch (error) {
        console.error('❌ Error checking assignment:', error)
        return false
      }
    },

    // ═══════════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════════

    async fetchStatsByTeacher(teacherId) {
      try {
        const response = await teacherAPI.countAssignmentsByTeacher(teacherId)

        if (response.data.success) {
          return response.data.data
        }
      } catch (error) {
        console.error('❌ Error fetching stats:', error)
        return 0
      }
    },

    async fetchStatsByTopic(topicId) {
      try {
        const response = await teacherAPI.countAssignmentsByTopic(topicId)

        if (response.data.success) {
          return response.data.data
        }
      } catch (error) {
        console.error('❌ Error fetching stats:', error)
        return 0
      }
    },

    // ═══════════════════════════════════════════════════════════════════
    // RESET
    // ═══════════════════════════════════════════════════════════════════

    reset() {
      this.assignments = []
      this.currentAssignment = null
      this.teachersByTopic = {}
      this.topicsByTeacher = {}
      this.stats = null
      this.assignmentsLoading = false
    },
  },
})
