// src/stores/user/readingUser.js
import { defineStore } from 'pinia'
import { readingUserAPI } from '@/api'
import { ElMessage } from 'element-plus'

export const useReadingUserStore = defineStore('readingUser', {
  state: () => ({
    topics: [],
    topicsLoading: false,
    currentTopicLessons: [],
    currentLesson: null,
    lessonsLoading: false,
    progressSummary: null,
    progressLoading: false,
    lastSubmitResult: null,
  }),

  getters: {
    allLessons: (state) => state.topics.flatMap((t) => t.lessons || []),

    getTopicById: (state) => (id) => state.topics.find((t) => t.id === id),

    nextLessonToLearn: (state) => {
      return state.topics
        .flatMap((t) => t.lessons || [])
        .find((l) => l.isUnlocked && !l.isCompleted)
    },

    getCurrentTopicLessons: (state) => {
      return state.currentTopicLessons
    },

    // ✅ NEW: Get flat questions array from groupedQuestions
    flatQuestions: (state) => {
      if (!state.currentLesson?.groupedQuestions) return []

      const grouped = state.currentLesson.groupedQuestions
      const questions = []

      if (grouped.standaloneQuestions?.length > 0) {
        questions.push(...grouped.standaloneQuestions)
      }

      if (grouped.tasks?.length > 0) {
        grouped.tasks.forEach((task) => {
          if (task.questions?.length > 0) {
            questions.push(...task.questions)
          }
        })
      }

      return questions
    },
  },

  actions: {
    async fetchTopics() {
      this.topicsLoading = true
      try {
        const response = await readingUserAPI.getTopics()
        if (response.data.success) {
          this.topics = response.data.data || []
          console.log('✅ Fetched reading topics:', this.topics.length)
        }
      } catch (error) {
        console.error('❌ Error:', error)
        ElMessage.error('Không thể tải danh sách chủ đề')
        throw error
      } finally {
        this.topicsLoading = false
      }
    },

    async fetchLessonsByTopic(topicId) {
      this.lessonsLoading = true
      try {
        const response = await readingUserAPI.getLessonsByTopic(topicId)
        if (response.data.success) {
          this.currentTopicLessons = response.data.data || []
          return this.currentTopicLessons
        }
      } catch (error) {
        console.error('❌ Error:', error)
        ElMessage.error('Không thể tải danh sách bài học')
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    async fetchLessonDetail(lessonId) {
      this.lessonsLoading = true
      try {
        const response = await readingUserAPI.getLessonDetail(lessonId)
        if (response.data.success) {
          this.currentLesson = response.data.data

          console.log('✅ Fetched lesson:', this.currentLesson.title)
          console.log('  - Has groupedQuestions:', !!this.currentLesson.groupedQuestions)

          if (this.currentLesson.topicId) {
            await this.fetchLessonsByTopic(this.currentLesson.topicId)
          }
          return this.currentLesson
        }
      } catch (error) {
        console.error('❌ Error:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể tải bài đọc')
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    async submitLesson(lessonId, submitData) {
      try {
        const response = await readingUserAPI.submitLesson(lessonId, submitData)
        if (response.data.success) {
          this.lastSubmitResult = response.data.data

          this.topics = this.topics.map((topic) => {
            const lessonIndex = (topic.lessons || []).findIndex((l) => l.id === lessonId)
            if (lessonIndex !== -1) {
              const updatedLessons = [...topic.lessons]
              updatedLessons[lessonIndex].isCompleted = this.lastSubmitResult.isPassed
              updatedLessons[lessonIndex].scorePercentage = this.lastSubmitResult.scorePercentage

              if (
                this.lastSubmitResult.hasUnlockedNext &&
                lessonIndex < updatedLessons.length - 1
              ) {
                updatedLessons[lessonIndex + 1].isUnlocked = true
              }

              return {
                ...topic,
                lessons: updatedLessons,
                completedLessons: updatedLessons.filter((l) => l.isCompleted).length,
              }
            }
            return topic
          })

          ElMessage.success(
            this.lastSubmitResult.isPassed
              ? `Đạt ${this.lastSubmitResult.scorePercentage}%`
              : 'Chưa đạt',
          )

          return this.lastSubmitResult
        }
      } catch (error) {
        console.error('❌ Error:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể nộp bài')
        throw error
      }
    },

    async fetchProgressSummary() {
      this.progressLoading = true
      try {
        const response = await readingUserAPI.getProgressSummary()
        if (response.data.success) {
          this.progressSummary = response.data.data
        }
      } catch (error) {
        console.error('❌ Error:', error)
      } finally {
        this.progressLoading = false
      }
    },

    clearCurrentLesson() {
      this.currentLesson = null
      this.lastSubmitResult = null
    },

    reset() {
      this.topics = []
      this.currentLesson = null
      this.progressSummary = null
      this.lastSubmitResult = null
    },
  },
})
