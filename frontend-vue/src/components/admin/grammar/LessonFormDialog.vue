<template>
  <el-dialog
    v-model="visible"
    :title="mode === 'create' ? 'Thêm bài ngữ pháp' : 'Cập nhật bài ngữ pháp'"
    width="900px"
    :close-on-click-modal="false"
    destroy-on-close
    top="5vh"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="140px"
      class="mt-4"
      status-icon
    >
      <el-form-item label="Tiêu đề bài học" prop="title">
        <el-input v-model="formData.title" placeholder="VD: Present Simple Tense" clearable />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Loại bài học" prop="lessonType">
            <el-select v-model="formData.lessonType" class="!w-full">
              <el-option label="Lý thuyết (Theory)" value="THEORY" />
              <el-option label="Thực hành (Practice)" value="PRACTICE" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Thứ tự hiển thị" prop="orderIndex">
            <el-input-number
              v-model="formData.orderIndex"
              :min="1"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Thời gian (s)">
            <el-input-number
              v-model="formData.timeLimitSeconds"
              :min="0"
              :step="60"
              class="!w-full"
              placeholder="0 = Không giới hạn"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Điểm thưởng">
            <el-input-number v-model="formData.pointsReward" :min="0" class="!w-full" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="Trạng thái">
        <el-switch
          v-model="formData.isActive"
          inline-prompt
          active-text="Hiện"
          inactive-text="Ẩn"
          style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
        />
      </el-form-item>

      <el-form-item label="Nội dung" prop="content" class="mt-4">
        <div class="mb-1 text-xs text-gray-500 italic">
          * Soạn thảo nội dung ngữ pháp hoặc hướng dẫn bài tập
        </div>
        <QuillRichEditor v-model="formData.content" height="400px" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button @click="visible = false">Hủy bỏ</el-button>
        <el-button type="primary" :loading="isLoading" @click="handleSubmit" class="!px-6">
          {{ mode === 'create' ? 'Lưu bài học' : 'Cập nhật' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import QuillRichEditor from '@/components/common/QuillRichEditor.vue'
import { useGrammarAdminStore } from '@/stores/admin/grammarAdmin'

const props = defineProps({ topicId: Number })
const emit = defineEmits(['success'])
const grammarStore = useGrammarAdminStore()

const visible = ref(false)
const mode = ref('create')
const isLoading = ref(false)
const formRef = ref(null)

// Map đúng với GrammarLessonDTO
const formData = ref({
  id: null,
  title: '',
  content: '',
  lessonType: 'THEORY', // Mặc định là Theory
  orderIndex: 1,
  timeLimitSeconds: 0,
  pointsReward: 10,
  isActive: true,
})

const rules = reactive({
  title: [{ required: true, message: 'Vui lòng nhập tiêu đề', trigger: 'blur' }],
  lessonType: [{ required: true, message: 'Chọn loại bài học', trigger: 'change' }],
  orderIndex: [{ required: true, message: 'Nhập thứ tự', trigger: 'change' }],
})

const openCreate = async () => {
  mode.value = 'create'

  formData.value = {
    id: null,
    title: '',
    content: '',
    lessonType: 'THEORY',
    orderIndex: 1,
    timeLimitSeconds: 0,
    pointsReward: 10,
    isActive: true,
  }

  if (props.topicId) {
    try {
      const nextOrder = await grammarStore.getNextLessonOrderIndex(props.topicId)
      console.log('Next lesson order index:', nextOrder)
      formData.value.orderIndex = nextOrder.data || 1
    } catch (e) {
      console.warn(e)
    }
  }

  visible.value = true
}

const openEdit = (row) => {
  mode.value = 'edit'
  // Clone data để tránh mutate trực tiếp store state
  formData.value = {
    ...row,
    // Backend trả về null cho timeLimitSeconds thì set về 0
    timeLimitSeconds: row.timeLimitSeconds || 0,
  }
  visible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  isLoading.value = true
  try {
    // Chuẩn bị payload đúng chuẩn JSON
    const payload = {
      topicId: props.topicId,
      title: formData.value.title,
      lessonType: formData.value.lessonType,
      content: formData.value.content,
      orderIndex: formData.value.orderIndex,
      timeLimitSeconds: formData.value.timeLimitSeconds,
      pointsReward: formData.value.pointsReward,
      isActive: formData.value.isActive,
    }

    if (mode.value === 'create') {
      await grammarStore.createLesson(payload)
    } else {
      await grammarStore.updateLesson(formData.value.id, payload)
    }

    ElMessage.success(mode.value === 'create' ? 'Tạo thành công' : 'Cập nhật thành công')
    visible.value = false
    emit('success')
  } catch (err) {
    console.error(err)
    // Error notification handled in store/interceptor
  } finally {
    isLoading.value = false
  }
}

defineExpose({ openCreate, openEdit })
</script>
