<!-- src/components/admin/users/dialogs/UserDetailDialog.vue -->
<template>
  <el-dialog
    v-model="visible"
    title="Thông tin chi tiết User"
    width="600px"
    align-center
    @close="handleClose"
  >
    <div v-if="user" class="p-2">
      <!-- User Header -->
      <div class="flex items-center gap-4 mb-6 pb-4 border-b">
        <el-avatar
          :size="80"
          class="! bg-gradient-to-br from-blue-500 to-purple-600 text-white ! text-3xl font-bold shadow-lg"
        >
          {{ user.username.charAt(0).toUpperCase() }}
        </el-avatar>
        <div class="flex-1">
          <h3 class="text-xl font-bold text-gray-900 dark:text-white">{{ user.fullName }}</h3>
          <p class="text-gray-500 dark:text-gray-400">@{{ user.username }}</p>
          <div class="flex gap-2 mt-2">
            <el-tag :type="getRoleType(user.role)" size="small">{{ user.role }}</el-tag>
            <el-tag
              :type="user.isVerified ? 'success' : 'warning'"
              size="small"
            >
              {{ user.isVerified ? '✓ Verified' : '⚠ Unverified' }}
            </el-tag>
            <el-tag
              :type="user.isActive ? 'success' : 'danger'"
              size="small"
            >
              {{ user.isActive ? '● Active' : '○ Inactive' }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- User Stats (NEW:  Using stats object) -->
      <div class="grid grid-cols-2 gap-4 mb-6">
        <div class="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg">
          <div class="text-sm text-gray-600 dark:text-gray-400">Tổng điểm</div>
          <div class="text-2xl font-bold text-blue-600 dark:text-blue-400">
            {{ user.stats?.totalPoints || user.totalPoints || 0 }}
          </div>
        </div>
        <div class="bg-orange-50 dark:bg-orange-900/20 p-4 rounded-lg">
          <div class="text-sm text-gray-600 dark:text-gray-400">Streak</div>
          <div class="text-2xl font-bold text-orange-600 dark:text-orange-400">
            🔥 {{ user.stats?.currentStreak || user.streakDays || 0 }}
          </div>
        </div>
        <div class="bg-green-50 dark:bg-green-900/20 p-4 rounded-lg">
          <div class="text-sm text-gray-600 dark:text-gray-400">Bài hoàn thành</div>
          <div class="text-2xl font-bold text-green-600 dark:text-green-400">
            {{ user.stats?.totalLessonsCompleted || 0 }}
          </div>
        </div>
        <div class="bg-purple-50 dark:bg-purple-900/20 p-4 rounded-lg">
          <div class="text-sm text-gray-600 dark:text-gray-400">Thời gian học</div>
          <div class="text-2xl font-bold text-purple-600 dark:text-purple-400">
            {{ formatStudyTime(user.stats?.totalStudyTimeMinutes || 0) }}
          </div>
        </div>
      </div>

      <!-- User Details -->
      <el-descriptions :column="1" border>
        <el-descriptions-item label="Email">
          <span class="font-mono text-sm">{{ user.email }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="English Level">
          <el-tag :type="getLevelType(user.englishLevel)" size="small">
            {{ user.englishLevel }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Ngày tham gia">
          {{ formatDate(user.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="Đăng nhập cuối">
          {{ formatDate(user.activity?.lastLoginDate || user.lastLoginDate) }}
        </el-descriptions-item>
        <el-descriptions-item label="Login Count">
          {{ user.activity?.loginCount || 0 }} lần
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <template #footer>
      <el-button @click="handleClose">Đóng</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import dayjs from 'dayjs'

const props = defineProps({
  modelValue: Boolean,
  user: Object,
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(props.modelValue)

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
  },
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const handleClose = () => {
  visible.value = false
}

const formatDate = (date) => {
  if (!date) return 'N/A'
  return dayjs(date).format('DD/MM/YYYY HH:mm')
}

const formatStudyTime = (minutes) => {
  if (!minutes) return '0 phút'
  if (minutes < 60) return `${minutes} phút`
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`
}

const getRoleType = (role) => {
  return role === 'ADMIN' ?  'danger' : role === 'TEACHER' ? 'warning' : 'primary'
}

const getLevelType = (level) => {
  const types = {
    BEGINNER: 'info',
    INTERMEDIATE: 'warning',
    ADVANCED: 'success',
  }
  return types[level] || 'info'
}
</script>
