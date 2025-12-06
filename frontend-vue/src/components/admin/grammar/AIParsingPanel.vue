<template>
  <div class="ai-parsing-panel">
    <el-steps :active="activeStep" finish-status="success" simple class="mb-4">
      <el-step title="Upload & Cấu hình" icon="Upload" />
      <el-step title="Review & Lưu" icon="View" />
    </el-steps>

    <div v-if="activeStep === 0" class="step-content">
      <el-card shadow="never" class="upload-card">
        <el-form label-position="top" class="config-form">

          <el-form-item label="Chọn Chủ đề (Topic)" required>
            <el-select
              v-model="selectedTopicId"
              placeholder="Chọn chủ đề để thêm bài học..."
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="topic in topics"
                :key="topic.id"
                :label="topic.name"
                :value="topic.id"
              >
                <span style="float: left">{{ topic.name }}</span>
                <el-tag size="small" type="info" style="float: right">{{ topic.levelRequired }}</el-tag>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="File tài liệu (PDF, DOCX, Ảnh)" required>
            <el-upload
              class="upload-area"
              drag
              action="#"
              :auto-upload="false"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              :limit="1"
              accept=".pdf,.docx,.jpg,.jpeg,.png,.webp"
            >
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">
                Kéo thả file hoặc <em>nhấn để tải lên</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  Hỗ trợ PDF, DOCX, JPG, PNG (Max 20MB)
                </div>
              </template>
            </el-upload>
          </el-form-item>

          <!-- ✅ ADVANCED OPTIONS COLLAPSE -->
          <el-collapse v-model="advancedOpen" class="advanced-options">
            <el-collapse-item name="advanced">
              <template #title>
                <div class="flex items-center">
                  <el-icon class="mr-2"><Setting /></el-icon>
                  <span class="font-semibold">Tùy chọn nâng cao</span>
                  <el-tag size="small" type="info" class="ml-2">Không bắt buộc</el-tag>
                </div>
              </template>

              <!-- Page Range (PDF only) -->
              <el-form-item label="Chọn trang cụ thể (Chỉ dành cho PDF)">
                <el-input
                  v-model="pageRange"
                  placeholder="VD: 1,3,5-10 (Để trống để parse toàn bộ)"
                  clearable
                >
                  <template #prefix><el-icon><Document /></el-icon></template>
                </el-input>
                <div class="text-xs text-gray-400 mt-1">
                  * Hỗ trợ chọn trang rời rạc (1,2) hoặc khoảng (1-5).
                </div>
              </el-form-item>

              <!-- ✅ PARSING CONTEXT (NEW) -->
              <el-form-item>
                <template #label>
                  <div class="flex items-center justify-between w-full">
                    <span>Hướng dẫn phân tích (Parsing Context)</span>
                    <el-popover placement="top" :width="400" trigger="hover">
                      <template #reference>
                        <el-button link size="small">
                          <el-icon><QuestionFilled /></el-icon>
                          Hướng dẫn
                        </el-button>
                      </template>
                      <div class="parsing-help">
                        <h4 class="font-bold mb-2">💡 Cách sử dụng Parsing Context:</h4>
                        <ul class="text-sm space-y-1">
                          <li>• Giúp AI hiểu rõ hơn về cấu trúc file của bạn</li>
                          <li>• Chỉ định phần nào cần parse, phần nào bỏ qua</li>
                          <li>• Hướng dẫn cách tách bài học và câu hỏi</li>
                        </ul>

                        <h5 class="font-semibold mt-3 mb-1">📝 Ví dụ:</h5>
                        <pre class="text-xs bg-gray-100 dark:bg-gray-800 p-2 rounded">Parse ONLY sections I, II and exercises.
