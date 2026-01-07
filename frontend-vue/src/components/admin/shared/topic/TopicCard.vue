<template>
  <div
    class="group h-full bg-white dark:bg-[#1d1d1d] border border-gray-300 dark:border-gray-700 rounded-xl overflow-hidden shadow-sm hover:shadow-md hover:border-blue-400 dark:hover:border-blue-500 transition-all duration-200 flex flex-col relative"
  >
    <div class="h-12 px-4 flex items-center justify-between border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-[#252525]">
      <div
        class="text-xs font-bold px-2 py-1 rounded uppercase tracking-wide border"
        :class="getLevelClass(topic.levelRequired)"
      >
        {{ topic.levelRequired }}
      </div>

      <div @click.stop>
        <el-switch
          v-model="localActive"
          size="small"
          inline-prompt
          active-text="ON"
          inactive-text="OFF"
          style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
          @change="handleToggleActive"
        />
      </div>
    </div>

    <div class="p-5 flex-1 flex flex-col">
      <h3
        class="text-base font-bold text-gray-800 dark:text-white mb-2 line-clamp-1 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors"
        :title="topic.name"
      >
        {{ topic.name }}
      </h3>

      <p class="text-sm text-gray-500 dark:text-gray-400 mb-4 line-clamp-2 h-10 leading-relaxed">
        {{ topic.description || 'Chưa có mô tả.' }}
      </p>

      <div class="mt-auto flex items-center gap-4 text-xs text-gray-500 dark:text-gray-500 font-medium">
        <span class="flex items-center gap-1.5">
          <el-icon><Document /></el-icon> {{ topic.totalLessons || 0 }} bài
        </span>
        <span class="flex items-center gap-1.5">
          <el-icon><Timer /></el-icon> STT: {{ topic.orderIndex }}
        </span>
      </div>
    </div>

    <div class="px-4 py-3 bg-gray-50 dark:bg-[#252525] border-t border-gray-200 dark:border-gray-700 flex justify-between items-center">
      <el-button
        type="primary"
        link
        size="small"
        class="!font-bold"
        @click="$emit('view-lessons', topic)"
      >
        Quản lý bài học
      </el-button>

      <div class="flex gap-1">
        <el-tooltip content="Chỉnh sửa" placement="top" :hide-after="0">
          <el-button link type="primary" class="!px-2" @click="$emit('edit', topic)">
            <el-icon :size="16"><Edit /></el-icon>
          </el-button>
        </el-tooltip>

        <!-- ✅ Only show Delete button for ADMIN -->
        <el-tooltip v-if="isAdmin" content="Xóa" placement="top" :hide-after="0">
          <el-button link type="danger" class="!px-2" @click="$emit('delete', topic)">
            <el-icon :size="16"><Delete /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Document, Timer, Edit, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  topic: { type: Object, required: true },
  isAdmin: { type: Boolean, default: true }, // ✅ NEW: Role check
})

const emit = defineEmits(['edit', 'delete', 'view-lessons', 'toggle-active'])

const localActive = ref(false)

watch(() => props.topic?.isActive, (newVal) => {
  localActive.value = newVal
}, { immediate: true })

const handleToggleActive = (val) => {
  emit('toggle-active', { ...props.topic, isActive: val })
}

const getLevelClass = (level) => {
  const map = {
    'A1': 'bg-green-100 text-green-700 border-green-200 dark:bg-green-900/30 dark:text-green-400 dark:border-green-800',
    'A2': 'bg-blue-100 text-blue-700 border-blue-200 dark:bg-blue-900/30 dark:text-blue-400 dark:border-blue-800',
    'B1': 'bg-yellow-100 text-yellow-700 border-yellow-200 dark:bg-yellow-900/30 dark:text-yellow-400 dark:border-yellow-800',
    'B2': 'bg-orange-100 text-orange-700 border-orange-200 dark:bg-orange-900/30 dark:text-orange-400 dark:border-orange-800',
    'C1': 'bg-red-100 text-red-700 border-red-200 dark:bg-red-900/30 dark:text-red-400 dark:border-red-800',
    'C2': 'bg-purple-100 text-purple-700 border-purple-200 dark:bg-purple-900/30 dark:text-purple-400 dark:border-purple-800',
  }
  return map[level] || 'bg-gray-100 text-gray-700 border-gray-200'
}
</script>
