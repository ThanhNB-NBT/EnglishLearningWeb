// src/utils/jwt.js

export function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      window
        .atob(base64)
        .split('')
        .map(function (c) {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
        })
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch (e) {
    console.error('Failed to parse JWT:', e)
    return null
  }
}

export function isTokenExpired(token) {
  if (!token) return true
  const decoded = parseJwt(token)
  if (!decoded || !decoded.exp) return true

  const currentTime = Date.now() / 1000
  // Kiểm tra nếu thời gian hiện tại > thời gian hết hạn (trừ hao 10s cho chắc)
  return decoded.exp < currentTime + 10
}
