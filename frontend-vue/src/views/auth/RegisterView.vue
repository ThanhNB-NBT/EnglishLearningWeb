<template>
  <div class="auth-page">
    <h2 class="auth-title">Đăng ký</h2>
    <p class="auth-subtitle">Tạo tài khoản mới để bắt đầu học tập!</p>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleRegister"
    >
      <!-- Username -->
      <el-form-item label="Tên đăng nhập" prop="username">
        <el-input
          v-model="formData.username"
          placeholder="Tối thiểu 3 ký tự"
          :prefix-icon="User"
          clearable
        />
      </el-form-item>

      <!-- Email -->
      <el-form-item label="Email" prop="email">
        <el-input
          v-model="formData.email"
          type="email"
          placeholder="example@email.com"
          :prefix-icon="Message"
          clearable
        />
      </el-form-item>

      <!-- Password -->
      <el-form-item label="Mật khẩu" prop="password">
        <el-input
          v-model="formData.password"
          type="password"
          placeholder="Tối thiểu 6 ký tự"
          :prefix-icon="Lock"
          show-password
        />
      </el-form-item>

      <!-- Confirm Password -->
      <el-form-item label="Xác nhận mật khẩu" prop="confirmPassword">
        <el-input
          v-model="formData.confirmPassword"
          type="password"
          placeholder="Nhập lại mật khẩu"
          :prefix-icon="Lock"
          show-password
          @keyup.enter="handleRegister"
        />
      </el-form-item>

      <!-- Submit Button -->
      <el-form-item style="margin-top: 32px">
        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          :disabled="loading"
          style="width: 100%"
          size="large"
        >
          Đăng ký
        </el-button>
      </el-form-item>

      <!-- Login Link -->
      <div class="form-link">
        <span>Đã có tài khoản? </span>
        <router-link to="/auth/login" class="link-primary">
          Đăng nhập ngay
        </router-link>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { User, Message, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const formData = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const validateUsername = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Vui lòng nhập tên đăng nhập'))
  } else if (value.length < 3) {
    callback(new Error('Tên đăng nhập phải có ít nhất 3 ký tự'))
  } else if (!/^[a-zA-Z0-9_]+$/.test(value)) {
    callback(new Error('Chỉ chứa chữ cái, số và dấu gạch dưới'))
  } else {
    callback()
  }
}

const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Vui lòng nhập mật khẩu'))
  } else if (value.length < 8) {
    callback(new Error('Mật khẩu phải có ít nhất 8 ký tự'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Vui lòng nhập lại mật khẩu'))
  } else if (value !== formData.value.password) {
    callback(new Error('Mật khẩu xác nhận không khớp'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { validator: validateUsername, trigger: 'blur' },
  ],
  email: [
    { required: true, message: 'Vui lòng nhập email', trigger: 'blur' },
    { type: 'email', message: 'Email không hợp lệ', trigger: 'blur' },
  ],
  password: [
    { validator: validatePassword, trigger: 'blur' },
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const handleRegister = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const { username, email, password } = formData.value
      await authStore.register({ username, email, password })

      // Chuyển sang trang verify email với email
      router.push({
        name: 'verify-email',
        query: { email },
      })
    } catch (error) {
      console.error('Register failed:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-page {
  width: 100%;
}

.auth-title {
  font-size: 28px;
  font-weight: bold;
  text-align: center;
  margin: 0 0 8px 0;
  color: #303133;
}

.auth-subtitle {
  text-align: center;
  color: #909399;
  margin: 0 0 16px 0;
  font-size: 14px;
}

.form-link {
  text-align: center;
  font-size: 14px;
  color: #606266;
}

.link-primary {
  color: #409eff;
  text-decoration: none;
  font-weight: 600;
}

.link-primary:hover {
  color: #66b1ff;
}
</style>