SKIP section III about -s endings.
Each exercise should be a SEPARATE lesson.</pre>

                        <h5 class="font-semibold mt-3 mb-1">🎯 Mẫu sẵn:</h5>
                        <div class="flex flex-wrap gap-2 mt-2">
                          <el-tag
                            v-for="(template, key) in contextTemplates"
                            :key="key"
                            size="small"
                            class="cursor-pointer hover:opacity-80"
                            @click="applyTemplate(key)"
                          >
                            {{ key }}
                          </el-tag>
                        </div>
                      </div>
                    </el-popover>
                  </div>
                </template>

                <el-input
                  v-model="parsingContext"
                  type="textarea"
                  :rows="5"
                  placeholder="Ví dụ: Parse sections I, II và các bài tập. Bỏ qua section III về đuôi -s. Mỗi Exercise là một lesson riêng."
                  clearable
                  show-word-limit
                  maxlength="500"
                >
                  <template #prepend>
                    <el-icon><EditPen /></el-icon>
                  </template>
                </el-input>

                <!-- Quick Templates -->
                <div class="template-buttons mt-2 flex flex-wrap gap-2">
                  <el-button
                    v-for="(template, key) in contextTemplates"
                    :key="key"
                    size="small"
                    @click="applyTemplate(key)"
                  >
                    {{ key }}
                  </el-button>
                </div>
              </el-form-item>

            </el-collapse-item>
          </el-collapse>

          <el-button
            type="primary"
            size="large"
            class="w-full mt-4"
            :loading="parsing"
            :disabled="!canParse"
            @click="handleParse"
          >
            <el-icon class="mr-2"><MagicStick /></el-icon>
            Bắt đầu Phân tích (AI)
          </el-button>
        </el-form>
      </el-card>
    </div>

    <div v-if="activeStep === 1" class="step-content">

      <div class="selection-toolbar flex justify-between items-center mb-4 p-3 bg-gray-50 rounded border">
        <div class="left-tools">
          <el-checkbox
            v-model="checkAll"
            :indeterminate="isIndeterminate"
            @change="handleCheckAllChange"
          >
            Chọn tất cả ({{ parsedLessons.length }})
          </el-checkbox>
          <span class="ml-4 text-sm text-gray-500">
            Đã chọn: <strong>{{ selectedCount }}</strong> bài
          </span>
        </div>
        <div class="right-tools">
           <el-tag type="warning">Review kỹ trước khi lưu</el-tag>
        </div>
      </div>

      <div class="lessons-preview">
        <el-collapse v-model="activeNames">
          <el-collapse-item
            v-for="(lesson, index) in parsedLessons"
            :key="index"
            :name="index"
            :class="{ 'is-selected': lesson.isSelected }"
          >
            <template #title>
              <div class="lesson-header w-full flex items-center pr-4" @click.stop>
                <el-checkbox v-model="lesson.isSelected" size="large" class="mr-3" />

                <el-tag :type="lesson.lessonType === 'THEORY' ? 'success' : 'warning'" size="small" class="mr-2">
                  {{ lesson.lessonType }}
                </el-tag>

                <span class="font-bold mr-2 flex-1 truncate">{{ lesson.title }}</span>

                <el-tag v-if="lesson.createQuestions?.length" size="small" type="danger" effect="dark">
                  {{ lesson.createQuestions.length }} câu hỏi
                </el-tag>
              </div>
            </template>

            <div class="lesson-body p-4 bg-white">
              <el-form-item label="Tiêu đề bài học">
                <el-input v-model="lesson.title" />
              </el-form-item>

              <div v-if="lesson.content" class="content-section mb-4">
                <div class="flex justify-between items-center mb-2">
                  <span class="label font-semibold">Nội dung bài học</span>
                  <el-switch
                    v-model="lesson.showHtmlSource"
                    active-text="Sửa HTML"
                    inactive-text="Xem trước"
                    size="small"
                  />
                </div>

                <el-input
                  v-if="lesson.showHtmlSource"
                  v-model="lesson.content"
                  type="textarea"
                  :rows="6"
                  placeholder="Nhập mã HTML..."
                />

                <div
                  v-else
                  class="html-preview-box"
                  v-html="lesson.content"
                ></div>
              </div>

              <div v-if="lesson.createQuestions?.length" class="questions-list mt-4">
                <div class="label font-semibold mb-2 text-blue-600">
                  Danh sách câu hỏi ({{ lesson.createQuestions.length }})
                  <!-- ✅ Show question type distribution -->
                  <div class="question-stats mt-2">
                    <el-tag
                      v-for="(count, type) in getQuestionTypeStats(lesson.createQuestions)"
                      :key="type"
                      size="small"
                      class="mr-1"
                    >
                      {{ type }}: {{ count }}
                    </el-tag>
                  </div>
                </div>
                <el-table :data="lesson.createQuestions" size="small" border stripe style="width: 100%">
                   <el-table-column type="index" width="50" align="center" />
                   <el-table-column prop="questionType" label="Loại" width="150">
                      <template #default="{ row }">
                        <el-tag size="small" :type="getQuestionTypeColor(row.questionType)">
                          {{ row.questionType }}
                        </el-tag>
                      </template>
                   </el-table-column>
                   <el-table-column prop="questionText" label="Nội dung câu hỏi" show-overflow-tooltip />
                   <el-table-column label="Chi tiết" width="80" align="center">
                      <template #default="{ row }">
                         <el-popover placement="left" title="Question Details" :width="400" trigger="hover">
                            <template #reference>
                               <el-button size="small" link><el-icon><View /></el-icon></el-button>
                            </template>
                            <div class="question-detail-popup">
                              <div class="mb-2">
                                <strong>Type:</strong>
                                <el-tag size="small" class="ml-1">{{ row.questionType }}</el-tag>
                              </div>
                              <div class="mb-2">
                                <strong>Question:</strong>
                                <div class="mt-1 text-sm">{{ row.questionText }}</div>
                              </div>

                              <!-- Show type-specific fields -->
                              <div v-if="row.options" class="mb-2">
                                <strong>Options ({{ row.options.length }}):</strong>
                                <ul class="text-xs mt-1 pl-4">
                                  <li v-for="opt in row.options" :key="opt.order">
                                    {{ opt.text }}
                                    <el-tag v-if="opt.isCorrect" size="small" type="success">✓</el-tag>
                                  </li>
                                </ul>
                              </div>

                              <div v-if="row.words" class="mb-2">
                                <strong>Words:</strong> {{ row.words.join(', ') }}
                              </div>

                              <div v-if="row.categories" class="mb-2">
                                <strong>Categories:</strong> {{ row.categories.join(', ') }}
                              </div>
                            </div>
                         </el-popover>
                      </template>
                   </el-table-column>
                </el-table>
              </div>
              <div v-else class="text-gray-400 text-sm italic py-2 text-center border border-dashed rounded mt-2">
                 (Không tìm thấy câu hỏi nào trong phần này)
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <div class="actions-footer mt-4 flex justify-end gap-3 sticky bottom-0 bg-white p-4 border-t z-10 shadow-up">
        <el-button @click="activeStep = 0">Hủy & Quay lại</el-button>
        <el-button
          type="success"
          :loading="saving"
          :disabled="selectedCount === 0"
          @click="handleSave"
          icon="Check"
        >
          Lưu {{ selectedCount }} bài đã chọn
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useGrammarStore } from '@/stores/grammar'
import { grammarAdminAPI } from '@/api/modules/grammar.api'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  UploadFilled,
  MagicStick,
  Document,
  Check,
  View,
  Setting,
  QuestionFilled,
  EditPen
} from '@element-plus/icons-vue'

