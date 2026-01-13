// src/composables/useTopicStore.js - SIMPLIFIED
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTopicAdminStore } from '@/stores/admin/topicAdmin'
import { useTopicTeacherStore } from '@/stores/teacher/topicTeacher'

export function useTopicStore(moduleType = null) {
  const authStore = useAuthStore()
  const adminStore = useTopicAdminStore()
  const teacherStore = useTopicTeacherStore()

  // Role detection
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

  // Store selection
  const activeStore = computed(() => {
    if (isAdmin.value) return adminStore
    if (isTeacher.value) return teacherStore
    return null
  })

  // Unified API
  const topics = computed(() => {
    if (! activeStore.value || !moduleType) return []
    return activeStore.value.getTopicsByModule(moduleType) || []
  })

  const isLoading = computed(() => {
    if (!activeStore.value || !moduleType) return false
    const loadingKey = `${moduleType.toLowerCase()}Loading`
    return activeStore.value[loadingKey] || false
  })

  /**
   * ✅ SIMPLIFIED: Both call the same method
   */
  const fetchTopics = async (params = {}) => {
    if (!activeStore.value || !moduleType) {
      throw new Error('Store or moduleType not available')
    }

    console.log(`📄 [useTopicStore] Fetching ${moduleType} for ${currentRole.value}`)

    // ✅ Both admin and teacher call fetchTopicsByModule / fetchMyTopics
    // They both call topicAPI.getTopicsByModule() now
    // Backend handles the filtering

    if (isAdmin.value) {
      await adminStore.fetchTopicsByModule(moduleType, params)
    } else if (isTeacher.value) {
      await teacherStore.fetchMyTopics(moduleType, params)
    }

    console.log(`✅ [useTopicStore] Fetched ${topics.value.length} topics`)
  }

  const createTopic = async (topicData) => {
    if (!isAdmin.value) throw new Error('Only admins can create topics')
    if (!moduleType) throw new Error('moduleType required')
    return await activeStore.value.createTopic(moduleType, topicData)
  }

  const updateTopic = async (topicId, topicData) => {
    if (!isAdmin.value) throw new Error('Only admins can update topics')
    return await activeStore.value.updateTopic(topicId, topicData)
  }

  const deleteTopic = async (topic) => {
    if (!isAdmin.value) throw new Error('Only admins can delete topics')
    if (!moduleType) throw new Error('moduleType required')
    return await activeStore.value.deleteTopic(topic.id, moduleType)
  }

  const toggleStatus = async (topicId) => {
    if (!isAdmin.value) throw new Error('Only admins can toggle status')
    return await activeStore.value.toggleTopicStatus(topicId)
  }

  const getTopicById = (topicId) => {
    if (!activeStore.value) return null
    return activeStore.value.getTopicById(topicId)
  }

  return {
    topics,
    isLoading,
    currentRole,
    isAdmin,
    isTeacher,
    fetchTopics,
    createTopic,
    updateTopic,
    deleteTopic,
    toggleStatus,
    getTopicById,
    activeStore,
  }
}
