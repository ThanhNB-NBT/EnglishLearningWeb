import { computed } from 'vue'

export function useQuestionData(question) {
  // Safe Accessor: Lấy từ question.data (nested) hoặc fallback

  // Explanation (Giải thích)
  const explanation = computed(() => question?.data?.explanation || question?.explanation || '')

  // Multiple Choice / True False
  const options = computed(() => question?.data?.options || [])

  // Fill Blank
  const blanks = computed(() => question?.data?.blanks || [])
  const wordBank = computed(() => question?.data?.wordBank || [])

  // Matching
  const pairs = computed(() => question?.data?.pairs || [])

  // Others
  const correctAnswers = computed(() => question?.data?.correctAnswers || [])
  const correction = computed(() => question?.data?.correction || '')
  const correctSentence = computed(() => question?.data?.correctSentence || '')

  // Cột cứng
  const questionText = computed(() => question?.questionText || '')
  const points = computed(() => question?.points || 0)
  const questionType = computed(() => question?.questionType || 'MULTIPLE_CHOICE')

  return {
    questionText,
    questionType,
    points,
    explanation,
    options,
    blanks,
    wordBank,
    pairs,
    correctAnswers,
    correction,
    correctSentence
  }
}
