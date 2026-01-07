// src/composables/user/useTeacherAssignment.js - FIXED
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { teacherAPI } from '@/api/modules/teacher.api'
import { userAPI } from '@/api'
import dayjs from 'dayjs'

export function useTeacherAssignment() {
  // States
  const loading = ref(false)
  const assignments = ref([])
  const teachers = ref([])
  const currentPage = ref(1)
  const pageSize = ref(20)

  // Filters
  const filters = reactive({
    teacherId: null,
    moduleType: null,
    isActive: null,
  })

  // Statistics
  const stats = reactive({
    total: 0,
    grammar: 0,
    reading: 0,
    listening: 0,
  })

  // Dialogs
  const assignDialogVisible = ref(false)

  // Computed
  const filteredAssignments = computed(() => {
    let result = assignments.value

    if (filters.teacherId) {
      result = result.filter((a) => a.teacherId === filters.teacherId)
    }
    if (filters.moduleType) {
      result = result.filter((a) => a.moduleType === filters.moduleType)
    }
    if (filters.isActive !== null) {
      result = result.filter((a) => a.isActive === filters.isActive)
    }

    return result
  })

  const paginatedAssignments = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    return filteredAssignments.value.slice(start, end)
  })

  // ==================== METHODS ====================

  /**
   * ✅ Fetch all assignments
   */
  const fetchAssignments = async () => {
    loading.value = true
    try {
      console.log('🔄 Fetching assignments...')

      // ✅ FIXED: Use correct API method
      const response = await teacherAPI.getAllAssignments()
      assignments.value = response.data.data || []

      console.log('✅ Loaded assignments:', assignments.value.length)
      calculateStats()
    } catch (error) {
      console.error('❌ Error fetching assignments:', error)
      ElMessage.error('Không thể tải danh sách phân quyền')
      assignments.value = []

      // Reset stats on error
      stats.total = 0
      stats.grammar = 0
      stats.reading = 0
      stats.listening = 0
    } finally {
      loading.value = false
    }
  }

  /**
   * ✅ Fetch teachers for filter dropdown
   */
  const fetchTeachersForFilter = async () => {
    try {
      console.log('🔄 Fetching teachers for filter...')
      const response = await userAPI.getAllUsers()
      const allUsers = response.data.data || []
      teachers.value = allUsers.filter((u) => u.role === 'TEACHER')
      console.log('✅ Loaded teachers:', teachers.value.length)
    } catch (error) {
      console.error('❌ Error fetching teachers:', error)
      teachers.value = []
    }
  }

  /**
   * ✅ Calculate stats from assignments
   */
  const calculateStats = () => {
    stats.total = assignments.value.length
    stats.grammar = assignments.value.filter((a) => a.moduleType === 'GRAMMAR').length
    stats.reading = assignments.value.filter((a) => a.moduleType === 'READING').length
    stats.listening = assignments.value.filter((a) => a.moduleType === 'LISTENING').length

    console.log('📊 Assignment stats:', stats)
  }

  /**
   * Handle filter change
   */
  const handleFilter = () => {
    currentPage.value = 1
  }

  /**
   * Reset all filters
   */
  const resetFilters = () => {
    filters.teacherId = null
    filters.moduleType = null
    filters.isActive = null
    currentPage.value = 1
  }

  /**
   * ✅ FIXED: Revoke assignment
   */
  const revokeAssignment = async (assignment) => {
    try {
      console.log('🗑️ Revoking assignment:', assignment.id)

      // ✅ FIXED: Pass assignmentId instead of teacherId + topicId
      await teacherAPI.revokeAssignment(assignment.id)

      ElMessage.success('✅ Đã thu hồi quyền thành công')
      await fetchAssignments()
    } catch (error) {
      console.error('❌ Error revoking assignment:', error)
      ElMessage.error('Không thể thu hồi quyền')
    }
  }

  /**
   * Show assign dialog
   */
  const showAssignDialog = () => {
    assignDialogVisible.value = true
  }

  /**
   * Format date
   */
  const formatDate = (date) => {
    if (!date) return 'N/A'
    return dayjs(date).format('DD/MM/YYYY HH:mm')
  }

  /**
   * Get module type color
   */
  const getModuleTypeColor = (moduleType) => {
    const colors = {
      GRAMMAR: 'primary',
      READING: 'success',
      LISTENING: 'warning',
    }
    return colors[moduleType] || 'info'
  }

  /**
   * ✅ Initialize composable
   */
  const initialize = async () => {
    console.log('✅ Initializing TeacherAssignment composable...')
    await Promise.all([
      fetchAssignments(),
      fetchTeachersForFilter()
    ])
  }

  return {
    // States
    loading,
    assignments,
    teachers,
    currentPage,
    pageSize,
    filters,
    stats,
    assignDialogVisible,

    // Computed
    filteredAssignments,
    paginatedAssignments,

    // Methods
    fetchAssignments,
    fetchTeachersForFilter,
    handleFilter,
    resetFilters,
    revokeAssignment,
    showAssignDialog,
    formatDate,
    getModuleTypeColor,
    initialize,
  }
}
