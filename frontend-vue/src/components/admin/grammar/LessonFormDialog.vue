<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="90%"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="localFormData"
      :rules="formRules"
      label-width="140px"
      label-position="top"
    >
      <!-- Basic Info Row -->
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Tiêu đề" prop="title">
            <el-input
              v-model="localFormData.title"
              placeholder="Nhập tiêu đề bài học"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="Loại bài học" prop="lessonType">
            <el-select
              v-model="localFormData.lessonType"
              placeholder="Chọn loại"
              @change="handleLessonTypeChange"
            >
              <el-option
                v-for="option in lessonTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              >
                <span>{{ option.label }}</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- Settings Row -->
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="Thứ tự" prop="orderIndex">
            <el-input-number
              v-model="localFormData.orderIndex"
              :min="1"
              :max="9999"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>

        <el-col :span="6">
          <el-form-item label="Điểm thưởng" prop="pointsReward">
            <el-input-number
              v-model="localFormData.pointsReward"
              :min="1"
              :max="1000"
              style="width: 100%"
            >
              <template #prefix>🏆</template>
            </el-input-number>
          </el-form-item>
        </el-col>

        <el-col :span="6">
          <el-form-item label="Thời gian (giây)" prop="estimatedDuration">
            <el-input-number
              v-model="localFormData.estimatedDuration"
              :min="10"
              :max="3600"
              :step="30"
              style="width: 100%"
            >
              <template #prefix>⏱️</template>
            </el-input-number>
            <div class="field-hint">{{ formatDuration(localFormData.estimatedDuration) }}</div>
          </el-form-item>
        </el-col>

        <el-col :span="6">
          <el-form-item label="Trạng thái" prop="isActive">
            <el-switch
              v-model="localFormData.isActive"
              active-text="Hoạt động"
              inactive-text="Tắt"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- Content Editor -->
      <el-form-item label="content">
        <QuillRichEditor
          v-model="localFormData.content"
          height="500px"
          :toolbar="lessonToolbar"
          placeholder="Nhập nội dung..."
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">Hủy</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ submitButtonText }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import QuillRichEditor from '@/components/common/QuillRichEditor.vue'

const props = defineProps({
  visible: {
    type: Boolean,
    required: true,
  },
  mode: {
    type: String, // 'create' | 'edit'
    required: true,
  },
  formData: {
    type: Object,
    required: true,
  },
  topicId: {
    type: Number,
    default: null,
  },
})

const lessonToolbar = [
  [{ 'header': [1, 2, 3, false] }],
  ['bold', 'italic', 'underline', 'strike'],
  [{ 'list': 'ordered'}, { 'list': 'bullet' }],
  [{ 'color': [] }, { 'background': [] }],
  [{ 'align': [] }],
  ['blockquote', 'code-block'],
  ['link', 'image'],
  ['clean']
]

const emit = defineEmits(['close', 'success'])

const grammarStore = useGrammarStore()

// State
const formRef = ref(null)
const submitting = ref(false)
const metadataString = ref('')
const metadataError = ref('')

// Local form data to avoid mutating props directly
const localFormData = ref({ ...props.formData })

const formRules = {
  title: [
    { required: true, message: 'Vui lòng nhập tiêu đề', trigger: 'blur' },
    { max: 200, message: 'Tiêu đề không được vượt quá 200 ký tự', trigger: 'blur' },
  ],
  lessonType: [
    { required: true, message: 'Vui lòng chọn loại bài học', trigger: 'change' },
  ],
  orderIndex: [
    { required: true, message: 'Vui lòng nhập thứ tự', trigger: 'blur' },
    { type: 'number', min: 1, message: 'Thứ tự phải lớn hơn 0', trigger: 'blur' },
  ],
  pointsReward: [
    { required: true, message: 'Vui lòng nhập điểm thưởng', trigger: 'blur' },
    { type: 'number', min: 1, message: 'Điểm thưởng phải lớn hơn 0', trigger: 'blur' },
  ],
  estimatedDuration: [
    { required: true, message: 'Vui lòng nhập thời gian ước tính', trigger: 'blur' },
    { type: 'number', min: 10, message: 'Thời gian phải >= 10 giây', trigger: 'blur' },
  ],
}

const lessonTypeOptions = [
  { value: 'THEORY', label: 'Theory (Lý thuyết)'},
  { value: 'PRACTICE', label: 'Practice (Thực hành)' },
]

// Computed
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => {
    if (!val) emit('close')
  },
})

const dialogTitle = computed(() => {
  return props.mode === 'create' ? 'Tạo Lesson Mới' : 'Chỉnh Sửa Lesson'
})

const submitButtonText = computed(() => {
  return props.mode === 'create' ? 'Tạo Mới' : 'Cập Nhật'
})

// Methods
const handleLessonTypeChange = (type) => {
  if (type === 'THEORY') {
    localFormData.value.pointsReward = 10
    localFormData.value.estimatedDuration = 180
  } else if (type === 'PRACTICE') {
    localFormData.value.pointsReward = 15
    localFormData.value.estimatedDuration = 300
  }
}

const formatDuration = (seconds) => {
  if (!seconds) return '0s'
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  return minutes > 0 ? `${minutes} phút ${secs} giây` : `${secs} giây`
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    submitting.value = true

    let result
    if (props.mode === 'create') {
      result = await grammarStore.createLesson(localFormData.value)
    } else {
      result = await grammarStore.updateLesson(localFormData.value.id, localFormData.value)
    }

    if (result) {
      emit('success')
      emit('close')
    }
  } catch (error) {
    console.error('Form validation or submit failed:', error)
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  formRef.value?.resetFields()
  metadataString.value = ''
  metadataError.value = ''
  emit('close')
}

// Watch for formData changes (when opening edit dialog)
watch(
  () => props.formData,
  (newData) => {
    localFormData.value = { ...newData }

    // Convert metadata object to JSON string for display
    if (newData.metadata && typeof newData.metadata === 'object') {
      metadataString.value = JSON.stringify(newData.metadata, null, 2)
    } else {
      metadataString.value = ''
    }
  },
  { deep: true, immediate: true }
)
</script>

<style scoped>
.editor-wrapper {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.field-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
}

/* TinyMCE custom styles */
:deep(.tox-tinymce) {
  border: none !important;
}

:deep(.tox .tox-toolbar__primary) {
  background: #f5f7fa !important;
}
</style>
