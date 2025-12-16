<template>
  <div class="min-h-screen flex w-full bg-gray-50 dark:bg-[#141414] transition-colors duration-300 font-sans">

    <div v-if="isMobileMenuOpen" class="fixed inset-0 bg-black/50 z-40 lg:hidden" @click="closeMobileMenu"></div>

    <aside
      class="fixed lg:static inset-y-0 left-0 z-50 bg-white dark:bg-[#1d1d1d] border-r border-gray-400 dark:border-gray-600 flex flex-col transition-all duration-300 ease-in-out shadow-sm"
      :class="[isCollapsed ? 'lg:w-[72px]' : 'lg:w-56', isMobileMenuOpen ? 'translate-x-0 w-64' : '-translate-x-full lg:translate-x-0']">
      <div
        class="h-16 flex items-center justify-center border-b border-gray-600 dark:border-gray-400 px-4 bg-white dark:bg-[#1d1d1d]">
        <div class="flex items-center gap-3 overflow-hidden w-full">
          <div class="w-9 h-9 rounded-lg bg-indigo-600 flex items-center justify-center text-white shrink-0 shadow-md">
            <span class="font-bold text-lg">E</span>
          </div>
          <span
            class="text-lg font-bold text-gray-800 dark:text-white whitespace-nowrap transition-opacity duration-200"
            :class="{ 'opacity-0': isCollapsed && !isMobileMenuOpen }">
            English App
          </span>
        </div>
      </div>

      <el-scrollbar class="flex-1 py-4 bg-white dark:bg-[#1d1d1d]">
        <el-menu :default-active="$route.path" :collapse="isCollapsed && !isMobileMenuOpen" router
          class="!border-none bg-transparent px-0 space-y-1">

          <el-menu-item index="/user/home" class="menu-item-user hover-left-bar">
            <el-icon>
              <Odometer />
            </el-icon> <template #title>Tổng quan</template>
          </el-menu-item>
          <el-menu-item index="/user/grammar" class="menu-item-user hover-left-bar">
            <el-icon>
              <Reading />
            </el-icon> <template #title>Ngữ pháp</template>
          </el-menu-item>
          <el-menu-item index="/user/reading" class="menu-item-user hover-left-bar">
            <el-icon>
              <Document />
            </el-icon> <template #title>Đọc hiểu</template>
          </el-menu-item>
          <el-menu-item index="/user/listening" class="menu-item-user hover-left-bar">
            <el-icon>
              <Microphone />
            </el-icon> <template #title>Nghe hiểu</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>

      <div class="p-3 border-t border-solid border-gray-300 dark:border-gray-700 bg-gray-50 dark:bg-[#1f1f1f]">
        <button @click="toggleSidebar"
          class="hidden lg:flex w-full p-2 rounded-lg text-gray-600 dark:text-gray-300 bg-white dark:bg-[#2c2c2c] border border-solid border-gray-300 dark:border-gray-500 hover:text-indigo-600 hover:border-indigo-500 dark:hover:text-indigo-400 transition-all justify-center shadow-sm">
          <el-icon :size="20">
            <component :is="isCollapsed ? 'Expand' : 'Fold'" />
          </el-icon>
        </button>
      </div>
    </aside>

    <div class="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
      <header
        class="h-16 bg-white dark:bg-[#1d1d1d] border-b border-gray-400 dark:border-gray-600 flex items-center justify-between px-4 lg:px-8 shrink-0 z-10 shadow-sm">
        <div class="flex items-center gap-4">
          <button @click="isMobileMenuOpen = !isMobileMenuOpen" class="lg:hidden text-gray-600 dark:text-gray-300">
            <el-icon :size="24">
              <Menu />
            </el-icon>
          </button>

          <el-breadcrumb separator="/" class="hidden sm:flex">
            <el-breadcrumb-item :to="{ path: '/user/home' }">
              <span class="text-gray-500 dark:text-gray-400 hover:text-indigo-600">Trang chủ</span>
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="$route.path !== '/user/home'">
              <span class="font-bold text-gray-900 dark:text-gray-100">{{ pageTitle }}</span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="flex items-center gap-4">
          <button @click="toggleTheme"
            class="w-10 h-10 flex items-center justify-center rounded-full bg-gray-100 dark:bg-[#2c2c2c] border border-gray-300 dark:border-gray-500 text-gray-600 dark:text-gray-300 hover:border-gray-400 transition-colors">
            <el-icon :size="20">
              <component :is="isDark ? Moon : Sunny" />
            </el-icon>
          </button>

          <el-dropdown @command="handleCommand" trigger="click">
            <div
              class="flex items-center gap-3 cursor-pointer pl-1 pr-1 py-1 rounded-full hover:bg-gray-100 dark:hover:bg-[#2c2c2c] transition-colors border border-transparent hover:border-gray-300 dark:hover:border-gray-500">
              <el-avatar :size="36"
                class="!bg-indigo-600 text-white font-bold border-2 border-gray-300 dark:border-gray-500 shadow-sm">
                {{ userInitial }}
              </el-avatar>
            </div>
            <template #dropdown>
              <el-dropdown-menu
                class="min-w-[180px] !p-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-[#1f1f1f]">
                <div class="px-3 py-2 mb-1 border-b border-gray-300 dark:border-gray-600">
                  <p class="font-bold text-gray-800 dark:text-gray-100 truncate">{{ currentUser?.username }}</p>
                  <p class="text-xs text-gray-500 dark:text-gray-400">Học viên</p>
                </div>
                <el-dropdown-item command="profile"
                  class="!rounded-md gap-2 dark:text-gray-300 dark:hover:bg-[#333]"><el-icon>
                    <User />
                  </el-icon> Hồ sơ</el-dropdown-item>
                <el-dropdown-item command="change-password"
                  class="!rounded-md gap-2 dark:text-gray-300 dark:hover:bg-[#333]"><el-icon>
                    <Lock />
                  </el-icon> Mật khẩu</el-dropdown-item>
                <el-dropdown-item command="logout"
                  class="!text-red-500 !rounded-md gap-2 dark:hover:bg-red-900/20"><el-icon>
                    <SwitchButton />
                  </el-icon> Đăng xuất</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="flex-1 overflow-x-hidden overflow-y-auto bg-gray-50 dark:bg-[#141414]">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDarkMode } from '@/composables/useDarkMode'
