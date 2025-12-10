<template>
  <div class="min-h-screen bg-gray-50 dark:bg-dark-bg p-4 flex items-center justify-center">
    <div class="w-full max-w-lg bg-white dark:bg-[#1d1d1d] rounded-2xl shadow-lg border border-gray-200 dark:border-[#303030] p-8">
      <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-6 text-center">Đổi mật khẩu</h2>

      <el-alert
        title="Lưu ý: Sau khi đổi mật khẩu, bạn sẽ cần đăng nhập lại."
        type="warning"
        :closable="false"
        show-icon
        class="!mb-6"
      />

      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="handleChangePassword"
      >
        <el-form-item label="Mật khẩu hiện tại" prop="oldPassword">
          <el-input v-model="formData.oldPassword" type="password" show-password />
        </el-form-item>

        <el-form-item label="Mật khẩu mới" prop="newPassword">
          <el-input v-model="formData.newPassword" type="password" placeholder="Tối thiểu 8 ký tự" show-password />
        </el-form-item>

        <el-form-item label="Xác nhận mật khẩu mới" prop="confirmPassword">
          <el-input v-model="formData.confirmPassword" type="password" show-password @keyup.enter="handleChangePassword" />
        </el-form-item>

        <div class="flex flex-col gap-3 mt-8">
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            class="!w-full !font-bold"
          >
            Đổi mật khẩu
          </el-button>
          <el-button @click="$router.back()" class="!w-full !ml-0">
            Hủy bỏ
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
// Script giữ nguyên
import { useChangePassword } from '@/composables/auth/useChangePassword'
const { loading, formRef, formData, rules, changePassword } = useChangePassword()
const handleChangePassword = async () => await changePassword()
</script>
