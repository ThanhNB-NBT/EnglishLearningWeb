<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isEditMode ? 'Chỉnh sửa Topic' : 'Tạo Topic mới'"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="140px"
      label-position="left"
    >
      <!-- Tên Topic -->
      <el-form-item label="Tên Topic" prop="name">
        <el-input
          v-model="formData.name"
          placeholder="VD: Present Simple Tense"
          clearable
        />
      </el-form-item>

      <!-- Mô tả -->
      <el-form-item label="Mô tả" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="4"
          placeholder="Mô tả chi tiết về chủ đề này..."
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <!-- Level -->
      <el-form-item label="Level Required" prop="levelRequired">
        <el-select
          v-model="formData.levelRequired"
          placeholder="Chọn level"
          style="width: 100%"
        >
          <el-option
            v-for="level in levels"
            :key="level.value"
            :label="level.label"
            :value="level.value"
          >
            <span>{{ level.label }}</span>
            <el-tag
              :type="level.color"
              size="small"
              style="float: right"
            >
              {{ level.value }}
            </el-tag>
          </el-option>
        </el-select>
      </el-form-item>

      <!-- Order Index -->
      <el-form-item label="Order Index" prop="orderIndex">
        <el-input-number
          v-model="formData.orderIndex"
          :min="1"
          :step="1"
          placeholder="Vị trí sắp xếp"
          style="width: 100%"
        />
        <el-text size="small" type="info" style="margin-top: 4px">
          Số thứ tự hiển thị trong danh sách
        </el-text>
      </el-form-item>

      <!-- Active Status -->
      <el-form-item label="Trạng thái">
        <el-switch
          v-model="formData.isActive"
          active-text="Active"
          inactive-text="Inactive"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">Hủy</el-button>
        <el-button
          type="primary"
          :loading="loading"
          @click="handleSubmit"
        >
          {{ isEditMode ? 'Cập nhật' : 'Tạo mới' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import { LEVEL_CONFIG } from '@/types/grammar.types'

// Store
const grammarStore = useGrammarStore()

// Props
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  topic: {
    type: Object,
    default: null,
  },
})

// Emits
const emit = defineEmits(['update:modelValue', 'success'])

// Refs
const formRef = ref(null)
const loading = ref(false)

// Computed
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const isEditMode = computed(() => !!props.topic?.id)

// Form data
const defaultFormData = {
  name: '',
  description: '',
  levelRequired: 'BEGINNER',
  orderIndex: 1,
  isActive: true,
}

const formData = reactive({ ...defaultFormData })

// Validation rules
const rules = {
  name: [
    { required: true, message: 'Vui lòng nhập tên topic', trigger: 'blur' },
    { min: 3, max: 200, message: 'Tên topic từ 3-200 ký tự', trigger: 'blur' },
  ],
  levelRequired: [
    { required: true, message: 'Vui lòng chọn level', trigger: 'change' },
  ],
  orderIndex: [
    { required: true, message: 'Vui lòng nhập order index', trigger: 'blur' },
    { type: 'number', min: 1, message: 'Order index phải >= 1', trigger: 'blur' },
  ],
}

// Use imported LEVEL_CONFIG
const levels = LEVEL_CONFIG

// Watch topic prop để populate form khi edit
watch(
  () => props.topic,
  async (newTopic) => {
    if (newTopic) {
      // Edit mode - populate form with existing data
      Object.assign(formData, {
        name: newTopic.name,
        description: newTopic.description || '',
        levelRequired: newTopic.levelRequired,
        orderIndex: newTopic.orderIndex,
        isActive: newTopic.isActive ?? true,
      })

      console.log('Edit mode - Loaded topic:', newTopic.name, 'orderIndex:', newTopic.orderIndex)
    } else {
      // Create mode - auto fetch next orderIndex từ backend
      console.log('Create mode - Fetching next orderIndex...')

      try {
        const nextOrder = await grammarStore.getNextTopicOrderIndex()
        formData.orderIndex = nextOrder

        console.log('Auto set orderIndex:', nextOrder)
      } catch (error) {
        console.error('Error getting next order:', error)
        // Fallback to manual calculation
        formData.orderIndex = grammarStore.topics.length + 1
      }
    }
  },
  { immediate: true }
)

// Watch dialog visibility để reset orderIndex khi mở dialog create
watch(
  () => props.modelValue,
  async (isVisible) => {
    if (isVisible && !props.topic) {
      // Dialog mở ở create mode
      const nextOrder = await grammarStore.getNextTopicOrderIndex()
      formData.orderIndex = nextOrder
      console.log('Dialog opened - Auto set orderIndex:', nextOrder)
    }
  }
)

// Methods
const handleSubmit = async () => {
  try {
    // Validate form
    const valid = await formRef.value?.validate()
    if (!valid) return

    loading.value = true

    if (isEditMode.value) {
      // Update existing topic
      await grammarStore.updateTopic(props.topic.id, formData)
    } else {
      // Create new topic
      await grammarStore.createTopic(formData)
    }

    emit('success')
    handleClose()
  } catch (error) {
    console.error('Error submitting form:', error)
    // Error message handled in store
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, defaultFormData)
  dialogVisible.value = false
}
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-select-dropdown__item) {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
