<template>
  <div class="auth-page">
    <h2 class="auth-title">Đăng nhập</h2>
    <p class="auth-subtitle">Chào mừng trở lại! Vui lòng đăng nhập để tiếp tục.</p>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleLogin"
    >
      <!-- Username -->
      <el-form-item label="Tên đăng nhập" prop="usernameOrEmail">
        <el-input
          v-model="formData.usernameOrEmail"
          placeholder="Nhập tên đăng nhập"
          :prefix-icon="User"
          clearable
        />
      </el-form-item>

      <!-- Password -->
      <el-form-item label="Mật khẩu" prop="password">
        <el-input
          v-model="formData.password"
          type="password"
          placeholder="Nhập mật khẩu"
          :prefix-icon="Lock"
          show-password
          @keyup.enter="handleLogin"
        />
      </el-form-item>

      <!-- Forgot Password Link -->
      <div class="form-footer">
        <router-link to="/auth/forgot-password" class="forgot-link">
          Quên mật khẩu?
        </router-link>
      </div>

      <!-- Submit Button -->
      <el-form-item>
        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          :disabled="loading"
          style="width: 100%"
          size="large"
        >
          Đăng nhập
        </el-button>
      </el-form-item>

      <!-- Register Link -->
      <div class="form-link">
        <span>Chưa có tài khoản? </span>
        <router-link to="/auth/register" class="link-primary">
          Đăng ký ngay
        </router-link>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const formData = ref({
  usernameOrEmail: '',
  password: '',
})

const rules = {
  usernameOrEmail: [
    { required: true, message: 'Vui lòng nhập tên đăng nhập', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'Vui lòng nhập mật khẩu', trigger: 'blur' },
  ],
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await authStore.login(formData.value)

      // Redirect về trang trước đó hoặc dashboard
      const redirectPath = route.query.redirect || '/user/dashboard'
      await new Promise((resolve) => setTimeout(resolve, 1000)) // Small delay for better UX
      await router.push(redirectPath)
    } catch (error) {
      console.error('Login failed:', error)
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
  margin: 0 0 32px 0;
  font-size: 14px;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 24px;
}

.forgot-link {
  color: #409eff;
  font-size: 14px;
  text-decoration: none;
}

.forgot-link:hover {
  color: #66b1ff;
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
