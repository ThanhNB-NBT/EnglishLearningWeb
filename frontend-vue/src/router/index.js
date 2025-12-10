import { createRouter, createWebHistory } from 'vue-router'
import { authGuard } from './guards/authGuard'
import { adminGuard } from './guards/adminGuard'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // ==================== PUBLIC ROUTES ====================
    {
      path: '/',
      component: () => import('@/layouts/PublicLayout.vue'),
      children: [
        {
          path: '',
          name: 'welcome',
          component: () => import('@/views/WelcomeView.vue'),
        },
      ],
    },

    // ==================== AUTH ROUTES (Guest Only) ====================
    {
      path: '/auth',
      component: () => import('@/layouts/AuthLayout.vue'),
      beforeEnter: (to, from, next) => {
        const userToken = localStorage.getItem('userToken')

        // CHỈ REDIRECT nếu đã login USER
        if (userToken) {
          console.log('Auth route: User already logged in, redirect to dashboard')
          return next('/user/home')
        }

        // CHO PHÉP truy cập - admin có thể vào để login user
        console.log('Auth route: Allow access to auth pages')
        next()
      },
      children: [
        {
          path: 'login',
          name: 'login',
          component: () => import('@/views/auth/LoginView.vue'),
        },
        {
          path: 'register',
          name: 'register',
          component: () => import('@/views/auth/RegisterView.vue'),
        },
        {
          path: 'verify-email',
          name: 'verify-email',
          component: () => import('@/views/auth/VerifyEmailView.vue'),
        },
        {
          path: 'forgot-password',
          name: 'forgot-password',
          component: () => import('@/views/auth/ForgotPasswordView.vue'),
        },
        {
          path: 'reset-password',
          name: 'reset-password',
          component: () => import('@/views/auth/ResetPasswordView.vue'),
        },
      ],
    },

    // ==================== USER ROUTES (Auth Required) ====================
    {
      path: '/user',
      component: () => import('@/layouts/HomeLayout.vue'),
      beforeEnter: authGuard,
      children: [
        {
          path: 'home',
          name: 'home',
          component: () => import('@/views/user/HomeView.vue'),
          meta: { title: 'Home' },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue'),
          meta: { title: 'Profile' },
        },
        {
          path: 'change-password',
          name: 'change-password',
          component: () => import('@/views/ChangePasswordView.vue'),
          meta: { title: 'Change Password' },
        },
        {
          path: 'grammar',
          name: 'user-grammar', // Danh sách chủ đề
          component: () => import('@/views/user/grammar/GrammarTopicsView.vue'),
          meta: { title: 'Grammar Topics' },
        },
        {
          path: 'grammar/lesson/:lessonId',
          name: 'user-grammar-lesson', // Màn hình học (Lý thuyết/Thực hành)
          component: () => import('@/views/user/grammar/LessonPlayerView.vue'),
          meta: { title: 'Grammar Lesson' },
        },
        {
          path: 'reading',
          name: 'reading',
          component: () => import('@/views/user/ReadingView.vue'),
          meta: { title: 'Reading' },
        },
      ],
    },

    // ==================== ADMIN ROUTES ====================
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('@/views/admin/AdminLoginView.vue'),
      beforeEnter: (to, from, next) => {
        const adminToken = localStorage.getItem('adminToken')

        // CHỈ REDIRECT nếu đã login ADMIN
        if (adminToken) {
          console.log('Admin login: Already logged in, redirect to admin dashboard')
          return next('/admin/dashboard')
        }

        // CHO PHÉP truy cập - user có thể vào để login admin
        console.log('Admin login: Allow access to admin login page')
        next()
      },
    },

    {
      path: '/admin',
      component: () => import('@/layouts/AdminDashboardLayout.vue'),
      beforeEnter: adminGuard,
      children: [
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/AdminDashboardView.vue'),
          meta: { title: 'Dashboard' },
        },
        {
          path: 'profile',
          name: 'admin-profile',
          component: () => import('@/views/ProfileView.vue'), // ✅ SHARED
          meta: { title: 'Profile' },
        },
        {
          path: 'change-password',
          name: 'admin-change-password',
          component: () => import('@/views/ChangePasswordView.vue'), // ✅ SHARED
          meta: { title: 'Change Password' },
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/UsersManagementView.vue'),
          meta: { title: 'Users Management' },
        },
        {
          path: 'grammar',
          name: 'admin-grammar',
          // redirect: '/admin/grammar', // Redirect to topics by default
          component: () => import('@/views/admin/GrammarManagementView.vue'),
          meta: { title: 'Grammar Management' },
        },
        {
          path: 'reading',
          name: 'admin-reading',
          component: () => import('@/views/admin/ReadingManagementView.vue'),
          meta: { title: 'Reading Management' },
        },
        {
          path: 'cleanup-user',
          name: 'admin-cleanup-user',
          component: () => import('@/views/admin/CleanupManagementView.vue'),
          meta: { title: 'Cleanup User' },
        },
      ],
    },

    // ==================== 404 ====================
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

export default router
