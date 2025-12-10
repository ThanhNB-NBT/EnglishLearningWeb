<template>
  <div class="w-full">
    <div class="text-center mb-6">
      <el-icon :size="64" :color="stepIcon.color" class="transition-colors duration-300">
        <component :is="stepIcon.icon" />
      </el-icon>
    </div>

    <template v-if="step === 1">
      <h2 class="text-2xl font-bold text-center text-gray-800 mb-2">Quên mật khẩu</h2>
      <p class="text-center text-gray-500 text-sm mb-8">
        Nhập email để nhận mã OTP đặt lại mật khẩu
      </p>

      <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-position="top" size="large" @submit.prevent="handleSendOtp">
        <el-form-item label="Email" prop="email" class="!mb-6">
          <el-input v-model="emailForm.email" type="email" placeholder="example@email.com" :prefix-icon="Message" clearable />
        </el-form-item>

        <el-form-item class="!mb-0">
          <el-button type="primary" native-type="submit" :loading="loading" class="!w-full !h-11 !font-bold !rounded-lg">
            Gửi mã OTP
          </el-button>
        </el-form-item>
      </el-form>
    </template>

    <template v-else-if="step === 2">
      <h2 class="text-2xl font-bold text-center text-gray-800 mb-2">Xác thực OTP</h2>
      <p class="text-center text-gray-500 text-sm mb-8 leading-relaxed">
        Nhập mã OTP đã được gửi đến:<br />
        <strong class="text-blue-600">{{ emailForm.email }}</strong>
      </p>

      <el-form ref="otpFormRef" :model="otpForm" :rules="otpRules" label-position="top" size="large" @submit.prevent="handleVerifyOtp">
        <el-form-item label="Mã OTP" prop="otp" class="!mb-2">
          <el-input
            v-model="otpForm.otp"
            placeholder="Nhập mã 6 chữ số"
            :prefix-icon="Lock"
            maxlength="6"
            clearable
            class="text-center"
          >
            <template #append><span class="text-xs">6 chữ số</span></template>
          </el-input>
        </el-form-item>

        <div class="text-center mb-6 h-6">
          <el-text v-if="countdown > 0" type="info" size="small">
            Gửi lại mã sau: <strong class="text-orange-500">{{ countdown }}s</strong>
          </el-text>
          <el-button v-else link type="primary" :loading="resending" @click="handleSendOtp">
            Gửi lại mã OTP
          </el-button>
        </div>

        <el-form-item class="!mb-0">
          <el-button type="primary" native-type="submit" :loading="loading" class="!w-full !h-11 !font-bold !rounded-lg">
            Xác thực OTP
          </el-button>
        </el-form-item>
      </el-form>
    </template>

    <template v-else-if="step === 3">
      <h2 class="text-2xl font-bold text-center text-green-600 mb-2">Xác thực thành công!</h2>
      <p class="text-center text-gray-500 text-sm mb-8">
        Đang chuyển hướng để đặt lại mật khẩu...
      </p>
      <el-button type="success" size="large" class="!w-full !h-11 !font-bold !rounded-lg" @click="goToResetPassword">
        Đặt lại mật khẩu ngay
      </el-button>
    </template>

    <div class="text-center mt-6 pt-4 border-t border-gray-100">
      <router-link to="/auth/login" class="text-sm text-gray-500 hover:text-blue-600 transition-colors no-underline flex items-center justify-center gap-1">
        <span class="text-lg">←</span> Quay lại đăng nhập
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted, watch, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { Message, Lock, Warning, SuccessFilled } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const authStore = useAuthStore()

const emailFormRef = ref(null)
const otpFormRef = ref(null)
const loading = ref(false)
const resending = ref(false)
const step = ref(1)
const countdown = ref(0)
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
  ]
}
const otpRules = { otp: [{ validator: validateOtp, trigger: 'blur' }] }

const stepIcon = computed(() => {
  if (step.value === 1) return { icon: Warning, color: '#e6a23c' } // Cam
  if (step.value === 2) return { icon: Message, color: '#409eff' } // Xanh
  return { icon: SuccessFilled, color: '#67c23a' } // Lục
})

const startCountdown = () => {
  countdown.value = 60
  countdownInterval = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) clearInterval(countdownInterval)
  }, 1000)
}

const handleSendOtp = async () => {
  if (!emailFormRef.value) return
  await emailFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      resending.value = true
      try {
        await authStore.forgotPassword(emailForm.email)
        step.value = 2
        startCountdown()
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
        resending.value = false
      }
    }
  })
}

const handleVerifyOtp = async () => {
  if (!otpFormRef.value) return
  await otpFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await authStore.verifyResetPassword(emailForm.email, otpForm.otp)
        step.value = 3
      } catch (error) {
        console.error(error)
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
  if (newStep === 3) setTimeout(() => goToResetPassword(), 1500)
})

onUnmounted(() => { if (countdownInterval) clearInterval(countdownInterval) })
</script>
