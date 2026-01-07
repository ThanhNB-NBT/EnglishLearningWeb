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
      <AIAdvisorWidget />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { Calendar, Medal, Reading, Trophy} from '@element-plus/icons-vue'
import AIAdvisorWidget from '@/components/user/dashboard/AIAdvisorWidget.vue'
const authStore = useAuthStore()
const user = computed(() => authStore.currentUser)

// ✅ UPDATED: Access stats from nested object
const stats = computed(() => [
  {
    label: 'Ngày liên tiếp',
    value: user.value?.stats?.currentStreak || user.value?.streakDays || 0, // Fallback for old structure
    icon: Calendar,
    iconColor: '#ff9800',
    bgClass: 'bg-orange-50 dark:bg-orange-900/20',
  },
  {
    label: 'Tổng điểm',
    value: user.value?.stats?.totalPoints || user.value?.totalPoints || 0, // Fallback for old structure
    icon: Medal,
    iconColor: '#2196f3',
    bgClass: 'bg-blue-50 dark:bg-blue-900/20',
  },
  {
    label: 'Bài học xong',
    value: user.value?.stats?.totalLessonsCompleted || 0,
    icon: Reading,
    iconColor: '#9c27b0',
    bgClass: 'bg-purple-50 dark:bg-purple-900/20',
  },
  {
    label: 'Thành tích',
    value: 0, // Could be calculated from achievements system later
    icon: Trophy,
    iconColor: '#4caf50',
    bgClass: 'bg-green-50 dark:bg-green-900/20',
  },
])
</script>
