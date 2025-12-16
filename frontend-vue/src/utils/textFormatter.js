// src/utils/textFormatter.js

/**
 * Format transcript text by removing escape characters and normalizing whitespace
 * @param {string} text - Raw transcript text
 * @returns {string} - Formatted text
 */
export function formatTranscript(text) {
  if (!text) return ''

  return text
    // Remove escaped tabs and replace with space
    .replace(/\\t/g, ' ')
    // Remove escaped newlines
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\\r/g, '\n')
    // Remove actual tabs
    .replace(/\t/g, ' ')
    // Normalize newlines
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    // Remove multiple spaces (optional)
    .replace(/  +/g, ' ')
    // Trim each line
    .split('\n')
    .map(line => line.trim())
    .join('\n')
    // Remove multiple consecutive newlines (optional, max 2)
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

/**
 * Strip HTML tags from text
 * @param {string} html - HTML string
 * @returns {string} - Plain text
 */
export function stripHtml(html) {
  if (!html) return ''

  const tmp = document.createElement('div')
  tmp.innerHTML = html
  return tmp.textContent || tmp.innerText || ''
}

/**
 * Truncate text to a specific length with ellipsis
 * @param {string} text - Text to truncate
 * @param {number} maxLength - Maximum length
 * @returns {string} - Truncated text
 */
export function truncateText(text, maxLength = 100) {
  if (!text || text.length <= maxLength) return text
  return text.substring(0, maxLength).trim() + '...'
}

/**
 * Format time in seconds to human-readable format
 * @param {number} seconds - Time in seconds
 * @returns {string} - Formatted time (e.g., "5 phút", "1 giờ 30 phút")
 */
export function formatTime(seconds) {
  if (!seconds || seconds < 0) return '0 phút'

  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60

  const parts = []
  if (hours > 0) parts.push(`${hours} giờ`)
  if (minutes > 0) parts.push(`${minutes} phút`)
  if (secs > 0 && hours === 0) parts.push(`${secs} giây`)

  return parts.length > 0 ? parts.join(' ') : '0 phút'
}

/**
 * Convert newlines to <br> tags for HTML display
 * @param {string} text - Plain text
 * @returns {string} - HTML with <br> tags
 */
export function nl2br(text) {
  if (!text) return ''
  return text.replace(/\n/g, '<br>')
}

/**
 * Clean and normalize text for display
 * @param {string} text - Raw text
 * @param {object} options - Formatting options
 * @returns {string} - Cleaned text
 */
export function cleanText(text, options = {}) {
  const {
    removeMultipleSpaces = true,
    removeMultipleNewlines = true,
    trimLines = true,
    maxNewlines = 2
  } = options

  if (!text) return ''

  let result = text

  // Remove multiple spaces
  if (removeMultipleSpaces) {
    result = result.replace(/  +/g, ' ')
  }

  // Trim each line
  if (trimLines) {
    result = result.split('\n').map(line => line.trim()).join('\n')
  }

  // Remove multiple newlines
  if (removeMultipleNewlines) {
    const regex = new RegExp(`\\n{${maxNewlines + 1},}`, 'g')
    result = result.replace(regex, '\n'.repeat(maxNewlines))
  }

  return result.trim()
}

/**
 * Highlight search term in text
 * @param {string} text - Text to search in
 * @param {string} searchTerm - Term to highlight
 * @returns {string} - HTML with highlighted terms
 */
export function highlightText(text, searchTerm) {
  if (!text || !searchTerm) return text

  const regex = new RegExp(`(${escapeRegex(searchTerm)})`, 'gi')
  return text.replace(regex, '<mark>$1</mark>')
}

/**
 * Escape special regex characters
 * @param {string} str - String to escape
 * @returns {string} - Escaped string
 */
function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * Count words in text
 * @param {string} text - Text to count
 * @returns {number} - Word count
 */
export function wordCount(text) {
  if (!text) return 0
  return text.trim().split(/\s+/).filter(word => word.length > 0).length
}

/**
 * Get excerpt from text
 * @param {string} text - Full text
 * @param {number} wordLimit - Maximum number of words
 * @returns {string} - Excerpt
 */
export function getExcerpt(text, wordLimit = 30) {
  if (!text) return ''

  const words = text.trim().split(/\s+/)
  if (words.length <= wordLimit) return text

  return words.slice(0, wordLimit).join(' ') + '...'
}
