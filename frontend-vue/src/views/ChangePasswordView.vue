<template>
  <div class="change-password-page">
    <div class="container">
      <h1 class="page-title">Đổi mật khẩu</h1>

      <el-card shadow="never">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 24px"
        >
          <template #title>
            Sau khi đổi mật khẩu, bạn sẽ cần đăng nhập lại
          </template>
        </el-alert>

        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-width="180px"
          label-position="left"
        >
          <!-- Old Password -->
          <el-form-item label="Mật khẩu hiện tại" prop="oldPassword">
            <el-input
              v-model="formData.oldPassword"
              type="password"
              placeholder="Nhập mật khẩu hiện tại"
              show-password
              clearable
            />
          </el-form-item>

          <!-- New Password -->
          <el-form-item label="Mật khẩu mới" prop="newPassword">
            <el-input
              v-model="formData.newPassword"
              type="password"
              placeholder="Tối thiểu 8 ký tự"
              show-password
              clearable
            />
          </el-form-item>

          <!-- Confirm Password -->
          <el-form-item label="Xác nhận mật khẩu mới" prop="confirmPassword">
            <el-input
              v-model="formData.confirmPassword"
              type="password"
              placeholder="Nhập lại mật khẩu mới"
              show-password
              clearable
              @keyup.enter="handleChangePassword"
            />
          </el-form-item>

          <!-- Action Buttons -->
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              :disabled="loading"
              @click="handleChangePassword"
            >
              Đổi mật khẩu
            </el-button>
            <el-button @click="resetForm">
              Đặt lại
            </el-button>
            <el-button @click="$router.back()">
              Quay lại
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { useChangePassword } from '@/composables/auth/useChangePassword'

const { loading, formRef, formData, rules, changePassword, resetForm } = useChangePassword()

const handleChangePassword = async () => {
  await changePassword()
}
</script>

<style scoped>
.change-password-page {
  padding: 24px;
}

.container {
  max-width: 700px;
  margin: 0 auto;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  margin: 0 0 24px 0;
  color: #303133;
}
</style>
