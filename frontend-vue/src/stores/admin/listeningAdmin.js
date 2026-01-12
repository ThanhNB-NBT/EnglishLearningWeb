import { defineStore } from 'pinia'
import { listeningAdminAPI } from '@/api'
import { ElMessage } from 'element-plus'

export const useListeningAdminStore = defineStore('listeningAdmin', {
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

    // Helper tìm question
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
        const response = await listeningAdminAPI.getLessonsByTopic(topicId, params)
        if (response.data.success) {
          const { content, totalElements, totalPages, page, size } = response.data.data

          this.lessons = content
          this.lessonsPagination = { page, size, totalElements, totalPages }
        }
      } catch (error) {
        ElMessage.error('Lỗi tải bài nghe')
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    async fetchLessonDetail(lessonId) {
      this.lessonsLoading = true
      try {
        const response = await listeningAdminAPI.getLessonById(lessonId)
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

    async createLesson(formData) {
      try {
        const res = await listeningAdminAPI.createLesson(formData)
        if (res.data.success) {
          ElMessage.success('Tạo bài học thành công')
          // Optional: update local state
          // this.lessons.unshift(res.data.data)
          return res.data.data
        }
      } catch (error) {
        ElMessage.error('Tạo bài học thất bại')
        throw error
      }
    },

    async updateLesson(id, formData) {
      try {
        const res = await listeningAdminAPI.updateLesson(id, formData)
        if (res.data.success) {
          ElMessage.success('Cập nhật thành công')

          // Update local state
          const index = this.lessons.findIndex((l) => l.id === id)
          if (index !== -1) {
            this.lessons[index] = res.data.data
          }

          return res.data.data
        }
      } catch (error) {
        ElMessage.error('Cập nhật thất bại')
        throw error
      }
    },

    async deleteLesson(id) {
      try {
        await listeningAdminAPI.deleteLesson(id)
        this.lessons = this.lessons.filter((l) => l.id !== id)
        ElMessage.success('Đã xóa bài học')
      } catch (error) {
        ElMessage.error('Xóa thất bại')
        throw error
      }
    },

    async toggleLessonStatus(id) {
      try {
        await listeningAdminAPI.toggleLessonStatus(id)

        const lesson = this.lessons.find((l) => l.id === id)
        if (lesson) lesson.isActive = !lesson.isActive
        ElMessage.success('Đã đổi trạng thái')
      } catch (error) {
        ElMessage.error('Lỗi đổi trạng thái')
        throw error
      }
    },

    async getNextOrderIndex(topicId) {
      try {
        const res = await listeningAdminAPI.getNextLessonOrderIndex(topicId)
        return res.data.data.nextOrderIndex
      } catch {
        return 1
      }
    },

    // --- QUESTIONS ---
    async fetchQuestions(lessonId) {
      this.questionsLoading = true
      try {
        const response = await listeningAdminAPI.getQuestionsByLesson(lessonId)
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
        const response = await listeningAdminAPI.getQuestionById(id)
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
        const response = await listeningAdminAPI.createQuestion(lessonId, data)
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
        const response = await listeningAdminAPI.updateQuestion(id, data)
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
        await listeningAdminAPI.deleteQuestion(id)
        ElMessage.success('Đã xóa câu hỏi')
        if (this.currentLesson?.id) await this.fetchQuestions(this.currentLesson.id)
      } catch (error) {
        ElMessage.error('Xóa thất bại')
        throw error
      }
    },

    async bulkDeleteQuestions(ids) {
      try {
        await listeningAdminAPI.bulkDeleteQuestions(ids)
        ElMessage.success(`Đã xóa ${ids.length} câu hỏi`)
        if (this.currentLesson?.id) await this.fetchQuestions(this.currentLesson.id)
      } catch (e) {
        ElMessage.error('Lỗi xóa hàng loạt')
        throw e
      }
    },

    async createQuestionsBulk(lessonId, dtos) {
      try {
        const res = await listeningAdminAPI.createQuestionsBulk(lessonId, dtos)
        ElMessage.success(`Đã tạo ${dtos.length} câu hỏi`)
        await this.fetchQuestions(lessonId)
        return res.data.data
      } catch (e) {
        ElMessage.error('Lỗi tạo hàng loạt')
        throw e
      }
    },

    async getNextQuestionOrderIndex(lessonId) {
      try {
        const res = await listeningAdminAPI.getNextQuestionOrderIndex(lessonId)
        return res.data.data.nextOrderIndex
      } catch {
        return 1
      }
    },

    async fixQuestionOrder(lessonId) {
      try {
        await listeningAdminAPI.fixQuestionOrder(lessonId)
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
