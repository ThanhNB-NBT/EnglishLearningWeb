import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authAPI } from '@/api'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/vi'

dayjs.extend(relativeTime)
dayjs.locale('vi')

export function useCleanupManagement() {
  // States
  const loading = ref(false)
  const cleanupLoading = ref(false)
  const unverifiedUsers = ref([])
  const currentPage = ref(1)
  const pageSize = ref(20)
  const lastCleanupCount = ref(0)
  const lastCleanupTime = ref(null)

  // Statistics - với default values để tránh undefined
  const stats = reactive({
    totalUnverified: 0,
    oldUnverified: 0,
    cutoffHours: 24,
    timestamp: null,
  })

  // Preview - với default values
  const preview = reactive({
    willBeDeleted: 0,
    cutoffDate: null,
    cutoffHours: 24,
    timestamp: null,
  })

  // Computed
  const paginatedUsers = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    return unverifiedUsers.value.slice(start, end)
  })

  // Computed - Hiển thị % tài khoản sẽ bị xóa
  const deletionPercentage = computed(() => {
    if (stats.totalUnverified === 0) return 0
    return ((stats.oldUnverified / stats.totalUnverified) * 100).toFixed(1)
  })

  // Computed - Status badge type
  const cleanupStatus = computed(() => {
    if (stats.oldUnverified === 0) return 'success'
    if (stats.oldUnverified < 10) return 'warning'
    return 'danger'
  })

  // Methods
  const fetchStats = async () => {
    try {
      const response = await authAPI.getCleanupStats()
      console.log('Cleanup stats response:', response.data)

      const data = response.data.data

      if (!data) {
        throw new Error('Invalid stats data')
      }

      // Safely assign với fallback values
      stats.totalUnverified = data.totalUnverifiedAccounts ?? 0
      stats.oldUnverified = data.accountsOlderThanCutoff ?? 0
      stats.cutoffHours = data.cutoffHours ?? 24
      stats.timestamp = data.timestamp ?? new Date()

      console.log('Stats updated:', stats)
    } catch (error) {
      console.error('Error fetching cleanup stats:', error)
      ElMessage.error('Không thể tải thống kê cleanup')

      // Reset về default nếu lỗi
      Object.assign(stats, {
        totalUnverified: 0,
        oldUnverified: 0,
        cutoffHours: 24,
        timestamp: new Date(),
      })
    }
  }

  const fetchPreview = async () => {
    try {
      const response = await authAPI.previewCleanup()
      console.log('Preview response:', response.data)

      const data = response.data.data

      if (!data) {
        throw new Error('Invalid preview data')
      }

      // Safely assign với fallback values
      preview.willBeDeleted = data.accountsWillBeDeleted ?? 0
      preview.cutoffDate = data.cutoffDate ?? null
      preview.cutoffHours = data.cutoffHours ?? 24
      preview.timestamp = new Date()

      console.log('Preview updated:', preview)
    } catch (error) {
      console.error('Error fetching cleanup preview:', error)
      ElMessage.error('Không thể tải preview cleanup')

      // Reset về default nếu lỗi
      Object.assign(preview, {
        willBeDeleted: 0,
        cutoffDate: null,
        cutoffHours: 24,
        timestamp: new Date(),
      })
    }
  }

  const fetchUnverifiedUsers = async () => {
    loading.value = true
    try {
      const response = await authAPI.getUnverifiedUsers(100)
      console.log('📋 Unverified users response:', response.data)

      const data = response.data.data

      if (!Array.isArray(data)) {
        throw new Error('Invalid users data format')
      }

      unverifiedUsers.value = data
      console.log('Unverified users count:', unverifiedUsers.value.length)
    } catch (error) {
      console.error('Error fetching unverified users:', error)
      ElMessage.error('Không thể tải danh sách tài khoản chưa xác thực')
      unverifiedUsers.value = []
    } finally {
      loading.value = false
    }
  }

  const handleRunCleanup = async () => {
    // Validate trước khi cleanup
    if (preview.willBeDeleted === 0) {
      ElMessage.info('Không có tài khoản nào cần xóa')
      return
    }

    try {
      await ElMessageBox.confirm(
        `Bạn có chắc chắn muốn xóa ${preview.willBeDeleted} tài khoản chưa xác thực?`,
        'Xác nhận chạy Cleanup',
        {
          confirmButtonText: 'Xóa',
          cancelButtonText: 'Hủy',
          type: 'error',
          distinguishCancelAndClose: true,
        }
      )

      cleanupLoading.value = true
      const response = await authAPI.runCleanup()
      console.log('Cleanup response:', response.data)

      const data = response.data.data
      const deletedCount = data?.deletedCount ?? 0

      lastCleanupCount.value = deletedCount
      lastCleanupTime.value = new Date()

      ElMessage.success({
        message: `Cleanup hoàn tất! Đã xóa ${deletedCount} tài khoản`,
        duration: 5000,
        showClose: true,
      })

      // Refresh data sau khi cleanup
      await Promise.all([
        fetchStats(),
        fetchPreview(),
        fetchUnverifiedUsers(),
      ])
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        console.log('Cleanup cancelled by user')
        return
      }

      console.error('Error running cleanup:', error)
      ElMessage.error({
        message: error.response?.data?.message || 'Không thể chạy cleanup',
        duration: 5000,
        showClose: true,
      })
    } finally {
      cleanupLoading.value = false
    }
  }

  const formatDate = (date) => {
    if (!date) return 'N/A'
    return dayjs(date).format('DD/MM/YYYY HH:mm:ss')
  }

  const formatRelativeTime = (date) => {
    if (!date) return 'N/A'
    return dayjs(date).fromNow()
  }

  const getAgeType = (hours) => {
    if (typeof hours !== 'number') return 'info'

    const cutoff = stats.cutoffHours || 24
    if (hours >= cutoff) return 'danger'
    if (hours >= cutoff * 0.75) return 'warning'
    return 'info'
  }

  const getAgeText = (hours) => {
    if (typeof hours !== 'number') return 'N/A'

    if (hours < 1) return `${Math.floor(hours * 60)} phút`
    if (hours < 24) return `${hours} giờ`
    const days = Math.floor(hours / 24)
    const remainingHours = hours % 24
    return remainingHours > 0
      ? `${days} ngày ${remainingHours} giờ`
      : `${days} ngày`
  }

  // Initialize - load all data with proper error handling
  const initialize = async () => {
    loading.value = true
    try {
      // Load tất cả data song song
      await Promise.allSettled([
        fetchStats(),
        fetchPreview(),
        fetchUnverifiedUsers(),
      ])

      console.log('Cleanup management initialized')
    } catch (error) {
      console.error('Error initializing cleanup management:', error)
      ElMessage.error('Không thể khởi tạo trang quản lý cleanup')
    } finally {
      loading.value = false
    }
  }

  // Refresh all data
  const refreshAll = async () => {
    console.log('Refreshing all cleanup data...')
    await initialize()
    ElMessage.success('Đã làm mới dữ liệu')
  }

  return {
    // States
    loading,
    cleanupLoading,
    unverifiedUsers,
    currentPage,
    pageSize,
    lastCleanupCount,
    lastCleanupTime,
    stats,
    preview,

    // Computed
    paginatedUsers,
    deletionPercentage,
    cleanupStatus,

    // Methods
    fetchStats,
    fetchPreview,
    fetchUnverifiedUsers,
    handleRunCleanup,
    formatDate,
    formatRelativeTime,
    getAgeType,
    getAgeText,
    initialize,
    refreshAll,
  }
}
