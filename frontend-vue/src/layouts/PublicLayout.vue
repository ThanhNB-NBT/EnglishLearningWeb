<template>
  <div class="min-h-screen flex flex-col bg-gray-50 dark:bg-[#141414] transition-colors duration-300">

    <header class="sticky top-0 z-50 bg-white/80 dark:bg-[#1d1d1d]/80 backdrop-blur-md border-b border-gray-200 dark:border-[#333]">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">

        <router-link to="/" class="flex items-center gap-2 no-underline">
          <span class="text-xl font-bold bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">
            English Learning
          </span>
        </router-link>

        <nav class="hidden md:flex items-center gap-8">
          <router-link to="/" class="text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
            Trang chủ
          </router-link>
          <router-link to="/grammar" class="text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
            Ngữ pháp
          </router-link>
          <router-link to="/reading" class="text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
            Đọc hiểu
          </router-link>
        </nav>

        <div class="flex items-center gap-4">
          <template v-if="!isLoggedIn">
            <div class="hidden sm:flex gap-3">
              <el-button @click="$router.push('/auth/login')" class="!rounded-lg font-medium">
                Đăng nhập
              </el-button>
              <el-button type="primary" @click="$router.push('/auth/register')" class="!rounded-lg font-bold shadow-md shadow-blue-500/20">
                Đăng ký
              </el-button>
            </div>
          </template>

          <template v-else>
            <el-dropdown @command="handleCommand">
              <div class="flex items-center gap-2 cursor-pointer hover:bg-gray-100 dark:hover:bg-[#2c2c2c] p-1 pr-2 rounded-full transition-colors">
                <el-avatar :size="32" class="!bg-gradient-to-br from-blue-500 to-indigo-600 text-white font-bold !text-sm">
                  {{ userInitial }}
                </el-avatar>
                <el-icon class="text-gray-400"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu class="min-w-[180px]">
                  <div class="px-4 py-2 border-b dark:border-gray-700 mb-1">
                    <p class="font-bold text-gray-800 dark:text-white truncate">{{ currentUser?.username }}</p>
                    <p class="text-xs text-gray-500 truncate">{{ currentUser?.email }}</p>
                  </div>
                  <el-dropdown-item command="dashboard" class="gap-2"><el-icon><Odometer /></el-icon> Dashboard</el-dropdown-item>
                  <el-dropdown-item command="profile" class="gap-2"><el-icon><User /></el-icon> Hồ sơ</el-dropdown-item>
                  <el-dropdown-item divided command="logout" class="text-red-500 gap-2"><el-icon><SwitchButton /></el-icon> Đăng xuất</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>

          <button class="md:hidden p-2 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-[#2c2c2c]" @click="drawerVisible = true">
            <el-icon :size="24"><Menu /></el-icon>
          </button>
        </div>
      </div>
    </header>

    <main class="flex-1 w-full">
      <router-view />
    </main>

    <footer class="bg-white dark:bg-[#1d1d1d] border-t border-gray-200 dark:border-[#333] py-8">
      <div class="max-w-7xl mx-auto px-4 text-center text-sm text-gray-500 dark:text-gray-400">
        <p>&copy; 2025 English Learning Platform. All rights reserved.</p>
      </div>
    </footer>

    <el-drawer v-model="drawerVisible" title="Menu" direction="ltr" size="80%" class="!rounded-r-2xl">
      <div class="flex flex-col gap-2">
        <router-link to="/" class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-100 dark:hover:bg-[#2c2c2c] text-gray-700 dark:text-gray-200 no-underline" @click="drawerVisible = false">
          <el-icon><HomeFilled /></el-icon> Trang chủ
        </router-link>
        <router-link to="/grammar" class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-100 dark:hover:bg-[#2c2c2c] text-gray-700 dark:text-gray-200 no-underline" @click="drawerVisible = false">
          <el-icon><Reading /></el-icon> Ngữ pháp
        </router-link>
        <router-link to="/reading" class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-100 dark:hover:bg-[#2c2c2c] text-gray-700 dark:text-gray-200 no-underline" @click="drawerVisible = false">
          <el-icon><Document /></el-icon> Đọc hiểu
        </router-link>

        <div v-if="!isLoggedIn" class="mt-4 pt-4 border-t border-gray-100 dark:border-[#333] flex flex-col gap-3">
          <el-button @click="$router.push('/auth/login'); drawerVisible = false" class="w-full !rounded-lg !h-10">Đăng nhập</el-button>
          <el-button type="primary" @click="$router.push('/auth/register'); drawerVisible = false" class="w-full !rounded-lg !h-10 font-bold">Đăng ký</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Menu, ArrowDown, Odometer, User, SwitchButton, HomeFilled, Reading, Document } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const drawerVisible = ref(false)

const isLoggedIn = computed(() => authStore.isLoggedIn)
const currentUser = computed(() => authStore.currentUser)
const userInitial = computed(() => currentUser.value?.username?.charAt(0).toUpperCase() || 'U')

const handleCommand = (command) => {
  if (command === 'logout') handleLogout()
  else router.push('/' + (command === 'dashboard' ? 'user/dashboard' : 'user/profile'))
}

const handleLogout = async () => {
  await authStore.logout()
  router.push('/')
}
</script>
