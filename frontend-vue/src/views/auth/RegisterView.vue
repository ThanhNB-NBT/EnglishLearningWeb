<template>
  <div class="w-full">
    <h2 class="text-2xl font-bold text-center text-gray-800 mb-2">Đăng ký</h2>
    <p class="text-center text-gray-500 text-sm mb-6">Tạo tài khoản mới để bắt đầu học tập!</p>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleRegister"
    >
      <el-form-item label="Tên đăng nhập" prop="username" class="!mb-5">
        <el-input
          v-model="formData.username"
          placeholder="Tối thiểu 3 ký tự"
          :prefix-icon="User"
          clearable
        />
      </el-form-item>

      <el-form-item label="Email" prop="email" class="!mb-5">
        <el-input
          v-model="formData.email"
          type="email"
          placeholder="example@email.com"
          :prefix-icon="Message"
          clearable
        />
      </el-form-item>

      <el-form-item label="Mật khẩu" prop="password" class="!mb-5">
        <el-input
          v-model="formData.password"
          type="password"
          placeholder="Tối thiểu 8 ký tự"
          :prefix-icon="Lock"
          show-password
        />
      </el-form-item>

      <el-form-item label="Xác nhận mật khẩu" prop="confirmPassword" class="!mb-8">
        <el-input
          v-model="formData.confirmPassword"
          type="password"
          placeholder="Nhập lại mật khẩu"
          :prefix-icon="Lock"
          show-password
          @keyup.enter="handleRegister"
        />
      </el-form-item>

      <el-form-item class="!mb-0">
        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          :disabled="loading"
          class="!w-full !h-11 !text-base !font-bold !rounded-lg"
        >
          Đăng ký tài khoản
        </el-button>
      </el-form-item>

      <div class="text-center text-sm text-gray-600 mt-6">
        <span>Đã có tài khoản? </span>
        <router-link to="/auth/login" class="text-blue-500 hover:text-blue-600 font-bold ml-1 no-underline transition-colors">
          Đăng nhập ngay
        </router-link>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { User, Message, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const formData = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

// Validation rules giữ nguyên logic cũ
const validateUsername = (rule, value, callback) => {
  if (!value) callback(new Error('Vui lòng nhập tên đăng nhập'))
  else if (value.length < 3) callback(new Error('Tên đăng nhập phải có ít nhất 3 ký tự'))
  else if (!/^[a-zA-Z0-9_]+$/.test(value)) callback(new Error('Chỉ chứa chữ cái, số và dấu gạch dưới'))
  else callback()
}

const validatePassword = (rule, value, callback) => {
  if (!value) callback(new Error('Vui lòng nhập mật khẩu'))
  else if (value.length < 8) callback(new Error('Mật khẩu phải có ít nhất 8 ký tự'))
  else callback()
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== formData.password) callback(new Error('Mật khẩu xác nhận không khớp'))
  else callback()
}

const rules = {
  username: [{ validator: validateUsername, trigger: 'blur' }],
  email: [
    { required: true, message: 'Vui lòng nhập email', trigger: 'blur' },
    { type: 'email', message: 'Email không hợp lệ', trigger: 'blur' },
  ],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
}

const handleRegister = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await authStore.register({
          username: formData.username,
          email: formData.email,
          password: formData.password
        })
        router.push({
          name: 'verify-email',
          query: { email: formData.email },
        })
      } catch (error) {
        console.error('Register failed:', error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>
