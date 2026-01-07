// src/composables/user/useUsersManagement.js - FIXED VERSION

import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userAPI, authAPI } from '@/api'
import dayjs from 'dayjs'

export function useUsersManagement() {
  // States
  const loading = ref(false)
  const submitting = ref(false)
  const users = ref([])
  const searchQuery = ref('')
  const currentPage = ref(1)
  const pageSize = ref(20)

  // Filters
  const filters = reactive({
    role: null,
    isActive: null,
    isVerified: null,
    englishLevel: null,
  })

  // Statistics
  const stats = reactive({
    total: 0,
    active: 0,
    unverified: 0,
    admins: 0,
  })

  // Dialogs
  const detailDialogVisible = ref(false)
  const createAdminDialogVisible = ref(false)
  const selectedUser = ref(null)

  // Admin Form
  const adminForm = reactive({
    username: '',
    email: '',
    password: '',
    fullName: '',
  })

  const adminFormRules = {
    username: [
      { required: true, message: 'Vui lòng nhập username', trigger: 'blur' },
      { min: 3, max: 50, message: 'Username phải từ 3-50 ký tự', trigger: 'blur' },
    ],
    email: [
      { required: true, message: 'Vui lòng nhập email', trigger: 'blur' },
      { type: 'email', message: 'Email không hợp lệ', trigger: 'blur' },
    ],
    password: [
      { required: true, message: 'Vui lòng nhập mật khẩu', trigger: 'blur' },
      { min: 8, message: 'Mật khẩu phải ít nhất 8 ký tự', trigger: 'blur' },
    ],
    fullName: [{ required: true, message: 'Vui lòng nhập họ và tên', trigger: 'blur' }],
  }

  // Computed
  const filteredUsers = computed(() => {
    let result = users.value

    // Search
    if (searchQuery.value) {
      const query = searchQuery.value.toLowerCase()
      result = result.filter(
        (user) =>
          user.username?.toLowerCase().includes(query) ||
          user.email?.toLowerCase().includes(query) ||
          user.fullName?.toLowerCase().includes(query),
      )
    }

    // Filters
    if (filters.role) {
      result = result.filter((user) => user.role === filters.role)
    }
    if (filters.isActive !== null) {
      result = result.filter((user) => user.isActive === filters.isActive)
    }
    if (filters.isVerified !== null) {
      result = result.filter((user) => user.isVerified === filters.isVerified)
    }
    if (filters.englishLevel) {
      result = result.filter((user) => user.englishLevel === filters.englishLevel)
    }

    return result
  })

  // ✅ FIX: Add pagination computed
  const paginatedUsers = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    return filteredUsers.value.slice(start, end)
  })

  // Methods
  const fetchUsers = async () => {
    loading.value = true
    try {
      console.log('🔄 Fetching users...')
      const response = await userAPI.getAllUsers()

      console.log('📦 Raw response:', response)

      // ✅ FIX: Handle different response structures
      let usersData = []

      if (response.data?.data) {
        usersData = response.data.data
      } else if (Array.isArray(response.data)) {
        usersData = response.data
      } else {
        console.error('❌ Unexpected response structure:', response)
        throw new Error('Invalid response structure')
      }

      console.log('📊 Users data:', usersData)

      // ✅ Map to ensure proper structure
      // Show USER and ADMIN roles (TEACHER has separate tab)
      users.value = usersData
        .filter(user => user.role !== 'TEACHER') // ✅ Exclude TEACHER role only
        .map((user) => ({
          id: user.id,
          username: user.username,
          email: user.email,
          fullName: user.fullName,
          role: user.role,
          englishLevel: user.englishLevel,
          isActive: user.isActive,
          isVerified: user.isVerified,
          createdAt: user.createdAt,
          updatedAt: user.updatedAt,

          // Extract stats
          stats: user.stats || {},
          totalPoints: user.stats?.totalPoints || 0,
          streakDays: user.stats?.currentStreak || 0,
          totalLessonsCompleted: user.stats?.totalLessonsCompleted || 0,

          // Extract activity
          activity: user.activity || {},
          lastLoginDate: user.activity?.lastLoginDate,
          loginCount: user.activity?.loginCount || 0,
        }))

      console.log('✅ Processed users:', users.value.length)

      calculateStats()
    } catch (error) {
      console.error('❌ Error fetching users:', error)
      ElMessage.error('Không thể tải danh sách người dùng')
      users.value = []
    } finally {
      loading.value = false
    }
  }

  const calculateStats = () => {
    stats.total = users.value.length
    stats.active = users.value.filter((u) => u.isActive).length
    stats.unverified = users.value.filter((u) => !u.isVerified).length
    stats.admins = users.value.filter((u) => u.role === 'ADMIN').length

    console.log('📊 Stats calculated:', stats)
  }

  const handleSearch = () => {
    currentPage.value = 1
  }

  const handleFilter = () => {
    currentPage.value = 1
  }

  const resetFilters = () => {
    searchQuery.value = ''
    filters.role = null
    filters.isActive = null
    filters.isVerified = null
    filters.englishLevel = null
    currentPage.value = 1
  }

  const viewUserDetail = (user) => {
    selectedUser.value = user
    detailDialogVisible.value = true
  }

  const handleAction = async (command, user) => {
    switch (command) {
      case 'block':
        await blockUser(user)
        break
      case 'unblock':
        await unblockUser(user)
        break
      case 'delete':
        await deleteUser(user)
        break
    }
  }

  const blockUser = async (user) => {
    try {
      await ElMessageBox.confirm(
        `Bạn có chắc chắn muốn khóa tài khoản "${user.username}"?`,
        'Xác nhận khóa',
        {
          confirmButtonText: 'Khóa',
          cancelButtonText: 'Hủy',
          type: 'warning',
        },
      )

      await userAPI.blockUser(user.id)
      ElMessage.success('Đã khóa tài khoản thành công')
      await fetchUsers()
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error('Không thể khóa tài khoản')
        console.error('Error blocking user:', error)
      }
    }
  }

  const unblockUser = async (user) => {
    try {
      await userAPI.unblockUser(user.id)
      ElMessage.success('Đã mở khóa tài khoản thành công')
      await fetchUsers()
    } catch (error) {
      ElMessage.error('Không thể mở khóa tài khoản')
      console.error('Error unblocking user:', error)
    }
  }

  const deleteUser = async (user) => {
    try {
      await ElMessageBox.confirm(
        `Bạn có chắc chắn muốn xóa tài khoản "${user.username}"? Hành động này không thể hoàn tác!`,
        'Xác nhận xóa',
        {
          confirmButtonText: 'Xóa',
          cancelButtonText: 'Hủy',
          type: 'error',
        },
      )

      await userAPI.deleteUser(user.id)
      ElMessage.success('Đã xóa tài khoản thành công')
      await fetchUsers()
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error('Không thể xóa tài khoản')
        console.error('Error deleting user:', error)
      }
    }
  }

  const showCreateAdminDialog = () => {
    Object.assign(adminForm, {
      fullName: '',
      username: '',
      email: '',
      password: '',
    })
    createAdminDialogVisible.value = true
  }

  const handleCreateAdmin = async (formRef) => {
    try {
      await formRef.validate()
      submitting.value = true

      await authAPI.createAdmin(adminForm)
      ElMessage.success('Tạo tài khoản Admin thành công!')
      createAdminDialogVisible.value = false
      await fetchUsers()
    } catch (error) {
      if (error.response?.data?.message) {
        ElMessage.error(error.response.data.message)
      } else {
        ElMessage.error('Không thể tạo tài khoản Admin')
      }
      console.error('Error creating admin:', error)
    } finally {
      submitting.value = false
    }
  }

  const handleSizeChange = (val) => {
    pageSize.value = val
    currentPage.value = 1
  }

  const handleCurrentChange = (val) => {
    currentPage.value = val
  }

  const formatDate = (date) => {
    if (!date) return 'N/A'
    return dayjs(date).format('DD/MM/YYYY HH:mm')
  }

  const getLevelType = (level) => {
    const types = {
      BEGINNER: 'info',
      INTERMEDIATE: 'warning',
      ADVANCED: 'success',
      A1: 'info',
      A2: 'info',
      B1: 'warning',
      B2: 'warning',
      C1: 'success',
      C2: 'success',
    }
    return types[level] || 'info'
  }

  return {
    // States
    loading,
    submitting,
    users,
    searchQuery,
    currentPage,
    pageSize,
    filters,
    stats,
    detailDialogVisible,
    createAdminDialogVisible,
    selectedUser,
    adminForm,
    adminFormRules,

    // Computed
    filteredUsers,
    paginatedUsers, // ✅ ADD: Export paginated users

    // Methods
    fetchUsers,
    calculateStats,
    handleSearch,
    handleFilter,
    resetFilters,
    viewUserDetail,
    handleAction,
    blockUser,
    unblockUser,
    deleteUser,
    showCreateAdminDialog,
    handleCreateAdmin,
    handleSizeChange,
    handleCurrentChange,
    formatDate,
    getLevelType,
  }
}
