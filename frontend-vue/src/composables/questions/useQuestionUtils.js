/**
 * Composable chứa các utility functions dùng chung cho Question components
 */
export function useQuestionUtils() {
  /**
   * Get question type class for styling
   */
  const getQuestionTypeClass = (type) => {
    const map = {
      MULTIPLE_CHOICE:
        'bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:border-blue-800',
      TRUE_FALSE:
        'bg-purple-50 text-purple-700 border-purple-200 dark:bg-purple-900/30 dark:text-purple-300 dark:border-purple-800',
      FILL_BLANK:
        'bg-green-50 text-green-700 border-green-200 dark:bg-green-900/30 dark:text-green-400 dark:border-green-800',
      VERB_FORM:
        'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-400 dark:border-emerald-800',
      TEXT_ANSWER:
        'bg-teal-50 text-teal-700 border-teal-200 dark:bg-teal-900/30 dark:text-teal-400 dark:border-teal-800',
      SENTENCE_TRANSFORMATION:
        'bg-violet-50 text-violet-700 border-violet-200 dark:bg-violet-900/30 dark:text-violet-300 dark:border-violet-800',
      MATCHING:
        'bg-orange-50 text-orange-700 border-orange-200 dark:bg-orange-900/30 dark:text-orange-400 dark:border-orange-800',
      SENTENCE_BUILDING:
        'bg-indigo-50 text-indigo-700 border-indigo-200 dark:bg-indigo-900/30 dark:text-indigo-300 dark:border-indigo-800',
      ERROR_CORRECTION:
        'bg-red-50 text-red-700 border-red-200 dark:bg-red-900/30 dark:text-red-400 dark:border-red-800',
      PRONUNCIATION:
        'bg-cyan-50 text-cyan-700 border-cyan-200 dark:bg-cyan-900/30 dark:text-cyan-300 dark:border-cyan-800',
      OPEN_ENDED:
        'bg-gray-100 text-gray-700 border-gray-200 dark:bg-gray-800 dark:text-gray-300 dark:border-gray-600',
    }
    return map[type] || 'bg-gray-50 text-gray-700 border-gray-200'
  }

  /**
   * Safely parse metadata
   */
  const getSafeMetadata = (row) => {
    if (!row.metadata) return {}
    if (typeof row.metadata === 'string') {
      try {
        return JSON.parse(row.metadata)
      } catch (e) {
        return {}
      }
    }
    return row.metadata
  }

  /**
   * Get answer preview HTML string based on question type
   */
  const getAnswerPreview = (row) => {
    const meta = getSafeMetadata(row)
    const type = row.questionType

    // 1. MULTIPLE_CHOICE
    if (type === 'MULTIPLE_CHOICE') {
      const correctOpt = meta.options?.find((opt) => opt.isCorrect === true)
      return correctOpt?.text
        ? `<span class="text-gray-400">Đúng:</span> <b class="text-green-600">${correctOpt.text}</b>`
        : '<span class="text-gray-300 italic">Chưa có đáp án</span>'
    }

    // 2. TRUE_FALSE
    if (type === 'TRUE_FALSE') {
      const correctOpt = meta.options?.find((opt) => opt.isCorrect === true)
      return correctOpt?.text
        ? `<span class="text-gray-400">Đúng:</span> <b class="text-green-600">${correctOpt.text}</b>`
        : '<span class="text-gray-300 italic">Chưa có đáp án</span>'
    }

    // 3. FILL_BLANK
    if (type === 'FILL_BLANK') {
      if (!meta.blanks || meta.blanks.length === 0) return ''
      const firstBlank = meta.blanks[0]
      const answers = Array.isArray(firstBlank.correctAnswers)
        ? firstBlank.correctAnswers
        : [firstBlank.correctAnswers]
      return answers.join(' <span class="text-gray-300">/</span> ')
    }

    // 4. VERB_FORM
    if (type === 'VERB_FORM') {
      if (!meta.blanks || meta.blanks.length === 0) return ''
      const firstBlank = meta.blanks[0]
      const verb = firstBlank.verb || firstBlank.hint || '?'
      const answers = Array.isArray(firstBlank.correctAnswers)
        ? firstBlank.correctAnswers
        : [firstBlank.correctAnswers]
      return `<span class="text-purple-600">${verb}</span> → ${answers.join(' / ')}`
    }

    // 5. TEXT_ANSWER
    if (type === 'TEXT_ANSWER') {
      // New format: blanks array
      if (meta.blanks && meta.blanks.length > 0) {
        const answers = Array.isArray(meta.blanks[0].correctAnswers)
          ? meta.blanks[0].correctAnswers
          : [meta.blanks[0].correctAnswers]
        return answers.join(' <span class="text-gray-300">/</span> ')
      }
      // Old format: correctAnswer string
      return meta.correctAnswer || ''
    }

    // 6. SENTENCE_TRANSFORMATION
    if (type === 'SENTENCE_TRANSFORMATION') {
      if (!meta.correctAnswers || meta.correctAnswers.length === 0) return ''
      return `<span class="text-violet-600">${meta.correctAnswers[0]}</span>`
    }

    // 7. SENTENCE_BUILDING
    if (type === 'SENTENCE_BUILDING') {
      return meta.correctSentence
        ? `<span class="text-indigo-600">${meta.correctSentence}</span>`
        : ''
    }

    // 8. MATCHING
    if (type === 'MATCHING') {
      const count = meta.pairs?.length || 0
      return `<span class="text-orange-600">${count} cặp từ</span>`
    }

    // 9. ERROR_CORRECTION
    if (type === 'ERROR_CORRECTION') {
      return meta.correction
        ? `<b class="text-red-500">${meta.errorText}</b> → <b class="text-green-600">${meta.correction}</b>`
        : ''
    }

    // 10. PRONUNCIATION
    if (type === 'PRONUNCIATION') {
      const count = meta.words?.length || 0
      return `<span class="text-cyan-600">${count} từ cần phân loại</span>`
    }

    // 11. OPEN_ENDED
    if (type === 'OPEN_ENDED') {
      return '<span class="text-gray-400 italic">Cần chấm điểm</span>'
    }

    return ''
  }

  /**
   * Truncate HTML content to specified length
   */
  const truncateHtml = (html, limit) => {
    if (!html) return ''
    const tmp = document.createElement('DIV')
    tmp.innerHTML = html
    const text = tmp.textContent || tmp.innerText || ''
    return text.length <= limit ? text : text.substring(0, limit) + '...'
  }

  return {
    getQuestionTypeClass,
    getSafeMetadata,
    getAnswerPreview,
    truncateHtml,
  }
}
