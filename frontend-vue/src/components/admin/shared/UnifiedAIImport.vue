<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
    <!-- Header -->
    <div class="mb-6 border-b border-gray-100 dark:border-gray-700 pb-4">
      <h2 class="text-xl font-bold text-gray-800 dark:text-white flex items-center">
        <el-icon class="mr-2 text-indigo-600"><MagicStick /></el-icon>
        AI Content Generator
      </h2>
      <p class="text-sm text-gray-500 mt-1">
        Tự động tạo nội dung bài học và trích xuất câu hỏi từ tài liệu (PDF, Word) bằng AI.
      </p>
    </div>

    <!-- Module & Topic Selection -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          1. Chọn Kỹ năng
        </label>
        <el-select
          v-model="selectedModule"
          @change="onModuleChange"
          placeholder="Chọn kỹ năng"
          class="w-full"
          size="large"
        >
          <el-option label="Ngữ pháp (Grammar)" value="GRAMMAR" />
          <el-option label="Đọc hiểu (Reading)" value="READING" />
          <el-option label="Nghe (Listening)" value="LISTENING" />
        </el-select>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          2. Chọn Chủ đề (Topic) *
        </label>
        <el-select
          v-model="selectedTopicId"
          :loading="isLoadingTopics"
          :disabled="!selectedModule"
          filterable
          placeholder="-- Chọn Topic --"
          class="w-full"
          size="large"
        >
          <el-option
            v-for="topic in topics"
            :key="topic.id"
            :label="`${topic.name} (${topic.englishLevel})`"
            :value="topic.id"
          />
        </el-select>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          3. Chỉ dẫn cho AI (Tùy chọn)
        </label>
        <el-input
          v-model="instruction"
          placeholder="VD: Lấy nội dung trang 5-7, tạo 10 câu trắc nghiệm..."
          clearable
          size="large"
        />
      </div>
    </div>

    <!-- File Upload -->
    <div class="mb-6">
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
        Upload Tài liệu (Tùy chọn)
      </label>
      <div
        class="border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg p-8 text-center hover:border-indigo-500 transition-colors cursor-pointer bg-gray-50 dark:bg-[#1d1d1d]"
        @click="$refs.docInput.click()"
        @dragover.prevent
        @drop.prevent="handleDrop"
      >
        <input
          type="file"
          ref="docInput"
          class="hidden"
          accept=".pdf,.docx,.doc"
          @change="handleFileSelect"
        />

        <div v-if="!file" class="flex flex-col items-center">
          <el-icon class="text-4xl text-gray-400 mb-2"><UploadFilled /></el-icon>
          <p class="text-gray-600 dark:text-gray-400 font-medium">
            Click để chọn file hoặc kéo thả vào đây
          </p>
          <p class="text-xs mt-1 text-gray-400">(Hỗ trợ .pdf, .docx - Max 10MB)</p>
        </div>

        <div v-else class="text-indigo-600 font-medium flex flex-col items-center">
          <div class="flex items-center text-lg bg-indigo-50 px-4 py-2 rounded-lg">
            <el-icon class="mr-2"><Document /></el-icon> {{ file.name }}
          </div>
          <el-button type="danger" link class="mt-2" @click.stop="clearFile">
            <el-icon class="mr-1"><Delete /></el-icon> Xóa file
          </el-button>
        </div>
      </div>
    </div>

    <!-- Audio Upload (Listening only) -->
    <div v-if="selectedModule === 'LISTENING'" class="mb-6">
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
        <el-icon class="mr-1"><Headset /></el-icon>
        Upload Audio File (Tùy chọn - có thể thêm sau)
      </label>
      <div
        class="border-2 border-dashed border-blue-300 dark:border-blue-600 rounded-lg p-6 text-center hover:border-blue-500 transition-colors cursor-pointer bg-blue-50 dark:bg-blue-900/10"
        @click="$refs.audioInput.click()"
      >
        <input
          type="file"
          ref="audioInput"
          class="hidden"
          accept=".mp3,.wav,.m4a,.ogg"
          @change="handleAudioSelect"
        />

        <div v-if="!audioFile" class="flex flex-col items-center">
          <el-icon class="text-3xl text-blue-400 mb-2"><Microphone /></el-icon>
          <p class="text-blue-600 dark:text-blue-400 font-medium text-sm">
            Click để chọn file audio
          </p>
          <p class="text-xs mt-1 text-gray-400">(MP3, WAV, M4A - Max 20MB)</p>
        </div>

        <div v-else class="text-blue-600 font-medium flex flex-col items-center">
          <div class="flex items-center text-base bg-blue-100 px-4 py-2 rounded-lg">
            <el-icon class="mr-2"><Headset /></el-icon> {{ audioFile.name }}
          </div>
          <el-button type="danger" link class="mt-2" @click.stop="clearAudio">
            <el-icon class="mr-1"><Delete /></el-icon> Xóa audio
          </el-button>
        </div>
      </div>
    </div>

    <!-- Action Button -->
    <div class="flex justify-end border-b border-gray-100 dark:border-gray-700 pb-6 mb-6">
      <el-button
        type="primary"
        size="large"
        @click="analyzeFile"
        :loading="isAnalyzing"
        :disabled="!canAnalyze"
        class="!px-8 !py-4 !rounded-lg !text-base font-bold shadow-lg shadow-indigo-200"
      >
        <el-icon v-if="!isAnalyzing" class="mr-2"><Cpu /></el-icon>
        {{ isAnalyzing ? 'AI đang phân tích...' : 'Phân tích ngay' }}
      </el-button>
    </div>

    <!-- Result Preview & Edit -->
    <div v-if="aiResult" class="animate-fade-in-up">
      <div class="flex justify-between items-center mb-4">
        <h3 class="text-lg font-bold text-gray-800 dark:text-white border-l-4 border-indigo-500 pl-3">
          Kết quả phân tích
        </h3>
        <el-tag type="success" effect="dark">Ready to Save</el-tag>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 h-[700px]">
        <!-- Left: Lesson Content -->
        <div class="flex flex-col h-full border rounded-xl overflow-hidden bg-white dark:bg-[#1d1d1d] shadow-sm">
          <div class="bg-gray-50 dark:bg-gray-800 p-3 border-b font-semibold text-indigo-700 dark:text-indigo-300 flex justify-between items-center">
            <span>Nội dung bài học</span>
            <el-button-group size="small">
              <el-button :type="showContentMode === 'edit' ? 'primary' : ''" @click="showContentMode = 'edit'">
                Edit
              </el-button>
              <el-button :type="showContentMode === 'preview' ? 'primary' : ''" @click="showContentMode = 'preview'">
                Preview
              </el-button>
            </el-button-group>
          </div>

          <div class="flex-1 overflow-hidden flex flex-col p-4 gap-4">
            <div>
              <label class="text-xs font-bold text-gray-500 uppercase mb-1 block">Tên bài học</label>
              <el-input v-model="aiResult.title" placeholder="Nhập tên bài học" size="large" />
            </div>

            <div class="flex-1 flex flex-col min-h-0">
              <label class="text-xs font-bold text-gray-500 uppercase mb-1 block">Nội dung chi tiết</label>

              <!-- ✅ FIX: Hiển thị content theo mode -->
              <div v-if="showContentMode === 'edit'" class="flex-1 overflow-hidden border rounded-lg">
                <QuillRichEditor v-model:content="aiResult.content" theme="snow" class="h-full" />
              </div>

              <!-- ✅ NEW: Preview mode với HTML rendering -->
              <div v-else class="flex-1 overflow-auto border rounded-lg p-4 bg-gray-50 dark:bg-gray-900 prose prose-sm max-w-none">
                <div v-html="aiResult.content"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Right: Questions -->
        <div class="flex flex-col h-full border rounded-xl overflow-hidden bg-gray-50 dark:bg-[#1d1d1d] shadow-sm">
          <div class="bg-gray-100 dark:bg-gray-800 p-3 border-b flex justify-between items-center">
            <span class="font-semibold text-indigo-700 dark:text-indigo-300">
              Danh sách câu hỏi ({{ totalQuestions }})
            </span>
            <el-button size="small" type="primary" plain @click="clearAllQuestions">Xóa tất cả</el-button>
          </div>

          <div class="flex-1 overflow-y-auto p-4 space-y-3">
            <!-- Task Groups -->
            <div v-if="aiResult.taskGroups && aiResult.taskGroups.length > 0" class="space-y-3">
              <div
                v-for="(taskGroup, tgIdx) in aiResult.taskGroups"
                :key="tgIdx"
                class="border border-blue-200 rounded-lg overflow-hidden"
              >
                <div class="bg-blue-50 dark:bg-blue-900/20 px-3 py-2 border-b border-blue-200 flex justify-between items-center">
                  <div>
                    <div class="font-bold text-blue-700 text-sm">{{ taskGroup.taskName }}</div>
                    <div class="text-xs text-blue-600 italic">{{ taskGroup.instruction }}</div>
                  </div>
                  <el-button
                    type="danger"
                    circle
                    size="small"
                    :icon="Delete"
                    @click="removeTaskGroup(tgIdx)"
                  />
                </div>

                <div class="p-2 space-y-2">
                  <div
                    v-for="(q, qIdx) in taskGroup.questions"
                    :key="qIdx"
                    class="bg-white dark:bg-[#252525] p-3 rounded border hover:border-blue-300 transition-colors group relative"
                  >
                    <div class="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <el-button
                        type="danger"
                        circle
                        size="small"
                        :icon="Delete"
                        @click="removeQuestionFromTask(tgIdx, qIdx)"
                      />
                    </div>

                    <QuestionCard :question="q" :show-task-info="false" />
                  </div>
                </div>
              </div>
            </div>

            <!-- Standalone Questions -->
            <div v-if="aiResult.standaloneQuestions && aiResult.standaloneQuestions.length > 0" class="space-y-2">
              <div class="text-xs font-bold text-gray-500 uppercase mb-2">Standalone Questions</div>
              <div
                v-for="(q, idx) in aiResult.standaloneQuestions"
                :key="idx"
                class="bg-white dark:bg-[#252525] p-4 rounded-lg shadow-sm border hover:border-indigo-300 transition-colors group relative"
              >
                <div class="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <el-button
                    type="danger"
                    circle
                    size="small"
                    :icon="Delete"
                    @click="removeStandaloneQuestion(idx)"
                  />
                </div>

                <QuestionCard :question="q" />
              </div>
            </div>

            <!-- Empty State -->
            <el-empty v-if="totalQuestions === 0" description="Chưa có câu hỏi nào" :image-size="80" />
          </div>
        </div>
      </div>

      <!-- Save Button -->
      <div class="mt-6 flex justify-end gap-3 pt-4 border-t border-gray-100 dark:border-gray-700">
        <el-button @click="aiResult = null" size="large">Hủy bỏ</el-button>
        <el-button
          type="success"
          size="large"
          @click="saveToDatabase"
          :loading="isSaving"
          class="!font-bold !px-6 !text-base"
        >
          <el-icon class="mr-2"><Check /></el-icon> LƯU VÀO HỆ THỐNG
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  UploadFilled,
  Document,
  Delete,
  MagicStick,
  Cpu,
  Check,
  Headset,
  Microphone,
} from '@element-plus/icons-vue'
import QuillRichEditor from '@/components/common/QuillRichEditor.vue'
import QuestionCard from '@/components/admin/shared/questions/QuestionCard.vue'
import { aiAPI } from '@/api/modules/ai.api'
import { topicAPI } from '@/api/modules/topic.api'

