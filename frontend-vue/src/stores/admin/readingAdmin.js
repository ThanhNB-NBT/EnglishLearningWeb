import { defineStore } from 'pinia'
import { readingAdminAPI } from '@/api'
import { ElMessage } from 'element-plus'

export const useReadingAdminStore = defineStore('readingAdmin', {
  state: () => ({
    // Lessons
    lessons: [],
    currentLesson: null,
    lessonsLoading: false,
    lessonsPagination: { page: 1, size: 20, totalElements: 0, totalPages: 0 },

    // Questions (Grouped)
    groupedQuestions: null,
    currentQuestion: null,
    questionsLoading: false,

    taskStats: null,
  }),

  getters: {
    activeLessons: (state) => state.lessons.filter((l) => l.isActive),
    inactiveLessons: (state) => state.lessons.filter((l) => !l.isActive),
    getLessonById: (state) => (id) => state.lessons.find((l) => l.id === id),

    taskGroups: (state) => state.groupedQuestions?.tasks || [],
    standaloneQuestions: (state) => state.groupedQuestions?.standaloneQuestions || [],

    findQuestionInState: (state) => (id) => {
      let found = state.groupedQuestions?.standaloneQuestions?.find((q) => q.id === id)
      if (found) return found
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
    // --- LESSONS ---
    async fetchLessons(topicId, params = {}) {
      this.lessonsLoading = true
      try {
        const response = await readingAdminAPI.getLessonsByTopic(topicId, params)
        if (response.data.success) {
          // ✅ FIX: Đổi từ "data" sang "content"
          const { content, totalElements, totalPages, page, size } = response.data.data

          this.lessons = content
          this.lessonsPagination = { page, size, totalElements, totalPages }
        }
      } catch (error) {
        ElMessage.error('Lỗi tải bài đọc')
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    async fetchLessonDetail(lessonId) {
      this.lessonsLoading = true
      try {
        const response = await readingAdminAPI.getLessonDetail(lessonId)
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
        const response = await readingAdminAPI.createLesson(lessonData)
        if (response.data.success) {
          ElMessage.success('Tạo bài đọc thành công')
          return response.data.data
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || 'Tạo thất bại')
        throw error
      }
    },

    async updateLesson(id, lessonData) {
      try {
        const response = await readingAdminAPI.updateLesson(id, lessonData)
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
        await readingAdminAPI.deleteLesson(id)
        this.lessons = this.lessons.filter((l) => l.id !== id)
        ElMessage.success('Đã xóa bài học')
      } catch (error) {
        ElMessage.error('Xóa thất bại')
        throw error
      }
    },

    async toggleLessonStatus(id) {
      try {
        await readingAdminAPI.toggleLessonStatus(id)
        const lesson = this.lessons.find((l) => l.id === id)
        if (lesson) lesson.isActive = !lesson.isActive
        ElMessage.success('Đã đổi trạng thái')
      } catch (error) {
        ElMessage.error('Lỗi đổi trạng thái')
        throw error
      }
    },

    async getNextLessonOrderIndex(topicId) {
      try {
        const res = await readingAdminAPI.getNextLessonOrderIndex(topicId)
        return res.data.data.nextOrderIndex
      } catch {
        return 1
      }
    },

    // --- QUESTIONS ---
    async fetchQuestions(lessonId) {
      this.questionsLoading = true
      try {
        const response = await readingAdminAPI.getQuestionsByLesson(lessonId)
        if (response.data.success) {
          this.groupedQuestions = response.data.data
        }
      } catch (error) {
        ElMessage.error('Lỗi tải câu hỏi')
        throw error
      } finally {
        this.questionsLoading = false
      }
    },

    async fetchQuestionById(id) {
      try {
        const cached = this.findQuestionInState(id)
        if (cached) {
          this.currentQuestion = cached
          return cached
        }
        const response = await readingAdminAPI.getQuestionById(id)
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
        const response = await readingAdminAPI.createQuestion(lessonId, data)
        if (response.data.success) {
          ElMessage.success('Tạo câu hỏi thành công')
          await this.fetchQuestions(lessonId)
          return response.data.data
        }
      } catch (error) {
        ElMessage.error('Tạo thất bại')
        throw error
      }
    },

    async updateQuestion(id, data) {
      try {
        const response = await readingAdminAPI.updateQuestion(id, data)
        if (response.data.success) {
          ElMessage.success('Cập nhật thành công')
          if (this.currentLesson?.id) await this.fetchQuestions(this.currentLesson.id)
          return response.data.data
        }
      } catch (error) {
        ElMessage.error('Cập nhật thất bại')
        throw error
      }
    },

    async deleteQuestion(id) {
      try {
        await readingAdminAPI.deleteQuestion(id)
        ElMessage.success('Đã xóa câu hỏi')
        if (this.currentLesson?.id) await this.fetchQuestions(this.currentLesson.id)
      } catch (error) {
        ElMessage.error('Xóa thất bại')
        throw error
      }
    },

    async bulkDeleteQuestions(ids) {
      try {
        await readingAdminAPI.bulkDeleteQuestions(ids)
        ElMessage.success(`Đã xóa ${ids.length} câu hỏi`)
        if (this.currentLesson?.id) await this.fetchQuestions(this.currentLesson.id)
      } catch (e) {
        ElMessage.error('Lỗi xóa hàng loạt')
        throw e
      }
    },

    async createQuestionsBulk(lessonId, dtos) {
      try {
        const res = await readingAdminAPI.createQuestionsBulk(lessonId, dtos)
        ElMessage.success(`Đã tạo ${dtos.length} câu hỏi`)
        await this.fetchQuestions(lessonId)
        return res.data.data
      } catch (e) {
        ElMessage.error('Lỗi tạo hàng loạt')
        throw e
      }
    },

    async getNextOrderIndex(lessonId) {
      try {
        const res = await readingAdminAPI.getNextQuestionOrderIndex(lessonId)
        return res.data.data.nextOrderIndex
      } catch {
        return 1
      }
    },

    async fixQuestionOrder(lessonId) {
      try {
        await readingAdminAPI.fixQuestionOrder(lessonId)
        await this.fetchQuestions(lessonId)
        ElMessage.success('Đã sắp xếp lại câu hỏi')
      } catch {
        ElMessage.error('Lỗi sắp xếp')
      }
    },

    reset() {
      this.lessons = []
      this.currentLesson = null
      this.groupedQuestions = null
    },
  },
})