const store = useGrammarStore()

// State
const activeStep = ref(0)
const selectedTopicId = ref(null)
const file = ref(null)
const pageRange = ref('')
const parsingContext = ref('') // ✅ NEW: Parsing context
const advancedOpen = ref([]) // Collapse control
const parsing = ref(false)
const saving = ref(false)

// Result Data
const parsedLessons = ref([])
const activeNames = ref([0])

// Checkbox Logic
const checkAll = ref(true)
const isIndeterminate = ref(false)

// ✅ PARSING CONTEXT TEMPLATES
const contextTemplates = ref({
  'Pronunciation': `Parse sections I, II and all exercises.
Each Exercise should be a SEPARATE lesson.
SKIP section III about -s endings.
Questions format "A. word B. word C. word" are MULTIPLE_CHOICE type.`,

  'Grammar Full': `Parse ALL content including theory and exercises.
Split each exercise into separate lessons.
Maintain question order as in the original document.`,

  'Theory Only': `Parse ONLY theory sections (I, II, III).
SKIP all exercises and practice sections.
Create ONE theory lesson with all content.`,

  'Exercises Only': `SKIP all theory sections.
Parse ONLY exercises and practice questions.
Each exercise is a separate PRACTICE lesson.`
})

// Computed
const topics = computed(() => store.topics)
const canParse = computed(() => selectedTopicId.value && file.value)
const selectedCount = computed(() => parsedLessons.value.filter(l => l.isSelected).length)

