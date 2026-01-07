<template>
  <div class="max-w-md mx-auto px-4 py-12">
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-8">
      <h2 class="text-2xl font-bold text-center mb-8 text-gray-900 dark:text-white">
        Đổi mật khẩu
      </h2>

      <form @submit.prevent="changePassword" class="space-y-6">
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Mật khẩu hiện tại
          </label>
          <input
            v-model="form.currentPassword"
            type="password"
            class="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-white focus:ring-2 focus:ring-blue-500"
            :class="{ 'border-red-500': errors.currentPassword }"
            placeholder="Nhập mật khẩu cũ"
          />
          <p v-if="errors.currentPassword" class="mt-1 text-sm text-red-500">
            {{ errors.currentPassword }}
          </p>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Mật khẩu mới
          </label>
          <input
            v-model="form.newPassword"
            type="password"
            class="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-white focus:ring-2 focus:ring-blue-500"
            :class="{ 'border-red-500': errors.newPassword }"
            placeholder="Tối thiểu 8 ký tự"
          />
          <p v-if="errors.newPassword" class="mt-1 text-sm text-red-500">
            {{ errors.newPassword }}
          </p>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Xác nhận mật khẩu mới
          </label>
          <input
            v-model="form.confirmPassword"
            type="password"
            class="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-white focus:ring-2 focus:ring-blue-500"
            :class="{ 'border-red-500': errors.confirmPassword }"
            placeholder="Nhập lại mật khẩu mới"
          />
          <p v-if="errors.confirmPassword" class="mt-1 text-sm text-red-500">
            {{ errors.confirmPassword }}
          </p>
        </div>

        <div class="flex gap-4 pt-4">
          <button
            type="submit"
            :disabled="loading"
            class="flex-1 bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:opacity-50 flex justify-center items-center"
          >
            <span v-if="loading" class="animate-spin mr-2">⌛</span>
            Lưu mật khẩu mới
          </button>

          <router-link
            to="/profile"
            class="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-gray-700"
          >
            Hủy
          </router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { useChangePassword } from '@/composables/auth/useChangePassword'

const { form, errors, loading, changePassword } = useChangePassword()
</script>
