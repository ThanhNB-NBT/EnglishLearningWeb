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
      size:  10,
      totalElements:  0,
      totalPages:  0,
    },
  }),

  getters: {
    // Lessons getters
    activeLessons: (state) => state.lessons. filter((l) => l.isActive),
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
    sortedQuestions: (state) => [... state.questions].sort((a, b) => a.orderIndex - b.orderIndex),
  },

  actions: {
    // ═════════════════════════════════════════════════════════════════
    // LESSONS CRUD
    // ═════════════════════════════════════════════════════════════════

    /**
     *  Fetch lessons với pagination
     */
    async fetchLessons(params = {}) {
      this.lessonsLoading = true
      try {
        const { page = 0, size = 10, sort = 'orderIndex,asc' } = params

        const response = await listeningAdminAPI.getAllLessons({ page, size, sort })

        if (response.data.success) {
          const data = response.data.data

          //  Handle paginated response
          if (data.content) {
            this.lessons = data.content
            this.lessonsPagination = {
              page: data.number,
              size: data.size,
              totalElements: data.totalElements,
              totalPages: data.totalPages,
            }
          } else {
            // Fallback:  non-paginated
            this.lessons = data || []
          }

          console.log(' Fetched listening lessons:', this.lessons.length)
        }
      } catch (error) {
        console.error(' Error fetching lessons:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tải danh sách bài nghe')
        this.lessons = []
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    /**
     *  Fetch ALL lessons (không pagination - dùng cho dropdown)
     */
    async fetchAllLessons() {
      this.lessonsLoading = true
      try {
        // Request với size lớn để lấy tất cả
        const response = await listeningAdminAPI.getAllLessons({
          page: 0,
          size: 1000,
          sort: 'orderIndex,asc',
        })

        if (response.data.success) {
          const data = response.data.data
          this.lessons = data.content || data || []
          console.log(' Fetched all listening lessons:', this.lessons.length)
        }
      } catch (error) {
        console.error(' Error fetching all lessons:', error)
        ElMessage.error('Không thể tải danh sách bài nghe')
        this.lessons = []
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    /**
     *  Fetch lesson by ID
     */
    async fetchLessonById(lessonId) {
      this.lessonsLoading = true
      try {
        const response = await listeningAdminAPI.getLessonById(lessonId)
        if (response.data.success) {
          this.currentLesson = response.data.data
          console.log(' Fetched lesson:', this.currentLesson.title)
          return this.currentLesson
        }
      } catch (error) {
        console.error(' Error fetching lesson:', error)
        ElMessage.error('Không thể tải chi tiết bài nghe')
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    /**
     *  Create lesson
     */
    async createLesson(formData) {
      try {
        const response = await listeningAdminAPI.createLesson(formData)
        if (response.data.success) {
          ElMessage.success(' Tạo bài nghe thành công!')
          return response.data.data
        }
      } catch (error) {
        console.error(' Error creating lesson:', error)
        const message = error.response?.data?.message || 'Không thể tạo bài nghe'
        ElMessage.error(message)
        throw error
      }
    },

    /**
     *  Update lesson
     */
    async updateLesson(id, formData) {
      try {
        const response = await listeningAdminAPI.updateLesson(id, formData)
        if (response.data.success) {
          ElMessage.success(' Cập nhật bài nghe thành công!')
          return response.data. data
        }
      } catch (error) {
        console.error(' Error updating lesson:', error)
        const message = error.response?.data?.message || 'Không thể cập nhật bài nghe'
        ElMessage.error(message)
        throw error
      }
    },

    /**
     *  Delete lesson
     */
    async deleteLesson(id) {
      try {
        const response = await listeningAdminAPI.deleteLesson(id)
        if (response.data.success) {
          ElMessage.success(' Xóa bài nghe thành công!')
          // Remove from local state
          this.lessons = this.lessons.filter((l) => l.id !== id)
        }
      } catch (error) {
        console.error(' Error deleting lesson:', error)
        const message = error.response?.data?.message || 'Không thể xóa bài nghe'
        ElMessage.error(message)
        throw error
      }
    },

    /**
     *  Toggle lesson status
     */
    async toggleLessonStatus(lessonId) {
      try {
        const response = await listeningAdminAPI.toggleLessonStatus(lessonId)
        if (response.data.success) {
          // Update local state
          const lesson = this.lessons.find((l) => l.id === lessonId)
          if (lesson) {
            lesson.isActive = !lesson.isActive
          }
          ElMessage.success(' Cập nhật trạng thái thành công!')
        }
      } catch (error) {
        console.error(' Error toggling lesson status:', error)
        const message = error.response?. data?.message || 'Không thể cập nhật trạng thái'
        ElMessage.error(message)
        throw error
      }
    },

    // ═════════════════════════════════════════════════════════════════
    // ORDER OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    /**
     *  Get next lesson order index
     */
    async getNextLessonOrderIndex() {
      try {
        const response = await listeningAdminAPI.getNextLessonOrderIndex()
        if (response.data.success) {
          return response.data.data.nextOrderIndex
        }
      } catch (error) {
        console.error(' Error getting next order index:', error)
        return 1
      }
    },

    /**
     *  Reorder lesson
     */
    async reorderLesson(lessonId, newOrderIndex) {
      try {
        const response = await listeningAdminAPI.reorderLesson(lessonId, newOrderIndex)
        if (response.data.success) {
          ElMessage.success('Thay đổi thứ tự thành công!')
          await this.fetchLessons() // Reload
        }
      } catch (error) {
        console.error(' Error reordering lesson:', error)
        const message = error.response?.data?.message || 'Không thể thay đổi thứ tự'
        ElMessage.error(message)
        throw error
      }
    },

    /**
     *  Swap lessons
     */
    async swapLessons(lessonId1, lessonId2) {
      try {
        const response = await listeningAdminAPI.swapLessons(lessonId1, lessonId2)
        if (response.data.success) {
          ElMessage.success(' Hoán đổi thành công!')
          await this.fetchLessons() // Reload
        }
      } catch (error) {
        console.error(' Error swapping lessons:', error)
        const message = error.response?.data?.message || 'Không thể hoán đổi'
        ElMessage.error(message)
        throw error
      }
    },

    // ═════════════════════════════════════════════════════════════════
    // QUESTIONS CRUD
    // ═════════════════════════════════════════════════════════════════

    /**
     *  Fetch questions by lesson
     */
    async fetchQuestions(lessonId, params = {}) {
      this.questionsLoading = true
      try {
        const { page = 0, size = 1000, sort = 'orderIndex,asc' } = params

        const response = await listeningAdminAPI.getQuestionsByLesson(lessonId, {
          page,
          size,
          sort,
        })

        if (response.data.success) {
          const data = response.data.data

          if (data.content) {
            this.questions = data.content
            this.questionsPagination = {
              page: data.number,
              size: data.size,
              totalElements: data.totalElements,
              totalPages: data.totalPages,
            }
          } else {
            this.questions = data || []
          }

          console.log(' Fetched questions:', this.questions.length)
        }
      } catch (error) {
        console.error(' Error fetching questions:', error)
        ElMessage.error('Không thể tải danh sách câu hỏi')
        this.questions = []
        throw error
      } finally {
        this.questionsLoading = false
      }
    },

    /**
     *  Fetch question by ID
     */
    async fetchQuestionById(questionId) {
      this.questionsLoading = true
      try {
        const response = await listeningAdminAPI.getQuestionById(questionId)
        if (response.data.success) {
          this.currentQuestion = response.data.data
          return this.currentQuestion
        }
      } catch (error) {
        console.error(' Error fetching question:', error)
        ElMessage.error('Không thể tải chi tiết câu hỏi')
        throw error
      } finally {
        this.questionsLoading = false
      }
    },

    /**
     *  Create question
     */
    async createQuestion(questionData) {
      try {
        const response = await listeningAdminAPI.createQuestion(questionData)
        if (response.data.success) {
          ElMessage.success(' Tạo câu hỏi thành công!')
          return response.data.data
        }
      } catch (error) {
        console.error(' Error creating question:', error)
        const message = error.response?.data?.message || 'Không thể tạo câu hỏi'
        ElMessage.error(message)
        throw error
      }
    },

    /**
     *  Update question
     */
    async updateQuestion(id, questionData) {
      try {
        const response = await listeningAdminAPI.updateQuestion(id, questionData)
        if (response.data.success) {
          ElMessage.success(' Cập nhật câu hỏi thành công!')
          return response.data.data
        }
      } catch (error) {
        console.error(' Error updating question:', error)
        const message = error.response?.data?.message || 'Không thể cập nhật câu hỏi'
        ElMessage.error(message)
        throw error
      }
    },

    /**
     *  Delete question
     */
    async deleteQuestion(id) {
      try {
        const response = await listeningAdminAPI.deleteQuestion(id)
        if (response.data.success) {
          ElMessage.success(' Xóa câu hỏi thành công!')
          this.questions = this.questions.filter((q) => q.id !== id)
        }
      } catch (error) {
        console.error(' Error deleting question:', error)
        const message = error.response?.data?.message || 'Không thể xóa câu hỏi'
        ElMessage.error(message)
        throw error
      }
    },

    /**
     *  Bulk create questions
     */
    async bulkCreateQuestions(lessonId, questionList) {
      try {
        const response = await listeningAdminAPI.createQuestionsInBulk(lessonId, questionList)
        if (response.data.success) {
          ElMessage.success(` Đã tạo ${questionList. length} câu hỏi! `)
          return response.data.data
        }
      } catch (error) {
        console.error(' Error bulk creating questions:', error)
        const message = error.response?.data?.message || 'Không thể tạo câu hỏi hàng loạt'
        ElMessage.error(message)
        throw error
      }
    },

    /**
     *  Bulk delete questions
     */
    async bulkDeleteQuestions(questionIds) {
      try {
        const response = await listeningAdminAPI.bulkDeleteQuestions(questionIds)
        if (response.data.success) {
          ElMessage.success(` Đã xóa ${questionIds.length} câu hỏi! `)
          this.questions = this.questions.filter((q) => !questionIds.includes(q.id))
        }
      } catch (error) {
        console.error(' Error bulk deleting questions:', error)
        const message = error.response?.data?.message || 'Không thể xóa câu hỏi hàng loạt'
        ElMessage.error(message)
        throw error
      }
    },

    /**
     *  Get next question order index
     */
    async getNextQuestionOrderIndex(lessonId) {
      try {
        const response = await listeningAdminAPI.getNextQuestionOrderIndex(lessonId)
        if (response.data.success) {
          return response.data.data.nextOrderIndex
        }
      } catch (error) {
        console.error(' Error getting next question order index:', error)
        return 1
      }
    },

    /**
     *  Copy questions
     */
    async copyQuestions(sourceLessonId, targetLessonId) {
      try {
        const response = await listeningAdminAPI.copyQuestions(sourceLessonId, targetLessonId)
        if (response.data.success) {
          ElMessage.success(' Copy câu hỏi thành công!')
        }
      } catch (error) {
        console.error(' Error copying questions:', error)
        const message = error.response?.data?.message || 'Không thể copy câu hỏi'
        ElMessage.error(message)
        throw error
      }
    },

    // ═════════════════════════════════════════════════════════════════
    // VALIDATION OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    /**
     *  Validate all lessons order
     */
    async validateAllLessonsOrder() {
      try {
        const response = await listeningAdminAPI.validateAllLessonsOrder()
        if (response.data.success) {
          return response.data.data
        }
      } catch (error) {
        console.error(' Error validating lessons order:', error)
        throw error
      }
    },

    /**
     *  Validate questions order
     */
    async validateQuestionsOrder(lessonId) {
      try {
        const response = await listeningAdminAPI.validateQuestionsOrder(lessonId)
        if (response.data.success) {
          return response.data.data
        }
      } catch (error) {
        console.error(' Error validating questions order:', error)
        throw error
      }
    },

    /**
     *  Validate all questions order
     */
    async validateAllQuestionsOrder() {
      try {
        const response = await listeningAdminAPI.validateAllQuestionsOrder()
        if (response.data.success) {
          return response.data.data
        }
      } catch (error) {
        console.error(' Error validating all questions order:', error)
        throw error
      }
    },

    /**
     *  Validate audio files
     */
    async validateAudioFiles() {
      try {
        const response = await listeningAdminAPI.validateAudioFiles()
        if (response.data.success) {
          return response.data.data
        }
      } catch (error) {
        console.error(' Error validating audio files:', error)
        throw error
      }
    },

    /**
     *  Health check
     */
    async healthCheck() {
      try {
        const response = await listeningAdminAPI.healthCheck()
        if (response.data.success) {
          return response.data.data
        }
      } catch (error) {
        console.error(' Error health check:', error)
        throw error
      }
    },
  },
})
