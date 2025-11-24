<template>
  <div class="auth-page">
    <!-- Icon -->
    <div class="icon-wrapper">
      <el-icon :size="64" :color="stepIcon.color">
        <component :is="stepIcon.icon" />
      </el-icon>
    </div>

    <!-- Step 1: Nhập Email -->
    <template v-if="step === 1">
      <h2 class="auth-title">Quên mật khẩu</h2>
      <p class="auth-subtitle">
        Nhập email để nhận mã OTP đặt lại mật khẩu
      </p>

      <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-position="top" size="large"
        @submit.prevent="handleSendOtp">
        <el-form-item label="Email" prop="email">
          <el-input v-model="emailForm.email" type="email" placeholder="example@email.com" :prefix-icon="Message"
            clearable @keyup.enter="handleSendOtp" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" :disabled="loading" style="width: 100%"
            size="large">
            Gửi mã OTP
          </el-button>
        </el-form-item>
      </el-form>
    </template>

    <!-- Step 2: Xác thực OTP -->
    <template v-else-if="step === 2">
      <h2 class="auth-title">Xác thực OTP</h2>
      <p class="auth-subtitle">
        Nhập mã OTP đã được gửi đến: <br />
        <strong>{{ emailForm.email }}</strong>
      </p>

      <el-form ref="otpFormRef" :model="otpForm" :rules="otpRules" label-position="top" size="large"
        @submit.prevent="handleVerifyOtp">
        <el-form-item label="Email">
          <el-input v-model="emailForm.email" :prefix-icon="Message" readonly disabled />
        </el-form-item>

        <el-form-item label="Mã OTP" prop="otp">
          <el-input v-model="otpForm.otp" placeholder="Nhập mã 6 chữ số" :prefix-icon="Lock" maxlength="6" clearable
            @keyup.enter="handleVerifyOtp">
            <template #append>
              <span class="otp-hint">6 chữ số</span>
            </template>
          </el-input>
        </el-form-item>

        <!-- Countdown Timer / Resend Button -->
        <div class="resend-wrapper">
          <template v-if="countdown > 0">
            <el-text type="info">
              Gửi lại mã sau: <strong>{{ countdown }}s</strong>
            </el-text>
          </template>
          <template v-else>
            <el-button link type="primary" :loading="resending" :disabled="resending" @click="handleSendOtp">
              Gửi lại mã OTP
            </el-button>
          </template>
        </div>

        <el-form-item style="margin-top: 32px">
          <el-button type="primary" native-type="submit" :loading="loading" :disabled="loading" style="width: 100%"
            size="large">
            Xác thực OTP
          </el-button>
        </el-form-item>
      </el-form>
    </template>

    <template v-else-if="step === 3">
      <h2 class="auth-title">Xác thực thành công!</h2>
      <p class="auth-subtitle">
        Mã OTP hợp lệ. Bạn sẽ được chuyển sang trang đặt lại mật khẩu...
      </p>

      <el-button type="primary" size="large" style="width: 100%" @click="goToResetPassword">
        Đặt lại mật khẩu ngay
      </el-button>
    </template>

    <!-- Back to Login -->
    <div class="form-link" style="margin-top: 24px">
      <router-link to="/auth/login" class="link-primary">
        ← Quay lại đăng nhập
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { Message, Lock, Warning, SuccessFilled } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const authStore = useAuthStore()

const emailFormRef = ref(null)
const otpFormRef = ref(null)
const loading = ref(false)
const resending = ref(false)
const step = ref(1) // 1: Email, 2: OTP, 3: Success
const countdown = ref(0)
let countdownInterval = null

const emailForm = ref({
  email: '',
})

const otpForm = ref({
  otp: '',
})

const validateOtp = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Vui lòng nhập mã OTP'))
  } else if (!/^\d{6}$/.test(value)) {
    callback(new Error('OTP phải là 6 chữ số'))
  } else {
    callback()
  }
}

const emailRules = {
  email: [
    { required: true, message: 'Vui lòng nhập email', trigger: 'blur' },
    { type: 'email', message: 'Email không hợp lệ', trigger: 'blur' },
  ],
}

const otpRules = {
  otp: [{ validator: validateOtp, trigger: 'blur' }],
}

const stepIcon = computed(() => {
  if (step.value === 1) {
    return { icon: Warning, color: '#e6a23c' }
  } else if (step.value === 2) {
    return { icon: Message, color: '#409eff' }
  } else {
    return { icon: SuccessFilled, color: '#67c23a' }
  }
})

const startCountdown = () => {
  countdown.value = 60
  countdownInterval = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownInterval)
    }
  }, 1000)
}

const handleSendOtp = async () => {
  if (!emailFormRef.value) return

  await emailFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    resending.value = true
    try {
      await authStore.forgotPassword(emailForm.value.email)
      step.value = 2
      startCountdown()
    } catch (error) {
      console.error('Send OTP failed:', error)
    } finally {
      loading.value = false
      resending.value = false
    }
  })
}

const handleVerifyOtp = async () => {
  if (!otpFormRef.value) return

  await otpFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await authStore.verifyResetPassword(emailForm.value.email, otpForm.value.otp)
      step.value = 3
    } catch (error) {
      console.error('Verify OTP failed:', error)
    } finally {
      loading.value = false
    }
  })
}

const goToResetPassword = () => {
  router.push({
    path: '/auth/reset-password',
    query: { email: emailForm.value.email }
  })
}

// Tự động chuyển sau 2 giây khi step = 3
watch(step, (newStep) => {
  if (newStep === 3) {
    setTimeout(() => {
      goToResetPassword()
    }, 2000)
  }
})

onUnmounted(() => {
  if (countdownInterval) {
    clearInterval(countdownInterval)
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
  color: #409eff;
}

.otp-hint {
  font-size: 12px;
  color: #909399;
}

.resend-wrapper {
  text-align: center;
  margin-bottom: 16px;
  min-height: 24px;
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
