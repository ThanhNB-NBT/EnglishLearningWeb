// src/stores/teacher/topicTeacher.js
import { defineStore } from 'pinia'
import teacherTopicAPI from '@/api/modules/teacher_topic.api'
import { topicAPI } from '@/api'

/**
 * ✅ Teacher Topic Store
 * Manages topics assigned to the current teacher
 */
export const useTopicTeacherStore = defineStore('topicTeacher', {
  state: () => ({
    // Topics by module (only assigned topics)
    grammarTopics: [],
    listeningTopics: [],
    readingTopics: [],

    // Current topic
    currentTopic: null,

    // Loading states
    grammarLoading: false,
    listeningLoading: false,
    readingLoading: false,

    // Pagination (per module)
    grammarPagination: {
      page: 1,
      size: 100,
      totalElements: 0,
      totalPages: 0,
    },
    listeningPagination: {
      page: 1,
      size: 100,
      totalElements: 0,
      totalPages: 0,
    },
    readingPagination: {
      page: 1,
      size: 100,
      totalElements: 0,
      totalPages: 0,
    },

    // Error tracking
    lastError: null,
  }),

  getters: {
    // Get topics by module type
    getTopicsByModule: (state) => (moduleType) => {
      const map = {
        GRAMMAR: state.grammarTopics,
        LISTENING: state.listeningTopics,
        READING: state.readingTopics,
      }
      return map[moduleType] || []
    },

    // Get topic by ID
    getTopicById: (state) => (id) => {
      return (
        state.grammarTopics.find((t) => t.id === id) ||
        state.listeningTopics.find((t) => t.id === id) ||
        state.readingTopics.find((t) => t.id === id)
      )
    },

    // Active topics
    activeGrammarTopics: (state) => state.grammarTopics.filter((t) => t.isActive),
    activeListeningTopics: (state) => state.listeningTopics.filter((t) => t.isActive),
    activeReadingTopics: (state) => state.readingTopics.filter((t) => t.isActive),

    // Check if any module is loading
    isAnyLoading: (state) => {
      return state.grammarLoading || state.listeningLoading || state.readingLoading
    },
  },

  actions: {
    // ═══════════════════════════════════════════════════════════════
    // FETCH ASSIGNED TOPICS (By Module)
    // ═══════════════════════════════════════════════════════════════

    async fetchMyTopics(moduleType, params = {}) {
      const loadingKey = `${moduleType.toLowerCase()}Loading`
      const topicsKey = `${moduleType.toLowerCase()}Topics`
      const paginationKey = `${moduleType.toLowerCase()}Pagination`

      this[loadingKey] = true
      this.lastError = null

      try {
        const { page = 1, size = 100, sortBy = 'orderIndex', sortDir = 'asc' } = params

        console.log(`🔄 [Teacher Store] Fetching MY ${moduleType} topics`)

        // Call teacher-specific API
        const response = await teacherTopicAPI.getMyTopicsPaginated(
          moduleType,
          page - 1, // Convert to 0-indexed for backend
          size,
          sortBy,
          sortDir
        )

        if (response.data.success) {
          const data = response.data.data

          if (!data || !Array.isArray(data.data)) {
            throw new Error('Invalid response structure from server')
          }

          this[topicsKey] = data.data || []
          this[paginationKey] = {
            page: data.page || 1,
            size: data.size || size,
            totalElements: data.totalElements || 0,
            totalPages: data.totalPages || 0,
          }

          console.log(`✅ [Teacher Store] Loaded ${this[topicsKey].length} assigned topics`)
          return this[topicsKey]
        } else {
          throw new Error(response.data.message || 'Failed to fetch topics')
        }
      } catch (error) {
        console.error(`❌ [Teacher Store] Fetch error:`, error)

        this.lastError = {
          action: 'fetch',
          moduleType,
          error: error.message,
          timestamp: new Date().toISOString(),
        }

        throw error
      } finally {
        this[loadingKey] = false
      }
    },

    // ═══════════════════════════════════════════════════════════════
    // UPDATE TOPIC (Teacher can update assigned topics)
    // ═══════════════════════════════════════════════════════════════

    async updateTopic(topicId, topicData) {
      this.lastError = null

      try {
        console.log('✏️ [Teacher Store] Updating topic:', topicId, topicData)

        // Check if teacher is assigned to this topic first
        const isAssigned = await this.checkAssignment(topicId)
        if (!isAssigned) {
          throw new Error('You are not assigned to manage this topic')
        }

        // Clean payload
        const payload = {}

        if (topicData.name !== undefined) {
          payload.name = topicData.name.trim()
        }
        if (topicData.description !== undefined) {
          payload.description = topicData.description?.trim() || ''
        }
        if (topicData.levelRequired !== undefined) {
          payload.levelRequired = topicData.levelRequired
        }
        if (topicData.orderIndex !== undefined) {
          payload.orderIndex = Number(topicData.orderIndex)
        }
        if (topicData.isActive !== undefined) {
          payload.isActive = Boolean(topicData.isActive)
        }

        // Use the same update endpoint as admin
        const response = await topicAPI.updateTopic(topicId, payload)

        if (response.data.success) {
          const updatedTopic = response.data.data

          if (!updatedTopic || !updatedTopic.id) {
            throw new Error('Invalid response from server')
          }

          // Update in store
          this._updateTopicInArrays(updatedTopic)

          console.log('✅ [Teacher Store] Topic updated:', updatedTopic.id)
          return updatedTopic
        } else {
          throw new Error(response.data.message || 'Failed to update topic')
        }
      } catch (error) {
        console.error('❌ [Teacher Store] Update error:', error)

        this.lastError = {
          action: 'update',
          topicId,
          data: topicData,
          error: error.message,
          timestamp: new Date().toISOString(),
        }

        throw error
      }
    },

    // ═══════════════════════════════════════════════════════════════
    // TOGGLE STATUS (Teacher can toggle assigned topics)
    // ═══════════════════════════════════════════════════════════════

    async toggleTopicStatus(topicId) {
      this.lastError = null

      try {
        console.log('🔄 [Teacher Store] Toggling topic status:', topicId)

        // Check assignment first
        const isAssigned = await this.checkAssignment(topicId)
        if (!isAssigned) {
          throw new Error('You are not assigned to manage this topic')
        }

        const response = await topicAPI.toggleStatus(topicId)

        if (response.data.success) {
          const updatedTopic = response.data.data

          if (!updatedTopic || !updatedTopic.id) {
            throw new Error('Invalid response from server')
          }

          // Update in store
          this._updateTopicInArrays(updatedTopic)

          console.log('✅ [Teacher Store] Status toggled:', updatedTopic.isActive)
          return updatedTopic
        } else {
          throw new Error(response.data.message || 'Failed to toggle status')
        }
      } catch (error) {
        console.error('❌ [Teacher Store] Toggle error:', error)

        this.lastError = {
          action: 'toggle',
          topicId,
          error: error.message,
          timestamp: new Date().toISOString(),
        }

        throw error
      }
    },

    // ═══════════════════════════════════════════════════════════════
    // CHECK ASSIGNMENT
    // ═══════════════════════════════════════════════════════════════

    async checkAssignment(topicId) {
      try {
        const response = await teacherTopicAPI.checkAssignment(topicId)
        return response.data.data === true
      } catch (error) {
        console.error('❌ Error checking assignment:', error)
        return false
      }
    },

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    _updateTopicInArrays(updatedTopic) {
      const updateInArray = (array) => {
        const index = array.findIndex((t) => t.id === updatedTopic.id)
        if (index !== -1) {
          array[index] = { ...array[index], ...updatedTopic }
        }
      }

      switch (updatedTopic.moduleType) {
        case 'GRAMMAR':
          updateInArray(this.grammarTopics)
          break
        case 'LISTENING':
          updateInArray(this.listeningTopics)
          break
        case 'READING':
          updateInArray(this.readingTopics)
          break
      }

      if (this.currentTopic?.id === updatedTopic.id) {
        this.currentTopic = updatedTopic
      }
    },

    /**
     * Get next available order index for a module
     */
    getNextOrderIndex(moduleType) {
      const topics = this.getTopicsByModule(moduleType)
      if (!topics || topics.length === 0) return 1

      const maxOrder = Math.max(...topics.map(t => t.orderIndex || 0))
      return maxOrder + 1
    },

    reset() {
      this.grammarTopics = []
      this.listeningTopics = []
      this.readingTopics = []
      this.currentTopic = null
      this.grammarLoading = false
      this.listeningLoading = false
      this.readingLoading = false
      this.lastError = null
    },
  },
})
