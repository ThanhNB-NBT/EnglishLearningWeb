<template>
  <div class="max-w-7xl mx-auto p-5">
    <div class="bg-gradient-to-r from-gray-800 to-gray-900 rounded-2xl p-8 mb-8 text-white shadow-lg">
      <h1 class="text-3xl font-bold mb-2">Admin Dashboard 🛡️</h1>
      <p class="text-gray-300">Chào mừng {{ admin?.username }}, chúc bạn một ngày làm việc hiệu quả.</p>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
      <div v-for="(stat, idx) in stats" :key="idx"
           class="bg-white dark:bg-[#1d1d1d] p-5 rounded-xl border border-gray-100 dark:border-[#333] shadow-sm flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl flex items-center justify-center text-2xl" :class="stat.bgClass">
          <el-icon :color="stat.iconColor"><component :is="stat.icon" /></el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-900 dark:text-white">0</div>
          <div class="text-sm text-gray-500 dark:text-gray-400">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <h2 class="text-xl font-bold text-gray-800 dark:text-white mb-6">Quản lý nhanh</h2>
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div v-for="action in actions" :key="action.path"
           class="bg-white dark:bg-[#1d1d1d] p-6 rounded-2xl border border-gray-100 dark:border-[#333] shadow-sm hover:shadow-lg transition-all cursor-pointer group text-center"
           @click="$router.push(action.path)">
        <div class="w-16 h-16 mx-auto mb-4 rounded-full flex items-center justify-center text-3xl group-hover:scale-110 transition-transform" :class="action.bgClass">
          <el-icon :color="action.color"><component :is="action.icon" /></el-icon>
        </div>
        <h3 class="text-lg font-bold text-gray-900 dark:text-white mb-2">{{ action.title }}</h3>
        <p class="text-sm text-gray-500 dark:text-gray-400">{{ action.desc }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { User, Reading, Document, TrendCharts } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const admin = computed(() => authStore.currentAdmin)

const stats = [
  { label: 'Người dùng', icon: User, iconColor: '#2196f3', bgClass: 'bg-blue-50 dark:bg-blue-900/20' },
  { label: 'Bài ngữ pháp', icon: Reading, iconColor: '#9c27b0', bgClass: 'bg-purple-50 dark:bg-purple-900/20' },
  { label: 'Bài đọc hiểu', icon: Document, iconColor: '#4caf50', bgClass: 'bg-green-50 dark:bg-green-900/20' },
  { label: 'Lượt truy cập', icon: TrendCharts, iconColor: '#ff9800', bgClass: 'bg-orange-50 dark:bg-orange-900/20' },
]

const actions = [
  { title: 'Quản lý User', desc: 'Xem danh sách, khóa/mở khóa tài khoản', path: '/admin/users', icon: User, color: '#409eff', bgClass: 'bg-blue-50 dark:bg-blue-900/20' },
  { title: 'QL Ngữ pháp', desc: 'Thêm/Sửa/Xóa chủ đề và bài học', path: '/admin/grammar', icon: Reading, color: '#67c23a', bgClass: 'bg-green-50 dark:bg-green-900/20' },
  { title: 'QL Bài đọc', desc: 'Quản lý kho bài đọc hiểu', path: '/admin/reading', icon: Document, color: '#e6a23c', bgClass: 'bg-orange-50 dark:bg-orange-900/20' },
]
</script>
