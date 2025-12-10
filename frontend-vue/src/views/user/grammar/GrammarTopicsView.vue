<template>
  <div class="min-h-screen bg-gray-50 dark:bg-[#141414] p-4 md:p-8">
    <div class="w-full">
      <div class="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
        <div>
          <h1 class="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-1">Chủ đề Ngữ pháp</h1>
          <p class="text-gray-500 dark:text-gray-400">Chọn chủ đề để bắt đầu học ngay</p>
        </div>

        <el-radio-group v-model="filterLevel" size="large" class="!bg-white dark:!bg-[#1d1d1d] !p-1 !rounded-lg !shadow-sm !border !border-gray-200 dark:!border-gray-700">
          <el-radio-button label="">Tất cả</el-radio-button>
          <el-radio-button label="BEGINNER">Sơ cấp</el-radio-button>
          <el-radio-button label="INTERMEDIATE">Trung cấp</el-radio-button>
          <el-radio-button label="ADVANCED">Nâng cao</el-radio-button>
        </el-radio-group>
      </div>

      <div v-loading="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        <template v-if="filteredTopics.length">
          <div
            v-for="topic in filteredTopics"
            :key="topic.id"
            class="group relative bg-white dark:bg-[#1d1d1d] rounded-2xl border border-gray-300 dark:border-gray-700 p-5 cursor-pointer hover:shadow-xl hover:-translate-y-1 hover:border-indigo-400 transition-all duration-300 flex flex-col h-full"
            @click="handleTopicClick(topic)"
          >
            <div v-if="!topic.isAccessible" class="absolute inset-0 bg-white/60 dark:bg-black/60 backdrop-blur-[2px] z-10 flex flex-col items-center justify-center rounded-2xl">
              <div class="bg-white dark:bg-[#2c2c2c] p-3 rounded-full shadow-md text-gray-400 mb-2 border border-gray-200 dark:border-gray-600">
                <el-icon :size="24"><Lock /></el-icon>
              </div>
              <span class="text-xs font-bold text-gray-500 uppercase tracking-wide">Chưa mở khóa</span>
            </div>

            <div class="flex justify-between items-start mb-4">
              <div class="w-12 h-12 rounded-xl bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center text-blue-600 dark:text-blue-400 group-hover:bg-blue-600 group-hover:text-white transition-colors shadow-sm">
                <el-icon :size="24"><Reading /></el-icon>
              </div>
              <el-tag :type="getLevelColor(topic.levelRequired)" effect="dark" round size="small" class="!border-none uppercase !font-bold">
                {{ topic.levelRequired }}
              </el-tag>
            </div>

            <h3 class="text-lg font-bold text-gray-900 dark:text-white mb-2 line-clamp-1 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
              {{ topic.name }}
            </h3>
            <p class="text-sm text-gray-500 dark:text-gray-400 mb-6 line-clamp-2 flex-1 leading-relaxed">
              {{ topic.description || 'Chưa có mô tả' }}
            </p>

            <div class="mt-auto pt-4 border-t border-gray-100 dark:border-gray-700">
              <div class="flex justify-between text-xs text-gray-500 dark:text-gray-400 mb-2 font-medium">
                <span>Tiến độ</span>
                <span class="text-gray-700 dark:text-gray-300">
                  {{ topic.completedLessons || 0 }}/{{ topic.totalLessons || 0 }} bài
                </span>
              </div>
              <el-progress
                :percentage="calculateProgress(topic)"
                :show-text="false"
                :stroke-width="6"
                :status="calculateProgress(topic) === 100 ? 'success' : ''"
              />
            </div>
          </div>
        </template>

        <el-empty v-else description="Không tìm thấy chủ đề nào" class="col-span-full" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { grammarUserAPI } from '@/api'
import { Reading, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const topics = ref([])
const filterLevel = ref('')

const fetchTopics = async () => {
  loading.value = true
  try {
    const res = await grammarUserAPI.getAccessibleTopics()
    topics.value = res.data.data || []
  } catch (e) { console.error(e) } finally { loading.value = false }
}

const filteredTopics = computed(() => {
  if (!filterLevel.value) return topics.value
  return topics.value.filter(t => t.levelRequired === filterLevel.value)
})

const calculateProgress = (topic) => {
  if (!topic.totalLessons) return 0
  return Math.round((topic.completedLessons / topic.totalLessons) * 100)
}

const getLevelColor = (level) => {
  const map = { 'BEGINNER': 'success', 'INTERMEDIATE': 'warning', 'ADVANCED': 'danger' }
  return map[level] || 'info'
}

const handleTopicClick = (topic) => {
  if (!topic.isAccessible) return

  // Logic: Nhảy thẳng vào bài học tiếp theo (hoặc bài đầu tiên nếu chưa học)
  const lessons = topic.lessons || []
  if (lessons.length === 0) {
    ElMessage.warning('Chủ đề này chưa có bài học nào')
    return
  }

  // Tìm bài chưa completed đầu tiên
  const nextLesson = lessons.find(l => !l.isCompleted)
  const targetId = nextLesson ? nextLesson.id : lessons[0].id // Nếu xong hết thì vào bài 1

  router.push({ name: 'user-grammar-lesson', params: { lessonId: targetId } })
}

onMounted(fetchTopics)
</script>
