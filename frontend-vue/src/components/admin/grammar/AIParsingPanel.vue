<template>
  <div class="w-full h-full flex flex-col items-center justify-center p-4">
    <div
      v-if="!parsedResult"
      class="w-full max-w-3xl bg-white dark:bg-[#1d1d1d] p-8 rounded-2xl shadow-sm border border-gray-300 dark:border-gray-700 text-center"
    >
      <div class="mb-6">
        <div
          class="w-10 h-10 bg-indigo-50 dark:bg-indigo-900/20 rounded-full flex items-center justify-center mx-auto mb-4 text-indigo-600 dark:text-indigo-400"
        >
          <el-icon :size="24"><MagicStick /></el-icon>
        </div>
        <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">AI Grammar Parser</h2>
        <p class="text-gray-500 dark:text-gray-400">
          Tải lên tài liệu (PDF, Word, Ảnh) để AI tự động trích xuất bài học.
        </p>
      </div>

      <div
        class="border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-xl p-1 cursor-pointer hover:border-indigo-500 hover:bg-indigo-50 dark:hover:bg-[#252525] transition-all mb-6 relative group"
        @click="triggerFileInput"
        @drop.prevent="handleDrop"
        @dragover.prevent
      >
        <input
          type="file"
          ref="fileInput"
          class="hidden"
          @change="handleFileChange"
          accept=".pdf,.docx,.jpg,.png,.jpeg"
        />

        <div v-if="selectedFile" class="flex flex-col items-center animate-fade-in">
          <el-icon class="text-green-500 mb-2" :size="24"><CircleCheckFilled /></el-icon>
          <div class="font-bold text-gray-800 dark:text-white text-lg">{{ selectedFile.name }}</div>
          <div class="text-sm text-gray-500 mb-3">
            {{ (selectedFile.size / 1024 / 1024).toFixed(2) }} MB
          </div>

          <el-button type="danger" size="small" round @click.stop="selectedFile = null">
            <el-icon class="mr-1"><Delete /></el-icon> Chọn file khác
          </el-button>
        </div>

        <div v-else class="group-hover:scale-105 transition-transform duration-300">
          <div class="text-gray-600 dark:text-gray-300 font-medium text-lg">
            Kéo thả file hoặc
            <span class="text-indigo-600 font-bold underline decoration-2 underline-offset-4"
              >Click để tải lên</span
            >
          </div>
          <div class="text-xs text-gray-400 mt-2">Hỗ trợ PDF, DOCX, JPG, PNG (Max 10MB)</div>
        </div>
      </div>

      <div
        class="text-left bg-gray-50 dark:bg-[#252525] p-5 rounded-xl border border-gray-200 dark:border-gray-700 mb-6"
      >
        <div class="flex justify-between items-center mb-3">
          <label class="text-sm font-bold text-gray-800 dark:text-gray-200 flex items-center gap-2">
            <el-icon><ChatLineSquare /></el-icon> Hướng dẫn AI (Parsing Context)
          </label>

          <el-dropdown trigger="click" @command="applyTemplate">
            <el-button type="primary" link size="small">
              <el-icon class="mr-1"><Notebook /></el-icon> Chọn mẫu câu lệnh
            </el-button>
            <template #dropdown>
              <el-dropdown-menu class="w-72">
                <el-dropdown-item
                  v-for="(tpl, idx) in promptTemplates"
                  :key="idx"
                  :command="tpl.value"
                >
                  <div class="flex flex-col py-1">
                    <span class="font-bold text-gray-700 dark:text-gray-200">{{ tpl.label }}</span>
                    <span class="text-xs text-gray-400 truncate">{{ tpl.desc }}</span>
                  </div>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <el-input
          v-model="parsingContext"
          type="textarea"
          :rows="4"
          placeholder="Nhập hướng dẫn cụ thể cho AI. Ví dụ: 'Chỉ lấy 10 câu trắc nghiệm đầu tiên', 'Bỏ qua phần giới thiệu'..."
          class="!text-base"
          resize="none"
        />

        <div class="mt-2 flex gap-2 overflow-x-auto pb-1 no-scrollbar">
          <el-tag
            v-for="tag in quickTags"
            :key="tag"
            type="info"
            size="small"
            effect="plain"
            class="cursor-pointer hover:!border-indigo-400 transition-colors shrink-0"
            @click="appendContext(tag)"
          >
            + {{ tag }}
          </el-tag>
        </div>
      </div>

      <div class="flex flex-col md:flex-row gap-4">
        <el-select
          v-model="selectedTopicId"
          placeholder="Chọn Chủ đề để lưu bài học..."
          filterable
          class="!w-full md:!w-2/3"
          size="large"
        >
          <template #prefix
            ><el-icon><Folder /></el-icon
          ></template>
          <el-option v-for="t in topics" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>

        <el-button
          type="primary"
          size="large"
          :loading="loading"
          :disabled="!selectedFile || !selectedTopicId"
          class="!w-full md:!w-1/3 !font-bold !rounded-lg shadow-lg shadow-indigo-500/30"
          @click="handleParse"
        >
          <span v-if="!loading">Bắt đầu Phân tích</span>
          <span v-else>Đang xử lý...</span>
        </el-button>
      </div>
    </div>

    <div v-else class="w-full max-w-6xl h-full flex flex-col animate-fade-in-up">
      <div
        class="flex justify-between items-center mb-4 shrink-0 bg-white dark:bg-[#1d1d1d] p-4 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm"
      >
        <div class="flex items-center gap-3">
          <div
            class="w-10 h-10 rounded-lg bg-green-100 dark:bg-green-900/30 flex items-center justify-center text-green-600"
          >
            <el-icon :size="20"><CircleCheckFilled /></el-icon>
          </div>
          <div>
            <h3 class="text-lg font-bold text-gray-800 dark:text-white">Kết quả phân tích</h3>
            <p class="text-xs text-gray-500">
              Tìm thấy {{ parsedResult.lessons?.length || 0 }} bài học
            </p>
          </div>
        </div>
        <div class="flex gap-3">
          <el-button @click="reset" :icon="RefreshLeft">Thử lại</el-button>
          <el-button type="success" :loading="saving" @click="handleSave" class="!font-bold px-6">
            Lưu vào Database
          </el-button>
        </div>
      </div>

      <div
        class="flex-1 overflow-y-auto bg-gray-50 dark:bg-[#121212] p-4 rounded-xl border border-gray-300 dark:border-gray-700 shadow-inner"
      >
        <div
          v-if="!parsedResult.lessons?.length"
          class="flex flex-col items-center justify-center h-full text-gray-400"
        >
          <el-icon :size="48" class="mb-2"><DocumentDelete /></el-icon>
          <p>Không tìm thấy nội dung nào phù hợp.</p>
        </div>

        <div v-else class="space-y-6">
          <div
            v-for="(lesson, idx) in parsedResult.lessons"
            :key="idx"
            class="bg-white dark:bg-[#1d1d1d] rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden shadow-sm"
          >
            <div
              class="px-6 py-4 border-b border-gray-100 dark:border-gray-700 flex justify-between items-center bg-gray-50/50 dark:bg-[#252525]"
            >
              <div class="flex items-center gap-3">
                <span class="bg-indigo-600 text-white font-bold px-2.5 py-0.5 rounded text-xs"
                  >Lesson {{ idx + 1 }}</span
                >
                <h4 class="text-base font-bold text-gray-800 dark:text-white">
                  {{ lesson.title }}
                </h4>
              </div>
              <el-tag
                effect="plain"
                :type="lesson.lessonType === 'THEORY' ? 'success' : 'warning'"
                class="!rounded-full"
                >{{ lesson.lessonType }}</el-tag
              >
            </div>

            <div class="p-6 grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div v-if="lesson.content">
                <div class="text-xs font-bold text-gray-400 uppercase mb-2">Nội dung bài học</div>
                <div
                  class="bg-gray-50 dark:bg-[#252525] p-4 rounded-lg text-sm border border-gray-200 dark:border-gray-600 max-h-60 overflow-y-auto custom-scrollbar"
                >
                  <div class="ql-editor !p-0" v-html="lesson.content"></div>
                </div>
              </div>

              <div v-if="lesson.createQuestions?.length > 0">
                <div class="text-xs font-bold text-gray-400 uppercase mb-2">
                  Câu hỏi ({{ lesson.createQuestions.length }})
                </div>
                <div class="space-y-3 max-h-60 overflow-y-auto custom-scrollbar pr-2">
                  <div
                    v-for="(q, qIdx) in lesson.createQuestions"
                    :key="qIdx"
                    class="bg-white dark:bg-[#2c2c2c] p-3 rounded-lg border border-gray-200 dark:border-gray-600 hover:border-indigo-300 transition-colors"
                  >
                    <div class="flex justify-between mb-1.5">
                      <span class="font-bold text-indigo-600 text-xs">{{ q.questionType }}</span>
                      <span class="text-xs font-bold text-orange-500">+{{ q.points }}</span>
                    </div>
                    <div
                      class="text-sm text-gray-700 dark:text-gray-300 line-clamp-2"
                      v-html="q.questionText"
                    ></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  UploadFilled,
  MagicStick,
  CircleCheckFilled,
  InfoFilled,
  Delete,
  RefreshLeft,
  DocumentDelete,
  ChatLineSquare,
  Notebook,
  Folder,
} from '@element-plus/icons-vue'
import { useGrammarStore } from '@/stores/grammar'
import { grammarAdminAPI } from '@/api/modules/grammar.api'
import { ElMessage } from 'element-plus'
import 'quill/dist/quill.snow.css'

