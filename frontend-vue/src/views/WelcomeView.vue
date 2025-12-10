<template>
  <div class="min-h-screen bg-gray-50 dark:bg-dark-bg transition-colors duration-300">

    <section class="bg-gradient-to-br from-blue-600 via-blue-700 to-indigo-800 text-white py-24 md:py-32 px-4 text-center relative overflow-hidden">
      <div class="absolute top-0 left-0 w-64 h-64 bg-white/5 rounded-full -translate-x-1/2 -translate-y-1/2 blur-3xl"></div>
      <div class="absolute bottom-0 right-0 w-80 h-80 bg-blue-400/10 rounded-full translate-x-1/3 translate-y-1/3 blur-3xl"></div>

      <div class="relative z-10 max-w-4xl mx-auto">
        <h1 class="text-4xl md:text-6xl font-bold mb-6 tracking-tight drop-shadow-sm">
          English Learning Platform
        </h1>
        <p class="text-lg md:text-xl text-blue-100 mb-10 max-w-2xl mx-auto leading-relaxed">
          Nâng cao kỹ năng tiếng Anh của bạn với các bài học ngữ pháp, đọc hiểu và luyện nghe tương tác thông minh.
        </p>

        <div class="flex flex-col sm:flex-row justify-center gap-4">
          <template v-if="!isLoggedIn">
            <el-button
              type="primary"
              size="large"
              class="!h-12 !px-8 !text-base !font-bold !rounded-full shadow-lg shadow-blue-900/30 border-none"
              @click="$router.push('/auth/register')"
            >
              Bắt đầu miễn phí
            </el-button>
            <el-button
              size="large"
              class="!h-12 !px-8 !text-base !font-bold !rounded-full !bg-white/10 !text-white !border-white/30 hover:!bg-white/20"
              @click="$router.push('/auth/login')"
            >
              Đăng nhập
            </el-button>
          </template>
          <el-button
            v-else
            type="primary"
            size="large"
            class="!h-12 !px-8 !text-base !font-bold !rounded-full shadow-lg"
            @click="$router.push('/user/dashboard')"
          >
            Vào Dashboard
          </el-button>
        </div>
      </div>
    </section>

    <section class="py-20 px-4">
      <div class="max-w-6xl mx-auto">
        <h2 class="text-3xl font-bold text-center text-gray-800 dark:text-white mb-16 relative inline-block left-1/2 -translate-x-1/2">
          Tính năng nổi bật
          <span class="absolute bottom-[-10px] left-0 w-full h-1 bg-blue-500 rounded-full"></span>
        </h2>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div class="group bg-white dark:bg-[#1d1d1d] p-8 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-800 hover:shadow-xl hover:-translate-y-1 transition-all duration-300">
            <div class="w-14 h-14 bg-blue-50 dark:bg-blue-900/20 rounded-xl flex items-center justify-center mb-6 text-blue-500 group-hover:scale-110 transition-transform">
              <el-icon :size="32"><Reading /></el-icon>
            </div>
            <h3 class="text-xl font-bold text-gray-800 dark:text-white mb-3">Ngữ pháp</h3>
            <p class="text-gray-600 dark:text-gray-400 leading-relaxed">
              Hệ thống bài học ngữ pháp từ cơ bản đến nâng cao với ví dụ minh họa và bài tập thực hành.
            </p>
          </div>

          <div class="group bg-white dark:bg-[#1d1d1d] p-8 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-800 hover:shadow-xl hover:-translate-y-1 transition-all duration-300">
            <div class="w-14 h-14 bg-green-50 dark:bg-green-900/20 rounded-xl flex items-center justify-center mb-6 text-green-500 group-hover:scale-110 transition-transform">
              <el-icon :size="32"><Document /></el-icon>
            </div>
            <h3 class="text-xl font-bold text-gray-800 dark:text-white mb-3">Đọc hiểu</h3>
            <p class="text-gray-600 dark:text-gray-400 leading-relaxed">
              Cải thiện kỹ năng đọc với các đoạn văn đa dạng chủ đề và bộ câu hỏi kiểm tra thông minh.
            </p>
          </div>

          <div class="group bg-white dark:bg-[#1d1d1d] p-8 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-800 hover:shadow-xl hover:-translate-y-1 transition-all duration-300">
            <div class="w-14 h-14 bg-orange-50 dark:bg-orange-900/20 rounded-xl flex items-center justify-center mb-6 text-orange-500 group-hover:scale-110 transition-transform">
              <el-icon :size="32"><TrendCharts /></el-icon>
            </div>
            <h3 class="text-xl font-bold text-gray-800 dark:text-white mb-3">Tiến độ</h3>
            <p class="text-gray-600 dark:text-gray-400 leading-relaxed">
              Theo dõi sát sao quá trình học tập, thống kê điểm số và chuỗi ngày học liên tiếp (Streak).
            </p>
          </div>
        </div>
      </div>
    </section>

    <section class="py-20 bg-gray-100 dark:bg-[#1a1a1a] text-center px-4">
      <div class="max-w-3xl mx-auto">
        <h2 class="text-3xl font-bold text-gray-800 dark:text-white mb-4">Sẵn sàng bắt đầu chưa?</h2>
        <p class="text-gray-600 dark:text-gray-400 mb-8 text-lg">
          Tham gia cùng hàng nghìn học viên đang chinh phục tiếng Anh mỗi ngày.
        </p>
        <el-button
          v-if="!isLoggedIn"
          type="primary"
          size="large"
          class="!h-12 !px-10 !text-base !font-bold !rounded-full shadow-lg"
          @click="$router.push('/auth/register')"
        >
          Đăng ký miễn phí ngay
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { Reading, Document, TrendCharts } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isLoggedIn)
</script>
