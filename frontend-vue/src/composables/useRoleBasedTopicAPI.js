// src/composables/useRoleBasedTopicAPI.js
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { topicAPI } from '@/api'
import teacherTopicAPI from '@/api/modules/teacher_topic.api'

/**
 * ✅ Role-based Topic API Composable
 * Automatically detects user role and calls the appropriate API
 *
 * Usage:
 * const { fetchTopics, isAdmin, isTeacher } = useRoleBasedTopicAPI()
 * const topics = await fetchTopics('GRAMMAR', { page: 1, size: 10 })
 */
export function useRoleBasedTopicAPI() {
  const authStore = useAuthStore()

  // ==================== ROLE DETECTION ====================
  const currentRole = computed(() => {
    if (authStore.isAdminAuthenticated && authStore.admin) {
      return authStore.admin.role
    }
    if (authStore.isTeacherAuthenticated && authStore.teacher) {
      return authStore.teacher.role
    }
    if (authStore.isUserAuthenticated && authStore.user) {
      return authStore.user.role
    }
    return null
  })

  const isAdmin = computed(() => currentRole.value === 'ADMIN')
  const isTeacher = computed(() => currentRole.value === 'TEACHER')
  const isUser = computed(() => currentRole.value === 'USER')

  // ==================== TOPIC API METHODS ====================

  /**
   * Fetch topics by module - automatically calls correct API based on role
   *
   * @param {string} moduleType - 'GRAMMAR' | 'READING' | 'LISTENING'
   * @param {object} params - { page, size, sortBy, sortDir }
   * @returns {Promise} - API response
   */
  const fetchTopics = async (moduleType, params = {}) => {
    const { page = 1, size = 100, sortBy = 'orderIndex', sortDir = 'asc' } = params

    console.log('🔍 [useRoleBasedTopicAPI] Fetching topics:', {
      moduleType,
      role: currentRole.value,
      params
    })

    if (isAdmin.value) {
      // ✅ ADMIN: Get all topics (with pagination)
      console.log('👑 Admin detected - fetching ALL topics')
      return await topicAPI.getTopicsByModule(moduleType, {
        page,
        size,
        sortBy,
        sortDir
      })
    } else if (isTeacher.value) {
      // ✅ TEACHER: Get only assigned topics
      console.log('👨‍🏫 Teacher detected - fetching ASSIGNED topics only')
      return await teacherTopicAPI.getMyTopicsPaginated(
        moduleType,
        page - 1, // Backend uses 0-indexed
        size,
        sortBy,
        sortDir
      )
    } else {
      // ❌ USER or unauthenticated
      throw new Error('Unauthorized: User role cannot manage topics')
    }
  }

  /**
   * Check if current user can manage a specific topic
   *
   * @param {number} topicId - Topic ID to check
   * @returns {Promise<boolean>} - true if can manage, false otherwise
   */
  const canManageTopic = async (topicId) => {
    if (isAdmin.value) {
      // Admin can manage all topics
      return true
    }

    if (isTeacher.value) {
      // Check if teacher is assigned to this topic
      try {
        const response = await teacherTopicAPI.checkAssignment(topicId)
        return response.data.data === true
      } catch (error) {
        console.error('Error checking assignment:', error)
        return false
      }
    }

    // User cannot manage topics
    return false
  }

  /**
   * Create topic - Admin only
   */
  const createTopic = async (moduleType, topicData) => {
    if (!isAdmin.value) {
      throw new Error('Only admins can create topics')
    }
    return await topicAPI.createTopic(moduleType, topicData)
  }

  /**
   * Update topic - Check permission first
   */
  const updateTopic = async (topicId, topicData) => {
    const canManage = await canManageTopic(topicId)
    if (!canManage) {
      throw new Error('You do not have permission to update this topic')
    }

    // Both admin and teacher use the same update endpoint
    return await topicAPI.updateTopic(topicId, topicData)
  }

  /**
   * Delete topic - Admin only
   */
  const deleteTopic = async (topicId) => {
    if (!isAdmin.value) {
      throw new Error('Only admins can delete topics')
    }
    return await topicAPI.deleteTopic(topicId)
  }

  /**
   * Toggle topic status - Check permission first
   */
  const toggleTopicStatus = async (topicId) => {
    const canManage = await canManageTopic(topicId)
    if (!canManage) {
      throw new Error('You do not have permission to toggle this topic')
    }
    return await topicAPI.toggleStatus(topicId)
  }

  /**
   * Fix order indexes - Admin only
   */
  const fixOrderIndexes = async (moduleType) => {
    if (!isAdmin.value) {
      throw new Error('Only admins can fix order indexes')
    }
    return await topicAPI.fixOrderIndexes(moduleType)
  }

  // ==================== RETURN ====================
  return {
    // Role checks
    currentRole,
    isAdmin,
    isTeacher,
    isUser,

    // API methods
    fetchTopics,
    canManageTopic,
    createTopic,
    updateTopic,
    deleteTopic,
    toggleTopicStatus,
    fixOrderIndexes
  }
}
