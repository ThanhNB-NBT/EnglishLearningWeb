<template>
  <div class="h-screen w-full flex flex-col bg-white dark:bg-[#121212] overflow-hidden">
    <header
      class="h-14 shrink-0 border-b border-gray-200 dark:border-gray-800 flex items-center justify-between px-4 bg-white dark:bg-[#1a1a1a] z-20"
    >
      <div class="flex items-center gap-3 min-w-0">
        <slot name="header-left"></slot>
      </div>

      <div class="absolute left-1/2 -translate-x-1/2 flex items-center gap-4">
        <slot name="header-center"></slot>
      </div>

      <div class="flex items-center gap-2">
        <slot name="header-right"></slot>

        <el-button text circle @click="handleToggleSidebar">
          <el-icon :size="20">
            <MenuIcon v-if="isMobile" />
            <template v-else> <Expand v-if="!isSidebarOpen" /> <Fold v-else /> </template>
          </el-icon>
        </el-button>
      </div>
    </header>

    <div class="flex-1 flex overflow-hidden relative">
      <main
        class="flex-1 flex flex-col md:flex-row relative overflow-hidden transition-all duration-300"
      >
        <div
          v-if="mode === 'full'"
          class="w-full h-full overflow-y-auto custom-scrollbar p-4 md:p-8 bg-white dark:bg-[#121212] relative"
        >
          <div class="max-w-4xl mx-auto h-full">
            <slot name="content-full"></slot>
          </div>
        </div>

        <template v-else>
          <div
            class="h-full overflow-y-auto custom-scrollbar border-r border-gray-200 dark:border-gray-800 md:w-1/2 lg:w-5/12 bg-white dark:bg-[#161616] p-6"
          >
            <slot name="content-left"></slot>
          </div>
          <div
            class="h-full overflow-y-auto custom-scrollbar md:w-1/2 lg:w-7/12 bg-gray-50 dark:bg-[#121212] p-6 pb-24"
          >
            <slot name="content-right"></slot>
          </div>
        </template>
      </main>

      <aside
        class="hidden lg:block shrink-0 bg-gray-50 dark:bg-[#161616] border-l border-gray-200 dark:border-gray-800 overflow-hidden transition-all duration-300 ease-in-out"
        :class="isSidebarOpen ? 'w-80' : 'w-0 border-none'"
      >
        <div class="w-80 h-full overflow-y-auto custom-scrollbar">
          <slot name="sidebar"></slot>
        </div>
      </aside>

      <el-drawer v-model="showMobileDrawer" direction="rtl" size="85%" :with-header="false">
        <div class="h-full overflow-y-auto custom-scrollbar">
          <slot name="sidebar"></slot>
        </div>
      </el-drawer>
    </div>

    <footer
      class="h-16 shrink-0 border-t border-gray-200 dark:border-gray-800 bg-white dark:bg-[#1a1a1a] flex items-center justify-between px-6 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)] z-30"
    >
      <slot name="footer"></slot>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Menu as MenuIcon, Expand, Fold } from '@element-plus/icons-vue'

defineProps({
  mode: { type: String, default: 'full' },
})

const isSidebarOpen = ref(true)
const showMobileDrawer = ref(false)
const windowWidth = ref(window.innerWidth)
const isMobile = computed(() => windowWidth.value < 1024)

// ✅ FIX: Debounce để tránh infinite loop
let resizeTimeout = null
const handleResize = () => {
  // Clear timeout cũ
  if (resizeTimeout) clearTimeout(resizeTimeout)

  // Debounce 150ms
  resizeTimeout = setTimeout(() => {
    const newWidth = window.innerWidth

    // ✅ CHỈ update khi thực sự thay đổi
    if (windowWidth.value !== newWidth) {
      windowWidth.value = newWidth

      // ✅ Update sidebar state dựa trên breakpoint
      if (newWidth < 1024 && isSidebarOpen.value) {
        isSidebarOpen.value = false
      } else if (newWidth >= 1024 && !isSidebarOpen.value) {
        isSidebarOpen.value = true
      }
    }
  }, 150)
}

onMounted(() => {
  window.addEventListener('resize', handleResize)

  // ✅ FIX: Set initial state KHÔNG gọi handleResize()
  // Điều này tránh trigger setTimeout ngay trong onMounted
  // → Tránh race condition với parent component
  const currentWidth = window.innerWidth
  windowWidth.value = currentWidth

  // Set sidebar state based on initial width
  if (currentWidth < 1024) {
    isSidebarOpen.value = false
  } else {
    isSidebarOpen.value = true
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (resizeTimeout) clearTimeout(resizeTimeout)
})

const handleToggleSidebar = () => {
  if (isMobile.value) {
    showMobileDrawer.value = true
  } else {
    isSidebarOpen.value = !isSidebarOpen.value
  }
}
</script>

<style scoped>
/* Custom scrollbar */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

html.dark .custom-scrollbar::-webkit-scrollbar-thumb {
  background: #374151;
}

html.dark .custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #4b5563;
}
</style>
