<script setup>
  import { useAuthStore } from '@/stores/auth'
  import { onMounted } from 'vue'

  const authStore = useAuthStore()

  onMounted(() => {
    authStore.checkAuth()
  })
</script>

<template>
  <router-view />
</template>

<style scoped>
*{
  transition: background-color 0.3s ease, border-color 0.3s ease;
}
</style>
