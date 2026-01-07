<template>
  <div class="bg-white dark:bg-[#1d1d1d] p-6 rounded-xl border border-gray-200 dark:border-gray-700">
    <div class="flex items-center gap-3 mb-6">
      <div class="w-10 h-10 rounded-full bg-gradient-to-br from-purple-500 to-indigo-600 flex items-center justify-center text-white">
        <el-icon class="text-xl"><MagicStick /></el-icon>
      </div>
      <div>
        <h3 class="text-lg font-bold text-gray-900 dark:text-white">AI Lesson Generator</h3>
        <p class="text-sm text-gray-500">Tải lên tài liệu (PDF, DOCX, Ảnh) để AI tự động tạo bài học</p>
      </div>
    </div>

    <div
      class="border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-xl p-8 text-center transition-all hover:border-purple-500 hover:bg-purple-50 dark:hover:bg-purple-900/10 cursor-pointer"
      :class="{ 'border-purple-500 bg-purple-50 dark:bg-purple-900/10': isDragging }"
      @dragover.prevent="isDragging = true"
      @dragleave.prevent="isDragging = false"
      @drop.prevent="handleDrop"
      @click="triggerFileInput"
    >
      <input
        type="file"
        ref="fileInput"
        class="hidden"
        accept=".pdf,.docx,.doc,.jpg,.jpeg,.png"
        @change="handleFileSelect"
      />

      <div v-if="!selectedFile">
        <el-icon class="text-5xl text-gray-300 mb-3"><UploadFilled /></el-icon>
        <p class="text-gray-600 dark:text-gray-300 font-medium">Kéo thả hoặc click để chọn file</p>
        <p class="text-xs text-gray-400 mt-2">Hỗ trợ PDF, Word, Ảnh (Max 20MB)</p>
      </div>

      <div v-else class="flex items-center justify-center gap-4">
        <div class="text-left">
           <div class="flex items-center gap-2 text-purple-600 font-bold text-lg">
              <el-icon><Document /></el-icon> {{ selectedFile.name }}
           </div>
           <p class="text-xs text-gray-500">{{ (selectedFile.size / 1024 / 1024).toFixed(2) }} MB</p>
        </div>
        <el-button type="danger" circle size="small" :icon="Close" @click.stop="selectedFile = null" />
      </div>
    </div>

    <div class="mt-6 grid grid-cols-1 md:grid-cols-2 gap-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Trang cần xử lý (PDF)</label>
        <el-input v-model="pageRange" placeholder="VD: 1,3,5-7 (Để trống = Toàn bộ)" :disabled="!isPdf" />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Hướng dẫn cho AI (Context)</label>
        <el-input v-model="parsingContext" placeholder="VD: Chỉ lấy phần bài tập thì quá khứ đơn..." />
      </div>
    </div>

    <div class="mt-6 flex justify-end">
      <el-button
        type="primary"
        size="large"
        :loading="isParsing"
        :disabled="!selectedFile"
        @click="handleParse"
        class="!rounded-lg !px-8 !font-bold bg-gradient-to-r from-purple-600 to-indigo-600 border-none hover:opacity-90"
      >
        <el-icon class="mr-2"><Cpu /></el-icon> Phân tích & Tạo bài học
      </el-button>
    </div>

    <el-dialog v-model="showPreview" title="Kết quả phân tích AI" width="80%" top="5vh">
      <div v-if="parseResult">
        <div class="grid grid-cols-4 gap-4 mb-6">
           <div class="bg-gray-100 p-3 rounded text-center">
              <div class="text-xs text-gray-500">Số bài học</div>
              <div class="text-xl font-bold text-blue-600">{{ parseResult.totalLessons }}</div>
           </div>
           <div class="bg-gray-100 p-3 rounded text-center">
              <div class="text-xs text-gray-500">Tổng câu hỏi</div>
              <div class="text-xl font-bold text-purple-600">{{ parseResult.totalQuestions }}</div>
           </div>
           </div>

        <el-table :data="parseResult.lessons" height="400" stripe border>
           <el-table-column prop="title" label="Tiêu đề bài học" />
           <el-table-column prop="lessonType" label="Loại" width="100">
              <template #default="{ row }">
                 <el-tag :type="row.lessonType === 'THEORY' ? 'success' : 'warning'">{{ row.lessonType }}</el-tag>
              </template>
           </el-table-column>
           <el-table-column label="Số câu hỏi" width="120" align="center">
              <template #default="{ row }">
                 {{ row.createQuestions?.length || 0 }}
              </template>
           </el-table-column>
        </el-table>
      </div>
      <template #footer>
         <el-button @click="showPreview = false">Hủy</el-button>
         <el-button type="success" :loading="isImporting" @click="handleImport">
            <el-icon class="mr-2"><Check /></el-icon> Xác nhận Import
         </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, UploadFilled, Document, Close, Cpu, Check } from '@element-plus/icons-vue'
import { useGrammarAdminStore } from '@/stores/admin/grammarAdmin'

const props = defineProps({ topicId: { type: Number, required: true } })
const emit = defineEmits(['success'])
const store = useGrammarAdminStore()

const fileInput = ref(null)
const selectedFile = ref(null)
const isDragging = ref(false)
const isParsing = ref(false)
const isImporting = ref(false)
const showPreview = ref(false)
const parseResult = ref(null)

const pageRange = ref('')
const parsingContext = ref('')

const isPdf = computed(() => selectedFile.value?.type === 'application/pdf')

const triggerFileInput = () => fileInput.value.click()

const handleFileSelect = (e) => {
  const file = e.target.files[0]
  if(file) selectedFile.value = file
}

const handleDrop = (e) => {
  isDragging.value = false
  const file = e.dataTransfer.files[0]
  if(file) selectedFile.value = file
}

const handleParse = async () => {
  if (!selectedFile.value) return

  isParsing.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)

    // Xử lý page range string "1,2,3" -> Backend cần List<Integer>
    // Spring Boot có thể tự convert chuỗi "1,2,3" thành List<Integer> nếu param là List
    if (isPdf.value && pageRange.value) {
       // Tách chuỗi và gửi từng giá trị nếu cần, hoặc gửi chuỗi csv
       // Cách an toàn nhất với Spring @RequestParam List<Integer>:
       const pages = pageRange.value.split(',').map(p => p.trim()).filter(p => !isNaN(p))
       pages.forEach(p => formData.append('pages', p))
    }

    if (parsingContext.value) {
       formData.append('parsingContext', parsingContext.value)
    }

    // Gọi Store action
    const res = await store.parseFile(props.topicId, formData)
    parseResult.value = res // Backend trả về summary map
    showPreview.value = true

  } catch (err) {
    console.error(err)
    // Error handled in store
  } finally {
    isParsing.value = false
  }
}

const handleImport = async () => {
  if (!parseResult.value?.lessons) return

  isImporting.value = true
  try {
    // Backend API: POST /topics/{id}/import-lessons -> Body: List<GrammarLessonDTO>
    await store.importLessons(props.topicId, parseResult.value.lessons)
    ElMessage.success(`Đã import thành công ${parseResult.value.lessons.length} bài học`)
    showPreview.value = false
    selectedFile.value = null
    emit('success')
  } catch (err) {
    console.error(err)
  } finally {
    isImporting.value = false
  }
}
</script>
