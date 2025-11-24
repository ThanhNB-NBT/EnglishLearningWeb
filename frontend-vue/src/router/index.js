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
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
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
          return next('/user/dashboard')
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
      component: () => import('@/layouts/DashboardLayout.vue'),
      beforeEnter: authGuard,
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/user/DashboardView.vue'),
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue'), // ✅ SHARED
        },
        {
          path: 'change-password',
          name: 'change-password',
          component: () => import('@/views/ChangePasswordView.vue'), // ✅ SHARED
        },
        {
          path: 'grammar',
          name: 'grammar',
          component: () => import('@/views/user/GrammarView.vue'),
        },
        {
          path: 'reading',
          name: 'reading',
          component: () => import('@/views/user/ReadingView.vue'),
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
        },
        {
          path: 'profile',
          name: 'admin-profile',
          component: () => import('@/views/ProfileView.vue'), // ✅ SHARED
        },
        {
          path: 'change-password',
          name: 'admin-change-password',
          component: () => import('@/views/ChangePasswordView.vue'), // ✅ SHARED
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/UsersManagementView.vue'),
        },
        {
          path: 'grammar',
          name: 'admin-grammar',
          // redirect: '/admin/grammar', // Redirect to topics by default
          component: () => import('@/views/admin/GrammarManagementView.vue'),
          // children: [
          //   {
          //     path: 'topics',
          //     name: 'admin-grammar-topics',
          //     component: () => import('@/views/admin/grammar/TopicManagementView.vue'),
          //   },
          //   {
          //     path: 'topics/:topicId/lessons',
          //     name: 'admin-grammar-lessons',
          //     component: () => import('@/views/admin/grammar/LessonManagementView.vue'),
          //     props: true, // Pass topicId as prop
          //   },
          //   {
          //     path: 'lessons/:lessonId/questions',
          //     name: 'admin-grammar-questions',
          //     component: () => import('@/views/admin/grammar/QuestionManagementView.vue'),
          //     props: true, // Pass lessonId as prop
          //   },
          // ],
        },
        {
          path: 'reading',
          name: 'admin-reading',
          component: () => import('@/views/admin/ReadingManagementView.vue'),
        },
        {
          path: 'cleanup-user',
          name: 'admin-cleanup-user',
          component: () => import('@/views/admin/CleanupManagementView.vue'),
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