// State
const selectedModule = ref('GRAMMAR')
const selectedTopicId = ref(null)
const topics = ref([])
const isLoadingTopics = ref(false)

const instruction = ref('')
const file = ref(null)
const audioFile = ref(null)
const docInput = ref(null)
const audioInput = ref(null)
const isAnalyzing = ref(false)
const isSaving = ref(false)

const aiResult = ref(null)
const showContentMode = ref('preview') // 'edit' or 'preview'

// Computed
const canAnalyze = computed(() => {
  return selectedModule.value && selectedTopicId.value && (file.value || instruction.value)
})

const totalQuestions = computed(() => {
  if (!aiResult.value) return 0
  let count = 0
  if (aiResult.value.taskGroups) {
    aiResult.value.taskGroups.forEach(tg => {
      if (tg.questions) count += tg.questions.length
    })
  }
  if (aiResult.value.standaloneQuestions) {
    count += aiResult.value.standaloneQuestions.length
  }
  return count
})

// Methods
const onModuleChange = async () => {
  selectedTopicId.value = null
  topics.value = []
  if (!selectedModule.value) return

  isLoadingTopics.value = true
  try {
    const res = await topicAPI.getTopicsByModule(selectedModule.value, {
      page: 0,
      size: 100,
      sort: 'orderIndex:asc'
    })

    if (res.data.success) {
      topics.value = res.data.data?.content || []
    }
  } catch (e) {
    console.error('Failed to load topics', e)
    ElMessage.warning('Không thể tải danh sách Topic')
  } finally {
    isLoadingTopics.value = false
  }
}

