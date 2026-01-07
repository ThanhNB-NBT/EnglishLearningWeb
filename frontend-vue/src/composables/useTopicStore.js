// src/composables/useTopicStore.js - FIXED VERSION
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTopicAdminStore } from '@/stores/admin/topicAdmin'
import { useTopicTeacherStore } from '@/stores/teacher/topicTeacher'

/**
 * ✅ Unified Topic Store Composable
 * Automatically selects the correct store based on user role
 * Returns a UNIFIED interface that works for both admin and teacher
 *
 * Usage in components:
 * const topicOps = useTopicStore('GRAMMAR')
 * await topicOps.fetchTopics()
 *
 * Response structure from API:
 * {
 *   status: 200,
 *   message: "Success",
 *   data: {
 *     content: [...],      // ✅ List of topics (changed from "data")
 *     page: 1,
 *     size: 10,
 *     totalElements: 5,
 *     totalPages: 1,
 *     last: true
 *   },
 *   timestamp: "...",
 *   success: true
 * }
 */
export function useTopicStore(moduleType = null) {
  const authStore = useAuthStore()
  const adminStore = useTopicAdminStore()
  const teacherStore = useTopicTeacherStore()

  console.log('🔧 [useTopicStore] Initializing with moduleType:', moduleType)
  console.log('🔧 [useTopicStore] authStore:', authStore ? '✓' : '✗')

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
    if (isAdmin.value) {
      console.log('📦 Using ADMIN topic store')
      return adminStore
    } else if (isTeacher.value) {
      console.log('📦 Using TEACHER topic store')
      return teacherStore
    }
    console.warn('⚠️ No active store - user not authenticated as admin or teacher')
    return null
  })

  // ==================== UNIFIED API ====================

  /**
   * Get topics for current module from active store
   */
  const topics = computed(() => {
    if (!activeStore.value) {
      console.warn('[useTopicStore] No active store')
      return []
    }

    if (!moduleType) {
      console.warn('[useTopicStore] No moduleType provided')
      return []
    }

    const topicsArray = activeStore.value.getTopicsByModule(moduleType)
    console.log(`📊 [useTopicStore] Topics for ${moduleType}:`, topicsArray?.length || 0)
    return topicsArray || []
  })

  /**
   * Get loading state for current module
   */
  const isLoading = computed(() => {
    if (!activeStore.value || !moduleType) return false

    const loadingKey = `${moduleType.toLowerCase()}Loading`
    return activeStore.value[loadingKey] || false
  })

  /**
   * Fetch topics by module
   * Automatically routes to admin or teacher store
   */
  const fetchTopics = async (params = {}) => {
    if (!activeStore.value) {
      throw new Error('No active store - user not authenticated')
    }

    if (!moduleType) {
      throw new Error('moduleType is required')
    }

    console.log(`📄 [useTopicStore] Fetching ${moduleType} topics...`)

    if (isAdmin.value) {
      // Admin: fetch all topics
      return await activeStore.value.fetchTopicsByModule(moduleType, params)
    } else if (isTeacher.value) {
      // Teacher: fetch only assigned topics
      return await activeStore.value.fetchMyTopics(moduleType, params)
    }
  }

  /**
   * Create topic (Admin only)
   */
  const createTopic = async (topicData) => {
    if (!isAdmin.value) {
      throw new Error('Only admins can create topics')
    }
    if (!moduleType) {
      throw new Error('moduleType is required')
    }
    return await activeStore.value.createTopic(moduleType, topicData)
  }

  /**
   * Update topic (Admin and Teacher if assigned)
   */
  const updateTopic = async (topicId, topicData) => {
    if (!activeStore.value) {
      throw new Error('No active store - user not authenticated')
    }
    return await activeStore.value.updateTopic(topicId, topicData)
  }

  /**
   * Delete topic (Admin only)
   */
  const deleteTopic = async (topic) => {
    if (!isAdmin.value) {
      throw new Error('Only admins can delete topics')
    }
    if (!moduleType) {
      throw new Error('moduleType is required')
    }
    return await activeStore.value.deleteTopic(topic.id, moduleType)
  }

  /**
   * Toggle topic status (Admin and Teacher if assigned)
   */
  const toggleStatus = async (topic) => {
    if (!activeStore.value) {
      throw new Error('No active store - user not authenticated')
    }
    return await activeStore.value.toggleTopicStatus(topic.id)
  }

  /**
   * Fix order indexes (Admin only)
   */
  const fixOrderIndexes = async () => {
    if (!isAdmin.value) {
      throw new Error('Only admins can fix order indexes')
    }
    if (!moduleType) {
      throw new Error('moduleType is required')
    }
    return await activeStore.value.fixOrderIndexes(moduleType)
  }

  /**
   * Get topic by ID from store
   */
  const getTopicById = (id) => {
    if (!activeStore.value) return null
    return activeStore.value.getTopicById(id)
  }

  /**
   * Get next order index for new topic
   */
  const getNextOrderIndex = () => {
    if (!activeStore.value) return 1
    return activeStore.value.getNextOrderIndex?.(moduleType) || 1
  }

  /**
   * Check if teacher can manage a topic
   */
  const canManageTopic = async (topicId) => {
    if (isAdmin.value) return true
    if (isTeacher.value && teacherStore.checkAssignment) {
      return await teacherStore.checkAssignment(topicId)
    }
    return false
  }

  // ==================== RETURN UNIFIED INTERFACE ====================
  return {
    // Role info
    currentRole,
    isAdmin,
    isTeacher,

    // State (computed)
    topics,
    isLoading,

    // Store reference (for advanced usage)
    store: activeStore,

    // Actions
    fetchTopics,
    createTopic,
    updateTopic,
    deleteTopic,
    toggleStatus,
    fixOrderIndexes,

    // Helpers
    getTopicById,
    getNextOrderIndex,
    canManageTopic,

    // Direct store access (for debugging)
    adminStore,
    teacherStore,
  }
}
