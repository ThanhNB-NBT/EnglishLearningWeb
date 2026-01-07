// src/composables/questions/useQuestionUtils.js - COMPLETE VERSION

export function useQuestionUtils() {

  // ✅ FIX: Thêm getQuestionTypeClass
  const getQuestionTypeClass = (type) => {
    const classMap = {
      MULTIPLE_CHOICE: 'bg-blue-50 text-blue-700 border-blue-200',
      TRUE_FALSE: 'bg-green-50 text-green-700 border-green-200',
      FILL_BLANK: 'bg-purple-50 text-purple-700 border-purple-200',
      VERB_FORM: 'bg-purple-50 text-purple-700 border-purple-200',
      TEXT_ANSWER: 'bg-indigo-50 text-indigo-700 border-indigo-200',
      MATCHING: 'bg-pink-50 text-pink-700 border-pink-200',
      SENTENCE_BUILDING: 'bg-yellow-50 text-yellow-700 border-yellow-200',
      SENTENCE_TRANSFORMATION: 'bg-orange-50 text-orange-700 border-orange-200',
      ERROR_CORRECTION: 'bg-red-50 text-red-700 border-red-200',
      PRONUNCIATION: 'bg-cyan-50 text-cyan-700 border-cyan-200',
      OPEN_ENDED: 'bg-teal-50 text-teal-700 border-teal-200',
      COMPLETE_CONVERSATION: 'bg-blue-50 text-blue-700 border-blue-200',
    }
    return classMap[type] || 'bg-gray-50 text-gray-700 border-gray-200'
  }

  const getQuestionTypeLabel = (type) => {
    const map = {
      MULTIPLE_CHOICE: 'Trắc nghiệm',
      FILL_BLANK: 'Điền từ',
      TRUE_FALSE: 'Đúng / Sai',
      MATCHING: 'Nối từ',
      SENTENCE_BUILDING: 'Xây dựng câu',
      SENTENCE_TRANSFORMATION: 'Viết lại câu',
      ERROR_CORRECTION: 'Sửa lỗi sai',
      PRONUNCIATION: 'Phát âm',
      OPEN_ENDED: 'Câu hỏi mở',
      TEXT_ANSWER: 'Trả lời ngắn',
      VERB_FORM: 'Chia động từ',
      COMPLETE_CONVERSATION: 'Hoàn thành hội thoại',
    }
    return map[type] || type
  }

  // ✅ FIX: Thêm getAnswerPreview
  const getAnswerPreview = (question) => {
    if (!question) return ''

    const data = question.data || {}

    switch (question.questionType) {
      case 'MULTIPLE_CHOICE':
      case 'TRUE_FALSE':
      case 'COMPLETE_CONVERSATION': {
        const options = data.options || []
        const correctOpt = options.find((o) => o.isCorrect === true)
        if (correctOpt) {
          return `<span class="text-green-600 font-medium">✓ ${correctOpt.text}</span>`
        }
        return '<span class="text-gray-400">No correct answer</span>'
      }

      case 'FILL_BLANK':
      case 'VERB_FORM':
      case 'TEXT_ANSWER': {
        const blanks = data.blanks || []
        if (blanks.length > 0) {
          const answers = blanks
            .map((b) => {
              const ans = b.correctAnswers || []
              return ans.length > 0 ? ans[0] : ''
            })
            .filter(Boolean)

          if (answers.length > 0) {
            return `<span class="text-green-600 font-medium">${answers.join(', ')}</span>`
          }
        }
        return '<span class="text-gray-400">No answers</span>'
      }

      case 'MATCHING': {
        const pairs = data.pairs || []
        if (pairs.length > 0) {
          return `<span class="text-blue-600">${pairs.length} pairs</span>`
        }
        return '<span class="text-gray-400">No pairs</span>'
      }

      case 'SENTENCE_BUILDING': {
        if (data.correctSentence) {
          return `<span class="text-green-600 font-medium">${data.correctSentence}</span>`
        }
        return '<span class="text-gray-400">No correct sentence</span>'
      }

      case 'SENTENCE_TRANSFORMATION': {
        const answers = data.correctAnswers || []
        if (answers.length > 0) {
          return `<span class="text-green-600 font-medium">${answers[0]}</span>`
        }
        return '<span class="text-gray-400">No correct answers</span>'
      }

      case 'ERROR_CORRECTION': {
        if (data.correction) {
          return `<span class="text-green-600 font-medium">${data.correction}</span>`
        }
        return '<span class="text-gray-400">No correction</span>'
      }

      case 'PRONUNCIATION': {
        const words = data.words || []
        if (words.length > 0) {
          return `<span class="text-purple-600">${words.length} words</span>`
        }
        return '<span class="text-gray-400">No words</span>'
      }

      case 'OPEN_ENDED': {
        if (data.suggestedAnswer) {
          return `<span class="text-blue-600">Has suggested answer</span>`
        }
        return '<span class="text-gray-400">Open ended</span>'
      }

      default:
        return '<span class="text-gray-400">-</span>'
    }
  }

  // Helper rút gọn HTML để preview
  const truncateHtml = (html, maxLength = 50) => {
    if (!html) return ''
    const tmp = document.createElement('div')
    tmp.innerHTML = html
    const text = tmp.textContent || tmp.innerText || ''
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
  }

  // ✅ VALIDATE: Kiểm tra dữ liệu dựa trên cấu trúc nested 'data'
  const validateQuestionData = (question) => {
    if (!question.questionType) return { valid: false, message: 'Chưa chọn loại câu hỏi' }

    // Ưu tiên lấy từ metadata (khi đang trong Form) hoặc data (khi từ API về)
    const data = question.metadata || question.data || {}

    switch (question.questionType) {
      case 'MULTIPLE_CHOICE':
      case 'TRUE_FALSE':
      case 'COMPLETE_CONVERSATION': {
        if (!data.options || data.options.length < 2) {
          return { valid: false, message: 'Cần ít nhất 2 options' }
        }
        const hasCorrect = data.options.some((o) => o.isCorrect)
        if (!hasCorrect) return { valid: false, message: 'Chưa chọn đáp án đúng' }
        const hasEmpty = data.options.some((o) => !o.text || !o.text.trim())
        if (hasEmpty) return { valid: false, message: 'Nội dung đáp án không được trống' }
        break
      }

      case 'FILL_BLANK':
      case 'VERB_FORM':
      case 'TEXT_ANSWER': {
        if (!data.blanks || data.blanks.length === 0) {
          return { valid: false, message: 'Chưa xác định vị trí điền từ (Blanks)' }
        }
        // Validate từng blank
        for (const b of data.blanks) {
          if (!b.correctAnswers || b.correctAnswers.length === 0) {
            return { valid: false, message: `Vị trí [${b.position}] chưa có đáp án đúng` }
          }
        }
        break
      }

      case 'MATCHING': {
        if (!data.pairs || data.pairs.length < 2) {
          return { valid: false, message: 'Cần ít nhất 2 cặp nối' }
        }
        const emptyPair = data.pairs.some((p) => !p.left || !p.right)
        if (emptyPair) return { valid: false, message: 'Các cặp nối không được để trống' }
        break
      }

      case 'SENTENCE_TRANSFORMATION': {
        if (!data.correctAnswers || data.correctAnswers.length === 0) {
          return { valid: false, message: 'Chưa nhập đáp án đúng' }
        }
        break
      }

      case 'ERROR_CORRECTION': {
        if (!data.correction) return { valid: false, message: 'Chưa nhập câu sửa lỗi' }
        break
      }

      case 'SENTENCE_BUILDING': {
        if (!data.correctSentence) return { valid: false, message: 'Chưa nhập câu đúng' }
        break
      }
    }

    return { valid: true }
  }

  const hasWordBank = (type) => ['FILL_BLANK', 'VERB_FORM'].includes(type)

  return {
    getQuestionTypeClass,    // ✅ Export
    getQuestionTypeLabel,
    getAnswerPreview,        // ✅ Export
    truncateHtml,            // ✅ Export
    validateQuestionData,
    hasWordBank,
  }
}
