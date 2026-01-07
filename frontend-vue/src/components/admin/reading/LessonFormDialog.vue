<template>
  <el-dialog
    v-model="visible"
    :title="mode === 'create' ? 'Thêm bài đọc' : 'Cập nhật bài đọc'"
    width="900px"
    :close-on-click-modal="false"
    destroy-on-close
    top="5vh"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="140px" class="mt-4">
      <el-form-item label="Tiêu đề" prop="title">
        <el-input v-model="formData.title" placeholder="VD: Environmental Protection" />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="Thứ tự" prop="orderIndex">
            <el-input-number v-model="formData.orderIndex" :min="1" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="Thời gian (s)" prop="timeLimitSeconds">
            <el-input-number v-model="formData.timeLimitSeconds" :min="60" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="Điểm thưởng" prop="pointsReward">
            <el-input-number v-model="formData.pointsReward" :min="0" class="!w-full" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="Trạng thái">
        <el-switch v-model="formData.isActive" active-text="Kích hoạt" inactive-text="Ẩn" />
      </el-form-item>

      <el-tabs type="border-card" class="mt-2">
        <el-tab-pane label="Nội dung bài đọc (English)">
          <QuillRichEditor v-model="formData.content" height="350px" />
        </el-tab-pane>
        <el-tab-pane label="Bản dịch (Vietnamese)">
          <QuillRichEditor v-model="formData.contentTranslation" height="350px" />
        </el-tab-pane>
      </el-tabs>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">Đóng</el-button>
      <el-button type="primary" :loading="isLoading" @click="handleSubmit">
        {{ mode === 'create' ? 'Lưu bài đọc' : 'Cập nhật' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref} from 'vue'
import { ElMessage } from 'element-plus'
import QuillRichEditor from '@/components/common/QuillRichEditor.vue'
import { useReadingAdminStore } from '@/stores/admin/readingAdmin'

const props = defineProps({ topicId: Number })
const emit = defineEmits(['success'])
const readingAdminStore = useReadingAdminStore()

const visible = ref(false)
const mode = ref('create')
const isLoading = ref(false)
const formRef = ref(null)

// Map đúng Entity ReadingLesson
const formData = ref({
  id: null,
  title: '',
  content: '',
  contentTranslation: '',
  orderIndex: 1,
  timeLimitSeconds: 600,
  pointsReward: 25,
  isActive: true,
})

const rules = {
  title: [{ required: true, message: 'Nhập tiêu đề', trigger: 'blur' }],
  content: [{ required: true, message: 'Nội dung bài đọc không được trống', trigger: 'blur' }],
}

const openCreate = async () => {
  mode.value = 'create'
  formData.value = {
    id: null,
    title: '',
    content: '',
    contentTranslation: '',
    orderIndex: 1,
    timeLimitSeconds: 600,
    pointsReward: 25,
    isActive: true,
  }
  if (props.topicId) {
    try {
      const nextOrder = await readingAdminStore.getNextLessonOrderIndex(props.topicId)
      formData.value.orderIndex = nextOrder || 1
    } catch (e) {
      console.warn('Cannot fetch next order index', e)
    }
  }
  visible.value = true
}

const openEdit = (row) => {
  mode.value = 'edit'
  formData.value = { ...row }
  visible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  isLoading.value = true
  try {
    // ✅ FIX: Ưu tiên lấy props.topicId, nếu không có thì lấy từ formData (trường hợp edit)
    const topicId = props.topicId || formData.value.topicId

    // Nếu vẫn không có topicId thì báo lỗi (Reading bắt buộc phải thuộc về 1 topic)
    if (!topicId) {
      ElMessage.error('Lỗi: Không xác định được chủ đề (Topic ID)')
      isLoading.value = false
      return
    }

    const payload = {
      ...formData.value,
      topicId: topicId,
    }

    if (mode.value === 'create') {
      await readingAdminStore.createLesson(payload)
      ElMessage.success('Thêm mới thành công')
    } else {
      await readingAdminStore.updateLesson(formData.value.id, payload)
      ElMessage.success('Cập nhật thành công')
    }
    emit('success')
    visible.value = false

  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi lưu dữ liệu')
  } finally {
    isLoading.value = false
  }
}

defineExpose({ openCreate, openEdit })
</script>
