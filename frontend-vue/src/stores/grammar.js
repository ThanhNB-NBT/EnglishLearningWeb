// src/stores/grammar.js
import { defineStore } from 'pinia'
import { grammarAdminAPI } from '@/api/modules/grammar.api'
import { ElMessage } from 'element-plus'

export const useGrammarStore = defineStore('grammar', {
  state: () => ({
    // Topics
    topics: [],
    currentTopic: null,
    topicsLoading: false,
    topicsPagination: {
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    },

    // Lessons
    lessons: [],
    currentLesson: null,
    lessonsLoading: false,
    lessonsPagination: {
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    },

    // Questions (sẽ dùng sau)
    questions: [],
    currentQuestion: null,
    questionsLoading: false,
    questionsPagination: {
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    },
  }),

  getters: {
    // Topics getters
    activeTopics: (state) => state.topics.filter((t) => t.isActive),
    inactiveTopics: (state) => state.topics.filter((t) => !t.isActive),
    getTopicById: (state) => (id) => state.topics.find((t) => t.id === id),

    // Lessons getters
    activeLessons: (state) => state.lessons.filter((l) => l.isActive),
    inactiveLessons: (state) => state.lessons.filter((l) => !l.isActive),
    getLessonById: (state) => (id) => state.lessons.find((l) => l.id === id),
    theoryLessons: (state) => state.lessons.filter((l) => l.lessonType === 'THEORY'),
    practiceLessons: (state) => state.lessons.filter((l) => l.lessonType === 'PRACTICE'),

    // Questions getters
    getQuestionById: (state) => (id) => state.questions.find((q) => q.id === id),
    getQuestionsByType: (state) => (type) => state.questions.filter((q) => q.questionType === type),
    multipleChoiceQuestions: (state) =>
      state.questions.filter((q) => q.questionType === 'MULTIPLE_CHOICE'),
    trueFalseQuestions: (state) => state.questions.filter((q) => q.questionType === 'TRUE_FALSE'),
    fillBlankQuestions: (state) => state.questions.filter((q) => q.questionType === 'FILL_BLANK'),
    questionCountByType: (state) => {
      const counts = {}
      state.questions.forEach((q) => {
        const type = q.questionType
        counts[type] = (counts[type] || 0) + 1
      })
      return counts
    },
    totalQuestionPoints: (state) => state.questions.reduce((sum, q) => sum + (q.points || 0), 0),
    hasQuestions: (state) => state.questions.length > 0,
    sortedQuestions: (state) => [...state.questions].sort((a, b) => a.orderIndex - b.orderIndex),
  },

  actions: {
    // ==================== TOPICS CRUD ====================

    async fetchTopics(params = {}) {
      this.topicsLoading = true
      try {
        const { page = 0, size = 10, sort = 'orderIndex,asc' } = params
        const response = await grammarAdminAPI.getAllTopics({ page, size, sort })

        if (response.data.success) {
          const data = response.data.data
          this.topics = data.content || []
          this.topicsPagination = {
            page: data.page ?? page,
            size: data.size ?? size,
            totalElements: data.totalElements ?? 0,
            totalPages: data.totalPages ?? 0,
          }
        }
      } catch (error) {
        console.error('Error fetching topics:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tải danh sách topics')
        this.topics = []
        this.topicsPagination = { page: 0, size: 10, totalElements: 0, totalPages: 0 }
        throw error
      } finally {
        this.topicsLoading = false
      }
    },

    async fetchTopicById(id) {
      this.topicsLoading = true
      try {
        const response = await grammarAdminAPI.getTopicById(id)
        if (response.data.success) {
          this.currentTopic = response.data.data
          return this.currentTopic
        }
      } catch (error) {
        console.error('Error fetching topic:', error)
        ElMessage.error('Không thể tải chi tiết topic')
        throw error
      } finally {
        this.topicsLoading = false
      }
    },

    async createTopic(topicData) {
      try {
        const response = await grammarAdminAPI.createTopic(topicData)
        if (response.data.success) {
          ElMessage.success('Tạo topic thành công!')
          return response.data.data
        }
      } catch (error) {
        console.error('Error creating topic:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tạo topic')
        throw error
      }
    },

    async updateTopic(id, topicData) {
      try {
        const response = await grammarAdminAPI.updateTopic(id, topicData)
        if (response.data.success) {
          const updatedTopic = response.data.data
          const index = this.topics.findIndex((t) => t.id === id)
          if (index !== -1) this.topics[index] = updatedTopic
          if (this.currentTopic?.id === id) this.currentTopic = updatedTopic
          ElMessage.success('Cập nhật topic thành công!')
          return updatedTopic
        }
      } catch (error) {
        console.error('Error updating topic:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể cập nhật topic')
        throw error
      }
    },

    async deleteTopic(id) {
      try {
        const response = await grammarAdminAPI.deleteTopic(id)
        if (response.data.success) {
          this.topics = this.topics.filter((t) => t.id !== id)
          if (this.topicsPagination.totalElements > 0) {
            this.topicsPagination.totalElements -= 1
          }
          if (this.currentTopic?.id === id) this.currentTopic = null
          ElMessage.success('Xóa topic thành công!')
        }
      } catch (error) {
        console.error('Error deleting topic:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể xóa topic')
        throw error
      }
    },

    async deactivateTopic(id) {
      try {
        const response = await grammarAdminAPI.deactivateTopic(id)
        if (response.data.success) {
          const topic = this.topics.find((t) => t.id === id)
          if (topic) topic.isActive = false
          if (this.currentTopic?.id === id) this.currentTopic.isActive = false
          ElMessage.success(' Đã tắt topic!')
        }
      } catch (error) {
        console.error('Error deactivating topic:', error)
        ElMessage.error('Không thể tắt topic')
        throw error
      }
    },

    async getNextTopicOrderIndex() {
      try {
        const response = await grammarAdminAPI.getNextTopicOrderIndex()
        if (response.data.success) {
          return response.data.data.nextOrderIndex
        }
      } catch (error) {
        console.error(' Error getting next order index:', error)
        const maxOrder =
          this.topics.length > 0 ? Math.max(...this.topics.map((t) => t.orderIndex)) : 0
        return maxOrder + 1
      }
    },

    async validateTopicsOrder() {
      try {
        const response = await grammarAdminAPI.validateAllTopicsOrder()
        if (response.data.success) {
          const result = response.data.data
          if (result.issuesFixed > 0) {
            ElMessage.success(` Đã fix ${result.issuesFixed} vấn đề orderIndex!`)
            await this.fetchTopics()
          } else {
            ElMessage.info(' OrderIndex đã đúng, không cần fix')
          }
          return result
        }
      } catch (error) {
        console.error(' Error validating topics order:', error)
        ElMessage.error('Không thể validate orderIndex')
        throw error
      }
    },

    clearCurrentTopic() {
      this.currentTopic = null
    },

    // ==================== LESSONS CRUD ====================

    async fetchAllLessons() {
      try {
        const response = await grammarAdminAPI.getAllLessons({ size: 1000 })
        if (response.data.success) {
          return response.data.data.content || []
        }
      } catch (error) {
        console.error('Error fetching all lessons:', error)
        return []
      }
    },

    async fetchLessons(topicId, params = {}) {
      this.lessonsLoading = true
      try {
        const { page = 0, size = 10, sort = 'orderIndex,asc' } = params
        console.log('📡 Fetching lessons for topicId:', topicId, { page, size, sort })

        const response = await grammarAdminAPI.getLessonsByTopic(topicId, { page, size, sort })

        if (response.data.success) {
          const data = response.data.data
          this.lessons = data.content || []
          this.lessonsPagination = {
            page: data.page ?? page,
            size: data.size ?? size,
            totalElements: data.totalElements ?? 0,
            totalPages: data.totalPages ?? 0,
          }
          console.log(' Fetched lessons:', this.lessons.length)
        }
      } catch (error) {
        console.error(' Error fetching lessons:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tải danh sách lessons')
        this.lessons = []
        this.lessonsPagination = { page: 0, size: 10, totalElements: 0, totalPages: 0 }
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    async fetchLessonById(lessonId) {
      this.lessonsLoading = true
      try {
        const response = await grammarAdminAPI.getLessonDetail(lessonId)
        if (response.data.success) {
          this.currentLesson = response.data.data
          console.log(' Fetched lesson:', this.currentLesson.title)
          return this.currentLesson
        }
      } catch (error) {
        console.error(' Error fetching lesson:', error)
        ElMessage.error('Không thể tải chi tiết lesson')
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    async createLesson(lessonData) {
      try {
        const response = await grammarAdminAPI.createLesson(lessonData)
        if (response.data.success) {
          ElMessage.success(' Tạo lesson thành công!')
          return response.data.data
        }
      } catch (error) {
        console.error(' Error creating lesson:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tạo lesson')
        throw error
      }
    },

    async updateLesson(id, lessonData) {
      try {
        const response = await grammarAdminAPI.updateLesson(id, lessonData)
        if (response.data.success) {
          const updatedLesson = response.data.data
          const index = this.lessons.findIndex((l) => l.id === id)
          if (index !== -1) this.lessons[index] = updatedLesson
          if (this.currentLesson?.id === id) this.currentLesson = updatedLesson
          ElMessage.success(' Cập nhật lesson thành công!')
          return updatedLesson
        }
      } catch (error) {
        console.error(' Error updating lesson:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể cập nhật lesson')
        throw error
      }
    },

    async deleteLesson(id, cascade = false) {
      try {
        const response = await grammarAdminAPI.deleteLesson(id, cascade)
        if (response.data.success) {
          this.lessons = this.lessons.filter((l) => l.id !== id)
          if (this.lessonsPagination.totalElements > 0) {
            this.lessonsPagination.totalElements -= 1
          }
          if (this.currentLesson?.id === id) this.currentLesson = null
          ElMessage.success(' Xóa lesson thành công!')
        }
      } catch (error) {
        console.error(' Error deleting lesson:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể xóa lesson')
        throw error
      }
    },

    async deactivateLesson(id) {
      try {
        const response = await grammarAdminAPI.deactivateLesson(id)
        if (response.data.success) {
          const lesson = this.lessons.find((l) => l.id === id)
          if (lesson) lesson.isActive = false
          if (this.currentLesson?.id === id) this.currentLesson.isActive = false
          ElMessage.success(' Đã tắt lesson!')
        }
      } catch (error) {
        console.error(' Error deactivating lesson:', error)
        ElMessage.error('Không thể tắt lesson')
        throw error
      }
    },

    async getNextLessonOrderIndex(topicId) {
      try {
        const response = await grammarAdminAPI.getNextLessonOrderIndex(topicId)
        if (response.data.success) {
          return response.data.data.nextOrderIndex
        }
      } catch (error) {
        console.error(' Error getting next lesson order index:', error)
        const maxOrder =
          this.lessons.length > 0 ? Math.max(...this.lessons.map((l) => l.orderIndex)) : 0
        return maxOrder + 1
      }
    },

    async validateLessonsOrder(topicId) {
      try {
        const response = await grammarAdminAPI.validateLessonOrder(topicId)
        if (response.data.success) {
          const result = response.data.data
          if (result.issuesFixed > 0) {
            ElMessage.success(` Đã fix ${result.issuesFixed} vấn đề orderIndex!`)
            await this.fetchLessons(topicId)
          } else {
            ElMessage.info(' OrderIndex đã đúng, không cần fix')
          }
          return result
        }
      } catch (error) {
        console.error(' Error validating lessons order:', error)
        ElMessage.error('Không thể validate orderIndex')
        throw error
      }
    },

    clearCurrentLesson() {
      this.currentLesson = null
    },

    clearLessons() {
      this.lessons = []
      this.lessonsPagination = { page: 0, size: 10, totalElements: 0, totalPages: 0 }
    },

    // ==================== QUESTIONS CRUD (ADD TO EXISTING STORE) ====================

    async fetchQuestions(lessonId, params = {}) {
      this.questionsLoading = true
      try {
        const { page = 0, size = 10, sort = 'orderIndex,asc' } = params
        console.log('Fetching questions for lessonId:', lessonId, { page, size, sort })

        const response = await grammarAdminAPI.getQuestionsByLesson(lessonId, { page, size, sort })

        if (response.data.success) {
          const data = response.data.data
          this.questions = data.content || []
          this.questionsPagination = {
            page: data.page ?? page,
            size: data.size ?? size,
            totalElements: data.totalElements ?? 0,
            totalPages: data.totalPages ?? 0,
          }
          console.log('Fetched questions:', this.questions.length)
        }
      } catch (error) {
        console.error('Error fetching questions:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tải danh sách questions')
        this.questions = []
        this.questionsPagination = { page: 0, size: 10, totalElements: 0, totalPages: 0 }
        throw error
      } finally {
        this.questionsLoading = false
      }
    },

    async fetchQuestionById(questionId) {
      this.questionsLoading = true
      try {
        const response = await grammarAdminAPI.getQuestionById(questionId)
        if (response.data.success) {
          this.currentQuestion = response.data.data
          console.log('Fetched question:', this.currentQuestion.questionText)
          return this.currentQuestion
        }
      } catch (error) {
        console.error('Error fetching question:', error)
        ElMessage.error('Không thể tải chi tiết question')
        throw error
      } finally {
        this.questionsLoading = false
      }
    },

    async createQuestion(questionData) {
      try {
        const response = await grammarAdminAPI.createQuestion(questionData)
        if (response.data.success) {
          ElMessage.success(' Tạo question thành công!')
          return response.data.data
        }
      } catch (error) {
        console.error(' Error creating question:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tạo question')
        throw error
      }
    },

    async createQuestionsInBulk(lessonId, questionsPayload) {
      this.questionsLoading = true // Dùng loading của questions
      try {
        const response = await grammarAdminAPI.createQuestionsInBulk(lessonId, questionsPayload)
        if (response.data.success) {
          return response.data.data
        }
      } catch (error) {
        console.error('Error bulk creating questions:', error)
        ElMessage.error(error.response?.data?.message || 'Lỗi khi tạo hàng loạt')
        throw error
      } finally {
        this.questionsLoading = false
      }
    },

    async updateQuestion(id, questionData) {
      try {
        const response = await grammarAdminAPI.updateQuestion(id, questionData)
        if (response.data.success) {
          const updatedQuestion = response.data.data
          const index = this.questions.findIndex((q) => q.id === id)
          if (index !== -1) this.questions[index] = updatedQuestion
          if (this.currentQuestion?.id === id) this.currentQuestion = updatedQuestion
          ElMessage.success(' Cập nhật question thành công!')
          return updatedQuestion
        }
      } catch (error) {
        console.error(' Error updating question:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể cập nhật question')
        throw error
      }
    },

    async deleteQuestion(id) {
      try {
        const response = await grammarAdminAPI.deleteQuestion(id)
        if (response.data.success) {
          this.questions = this.questions.filter((q) => q.id !== id)
          if (this.questionsPagination.totalElements > 0) {
            this.questionsPagination.totalElements -= 1
          }
          if (this.currentQuestion?.id === id) this.currentQuestion = null
          ElMessage.success(' Xóa question thành công!')
        }
      } catch (error) {
        console.error(' Error deleting question:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể xóa question')
        throw error
      }
    },

    async bulkDeleteQuestions(questionIds) {
      try {
        const response = await grammarAdminAPI.bulkDeleteQuestions(questionIds)
        if (response.data.success) {
          this.questions = this.questions.filter((q) => !questionIds.includes(q.id))
          const deleted = response.data.data.deleted
          if (this.questionsPagination.totalElements >= deleted) {
            this.questionsPagination.totalElements -= deleted
          }
          ElMessage.success(` Đã xóa ${deleted} questions!`)
          return response.data.data
        }
      } catch (error) {
        console.error(' Error bulk deleting questions:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể xóa hàng loạt')
        throw error
      }
    },

    async getNextQuestionOrderIndex(lessonId) {
      try {
        const response = await grammarAdminAPI.getNextQuestionOrderIndex(lessonId)
        if (response.data.success) {
          return response.data.data.nextOrderIndex
        }
      } catch (error) {
        console.error(' Error getting next question order index:', error)
        const maxOrder =
          this.questions.length > 0 ? Math.max(...this.questions.map((q) => q.orderIndex)) : 0
        return maxOrder + 1
      }
    },

    clearCurrentQuestion() {
      this.currentQuestion = null
    },

    clearQuestions() {
      this.questions = []
      this.questionsPagination = { page: 0, size: 10, totalElements: 0, totalPages: 0 }
    },

    // ==================== RESET ====================
    reset() {
      this.topics = []
      this.currentTopic = null
      this.topicsLoading = false
      this.topicsPagination = { page: 0, size: 10, totalElements: 0, totalPages: 0 }

      this.lessons = []
      this.currentLesson = null
      this.lessonsLoading = false
      this.lessonsPagination = { page: 0, size: 10, totalElements: 0, totalPages: 0 }

      this.questions = []
      this.currentQuestion = null
      this.questionsLoading = false
      this.questionsPagination = { page: 0, size: 10, totalElements: 0, totalPages: 0 }
    },
  },
})
