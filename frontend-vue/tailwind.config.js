/** @type {import('tailwindcss').Config} */
export default {
  // Quét tất cả các file có khả năng chứa class tailwind
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  // Sử dụng class "dark" trên thẻ html để kích hoạt dark mode (khớp với logic cũ của bạn)
  darkMode: 'class',
  corePlugins: {
    preflight: false,
  },
  theme: {
    extend: {
      // Map màu của Tailwind vào biến CSS của Element Plus để đồng bộ giao diện
      colors: {
        primary: 'var(--el-color-primary)',
        'primary-light': 'var(--el-color-primary-light-3)',
        'primary-dark': 'var(--el-color-primary-dark-2)',
        success: 'var(--el-color-success)',
        warning: 'var(--el-color-warning)',
        danger: 'var(--el-color-danger)',
        info: 'var(--el-color-info)',
        // Màu nền dark mode chuẩn
        dark: {
          bg: '#141414',
          surface: '#1d1d1d',
          border: '#303030'
        }
      }
    },
  },
  plugins: [],
}
