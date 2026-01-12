<template>
  <div class="w-full">
    <div class="text-center mb-6">
      <el-icon :size="64" :color="stepIcon.color" class="transition-colors duration-300">
        <component :is="stepIcon.icon" />
      </el-icon>
    </div>

    <!-- ✅ STEP 1: Nhập Email -->
    <template v-if="step === 1">
      <h2 class="text-2xl font-bold text-center text-gray-800 dark:text-white mb-2">
        Quên mật khẩu
      </h2>
      <p class="text-center text-gray-500 dark:text-gray-400 text-sm mb-8">
        Nhập email để nhận mã OTP đặt lại mật khẩu
      </p>

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

      <el-form
        ref="emailFormRef"
        :model="emailForm"
        :rules="emailRules"
        label-position="top"
        size="large"
        @submit.prevent="handleSendOtp"
      >
        <el-form-item label="Email" prop="email" class="!mb-6">
          <el-input
            v-model="emailForm.email"
            type="email"
            placeholder="example@email.com"
            :prefix-icon="Message"
            clearable
            :disabled="loading"
            @input="errorMessage = ''"
          />
        </el-form-item>

        <el-form-item class="!mb-0">
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            :disabled="loading"
            class="!w-full !h-11 !font-bold !rounded-lg"
          >
            Gửi mã OTP
          </el-button>
        </el-form-item>
      </el-form>
    </template>

    <!-- ✅ STEP 2: Nhập OTP -->
    <template v-else-if="step === 2">
      <h2 class="text-2xl font-bold text-center text-gray-800 dark:text-white mb-2">
        Xác thực OTP
      </h2>
      <p class="text-center text-gray-500 dark:text-gray-400 text-sm mb-8 leading-relaxed">
        Nhập mã OTP đã được gửi đến:<br />
        <strong class="text-blue-600 dark:text-blue-400">{{ emailForm.email }}</strong>
      </p>

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

      <!-- ✅ Hiển thị số lần thử còn lại -->
      <el-alert
        v-if="attemptsRemaining !== null && attemptsRemaining < 3"
        :title="`Còn ${attemptsRemaining} lần thử`"
        type="warning"
        :closable="false"
        class="mb-4"
        show-icon
      />

      <el-form
        ref="otpFormRef"
        :model="otpForm"
        :rules="otpRules"
        label-position="top"
        size="large"
        @submit.prevent="handleVerifyOtp"
      >
        <el-form-item label="Mã OTP" prop="otp" class="!mb-2">
          <el-input
            v-model="otpForm.otp"
            placeholder="Nhập mã 6 chữ số"
            :prefix-icon="Lock"
            maxlength="6"
            clearable
            :disabled="loading"
            class="text-center tracking-widest font-mono"
            @input="errorMessage = ''"
            @keyup.enter="handleVerifyOtp"
          >
            <template #append><span class="text-xs">6 chữ số</span></template>
          </el-input>
        </el-form-item>

        <div class="text-center mb-6 h-6">
          <el-text v-if="countdown > 0" type="info" size="small">
            Gửi lại mã sau: <strong class="text-orange-500">{{ countdown }}s</strong>
          </el-text>
          <el-button
            v-else
            link
            type="primary"
            :loading="resending"
            :disabled="resending || rateLimitReached"
            @click="handleSendOtp"
          >
            {{ rateLimitReached ? 'Đã vượt giới hạn' : 'Gửi lại mã OTP' }}
          </el-button>
        </div>

        <el-form-item class="!mb-0">
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            :disabled="loading || otpForm.otp.length !== 6"
            class="!w-full !h-11 !font-bold !rounded-lg"
          >
            Xác thực OTP
          </el-button>
        </el-form-item>
      </el-form>
    </template>

    <!-- ✅ STEP 3: Thành công -->
    <template v-else-if="step === 3">
      <h2 class="text-2xl font-bold text-center text-green-600 dark:text-green-400 mb-2">
        Xác thực thành công!
      </h2>
      <p class="text-center text-gray-500 dark:text-gray-400 text-sm mb-8">
        Đang chuyển hướng để đặt lại mật khẩu...
      </p>

      <div class="flex justify-center mb-6">
        <el-icon :size="64" color="#67c23a">
          <SuccessFilled />
        </el-icon>
      </div>

      <el-button
        type="success"
        size="large"
        class="!w-full !h-11 !font-bold !rounded-lg"
        @click="goToResetPassword"
      >
        Đặt lại mật khẩu ngay
      </el-button>
    </template>

    <div class="text-center mt-6 pt-4 border-t border-gray-100 dark:border-gray-800">
      <router-link
        to="/auth/login"
        class="text-sm text-gray-500 hover:text-blue-600 dark:text-gray-400 dark:hover:text-blue-400 transition-colors no-underline flex items-center justify-center gap-1"
      >
        <span class="text-lg">←</span> Quay lại đăng nhập
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted, watch, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'vue-toastification'
import { Message, Lock, Warning, SuccessFilled } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()

