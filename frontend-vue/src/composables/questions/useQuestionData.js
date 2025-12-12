import { computed } from 'vue'

export function useQuestionData(props) {
  const meta = computed(() => props.question?.metadata || {})

  // 1. Multiple Choice (Trắc nghiệm, True/False)
  const options = computed(() => {
    return (meta.value.options || []).map(o => ({
      text: o.text || '',
      isCorrect: o.isCorrect || false,
      order: o.order || 0
    })).sort((a, b) => a.order - b.order)
  })

  // 2. Matching (Nối từ)
  const matchingPairs = computed(() => {
    const raw = meta.value.pairs || meta.value.matchingPairs || []
    return raw.map(p => ({
      left: p.left || p.key || '',
      right: p.right || p.value || ''
    })).filter(p => p.left && p.right)
  })

  // 3. Sentence Building (Sắp xếp câu)
  const buildingWords = computed(() => {
    if (meta.value.words?.length) return meta.value.words
    if (meta.value.correctOrder?.length) return meta.value.correctOrder
    if (meta.value.correctSentence) return meta.value.correctSentence.split(' ')
    return []
  })

  // 4. Sentence Transformation (Viết lại câu)
  const transformationData = computed(() => ({
    originalSentence: meta.value.originalSentence || '',
    beginningPhrase: meta.value.beginningPhrase || ''
  }))

  // 5. Pronunciation (Phát âm)
  const pronunciationData = computed(() => ({
    words: meta.value.words || [],
    categories: meta.value.categories || []
  }))

  // 6. Error Correction (Tìm lỗi sai)
  const errorCorrectionData = computed(() => ({
    errorText: meta.value.errorText || '',
    correction: meta.value.correction || ''
  }))

  // 7. Fill Blank (Điền từ)
  const blanks = computed(() => meta.value.blanks || [])

  return {
    options,
    matchingPairs,
    buildingWords,
    transformationData,
    pronunciationData,
    errorCorrectionData,
    blanks
  }
}