// --- Methods ---

const handleFileChange = (uploadFile) => {
  file.value = uploadFile.raw
}

const handleFileRemove = () => {
  file.value = null
}

const parsePageRange = (str) => {
  if (!str || !str.trim()) return null
  const pages = new Set()
  const parts = str.split(',')

  parts.forEach(part => {
    if (part.includes('-')) {
      const [start, end] = part.split('-').map(Number)
      if (!isNaN(start) && !isNaN(end)) {
        for (let i = start; i <= end; i++) pages.add(i)
      }
    } else {
      const num = Number(part)
      if (!isNaN(num)) pages.add(num)
    }
  })
  return Array.from(pages).sort((a, b) => a - b)
}

// ✅ Apply template
const applyTemplate = (templateKey) => {
  parsingContext.value = contextTemplates.value[templateKey]
  ElMessage.success(`Đã áp dụng template: ${templateKey}`)
}

// ✅ Get question type statistics
const getQuestionTypeStats = (questions) => {
  const stats = {}
  questions.forEach(q => {
    stats[q.questionType] = (stats[q.questionType] || 0) + 1
  })
  return stats
}

// ✅ Get color for question type tag
const getQuestionTypeColor = (type) => {
  const colorMap = {
    'MULTIPLE_CHOICE': 'primary',
    'PRONUNCIATION': 'warning',
    'TRUE_FALSE': 'success',
    'FILL_BLANK': 'info',
    'SENTENCE_TRANSFORMATION': 'danger',
    'SENTENCE_BUILDING': '',
    'MATCHING': 'warning',
    'ERROR_CORRECTION': 'danger'
  }
  return colorMap[type] || ''
}

// --- API ACTIONS ---

const handleParse = async () => {
  if (!file.value) return

  parsing.value = true
  try {
    const pages = parsePageRange(pageRange.value)

    // ✅ Call API with parsing context (4 parameters)
    const response = await grammarAdminAPI.parseFile(
      selectedTopicId.value,     // topicId
      file.value,                 // file
      pages,                      // pages array or null
      parsingContext.value.trim() || null // ✅ parsingContext
    )

    if (response.data.success) {
      parsedLessons.value = response.data.data.parsedData.lessons.map(l => ({
        ...l,
        isSelected: true,
        showHtmlSource: false
      }))

      activeNames.value = [0]
      activeStep.value = 1

      // ✅ Show parsing stats
      const stats = {}
      parsedLessons.value.forEach(lesson => {
        if (lesson.createQuestions) {
          lesson.createQuestions.forEach(q => {
            stats[q.questionType] = (stats[q.questionType] || 0) + 1
          })
        }
      })

      ElMessage.success({
        message: `Tìm thấy ${parsedLessons.value.length} bài học. Question types: ${JSON.stringify(stats)}`,
        duration: 5000
      })
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || 'Lỗi khi phân tích file. Vui lòng thử lại.')
  } finally {
    parsing.value = false
  }
}

// Logic Select All Checkbox
const handleCheckAllChange = (val) => {
  parsedLessons.value.forEach(item => (item.isSelected = val))
  isIndeterminate.value = false
}

watch(parsedLessons, (newVal) => {
  if (newVal.length === 0) return
  const checkedCount = newVal.filter(l => l.isSelected).length
  checkAll.value = checkedCount === newVal.length
  isIndeterminate.value = checkedCount > 0 && checkedCount < newVal.length
}, { deep: true })

const handleSave = async () => {
  const lessonsToSave = parsedLessons.value.filter(l => l.isSelected)

  if (lessonsToSave.length === 0) return

  try {
    await ElMessageBox.confirm(
      `Bạn có chắc muốn lưu ${lessonsToSave.length} bài học đã chọn vào CSDL?`,
      'Xác nhận lưu',
      { confirmButtonText: 'Lưu ngay', cancelButtonText: 'Hủy', type: 'warning' }
    )

    saving.value = true

    const payload = {
      lessons: lessonsToSave
    }

    const response = await grammarAdminAPI.saveParsedLessons(selectedTopicId.value, payload)

    if (response.data.success) {
      ElMessage.success(response.data.message)
      // Reset
      file.value = null
      pageRange.value = ''
      parsingContext.value = ''
      parsedLessons.value = []
      activeStep.value = 0
      advancedOpen.value = []
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error.response?.data?.message || 'Lỗi khi lưu dữ liệu.')
    }
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (store.topics.length === 0) {
    await store.fetchTopics({ size: 100 })
  }
})
</script>

