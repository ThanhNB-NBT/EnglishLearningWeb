/**
 * Tạo link Avatar dựa trên tên người dùng
 * @param {String} name - Tên người dùng (FullName hoặc Username)
 * @returns {String} URL ảnh avatar
 */
export function getAvatarUrl(name) {
  // Nếu không có tên thì lấy chữ "User" làm mặc định
  const seed = name ? name : 'User'

  // Dùng encodeURIComponent để xử lý tên có dấu tiếng Việt hoặc khoảng trắng
  return `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(seed)}`
}
