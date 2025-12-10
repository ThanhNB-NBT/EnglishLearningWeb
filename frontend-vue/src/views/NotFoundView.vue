<template>
  <div class="h-screen w-full flex flex-col items-center justify-center bg-gray-50 dark:bg-dark-bg p-4 text-center">
    <div class="text-9xl font-black text-gray-200 dark:text-gray-800 mb-4 select-none">404</div>
    <h1 class="text-3xl font-bold text-gray-900 dark:text-white mb-2">Trang không tìm thấy</h1>
    <p class="text-gray-500 dark:text-gray-400 mb-8 max-w-md">
      Xin lỗi, trang bạn đang tìm kiếm không tồn tại hoặc đã bị di chuyển.
    </p>
    <el-button type="primary" size="large" @click="$router.push('/')" class="!px-8 !rounded-full !font-bold">
      Về trang chủ
    </el-button>
  </div>
</template>
