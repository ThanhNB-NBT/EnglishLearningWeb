<template>
  <div class="ai-import-view h-full flex flex-col">
    <div class="mb-6 flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-800 dark:text-white flex items-center gap-3">
          <span class="p-2 bg-indigo-100 rounded-lg text-indigo-600">
            <el-icon size="24"><MagicStick /></el-icon>
          </span>
          AI Content Generator
        </h1>
        <p class="text-gray-500 mt-1 ml-12">
          Công cụ hỗ trợ soạn thảo bài học và trích xuất câu hỏi tự động từ tài liệu.
        </p>
      </div>
    </div>

    <div class="flex-1 min-h-0">
      <UnifiedAIImport />
    </div>
  </div>
</template>

<script setup>
import { MagicStick } from '@element-plus/icons-vue';
// Import component logic chính mà chúng ta đã viết
import UnifiedAIImport from '@/components/admin/shared/UnifiedAIImport.vue';
</script>
