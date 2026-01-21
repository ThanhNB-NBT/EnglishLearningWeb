<template>
  <el-dialog
    v-model="visible"
    title="📊 Import Questions từ Excel"
    width="95%"
    top="3vh"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <!-- Step Indicator -->
    <div class="mb-6">
      <el-steps :active="step - 1" finish-status="success" align-center>
        <el-step title="Upload File" icon="Upload" />
        <el-step title="Review & Edit" icon="View" />
        <el-step title="Save to Database" icon="Check" />
      </el-steps>
    </div>

    <!-- Step 1: Upload File -->
    <div v-if="step === 1" class="space-y-4">
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleFileSelect"
        :file-list="fileList"
        class="upload-area"
      >
        <el-icon class="el-icon--upload text-6xl text-blue-500">
          <upload-filled />
        </el-icon>
        <div class="el-upload__text text-lg mt-4">
          Kéo file Excel vào đây hoặc <em class="text-blue-600">click để chọn</em>
        </div>
        <template #tip>
          <div class="el-upload__tip text-sm text-gray-500">
            ✅ Chỉ hỗ trợ file .xlsx, .xls<br />
            📄 Tải template mẫu nếu chưa có
          </div>
        </template>
      </el-upload>

      <div class="flex justify-end gap-2 pt-4 border-t">
        <el-button @click="visible = false">Hủy</el-button>
        <el-button type="primary" @click="parseExcel" :disabled="!selectedFile" :loading="parsing">
          <el-icon class="mr-1"><Right /></el-icon>
          Phân tích Excel
        </el-button>
      </div>
    </div>

    <!-- Step 2: Preview & Edit -->
    <div v-if="step === 2" class="space-y-4">
      <!-- Header Actions -->
      <div
        class="flex justify-between items-center mb-4 p-4 bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 rounded-lg border border-blue-200"
      >
        <div>
          <h3 class="font-bold text-lg text-gray-800 dark:text-white flex items-center gap-2">
            <el-icon class="text-blue-600"><DocumentChecked /></el-icon>
            Review {{ parsedQuestions.length }} câu hỏi
          </h3>
          <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
            Kiểm tra kỹ trước khi lưu vào hệ thống
            <span v-if="taskGroupCount > 0" class="ml-2 text-blue-600">
              ({{ taskGroupCount }} task groups phát hiện)
            </span>
          </p>
        </div>
        <div class="flex gap-2">
          <el-button @click="step = 1" :icon="Back"> ← Chọn file khác </el-button>
          <el-button type="danger" plain :icon="Delete" @click="removeAllQuestions">
            Xóa tất cả
          </el-button>
        </div>
      </div>

      <!-- ✅ Task Groups Summary WITH INSTRUCTIONS -->
      <div
        v-if="taskGroupSummary.length > 0"
        class="p-4 bg-blue-50 dark:bg-blue-900/10 rounded-lg border border-blue-200"
      >
        <div
          class="font-bold text-sm text-blue-700 dark:text-blue-300 mb-3 flex items-center gap-2"
        >
          <el-icon><Collection /></el-icon>
          Task Groups trong file Excel:
        </div>

        <div class="space-y-2">
          <div
            v-for="task in taskGroupSummary"
            :key="task.name"
            class="bg-white dark:bg-blue-900/20 p-3 rounded-lg border border-blue-300"
          >
            <div class="flex items-center justify-between mb-1">
              <span class="font-bold text-blue-800 dark:text-blue-200">{{ task.name }}</span>
              <el-tag type="primary" size="small" effect="dark">
                {{ task.count }} câu hỏi
              </el-tag>
            </div>

            <div v-if="task.instruction" class="text-xs text-gray-600 dark:text-gray-400 italic">
              📝 Hướng dẫn: "{{ task.instruction }}"
            </div>
            <div v-else class="text-xs text-orange-500">
              ⚠️ Chưa có hướng dẫn (sẽ dùng mặc định)
            </div>
          </div>
        </div>

        <div class="text-xs text-gray-600 dark:text-gray-400 mt-3 p-2 bg-blue-100 dark:bg-blue-900/30 rounded">
          <el-icon><InfoFilled /></el-icon>
          Hệ thống sẽ tự động tạo task groups mới hoặc gán vào task đã có (nếu tên trùng)
        </div>
      </div>

      <!-- Questions Preview Cards -->
      <div class="space-y-3 max-h-[600px] overflow-y-auto pr-2">
        <div
          v-for="(question, index) in parsedQuestions"
          :key="index"
          class="border rounded-xl overflow-hidden bg-white dark:bg-[#1d1d1d] shadow-sm hover:shadow-md transition-shadow"
        >
          <!-- Question Header -->
          <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-[#252525] border-b">
            <div class="flex items-center gap-3">
              <div
                class="w-10 h-10 bg-blue-600 text-white rounded-full flex items-center justify-center font-bold"
              >
                {{ index + 1 }}
              </div>
              <div>
                <el-tag
                  :type="getQuestionTypeColor(question.questionType)"
                  size="large"
                  effect="dark"
                >
                  {{ getQuestionTypeLabel(question.questionType) }}
                </el-tag>
                <span class="ml-2 text-sm text-gray-500">
                  Điểm: <strong class="text-orange-600">{{ question.points }}</strong>
                </span>
              </div>
              <!-- Task Group Tag -->
              <el-tag v-if="question.taskGroupName" type="primary" size="small">
                <el-icon class="mr-1"><Collection /></el-icon>
                {{ question.taskGroupName }}
              </el-tag>
            </div>

            <el-button
              type="danger"
              circle
              plain
              size="small"
              :icon="Delete"
              @click="removeQuestion(index)"
            />
          </div>

          <!-- Question Content -->
          <div class="p-4 space-y-3">
            <!-- Question Text -->
            <div v-if="question.questionText && question.questionText.trim()">
              <div class="text-xs font-bold text-gray-500 uppercase mb-1">Nội dung câu hỏi:</div>
              <div
                class="bg-blue-50 dark:bg-blue-900/10 p-3 rounded-lg border border-blue-200 text-sm"
              >
                {{ question.questionText }}
              </div>
            </div>

            <!-- Data Preview by Type -->
            <div class="bg-gray-50 dark:bg-[#252525] p-4 rounded-lg border">
              <div class="text-xs font-bold text-gray-500 uppercase mb-3">Cấu hình đáp án:</div>

              <!-- MULTIPLE_CHOICE / TRUE_FALSE -->
              <div v-if="['MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(question.questionType)">
                <div
                  v-if="question.data?.options && question.data.options.length > 0"
                  class="space-y-2"
                >
                  <div
                    v-for="(opt, optIdx) in question.data.options"
                    :key="optIdx"
                    class="flex items-center gap-2 p-2 rounded"
                    :class="
                      opt.isCorrect
                        ? 'bg-green-50 dark:bg-green-900/20 border border-green-200'
                        : 'bg-white dark:bg-[#1d1d1d] border'
                    "
                  >
                    <span
                      class="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold border"
                      :class="
                        opt.isCorrect
                          ? 'bg-green-500 text-white border-green-500'
                          : 'bg-gray-100 text-gray-600'
                      "
                    >
                      {{ String.fromCharCode(65 + optIdx) }}
                    </span>
                    <span class="flex-1">{{ opt.text }}</span>
                    <el-icon v-if="opt.isCorrect" class="text-green-600 text-xl"><Check /></el-icon>
                  </div>
                </div>
                <el-alert v-else type="error" :closable="false" show-icon>
                  ⚠️ Thiếu options! Vui lòng kiểm tra lại file Excel
                </el-alert>
              </div>

              <!-- FILL_BLANK / TEXT_ANSWER -->
              <div
                v-else-if="
                  ['FILL_BLANK', 'TEXT_ANSWER', 'VERB_FORM'].includes(question.questionType)
                "
              >
                <div v-if="question.data?.blanks && question.data.blanks.length > 0">
                  <div class="space-y-2">
                    <div
                      v-for="(blank, blankIdx) in question.data.blanks"
                      :key="blankIdx"
                      class="flex items-center gap-2"
                    >
                      <span class="text-xs font-bold text-gray-500 min-w-[80px]"
                        >Vị trí {{ blank.position }}:</span
                      >
                      <div class="flex flex-wrap gap-1">
                        <el-tag
                          v-for="ans in getBlankAnswers(blank)"
                          :key="ans"
                          type="success"
                          effect="dark"
                          size="small"
                        >
                          {{ ans }}
                        </el-tag>
                      </div>
                    </div>
                  </div>
                  <!-- Word Bank -->
                  <div
                    v-if="question.data.wordBank && question.data.wordBank.length > 0"
                    class="mt-3 pt-3 border-t"
                  >
                    <div class="text-xs font-bold text-blue-600 mb-2">Word Bank:</div>
                    <div class="flex flex-wrap gap-1">
                      <el-tag
                        v-for="word in question.data.wordBank"
                        :key="word"
                        type="primary"
                        size="small"
                      >
                        {{ word }}
                      </el-tag>
                    </div>
                  </div>
                </div>
                <el-alert v-else type="error" :closable="false" show-icon>
                  ⚠️ Thiếu blanks! Vui lòng kiểm tra lại file Excel
                </el-alert>
              </div>

              <!-- MATCHING -->
              <div v-else-if="question.questionType === 'MATCHING'">
                <div
                  v-if="question.data?.pairs && question.data.pairs.length > 0"
                  class="space-y-2"
                >
                  <div
                    v-for="(pair, pairIdx) in question.data.pairs"
                    :key="pairIdx"
                    class="flex items-center gap-2 p-2 bg-white dark:bg-[#1d1d1d] rounded border"
                  >
                    <span class="font-bold text-gray-700">{{ pair.left }}</span>
                    <el-icon class="text-blue-500"><Right /></el-icon>
                    <span class="text-gray-600">{{ pair.right }}</span>
                  </div>
                </div>
                <el-alert v-else type="error" :closable="false" show-icon>
                  ⚠️ Thiếu pairs! Vui lòng kiểm tra lại file Excel
                </el-alert>
              </div>

              <!-- ERROR_CORRECTION -->
              <div v-else-if="question.questionType === 'ERROR_CORRECTION'">
                <div v-if="question.data?.errorText && question.data?.correction" class="space-y-2">
                  <div class="flex items-center gap-2">
                    <span class="text-xs font-bold text-red-600">Sai:</span>
                    <span class="line-through text-red-500">{{ question.data.errorText }}</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="text-xs font-bold text-green-600">Đúng:</span>
                    <span class="text-green-600 font-medium">{{ question.data.correction }}</span>
                  </div>
                </div>
                <el-alert v-else type="error" :closable="false" show-icon>
                  ⚠️ Thiếu errorText hoặc correction!
                </el-alert>
              </div>

              <!-- SENTENCE_TRANSFORMATION -->
              <div v-else-if="question.questionType === 'SENTENCE_TRANSFORMATION'">
                <div
                  v-if="question.data?.correctAnswers && question.data.correctAnswers.length > 0"
                >
                  <div v-if="question.data.beginningPhrase" class="mb-2">
                    <span class="text-xs font-bold text-blue-600">Gợi ý đầu câu:</span>
                    <span class="ml-2">{{ question.data.beginningPhrase }}</span>
                  </div>
                  <div class="text-xs font-bold text-green-600 mb-2">Đáp án đúng:</div>
                  <div class="space-y-1">
                    <div
                      v-for="ans in question.data.correctAnswers"
                      :key="ans"
                      class="p-2 bg-green-50 dark:bg-green-900/20 rounded border border-green-200"
                    >
                      {{ ans }}
                    </div>
                  </div>
                </div>
                <el-alert v-else type="error" :closable="false" show-icon>
                  ⚠️ Thiếu correctAnswers!
                </el-alert>
              </div>

              <!-- SENTENCE_BUILDING -->
              <div v-else-if="question.questionType === 'SENTENCE_BUILDING'">
                <div v-if="question.data?.correctSentence">
                  <div class="mb-2">
                    <span class="text-xs font-bold text-blue-600">Từ rời:</span>
                    <div class="flex flex-wrap gap-1 mt-1">
                      <el-tag v-for="word in question.data.words" :key="word" size="small">
                        {{ word }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="p-2 bg-green-50 dark:bg-green-900/20 rounded border border-green-200">
                    <span class="text-xs font-bold text-green-600">Câu đúng:</span>
                    <div class="mt-1">{{ question.data.correctSentence }}</div>
                  </div>
                </div>
                <el-alert v-else type="error" :closable="false" show-icon>
                  ⚠️ Thiếu correctSentence!
                </el-alert>
              </div>

              <!-- Fallback -->
              <div v-else class="text-gray-500 text-sm italic text-center py-2">
                Không có dữ liệu preview cho loại câu hỏi này
              </div>
            </div>

            <!-- Explanation -->
            <div
              v-if="question.data?.explanation"
              class="bg-blue-50 dark:bg-blue-900/10 p-3 rounded-lg border border-blue-200"
            >
              <div class="text-xs font-bold text-blue-600 mb-1 flex items-center gap-1">
                <el-icon><InfoFilled /></el-icon>
                Giải thích:
              </div>
              <div class="text-sm text-gray-700 dark:text-gray-300">
                {{ question.data.explanation }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer Actions -->
      <div
        class="flex justify-between items-center pt-4 border-t bg-gray-50 dark:bg-[#252525] -mx-6 -mb-6 px-6 py-4"
      >
        <div class="text-sm text-gray-600">
          Tổng: <strong class="text-blue-600">{{ parsedQuestions.length }}</strong> câu hỏi
          <span v-if="taskGroupCount > 0" class="ml-2 text-blue-600">
            • {{ taskGroupCount }} task groups
          </span>
        </div>
        <div class="flex gap-2">
          <el-button @click="step = 1" :icon="Back">← Quay lại</el-button>
          <el-button
            type="success"
            @click="saveToDatabase"
            :loading="saving"
            :disabled="parsedQuestions.length === 0 || hasValidationErrors"
          >
            <el-icon class="mr-1"><Check /></el-icon>
            Lưu vào hệ thống ({{ parsedQuestions.length }})
          </el-button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import {
  UploadFilled,
  Delete,
  Back,
  Right,
  Check,
  DocumentChecked,
  InfoFilled,
  Collection,
} from '@element-plus/icons-vue'
import { questionImportApi } from '@/api/modules/question.api'
import { useQuestionStore } from '@/stores/admin/questionAdmin'

const props = defineProps({
  parentType: { type: String, required: true },
})

const emit = defineEmits(['saved'])

const questionStore = useQuestionStore()

const visible = ref(false)
const step = ref(1)
const lessonId = ref(null)
const selectedFile = ref(null)
const fileList = ref([])
const parsedQuestions = ref([])
const parsing = ref(false)
const saving = ref(false)

// ✅ Computed: Task Group Summary WITH INSTRUCTIONS
const taskGroupSummary = computed(() => {
  const taskMap = new Map()

  parsedQuestions.value.forEach((q) => {
    if (q.taskGroupName) {
      if (!taskMap.has(q.taskGroupName)) {
        taskMap.set(q.taskGroupName, {
          name: q.taskGroupName,
          instruction: q.taskInstruction || '', // ✅ Lưu instruction
          count: 0
        })
      }
      taskMap.get(q.taskGroupName).count++
    }
  })

  return Array.from(taskMap.values())
})

const taskGroupCount = computed(() => taskGroupSummary.value.length)

const hasValidationErrors = computed(() => {
  return parsedQuestions.value.some((q) => {
    if (['MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(q.questionType)) {
      return !q.data?.options || q.data.options.length === 0
    }
    if (['FILL_BLANK', 'TEXT_ANSWER', 'VERB_FORM'].includes(q.questionType)) {
      return !q.data?.blanks || q.data.blanks.length === 0
    }
    if (q.questionType === 'MATCHING') {
      return !q.data?.pairs || q.data.pairs.length === 0
    }
    if (q.questionType === 'ERROR_CORRECTION') {
      return !q.data?.errorText || !q.data?.correction
    }
    if (q.questionType === 'SENTENCE_TRANSFORMATION') {
      return !q.data?.correctAnswers || q.data.correctAnswers.length === 0
    }
    if (q.questionType === 'SENTENCE_BUILDING') {
      return !q.data?.correctSentence
    }
    return false
  })
})

const open = (currentLessonId) => {
  lessonId.value = currentLessonId
  visible.value = true
  step.value = 1
  selectedFile.value = null
  fileList.value = []
  parsedQuestions.value = []
}

const handleFileSelect = (file) => {
  selectedFile.value = file.raw
  fileList.value = [file]
}

const parseExcel = async () => {
  if (!selectedFile.value || !lessonId.value) {
    ElMessage.warning('Chưa chọn file hoặc lesson')
    return
  }

  parsing.value = true
  try {
    const response = await questionImportApi.parseExcel(
      selectedFile.value,
      props.parentType,
      lessonId.value,
    )

    if (response.data.success) {
      parsedQuestions.value = response.data.data
      step.value = 2

      let message = `Phân tích thành công ${parsedQuestions.value.length} câu hỏi`
      if (taskGroupCount.value > 0) {
        message += ` (${taskGroupCount.value} task groups)`
      }
      ElMessage.success(message)
    }
  } catch (error) {
    console.error('Parse error:', error)
    ElMessage.error('Lỗi phân tích file: ' + (error.response?.data?.message || error.message))
  } finally {
    parsing.value = false
  }
}

// ✅ COMPLETE: Save with Task Group resolution + Instruction
const saveToDatabase = async () => {
  if (!parsedQuestions.value.length) {
    ElMessage.warning('Không có câu hỏi nào để lưu')
    return
  }

  if (hasValidationErrors.value) {
    ElMessage.error('Một số câu hỏi thiếu dữ liệu! Vui lòng kiểm tra lại')
    return
  }

  try {
    let confirmMessage = `Lưu ${parsedQuestions.value.length} câu hỏi vào hệ thống?`
    if (taskGroupCount.value > 0) {
      confirmMessage += `\n\n📦 Sẽ tạo/gán ${taskGroupCount.value} task groups`
    }

    await ElMessageBox.confirm(confirmMessage, 'Xác nhận', {
      confirmButtonText: 'Lưu',
      cancelButtonText: 'Hủy',
      type: 'info',
    })
  } catch {
    return
  }

  const loading = ElLoading.service({
    lock: true,
    text: 'Đang xử lý...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  saving.value = true

  try {
    // ✅ Step 1: Load existing task groups
    await questionStore.fetchTaskGroups(props.parentType, lessonId.value)

    // ✅ Step 2: Group questions by task name and collect instructions
    const taskInstructions = {}
    parsedQuestions.value.forEach(q => {
      if (q.taskGroupName && q.taskInstruction) {
        // Only store if not already stored (take first occurrence)
        if (!taskInstructions[q.taskGroupName]) {
          taskInstructions[q.taskGroupName] = q.taskInstruction
        }
      }
    })

    console.log('📝 Task Instructions collected:', taskInstructions)

    // ✅ Step 3: Resolve taskGroupName → taskGroupId for ALL questions
    const taskIdMap = new Map() // taskName -> taskId

    // First, identify all unique task names
    const uniqueTaskNames = [...new Set(
      parsedQuestions.value
        .filter(q => q.taskGroupName)
        .map(q => q.taskGroupName)
    )]

    console.log('📦 Unique task names:', uniqueTaskNames)

    // Process each unique task name
    for (const taskName of uniqueTaskNames) {
      // Find existing task
      let existingTask = questionStore.taskGroups.find(
        (tg) => tg.taskName.trim() === taskName.trim(),
      )

      if (!existingTask) {
        // ✅ Create new task WITH INSTRUCTION from Excel
        try {
          const taskData = {
            taskName: taskName,
            instruction: taskInstructions[taskName] || 'Imported from Excel',
            orderIndex: null,
          }

          console.log('📦 Creating task:', taskData)

          existingTask = await questionStore.createTaskGroup(
            props.parentType,
            lessonId.value,
            taskData
          )

          console.log('✅ Created task:', existingTask)
        } catch (error) {
          console.error('❌ Error creating task:', error)

          if (error.message && error.message.includes('tồn tại')) {
            await questionStore.fetchTaskGroups(props.parentType, lessonId.value)
            existingTask = questionStore.taskGroups.find(
              (tg) => tg.taskName.trim() === taskName.trim(),
            )
          }

          if (!existingTask) {
            throw new Error(`Không thể tạo/tìm task: ${taskName}`)
          }
        }
      }

      // Store mapping
      taskIdMap.set(taskName, existingTask.id)
      console.log(`✅ Task "${taskName}" → ID ${existingTask.id}`)
    }

    // ✅ Step 4: Assign taskGroupId to ALL questions
    parsedQuestions.value.forEach((q) => {
      if (q.taskGroupName) {
        const taskId = taskIdMap.get(q.taskGroupName)
        if (taskId) {
          q.taskGroupId = taskId
          console.log(`✅ Question "${q.questionText?.substring(0, 30)}..." → Task ID ${taskId}`)
        }
        // Clean up temporary fields
        delete q.taskGroupName
        delete q.taskInstruction
      }
    })

    console.log('📤 Final questions to save:', parsedQuestions.value.length)
    console.log('📤 Questions with taskGroupId:', parsedQuestions.value.filter(q => q.taskGroupId).length)

    // ✅ Step 5: Save all questions
    await questionImportApi.saveBatchQuestions(parsedQuestions.value)

    let successMessage = `Đã lưu ${parsedQuestions.value.length} câu hỏi`
    if (taskGroupCount.value > 0) {
      successMessage += ` và ${taskGroupCount.value} task groups`
    }

    ElMessage.success({
      message: successMessage,
      duration: 3000,
      showClose: true,
    })

    visible.value = false
    emit('saved')
  } catch (error) {
    console.error('Save error:', error)
    ElMessage.error({
      message: 'Lỗi lưu: ' + (error.response?.data?.message || error.message),
      duration: 5000,
      showClose: true,
    })
  } finally {
    saving.value = false
    loading.close()
  }
}

const removeQuestion = (index) => {
  parsedQuestions.value.splice(index, 1)
  ElMessage.info(`Đã xóa câu hỏi #${index + 1}`)
}

const removeAllQuestions = async () => {
  try {
    await ElMessageBox.confirm(`Xóa tất cả ${parsedQuestions.value.length} câu hỏi?`, 'Cảnh báo', {
      type: 'warning',
      confirmButtonText: 'Xóa',
      cancelButtonText: 'Hủy',
    })
    parsedQuestions.value = []
    ElMessage.success('Đã xóa tất cả câu hỏi')
  } catch {
    // User cancelled
  }
}

const getQuestionTypeLabel = (type) => {
  const map = {
    MULTIPLE_CHOICE: 'Trắc nghiệm',
    TRUE_FALSE: 'Đúng/Sai',
    FILL_BLANK: 'Điền từ',
    TEXT_ANSWER: 'Trả lời ngắn',
    VERB_FORM: 'Chia động từ',
    MATCHING: 'Nối từ',
    ERROR_CORRECTION: 'Sửa lỗi',
    SENTENCE_TRANSFORMATION: 'Viết lại câu',
    SENTENCE_BUILDING: 'Sắp xếp câu',
    PRONUNCIATION: 'Phát âm',
    OPEN_ENDED: 'Câu hỏi mở',
  }
  return map[type] || type
}

const getQuestionTypeColor = (type) => {
  const map = {
    MULTIPLE_CHOICE: 'primary',
    TRUE_FALSE: 'success',
    FILL_BLANK: 'warning',
    TEXT_ANSWER: 'info',
    VERB_FORM: 'warning',
    MATCHING: 'danger',
    ERROR_CORRECTION: 'danger',
    SENTENCE_TRANSFORMATION: '',
    SENTENCE_BUILDING: '',
    PRONUNCIATION: 'info',
    OPEN_ENDED: 'info',
  }
  return map[type] || ''
}

const getBlankAnswers = (blank) => {
  if (Array.isArray(blank.correctAnswers)) return blank.correctAnswers
  if (typeof blank.correctAnswers === 'string') return [blank.correctAnswers]
  return []
}

defineExpose({ open })
</script>

<style scoped>
.upload-area :deep(.el-upload-dragger) {
  padding: 40px;
}

:deep(.el-step__title) {
  font-size: 14px;
}

:deep(.el-step__description) {
  font-size: 12px;
}
</style>