<style scoped>
.ai-parsing-panel {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.upload-card {
  padding: 20px;
  text-align: center;
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
}

.config-form {
  text-align: left;
  max-width: 600px;
  margin: 0 auto;
}

.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  background-color: var(--el-bg-color);
  border-color: var(--el-border-color);
}

/* ✅ ADVANCED OPTIONS STYLING */
.advanced-options {
  margin-top: 16px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
}

.advanced-options :deep(.el-collapse-item__header) {
  padding: 12px 16px;
  background-color: var(--el-fill-color-lighter);
}

.advanced-options :deep(.el-collapse-item__content) {
  padding: 16px;
  background-color: var(--el-bg-color-overlay);
}

.template-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.parsing-help ul {
  padding-left: 0;
  list-style: none;
}

.parsing-help pre {
  margin-top: 8px;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.question-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.question-detail-popup {
  font-size: 13px;
}

.question-detail-popup strong {
  color: var(--el-text-color-primary);
}

/* --- SELECTION TOOLBAR --- */
.selection-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid var(--el-border-color);
  background-color: var(--el-fill-color-light);
}

/* --- LESSON ITEM STYLES --- */
.lesson-header {
  user-select: none;
  display: flex;
  align-items: center;
  width: 100%;
  padding-right: 16px;
}

.lesson-body {
  padding: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  background-color: var(--el-bg-color-overlay);
}

/* --- CONTENT SECTION --- */
.content-section {
  margin-bottom: 16px;
}

.label {
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.html-preview-box {
  padding: 15px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  min-height: 80px;
  max-height: 300px;
  overflow-y: auto;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  line-height: 1.6;
  background-color: var(--el-fill-color-lighter);
  color: var(--el-text-color-primary);
}

.html-preview-box :deep(h2), .html-preview-box :deep(h3) {
  font-weight: bold;
  margin-bottom: 0.5em;
  color: var(--el-text-color-primary);
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 5px;
}

.html-preview-box :deep(p) { margin-bottom: 0.8em; }
.html-preview-box :deep(ul) { list-style-type: disc; padding-left: 20px; margin-bottom: 1em; }
.html-preview-box :deep(table) { width: 100%; border-collapse: collapse; margin-bottom: 1em; }
.html-preview-box :deep(td), .html-preview-box :deep(th) {
  border: 1px solid var(--el-border-color);
  padding: 6px;
}

/* --- QUESTIONS LIST --- */
.questions-list {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed var(--el-border-color);
}

/* Footer Actions */
.actions-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  position: sticky;
  bottom: 0;
  padding: 16px;
  z-index: 10;
  background-color: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color);
  box-shadow: 0 -2px 10px rgba(0,0,0,0.1);
}

/* --- SELECTED STATE --- */
:deep(.is-selected .el-collapse-item__header) {
  background-color: var(--el-color-success-light-9);
}

html.dark :deep(.is-selected .el-collapse-item__header) {
  background-color: var(--el-color-success-dark-2);
}

/* Utilities */
.w-full { width: 100%; }
.mb-4 { margin-bottom: 16px; }
.mt-4 { margin-top: 16px; }
.mt-1 { margin-top: 4px; }
.mt-2 { margin-top: 8px; }
.mr-1 { margin-right: 4px; }
.mr-2 { margin-right: 8px; }
.mr-3 { margin-right: 12px; }
.ml-2 { margin-left: 8px; }
.ml-4 { margin-left: 16px; }
.flex { display: flex; }
.items-center { align-items: center; }
.justify-between { justify-content: space-between; }
.flex-wrap { flex-wrap: wrap; }
.gap-2 { gap: 8px; }
.cursor-pointer { cursor: pointer; }
.hover\:opacity-80:hover { opacity: 0.8; }
.space-y-1 > * + * { margin-top: 4px; }
</style>
