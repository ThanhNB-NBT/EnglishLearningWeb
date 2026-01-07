<!-- src/components/admin/users/dialogs/TeacherTopicsDialog.vue -->
<template>
  <el-dialog
    v-model="visible"
    title="Topics được phân quyền"
    width="700px"
    align-center
    @close="handleClose"
  >
    <div v-if="teacher">
      <!-- Teacher Info -->
      <div class="flex items-center gap-3 mb-6 pb-4 border-b">
        <el-avatar :size="60" class="!bg-green-600 text-white !text-2xl font-bold">
          {{ teacher.username.charAt(0).toUpperCase() }}
        </el-avatar>
        <div>
          <h3 class="text-lg font-bold text-gray-900 dark:text-white">{{ teacher.fullName }}</h3>
          <p class="text-sm text-gray-500">@{{ teacher.username }}</p>
          <el-tag type="success" size="small" class="mt-1">Teacher</el-tag>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="flex justify-center py-8">
        <el-icon class="animate-spin text-2xl text-blue-500"><Loading /></el-icon>
      </div>

      <!-- Topics List -->
      <div v-else-if="assignments.length > 0" class="space-y-3">
        <div
          v-for="assignment in assignments"
          :key="assignment.id"
          class="p-4 border rounded-lg hover:border-blue-400 transition-colors"
        >
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <h4 class="font-semibold text-gray-900 dark:text-white">{{ assignment.topicName }}</h4>
              <div class="flex items-center gap-2 mt-2">
                <el-tag :type="getModuleTypeColor(assignment.moduleType)" size="small">
                  {{ assignment.moduleType }}
                </el-tag>
                <el-tag :type="assignment.isActive ? 'success' : 'info'" size="small">
                  {{ assignment.isActive ? 'Active' : 'Inactive' }}
                </el-tag>
              </div>
              <p class="text-xs text-gray-500 mt-2">
                Phân quyền lúc: {{ formatDate(assignment.assignedAt) }}
              </p>
            </div>
            <el-button
              type="danger"
              size="small"
              :icon="Delete"
              @click="handleRevoke(assignment)"
            >
              Thu hồi
            </el-button>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <el-empty v-else description="Chưa có topic nào được phân quyền" :image-size="100" />
    </div>

    <template #footer>
      <el-button @click="handleClose">Đóng</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Loading } from '@element-plus/icons-vue'
import { teacherAPI } from '@/api/modules/teacher.api'
import dayjs from 'dayjs'

const props = defineProps({
  modelValue: Boolean,
  teacher: Object,
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(props.modelValue)
const loading = ref(false)
const assignments = ref([])

watch(
  () => props.modelValue,
  async (val) => {
    visible.value = val
    if (val && props.teacher) {
      await fetchAssignments()
    }
  },
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const fetchAssignments = async () => {
  loading.value = true
  try {
    const response = await teacherAPI.getTeacherAssignments(props. teacher.id)
    assignments.value = response.data.data || []
  } catch (error) {
    console.error('Error fetching assignments:', error)
    ElMessage.error('Không thể tải danh sách phân quyền')
    assignments.value = []
  } finally {
    loading.value = false
  }
}

const handleRevoke = async (assignment) => {
  try {
    await ElMessageBox.confirm(
      `Xác nhận thu hồi quyền quản lý topic "${assignment.topicName}"? `,
      'Xác nhận',
      {
        confirmButtonText: 'Thu hồi',
        cancelButtonText: 'Hủy',
        type: 'warning',
      },
    )

    await teacherAPI.revokeAssignment(assignment.id)
    ElMessage.success('Đã thu hồi quyền thành công')
    await fetchAssignments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Error revoking assignment:', error)
      ElMessage.error('Không thể thu hồi quyền')
    }
  }
}

const handleClose = () => {
  visible.value = false
}

const formatDate = (date) => {
  if (!date) return 'N/A'
  return dayjs(date).format('DD/MM/YYYY HH: mm')
}

const getModuleTypeColor = (moduleType) => {
  const colors = {
    GRAMMAR: 'primary',
    READING: 'success',
    LISTENING: 'warning',
  }
  return colors[moduleType] || 'info'
}
</script>
