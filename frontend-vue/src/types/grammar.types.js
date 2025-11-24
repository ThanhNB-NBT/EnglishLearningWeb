// src/types/grammar.js

/**
 * English Level Enum
 */
export const EnglishLevel = {
  BEGINNER: 'BEGINNER',
  ELEMENTARY: 'ELEMENTARY',
  INTERMEDIATE: 'INTERMEDIATE',
  UPPER_INTERMEDIATE: 'UPPER_INTERMEDIATE',
  ADVANCED: 'ADVANCED',
}

/**
 * Lesson Type Enum
 */
export const LessonType = {
  THEORY: 'THEORY',
  PRACTICE: 'PRACTICE',
}

/**
 * Question Type Enum
 */
export const QuestionType = {
  MULTIPLE_CHOICE: 'MULTIPLE_CHOICE',
  FILL_BLANK: 'FILL_BLANK',
  TRUE_FALSE: 'TRUE_FALSE',
  VERB_FORM: 'VERB_FORM',
  SHORT_ANSWER: 'SHORT_ANSWER',
  ERROR_CORRECTION: 'ERROR_CORRECTION',
  MATCHING: 'MATCHING',
  COMPLETE_CONVERSATION: 'COMPLETE_CONVERSATION',
  PRONUNCIATION: 'PRONUNCIATION',
  READING_COMPREHENSION: 'READING_COMPREHENSION',
  OPEN_ENDED: 'OPEN_ENDED',
  SENTENCE_BUILDING: 'SENTENCE_BUILDING',
}

/**
 * Level display config
 */
export const LEVEL_CONFIG = [
  { value: 'BEGINNER', label: 'Beginner (Sơ cấp)', color: 'success' },
  { value: 'ELEMENTARY', label: 'Elementary (Cơ bản)', color: 'primary' },
  { value: 'INTERMEDIATE', label: 'Intermediate (Trung cấp)', color: 'warning' },
  { value: 'UPPER_INTERMEDIATE', label: 'Upper Intermediate (Trung cao)', color: 'danger' },
  { value: 'ADVANCED', label: 'Advanced (Nâng cao)', color: 'danger' },
]

/**
 * Get level color helper
 */
export const getLevelColor = (level) => {
  const config = LEVEL_CONFIG.find(l => l.value === level)
  return config?.color || 'info'
}

/**
 * Get level label helper
 */
export const getLevelLabel = (level) => {
  const config = LEVEL_CONFIG.find(l => l.value === level)
  return config?.label || level
}