const store = useGrammarStore()
const fileInput = ref(null)
const selectedFile = ref(null)
const selectedTopicId = ref(null)
const parsingContext = ref('')
const loading = ref(false)
const saving = ref(false)
const parsedResult = ref(null)

const topics = computed(() => store.topics)

// --- PROMPT LIBRARY ---
const promptTemplates = [
  { label: 'Tự động (Mặc định)', value: '', desc: 'AI tự động nhận diện cấu trúc' },
  {
    label: 'Trắc nghiệm (Multiple Choice)',
    value:
      'Chỉ trích xuất các câu hỏi trắc nghiệm (Multiple Choice). Đảm bảo lấy đủ 4 đáp án A, B, C, D và đáp án đúng.',
    desc: 'Ưu tiên lấy câu hỏi trắc nghiệm',
  },
  {
    label: 'Bài đọc hiểu (Reading)',
    value:
      'Đây là bài đọc hiểu. Hãy trích xuất đoạn văn (Passage) vào nội dung bài học, sau đó là các câu hỏi liên quan.',
    desc: 'Tách bài đọc và câu hỏi',
  },
  {
    label: 'Bài tập điền từ',
    value:
      'Trích xuất bài tập điền từ (Fill in the blank). Giữ nguyên định dạng chỗ trống là "___".',
    desc: 'Dạng bài Fill Blank',
  },
  {
    label: 'Chỉ lấy bài tập (Bỏ lý thuyết)',
    value: 'Bỏ qua phần lý thuyết giải thích. Chỉ trích xuất các bài tập thực hành.',
    desc: 'Lọc bỏ nội dung thừa',
  },
]

