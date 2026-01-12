import { defineStore } from 'pinia'
import { grammarAdminAPI } from '@/api'
import { ElMessage } from 'element-plus'

export const useGrammarAdminStore = defineStore('grammarAdmin', {
  state: () => ({
    // === LESSONS (Có phân trang) ===
    lessons: [],
    currentLesson: null,
    lessonsLoading: false,
    lessonsPagination: {
      page: 1,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    },

    // === QUESTIONS (Load All - Cấu trúc Grouped) ===
    groupedQuestions: null, // TaskGroupedQuestionsDTO
    currentQuestion: null, // Để xem chi tiết/edit
    questionsLoading: false,

    // Stats
    taskStats: null,
  }),

  getters: {
    // Lesson Getters
    activeLessons: (state) => state.lessons.filter((l) => l.isActive),
    inactiveLessons: (state) => state.lessons.filter((l) => !l.isActive),
    getLessonById: (state) => (id) => state.lessons.find((l) => l.id === id),

    // Question Getters (Helper để lấy danh sách phẳng cho UI cũ)
    questions: (state) => state.groupedQuestions?.standaloneQuestions || [],

    // Helper tìm question trong state (dù nằm ở standalone hay trong task)
    findQuestionInState: (state) => (id) => {
      // 1. Tìm trong standalone
      let found = state.groupedQuestions?.standaloneQuestions?.find((q) => q.id === id)
      if (found) return found

      // 2. Tìm trong tasks
      if (state.groupedQuestions?.tasks) {
        for (const task of state.groupedQuestions.tasks) {
          found = task.questions?.find((q) => q.id === id)
          if (found) return found
        }
      }
      return null
    },
  },

  actions: {

    async parseFile(topicId, formData) {
      try {
        // API: POST /api/admin/grammar/topics/{id}/parse-file
        // formData chứa 'file', 'pages', 'parsingContext'
        const res = await grammarAdminAPI.parseFile(topicId, formData)
        return res.data.data // Trả về Map summary từ backend
      } catch (error) {
        ElMessage.error(error.response?.data?.message || 'Lỗi phân tích file')
        throw error
      }
    },

    async importLessons(topicId, lessonsList) {
      try {
        // API: POST /api/admin/grammar/topics/{id}/import-lessons
        // Body: List<GrammarLessonDTO> -> JSON
        const res = await grammarAdminAPI.importLessons(topicId, lessonsList)
        return res.data.data
      } catch (error) {
        ElMessage.error('Lỗi import dữ liệu')
        throw error
      }
    },

    // ══════════════════════════════════════════════════════════════
    // LESSONS
    // ══════════════════════════════════════════════════════════════
    async fetchLessons(topicId, params = {}) {
      this.lessonsLoading = true
      try {
        const response = await grammarAdminAPI.getLessonsByTopic(topicId, params)
        if (response.data.success) {
          // ✅ FIX: Đổi từ "data" sang "content"
          const { content, totalElements, totalPages, page, size } = response.data.data

          this.lessons = content
          this.lessonsPagination = {
            page,
            size,
            totalElements,
            totalPages,
          }
        }
      } catch (error) {
        console.error('❌ Error fetching lessons:', error)
        ElMessage.error('Không thể tải danh sách bài học')
      } finally {
        this.lessonsLoading = false
      }
    },

    async fetchLessonDetail(lessonId) {
      this.lessonsLoading = true
      try {
        const response = await grammarAdminAPI.getLessonById(lessonId)
        if (response.data.success) {
          this.currentLesson = response.data.data
          return response.data.data
        }
      } catch (error) {
        ElMessage.error('Lỗi tải chi tiết bài học')
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    async createLesson(lessonData) {
      try {
        const response = await grammarAdminAPI.createLesson(lessonData)
        if (response.data.success) {
          ElMessage.success('Tạo bài học thành công')
          return response.data.data
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || 'Tạo thất bại')
        throw error
      }
    },

    async updateLesson(id, lessonData) {
      try {
        const response = await grammarAdminAPI.updateLesson(id, lessonData)
        if (response.data.success) {
          ElMessage.success('Cập nhật thành công')
          return response.data.data
        }
      } catch (error) {
        ElMessage.error('Cập nhật thất bại')
        throw error
      }
    },

    async deleteLesson(id) {
      try {
        await grammarAdminAPI.deleteLesson(id)
        this.lessons = this.lessons.filter((l) => l.id !== id)
        ElMessage.success('Đã xóa bài học')
      } catch (error) {
        ElMessage.error('Xóa thất bại')
        throw error
      }
    },

    async toggleLessonStatus(id) {
      try {
        await grammarAdminAPI.toggleLessonStatus(id)
        const lesson = this.lessons.find(l => l.id === id)
        if (lesson) lesson.isActive = !lesson.isActive
        ElMessage.success('Đã đổi trạng thái')
      } catch (error) {
        ElMessage.error('Lỗi đổi trạng thái')
        console.error('❌ Error toggling lesson status:', error)
      }
    },

    async getNextLessonOrderIndex(topicId) {
      try {
        const res = await grammarAdminAPI.getNextLessonOrderIndex(topicId)
        return res.data
      } catch {
        return 1
      }
    },

    async fixLessonOrder(topicId) {
      try {
        await grammarAdminAPI.fixLessonOrder(topicId)
        await this.fetchLessons(topicId, { page: this.lessonsPagination.page })
        ElMessage.success('Đã sắp xếp lại thứ tự bài học')
      } catch (error) {
        ElMessage.error('Lỗi sắp xếp bài học')
        console.error('❌ Error fixing lesson order:', error)
      }
    },

    // ══════════════════════════════════════════════════════════════
    // QUESTIONS
    // ══════════════════════════════════════════════════════════════

    // Load All (Grouped)
    async fetchQuestions(lessonId) {
      this.questionsLoading = true
      try {
        const response = await grammarAdminAPI.getQuestionsByLesson(lessonId)
        if (response.data.success) {
          this.groupedQuestions = response.data.data
        }
      } catch (error) {
        ElMessage.error('Lỗi tải câu hỏi')
        console.error('❌ Error fetching questions:', error)
      } finally {
        this.questionsLoading = false
      }
    },

    async fetchQuestionById(id) {
      try {
        // Check cache trước
        const cached = this.findQuestionInState(id)
        if (cached) {
          this.currentQuestion = cached
          return cached
        }
        // Gọi API nếu không thấy
        const response = await grammarAdminAPI.getQuestionById(id)
        if (response.data.success) {
          this.currentQuestion = response.data.data
          return response.data.data
        }
      } catch (error) {
        ElMessage.error('Không tìm thấy câu hỏi')
        throw error
      }
    },

    async createQuestion(lessonId, data) {
      try {
        const response = await grammarAdminAPI.createQuestion(lessonId, data)
        if (response.data.success) {
          ElMessage.success('Tạo câu hỏi thành công')
          await this.fetchQuestions(lessonId) // Reload list
          return response.data.data
        }
      } catch (error) {
        ElMessage.error('Tạo câu hỏi thất bại')
        throw error
      }
    },

    async updateQuestion(id, data) {
      try {
        const response = await grammarAdminAPI.updateQuestion(id, data)
        if (response.data.success) {
          ElMessage.success('Cập nhật thành công')
          // Nếu đang ở trong context bài học, reload lại list
          if (this.currentLesson?.id) {
            await this.fetchQuestions(this.currentLesson.id)
          }
          return response.data.data
        }
      } catch (error) {
        ElMessage.error('Cập nhật thất bại')
        throw error
      }
    },

    async deleteQuestion(id) {
      try {
        await grammarAdminAPI.deleteQuestion(id)
        ElMessage.success('Đã xóa câu hỏi')
        if (this.currentLesson?.id) {
          await this.fetchQuestions(this.currentLesson.id)
        }
      } catch (error) {
        ElMessage.error('Xóa thất bại')
        throw error
      }
    },

    async bulkDeleteQuestions(ids) {
      try {
        await grammarAdminAPI.bulkDeleteQuestions(ids)
        ElMessage.success(`Đã xóa ${ids.length} câu hỏi`)
        if (this.currentLesson?.id) await this.fetchQuestions(this.currentLesson.id)
      } catch (error) {
        ElMessage.error('Xóa hàng loạt thất bại')
        throw error
      }
    },

    async createQuestionsBulk(lessonId, questions) {
      try {
        const res = await grammarAdminAPI.createQuestionsBulk(lessonId, questions)
        ElMessage.success(`Đã tạo ${questions.length} câu hỏi`)
        await this.fetchQuestions(lessonId)
        return res.data.data
      } catch (e) {
        ElMessage.error('Lỗi tạo hàng loạt')
        throw e
      }
    },

    async getNextOrderIndex(lessonId) {
      try {
        const res = await grammarAdminAPI.getNextQuestionOrderIndex(lessonId)
        return res.data.data.nextOrderIndex
      } catch {
        return 1
      }
    },

    async fixQuestionOrder(lessonId) {
      try {
        await grammarAdminAPI.fixQuestionOrder(lessonId)
        await this.fetchQuestions(lessonId)
        ElMessage.success('Sắp xếp câu hỏi thành công')
      } catch {
        ElMessage.error('Lỗi sắp xếp')
      }
    },

    reset() {
      this.lessons = []
      this.currentLesson = null
      this.groupedQuestions = null
      this.currentQuestion = null
    },
  },
})
