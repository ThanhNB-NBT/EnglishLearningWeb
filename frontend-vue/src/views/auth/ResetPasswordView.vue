<template>
  <div class="w-full">
    <div class="text-center mb-6">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-green-50 dark:bg-green-900/30 text-green-500 mb-4">
        <el-icon :size="32"><Lock /></el-icon>
      </div>
      <h2 class="text-2xl font-bold text-gray-800 dark:text-white mb-2">Đặt lại mật khẩu</h2>
      <p class="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">
        Nhập mật khẩu mới cho tài khoản:<br/>
        <strong class="text-blue-600 dark:text-blue-400 font-medium">{{ email }}</strong>
      </p>
    </div>

    <!-- ✅ Hiển thị lỗi -->
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="true"
      @close="errorMessage = ''"
      class="mb-4"
      show-icon
    />

    <!-- ✅ Yêu cầu mật khẩu -->
    <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4 mb-4">
      <h3 class="text-sm font-semibold text-blue-900 dark:text-blue-300 mb-2">Yêu cầu mật khẩu:</h3>
      <ul class="text-xs text-blue-800 dark:text-blue-400 space-y-1">
        <li :class="passwordChecks.length ? 'text-green-600' : ''">
          <el-icon :size="14" class="mr-1">
            <component :is="passwordChecks.length ? 'CircleCheck' : 'CircleClose'" />
          </el-icon>
          Ít nhất 8 ký tự
        </li>
        <li :class="passwordChecks.uppercase ? 'text-green-600' : ''">
          <el-icon :size="14" class="mr-1">
            <component :is="passwordChecks.uppercase ? 'CircleCheck' : 'CircleClose'" />
          </el-icon>
          Có ít nhất 1 chữ in hoa
        </li>
        <li :class="passwordChecks.lowercase ? 'text-green-600' : ''">
          <el-icon :size="14" class="mr-1">
            <component :is="passwordChecks.lowercase ? 'CircleCheck' : 'CircleClose'" />
          </el-icon>
          Có ít nhất 1 chữ thường
        </li>
        <li :class="passwordChecks.number ? 'text-green-600' : ''">
          <el-icon :size="14" class="mr-1">
            <component :is="passwordChecks.number ? 'CircleCheck' : 'CircleClose'" />
          </el-icon>
          Có ít nhất 1 chữ số
        </li>
      </ul>
    </div>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleReset"
      class="space-y-4"
    >
      <el-form-item label="Mật khẩu mới" prop="newPassword" class="!mb-4">
        <el-input
          v-model="formData.newPassword"
          type="password"
          placeholder="Nhập mật khẩu mới"
          :prefix-icon="Lock"
          show-password
          clearable
          :disabled="loading"
          @input="errorMessage = ''; checkPassword()"
        />
      </el-form-item>

      <el-form-item label="Xác nhận mật khẩu" prop="confirmPassword" class="!mb-6">
        <el-input
          v-model="formData.confirmPassword"
          type="password"
          placeholder="Nhập lại mật khẩu mới"
          :prefix-icon="Lock"
          show-password
          clearable
          :disabled="loading"
          @input="errorMessage = ''"
          @keyup.enter="handleReset"
        />
      </el-form-item>

      <el-button
        type="primary"
        native-type="submit"
        :loading="loading"
        :disabled="loading || !isPasswordValid"
        class="!w-full !h-11 !text-base !font-bold !rounded-lg shadow-md shadow-green-500/20"
      >
        Đặt lại mật khẩu
      </el-button>

      <div class="text-center mt-6 pt-4 border-t border-gray-100 dark:border-gray-800">
        <router-link to="/auth/login" class="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-blue-600 dark:text-gray-400 dark:hover:text-blue-400 transition-colors no-underline">
          <span>←</span> Quay lại đăng nhập
        </router-link>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'vue-toastification'
import { Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const toast = useToast()

const formRef = ref(null)
const loading = ref(false)
const email = ref(route.query.email || '')
const errorMessage = ref('')

const formData = reactive({
  newPassword: '',
  confirmPassword: ''
})

// ✅ Kiểm tra yêu cầu mật khẩu real-time
const passwordChecks = reactive({
  length: false,
  uppercase: false,
  lowercase: false,
  number: false
})

const checkPassword = () => {
  const pwd = formData.newPassword
  passwordChecks.length = pwd.length >= 8
  passwordChecks.uppercase = /[A-Z]/.test(pwd)
  passwordChecks.lowercase = /[a-z]/.test(pwd)
  passwordChecks.number = /\d/.test(pwd)
}

const isPasswordValid = computed(() => {
  return passwordChecks.length &&
         passwordChecks.uppercase &&
         passwordChecks.lowercase &&
         passwordChecks.number &&
         formData.newPassword === formData.confirmPassword &&
         formData.confirmPassword.length > 0
})

const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Vui lòng nhập mật khẩu mới'))
  } else if (value.length < 8) {
    callback(new Error('Mật khẩu phải có ít nhất 8 ký tự'))
  } else if (!/[A-Z]/.test(value)) {
    callback(new Error('Mật khẩu phải có ít nhất 1 chữ in hoa'))
  } else if (!/[a-z]/.test(value)) {
    callback(new Error('Mật khẩu phải có ít nhất 1 chữ thường'))
  } else if (!/\d/.test(value)) {
    callback(new Error('Mật khẩu phải có ít nhất 1 chữ số'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Vui lòng xác nhận mật khẩu'))
  } else if (value !== formData.newPassword) {
    callback(new Error('Mật khẩu xác nhận không khớp'))
  } else {
    callback()
  }
}

const rules = {
  newPassword: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const handleReset = async () => {
  if (!formRef.value) return

  // Reset error
  errorMessage.value = ''

  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (!email.value) {
        errorMessage.value = 'Email không hợp lệ'
        toast.error('Email không hợp lệ')
        return
      }

      loading.value = true
      try {
        await authStore.resetPassword(email.value, formData.newPassword)

        // ✅ Hiển thị thông báo thành công
        toast.success('Đặt lại mật khẩu thành công! Vui lòng đăng nhập.')

        // Chờ 1 giây rồi chuyển về trang login
        setTimeout(() => {
          router.push('/auth/login')
        }, 1000)

      } catch (error) {
        console.error('Reset password error:', error)

        // ✅ Xử lý lỗi từ backend
        const errorMsg = error.response?.data?.message || error.message || 'Đặt lại mật khẩu thất bại'

        errorMessage.value = errorMsg
        toast.error(errorMsg)

        // ✅ Nếu session hết hạn, chuyển về forgot password
        if (errorMsg.includes('hết hạn') || errorMsg.includes('không hợp lệ')) {
          toast.warning('Phiên xác thực đã hết hạn. Vui lòng thực hiện lại.')
          setTimeout(() => {
            router.push('/auth/forgot-password')
          }, 2000)
        }
      } finally {
        loading.value = false
      }
    }
  })
}

// Kiểm tra email có tồn tại không
if (!email.value) {
  toast.error('Email không được cung cấp. Vui lòng thực hiện lại quy trình quên mật khẩu.')
  setTimeout(() => {
    router.push('/auth/forgot-password')
  }, 2000)
}
</script>

<style scoped>
.el-icon {
  vertical-align: middle;
}
</style>