import { Odometer, Reading, Document, Moon, Sunny, User, Lock, SwitchButton, Menu } from '@element-plus/icons-vue'

const router = useRouter(); const route = useRoute()
const authStore = useAuthStore(); const { isDark, toggleTheme } = useDarkMode()
const isCollapsed = ref(false); const isMobileMenuOpen = ref(false)

const currentUser = computed(() => authStore.currentUser)
const userInitial = computed(() => currentUser.value?.username?.charAt(0).toUpperCase() || 'U')

const pageTitle = computed(() => {
  const map = {
    '/user/grammar': 'Ngữ pháp',
    '/user/reading': 'Đọc hiểu',
    '/user/profile': 'Hồ sơ cá nhân',
    '/user/change-password': 'Đổi mật khẩu',
    '/user/home': 'Tổng quan',
    '/user/listening': 'Nghe hiểu'
  }
  if (route.path.includes('/lesson/')) return 'Bài học'
  return map[route.path] || ''
})

const toggleSidebar = () => isCollapsed.value = !isCollapsed.value
const closeMobileMenu = () => isMobileMenuOpen.value = false
const handleCommand = (cmd) => {
  if (cmd === 'logout') { authStore.logout(); router.push('/') }
  else router.push('/user/' + cmd)
}
</script>

<style scoped>
:deep(.menu-item-user) {
  height: 48px;
  line-height: 48px;
  margin: 4px 0;
  border-left: 4px solid transparent;
  color: #64748b;
  padding-left: 20px !important;
}

html.dark :deep(.menu-item-user) {
  color: #d1d5db;
}

:deep(.el-menu-item.is-active) {
  background-color: #e0e7ff !important;
  border-left-color: #4f46e5 !important;
  color: #4f46e5 !important;
  font-weight: 700;
}

html.dark :deep(.el-menu-item.is-active) {
  background-color: rgba(79, 70, 229, 0.15) !important;
  border-left-color: #6366f1 !important;
  color: #818cf8 !important;
}

:deep(.el-menu-item:hover) {
  background-color: #f8fafc !important;
}

html.dark :deep(.el-menu-item:hover) {
  background-color: #262626 !important;
}

:deep(.el-menu-item [class^=el-icon]) {
  margin-right: 12px;
  font-size: 20px;
}
</style>
