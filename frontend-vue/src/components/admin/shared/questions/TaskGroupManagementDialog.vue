<!-- src/components/admin/shared/questions/TaskGroupManagementDialog.vue -->
<template>
  <el-dialog
    v-model="visible"
    title="Quản lý Task Groups"
    width="900px"
    :close-on-click-modal="false"
    destroy-on-close
    class="!rounded-xl"
  >
    <div v-loading="loading" class="space-y-4">
      <!-- Header Actions -->
      <div class="flex justify-between items-center pb-3 border-b">
        <div class="text-sm text-gray-600">
          Tổng: <strong class="text-blue-600">{{ taskGroups.length }}</strong> task groups
        </div>
        <el-button type="primary" :icon="Plus" @click="handleCreate">
          Tạo Task Group mới
        </el-button>
      </div>

      <!-- Task Groups List -->
      <el-empty
        v-if="taskGroups.length === 0"
        description="Chưa có Task Group nào"
        :image-size="100"
      >
        <el-button type="primary" :icon="Plus" @click="handleCreate">
          Tạo Task Group đầu tiên
        </el-button>
      </el-empty>

      <div v-else class="space-y-3 max-h-[500px] overflow-y-auto">
        <div
          v-for="tg in taskGroups"
          :key="tg.id"
          class="border border-gray-300 dark:border-gray-700 rounded-lg p-4 bg-white dark:bg-[#1d1d1d] hover:shadow-md transition-shadow"
        >
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <div class="flex items-center gap-3 mb-2">
                <el-icon class="text-blue-600"><Collection /></el-icon>
                <h4 class="font-bold text-gray-800 dark:text-white">{{ tg.taskName }}</h4>
                <el-tag size="small" type="info">
                  {{ tg.questionCount || 0 }} questions
                </el-tag>
                <el-tag size="small" type="warning">
                  {{ tg.totalPoints || 0 }} điểm
                </el-tag>
                <el-tag size="small" class="!text-xs">
                  Order: {{ tg.orderIndex }}
                </el-tag>
              </div>

              <div
                v-if="tg.instruction"
                class="text-sm text-gray-600 dark:text-gray-400 italic ml-7"
              >
                "{{ tg.instruction }}"
              </div>

              <div class="text-xs text-gray-400 ml-7 mt-1">
                Created: {{ formatDate(tg.createdAt) }}
              </div>
            </div>

            <div class="flex gap-2">
              <el-button
                type="primary"
                :icon="Edit"
                circle
                plain
                size="small"
                @click="handleEdit(tg)"
              />
              <el-button
                type="danger"
                :icon="Delete"
                circle
                plain
                size="small"
                @click="handleDelete(tg)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">Đóng</el-button>
    </template>

    <!-- Create/Edit Form Dialog -->
    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? 'Tạo Task Group mới' : 'Chỉnh sửa Task Group'"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
      append-to-body
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
        label-width="120px"
      >
        <el-form-item label="Task Name" prop="taskName">
          <el-input
            v-model="formData.taskName"
            placeholder="VD: Task 1: Multiple Choice"
            clearable
          />
        </el-form-item>

        <el-form-item label="Instruction (Hướng dẫn)" prop="instruction">
          <el-input
            v-model="formData.instruction"
            type="textarea"
            :rows="3"
            placeholder="VD: Choose the correct answer (A, B, C or D)"
          />
        </el-form-item>

        <el-form-item label="Order Index" prop="orderIndex">
          <el-input-number
            v-model="formData.orderIndex"
            :min="1"
            class="!w-full"
            placeholder="Auto nếu để trống"
          />
          <div class="text-xs text-gray-500 mt-1">
            Để trống để tự động đánh số tiếp theo
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">Hủy</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
        >
          {{ formMode === 'create' ? 'Tạo mới' : 'Cập nhật' }}
        </el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Plus, Edit, Delete, Collection } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQuestionStore } from '@/stores/admin/questionAdmin'
import dayjs from 'dayjs'

const props = defineProps({
  parentType: {
    type: String,
    required: true,
  },
  lessonId: {
    type: Number,
    required: true,
  },
})

const emit = defineEmits(['refresh'])

const questionStore = useQuestionStore()
const visible = ref(false)
const loading = ref(false)
const formVisible = ref(false)
const formMode = ref('create') // 'create' | 'edit'
const submitting = ref(false)
const formRef = ref(null)

const taskGroups = computed(() => questionStore.taskGroups || [])

const defaultFormData = {
  taskName: '',
  instruction: '',
  orderIndex: null,
}

const formData = ref({ ...defaultFormData })

const formRules = {
  taskName: [
    { required: true, message: 'Vui lòng nhập tên task', trigger: 'blur' },
    { min: 3, message: 'Tên task phải có ít nhất 3 ký tự', trigger: 'blur' },
  ],
}

// ==================== METHODS ====================

const open = async () => {
  visible.value = true
  await loadTaskGroups()
}

const loadTaskGroups = async () => {
  loading.value = true
  try {
    await questionStore.fetchTaskGroups(props.parentType, props.lessonId)
  } catch (error) {
    console.error('Failed to load task groups:', error)
    ElMessage.error('Lỗi tải danh sách Task Groups')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  formMode.value = 'create'
  formData.value = { ...defaultFormData }
  formVisible.value = true
}

const handleEdit = (taskGroup) => {
  formMode.value = 'edit'
  formData.value = {
    id: taskGroup.id,
    taskName: taskGroup.taskName,
    instruction: taskGroup.instruction || '',
    orderIndex: taskGroup.orderIndex,
  }
  formVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('Vui lòng kiểm tra lại các trường bắt buộc')
      return
    }

    submitting.value = true
    try {
      if (formMode.value === 'create') {
        await questionStore.createTaskGroup(
          props.parentType,
          props.lessonId,
          formData.value
        )
        ElMessage.success('Tạo Task Group thành công!')
      } else {
        await questionStore.updateTaskGroup(
          formData.value.id,
          formData.value
        )
        ElMessage.success('Cập nhật Task Group thành công!')
      }

      formVisible.value = false
      await loadTaskGroups()
      emit('refresh')
    } catch (error) {
      console.error('Submit error:', error)
      ElMessage.error(
        error.response?.data?.message || 'Có lỗi xảy ra'
      )
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = async (taskGroup) => {
  try {
    await ElMessageBox.confirm(
      `Xóa Task Group "${taskGroup.taskName}" và TẤT CẢ ${taskGroup.questionCount || 0} câu hỏi bên trong?`,
      'Cảnh báo',
      {
        type: 'warning',
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy',
        confirmButtonClass: 'el-button--danger',
      }
    )

    loading.value = true
    await questionStore.deleteTaskGroup(
      taskGroup.id,
      props.parentType,
      props.lessonId
    )

    ElMessage.success('Đã xóa Task Group')
    await loadTaskGroups()
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Delete error:', error)
      ElMessage.error('Lỗi xóa Task Group')
    }
  } finally {
    loading.value = false
  }
}

const formatDate = (date) => {
  if (!date) return 'N/A'
  return dayjs(date).format('DD/MM/YYYY HH:mm')
}

defineExpose({ open })
</script>

<style scoped>
:deep(.el-dialog__body) {
  padding: 20px;
}
</style>
