<template>
  <div class="w-full">
    <div class="text-center mb-6">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-green-50 dark:bg-green-900/30 text-green-500 mb-4">
        <el-icon :size="32"><Key /></el-icon>
      </div>
      <h2 class="text-2xl font-bold text-gray-800 dark:text-white mb-2">Đặt lại mật khẩu</h2>
      <p class="text-sm text-gray-500 dark:text-gray-400">
        Nhập mật khẩu mới cho tài khoản:<br/>
        <strong class="text-gray-700 dark:text-gray-300">{{ email }}</strong>
      </p>
    </div>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleResetPassword"
      class="space-y-4"
    >
      <el-form-item label="Mật khẩu mới" prop="newPassword" class="!mb-4">
        <el-input
          v-model="formData.newPassword"
          type="password"
          placeholder="Tối thiểu 8 ký tự"
          :prefix-icon="Lock"
          show-password
        />
      </el-form-item>

      <el-form-item label="Xác nhận mật khẩu" prop="confirmPassword" class="!mb-6">
        <el-input
          v-model="formData.confirmPassword"
          type="password"
          placeholder="Nhập lại mật khẩu mới"
          :prefix-icon="Lock"
          show-password
          @keyup.enter="handleResetPassword"
        />
      </el-form-item>

      <el-button
        type="primary"
        native-type="submit"
        :loading="loading"
        :disabled="loading"
        class="!w-full !h-11 !text-base !font-bold !rounded-lg"
      >
        Đặt lại mật khẩu
      </el-button>

      <div class="text-center mt-4">
        <router-link to="/auth/login" class="text-sm text-gray-500 hover:text-blue-600 no-underline transition-colors">
          Hủy bỏ
        </router-link>
      </div>
    </el-form>
  </div>
</template>

<script setup>
// Giữ nguyên logic script, chỉ thay đổi template
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
