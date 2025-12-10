<template>
  <div class="w-full max-w-7xl mx-auto p-8">
    <div
      class="bg-gradient-to-r from-blue-600 to-indigo-700 rounded-2xl p-8 mb-8 text-white shadow-lg shadow-blue-500/20">
      <h1 class="text-3xl font-bold mb-2">Chào mừng trở lại, {{ user?.fullName || user?.username }}! 👋</h1>
      <p class="text-blue-100 text-lg opacity-90">Hãy tiếp tục hành trình chinh phục tiếng Anh của bạn.</p>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
      <div v-for="(stat, idx) in stats" :key="idx"
        class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border border-gray-100 dark:border-[#333] shadow-sm hover:shadow-md transition-all flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl flex items-center justify-center text-2xl" :class="stat.bgClass">
          <el-icon :color="stat.iconColor">
            <component :is="stat.icon" />
          </el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ stat.value }}</div>
          <div class="text-sm text-gray-500 dark:text-gray-400">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <h2 class="text-xl font-bold text-gray-800 dark:text-white mb-6">Khóa học của bạn</h2>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-10">
      <div
        class="bg-white dark:bg-[#1d1d1d] p-6 rounded-2xl border border-gray-100 dark:border-[#333] shadow-sm hover:shadow-lg transition-all group cursor-pointer"
        @click="$router.push('/user/grammar')">
        <div class="flex items-start justify-between mb-6">
          <div
            class="w-14 h-14 bg-blue-50 dark:bg-blue-900/20 rounded-xl flex items-center justify-center text-blue-500 group-hover:scale-110 transition-transform">
            <el-icon :size="30">
              <Reading />
            </el-icon>
          </div>
          <div
            class="bg-gray-100 dark:bg-[#333] px-3 py-1 rounded-full text-xs font-bold text-gray-600 dark:text-gray-300">
            0% Hoàn thành
          </div>
        </div>
        <h3 class="text-xl font-bold text-gray-900 dark:text-white mb-2 group-hover:text-blue-600 transition-colors">Ngữ
          pháp</h3>
        <p class="text-gray-500 dark:text-gray-400 mb-6">Lộ trình học ngữ pháp từ cơ bản đến nâng cao.</p>
        <el-button type="primary" class="w-full !h-10 !font-bold !rounded-lg">Tiếp tục học</el-button>
      </div>

      <div
        class="bg-white dark:bg-[#1d1d1d] p-6 rounded-2xl border border-gray-100 dark:border-[#333] shadow-sm hover:shadow-lg transition-all group cursor-pointer"
        @click="$router.push('/user/reading')">
        <div class="flex items-start justify-between mb-6">
          <div
            class="w-14 h-14 bg-green-50 dark:bg-green-900/20 rounded-xl flex items-center justify-center text-green-500 group-hover:scale-110 transition-transform">
            <el-icon :size="30">
              <Document />
            </el-icon>
          </div>
          <div
            class="bg-gray-100 dark:bg-[#333] px-3 py-1 rounded-full text-xs font-bold text-gray-600 dark:text-gray-300">
            0% Hoàn thành
          </div>
        </div>
        <h3 class="text-xl font-bold text-gray-900 dark:text-white mb-2 group-hover:text-green-600 transition-colors">
          Đọc hiểu</h3>
        <p class="text-gray-500 dark:text-gray-400 mb-6">Rèn luyện kỹ năng đọc hiểu qua các bài văn đa dạng.</p>
        <el-button type="success" class="w-full !h-10 !font-bold !rounded-lg">Tiếp tục học</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { Calendar, Medal, Reading, Trophy, Document } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const user = computed(() => authStore.currentUser)

const stats = computed(() => [
  { label: 'Ngày liên tiếp', value: user.value?.streakDays || 0, icon: Calendar, iconColor: '#ff9800', bgClass: 'bg-orange-50 dark:bg-orange-900/20' },
  { label: 'Tổng điểm', value: user.value?.totalPoints || 0, icon: Medal, iconColor: '#2196f3', bgClass: 'bg-blue-50 dark:bg-blue-900/20' },
  { label: 'Bài học xong', value: 0, icon: Reading, iconColor: '#9c27b0', bgClass: 'bg-purple-50 dark:bg-purple-900/20' },
  { label: 'Thành tích', value: 0, icon: Trophy, iconColor: '#4caf50', bgClass: 'bg-green-50 dark:bg-green-900/20' },
])
</script>
