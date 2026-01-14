// src/stores/teacher/topicTeacher.js - FIXED
import { defineStore } from 'pinia'
import { topicAPI } from '@/api' // ✅ Dùng chung topicAPI với admin

export const useTopicTeacherStore = defineStore('topicTeacher', {
  state: () => ({
    grammarTopics: [],
    listeningTopics: [],
    readingTopics: [],
    currentTopic: null,

    grammarLoading: false,
    listeningLoading: false,
    readingLoading: false,

    grammarPagination: { page: 1, size: 100, totalElements: 0, totalPages: 0 },
    listeningPagination: { page: 1, size: 100, totalElements: 0, totalPages: 0 },
    readingPagination: { page: 1, size: 100, totalElements: 0, totalPages: 0 },

    lastError: null,
  }),

  getters: {
    getTopicsByModule: (state) => (moduleType) => {
      const map = {
        GRAMMAR: state.grammarTopics,
        LISTENING: state.listeningTopics,
        READING: state.readingTopics,
      }
      return map[moduleType] || []
    },

    getTopicById: (state) => (id) => {
      return (
        state.grammarTopics.find((t) => t.id === id) ||
        state.listeningTopics.find((t) => t.id === id) ||
        state.readingTopics.find((t) => t.id === id)
      )
    },

    activeGrammarTopics: (state) => state.grammarTopics.filter((t) => t.isActive),
    activeListeningTopics: (state) => state.listeningTopics.filter((t) => t.isActive),
    activeReadingTopics: (state) => state.readingTopics.filter((t) => t.isActive),

    isAnyLoading: (state) => {
      return state.grammarLoading || state.listeningLoading || state.readingLoading
    },
  },

  actions: {
    /**
     * ✅ FIXED: Dùng CHUNG API với admin
     * Backend tự động filter theo role
     */
    async fetchMyTopics(moduleType, params = {}) {
      const loadingKey = `${moduleType.toLowerCase()}Loading`
      const topicsKey = `${moduleType.toLowerCase()}Topics`
      const paginationKey = `${moduleType.toLowerCase()}Pagination`

      this[loadingKey] = true
      this.lastError = null

      try {
        const { page = 1, size = 100, sortBy = 'orderIndex', sortDir = 'asc' } = params

        console.log(`🔄 [Teacher Store] Fetching ${moduleType} topics`)

        // ✅ GỌI CHUNG API /api/admin/topics/{moduleType}
        // Backend tự động nhận biết Teacher và filter
        const response = await topicAPI.getTopicsByModule(moduleType, {
          page,
          size,
          sort: `${sortBy}:${sortDir}`,
        })

        if (response.data.success) {
          const data = response.data.data

          if (!data || !data.content) {
            throw new Error('Invalid response structure')
          }

          // Update topics
          this[topicsKey] = data.content || []

          // Update pagination
          this[paginationKey] = {
            page: data.page || page,
            size: data.size || size,
            totalElements: data.totalElements || 0,
            totalPages: data.totalPages || 0,
          }

          console.log(`✅ [Teacher Store] Loaded ${this[topicsKey].length} topics`)
          return this[topicsKey]
        } else {
          throw new Error(response.data.message || 'Failed to fetch topics')
        }
      } catch (error) {
        console.error(`❌ [Teacher Store] Fetch error: `, error)

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

    /**
     * Helper:  Update topic in arrays
     */
    _updateTopicInArrays(updatedTopic) {
      const moduleKey = `${updatedTopic.moduleType.toLowerCase()}Topics`

      if (this[moduleKey]) {
        const index = this[moduleKey].findIndex((t) => t.id === updatedTopic.id)
        if (index !== -1) {
          this[moduleKey][index] = updatedTopic
        }
      }

      if (this.currentTopic?.id === updatedTopic.id) {
        this.currentTopic = updatedTopic
      }
    },
  },
})