const handleFileSelect = (e) => {
  if (e.target.files.length > 0) {
    const selectedFile = e.target.files[0]
    if (selectedFile.size > 10 * 1024 * 1024) {
      ElMessage.error('File quá lớn. Max 10MB.')
      return
    }
    file.value = selectedFile
  }
}

const handleDrop = (e) => {
  if (e.dataTransfer.files.length > 0) {
    file.value = e.dataTransfer.files[0]
  }
}

const clearFile = () => {
  file.value = null
  if (docInput.value) docInput.value.value = ''
}

const handleAudioSelect = (e) => {
  if (e.target.files.length > 0) {
    const selectedAudio = e.target.files[0]
    if (selectedAudio.size > 20 * 1024 * 1024) {
      ElMessage.error('Audio quá lớn. Max 20MB.')
      return
    }
    audioFile.value = selectedAudio
  }
}

const clearAudio = () => {
  audioFile.value = null
  if (audioInput.value) audioInput.value.value = ''
}

const analyzeFile = async () => {
  if (!selectedTopicId.value) {
    ElMessage.warning('Vui lòng chọn Topic')
    return
  }

  if (!file.value && !instruction.value) {
    ElMessage.warning('Vui lòng upload file HOẶC nhập instruction')
    return
  }

  isAnalyzing.value = true
  aiResult.value = null

  try {
    console.log('📤 Sending to AI:', {
      module: selectedModule.value,
      topicId: selectedTopicId.value,
      hasFile: !!file.value,
      instruction: instruction.value
    })

    const res = await aiAPI.parseLesson(
      selectedModule.value,
      selectedTopicId.value,
      file.value,
      instruction.value
    )

    console.log('📥 AI Response:', res.data)

    if (res.data && (res.data.status === 200 || res.status === 200)) {
      aiResult.value = res.data.data

      // Ensure title
      if (!aiResult.value.title && aiResult.value.name) {
        aiResult.value.title = aiResult.value.name
      }

      console.log('✅ Parsed result:', {
        title: aiResult.value.title,
        contentLength: aiResult.value.content?.length,
        taskGroups: aiResult.value.taskGroups?.length,
        questions: totalQuestions.value
      })

      ElMessage.success('AI phân tích thành công!')
    } else {
      throw new Error(res.data?.message || 'Phân tích thất bại')
    }
  } catch (error) {
    console.error('❌ Parse error:', error)
    ElMessage.error('Lỗi: ' + (error.response?.data?.message || error.message))
  } finally {
    isAnalyzing.value = false
  }
}

