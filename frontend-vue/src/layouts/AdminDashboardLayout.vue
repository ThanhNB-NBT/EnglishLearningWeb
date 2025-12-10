<template>
  <div class="min-h-screen flex w-full bg-gray-100 dark:bg-[#0f0f0f] transition-colors duration-300 font-sans">

    <div v-if="isMobileMenuOpen" class="fixed inset-0 bg-black/50 z-40 lg:hidden backdrop-blur-sm"
      @click="isMobileMenuOpen = false"></div>

    <aside
      class="fixed lg:static inset-y-0 left-0 z-50 bg-white dark:bg-[#1a1a1a] border-r border-solid border-gray-300 dark:border-gray-700 flex flex-col transition-all duration-300 ease-in-out shadow-sm"
      :class="[
        isCollapsed ? 'lg:w-[70px]' : 'lg:w-64',
        isMobileMenuOpen ? 'translate-x-0 w-64' : '-translate-x-full lg:translate-x-0'
      ]">
      <div
        class="h-16 flex items-center justify-center border-b border-solid border-gray-300 dark:border-gray-700 shrink-0 bg-white dark:bg-[#1a1a1a]">
        <div class="flex items-center gap-3 overflow-hidden px-2">
          <div class="w-9 h-9 rounded-lg bg-blue-600 flex items-center justify-center text-white shrink-0 shadow-md">
            <span class="font-extrabold text-lg">E</span>
          </div>
          <span
            class="text-lg font-bold text-gray-800 dark:text-white whitespace-nowrap transition-opacity duration-200"
            :class="{ 'opacity-0 w-0': isCollapsed && !isMobileMenuOpen }">
            Admin Panel
          </span>
        </div>
      </div>

      <el-scrollbar class="flex-1 py-4 bg-white dark:bg-[#1a1a1a]">
        <el-menu :default-active="$route.path" :collapse="isCollapsed && !isMobileMenuOpen" :collapse-transition="false"
          router class="!border-none bg-transparent space-y-1">
          <el-menu-item index="/admin/dashboard" class="menu-item-custom hover-left-bar">
            <el-icon>
              <Odometer />
            </el-icon>
            <template #title>Dashboard</template>
          </el-menu-item>
          <el-menu-item index="/admin/users" class="menu-item-custom hover-left-bar">
            <el-icon>
              <User />
            </el-icon>
            <template #title>Quản lý User</template>
          </el-menu-item>
          <el-menu-item index="/admin/grammar" class="menu-item-custom hover-left-bar">
            <el-icon>
              <Reading />
            </el-icon>
            <template #title>QL Ngữ pháp</template>
          </el-menu-item>
          <el-menu-item index="/admin/reading" class="menu-item-custom hover-left-bar">
            <el-icon>
              <Document />
            </el-icon>
            <template #title>QL Bài đọc</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>

      <div class="p-3 border-t border-solid border-gray-300 dark:border-gray-700 bg-gray-50 dark:bg-[#1f1f1f]">
        <button @click="toggleSidebar"
          class="w-full p-2 rounded-lg text-gray-600 dark:text-gray-300 bg-white dark:bg-[#2c2c2c] border border-solid border-gray-300 dark:border-gray-500 hover:border-blue-500 hover:text-blue-600 dark:hover:text-blue-400 transition-all flex items-center justify-center shadow-sm">
          <el-icon :size="20" class="transition-transform duration-300" :class="{ 'rotate-180': isCollapsed }">
            <Fold />
          </el-icon>
        </button>
      </div>
    </aside>

    <div class="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
      <header
        class="h-16 bg-white dark:bg-[#1a1a1a] border-b border-solid border-gray-300 dark:border-gray-700 flex items-center justify-between px-4 lg:px-8 shrink-0 z-10 shadow-sm">

        <div class="flex items-center gap-4 flex-1">
          <button @click="isMobileMenuOpen = !isMobileMenuOpen"
            class="lg:hidden p-2 -ml-2 rounded-lg hover:bg-gray-100 dark:hover:bg-[#2c2c2c] text-gray-600 dark:text-gray-200">
            <el-icon :size="24">
              <Menu />
            </el-icon>
          </button>

          <el-breadcrumb separator="/" class="hidden sm:flex">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">
              <span class="text-gray-500 dark:text-gray-400 hover:text-blue-600 cursor-pointer">Dashboard</span>
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="$route.path !== '/admin/dashboard'">
              <span class="font-semibold text-gray-900 dark:text-gray-100">{{ pageTitle }}</span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="flex items-center gap-3">
          <button @click="toggleTheme"
            class="w-10 h-10 flex items-center justify-center rounded-full bg-gray-100 dark:bg-[#2c2c2c] border border-solid border-gray-300 dark:border-gray-500 text-gray-600 dark:text-gray-300 hover:border-blue-400 transition-colors">
            <el-icon :size="20">
              <component :is="isDark ? Moon : Sunny" />
            </el-icon>
          </button>

          <el-dropdown trigger="click" @command="handleCommand">
            <div
              class="flex items-center gap-3 cursor-pointer pl-2 py-1 pr-1 rounded-full hover:bg-gray-100 dark:hover:bg-[#2c2c2c] border border-solid border-transparent hover:border-gray-300 dark:hover:border-gray-500 transition-all">
              <el-avatar :size="34"
                class="!bg-gradient-to-br from-gray-700 to-gray-900 text-white font-bold text-sm shadow-sm border border-solid border-gray-300 dark:border-gray-500">
                {{ adminInitial }}
              </el-avatar>
              <div class="hidden md:block text-right pr-2">
                <p class="text-sm font-bold text-gray-800 dark:text-gray-100 leading-none mb-0.5">{{
                  currentAdmin?.username
                  }}</p>
                <p class="text-[10px] text-gray-500 dark:text-gray-400 uppercase font-bold tracking-wider">Admin</p>
              </div>
              <el-icon class="text-gray-500 dark:text-gray-400">
                <ArrowDown />
              </el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu
                class="min-w-[200px] !p-2 border border-solid border-gray-300 dark:border-gray-600 bg-white dark:bg-[#1f1f1f]">
                <el-dropdown-item command="profile"
                  class="!rounded-md !mb-1 gap-2 dark:text-gray-300 dark:hover:bg-[#333]"><el-icon>
                    <User />
                  </el-icon> Thông tin</el-dropdown-item>
                <el-dropdown-item command="change-password"
                  class="!rounded-md !mb-1 gap-2 dark:text-gray-300 dark:hover:bg-[#333]"><el-icon>
                    <Lock />
                  </el-icon> Đổi mật khẩu</el-dropdown-item>
                <div class="h-px bg-gray-300 dark:bg-gray-600 my-1"></div>
                <el-dropdown-item command="logout"
                  class="!text-red-500 hover:!bg-red-50 dark:hover:!bg-red-900/20 !rounded-md gap-2"><el-icon>
                    <SwitchButton />
                  </el-icon> Đăng xuất</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="flex-1 overflow-x-hidden overflow-y-auto bg-gray-100 dark:bg-[#0f0f0f]">
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
import { Odometer, User, Reading, Document, Fold, Moon, Sunny, ArrowDown, Lock, SwitchButton, Menu } from '@element-plus/icons-vue'

