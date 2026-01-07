// src/stores/admin/topicAdmin.js - FIXED VERSION
import { defineStore } from 'pinia'
import { topicAPI } from '@/api'

/**
 * Topic Admin Store
 * Pure data management - NO UI messages (handled by composables)
 */
export const useTopicAdminStore = defineStore('topicAdmin', {
  state: () => ({
    // Topics by module
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
    // Grammar getters
    activeGrammarTopics: (state) => state.grammarTopics.filter((t) => t.isActive),
    inactiveGrammarTopics: (state) => state.grammarTopics.filter((t) => !t.isActive),

    // Listening getters
    activeListeningTopics: (state) => state.listeningTopics.filter((t) => t.isActive),
    inactiveListeningTopics: (state) => state.listeningTopics.filter((t) => !t.isActive),

    // Reading getters
    activeReadingTopics: (state) => state.readingTopics.filter((t) => t.isActive),
    inactiveReadingTopics: (state) => state.readingTopics.filter((t) => !t.isActive),

    // Get topic by ID (across all modules)
    getTopicById: (state) => (id) => {
      return (
        state.grammarTopics.find((t) => t.id === id) ||
        state.listeningTopics.find((t) => t.id === id) ||
        state.readingTopics.find((t) => t.id === id)
      )
    },

    // Get topics by module type
    getTopicsByModule: (state) => (moduleType) => {
      const map = {
        GRAMMAR: state.grammarTopics,
        LISTENING: state.listeningTopics,
        READING: state.readingTopics,
      }
      return map[moduleType] || []
    },

    // Check if any module is loading
    isAnyLoading: (state) => {
      return state.grammarLoading || state.listeningLoading || state.readingLoading
    },
  },

  actions: {
    // ══════════════════════════════════════════════════════════════
    // FETCH TOPICS (By Module)
    // ══════════════════════════════════════════════════════════════

    async fetchTopicsByModule(moduleType, params = {}) {
      const loadingKey = `${moduleType.toLowerCase()}Loading`
      const topicsKey = `${moduleType.toLowerCase()}Topics`
      const paginationKey = `${moduleType.toLowerCase()}Pagination`

      this[loadingKey] = true
      this.lastError = null

      try {
        const { page = 1, size = 100, sort = 'orderIndex:asc' } = params

        console.log(`📄 [Store] Fetching ${moduleType} topics (page: ${page}, size: ${size})`)

        const response = await topicAPI.getTopicsByModule(moduleType, {
          page,
          size,
          sort,
        })

        if (response.data.success) {
          const data = response.data.data

          // ✅ FIX: Đổi từ "data.data" sang "data.content"
          if (!data || !Array.isArray(data.content)) {
            throw new Error('Invalid response structure from server')
          }

          this[topicsKey] = data.content || []
          this[paginationKey] = {
            page: data.page || 1,
            size: data.size || size,
            totalElements: data.totalElements || 0,
            totalPages: data.totalPages || 0,
          }

          console.log(`✅ [Store] Loaded ${this[topicsKey].length} topics`)
          return this[topicsKey]
        } else {
          throw new Error(response.data.message || 'Failed to fetch topics')
        }
      } catch (error) {
        console.error(`❌ [Store] Fetch error:`, error)

        this.lastError = {
          action: 'fetch',
          moduleType,
          error: error.message,
          timestamp: new Date().toISOString(),
        }

        throw error // Re-throw for composable to handle
      } finally {
        this[loadingKey] = false
      }
    },

    // ══════════════════════════════════════════════════════════════
    // CREATE TOPIC
    // ══════════════════════════════════════════════════════════════

    async createTopic(moduleType, topicData) {
      this.lastError = null

      try {
        console.log('➕ [Store] Creating topic:', { moduleType, topicData })

        // Validate
        if (!topicData.name || !topicData.levelRequired) {
          throw new Error('Missing required fields: name or levelRequired')
        }

        // Clean payload
        const payload = {
          name: topicData.name.trim(),
          description: topicData.description?.trim() || '',
          levelRequired: topicData.levelRequired,
          orderIndex: topicData.orderIndex || null,
          isActive: topicData.isActive ?? true,
        }

        console.log('📤 [Store] Payload:', payload)

        const response = await topicAPI.createTopic(moduleType, payload)

        if (response.data.success) {
          const newTopic = response.data.data

          if (!newTopic || !newTopic.id) {
            throw new Error('Invalid response from server')
          }

          // Add to store
          const topicsKey = `${moduleType.toLowerCase()}Topics`
          this[topicsKey].push(newTopic)

          // Refresh to sync with backend
          await this.fetchTopicsByModule(moduleType)

          console.log('✅ [Store] Topic created:', newTopic.id)
          return newTopic
        } else {
          throw new Error(response.data.message || 'Failed to create topic')
        }
      } catch (error) {
        console.error('❌ [Store] Create error:', error)

        this.lastError = {
          action: 'create',
          moduleType,
          data: topicData,
          error: error.message,
          timestamp: new Date().toISOString(),
        }

        throw error
      }
    },

    // ══════════════════════════════════════════════════════════════
    // UPDATE TOPIC
    // ══════════════════════════════════════════════════════════════

    async updateTopic(topicId, topicData) {
      this.lastError = null

      try {
        console.log('✏️ [Store] Updating topic:', topicId, topicData)

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

        const response = await topicAPI.updateTopic(topicId, payload)

        if (response.data.success) {
          const updatedTopic = response.data.data

          if (!updatedTopic || !updatedTopic.id) {
            throw new Error('Invalid response from server')
          }

          // Update in store
          this._updateTopicInArrays(updatedTopic)

          console.log('✅ [Store] Topic updated:', updatedTopic.id)
          return updatedTopic
        } else {
          throw new Error(response.data.message || 'Failed to update topic')
        }
      } catch (error) {
        console.error('❌ [Store] Update error:', error)

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

    // ══════════════════════════════════════════════════════════════
    // DELETE TOPIC
    // ══════════════════════════════════════════════════════════════

    async deleteTopic(topicId, moduleType) {
      this.lastError = null

      try {
        console.log('🗑️ [Store] Deleting topic:', topicId)

        const response = await topicAPI.deleteTopic(topicId)

        if (response.data.success) {
          // Remove from store
          this._removeTopicFromArrays(topicId)

          // Refresh to get updated order
          await this.fetchTopicsByModule(moduleType)

          console.log('✅ [Store] Topic deleted:', topicId)
        } else {
          throw new Error(response.data.message || 'Failed to delete topic')
        }
      } catch (error) {
        console.error('❌ [Store] Delete error:', error)

        this.lastError = {
          action: 'delete',
          topicId,
          moduleType,
          error: error.message,
          timestamp: new Date().toISOString(),
        }

        throw error
      }
    },

    // ══════════════════════════════════════════════════════════════
    // TOGGLE STATUS
    // ══════════════════════════════════════════════════════════════

    async toggleTopicStatus(topicId) {
      this.lastError = null

      try {
        console.log('🔄 [Store] Toggling topic status:', topicId)

        const response = await topicAPI.toggleStatus(topicId)

        if (response.data.success) {
          const updatedTopic = response.data.data

          if (!updatedTopic || !updatedTopic.id) {
            throw new Error('Invalid response from server')
          }

          // Update in store
          this._updateTopicInArrays(updatedTopic)

          console.log('✅ [Store] Status toggled:', updatedTopic.isActive)
          return updatedTopic
        } else {
          throw new Error(response.data.message || 'Failed to toggle status')
        }
      } catch (error) {
        console.error('❌ [Store] Toggle error:', error)

        this.lastError = {
          action: 'toggle',
          topicId,
          error: error.message,
          timestamp: new Date().toISOString(),
        }

        throw error
      }
    },

    // ══════════════════════════════════════════════════════════════
    // FIX ORDER
    // ══════════════════════════════════════════════════════════════

    async fixOrderIndexes(moduleType) {
      this.lastError = null

      try {
        console.log('🔧 [Store] Fixing order for:', moduleType)

        const response = await topicAPI.fixOrderIndexes(moduleType)

        if (response.data.success) {
          // Refresh to get updated order
          await this.fetchTopicsByModule(moduleType)

          console.log('✅ [Store] Order fixed')
        } else {
          throw new Error(response.data.message || 'Failed to fix order')
        }
      } catch (error) {
        console.error('❌ [Store] Fix order error:', error)

        this.lastError = {
          action: 'fixOrder',
          moduleType,
          error: error.message,
          timestamp: new Date().toISOString(),
        }

        throw error
      }
    },

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    getNextOrderIndex(moduleType) {
      const topics = this.getTopicsByModule(moduleType)
      if (topics.length === 0) return 1

      const maxOrder = Math.max(...topics.map((t) => t.orderIndex || 0))
      return maxOrder + 1
    },

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

    _removeTopicFromArrays(topicId) {
      this.grammarTopics = this.grammarTopics.filter((t) => t.id !== topicId)
      this.listeningTopics = this.listeningTopics.filter((t) => t.id !== topicId)
      this.readingTopics = this.readingTopics.filter((t) => t.id !== topicId)

      if (this.currentTopic?.id === topicId) {
        this.currentTopic = null
      }
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
