<template>
  <el-dialog v-model="dialogVisible" title="Thêm câu hỏi hàng loạt (Bulk Create)" width="98%" top="2vh"
    :close-on-click-modal="false" destroy-on-close class="bulk-create-dialog">
    <div class="dialog-body-layout">
      <div class="info-bar">
        <div class="info-left">
          <el-button-group class="mr-3">
            <el-button size="small" icon="ArrowUp" @click="collapseAll">Thu gọn</el-button>
            <el-button size="small" icon="ArrowDown" @click="expandAll">Mở rộng</el-button>
          </el-button-group>

          <el-switch v-model="showLessonContent" active-text="Hiện bài đọc" inactive-text="Ẩn" inline-prompt
            style="--el-switch-on-color: #13ce66;" />
        </div>

        <div class="quick-actions">
          <el-button type="primary" icon="Plus" @click="addNewQuestion">Thêm câu hỏi</el-button>
        </div>
      </div>

      <div class="main-content">

        <div v-if="showLessonContent" class="lesson-content-panel">
          <div class="panel-header">
            <el-icon>
              <Document />
            </el-icon> <b>Nội dung bài học</b>
          </div>
          <div class="panel-body">
            <div v-if="lessonContent" class="ql-editor" v-html="lessonContent"></div>
            <el-empty v-else description="Bài học này chưa có nội dung" :image-size="60" />
          </div>
        </div>

        <div class="questions-container" ref="scrollContainer">
          <div v-if="questions.length === 0" class="empty-state">
            <el-empty description="Chưa có câu hỏi nào">
              <el-button type="primary" @click="addNewQuestion">Bắt đầu tạo</el-button>
            </el-empty>
          </div>

          <div v-else>
            <div v-for="(q, idx) in questions" :key="idx" class="question-wrapper">
              <SingleQuestionEditor ref="questionRefs" v-model="questions[idx]" :index="idx"
                :is-initially-collapsed="q.isCollapsed" @remove="removeQuestion(idx)" />
            </div>

            <div class="add-more-placeholder" @click="addNewQuestion">
              <el-icon class="icon">
                <Plus />
              </el-icon>
              <span>Thêm câu hỏi tiếp theo</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <div class="footer-left">
          <el-text>Tổng: <b class="text-primary">{{ questions.length }}</b> câu</el-text>
        </div>
        <div class="footer-right">
          <el-button @click="closeDialog">Hủy</el-button>
          <el-button type="success" :loading="submitting" @click="submitAll" :disabled="questions.length === 0"
            icon="Check">
            Lưu tất cả ({{ questions.length }})
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, nextTick, defineAsyncComponent } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Check, ArrowUp, ArrowDown, Document } from '@element-plus/icons-vue'
import SingleQuestionEditor from '@/components/admin/questions/SingleQuestionEditor.vue'
import { useGrammarStore } from '@/stores/grammar'
import '@vueup/vue-quill/dist/vue-quill.snow.css'

const props = defineProps({ lessonId: { type: Number, default: null } })
const emit = defineEmits(['success'])
const store = useGrammarStore()

const dialogVisible = ref(false)
const submitting = ref(false)
const questions = ref([])
const scrollContainer = ref(null)
const questionRefs = ref([])

const showLessonContent = ref(false)
const lessonContent = ref('')

const createEmptyQuestion = (orderIndex) => ({
  parentId: props.lessonId,
  parentType: 'GRAMMAR',
  questionType: '', questionText: '', points: 5, orderIndex: orderIndex, metadata: {}, isCollapsed: false
})

const open = async () => {
  dialogVisible.value = true
  questions.value = []

  if (props.lessonId) {
    const lesson = await store.fetchLessonById(props.lessonId)
    if (lesson) {
      lessonContent.value = lesson.content
      if (lesson.content && lesson.content.length > 100) {
        showLessonContent.value = true
      }
    }
  }

  await addNewQuestion()
}

const addNewQuestion = async () => {
  collapseAll()
  let nextOrder = 1
  if (questions.value.length > 0) {
    nextOrder = questions.value[questions.value.length - 1].orderIndex + 1
  } else if (props.lessonId) {
    try {
      const serverOrder = await store.getNextQuestionOrderIndex(props.lessonId)
      nextOrder = serverOrder || 1
    } catch (e) { }
  }
  questions.value.push(createEmptyQuestion(nextOrder))
  nextTick(() => { if (scrollContainer.value) scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight })
}

const removeQuestion = (index) => { questions.value.splice(index, 1) }
const collapseAll = () => { if (questionRefs.value) questionRefs.value.forEach(comp => comp?.collapse && comp.collapse()) }
const expandAll = () => { if (questionRefs.value) questionRefs.value.forEach(comp => comp?.expand && comp.expand()) }