const quickTags = [
  'Bỏ qua trang bìa',
  'Chỉ lấy 10 câu đầu',
  'Format dạng HTML',
  'Ưu tiên câu hỏi khó',
]

const applyTemplate = (val) => {
  parsingContext.value = val
}

const appendContext = (tag) => {
  parsingContext.value = parsingContext.value ? `${parsingContext.value}. ${tag}` : tag
}

// ... (Các hàm handle file, parse, save giữ nguyên như cũ) ...
const triggerFileInput = () => fileInput.value.click()
const handleFileChange = (e) => {
  selectedFile.value = e.target.files[0]
}
const handleDrop = (e) => {
  selectedFile.value = e.dataTransfer.files[0]
}

const handleParse = async () => {
  if (!selectedFile.value || !selectedTopicId.value) return
  loading.value = true
  try {
    const res = await grammarAdminAPI.parseFile(
      selectedTopicId.value,
      selectedFile.value,
      null,
      parsingContext.value,
    )
    if (res.data.success) {
      parsedResult.value = res.data.data.parsedData
      ElMessage.success(`Phân tích xong! Tìm thấy ${parsedResult.value.lessons.length} bài học.`)
    }
  } catch (e) {
    ElMessage.error('Lỗi phân tích: ' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!parsedResult.value) return
  saving.value = true
  try {
    await grammarAdminAPI.saveParsedLessons(selectedTopicId.value, parsedResult.value)
    ElMessage.success('Lưu thành công!')
    reset()
  } catch (e) {
    ElMessage.error('Lỗi lưu dữ liệu')
  } finally {
    saving.value = false
  }
}

const reset = () => {
  parsedResult.value = null
  selectedFile.value = null
  parsingContext.value = ''
  if (fileInput.value) fileInput.value.value = ''
}

onMounted(async () => {
  if (topics.value.length === 0) await store.fetchTopics({ size: 100 })
})
</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.animate-fade-in {
  animation: fadeIn 0.5s ease-out;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
