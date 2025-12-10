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
          class="!text-center tracking-widest font-mono"
          @keyup.enter="handleVerify"
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
          :disabled="resending"
          @click="handleResend"
        >
          Gửi lại mã OTP
        </button>
      </div>

      <el-button
        type="primary"
        native-type="submit"
        :loading="loading"
        :disabled="loading"
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
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (!email.value) { toast.error('Email không hợp lệ'); return }
      loading.value = true
      try {
        await authStore.verifyEmail(email.value, formData.otp)
        // toast.success('Tài khoản đã được kích hoạt! Vui lòng đăng nhập.') // Store đã toast rồi
        router.push('/auth/login')
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}

const handleResend = async () => {
  if (!email.value) { toast.error('Email không hợp lệ'); return }
  resending.value = true
  try {
    await authStore.resendVerifyEmail(email.value)
    startCountdown()
  } catch (error) {
    console.error(error)
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

onUnmounted(() => { if (countdownInterval) clearInterval(countdownInterval) })
</script>
