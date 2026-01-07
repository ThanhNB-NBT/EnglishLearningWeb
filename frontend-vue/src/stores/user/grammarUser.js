// src/stores/user/grammarUser.js
import { defineStore } from 'pinia'
import { grammarUserAPI } from '@/api'
import { ElMessage } from 'element-plus'

export const useGrammarUserStore = defineStore('grammarUser', {
  state: () => ({
    topics: [],
    topicsLoading: false,

    // ✅ NEW: Current topic lessons
    currentTopicLessons: [],
    lessonsLoading: false,

    currentLesson: null,
    progressSummary: null,
    progressLoading: false,
    lastSubmitResult: null,
  }),

  getters: {
    allLessons: (state) => {
      return state.topics.flatMap((topic) => topic.lessons || [])
    },

    getTopicById: (state) => (id) => {
      return state.topics.find((t) => t.id === id)
    },

    getLessonsByTopicId: (state) => (topicId) => {
      const topic = state.topics.find((t) => t.id === topicId)
      return topic?.lessons || []
    },

    // ✅ NEW: Get current topic lessons
    getCurrentTopicLessons: (state) => {
      return state.currentTopicLessons
    },

    nextLessonToLearn: (state) => {
      return state.topics
        .flatMap((t) => t.lessons || [])
        .find((l) => l.isUnlocked && !l.isCompleted)
    },

    overallCompletionRate: (state) => {
      const totalLessons = state.topics.reduce((sum, t) => sum + (t.totalLessons || 0), 0)
      const completed = state.topics.reduce((sum, t) => sum + (t.completedLessons || 0), 0)
      return totalLessons > 0 ? Math.round((completed / totalLessons) * 100) : 0
    },

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
        const response = await grammarUserAPI.getTopics()

        if (response.data.success) {
          this.topics = response.data.data || []
          console.log('✅ Fetched grammar topics:', this.topics.length)
        }
      } catch (error) {
        console.error('❌ Error fetching topics:', error)
        ElMessage.error('Không thể tải danh sách chủ đề')
        throw error
      } finally {
        this.topicsLoading = false
      }
    },

    // ✅ NEW: Fetch lessons by topic (for sidebar in PlayerView)
    async fetchLessonsByTopic(topicId) {
      this.lessonsLoading = true
      try {
        const response = await grammarUserAPI.getLessonsByTopic(topicId)

        if (response.data.success) {
          this.currentTopicLessons = response.data.data || []
          console.log(`✅ Fetched ${this.currentTopicLessons.length} lessons for topic ${topicId}`)
          return this.currentTopicLessons
        }
      } catch (error) {
        console.error('❌ Error fetching lessons:', error)
        ElMessage.error('Không thể tải danh sách bài học')
        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    async fetchLessonDetail(lessonId) {
      this.lessonsLoading = true
      try {
        const response = await grammarUserAPI.getLessonDetail(lessonId)

        if (response.data.success) {
          this.currentLesson = response.data.data

          console.log('✅ Fetched lesson:', this.currentLesson.title)
          console.log('  - Has groupedQuestions:', !!this.currentLesson.groupedQuestions)
          console.log('  - Tasks:', this.currentLesson.groupedQuestions?.tasks?.length || 0)
          console.log(
            '  - Standalone:',
            this.currentLesson.groupedQuestions?.standaloneQuestions?.length || 0,
          )

          // ❌ REMOVED: Auto fetch - Let component decide when to fetch
          // This was causing duplicate fetches and infinite loops
          // if (this.currentLesson.topicId) {
          //   await this.fetchLessonsByTopic(this.currentLesson.topicId)
          // }

          return this.currentLesson
        }
      } catch (error) {
        console.error('❌ Error fetching lesson:', error)

        const message = error.response?.data?.message || 'Không thể tải chi tiết bài học'

        if (message.includes('locked') || message.includes('khóa')) {
          ElMessage.warning('Bài học này đang bị khóa. Hoàn thành bài trước để mở khóa!')
        } else if (
          message.includes('trình độ') ||
          message.includes('level') ||
          message.includes('yêu cầu')
        ) {
          ElMessage.warning({
            message: message,
            duration: 4000,
            showClose: true,
          })
        } else {
          ElMessage.error(message)
        }

        throw error
      } finally {
        this.lessonsLoading = false
      }
    },

    async submitLesson(submitData) {
      try {
        const response = await grammarUserAPI.submitLesson(submitData)

        if (response.data.success) {
          this.lastSubmitResult = response.data.data

          // Update topics
          this.topics = this.topics.map((topic) => {
            const lessonIndex = (topic.lessons || []).findIndex((l) => l.id === submitData.lessonId)

            if (lessonIndex !== -1) {
              const updatedLessons = [...topic.lessons]
              updatedLessons[lessonIndex] = {
                ...updatedLessons[lessonIndex],
                isCompleted: this.lastSubmitResult.isPassed,
                scorePercentage: this.lastSubmitResult.scorePercentage,
              }

              if (
                this.lastSubmitResult.hasUnlockedNext &&
                lessonIndex < updatedLessons.length - 1
              ) {
                updatedLessons[lessonIndex + 1].isUnlocked = true
              }

              const completedCount = updatedLessons.filter((l) => l.isCompleted).length

              return {
                ...topic,
                lessons: updatedLessons,
                completedLessons: completedCount,
              }
            }

            return topic
          })

          // ✅ Update currentTopicLessons
          this.currentTopicLessons = this.currentTopicLessons.map((lesson) => {
            if (lesson.id === submitData.lessonId) {
              return {
                ...lesson,
                isCompleted: this.lastSubmitResult.isPassed,
                scorePercentage: this.lastSubmitResult.scorePercentage,
              }
            }
            if (lesson.id === this.lastSubmitResult.nextLessonId) {
              return { ...lesson, isUnlocked: true }
            }
            return lesson
          })

          if (this.lastSubmitResult.isPassed) {
            ElMessage.success(
              `Chúc mừng! Bạn đạt ${this.lastSubmitResult.scorePercentage?.toFixed(0)}%`,
            )
          } else {
            ElMessage.warning(
              `Chưa đạt! Cần ≥80% (Hiện tại: ${this.lastSubmitResult.scorePercentage?.toFixed(0)}%)`,
            )
          }

          return this.lastSubmitResult
        }
      } catch (error) {
        console.error('❌ Error submitting lesson:', error)
        ElMessage.error(error.response?.data?.message || 'Không thể nộp bài')
        throw error
      }
    },

    async fetchProgressSummary() {
      this.progressLoading = true
      try {
        const response = await grammarUserAPI.getProgressSummary()

        if (response.data.success) {
          this.progressSummary = response.data.data
          console.log('✅ Fetched progress summary:', this.progressSummary)
        }
      } catch (error) {
        console.error('❌ Error fetching progress summary:', error)
        ElMessage.error('Không thể tải tổng quan tiến độ')
      } finally {
        this.progressLoading = false
      }
    },

    clearCurrentLesson() {
      this.currentLesson = null
      this.lastSubmitResult = null
      this.currentTopicLessons = []
    },

    reset() {
      this.topics = []
      this.currentLesson = null
      this.currentTopicLessons = []
      this.progressSummary = null
      this.lastSubmitResult = null
    },
  },
})
