<template>
  <!-- ✅ UPGRADE SUCCESS -->
  <el-alert
    v-if="levelResult && levelResult.upgraded"
    type="success"
    :closable="false"
    class="mb-4 animate-fade-in"
  >
    <template #title>
      <div class="flex items-center gap-2">
        <el-icon :size="24"><Trophy /></el-icon>
        <span class="text-lg font-bold">{{ levelResult.message }}</span>
      </div>
    </template>
    <div v-if="levelResult.oldLevel && levelResult.newLevel" class="mt-2 text-sm">
      <p class="flex items-center gap-2">
        <span class="px-2 py-1 bg-gray-100 dark:bg-gray-700 rounded">
          {{ levelResult.oldLevel }}
        </span>
        <el-icon><Right /></el-icon>
        <span class="px-2 py-1 bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300 rounded font-bold">
          {{ levelResult.newLevel }}
        </span>
      </p>
    </div>
  </el-alert>

  <!-- ✅ PARTIAL COMPLETE (e.g., completed Grammar but need Reading/Listening) -->
  <el-alert
    v-else-if="levelResult && levelResult.partialComplete"
    type="info"
    :closable="false"
    class="mb-4"
  >
    <template #title>
      <div class="flex items-center gap-2">
        <el-icon :size="20"><InfoFilled /></el-icon>
        <span class="font-semibold">{{ levelResult.message }}</span>
      </div>
    </template>
  </el-alert>

  <!-- ✅ MAX LEVEL REACHED -->
  <el-alert
    v-else-if="levelResult && levelResult.maxLevelReached"
    type="warning"
    :closable="false"
    class="mb-4"
  >
    <template #title>
      <div class="flex items-center gap-2">
        <el-icon :size="20"><Medal /></el-icon>
        <span class="font-semibold">{{ levelResult.message }}</span>
      </div>
    </template>
  </el-alert>
</template>

<script setup>
import { Trophy, Right, InfoFilled, Medal } from '@element-plus/icons-vue'

defineProps({
  levelResult: {
    type: Object,
    default: null,
  },
})
</script>

<style scoped>
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in {
  animation: fade-in 0.5s ease-out;
}
</style>