const emailFormRef = ref(null)
const otpFormRef = ref(null)
const loading = ref(false)
const resending = ref(false)
const step = ref(1)
const countdown = ref(0)
const errorMessage = ref('')
const attemptsRemaining = ref(null)
const rateLimitReached = ref(false)
let countdownInterval = null

const emailForm = reactive({ email: '' })
const otpForm = reactive({ otp: '' })

const validateOtp = (rule, value, callback) => {
  if (!value) callback(new Error('Vui lòng nhập mã OTP'))
  else if (!/^\d{6}$/.test(value)) callback(new Error('OTP phải là 6 chữ số'))
  else callback()
}

const emailRules = {
  email: [
    { required: true, message: 'Vui lòng nhập email', trigger: 'blur' },
    { type: 'email', message: 'Email không hợp lệ', trigger: 'blur' },
  ],
}
const otpRules = { otp: [{ validator: validateOtp, trigger: 'blur' }] }

const stepIcon = computed(() => {
  if (step.value === 1) return { icon: Warning, color: '#e6a23c' }
  if (step.value === 2) return { icon: Message, color: '#409eff' }
  return { icon: SuccessFilled, color: '#67c23a' }
})

const startCountdown = () => {
  countdown.value = 60
  if (countdownInterval) clearInterval(countdownInterval)
  countdownInterval = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) clearInterval(countdownInterval)
  }, 1000)
}

const handleSendOtp = async () => {
  if (!emailFormRef.value) return

  // Reset error
  errorMessage.value = ''

  await emailFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      resending.value = true
      try {
        await authStore.forgotPassword(emailForm.email)

        // ✅ Hiển thị thông báo thành công
        toast.success('Đã gửi mã OTP! Vui lòng kiểm tra email.')

        step.value = 2
        attemptsRemaining.value = null // Reset attempts
        otpForm.otp = '' // Clear OTP input
        startCountdown()
      } catch (error) {
        console.error('Send OTP error:', error)

        // ✅ Xử lý lỗi từ backend
        const errorMsg = error.response?.data?.message || error.message || 'Gửi OTP thất bại'

        errorMessage.value = errorMsg
        toast.error(errorMsg)

        // ✅ Xử lý rate limit
        if (errorMsg.includes('vượt quá giới hạn') || errorMsg.includes('thử lại sau')) {
          rateLimitReached.value = true
          setTimeout(
            () => {
              rateLimitReached.value = false
            },
            60 * 60 * 1000,
          ) // Reset sau 1 giờ
        }
      } finally {
        loading.value = false
        resending.value = false
      }
    }
  })
}

const handleVerifyOtp = async () => {
  if (!otpFormRef.value) return

  // Reset error
  errorMessage.value = ''

  await otpFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await authStore.verifyResetPassword(emailForm.email, otpForm.otp)

        // ✅ Hiển thị thông báo thành công
        toast.success('Xác thực thành công! Bạn có thể đặt lại mật khẩu.')

        step.value = 3
      } catch (error) {
        console.error('Verify OTP error:', error)

        // ✅ Xử lý lỗi từ backend
        const errorMsg = error.response?.data?.message || error.message || 'Xác thực OTP thất bại'

        errorMessage.value = errorMsg
        toast.error(errorMsg)

        // ✅ Parse số lần thử còn lại
        const remainingMatch = errorMsg.match(/Còn lại (\d+) lần thử/)
        if (remainingMatch) {
          attemptsRemaining.value = parseInt(remainingMatch[1])
        }

        // ✅ Nếu hết lần thử
        if (errorMsg.includes('vượt quá') || errorMsg.includes('Đã vượt quá')) {
          otpForm.otp = ''
          attemptsRemaining.value = 0
          toast.warning('Vui lòng yêu cầu mã OTP mới')
        }

        // ✅ Nếu OTP hết hạn
        if (errorMsg.includes('hết hạn') || errorMsg.includes('không tồn tại')) {
          otpForm.otp = ''
          toast.warning('Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.')
        }
      } finally {
        loading.value = false
      }
    }
  })
}

const goToResetPassword = () => {
  router.push({ path: '/auth/reset-password', query: { email: emailForm.email } })
}

watch(step, (newStep) => {
  if (newStep === 3) {
    // Tự động chuyển sang trang reset password sau 2 giây
    setTimeout(() => goToResetPassword(), 2000)
  }
})

onUnmounted(() => {
  if (countdownInterval) clearInterval(countdownInterval)
})
</script>

<style scoped>
.el-input :deep(.el-input__inner) {
  text-align: center;
  letter-spacing: 0.5em;
  font-family: 'Courier New', monospace;
  font-size: 1.25rem;
  font-weight: 600;
}
</style>
