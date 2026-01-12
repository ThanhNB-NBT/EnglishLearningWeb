<template>
  <div class="w-full">
    <div class="text-center mb-6">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-blue-50 dark:bg-blue-900/30 text-blue-500 mb-4">
        <el-icon :size="32"><Message /></el-icon>
      </div>
      <h2 class="text-2xl font-bold text-gray-800 dark:text-white mb-2">Xác thực Email</h2>
      <p class="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">
        Vui lòng nhập mã OTP đã được gửi đến email:<br/>
        <strong class="text-blue-600 dark:text-blue-400 font-medium">{{ email }}</strong>
      </p>
    </div>

    <!-- ✅ Hiển thị thông báo lỗi từ backend -->
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="true"
      @close="errorMessage = ''"
      class="mb-4"
      show-icon
    />

    <!-- ✅ Hiển thị thông tin số lần thử còn lại -->
    <el-alert
      v-if="attemptsRemaining !== null && attemptsRemaining < 3"
      :title="`Còn ${attemptsRemaining} lần thử`"
      type="warning"
      :closable="false"
      class="mb-4"
      show-icon
    />

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleVerify"
      class="space-y-4"
    >
      <el-form-item label="Mã OTP" prop="otp" class="!mb-2">
        <el-input
          v-model="formData.otp"
          placeholder="Nhập mã 6 chữ số"
          :prefix-icon="Lock"
          maxlength="6"
          clearable
          :disabled="loading"
          class="!text-center tracking-widest font-mono"
          @keyup.enter="handleVerify"
          @input="errorMessage = ''"
        >
          <template #append><span class="text-xs">6 số</span></template>
        </el-input>
      </el-form-item>

      <div class="text-center h-6 mb-4">
        <div v-if="countdown > 0" class="text-sm text-gray-500 dark:text-gray-400">
          Gửi lại mã sau: <strong class="text-orange-500">{{ countdown }}s</strong>
        </div>
        <button
          v-else
          type="button"
          class="text-sm text-blue-600 dark:text-blue-400 hover:underline font-medium disabled:opacity-50"
          :disabled="resending || rateLimitReached"
          @click="handleResend"
        >
          {{ rateLimitReached ? 'Đã vượt giới hạn gửi OTP' : 'Gửi lại mã OTP' }}
        </button>
      </div>

      <el-button
        type="primary"
        native-type="submit"
        :loading="loading"
        :disabled="loading || formData.otp.length !== 6"
        class="!w-full !h-11 !text-base !font-bold !rounded-lg shadow-md shadow-blue-500/20"
      >
        Xác thực
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
import { ref, onMounted, onUnmounted, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'vue-toastification'
import { Message, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const toast = useToast()

const formRef = ref(null)
const loading = ref(false)
const resending = ref(false)
const email = ref(route.query.email || '')
const countdown = ref(60)
const errorMessage = ref('')
const attemptsRemaining = ref(null)
const rateLimitReached = ref(false)
let countdownInterval = null

const formData = reactive({ otp: '' })

const validateOtp = (rule, value, callback) => {
  if (!value) callback(new Error('Vui lòng nhập mã OTP'))
  else if (!/^\d{6}$/.test(value)) callback(new Error('OTP phải là 6 chữ số'))
  else callback()
}

const rules = { otp: [{ validator: validateOtp, trigger: 'blur' }] }

const startCountdown = () => {
  countdown.value = 60
  if (countdownInterval) clearInterval(countdownInterval)
  countdownInterval = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) clearInterval(countdownInterval)
  }, 1000)
}

const handleVerify = async () => {
  if (!formRef.value) return

  // Reset error trước khi verify
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
        await authStore.verifyEmail(email.value, formData.otp)

        // ✅ Hiển thị thông báo thành công
        toast.success('Xác thực thành công! Tài khoản đã được kích hoạt.')

        // Chờ 1 giây để user đọc thông báo rồi chuyển trang
        setTimeout(() => {
          router.push('/auth/login')
        }, 1000)

      } catch (error) {
        console.error('Verify error:', error)

        // ✅ Xử lý các loại lỗi cụ thể từ backend
        const errorMsg = error.response?.data?.message || error.message || 'Xác thực OTP thất bại'

        errorMessage.value = errorMsg
        toast.error(errorMsg)

        // ✅ Parse số lần thử còn lại từ message
        const remainingMatch = errorMsg.match(/Còn lại (\d+) lần thử/)
        if (remainingMatch) {
          attemptsRemaining.value = parseInt(remainingMatch[1])
        }

        // ✅ Nếu hết lần thử, clear form và yêu cầu gửi lại OTP
        if (errorMsg.includes('vượt quá') || errorMsg.includes('Đã vượt quá')) {
          formData.otp = ''
          attemptsRemaining.value = 0
          toast.warning('Vui lòng yêu cầu mã OTP mới')
        }

        // ✅ Nếu OTP hết hạn
        if (errorMsg.includes('hết hạn') || errorMsg.includes('không tồn tại')) {
          formData.otp = ''
          toast.warning('Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.')
        }
      } finally {
        loading.value = false
      }
    }
  })
}

const handleResend = async () => {
  if (!email.value) {
    errorMessage.value = 'Email không hợp lệ'
    toast.error('Email không hợp lệ')
    return
  }

  // Reset error và số lần thử
  errorMessage.value = ''
  attemptsRemaining.value = null
  formData.otp = ''

  resending.value = true
  try {
    await authStore.resendVerifyEmail(email.value)

    // ✅ Hiển thị thông báo thành công
    toast.success('Đã gửi lại mã OTP! Vui lòng kiểm tra email.')

    startCountdown()
  } catch (error) {
    console.error('Resend error:', error)

    // ✅ Xử lý rate limit
    const errorMsg = error.response?.data?.message || error.message || 'Gửi lại mã thất bại'

    errorMessage.value = errorMsg
    toast.error(errorMsg)

    // ✅ Nếu vượt giới hạn rate limit
    if (errorMsg.includes('vượt quá giới hạn') || errorMsg.includes('thử lại sau')) {
      rateLimitReached.value = true
      // Reset sau 1 giờ
      setTimeout(() => {
        rateLimitReached.value = false
      }, 60 * 60 * 1000)
    }
  } finally {
    resending.value = false
  }
}

onMounted(() => {
  if (!email.value) {
    toast.error('Email không được cung cấp')
    router.push('/auth/register')
    return
  }
  startCountdown()
})

onUnmounted(() => {
  if (countdownInterval) clearInterval(countdownInterval)
})
</script>

<style scoped>
/* Custom styles for better UX */
.el-input :deep(.el-input__inner) {
  text-align: center;
  letter-spacing: 0.5em;
  font-family: 'Sans Serif', monospace;
  font-size: 0.75rem;
  font-weight: 600;
}
</style>
