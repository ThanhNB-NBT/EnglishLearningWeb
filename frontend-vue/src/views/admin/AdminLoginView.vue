<template>
  <div class="admin-login-page">
    <div class="admin-login-container">
      <!-- Admin Badge -->
      <div class="admin-badge">
        <el-icon :size="48" color="#409eff">
          <Lock />
        </el-icon>
        <h2 class="admin-title">Quản trị viên</h2>
      </div>

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
            placeholder="Nhập tên đăng nhập admin"
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
            Đăng nhập
          </el-button>
        </el-form-item>

        <!-- Back to User Login -->
        <div class="form-link">
          <router-link to="/auth/login" class="link-secondary">
            ← Đăng nhập với tài khoản người dùng
          </router-link>
        </div>
      </el-form>
    </div>
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
      await authStore.loginAdmin(formData.value)

      // Redirect về admin dashboard
      const redirectPath = route.query.redirect || '/admin/dashboard'
      router.push(redirectPath)
    } catch (error) {
      console.error('Admin login failed:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.admin-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  padding: 24px;
}

.admin-login-container {
  width: 100%;
  max-width: 440px;
  background: white;
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.admin-badge {
  text-align: center;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 2px solid #f5f7fa;
}

.admin-title {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin: 16px 0 0 0;
}

.form-link {
  text-align: center;
  font-size: 14px;
  margin-top: 16px;
}

.link-secondary {
  color: #606266;
  text-decoration: none;
}

.link-secondary:hover {
  color: #409eff;
}

@media (max-width: 768px) {
  .admin-login-container {
    padding: 32px 24px;
  }
}
</style>