const removeTaskGroup = (index) => {
  if (aiResult.value.taskGroups) {
    aiResult.value.taskGroups.splice(index, 1)
  }
}

const removeQuestionFromTask = (taskIdx, questionIdx) => {
  if (aiResult.value.taskGroups && aiResult.value.taskGroups[taskIdx]) {
    aiResult.value.taskGroups[taskIdx].questions.splice(questionIdx, 1)
  }
}

const removeStandaloneQuestion = (index) => {
  if (aiResult.value.standaloneQuestions) {
    aiResult.value.standaloneQuestions.splice(index, 1)
  }
}

const clearAllQuestions = () => {
  if (aiResult.value) {
    aiResult.value.taskGroups = []
    aiResult.value.standaloneQuestions = []
  }
}

const saveToDatabase = async () => {
  if (!selectedTopicId.value || !aiResult.value) {
    ElMessage.warning('Thiếu dữ liệu')
    return
  }

  if (!aiResult.value.title?.trim()) {
    ElMessage.warning('Vui lòng nhập tên bài học')
    return
  }

  if (totalQuestions.value === 0) {
    ElMessage.warning('Bài học phải có ít nhất 1 câu hỏi')
    return
  }

  isSaving.value = true
  try {
    const lessonData = {
      topicId: selectedTopicId.value,
      title: aiResult.value.title,
      content: aiResult.value.content || '',
      taskGroups: aiResult.value.taskGroups || [],
      standaloneQuestions: aiResult.value.standaloneQuestions || [],

      ...(selectedModule.value === 'GRAMMAR' && {
        lessonType: 'PRACTICE',
      }),

      ...(selectedModule.value === 'LISTENING' && {
        transcript: aiResult.value.content || '',
        transcriptTranslation: aiResult.value.transcriptTranslation || null,
      }),

      orderIndex: 1,
      isActive: true,
      timeLimitSeconds: 180,
      pointsReward: 50,
    }

    const res = await aiAPI.saveLesson(
      selectedModule.value,
      lessonData,
      audioFile.value
    )

    if (res.data?.status === 200 || res.status === 200) {
      ElMessageBox.alert('Bài học đã được lưu thành công!', 'Thành công', {
        confirmButtonText: 'OK',
        type: 'success',
        callback: () => {
          aiResult.value = null
          clearFile()
          clearAudio()
          instruction.value = ''
        },
      })
    }
  } catch (error) {
    console.error('❌ Save error:', error)
    ElMessage.error('Lỗi lưu: ' + (error.response?.data?.message || error.message))
  } finally {
    isSaving.value = false
  }
}

onMounted(() => {
  onModuleChange()
})
</script>

<style scoped>
.animate-fade-in-up {
  animation: fadeInUp 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ✅ Style for HTML content preview */
.prose {
  color: inherit;
}

.prose h1,
.prose h2,
.prose h3 {
  color: inherit;
  font-weight: bold;
  margin-top: 1em;
  margin-bottom: 0.5em;
}

.prose h2 {
  font-size: 1.5em;
}

.prose h3 {
  font-size: 1.25em;
}

.prose p {
  margin-bottom: 1em;
}

.prose ul,
.prose ol {
  margin-left: 1.5em;
  margin-bottom: 1em;
}

.prose li {
  margin-bottom: 0.5em;
}

.prose strong {
  font-weight: 600;
}
</style>
