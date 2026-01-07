// src/composables/useTopic.js - UNIFIED VERSION
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTopicAdminStore } from '@/stores/admin/topicAdmin'
import { useTopicTeacherStore } from '@/stores/teacher/topicTeacher'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * ✅ Unified Composable for Topic CRUD operations
 * Works for both ADMIN and TEACHER
 * Handles business logic, API calls, and user feedback
 *
 * @param {string} moduleType - GRAMMAR | LISTENING | READING
 * @returns {Object} Topic operations and state
 */
export function useTopic(moduleType) {
  const authStore = useAuthStore()
  const adminStore = useTopicAdminStore()
  const teacherStore = useTopicTeacherStore()

  // ==================== ROLE DETECTION ====================
  const currentRole = computed(() => {
    if (authStore.isAdminAuthenticated && authStore.admin) {
      return authStore.admin.role
    }
    if (authStore.isTeacherAuthenticated && authStore.teacher) {
      return authStore.teacher.role
    }
    return null
  })

  const isAdmin = computed(() => currentRole.value === 'ADMIN')
  const isTeacher = computed(() => currentRole.value === 'TEACHER')

  // ==================== STORE SELECTION ====================
  const activeStore = computed(() => {
    if (isAdmin.value) return adminStore
    if (isTeacher.value) return teacherStore
    return null
  })

  // Local state
  const loading = ref(false)
  const error = ref(null)

  // ==================== COMPUTED PROPERTIES ====================

  /**
   * Get topics for current module
   */
  const topics = computed(() => {
    if (!activeStore.value) return []
    return activeStore.value.getTopicsByModule(moduleType) || []
  })

  /**
   * Get loading state for current module
   */
  const isLoading = computed(() => {
    if (!activeStore.value) return false
    const loadingKey = `${moduleType.toLowerCase()}Loading`
    return activeStore.value[loadingKey] || false
  })

  // ==================== ACTIONS ====================

  /**
   * Fetch topics for module
   */
  const fetchTopics = async (params = {}) => {
    if (!activeStore.value) {
      throw new Error('No active store - user not authenticated')
    }

    loading.value = true
    error.value = null

    try {
      console.log(`🔄 [useTopic] Fetching ${moduleType} topics...`)

      if (isAdmin.value) {
        // Admin: fetch all topics
        await activeStore.value.fetchTopicsByModule(moduleType, params)
      } else if (isTeacher.value) {
        // Teacher: fetch only assigned topics
        await activeStore.value.fetchMyTopics(moduleType, params)
      }

      console.log(`✅ [useTopic] Fetched ${topics.value.length} topics`)
    } catch (err) {
      console.error('❌ [useTopic] Fetch error:', err)
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Create new topic (ADMIN ONLY)
   * @param {Object} topicData - Topic data from form
   * @returns {Promise<Object>} Created topic
   */
  const createTopic = async (topicData) => {
    if (!isAdmin.value) {
      throw new Error('Only admins can create topics')
    }

    loading.value = true
    error.value = null

    try {
      console.log('➕ [useTopic] Creating topic:', topicData)

      // Validate
      if (!topicData.name?.trim()) {
        throw new Error('Tên chủ đề không được để trống')
      }

      if (!topicData.levelRequired) {
        throw new Error('Vui lòng chọn trình độ')
      }

      // Call store
      const result = await activeStore.value.createTopic(moduleType, topicData)

      // Success feedback
      ElMessage.success({
        message: '✅ Tạo chủ đề thành công!',
        duration: 3000,
        showClose: true,
      })

      console.log('✅ [useTopic] Created:', result)
      return result

    } catch (err) {
      console.error('❌ [useTopic] Create error:', err)
      error.value = err.message

      // Error feedback
      const errorMsg = err.response?.data?.message || err.message || 'Không thể tạo chủ đề'
      ElMessage.error({
        message: `❌ ${errorMsg}`,
        duration: 5000,
        showClose: true,
      })

      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Update topic
   * @param {number} topicId - Topic ID
   * @param {Object} topicData - Updated data
   * @returns {Promise<Object>} Updated topic
   */
  const updateTopic = async (topicId, topicData) => {
    if (!activeStore.value) {
      throw new Error('No active store - user not authenticated')
    }

    loading.value = true
    error.value = null

    try {
      console.log('✏️ [useTopic] Updating topic:', topicId, topicData)

      // Check permission for teacher
      if (isTeacher.value) {
        const canManage = await activeStore.value.checkAssignment(topicId)
        if (!canManage) {
          throw new Error('Bạn không có quyền chỉnh sửa chủ đề này')
        }
      }

      // Call store
      const result = await activeStore.value.updateTopic(topicId, topicData)

      // Success feedback
      ElMessage.success({
        message: '✅ Cập nhật thành công!',
        duration: 3000,
        showClose: true,
      })

      console.log('✅ [useTopic] Updated:', result)
      return result

    } catch (err) {
      console.error('❌ [useTopic] Update error:', err)
      error.value = err.message

      const errorMsg = err.response?.data?.message || err.message || 'Không thể cập nhật'
      ElMessage.error({
        message: `❌ ${errorMsg}`,
        duration: 5000,
        showClose: true,
      })

      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Delete topic with confirmation (ADMIN ONLY)
   * @param {Object} topic - Topic to delete
   * @returns {Promise<void>}
   */
  const deleteTopic = async (topic) => {
    if (!isAdmin.value) {
      throw new Error('Only admins can delete topics')
    }

    try {
      // Confirmation dialog
      await ElMessageBox.confirm(
        `Bạn có chắc muốn xóa chủ đề "${topic.name}"?\nTất cả bài học liên quan sẽ bị xóa!`,
        'Xác nhận xóa',
        {
          type: 'warning',
          confirmButtonText: 'Xóa',
          cancelButtonText: 'Hủy',
          confirmButtonClass: 'el-button--danger',
          distinguishCancelAndClose: true,
        }
      )

      loading.value = true
      console.log('🗑️ [useTopic] Deleting topic:', topic.id)

      // Call store
      await activeStore.value.deleteTopic(topic.id, moduleType)

      // Success feedback
      ElMessage.success({
        message: '✅ Đã xóa chủ đề thành công',
        duration: 3000,
        showClose: true,
      })

      console.log('✅ [useTopic] Deleted:', topic.id)

    } catch (err) {
      if (err === 'cancel' || err === 'close') {
        console.log('ℹ️ [useTopic] User cancelled delete')
        return
      }

      console.error('❌ [useTopic] Delete error:', err)
      error.value = err.message

      const errorMsg = err.response?.data?.message || err.message || 'Không thể xóa chủ đề'
      ElMessage.error({
        message: `❌ ${errorMsg}`,
        duration: 5000,
        showClose: true,
      })

      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Toggle topic status
   * @param {Object} topic - Topic to toggle
   * @returns {Promise<Object>} Updated topic
   */
  const toggleStatus = async (topic) => {
    if (!activeStore.value) {
      throw new Error('No active store - user not authenticated')
    }

    const originalStatus = topic.isActive

    try {
      console.log(`🔄 [useTopic] Toggling topic ${topic.id} status`)

      // Check permission for teacher
      if (isTeacher.value) {
        const canManage = await activeStore.value.checkAssignment(topic.id)
        if (!canManage) {
          throw new Error('Bạn không có quyền thay đổi trạng thái chủ đề này')
        }
      }

      // Optimistic update
      topic.isActive = !originalStatus

      // Call store
      const result = await activeStore.value.toggleTopicStatus(topic.id)

      // Success feedback
      const status = result.isActive ? 'kích hoạt' : 'vô hiệu hóa'
      ElMessage.success({
        message: `✅ Đã ${status} chủ đề`,
        duration: 2000,
        showClose: true,
      })

      console.log('✅ [useTopic] Toggled:', result)
      return result

    } catch (err) {
      console.error('❌ [useTopic] Toggle error:', err)

      // Revert on error
      topic.isActive = originalStatus
      error.value = err.message

      const errorMsg = err.response?.data?.message || err.message || 'Không thể thay đổi trạng thái'
      ElMessage.error({
        message: `❌ ${errorMsg}`,
        duration: 3000,
        showClose: true,
      })

      throw err
    }
  }

  /**
   * Fix order indexes with confirmation (ADMIN ONLY)
   * @returns {Promise<void>}
   */
  const fixOrderIndexes = async () => {
    if (!isAdmin.value) {
      throw new Error('Only admins can fix order indexes')
    }

    try {
      await ElMessageBox.confirm(
        'Chuẩn hóa thứ tự các chủ đề (1, 2, 3...)?\nThao tác này sẽ sắp xếp lại tất cả topics.',
        'Xác nhận',
        {
          type: 'info',
          confirmButtonText: 'Đồng ý',
          cancelButtonText: 'Hủy',
        }
      )

      loading.value = true
      console.log('🔧 [useTopic] Fixing order indexes...')

      await activeStore.value.fixOrderIndexes(moduleType)

      ElMessage.success({
        message: '✅ Đã chuẩn hóa thứ tự thành công',
        duration: 3000,
        showClose: true,
      })

      console.log('✅ [useTopic] Order fixed')

    } catch (err) {
      if (err === 'cancel' || err === 'close') {
        console.log('ℹ️ [useTopic] User cancelled fix order')
        return
      }

      console.error('❌ [useTopic] Fix order error:', err)
      error.value = err.message

      ElMessage.error({
        message: '❌ Không thể chuẩn hóa thứ tự',
        duration: 3000,
        showClose: true,
      })

      throw err
    } finally {
      loading.value = false
    }
  }

  // ==================== HELPERS ====================

  /**
   * Get next order index for new topic
   * @returns {number}
   */
  const getNextOrderIndex = () => {
    if (!activeStore.value) return 1
    return activeStore.value.getNextOrderIndex?.(moduleType) || 1
  }

  /**
   * Get topic by ID
   * @param {number} id - Topic ID
   * @returns {Object|undefined}
   */
  const getTopicById = (id) => {
    if (!activeStore.value) return null
    return activeStore.value.getTopicById(id)
  }

  /**
   * Clear error
   */
  const clearError = () => {
    error.value = null
  }

  // ==================== RETURN ====================
  return {
    // Role info
    currentRole,
    isAdmin,
    isTeacher,

    // State
    topics,
    loading,
    isLoading,
    error,

    // Actions
    fetchTopics,
    createTopic,
    updateTopic,
    deleteTopic,
    toggleStatus,
    fixOrderIndexes,

    // Helpers
    getNextOrderIndex,
    getTopicById,
    clearError,
  }
}
