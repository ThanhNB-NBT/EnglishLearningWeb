import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { userAPI } from '@/api/modules/user.api'
import { useToast } from 'vue-toastification'
import { useRouter } from 'vue-router'

export function useChangePassword() {
  const authStore = useAuthStore()
  const toast = useToast()
  const router = useRouter()

  const loading = ref(false)
  const formRef = ref(null)
  const formData = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  })

  // Validation rules
  const validateNewPassword = (rule, value, callback) => {
    if (!value) {
      callback(new Error('Vui lòng nhập mật khẩu mới'))
    } else if (value.length < 8) {
      callback(new Error('Mật khẩu phải có ít nhất 6 ký tự'))
    } else if (value === formData.oldPassword) {
      callback(new Error('Mật khẩu mới phải khác mật khẩu cũ'))
    } else {
      callback()
    }
  }

  const validateConfirmPassword = (rule, value, callback) => {
    if (!value) {
      callback(new Error('Vui lòng xác nhận mật khẩu mới'))
    } else if (value !== formData.newPassword) {
      callback(new Error('Mật khẩu xác nhận không khớp'))
    } else {
      callback()
    }
  }

  const rules = {
    oldPassword: [{ required: true, message: 'Vui lòng nhập mật khẩu cũ', trigger: 'blur' }],
    newPassword: [{ validator: validateNewPassword, trigger: 'blur' }],
    confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  }

  // Change password
  const changePassword = async () => {
    if (!formRef.value) return

    try {
      const valid = await formRef.value.validate()
      if (!valid) return

      loading.value = true

      const payload = {
        oldPassword: formData.oldPassword,
        newPassword: formData.newPassword,
        confirmPassword: formData.confirmPassword,
      }

      // CHỈ TRUYỀN OBJECT, KHÔNG CẦN userId
      await userAPI.changePassword(payload)

      toast.success('Đổi mật khẩu thành công! Vui lòng đăng nhập lại.')

      // Logout và redirect về login
      const currentPath = router.currentRoute.value.path
      const isAdminRoute = currentPath.startsWith('/admin')

      if (isAdminRoute) {
        console.log('🔵 Logging out admin...')
        await authStore.logoutAdmin()
        router.push('/admin/login')
      } else {
        console.log('🔵 Logging out user...')
        await authStore.logoutUser()
        router.push('/auth/login')
      }
    } catch (error) {
      // Phân biệt validation error vs API error
      if (error !== false) {
        // false = validation failed
        const message = error.response?.data?.message || 'Đổi mật khẩu thất bại'
        toast.error(message)
      }
    } finally {
      loading.value = false
    }
  }

  // Reset form
  const resetForm = () => {
    if (formRef.value) {
      formRef.value.resetFields()
    }
  }

  return {
    loading,
    formRef,
    formData,
    rules,
    changePassword,
    resetForm,
  }
}
