<!-- src/components/admin/users/dialogs/CreateTeacherDialog.vue -->
<template>
  <el-dialog
    v-model="visible"
    title="Tạo tài khoản Teacher"
    width="500px"
    align-center
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="Họ và tên" prop="fullName">
        <el-input v-model="form.fullName" placeholder="Nguyễn Văn A" />
      </el-form-item>

      <el-form-item label="Username" prop="username">
        <el-input v-model="form.username" placeholder="teacher_a" />
      </el-form-item>

      <el-form-item label="Email" prop="email">
        <el-input v-model="form.email" type="email" placeholder="teacher@example.com" />
      </el-form-item>

      <el-form-item label="Mật khẩu" prop="password">
        <el-input v-model="form.password" type="password" show-password placeholder="••••••••" />
      </el-form-item>

      <el-alert type="info" :closable="false" class="mb-4">
        <template #title>
          <span class="text-sm">Teacher sẽ có quyền quản lý Topics được phân công bởi Admin</span>
        </template>
      </el-alert>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">Hủy</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        Tạo Teacher
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { authAPI } from '@/api'

const props = defineProps({
  modelValue: Boolean,
})

const emit = defineEmits(['update:modelValue', 'created'])

const visible = ref(props.modelValue)
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  fullName: '',
  username: '',
  email: '',
  password: '',
})

const rules = {
  fullName: [{ required: true, message: 'Vui lòng nhập họ tên', trigger: 'blur' }],
  username: [
    { required: true, message: 'Vui lòng nhập username', trigger: 'blur' },
    { min: 3, max: 50, message: 'Username phải từ 3-50 ký tự', trigger: 'blur' },
  ],
  email: [
    { required: true, message: 'Vui lòng nhập email', trigger: 'blur' },
    { type: 'email', message: 'Email không hợp lệ', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'Vui lòng nhập mật khẩu', trigger: 'blur' },
    { min: 8, message: 'Mật khẩu phải ít nhất 8 ký tự', trigger: 'blur' },
  ],
}

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
  },
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      console.log('📤 Creating teacher:', form)

      const response = await authAPI.createTeacher(form)

      console.log('✅ Teacher created successfully:', response.data)

      ElMessage.success('Tạo tài khoản Teacher thành công!')
      emit('created')
      handleClose()
    } catch (error) {
      console.error('❌ Error creating teacher:', error)
      console.error('Response:', error.response?.data)

      ElMessage.error(error.response?.data?.message || 'Không thể tạo tài khoản Teacher')
    } finally {
      submitting.value = false
    }
  })
}

const handleClose = () => {
  formRef.value?.resetFields()
  visible.value = false
}
</script>
