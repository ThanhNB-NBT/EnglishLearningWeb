import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { useToast } from 'vue-toastification'

export function useResetPassword(email) {
  const authStore = useAuthStore()
  const router = useRouter()
  const toast = useToast()

  const loading = ref(false)
  const formRef = ref(null)
  const formData = ref({
    newPassword: '',
    confirmPassword: '',
  })

  // Validation
  const validateNewPassword = (rule, value, callback) => {
    if (!value) {
      callback(new Error('Vui lòng nhập mật khẩu mới'))
    } else if (value.length < 6) {
      callback(new Error('Mật khẩu phải có ít nhất 6 ký tự'))
    } else {
      callback()
    }
  }

  const validateConfirmPassword = (rule, value, callback) => {
    if (!value) {
      callback(new Error('Vui lòng xác nhận mật khẩu'))
    } else if (value !== formData.value.newPassword) {
      callback(new Error('Mật khẩu xác nhận không khớp'))
    } else {
      callback()
    }
  }

  const rules = {
    newPassword: [{ validator: validateNewPassword, trigger: 'blur' }],
    confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  }

  // Reset password
  const resetPassword = async () => {
    if (!formRef.value) return

    await formRef.value.validate(async (valid) => {
      if (!valid) return

      if (!email) {
        toast.error('Email không hợp lệ')
        return
      }

      loading.value = true
      try {
        await authStore.resetPassword(email, formData.value.newPassword)

        toast.success('Đặt lại mật khẩu thành công! Vui lòng đăng nhập.')
        router.push('/auth/login')
      } catch (error) {
        console.error('Reset password failed:', error)
      } finally {
        loading.value = false
      }
    })
  }

  return {
    loading,
    formRef,
    formData,
    rules,
    resetPassword,
  }
}
