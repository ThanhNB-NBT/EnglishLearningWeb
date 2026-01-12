<template>
  <div class="h-screen flex overflow-hidden bg-gray-50 dark:bg-[#0f0f0f]">
    <div
      v-if="isMobileMenuOpen"
      class="fixed inset-0 bg-black/50 z-40 lg:hidden"
      @click="closeMobileMenu"
    ></div>

    <aside
      :class="[
        'h-screen transition-all duration-300 ease-in-out',
        'bg-white dark:bg-[#1a1a1a] border-r border-gray-200 dark:border-gray-800',
        'flex flex-col shrink-0',
        'hidden lg:flex',
        isCollapsed ? 'w-[70px]' : 'w-64',
        'lg:relative fixed left-0 top-0 z-50',
        isMobileMenuOpen ? 'flex translate-x-0 w-64' : 'lg:flex -translate-x-full lg:translate-x-0',
      ]"
    >
      <div
        :class="[
          'h-16 flex items-center border-b border-gray-200 dark:border-gray-800 shrink-0',
          'bg-gradient-to-r transition-all duration-300 overflow-hidden',
          isTeacher ? 'from-green-600 to-green-700' : 'from-blue-600 to-purple-600',
          isTeacher ? 'dark:from-green-700 dark:to-green-800' : 'dark:from-blue-700 dark:to-purple-700',
          isCollapsed ? 'justify-center' : 'px-4'
        ]"
      >
        <div class="flex items-center min-w-max">
          <div
            class="w-9 h-9 rounded-lg flex items-center justify-center text-white bg-white/20 backdrop-blur-sm shrink-0"
          >
            <span class="font-extrabold text-lg">{{ isAdmin ? 'A' : 'T' }}</span>
          </div>
          <span
            :class="[
              'text-lg font-bold text-white whitespace-nowrap transition-all duration-300 ml-3',
              isCollapsed ? 'opacity-0 w-0' : 'opacity-100'
            ]"
          >
            {{ isAdmin ? 'Admin Panel' : 'Teacher Panel' }}
          </span>
        </div>
      </div>

      <div class="flex-1 overflow-y-auto overflow-x-hidden">
        <el-menu
          :default-active="$route.path"
          :collapse="isCollapsed"
          router
          class="!border-0 h-full"
          @select="handleMenuSelect"
        >
          <el-menu-item :index="dashboardPath" class="menu-item-custom">
            <el-icon><Odometer /></el-icon>
            <template #title>Dashboard</template>
          </el-menu-item>

          <template v-if="isAdmin">
            <el-menu-item index="/admin/users" class="menu-item-custom">
              <el-icon><User /></el-icon>
              <template #title>Quản lý User</template>
            </el-menu-item>
          </template>

          <el-menu-item :index="grammarPath" class="menu-item-custom">
            <el-icon><Reading /></el-icon>
            <template #title>QL Ngữ pháp</template>
          </el-menu-item>

          <el-menu-item :index="readingPath" class="menu-item-custom">
            <el-icon><Document /></el-icon>
            <template #title>QL Bài đọc</template>
          </el-menu-item>

          <el-menu-item :index="listeningPath" class="menu-item-custom">
            <el-icon><Microphone /></el-icon>
            <template #title>QL Nghe hiểu</template>
          </el-menu-item>
        </el-menu>
      </div>

      <div
        class="hidden lg:flex h-14 items-center justify-center border-t border-gray-200 dark:border-gray-800 shrink-0"
      >
        <el-button
          link
          @click="toggleSidebar"
          class="!text-gray-600 dark:!text-gray-400 hover:!text-blue-600 dark:hover:!text-blue-400 transition-colors"
        >
          <el-icon :size="20">
            <component :is="isCollapsed ? Expand : Fold" />
          </el-icon>
        </el-button>
      </div>
    </aside>

    <div class="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
      <header class="h-16 bg-white dark:bg-[#1a1a1a] border-b border-gray-200 dark:border-gray-800 flex items-center justify-between px-4 lg:px-8 shrink-0 z-30">
        <button
          class="lg:hidden w-10 h-10 rounded-lg flex items-center justify-center hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
          @click="toggleMobileMenu"
        >
          <el-icon :size="24" class="text-gray-700 dark:text-gray-300">
            <Menu />
          </el-icon>
        </button>

        <el-breadcrumb separator="/" class="hidden lg:block text-sm">
          <el-breadcrumb-item :to="{ path: dashboardPath }">
            <span class="text-gray-500 hover:text-blue-600 dark:text-gray-400 dark:hover:text-blue-400 transition-colors">
              Dashboard
            </span>
          </el-breadcrumb-item>
          <el-breadcrumb-item v-if="currentPageTitle && currentPageTitle !== 'Dashboard'">
            <span class="font-semibold text-gray-900 dark:text-white">{{ currentPageTitle }}</span>
          </el-breadcrumb-item>
        </el-breadcrumb>

        <h1 class="lg:hidden text-base font-bold text-gray-900 dark:text-white truncate">
          {{ currentPageTitle }}
        </h1>

        <div class="flex items-center gap-2 lg:gap-3">
          <button
            @click="toggleTheme"
            class="w-10 h-10 rounded-full bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700 flex items-center justify-center transition-colors"
          >
            <el-icon :size="20" class="text-gray-700 dark:text-gray-300">
              <component :is="isDark ? Moon : Sunny" />
            </el-icon>
          </button>

          <el-dropdown @command="handleCommand" trigger="click">
            <div
              class="flex items-center gap-2 cursor-pointer px-2 lg:px-3 py-1 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
            >
              <el-avatar
                :size="34"
                :src="userAvatar"
                class="bg-gray-200 border border-gray-300 dark:border-gray-600"
              />

              <div class="hidden md:block">
                <p class="text-sm font-bold text-gray-900 dark:text-white truncate max-w-[120px]">
                  {{ currentUser?.username || 'Unknown' }}
                </p>
                <p class="text-[10px] text-gray-500 dark:text-gray-400 uppercase font-bold">
                  {{ userRole }}
                </p>
              </div>
              <el-icon class="hidden md:block text-gray-500 dark:text-gray-400"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> Thông tin
                </el-dropdown-item>
                <el-dropdown-item command="change-password">
                  <el-icon><Lock /></el-icon> Đổi mật khẩu
                </el-dropdown-item>
                <el-divider class="!my-1" />
                <el-dropdown-item command="logout" class="!text-red-500">
                  <el-icon><SwitchButton /></el-icon> Đăng xuất
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="flex-1 overflow-y-auto overflow-x-hidden bg-gray-50 dark:bg-[#0f0f0f]">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDarkMode } from '@/composables/useDarkMode'
import { getAvatarUrl } from '@/utils/avatar'
import { ElMessage } from 'element-plus'
import {
  Odometer,
  User,
  Reading,
  Document,
  Microphone,
  Moon,
  Sunny,
  ArrowDown,
  Lock,
  SwitchButton,
  Fold,
  Expand,
  Menu,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { isDark, toggleTheme } = useDarkMode()

const isCollapsed = ref(false)
const isMobileMenuOpen = ref(false)

const toggleSidebar = () => isCollapsed.value = !isCollapsed.value
const toggleMobileMenu = () => isMobileMenuOpen.value = !isMobileMenuOpen.value
const closeMobileMenu = () => isMobileMenuOpen.value = false
const handleMenuSelect = () => closeMobileMenu()

watch(() => route.path, () => closeMobileMenu())

const isAdmin = computed(() => authStore.isAdminAuthenticated && authStore.admin?.role === 'ADMIN')
const isTeacher = computed(() => authStore.isTeacherAuthenticated && authStore.teacher?.role === 'TEACHER')

const currentUser = computed(() => {
  if (isAdmin.value) return authStore.admin
  if (isTeacher.value) return authStore.teacher
  return null
})

const userRole = computed(() => {
  if (isAdmin.value) return 'ADMIN'
  if (isTeacher.value) return 'TEACHER'
  return 'UNKNOWN'
})

// ✅ FIX: Computed lấy Avatar
const userAvatar = computed(() => {
  const name = currentUser.value?.fullName || currentUser.value?.username
  return getAvatarUrl(name)
})

// Dynamic paths
const dashboardPath = computed(() => isAdmin.value ? '/admin/dashboard' : '/teacher/dashboard')
const grammarPath = computed(() => isAdmin.value ? '/admin/grammar' : '/teacher/grammar')
const readingPath = computed(() => isAdmin.value ? '/admin/reading' : '/teacher/reading')
const listeningPath = computed(() => isAdmin.value ? '/admin/listening' : '/teacher/listening')

const currentPageTitle = computed(() => {
  const path = route.path
  const titleMap = {
    '/admin/dashboard': 'Dashboard',
    '/teacher/dashboard': 'Dashboard',
    '/admin/users': 'Quản lý User',
    '/admin/cleanup-user': 'Cleanup User',
    '/admin/grammar': 'Quản lý Ngữ pháp',
    '/admin/reading': 'Quản lý Bài đọc',
    '/admin/listening': 'Quản lý Nghe hiểu',
    '/teacher/grammar': 'Quản lý Ngữ pháp',
    '/teacher/reading': 'Quản lý Bài đọc',
    '/teacher/listening': 'Quản lý Nghe hiểu',
    '/admin/profile': 'Thông tin cá nhân',
    '/teacher/profile': 'Thông tin cá nhân',
    '/admin/change-password': 'Đổi mật khẩu',
    '/teacher/change-password': 'Đổi mật khẩu',
  }
  return titleMap[path] || 'Dashboard'
})

const handleCommand = async (cmd) => {
  if (cmd === 'logout') {
    try {
      const role = userRole.value
      await authStore.logout(role)
      ElMessage.success('Đã đăng xuất thành công')

      if (role === 'TEACHER') await router.push('/teacher/login')
      else if (role === 'ADMIN') await router.push('/admin/login')
      else await router.push('/')
    } catch (error) {
      console.error('Logout error:', error)
      authStore.clearLocalAuth && authStore.clearLocalAuth() // Fallback nếu có hàm này
      if (isTeacher.value) await router.push('/teacher/login')
      else await router.push('/admin/login')
    }
  } else {
    const prefix = isAdmin.value ? '/admin' : '/teacher'
    await router.push(`${prefix}/${cmd}`)
  }
}

onMounted(() => {
  if (route.path.startsWith('/admin') && !authStore.isAdminAuthenticated) authStore.checkAuth()
  if (route.path.startsWith('/teacher') && !authStore.isTeacherAuthenticated) authStore.checkAuth()
})
</script>

<style scoped>
/* Giữ nguyên Style cũ */
.overflow-y-auto::-webkit-scrollbar { width: 6px; }
.overflow-y-auto::-webkit-scrollbar-track { background: transparent; }
.overflow-y-auto::-webkit-scrollbar-thumb { background: rgba(156, 163, 175, 0.3); border-radius: 3px; }
.overflow-y-auto::-webkit-scrollbar-thumb:hover { background: rgba(156, 163, 175, 0.5); }
html.dark .overflow-y-auto::-webkit-scrollbar-thumb { background: rgba(75, 85, 99, 0.5); }
html.dark .overflow-y-auto::-webkit-scrollbar-thumb:hover { background: rgba(75, 85, 99, 0.7); }

:deep(.menu-item-custom) {
  height: 48px; line-height: 48px; margin: 4px 8px; border-radius: 8px;
  color: #4b5563; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
html.dark :deep(.menu-item-custom) { color: #d1d5db; }

:deep(.el-menu--collapse .menu-item-custom) {
  display: flex !important; align-items: center !important; justify-content: center !important;
  padding: 0 !important; margin: 4px 0 !important; width: 70px !important;
}
:deep(.el-menu--collapse .menu-item-custom .el-icon) { margin: 0 !important; }

:deep(.el-menu-item.is-active) {
  background-color: #eff6ff !important; color: #2563eb !important; font-weight: 600; transform: translateX(0);
}
html.dark :deep(.el-menu-item.is-active) {
  background-color: rgba(37, 99, 235, 0.15) !important; color: #60a5fa !important;
}

:deep(.el-menu-item:hover) { background-color: #f3f4f6 !important; transform: translateX(2px); }
html.dark :deep(.el-menu-item:hover) { background-color: rgba(255, 255, 255, 0.05) !important; }

:deep(.el-menu-item [class^='el-icon']) { margin-right: 12px; font-size: 20px; transition: transform 0.3s; }
:deep(.el-menu--collapse .el-menu-item [class^='el-icon']) { margin-right: 0; }
:deep(.el-menu--collapse) { width: 70px; }
:deep(.el-menu) { transition: width 0.3s; border-right: none; }
</style>
