import { ref, computed, onUnmounted, nextTick } from 'vue'

export function useLearningPlayer(store) {
  // --- STATE ---
  const viewMode = ref('full')
  const remainingTime = ref(0)
  const timerInterval = ref(null)

  const userAnswers = ref({})
  const submitting = ref(false)
  const showResult = ref(false)
  const resultData = ref({})
  const hasScrolledToBottom = ref(false)

  let observer = null

  // --- COMPUTED ---
  const answeredCount = computed(() => Object.keys(userAnswers.value).length)

  // --- METHODS ---

  const startTimer = (onTimeoutCallback) => {
    stopTimer()

    if (remainingTime.value <= 0) {
      remainingTime.value = 0
      if (onTimeoutCallback) onTimeoutCallback()
      return
    }

    timerInterval.value = setInterval(() => {
      if (remainingTime.value > 0) {
        remainingTime.value--
      } else {
        stopTimer()
        if (onTimeoutCallback) onTimeoutCallback()
      }
    }, 1000)
  }

  const stopTimer = () => {
    if (timerInterval.value) {
      clearInterval(timerInterval.value)
      timerInterval.value = null
    }
  }

  const setupScrollObserver = (markerId) => {
    if (observer) {
      observer.disconnect()
      observer = null
    }

    nextTick(() => {
      const markerElement = document.getElementById(markerId)
      if (!markerElement) {
        console.warn(`⚠️ Element #${markerId} not found for scroll observer`)
        return
      }

      const contentElement = markerElement.closest('.overflow-y-auto')
      if (!contentElement) {
        console.warn('⚠️ No scrollable parent found')
        return
      }

      let debounceTimer = null
      observer = new IntersectionObserver(
        (entries) => {
          if (debounceTimer) clearTimeout(debounceTimer)
          debounceTimer = setTimeout(() => {
            const isVisible = entries.some((entry) => entry.isIntersecting)
            if (hasScrolledToBottom.value !== isVisible) {
              hasScrolledToBottom.value = isVisible
            }
          }, 200)
        },
        {
          root: contentElement,
          threshold: 0.1,
        },
      )

      observer.observe(markerElement)
      console.log(`✅ Scroll observer setup for #${markerId}`)
    })
  }

  const handleAnswerUpdate = ({ questionId, value }) => {
    if (!questionId) return
    userAnswers.value[questionId] = value
  }

  // ✅ FIX: NEW METHOD - Clear question state để retry hoạt động đúng
  const clearQuestionsState = (groupedTasks, standaloneQuestions) => {
    console.log('🔄 Clearing questions state for retry...')

    const clearQuestion = (q) => {
      // Clear feedback và correct answer
      delete q.isCorrect
      delete q.feedback
      delete q.score
      delete q.correctAnswer

      // Clear options state
      if (q.data?.options) {
        q.data.options.forEach((opt) => {
          delete opt.isCorrect
        })
      }

      // Clear blanks correct answers
      if (q.data?.blanks) {
        q.data.blanks.forEach((blank) => {
          delete blank.correctAnswers
        })
      }

      // Clear other data fields
      if (q.data) {
        delete q.data.correction
        delete q.data.correctAnswer
        delete q.data.correctAnswers
      }
    }

    if (groupedTasks) {
      groupedTasks.forEach((task) => {
        if (task.questions) {
          task.questions.forEach(clearQuestion)
        }
      })
    }

    if (standaloneQuestions) {
      standaloneQuestions.forEach(clearQuestion)
    }

    console.log('✅ Questions state cleared')
  }

  const mergeResultsToQuestions = (resultData, groupedTasks, standaloneQuestions) => {
    if (!resultData.results) return

    const resultsMap = new Map()
    resultData.results.forEach((r) => resultsMap.set(r.questionId, r))

    const updateQ = (q) => {
      const res = resultsMap.get(q.id)
      if (!res) return

      q.isCorrect = res.isCorrect
      q.feedback = res.feedback
      q.score = res.points

      switch (q.questionType) {
        case 'MULTIPLE_CHOICE':
        case 'TRUE_FALSE':
        case 'PRONUNCIATION':
          q.correctAnswer = res.correctAnswer
          if (q.data && q.data.options && res.correctAnswer) {
            q.data.options.forEach((opt) => {
              if (opt.text == res.correctAnswer || opt.value == res.correctAnswer) {
                opt.isCorrect = true
              }
            })
          }
          break

        case 'FILL_BLANK':
        case 'TEXT_ANSWER':
          q.correctAnswer = res.correctAnswer
          if (q.data && q.data.blanks) {
            let parsedAnswers = {}
            try {
              if (res.correctAnswer && res.correctAnswer.startsWith('{')) {
                parsedAnswers = JSON.parse(res.correctAnswer)
              }
            } catch (e) {
              console.error('Error parsing fill-in-the-blank answers:', e)
            }

            q.data.blanks.forEach((blank) => {
              if (Object.keys(parsedAnswers).length > 0) {
                const key = blank.position || 1
                if (parsedAnswers[key]) blank.correctAnswers = [parsedAnswers[key]]
              } else if (res.correctAnswer) {
                blank.correctAnswers = [res.correctAnswer]
              }
            })
          }
          break

        case 'ERROR_CORRECTION':
          if (q.data) q.data.correction = res.correctAnswer
          break

        case 'SENTENCE_TRANSFORMATION':
        case 'SENTENCE_BUILDING':
        case 'OPEN_ENDED':
          if (q.data) {
            q.data.correctAnswer = res.correctAnswer
            q.data.correctAnswers = [res.correctAnswer]
          }
          break

        case 'MATCHING':
          try {
            q.correctAnswer =
              typeof res.correctAnswer === 'string'
                ? JSON.parse(res.correctAnswer)
                : res.correctAnswer
          } catch (e) {
            console.error('Error parsing matching correct answer:', e)
            q.correctAnswer = res.correctAnswer
          }
          break
      }
    }

    if (groupedTasks) groupedTasks.forEach((t) => t.questions.forEach(updateQ))
    if (standaloneQuestions) standaloneQuestions.forEach(updateQ)
  }

  const submitExam = async (lessonId, groupedTasks, standaloneQuestions) => {
    submitting.value = true
    try {
      const answers = Object.entries(userAnswers.value).map(([id, val]) => ({
        questionId: parseInt(id),
        textAnswer: typeof val === 'object' ? JSON.stringify(val) : String(val ?? ''),
        selectedOptions: [],
      }))

      const res = await store.submitLesson({
        lessonId: lessonId,
        answers: answers,
      })

      resultData.value = res
      mergeResultsToQuestions(res, groupedTasks, standaloneQuestions)
      showResult.value = true
      stopTimer()

      return res
    } catch (error) {
      console.error('Submit error:', error)
      throw error
    } finally {
      submitting.value = false
    }
  }

  const resetPlayerState = () => {
    stopTimer()

    userAnswers.value = {}
    showResult.value = false
    submitting.value = false
    resultData.value = {}
    hasScrolledToBottom.value = false
    viewMode.value = 'full'
    remainingTime.value = 0

    if (observer) {
      observer.disconnect()
      observer = null
    }
  }

  const formatTime = (s) => {
    if (!s && s !== 0) return '0:00'
    const m = Math.floor(s / 60)
    const sec = s % 60
    return `${m}:${sec.toString().padStart(2, '0')}`
  }

  onUnmounted(() => {
    stopTimer()
    if (observer) {
      observer.disconnect()
      observer = null
    }
  })

  return {
    viewMode,
    remainingTime,
    userAnswers,
    submitting,
    showResult,
    resultData,
    hasScrolledToBottom,
    answeredCount,

    startTimer,
    stopTimer,
    setupScrollObserver,
    handleAnswerUpdate,
    submitExam,
    resetPlayerState,
    clearQuestionsState, // ✅ NEW: Export method mới
    formatTime,
  }
}
