import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { userAPI } from '@/api/modules/user.api'
import { useToast } from 'vue-toastification'

export function useProfile() {
  const authStore = useAuthStore()
  const toast = useToast()

  const loading = ref(false)
  const formRef = ref(null)

  // Fetch user profile
  const fetchProfile = async () => {
    loading.value = true
    try {
      const response = await userAPI.getCurrentUser()
      return response.data.data
    } catch (error) {
      const message = error.response?.data?.message || 'Lỗi khi tải thông tin'
      toast.error(message)
      throw error
    } finally {
      loading.value = false
    }
  }

  // Update profile (chỉ fullName)
  const updateProfile = async (fullName) => {
    loading.value = true
    try {
      const response = await userAPI.updateCurrentUser({ fullName })
      toast.success('Cập nhật thông tin thành công!')

      // Update store
      const updatedUser = response.data.data
      if (authStore.currentUser) {
        authStore.user.fullName = updatedUser.fullName
        localStorage.setItem('user', JSON.stringify(authStore.user))
      } else if (authStore.currentAdmin) {
        authStore.admin.fullName = updatedUser.fullName
        localStorage.setItem('admin', JSON.stringify(authStore.admin))
      }

      return updatedUser
    } catch (error) {
      const message = error.response?.data?.message || 'Cập nhật thất bại'
      toast.error(message)
      throw error
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    formRef,
    fetchProfile,
    updateProfile,
  }
}
