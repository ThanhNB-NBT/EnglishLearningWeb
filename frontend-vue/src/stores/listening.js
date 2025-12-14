// src/stores/listening.js
import { defineStore } from 'pinia'
import { listeningAdminAPI } from '@/api/modules/listening.api'
import { ElMessage } from 'element-plus'

export const useListeningStore = defineStore('listening', {
  state: () => ({
    // Lessons
    lessons: [],
    currentLesson: null,
    lessonsLoading: false,

    // Questions
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
    // Lessons getters
    activeLessons: (state) => state.lessons.filter((l) => l.isActive),
    inactiveLessons: (state) => state.lessons.filter((l) => !l.isActive),
    getLessonById: (state) => (id) => state.lessons.find((l) => l.id === id),

    // Questions getters
    getQuestionById: (state) => (id) => state.questions.find((q) => q.id === id),
    getQuestionsByType: (state) => (type) => state.questions.filter((q) => q.questionType === type),
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
    // ==================== LESSONS CRUD ====================

    async fetchLessons() {
      this.lessonsLoading = true
      try {
        const response = await listeningAdminAPI.getAllLessons()
        if (response.data.success) {
          this.lessons = response.data.data || []
          console.log('✅ Fetched listening lessons:', this.lessons.length)
        }
      } catch (error) {
        console.error('❌ Error fetching lessons:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tải danh sách bài nghe')
        this.lessons = []
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    async fetchLessonById(lessonId) {
      this.lessonsLoading = true
      try {
        const response = await listeningAdminAPI.getLessonById(lessonId)
        if (response.data.success) {
          this.currentLesson = response.data.data
          console.log('✅ Fetched lesson:', this.currentLesson.title)
          return this.currentLesson
        }
      } catch (error) {
        console.error('❌ Error fetching lesson:', error)
        ElMessage.error('Không thể tải chi tiết bài nghe')
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    async createLesson(formData) {
      try {
        const response = await listeningAdminAPI.createLesson(formData)
        if (response.data.success) {
          ElMessage.success('✅ Tạo bài nghe thành công!')
          return response.data.data
        }
      } catch (error) {
        console.error('❌ Error creating lesson:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tạo bài nghe')
        throw error
      }
    },

    async updateLesson(id, formData) {
      try {
        const response = await listeningAdminAPI.updateLesson(id, formData)
        if (response.data.success) {
          const updatedLesson = response.data.data
          const index = this.lessons.findIndex((l) => l.id === id)
          if (index !== -1) this.lessons[index] = updatedLesson
          if (this.currentLesson?.id === id) this.currentLesson = updatedLesson
          ElMessage.success('✅ Cập nhật bài nghe thành công!')
          return updatedLesson
        }
      } catch (error) {
        console.error('❌ Error updating lesson:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể cập nhật bài nghe')
        throw error
      }
    },

    async deleteLesson(id) {
      try {
        const response = await listeningAdminAPI.deleteLesson(id)
        if (response.data.success) {
          this.lessons = this.lessons.filter((l) => l.id !== id)
          if (this.currentLesson?.id === id) this.currentLesson = null
          ElMessage.success('✅ Xóa bài nghe thành công!')
        }
      } catch (error) {
        console.error('❌ Error deleting lesson:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể xóa bài nghe')
        throw error
      }
    },

    async getNextLessonOrderIndex() {
      try {
        const maxOrder = this.lessons.length > 0
          ? Math.max(...this.lessons.map((l) => l.orderIndex))
          : 0
        return maxOrder + 1
      } catch (error) {
        console.error('❌ Error getting next order index:', error)
        return 1
      }
    },

    clearCurrentLesson() {
      this.currentLesson = null
    },

    clearLessons() {
      this.lessons = []
    },

    // ==================== QUESTIONS CRUD ====================

    async fetchQuestions(lessonId, params = {}) {
      this.questionsLoading = true
      try {
        const { page = 0, size = 1000, sort = 'orderIndex,asc' } = params
        console.log('📡 Fetching questions for lessonId:', lessonId)

        const response = await listeningAdminAPI.getQuestionsByLesson(lessonId, { page, size, sort })

        if (response.data.success) {
          const data = response.data.data

          // Parse metadata
          const questions = (data.content || []).map((question) => {
            if (question.metadata && typeof question.metadata === 'string') {
              try {
                question.metadata = JSON.parse(question.metadata)
              } catch (e) {
                console.error(`❌ Failed to parse metadata for question ${question.id}:`, e)
                question.metadata = {}
              }
            }
            return question
          })

          this.questions = questions
          this.questionsPagination = {
            page: data.page ?? page,
            size: data.size ?? size,
            totalElements: data.totalElements ?? 0,
            totalPages: data.totalPages ?? 0,
          }
          console.log('✅ Fetched questions:', this.questions.length)
        }
      } catch (error) {
        console.error('❌ Error fetching questions:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tải danh sách câu hỏi')
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
        const response = await listeningAdminAPI.getQuestionById(questionId)
        if (response.data.success) {
          const question = response.data.data

          if (question.metadata && typeof question.metadata === 'string') {
            try {
              question.metadata = JSON.parse(question.metadata)
            } catch (e) {
              console.error('❌ Failed to parse metadata:', e)
              question.metadata = {}
            }
          }

          this.currentQuestion = question
          return this.currentQuestion
        }
      } catch (error) {
        console.error('❌ Error fetching question:', error)
        ElMessage.error('Không thể tải chi tiết câu hỏi')
        throw error
      } finally {
        this.questionsLoading = false
      }
    },

    async createQuestion(questionData) {
      try {
        const response = await listeningAdminAPI.createQuestion(questionData)
        if (response.data.success) {
          ElMessage.success('✅ Tạo câu hỏi thành công!')
          return response.data.data
        }
      } catch (error) {
        console.error('❌ Error creating question:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tạo câu hỏi')
        throw error
      }
    },

    async createQuestionsInBulk(lessonId, questionsPayload) {
      this.questionsLoading = true
      try {
        const response = await listeningAdminAPI.createQuestionsInBulk(lessonId, questionsPayload)
        if (response.data.success) {
          return response.data.data
        }
      } catch (error) {
        console.error('❌ Error bulk creating questions:', error)
        ElMessage.error(error.response?.data?.message || 'Lỗi khi tạo hàng loạt')
        throw error
      } finally {
        this.questionsLoading = false
      }
    },

    async updateQuestion(id, questionData) {
      try {
        const response = await listeningAdminAPI.updateQuestion(id, questionData)
        if (response.data.success) {
          const updatedQuestion = response.data.data
          const index = this.questions.findIndex((q) => q.id === id)
          if (index !== -1) this.questions[index] = updatedQuestion
          if (this.currentQuestion?.id === id) this.currentQuestion = updatedQuestion
          ElMessage.success('✅ Cập nhật câu hỏi thành công!')
          return updatedQuestion
        }
      } catch (error) {
        console.error('❌ Error updating question:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể cập nhật câu hỏi')
        throw error
      }
    },

    async deleteQuestion(id) {
      try {
        const response = await listeningAdminAPI.deleteQuestion(id)
        if (response.data.success) {
          this.questions = this.questions.filter((q) => q.id !== id)
          if (this.questionsPagination.totalElements > 0) {
            this.questionsPagination.totalElements -= 1
          }
          if (this.currentQuestion?.id === id) this.currentQuestion = null
          ElMessage.success('✅ Xóa câu hỏi thành công!')
        }
      } catch (error) {
        console.error('❌ Error deleting question:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể xóa câu hỏi')
        throw error
      }
    },

    async bulkDeleteQuestions(questionIds) {
      try {
        const response = await listeningAdminAPI.bulkDeleteQuestions(questionIds)
        if (response.data.success) {
          this.questions = this.questions.filter((q) => !questionIds.includes(q.id))
          const deleted = response.data.data.deleted
          if (this.questionsPagination.totalElements >= deleted) {
            this.questionsPagination.totalElements -= deleted
          }
          ElMessage.success(`✅ Đã xóa ${deleted} câu hỏi!`)
          return response.data.data
        }
      } catch (error) {
        console.error('❌ Error bulk deleting questions:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể xóa hàng loạt')
        throw error
      }
    },

    async getNextQuestionOrderIndex(lessonId) {
      try {
        const response = await listeningAdminAPI.getNextQuestionOrderIndex(lessonId)
        if (response.data.success) {
          return response.data.data.nextOrderIndex
        }
      } catch (error) {
        console.error('❌ Error getting next question order index:', error)
        const maxOrder = this.questions.length > 0
          ? Math.max(...this.questions.map((q) => q.orderIndex))
          : 0
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
      this.lessons = []
      this.currentLesson = null
      this.lessonsLoading = false

      this.questions = []
      this.currentQuestion = null
      this.questionsLoading = false
      this.questionsPagination = { page: 0, size: 10, totalElements: 0, totalPages: 0 }
    },
  },
})
