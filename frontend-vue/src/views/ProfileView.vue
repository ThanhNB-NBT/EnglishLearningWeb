<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
      <div class="h-32 bg-blue-600 relative">
        <div class="absolute -bottom-12 left-8">
          <div class="w-24 h-24 rounded-full bg-white dark:bg-gray-700 p-1 shadow-lg">
            <img
              :src="avatarUrl"
              alt="Avatar"
              class="w-full h-full rounded-full bg-gray-200 object-cover"
            />
          </div>
        </div>
      </div>

      <div class="pt-16 px-8 pb-8">
        <div class="flex justify-between items-start mb-8">
          <div>
            <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
              {{ userProfile.fullName || userProfile.username }}
            </h1>
            <p class="text-gray-500 dark:text-gray-400">
              {{ userProfile.email }}
            </p>
            <div class="mt-2 flex gap-2">
              <span class="px-2 py-1 text-xs font-semibold rounded bg-blue-100 text-blue-800">
                {{ userProfile.role }}
              </span>
              <span class="px-2 py-1 text-xs font-semibold rounded bg-green-100 text-green-800">
                Level: {{ userProfile.level }}
              </span>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div class="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg text-center">
            <div class="text-3xl font-bold text-blue-600 mb-1">
              {{ userProfile.totalPoints }}
            </div>
            <div class="text-sm text-gray-500 dark:text-gray-300">Tổng điểm</div>
          </div>

          <div class="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg text-center">
            <div class="text-3xl font-bold text-orange-500 mb-1">
              {{ userProfile.streakDays }} 🔥
            </div>
            <div class="text-sm text-gray-500 dark:text-gray-300">Chuỗi ngày (Streak)</div>
          </div>

          <div class="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg text-center">
            <div class="text-lg font-medium text-gray-700 dark:text-gray-200 mb-1 mt-1">
              {{ userProfile.lastLogin }}
            </div>
            <div class="text-sm text-gray-500 dark:text-gray-300 mt-2">Đăng nhập lần cuối</div>
          </div>
        </div>

        <form @submit.prevent="updateProfile" class="space-y-6 max-w-lg">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Họ và tên
            </label>
            <input
              v-model="userProfile.fullName"
              type="text"
              class="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-white"
              placeholder="Nhập họ tên của bạn"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Username (Không thể đổi)
            </label>
            <input
              v-model="userProfile.username"
              type="text"
              disabled
              class="w-full px-3 py-2 border rounded-md bg-gray-100 dark:bg-gray-600 dark:border-gray-500 text-gray-500 cursor-not-allowed"
            />
          </div>

          <div class="flex gap-4">
            <button
              type="submit"
              :disabled="loading"
              class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 flex items-center gap-2"
            >
              <span v-if="loading" class="animate-spin">⌛</span>
              Lưu thay đổi
            </button>

            <router-link
              :to="{ name: changePasswordRouteName }"
              class="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              Đổi mật khẩu
            </router-link>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useProfile } from '@/composables/auth/useProfile'
import { getAvatarUrl } from '@/utils/avatar'

const authStore = useAuthStore()
const { userProfile, loading, updateProfile } = useProfile()

const changePasswordRouteName = computed(() => {
  if (authStore.isAdminAuthenticated) return 'admin-change-password'
  if (authStore.isTeacherAuthenticated) return 'teacher-change-password'
  return 'user-change-password' // Mặc định cho User
})

const avatarUrl = computed(() => {
  const name = userProfile.value?.fullName || userProfile.value?.username
  return getAvatarUrl(name)
})
</script>