const submitAll = async () => {
  const emptyIndex = questions.value.findIndex(q => !q.questionType || !q.questionText)
  if (emptyIndex !== -1) {
    ElMessage.warning(`Câu hỏi #${emptyIndex + 1} chưa hoàn thiện.`)
    if (questionRefs.value[emptyIndex]) questionRefs.value[emptyIndex].expand()
    return
  }

  try {
    await ElMessageBox.confirm(`Lưu ${questions.value.length} câu hỏi?`, 'Xác nhận', { confirmButtonText: 'Lưu', cancelButtonText: 'Hủy' })
    submitting.value = true

    const payload = questions.value.map(q => {
      const p = {
        parentId: props.lessonId, parentType: 'GRAMMAR', questionType: q.questionType,
        questionText: q.questionText, points: q.points, orderIndex: q.orderIndex,
        explanation: q.metadata?.explanation || ''
      }
      const meta = q.metadata || {}

      if (q.questionType === 'MULTIPLE_CHOICE' || q.questionType === 'TRUE_FALSE' || q.questionType === 'COMPLETE_CONVERSATION') {
        p.options = meta.options
      }
      else if (q.questionType === 'FILL_BLANK' || q.questionType === 'TEXT_ANSWER' || q.questionType === 'VERB_FORM') {
        p.blanks = meta.blanks
        if (meta.wordBank?.length) p.wordBank = meta.wordBank
      }
      else if (q.questionType === 'MATCHING') p.pairs = meta.pairs
      else if (q.questionType === 'SENTENCE_BUILDING') { p.words = meta.words; p.correctSentence = meta.correctSentence }
      else if (q.questionType === 'SENTENCE_TRANSFORMATION') { p.originalSentence = meta.originalSentence; p.beginningPhrase = meta.beginningPhrase; p.correctAnswers = meta.correctAnswers }
      else if (q.questionType === 'ERROR_CORRECTION') { p.errorText = meta.errorText; p.correction = meta.correction }
      else if (q.questionType === 'PRONUNCIATION') { p.words = meta.words; p.categories = meta.categories; p.classifications = meta.classifications }
      else if (q.questionType === 'OPEN_ENDED') { p.suggestedAnswer = meta.suggestedAnswer; p.timeLimitSeconds = meta.timeLimitSeconds; p.minWord = meta.minWord; p.maxWord = meta.maxWord }
      else if (q.questionType === 'READING_COMPREHENSION') { p.passage = meta.passage; p.blanks = meta.blanks }

      return p
    })

    await store.createQuestionsInBulk(props.lessonId, payload)
    ElMessage.success('Thành công!')
    emit('success')
    closeDialog()
  } catch (error) { if (error !== 'cancel') console.error(error) } finally { submitting.value = false }
}

const closeDialog = () => { dialogVisible.value = false }
defineExpose({ open })
</script>

<style scoped>
.bulk-create-dialog :deep(.el-dialog__body) {
  padding: 0;
  height: 85vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.dialog-body-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.info-bar {
  padding: 8px 20px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 10;
}

.info-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* Lesson Panel */
.lesson-content-panel {
  width: 40%;
  background: white;
  border-right: 1px solid #dcdfe6;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  padding: 10px 15px;
  background: #f0f2f5;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

/* Questions Container */
.questions-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  scroll-behavior: smooth;
}

/* Add More Placeholder */
.add-more-placeholder {
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #909399;
  font-weight: 600;
  transition: all 0.3s;
  background: white;
}

.add-more-placeholder:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
}

.text-primary {
  color: var(--el-color-primary);
}

.mr-3 {
  margin-right: 12px;
}

/* --- DARK MODE --- */
html.dark .bulk-create-dialog :deep(.el-dialog__body) {
  background-color: var(--el-bg-color-page);
}

html.dark .info-bar {
  background-color: var(--el-bg-color);
  border-bottom-color: var(--el-border-color-darker);
}

html.dark .lesson-content-panel {
  background-color: var(--el-bg-color);
  border-right-color: var(--el-border-color-darker);
}

html.dark .panel-header {
  background-color: var(--el-fill-color-darker);
  border-bottom-color: var(--el-border-color-darker);
  color: var(--el-text-color-regular);
}

html.dark .add-more-placeholder {
  background-color: var(--el-bg-color-overlay);
  border-color: var(--el-border-color-darker);
  color: var(--el-text-color-secondary);
}

html.dark .add-more-placeholder:hover {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}
</style>
