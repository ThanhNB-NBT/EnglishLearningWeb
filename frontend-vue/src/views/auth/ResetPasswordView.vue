<template>
  <div class="auth-page">
    <!-- Icon -->
    <div class="icon-wrapper">
      <el-icon :size="64" color="#67c23a">
        <Key />
      </el-icon>
    </div>

    <h2 class="auth-title">Đặt lại mật khẩu</h2>
    <p class="auth-subtitle">
      Nhập mật khẩu mới cho tài khoản: <br />
      <strong>{{ email }}</strong>
    </p>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleResetPassword"
    >
      <!-- Email (readonly) -->
      <el-form-item label="Email">
        <el-input
          v-model="email"
          :prefix-icon="Message"
          readonly
          disabled
        />
      </el-form-item>

      <!-- New Password -->
      <el-form-item label="Mật khẩu mới" prop="newPassword">
        <el-input
          v-model="formData.newPassword"
          type="password"
          placeholder="Tối thiểu 6 ký tự"
          :prefix-icon="Lock"
          show-password
        />
      </el-form-item>

      <!-- Confirm Password -->
      <el-form-item label="Xác nhận mật khẩu mới" prop="confirmPassword">
        <el-input
          v-model="formData.confirmPassword"
          type="password"
          placeholder="Nhập lại mật khẩu mới"
          :prefix-icon="Lock"
          show-password
          @keyup.enter="handleResetPassword"
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
          Đặt lại mật khẩu
        </el-button>
      </el-form-item>

      <!-- Back to Login -->
      <div class="form-link">
        <router-link to="/auth/login" class="link-primary">
          ← Quay lại đăng nhập
        </router-link>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useResetPassword } from '@/composables/auth/useResetPassword'
import { Message, Lock, Key } from '@element-plus/icons-vue'
import { useToast } from 'vue-toastification'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const email = ref(route.query.email || '')

const { loading, formRef, formData, rules, resetPassword } = useResetPassword(email.value)

const handleResetPassword = async () => {
  await resetPassword()
}

onMounted(() => {
  if (!email.value) {
    toast.error('Email không hợp lệ. Vui lòng thực hiện lại quy trình quên mật khẩu.')
    router.push('/auth/forgot-password')
  }
})
</script>

<style scoped>
.auth-page {
  width: 100%;
}

.icon-wrapper {
  text-align: center;
  margin-bottom: 24px;
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
  line-height: 1.6;
}

.auth-subtitle strong {
  color: #67c23a;
}

.form-link {
  text-align: center;
  font-size: 14px;
  color: #606266;
  margin-top: 16px;
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
