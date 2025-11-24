<template>
  <div class="auth-page">
    <!-- Icon -->
    <div class="icon-wrapper">
      <el-icon :size="64" color="#409eff">
        <Message />
      </el-icon>
    </div>

    <h2 class="auth-title">Xác thực Email</h2>
    <p class="auth-subtitle">
      Vui lòng nhập mã OTP đã được gửi đến email: <br />
      <strong>{{ email }}</strong>
    </p>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleVerify"
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

      <!-- OTP Input -->
      <el-form-item label="Mã OTP" prop="otp">
        <el-input
          v-model="formData.otp"
          placeholder="Nhập mã 6 chữ số"
          :prefix-icon="Lock"
          maxlength="6"
          clearable
          @keyup.enter="handleVerify"
        >
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
          <el-button
            link
            type="primary"
            :loading="resending"
            :disabled="resending"
            @click="handleResend"
          >
            Gửi lại mã OTP
          </el-button>
        </template>
      </div>

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
          Xác thực
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
import { ref, onMounted, onUnmounted } from 'vue'
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
let countdownInterval = null

const formData = ref({
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

const rules = {
  otp: [{ validator: validateOtp, trigger: 'blur' }],
}

const startCountdown = () => {
  countdown.value = 60
  countdownInterval = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownInterval)
    }
  }, 1000)
}

const handleVerify = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    if (!email.value) {
      toast.error('Email không hợp lệ')
      return
    }

    loading.value = true
    try {
      await authStore.verifyEmail(email.value, formData.value.otp)

      // Xác thực thành công -> chuyển về login
      toast.success('Tài khoản đã được kích hoạt! Vui lòng đăng nhập.')
      router.push({ name: 'login' })
    } catch (error) {
      console.error('Verify failed:', error)
    } finally {
      loading.value = false
    }
  })
}

const handleResend = async () => {
  if (!email.value) {
    toast.error('Email không hợp lệ')
    return
  }

  resending.value = true
  try {
    await authStore.resendVerifyEmail(email.value)
    startCountdown()
  } catch (error) {
    console.error('Resend failed:', error)
  } finally {
    resending.value = false
  }
}

onMounted(() => {
  if (!email.value) {
    toast.error('Email không được cung cấp')
    router.push({ name: 'register' })
    return
  }
  startCountdown()
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