const router = useRouter(); const route = useRoute()
const authStore = useAuthStore(); const { isDark, toggleTheme } = useDarkMode()
const isCollapsed = ref(false); const isMobileMenuOpen = ref(false)

const currentAdmin = computed(() => authStore.currentAdmin)
const adminInitial = computed(() => currentAdmin.value?.username?.charAt(0).toUpperCase() || 'A')

const pageTitle = computed(() => {
  const map = {
    '/admin/users': 'Quản lý Người dùng',
    '/admin/grammar': 'Quản lý Ngữ pháp',
    '/admin/reading': 'Quản lý Bài đọc',
    '/admin/profile': 'Thông tin cá nhân',
    '/admin/change-password': 'Đổi mật khẩu'
  }
  if (route.path.startsWith('/admin/grammar')) return 'Quản lý Ngữ pháp'
  return map[route.path] || 'Trang quản trị'
})

const toggleSidebar = () => isCollapsed.value = !isCollapsed.value
const handleCommand = (cmd) => {
  if (cmd === 'logout') { authStore.logoutAdmin(); router.push('/admin/login') }
  else router.push('/admin/' + cmd)
}
</script>

<style scoped>
/* CUSTOM MENU ITEM STYLE */
:deep(.menu-item-custom) {
  height: 48px;
  line-height: 48px;
  margin: 4px 0;
  border-radius: 8px;
  /* Bo góc để vạch màu khớp với item */
  color: #4b5563;
  /* text-gray-600 */
  padding-left: 20px !important;
  /* Quan trọng: Để hiệu ứng ::before hoạt động đúng vị trí tương đối */
  position: relative;
  overflow: hidden;
}

html.dark :deep(.menu-item-custom) {
  color: #d1d5db;
  /* text-gray-300 */
}

/* Active State - Chữ đậm, màu xanh, nền nhạt */
:deep(.el-menu-item.is-active) {
  background-color: #eff6ff !important;
  color: #2563eb !important;
  font-weight: 700;
}

html.dark :deep(.el-menu-item.is-active) {
  background-color: rgba(37, 99, 235, 0.15) !important;
  color: #60a5fa !important;
}

:deep(.el-menu-item:hover) {
  /* Để background trong suốt để hiển thị màu của hover-left-bar */
  background-color: transparent !important;
}

:deep(.el-menu-item [class^=el-icon]) {
  margin-right: 12px;
  font-size: 20px;
  /* Đảm bảo icon nằm trên lớp nền hover */
  position: relative;
  z-index: 20;
}

:deep(.el-menu-item span) {
  position: relative;
  z-index: 20;
}
</style>
