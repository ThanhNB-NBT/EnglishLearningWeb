import { defineStore } from 'pinia'
import { readingAdminAPI } from '@/api/modules/reading.api'
import { ElMessage } from 'element-plus'

export const useReadingStore = defineStore('reading', {
  state:  () => ({
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
    getQuestionById: (state) => (id) => state.questions. find((q) => q.id === id),
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
    sortedQuestions: (state) => [... state.questions].sort((a, b) => a.orderIndex - b.orderIndex),
  },

  actions: {
    // ==================== LESSONS CRUD ====================

    async fetchLessons(params = {}) {
      this.lessonsLoading = true
      try {
        const { page = 0, size = 10, sort = 'orderIndex,asc' } = params
        console.log('📡 Fetching reading lessons:', { page, size, sort })

        const response = await readingAdminAPI.getAllLessons({ page, size, sort })

        if (response.data.success) {
          const data = response.data.data
          this.lessons = data.content || []
          this.lessonsPagination = {
            page:  data.page ??  page,
            size: data.size ?? size,
            totalElements:  data.totalElements ?? 0,
            totalPages: data.totalPages ?? 0,
          }
          console.log('✅ Fetched lessons:', this.lessons. length)
        }
      } catch (error) {
        console.error('❌ Error fetching lessons:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tải danh sách bài đọc')
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
        const response = await readingAdminAPI.getLessonDetail(lessonId)
        if (response.data.success) {
          this.currentLesson = response.data.data
          console.log('✅ Fetched lesson:', this.currentLesson. title)
          return this.currentLesson
        }
      } catch (error) {
        console.error('❌ Error fetching lesson:', error)
        ElMessage.error('Không thể tải chi tiết bài đọc')
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    async createLesson(lessonData) {
      try {
        const response = await readingAdminAPI.createLesson(lessonData)
        if (response.data.success) {
          ElMessage.success('✅ Tạo bài đọc thành công!')
          return response.data.data
        }
      } catch (error) {
        console.error('❌ Error creating lesson:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tạo bài đọc')
        throw error
      }
    },

    async updateLesson(id, lessonData) {
      try {
        const response = await readingAdminAPI.updateLesson(id, lessonData)
        if (response.data. success) {
          const updatedLesson = response.data. data
          const index = this.lessons.findIndex((l) => l.id === id)
          if (index !== -1) this.lessons[index] = updatedLesson
          if (this.currentLesson?.id === id) this.currentLesson = updatedLesson
          ElMessage.success('✅ Cập nhật bài đọc thành công!')
          return updatedLesson
        }
      } catch (error) {
        console.error('❌ Error updating lesson:', error)
        ElMessage.error(error.response?. data?.message || 'Không thể cập nhật bài đọc')
        throw error
      }
    },

    async deleteLesson(id) {
      try {
        const response = await readingAdminAPI. deleteLesson(id)
        if (response.data.success) {
          this.lessons = this.lessons.filter((l) => l.id !== id)
          if (this.lessonsPagination.totalElements > 0) {
            this.lessonsPagination. totalElements -= 1
          }
          if (this.currentLesson?.id === id) this.currentLesson = null
          ElMessage.success('✅ Xóa bài đọc thành công!')
        }
      } catch (error) {
        console.error('❌ Error deleting lesson:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể xóa bài đọc')
        throw error
      }
    },

    async toggleLessonStatus(id) {
      try {
        const response = await readingAdminAPI.toggleLessonStatus(id)
        if (response.data.success) {
          const lesson = this.lessons. find((l) => l.id === id)
          if (lesson) lesson.isActive = !lesson.isActive
          if (this.currentLesson?.id === id) this.currentLesson. isActive = !this.currentLesson.isActive
          ElMessage.success('✅ Đã thay đổi trạng thái bài đọc!')
        }
      } catch (error) {
        console.error('❌ Error toggling lesson status:', error)
        ElMessage.error('Không thể thay đổi trạng thái')
        throw error
      }
    },

    async getNextLessonOrderIndex() {
      try {
        const response = await readingAdminAPI.getNextLessonOrderIndex()
        if (response.data.success) {
          return response.data.data.nextOrderIndex
        }
      } catch (error) {
        console.error('❌ Error getting next lesson order index:', error)
        const maxOrder =
          this.lessons.length > 0 ? Math.max(...this.lessons.map((l) => l.orderIndex)) : 0
        return maxOrder + 1
      }
    },

    // 🆕 VALIDATE LESSONS ORDER
    async validateLessonsOrder() {
      try {
        const response = await readingAdminAPI.validateAllLessonsOrder()
        if (response.data.success) {
          const result = response.data.data
          if (result.issuesFixed > 0) {
            ElMessage.success(`✅ Đã fix ${result.issuesFixed} vấn đề orderIndex của bài đọc! `)
            await this.fetchLessons({ size: 1000 })
          } else {
            ElMessage.info('✅ OrderIndex của bài đọc đã đúng, không cần fix')
          }
          return result
        }
      } catch (error) {
        console.error('❌ Error validating lessons order:', error)
        ElMessage.error('Không thể validate orderIndex của bài đọc')
        throw error
      }
    },

    clearCurrentLesson() {
      this.currentLesson = null
    },

    clearLessons() {
      this.lessons = []
      this.lessonsPagination = { page:  0, size: 10, totalElements: 0, totalPages: 0 }
    },

    // ==================== QUESTIONS CRUD ====================

    async fetchQuestions(lessonId, params = {}) {
      this.questionsLoading = true
      try {
        const { page = 0, size = 10, sort = 'orderIndex,asc' } = params
        console.log('📡 Fetching questions for lessonId:', lessonId, { page, size, sort })

        const response = await readingAdminAPI.getQuestionsByLesson(lessonId, { page, size, sort })

        if (response.data.success) {
          const data = response.data.data

          // 🔧 Parse metadata cho tất cả questions
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
          this. questionsPagination = {
            page: data.page ?? page,
            size: data.size ??  size,
            totalElements: data.totalElements ?? 0,
            totalPages: data.totalPages ?? 0,
          }
          console.log('✅ Fetched questions:', this.questions.length)
        }
      } catch (error) {
        console.error('❌ Error fetching questions:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tải danh sách câu hỏi')
        this.questions = []
        this.questionsPagination = { page: 0, size:  10, totalElements: 0, totalPages: 0 }
        throw error
      } finally {
        this.questionsLoading = false
      }
    },

    async fetchQuestionById(questionId) {
      this.questionsLoading = true
      try {
        const response = await readingAdminAPI.getQuestionById(questionId)
        if (response.data.success) {
          const question = response.data.data

          // 🔧 Parse metadata nếu là string
          if (question.metadata && typeof question.metadata === 'string') {
            try {
              question.metadata = JSON.parse(question.metadata)
              console.log('✅ Parsed metadata from string to object')
            } catch (e) {
              console.error('❌ Failed to parse metadata:', e)
              question.metadata = {}
            }
          }

          this.currentQuestion = question
          console.log('📝 Fetched question:', this.currentQuestion.questionText)

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
        const response = await readingAdminAPI.createQuestion(questionData)
        if (response. data.success) {
          ElMessage.success('✅ Tạo câu hỏi thành công!')
          return response.data.data
        }
      } catch (error) {
        console.error('❌ Error creating question:', error)
        ElMessage.error(error. response?.data?.message || 'Không thể tạo câu hỏi')
        throw error
      }
    },

    async createQuestionsInBulk(lessonId, questionsPayload) {
      this.questionsLoading = true
      try {
        const response = await readingAdminAPI.createQuestionsInBulk(lessonId, questionsPayload)
        if (response.data. success) {
          return response.data.data
        }
      } catch (error) {
        console.error('❌ Error bulk creating questions:', error)
        ElMessage.error(error.response?. data?.message || 'Lỗi khi tạo hàng loạt')
        throw error
      } finally {
        this.questionsLoading = false
      }
    },

    async updateQuestion(id, questionData) {
      try {
        const response = await readingAdminAPI.updateQuestion(id, questionData)
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
        const response = await readingAdminAPI.deleteQuestion(id)
        if (response. data.success) {
          this.questions = this.questions.filter((q) => q.id !== id)
          if (this. questionsPagination.totalElements > 0) {
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
        const response = await readingAdminAPI.bulkDeleteQuestions(questionIds)
        if (response.data.success) {
          this.questions = this.questions.filter((q) => !questionIds.includes(q.id))
          const deleted = response.data.data. deleted
          if (this.questionsPagination.totalElements >= deleted) {
            this.questionsPagination.totalElements -= deleted
          }
          ElMessage.success(`✅ Đã xóa ${deleted} câu hỏi! `)
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
        const response = await readingAdminAPI.getNextQuestionOrderIndex(lessonId)
        if (response.data. success) {
          return response. data.data.nextOrderIndex
        }
      } catch (error) {
        console.error('❌ Error getting next question order index:', error)
        const maxOrder =
          this.questions. length > 0 ? Math. max(...this.questions.map((q) => q.orderIndex)) : 0
        return maxOrder + 1
      }
    },

    // 🆕 VALIDATE QUESTIONS ORDER
    async validateQuestionsOrder(lessonId) {
      try {
        const response = await readingAdminAPI.validateQuestionsOrder(lessonId)
        if (response.data. success) {
          const result = response.data.data
          if (result.issuesFixed > 0) {
            ElMessage. success(`✅ Đã fix ${result.issuesFixed} vấn đề orderIndex của câu hỏi!`)
            await this.fetchQuestions(lessonId, { size: 1000 })
          } else {
            ElMessage.info('✅ OrderIndex của câu hỏi đã đúng, không cần fix')
          }
          return result
        }
      } catch (error) {
        console.error('❌ Error validating questions order:', error)
        ElMessage.error('Không thể validate orderIndex của câu hỏi')
        throw error
      }
    },

    // 🆕 VALIDATE ALL QUESTIONS ORDER
    async validateAllQuestionsOrder() {
      try {
        const response = await readingAdminAPI.validateAllQuestionsOrder()
        if (response.data.success) {
          const result = response.data.data
          if (result. totalIssuesFixed > 0) {
            ElMessage.success(`✅ Đã fix ${result.totalIssuesFixed} vấn đề orderIndex toàn bộ câu hỏi!`)
          } else {
            ElMessage. info('✅ OrderIndex của tất cả câu hỏi đã đúng')
          }
          return result
        }
      } catch (error) {
        console.error('❌ Error validating all questions order:', error)
        ElMessage.error('Không thể validate orderIndex')
        throw error
      }
    },

    // 🆕 HEALTH CHECK
    async healthCheck() {
      try {
        const response = await readingAdminAPI. healthCheck()
        if (response.data.success) {
          const result = response.data.data
          const summary = result.summary || {}
          const status = summary.status || 'UNKNOWN'

          if (status === 'HEALTHY') {
            ElMessage.success('✅ Module Reading khỏe mạnh, không có vấn đề!')
          } else {
            const totalIssuesFixed = summary.totalIssuesFixed || 0
            ElMessage.warning(`⚠️ Đã phát hiện và fix ${totalIssuesFixed} vấn đề`)
          }

          return result
        }
      } catch (error) {
        console.error('❌ Error health check:', error)
        ElMessage.error('Không thể thực hiện health check')
        throw error
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
      this. lessonsLoading = false
      this.lessonsPagination = { page:  0, size: 10, totalElements: 0, totalPages: 0 }

      this.questions = []
      this. currentQuestion = null
      this. questionsLoading = false
      this.questionsPagination = { page: 0, size: 10, totalElements: 0, totalPages: 0 }
    },
  },
})
