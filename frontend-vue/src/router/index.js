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
          name: 'user-grammar',
          component: () => import('@/views/user/grammar/GrammarTopicsView.vue'),
          meta: { title: 'Grammar Topics' },
        },
        {
          path: 'grammar/lesson/:lessonId',
          name: 'user-grammar-lesson',
          component: () => import('@/views/user/grammar/LessonPlayerView.vue'),
          meta: { title: 'Grammar Lesson' },
        },
        {
          path: 'reading',
          name: 'user-reading',
          component: () => import('@/views/user/reading/ReadingPlayerView.vue'),
          meta: { title: 'Reading Lessons' },
        },
        {
          path: 'reading/lesson/:lessonId',
          name: 'user-reading-detail',
          component: () => import('@/views/user/reading/ReadingPlayerView.vue'),
          meta: { title: 'Reading Player' },
          props: true,
        },
        {
          path: 'listening',
          name: 'user-listening',
          component: () => import('@/views/user/listening/ListeningPlayerView.vue'),
          meta: { title: 'Listening Lessons' },
        },
        {
          path: 'listening/lesson/:lessonId',
          name: 'user-listening-player',
          component: () => import('@/views/user/listening/ListeningPlayerView.vue'),
          props: true,
          meta: { title: 'Listening Player' },
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
        if (adminToken) {
          return next('/admin/dashboard')
        }
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
          component: () => import('@/views/admin/UsersManagementView.vue'),
          meta: { title: 'Users Management' },
        },
        {
          path: 'grammar',
          name: 'admin-grammar',
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
          path: 'listening',
          name: 'admin-listening',
          component: () => import('@/views/admin/ListeningManagementView.vue'),
          meta: { title: 'Listening Management' },
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
