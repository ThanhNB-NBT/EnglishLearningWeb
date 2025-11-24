import { ref, watch, onMounted } from 'vue'

export function useDarkMode() {
  const isDark = ref(false)

  // Load từ localStorage
  const loadTheme = () => {
    const savedTheme = localStorage.getItem('theme')
    isDark.value = savedTheme === 'dark'
    applyTheme()
  }

  // Apply theme vào DOM và Element Plus
  const applyTheme = () => {
    const html = document.documentElement

    if (isDark.value) {
      html.classList.add('dark')
      // Element Plus dark mode
      html.classList.add('el-dark')
    } else {
      html.classList.remove('dark')
      html.classList.remove('el-dark')
    }
  }

  // Toggle theme
  const toggleTheme = () => {
    isDark.value = !isDark.value
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
    applyTheme()
  }

  // Auto-apply khi mount
  onMounted(() => {
    loadTheme()
  })

  // Watch changes
  watch(isDark, (newVal) => {
    localStorage.setItem('theme', newVal ? 'dark' : 'light')
    applyTheme()
  })

  return {
    isDark,
    toggleTheme,
  }
}
