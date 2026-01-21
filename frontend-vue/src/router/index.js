import { createRouter, createWebHistory } from 'vue-router'
import { roleGuard } from './guards/roleGuard' // ✅ CHỈ CẦN 1 GUARD

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

    // ==================== AUTH ROUTES ====================
    {
      path: '/auth',
      component: () => import('@/layouts/AuthLayout.vue'),
      beforeEnter: (to, from, next) => {
        const userToken = localStorage.getItem('userToken')
        if (userToken) {
          return next('/user/home')
        }
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

    // ==================== USER ROUTES ====================
    {
      path: '/user',
      component: () => import('@/layouts/HomeLayout.vue'),
      beforeEnter: roleGuard,
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
          name: 'user-change-password',
          component: () => import('@/views/ChangePasswordView.vue'),
          meta: { title: 'Change Password' },
        },
        {
          path: 'grammar',
          name: 'user-grammar',
          component: () => import('@/views/user/grammar/GrammarTopicsView.vue'),
          meta: { title: 'Grammar Topics' },
        },
        {
          path: 'grammar/lesson/:lessonId',
          name: 'user-grammar-lesson',
          component: () => import('@/views/user/grammar/GrammarPlayerView.vue'),
          meta: { title: 'Grammar Lesson' },
        },
        {
          path: 'reading',
          name: 'user-reading',
          component: () => import('@/views/user/reading/ReadingTopicsView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'reading/lesson/:lessonId',
          name: 'user-reading-lesson',
          component: () => import('@/views/user/reading/ReadingPlayerView.vue'),
          meta: { title: 'Reading Lesson' },
          props: true,
        },
        {
          path: 'listening',
          name: 'user-listening',
          component: () => import('@/views/user/listening/ListeningTopicsView.vue'),
          meta: { title: 'Listening Lessons' },
        },
        {
          path: 'listening/lesson/:lessonId',
          name: 'user-listening-lesson',
          component: () => import('@/views/user/listening/ListeningPlayerView.vue'),
          props: true,
          meta: { title: 'Listening Lesson' },
        },
        {
          path: 'placement-test',
          name: 'user-placement-test',
          component: () => import('@/views/user/PlacementTestView.vue'),
          meta: { title: 'Placement Test' },
        },
      ],
    },

    // ==================== ADMIN LOGIN ====================
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('@/views/admin/AdminLoginView.vue'),
      beforeEnter: (to, from, next) => {
        const adminToken = localStorage.getItem('adminToken')
        if (adminToken) {
          return next('/admin/dashboard')
        }
        next()
      },
    },

    // ==================== ADMIN ROUTES ====================
    {
      path: '/admin',
      component: () => import('@/layouts/ManagementLayout.vue'),
      beforeEnter: roleGuard, // ✅ Dùng chung 1 guard
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
          component: () => import('@/views/ProfileView.vue'),
          meta: { title: 'Profile' },
        },
        {
          path: 'change-password',
          name: 'admin-change-password',
          component: () => import('@/views/ChangePasswordView.vue'),
          meta: { title: 'Change Password' },
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/user/UsersManagementView.vue'),
          meta: { title: 'Users Management' },
        },
        {
          path: 'grammar',
          name: 'admin-grammar',
          component: () => import('@/views/admin/grammar/GrammarManagementView.vue'),
          meta: { title: 'Grammar Management' },
        },
        {
          path: 'reading',
          name: 'admin-reading',
          component: () => import('@/views/admin/reading/ReadingManagementView.vue'),
          meta: { title: 'Reading Management' },
        },
        {
          path: 'listening',
          name: 'admin-listening',
          component: () => import('@/views/admin/listening/ListeningManagementView.vue'),
          meta: { title: 'Listening Management' },
        },
        {
          path: 'cleanup-user',
          name: 'admin-cleanup-user',
          component: () => import('@/views/admin/user/CleanupManagementView.vue'),
          meta: { title: 'Cleanup User' },
        },
        {
          path: 'ai-import', // Đường dẫn sẽ là: /admin/ai-import
          name: 'admin-ai-import',
          component: () => import('@/views/admin/AIImportView.vue'),
          meta: {
            title: 'AI Nhập liệu thông minh',
          }
        },
      ],
    },

    // ==================== TEACHER LOGIN ====================
    {
      path: '/teacher/login',
      name: 'teacher-login',
      component: () => import('@/views/teacher/TeacherLoginView.vue'),
      beforeEnter: (to, from, next) => {
        const teacherToken = localStorage.getItem('teacherToken')
        if (teacherToken) {
          console.log('✅ Teacher already logged in, redirect to dashboard')
          return next('/teacher/dashboard')
        }
        next()
      },
    },

    // ==================== TEACHER ROUTES ====================
    {
      path: '/teacher',
      component: () => import('@/layouts/ManagementLayout.vue'),
      beforeEnter: roleGuard, // ✅ Dùng chung 1 guard
      children: [
        {
          path: 'dashboard',
          name: 'teacher-dashboard',
          component: () => import('@/views/teacher/TeacherDashboardView.vue'),
          meta: { title: 'Teacher Dashboard' },
        },
        {
          path: 'profile',
          name: 'teacher-profile',
          component: () => import('@/views/ProfileView.vue'),
          meta: { title: 'Profile' },
        },
        {
          path: 'change-password',
          name: 'teacher-change-password',
          component: () => import('@/views/ChangePasswordView.vue'),
          meta: { title: 'Change Password' },
        },
        {
          path: 'grammar',
          name: 'teacher-grammar',
          component: () => import('@/views/admin/grammar/GrammarManagementView.vue'),
          meta: { title: 'Grammar Management' },
        },
        {
          path: 'reading',
          name: 'teacher-reading',
          component: () => import('@/views/admin/reading/ReadingManagementView.vue'),
          meta: { title: 'Reading Management' },
        },
        {
          path: 'listening',
          name: 'teacher-listening',
          component: () => import('@/views/admin/listening/ListeningManagementView.vue'),
          meta: { title: 'Listening Management' },
        },
      ],
    },

    // ==================== 404 ====================
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { public: true },
    },
  ],
})

// ==================== GLOBAL NAVIGATION GUARD ====================
router.beforeEach((to, from, next) => {
  console.log('🧭 Navigation:', { from: from.path, to: to.path })
  next()
})

export default router
