// src/composables/user/useTeacherManagement.js - FIXED

import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userAPI } from '@/api'
import { teacherAPI } from '@/api/modules/teacher.api'
import dayjs from 'dayjs'

export function useTeacherManagement() {
  // States
  const loading = ref(false)
  const teachers = ref([])
  const searchQuery = ref('')
  const currentPage = ref(1)
  const pageSize = ref(20)

  // Filters
  const filters = reactive({
    isActive: null,
    hasAssignment: null,
  })

  // Statistics
  const stats = reactive({
    total: 0,
    assigned: 0,
    unassigned: 0,
    inactive: 0,
  })

  // Dialogs
  const createTeacherDialogVisible = ref(false)
  const topicsDialogVisible = ref(false)
  const assignDialogVisible = ref(false)
  const selectedTeacher = ref(null)

  // Computed
  const filteredTeachers = computed(() => {
    let result = teachers.value

    if (searchQuery.value) {
      const query = searchQuery.value.toLowerCase()
      result = result.filter(
        (teacher) =>
          teacher.username?.toLowerCase().includes(query) ||
          teacher.email?.toLowerCase().includes(query) ||
          teacher.fullName?.toLowerCase().includes(query),
      )
    }

    if (filters.isActive !== null) {
      result = result.filter((t) => t.isActive === filters.isActive)
    }
    if (filters.hasAssignment !== null) {
      result = result.filter((t) =>
        filters.hasAssignment
          ? t.assignedTopicsCount > 0
          : t.assignedTopicsCount === 0,
      )
    }

    return result
  })

  const paginatedTeachers = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    return filteredTeachers.value.slice(start, end)
  })

  // Methods
  const fetchTeachers = async () => {
    loading.value = true
    try {
      console.log('📄 Fetching teachers...')
      const response = await userAPI.getAllUsers()
      const allUsers = response.data.data || []

      console.log('📦 All users:', allUsers.length)

      // ✅ FIX: Only get TEACHER role
      const teacherUsers = allUsers.filter((user) => user.role === 'TEACHER')

      console.log('👨‍🏫 Teachers found:', teacherUsers.length)

      // Fetch assignments for each teacher
      const teachersWithAssignments = await Promise.all(
        teacherUsers.map(async (teacher) => {
          try {
            const assignResponse = await teacherAPI.getTeacherAssignments(teacher.id)
            const assignments = assignResponse.data.data || []

            return {
              ...teacher,
              assignedTopicsCount: assignments.length,
              assignments: assignments,
            }
          } catch (error) {
            console.error(`❌ Failed to fetch assignments for teacher ${teacher.id}:`, error)
            // ✅ Return teacher with 0 assignments on error
            return {
              ...teacher,
              assignedTopicsCount: 0,
              assignments: [],
            }
          }
        }),
      )

      teachers.value = teachersWithAssignments

      console.log('✅ Teachers with assignments:', teachers.value.length)

      calculateStats()
    } catch (error) {
      console.error('❌ Error fetching teachers:', error)
      ElMessage.error('Không thể tải danh sách giáo viên')
      teachers.value = []

      // Reset stats on error
      stats.total = 0
      stats.assigned = 0
      stats.unassigned = 0
      stats.inactive = 0
    } finally {
      loading.value = false
    }
  }

  const calculateStats = () => {
    stats.total = teachers.value.length
    stats.assigned = teachers.value.filter((t) => t.assignedTopicsCount > 0).length
    stats.unassigned = teachers.value.filter((t) => t.assignedTopicsCount === 0).length
    stats.inactive = teachers.value.filter((t) => !t.isActive).length

    console.log('📊 Teacher stats:', stats)
  }

  const handleSearch = () => {
    currentPage.value = 1
  }

  const handleFilter = () => {
    currentPage.value = 1
  }

  const resetFilters = () => {
    searchQuery.value = ''
    filters.isActive = null
    filters.hasAssignment = null
    currentPage.value = 1
  }

  const viewTeacherTopics = (teacher) => {
    selectedTeacher.value = teacher
    topicsDialogVisible.value = true
  }

  const assignTopics = (teacher) => {
    selectedTeacher.value = teacher
    assignDialogVisible.value = true
  }

  const handleAction = async (command, teacher) => {
    switch (command) {
      case 'block':
        await blockTeacher(teacher)
        break
      case 'unblock':
        await unblockTeacher(teacher)
        break
      case 'delete':
        await deleteTeacher(teacher)
        break
    }
  }

  const blockTeacher = async (teacher) => {
    try {
      await ElMessageBox.confirm(
        `Bạn có chắc chắn muốn khóa tài khoản giáo viên "${teacher.username}"?`,
        'Xác nhận khóa',
        {
          confirmButtonText: 'Khóa',
          cancelButtonText: 'Hủy',
          type: 'warning',
        },
      )

      await userAPI.blockUser(teacher.id)
      ElMessage.success('Đã khóa tài khoản giáo viên')
      await fetchTeachers()
    } catch (error) {
      if (error !== 'cancel') {
        console.error('Error blocking teacher:', error)
        ElMessage.error('Không thể khóa tài khoản')
      }
    }
  }

  const unblockTeacher = async (teacher) => {
    try {
      await userAPI.unblockUser(teacher.id)
      ElMessage.success('Đã mở khóa tài khoản giáo viên')
      await fetchTeachers()
    } catch (error) {
      console.error('Error unblocking teacher:', error)
      ElMessage.error('Không thể mở khóa tài khoản')
    }
  }

  const deleteTeacher = async (teacher) => {
    try {
      await ElMessageBox.confirm(
        `Bạn có chắc chắn muốn xóa giáo viên "${teacher.username}"?\nTất cả phân quyền topics sẽ bị thu hồi!`,
        'Xác nhận xóa',
        {
          confirmButtonText: 'Xóa',
          cancelButtonText: 'Hủy',
          type: 'error',
        },
      )

      // Revoke all assignments first
      try {
        await teacherAPI.revokeAllTeacherAssignments(teacher.id)
      } catch (err) {
        console.warn('⚠️ Could not revoke assignments, continuing with delete:', err)
      }

      // Delete teacher
      await userAPI.deleteUser(teacher.id)

      ElMessage.success('Đã xóa giáo viên thành công')
      await fetchTeachers()
    } catch (error) {
      if (error !== 'cancel') {
        console.error('Error deleting teacher:', error)
        ElMessage.error('Không thể xóa giáo viên')
      }
    }
  }

  const showCreateTeacherDialog = () => {
    createTeacherDialogVisible.value = true
  }

  const formatDate = (date) => {
    if (!date) return 'N/A'
    return dayjs(date).format('DD/MM/YYYY HH:mm')
  }

  return {
    // States
    loading,
    teachers,
    searchQuery,
    currentPage,
    pageSize,
    filters,
    stats,
    createTeacherDialogVisible,
    topicsDialogVisible,
    assignDialogVisible,
    selectedTeacher,

    // Computed
    filteredTeachers,
    paginatedTeachers,

    // Methods
    fetchTeachers,
    handleSearch,
    handleFilter,
    resetFilters,
    viewTeacherTopics,
    assignTopics,
    handleAction,
    showCreateTeacherDialog,
    formatDate,
  }
}
