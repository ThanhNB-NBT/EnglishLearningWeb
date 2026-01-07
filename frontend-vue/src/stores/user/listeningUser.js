// src/stores/user/listeningUser.js
import { defineStore } from 'pinia'
import { listeningUserAPI } from '@/api'
import { ElMessage } from 'element-plus'

export const useListeningUserStore = defineStore('listeningUser', {
  state: () => ({
    topics: [],
    topicsLoading: false,
    currentLesson: null,
    lessonsLoading: false,
    progressSummary: null,
    progressLoading: false,
    lastSubmitResult: null,
    currentTopicLessons: [],
  }),

  getters: {
    allLessons: (state) => state.topics.flatMap((t) => t.lessons || []),

    getTopicById: (state) => (id) => state.topics.find((t) => t.id === id),

    getLessonsByTopicId: (state) => (topicId) => {
      const topic = state.topics.find((t) => t.id === topicId)
      return topic?.lessons || []
    },

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
        const response = await listeningUserAPI.getTopics()
        if (response.data.success) {
          this.topics = response.data.data || []
          console.log('✅ Fetched listening topics:', this.topics.length)
        }
      } catch (error) {
        console.error('❌ Error fetching topics:', error)
        ElMessage.error('Không thể tải danh sách chủ đề')
        throw error
      } finally {
        this.topicsLoading = false
      }
    },

    async fetchLessonsByTopic(topicId) {
      this.lessonsLoading = true
      try {
        const response = await listeningUserAPI.getLessonsByTopic(topicId)

        if (response.data.success) {
          this.lessons = response.data.data || []
          console.log('✅ Fetched lessons:', this.lessons.length)
        }
      } catch (error) {
        console.error('❌ Error fetching lessons:', error)
        ElMessage.error('Không thể tải danh sách bài nghe')
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    async fetchLessonDetail(lessonId) {
      this.lessonsLoading = true
      try {
        const response = await listeningUserAPI.getLessonDetail(lessonId)
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
        const message = error.response?.data?.message || 'Không thể tải bài nghe'
        ElMessage.error(message)
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    async trackPlay(lessonId) {
      try {
        await listeningUserAPI.trackPlay(lessonId)
        if (this.currentLesson?.id === lessonId) {
          this.currentLesson.playCount = (this.currentLesson.playCount || 0) + 1
        }
      } catch (error) {
        console.error('❌ Error tracking play:', error)
      }
    },

    async viewTranscript(lessonId) {
      try {
        await listeningUserAPI.viewTranscript(lessonId)
        if (this.currentLesson?.id === lessonId) {
          this.currentLesson.transcriptUnlocked = true
        }
        ElMessage.success('Đã mở transcript!')
      } catch (error) {
        console.error('❌ Error:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể mở transcript')
      }
    },

    async submitLesson(lessonId, submitData) {
      try {
        const response = await listeningUserAPI.submitLesson(lessonId, submitData)
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
              : `Chưa đạt (${this.lastSubmitResult.scorePercentage}%)`,
          )

          return this.lastSubmitResult
        }
      } catch (error) {
        console.error('❌ Error submitting:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể nộp bài')
        throw error
      }
    },

    async fetchCompletedLessons() {
      this.progressLoading = true
      try {
        const response = await listeningUserAPI.getCompletedLessons()

        if (response.data.success) {
          this.completedLessons = response.data.data || []
          console.log('✅ Fetched completed lessons:', this.completedLessons.length)
        }
      } catch (error) {
        console.error('❌ Error fetching completed lessons:', error)
        ElMessage.error('Không thể tải danh sách bài đã hoàn thành')
        throw error
      } finally {
        this.progressLoading = false
      }
    },

    async fetchProgressSummary() {
      this.progressLoading = true
      try {
        const response = await listeningUserAPI.getProgressSummary()
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

    clearLessons() {
      this.lessons = []
      this.currentLesson = null
      this.lastSubmitResult = null
    },

    reset() {
      this.topics = []
      this.currentTopic = null
      this.lessons = []
      this.currentLesson = null
      this.completedLessons = []
      this.progressSummary = null
      this.lastSubmitResult = null
    },
  },
})
