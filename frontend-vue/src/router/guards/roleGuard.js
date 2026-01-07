import { useAuthStore } from '@/stores/auth'
import { isTokenExpired } from '@/utils/jwt'

// Danh sách các trang Public (Ai cũng vào được)
const PUBLIC_PATHS = [
  '/auth/login',
  '/auth/register',
  '/auth/forgot-password',
  '/auth/verify-email',
  '/auth/reset-password',
  '/admin/login',
  '/teacher/login',
  '/',
  '/welcome',
  '/about',
  '/forbidden',
  '/404',
]

export const roleGuard = async (to, from, next) => {
  const authStore = useAuthStore()
  const path = to.path

  // ==============================================================
  // 1. CẤU HÌNH ROLE & TÊN ROUTE (Quan trọng: Phải đúng tên trong router/index.js)
  // ==============================================================
  let requiredRole = null
  let tokenKey = null
  let loginRouteName = 'login' // Mặc định là user login

  // --- ADMIN ZONE ---
  if (path.startsWith('/admin')) {
    requiredRole = 'ADMIN'
    tokenKey = 'adminToken'
    loginRouteName = 'admin-login' // ✅ Sửa thành chữ thường gạch nối
  }
  // --- TEACHER ZONE ---
  else if (path.startsWith('/teacher')) {
    requiredRole = 'TEACHER'
    tokenKey = 'teacherToken'
    loginRouteName = 'teacher-login' // ✅ Sửa thành chữ thường gạch nối
  }
  // --- USER ZONE ---
  else if (
    path.startsWith('/user') ||
    path.startsWith('/profile') ||
    path.includes('/change-password')
  ) {
    requiredRole = 'USER'
    tokenKey = 'userToken'
    loginRouteName = 'login' // ✅ Sửa thành chữ thường
  }

  // ==============================================================
  // 2. XỬ LÝ PUBLIC ROUTE & GUEST CHECK
  // ==============================================================
  if (PUBLIC_PATHS.includes(path) || to.meta.public || !requiredRole) {
    authStore.checkAuth() // Sync state nhẹ

    // Nếu đã login rồi thì không cho vào trang login nữa (Redirect về Dashboard)
    if (to.name === 'admin-login' && authStore.isAdminAuthenticated)
      return next({ name: 'admin-dashboard' })
    if (to.name === 'teacher-login' && authStore.isTeacherAuthenticated)
      return next({ name: 'teacher-dashboard' })
    if ((to.name === 'login' || to.name === 'register') && authStore.isUserAuthenticated)
      return next({ name: 'home' })

    return next()
  }

  // ==============================================================
  // 3. KIỂM TRA BẢO MẬT (TOKEN & USER DATA)
  // ==============================================================

  // 3.1. Kiểm tra Token
  const token = localStorage.getItem(tokenKey)
  if (!token || isTokenExpired(token)) {
    console.log(`🚫 Token invalid/expired for ${requiredRole} -> Redirect to Login`)

    // Xóa rác để tránh lỗi loop
    localStorage.removeItem(tokenKey)
    if (requiredRole === 'ADMIN') authStore.clearAdmin()
    else if (requiredRole === 'TEACHER') authStore.clearTeacher()
    else authStore.clearUser()

    // Đá về trang Login tương ứng
    return next({
      name: loginRouteName,
      query: { redirect: to.fullPath, reason: 'expired' },
    })
  }

  // 3.2. Token OK -> Check tiếp Data User trong Store
  authStore.checkAuth() // Nạp data từ LocalStorage vào Store

  // Lấy object user cụ thể tương ứng với Role
  let specificUser = null
  if (requiredRole === 'ADMIN') specificUser = authStore.admin
  else if (requiredRole === 'TEACHER') specificUser = authStore.teacher
  else specificUser = authStore.user

  // 3.3. Trường hợp: Có Token nhưng mất Data User (User undefined)
  // Đây là lỗi bạn đang gặp! Code sẽ tự động xóa token và bắt đăng nhập lại.
  if (!specificUser) {
    console.warn(`⚠️ Token exists but User Data is missing for ${requiredRole}. Force Logout.`)

    localStorage.removeItem(tokenKey)
    if (requiredRole === 'ADMIN') authStore.clearAdmin()
    else if (requiredRole === 'TEACHER') authStore.clearTeacher()
    else authStore.clearUser()

    return next({ name: loginRouteName })
  }

  // 3.4. Trường hợp: Có User nhưng sai Role (Ví dụ User thường mò vào Admin)
  if (specificUser.role !== requiredRole) {
    console.warn(`⚠️ Role mismatch! Got ${specificUser.role} but req ${requiredRole}`)
    return next({ name: 'not-found' }) // Redirect về trang 404
  }

  // 4. Mọi thứ OK -> Cho vào
  next()
}
