<template>
  <div class="min-h-screen w-full flex items-center justify-center p-4 bg-gradient-to-br from-[#667eea] to-[#764ba2]">

    <div class="w-full max-w-[450px]">
      <div class="text-center mb-6">
        <router-link to="/" class="inline-flex flex-col items-center group no-underline">
          <h1 class="text-4xl font-bold text-white m-0 drop-shadow-md transition-transform duration-300 group-hover:scale-105">
            English Learning
          </h1>
          <p class="text-blue-100 mt-2 text-sm opacity-90">Nền tảng học tiếng Anh trực tuyến</p>
        </router-link>
      </div>

      <el-card class="!rounded-xl !border-none shadow-2xl overflow-hidden" :body-style="{ padding: '40px' }">
        <router-view v-slot="{ Component }">
          <transition name="el-fade-in-linear" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-card>

      <div class="mt-6 text-center">
        <router-link to="/" class="inline-flex items-center gap-2 text-white/80 hover:text-white text-sm font-medium transition-colors no-underline">
          <el-icon><ArrowLeft /></el-icon> Quay về trang chủ
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ArrowLeft } from '@element-plus/icons-vue'
</script>
