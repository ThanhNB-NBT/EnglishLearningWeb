// src/stores/user/placementTest.js
import { defineStore } from 'pinia'
import { placementAPI } from '@/api'
import { ElMessage } from 'element-plus'

export const usePlacementTestStore = defineStore('placementTest', {
  state: () => ({
    testData: null,
    loading: false,
    submitting: false,
    lastResult: null,
    canRetakeAfter: null,
    cooldownMessage: null,
  }),

  getters: {
    sections: (state) => state.testData?.sections || [],
    totalQuestions: (state) => state.testData?.totalQuestions || 0,
    timeLimitSeconds: (state) => state.testData?.timeLimitSeconds || 1800,
    isInCooldown: (state) => {
      if (!state.canRetakeAfter) return false
      return new Date(state.canRetakeAfter) > new Date()
    },
    allQuestions: (state) => {
      if (!state.testData?.sections) return []
      const questions = []
      state.testData.sections.forEach((section) => {
        if (section.questions) questions.push(...section.questions)
      })
      return questions
    },
    grammarSection: (state) => state.testData?.sections?.find(s => s.sectionType === 'GRAMMAR') || null,
    readingSections: (state) => state.testData?.sections?.filter(s => s.sectionType === 'READING') || [],
    listeningSections: (state) => state.testData?.sections?.filter(s => s.sectionType === 'LISTENING') || [],
  },

  actions: {
    async fetchPlacementTest() {
      this.loading = true
      this.cooldownMessage = null

      try {
        const response = await placementAPI.getPlacementTest()
        console.log('📥 Raw API Response:', response)

        let data = null

        if (response.data?.success && response.data?.data) {
          data = response.data.data
        } else if (response.data?.sections) {
          data = response.data
        } else if (response.sections) {
          data = response
        }

        if (!data || !data.sections || !Array.isArray(data.sections)) {
          console.error('❌ Invalid data structure:', data)
          throw new Error('Invalid response format: missing sections array')
        }

        this.testData = data
        this.canRetakeAfter = null

        console.log('✅ Placement test loaded:', {
          sections: this.testData.sections.length,
          totalQuestions: this.testData.totalQuestions,
          timeLimitSeconds: this.testData.timeLimitSeconds
        })

        return true
      } catch (error) {
        console.error('❌ Error fetching placement test:', error)

        // ✅ FIX: Properly extract error message from response
        let errorMessage = 'Không thể tải bài thi. Vui lòng thử lại sau.'

        if (error.response?.data) {
          // Case 1: Direct message in response.data
          if (typeof error.response.data === 'string') {
            errorMessage = error.response.data
          }
          // Case 2: Message in response.data.message
          else if (error.response.data.message) {
            errorMessage = error.response.data.message
          }
          // Case 3: Error in response.data.error
          else if (error.response.data.error) {
            errorMessage = error.response.data.error
          }
        }

        // Handle cooldown specifically (400 or 403 status with cooldown message)
        if (error.response?.status === 400 || error.response?.status === 500) {
          this.cooldownMessage = errorMessage

          // Extract cooldown time if available
          const match = errorMessage.match(/(\d+)\s*giờ/)
          if (match) {
            const hours = parseInt(match[1])
            const cooldownTime = new Date()
            cooldownTime.setHours(cooldownTime.getHours() + hours)
            this.canRetakeAfter = cooldownTime.toISOString()
          }

          ElMessage.warning({
            message: errorMessage,
            duration: 5000,
            showClose: true,
          })
        } else {
          ElMessage.error({
            message: errorMessage,
            duration: 5000,
            showClose: true,
          })
        }

        return false
      } finally {
        this.loading = false
      }
    },

    async submitPlacementTest(answers) {
      this.submitting = true

      try {
        const formattedAnswers = answers.map((ans) => ({
          questionId: ans.questionId,
          selectedOptions: ans.selectedOptions || [],
          textAnswer: ans.textAnswer || null,
        }))

        console.log('📤 Submitting placement test:', {
          totalAnswers: formattedAnswers.length,
          answers: formattedAnswers,
        })

        const response = await placementAPI.submitPlacementTest({
          answers: formattedAnswers,
        })

        console.log('📥 Submit Response:', response)

        let result = null

        if (response.data?.success && response.data?.data) {
          result = response.data.data
        } else if (response.data?.score !== undefined) {
          result = response.data
        } else if (response.score !== undefined) {
          result = response
        }

        if (!result) {
          throw new Error('Invalid response format from submit endpoint')
        }

        this.lastResult = result

        if (this.lastResult.canRetakeAfter) {
          this.canRetakeAfter = this.lastResult.canRetakeAfter
        }

        console.log('✅ Placement test submitted:', this.lastResult)

        ElMessage.success({
          message: `Hoàn thành! Điểm: ${this.lastResult.score}% - Level: ${this.lastResult.assignedLevel}`,
          duration: 5000,
        })

        return this.lastResult
      } catch (error) {
        console.error('❌ Error submitting placement test:', error)

        // ✅ Extract proper error message
        let errorMessage = 'Không thể nộp bài. Vui lòng thử lại.'

        if (error.response?.data) {
          if (typeof error.response.data === 'string') {
            errorMessage = error.response.data
          } else if (error.response.data.message) {
            errorMessage = error.response.data.message
          } else if (error.response.data.error) {
            errorMessage = error.response.data.error
          }
        }

        ElMessage.error({
          message: errorMessage,
          duration: 5000,
          showClose: true,
        })

        throw error
      } finally {
        this.submitting = false
      }
    },

    clearTestData() {
      this.testData = null
      this.lastResult = null
    },

    resetStore() {
      this.testData = null
      this.loading = false
      this.submitting = false
      this.lastResult = null
      this.canRetakeAfter = null
      this.cooldownMessage = null
    },
  },
})
