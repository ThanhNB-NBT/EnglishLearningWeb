<template>
  <el-dialog
    v-model="visible"
    width="98%"
    top="2vh"
    class="!rounded-xl !p-0 overflow-hidden flex flex-col"
    :close-on-click-modal="false"
    destroy-on-close
    :show-close="false"
  >
    <template #header="{ close }">
      <div
        class="flex justify-between items-center px-6 py-3 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-[#1d1d1d]"
      >
        <div class="flex items-center gap-4">
          <div class="flex flex-col">
            <h4 class="text-lg font-bold text-gray-800 dark:text-white m-0">Soạn thảo hàng loạt</h4>
            <span class="text-xs text-gray-500 mt-0.5" v-if="currentLesson">
              Bài: <b class="text-blue-600">{{ currentLesson.title }}</b>
            </span>
          </div>

          <div
            v-if="currentLesson?.content"
            class="ml-4 pl-4 border-l border-gray-300 dark:border-gray-600"
          >
            <el-button
              @click="showContent = !showContent"
              :type="showContent ? 'primary' : 'default'"
              link
              size="small"
              class="!font-bold"
            >
              <el-icon class="mr-1"><Reading /></el-icon>
              {{ showContent ? 'Tắt bài đọc' : 'Xem bài đọc' }}
            </el-button>
          </div>
        </div>

        <button
          @click="close"
          class="w-8 h-8 flex items-center justify-center rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 dark:hover:bg-[#333] dark:hover:text-gray-200 transition-all"
        >
          <el-icon :size="20"><Close /></el-icon>
        </button>
      </div>
    </template>

    <div class="flex-1 flex overflow-hidden bg-gray-100 dark:bg-[#121212] h-[80vh]">
      <transition name="el-fade-in-linear">
        <div
          v-if="showContent && currentLesson?.content"
          class="w-1/3 min-w-[400px] max-w-[50%] bg-white dark:bg-[#1d1d1d] border-r border-gray-200 dark:border-gray-700 flex flex-col shadow-lg z-10 relative"
        >
          <div
            class="h-10 px-4 bg-gray-50 dark:bg-[#252525] border-b border-gray-200 dark:border-gray-700 font-bold text-gray-600 dark:text-gray-300 flex items-center shrink-0 text-sm"
          >
            <span class="flex items-center gap-2"
              ><el-icon><Document /></el-icon> Nội dung bài đọc</span
            >
          </div>
          <div class="flex-1 overflow-y-auto p-6 bg-white dark:bg-[#1d1d1d]">
            <div
              class="ql-editor !p-0 text-gray-800 dark:text-gray-200 text-base leading-relaxed"
              v-html="currentLesson.content"
            ></div>
          </div>
        </div>
      </transition>

      <div class="flex-1 flex flex-col h-full overflow-hidden relative">
        <div
          class="h-12 px-6 bg-gray-50 dark:bg-[#252525] border-b border-gray-200 dark:border-gray-700 flex justify-between items-center shrink-0"
        >
          <div class="text-sm text-gray-500">
            Danh sách: <b class="text-blue-600">{{ questionList.length }}</b> câu hỏi
          </div>
          <div class="flex gap-2">
            <el-button @click="removeAll" :icon="Delete" type="danger" plain class="!h-8"
              >Xóa hết</el-button
            >
            <el-button type="primary" :icon="Plus" @click="addNewQuestion" class="!h-8 font-bold"
              >Thêm dòng</el-button
            >
          </div>
        </div>

        <div class="flex-1 overflow-y-auto p-6 space-y-4">
          <div
            v-if="questionList.length === 0"
            class="flex flex-col items-center justify-center h-full text-gray-400"
          >
            <el-icon :size="48" class="mb-4"><DocumentAdd /></el-icon>
            <p>Chưa có câu hỏi nào.</p>
            <el-button type="primary" plain class="mt-4" @click="addNewQuestion"
              >Thêm câu hỏi đầu tiên</el-button
            >
          </div>

          <SingleQuestionEditor
            v-for="(item, index) in questionList"
            :key="index"
            :model-value="item"
            :index="index"
            @update:model-value="(val) => updateQuestion(index, val)"
            @remove="removeQuestion(index)"
            @clone="cloneQuestion(index)"
          />

          <div v-if="questionList.length > 0" class="text-center pt-4 pb-10">
            <el-button
              type="primary"
              plain
              :icon="Plus"
              class="!w-full !max-w-md !border-dashed !h-12"
              @click="addNewQuestion"
              >Thêm câu hỏi tiếp theo</el-button
            >
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div
        class="flex justify-end gap-3 px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-[#1d1d1d] z-20"
      >
        <el-button @click="visible = false" class="!rounded-lg !h-10 !px-6">Hủy bỏ</el-button>
        <el-button
          type="primary"
          :loading="loading"
          @click="handleSubmit"
          class="!rounded-lg !font-bold px-8 !h-10 !text-base shadow-lg shadow-blue-500/20"
        >
          Lưu tất cả ({{ questionList.length }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, defineAsyncComponent, watch } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, DocumentAdd, Close, Reading, Document } from '@element-plus/icons-vue'
const SingleQuestionEditor = defineAsyncComponent(
  () => import('@/components/admin/questions/SingleQuestionEditor.vue'),
)

const props = defineProps({ lessonId: Number })
const emit = defineEmits(['success'])
const store = useGrammarStore()

const visible = ref(false)
const loading = ref(false)
const questionList = ref([])
const currentLesson = ref(null)
const showContent = ref(true)

const createEmptyQuestion = () => ({
  questionType: 'MULTIPLE_CHOICE',
  questionText: '',
  points: 10,
  explanation: '',
  metadata: {},
  isCollapsed: false,
  orderIndex: 0,
})

const open = async () => {
  questionList.value = [createEmptyQuestion()]
  visible.value = true

  if (props.lessonId) {
    try {
      const res = await store.fetchLessonById(props.lessonId)
      currentLesson.value = res
      showContent.value = !!(res && res.content && res.content.length > 50)
    } catch (e) {
      console.error(e)
    }
  }
}

const addNewQuestion = () => {
  questionList.value.forEach((q) => (q.isCollapsed = true))
  questionList.value.push(createEmptyQuestion())
}

const updateQuestion = (index, newVal) => {
  questionList.value[index] = newVal
}

const removeQuestion = (index) => {
  questionList.value.splice(index, 1)
}

const removeAll = () => {
  ElMessageBox.confirm('Xóa hết danh sách?', 'Warning', { type: 'warning' })
    .then(() => (questionList.value = []))
    .catch(() => {})
}

const cloneQuestion = (index) => {
  const original = questionList.value[index]
  const clone = JSON.parse(JSON.stringify(original))
  clone.isCollapsed = false
  questionList.value.splice(index + 1, 0, clone)
  ElMessage.success('Đã nhân bản')
}

const handleSubmit = async () => {
  if (questionList.value.length === 0) {
    ElMessage.warning('Danh sách trống')
    return
  }

  loading.value = true
  try {
    const payload = questionList.value.map(({ isCollapsed, ...rest }, idx) => ({
      ...rest,
      orderIndex: idx + 1,
    }))

    await store.createQuestionsInBulk(props.lessonId, payload)
    ElMessage.success(`Đã tạo thành công ${payload.length} câu hỏi`)
    emit('success')
    visible.value = false
  } catch (e) {
    console.error(e)
    ElMessage.error(e.response?.data?.message || 'Lỗi khi lưu dữ liệu')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.el-fade-in-linear-enter-active,
.el-fade-in-linear-leave-active {
  transition: all 0.3s;
}
</style>
