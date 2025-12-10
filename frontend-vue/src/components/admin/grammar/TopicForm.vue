<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? 'Cập nhật Chủ đề' : 'Thêm Chủ đề mới'"
    width="600px"
    align-center
    destroy-on-close
    :close-on-click-modal="false"
    class="!rounded-xl"
    @close="resetForm"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="p-2"
    >
      <el-form-item label="Tên chủ đề" prop="name" class="!mb-5">
        <el-input
          v-model="form.name"
          placeholder="VD: Thì hiện tại đơn, Mệnh đề quan hệ..."
          size="large"
        />
      </el-form-item>

      <div class="grid grid-cols-2 gap-5 mb-2">
        <el-form-item label="Trình độ" prop="levelRequired">
          <el-select v-model="form.levelRequired" placeholder="Chọn trình độ" class="!w-full" size="large">
            <el-option label="Sơ cấp (Beginner)" value="BEGINNER" />
            <el-option label="Trung cấp (Intermediate)" value="INTERMEDIATE" />
            <el-option label="Nâng cao (Advanced)" value="ADVANCED" />
          </el-select>
        </el-form-item>

        <el-form-item label="Thứ tự hiển thị" prop="orderIndex">
          <el-input-number
            v-model="form.orderIndex"
            :min="1"
            class="!w-full"
            size="large"
            controls-position="right"
          />
        </el-form-item>
      </div>

      <el-form-item label="Mô tả ngắn" prop="description" class="!mb-5">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="Mô tả nội dung chính của chủ đề này..."
          resize="none"
        />
      </el-form-item>

      <el-form-item label="Trạng thái">
        <div class="flex items-center gap-3 p-3 border border-gray-200 dark:border-gray-700 rounded-lg w-full">
          <el-switch
            v-model="form.isActive"
            style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
          />
          <span class="text-sm font-medium" :class="form.isActive ? 'text-green-600' : 'text-red-500'">
            {{ form.isActive ? 'Đang kích hoạt' : 'Đang ẩn' }}
          </span>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-3 pt-4 border-t border-gray-100 dark:border-gray-700">
        <el-button @click="visible = false" class="!rounded-lg">Hủy bỏ</el-button>
        <el-button
          type="primary"
          :loading="loading"
          @click="handleSubmit"
          class="!rounded-lg !font-bold px-6"
        >
          {{ isEdit ? 'Lưu thay đổi' : 'Tạo mới' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['success'])
const store = useGrammarStore()
const formRef = ref(null)
const visible = ref(false)
const loading = ref(false)
const isEdit = ref(false)

const form = reactive({
  id: null,
  name: '',
  description: '',
  levelRequired: 'BEGINNER',
  orderIndex: 1,
  isActive: true,
  imageUrl: '' // Nếu sau này cần
})

const rules = {
  name: [{ required: true, message: 'Vui lòng nhập tên chủ đề', trigger: 'blur' }],
  levelRequired: [{ required: true, message: 'Vui lòng chọn trình độ', trigger: 'change' }]
}

// Public methods called by parent
const openCreate = async () => {
  isEdit.value = false
  resetFormState()
  visible.value = true
  // Lấy order tiếp theo
  const nextOrder = await store.getNextTopicOrderIndex()
  form.orderIndex = nextOrder
}

const openEdit = (topic) => {
  isEdit.value = true
  Object.assign(form, topic)
  visible.value = true
}

const resetFormState = () => {
  form.id = null
  form.name = ''
  form.description = ''
  form.levelRequired = 'BEGINNER'
  form.orderIndex = 1
  form.isActive = true
}

const resetForm = () => {
  if (formRef.value) formRef.value.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        if (isEdit.value) {
          await store.updateTopic(form.id, form)
        } else {
          await store.createTopic(form)
        }
        emit('success')
        visible.value = false
      } catch (e) {
        console.error(e)
      } finally {
        loading.value = false
      }
    }
  })
}

defineExpose({ openCreate, openEdit })
</script>
