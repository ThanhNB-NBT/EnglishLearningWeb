<!-- src/components/admin/questions/ReadingComprehensionForm.vue -->
<template>
  <div class="reading-comprehension-form">
    <!-- Hint (Optional) -->
    <el-form-item label="Hint (Optional)">
      <el-input
        v-model="localMetadata.hint"
        type="textarea"
        :rows="2"
        placeholder="Enter a hint to help users..."
        maxlength="200"
        show-word-limit
        @input="emitUpdate"
      />
    </el-form-item>

    <!-- Passage -->
    <el-form-item label="Reading Passage" required>
      <el-input
        v-model="localMetadata.passage"
        type="textarea"
        :rows="6"
        placeholder="Enter the reading passage with ___ for blanks (e.g., The cat ___ on the mat.)"
        @input="emitUpdate"
      />
      <template #extra>
        <el-text size="small" type="info">
          Use "___" to mark where blanks should be. Each ___ will be numbered automatically.
        </el-text>
      </template>
    </el-form-item>

    <!-- Blanks Count Info -->
    <el-alert
      v-if="blankCount > 0"
      :title="`Found ${blankCount} blank(s) in passage`"
      type="success"
      :closable="false"
      style="margin-bottom: 16px"
    />

    <!-- Blanks Configuration -->
    <el-form-item label="Blank Options & Answers" required>
      <el-space direction="vertical" fill style="width: 100%">
        <div
          v-for="(blank, index) in localMetadata.blanks"
          :key="index"
          class="blank-item"
        >
          <el-card shadow="hover" :body-style="{ padding: '12px' }">
            <div class="blank-header">
              <el-text tag="b">Blank {{ index + 1 }}</el-text>
              <el-button
                type="danger"
                size="small"
                :icon="Delete"
                circle
                @click="removeBlank(index)"
              />
            </div>

            <!-- Options -->
            <el-input
              v-model="blank.optionsInput"
              placeholder="Enter options separated by commas (e.g., is, was, are)"
              style="margin-top: 8px"
              @input="() => handleBlankOptionsInput(index)"
            />

            <!-- Options Preview -->
            <el-space wrap style="margin-top: 8px" v-if="blank.options && blank.options.length > 0">
              <el-tag
                v-for="(option, optIdx) in blank.options"
                :key="optIdx"
                size="small"
              >
                {{ option }}
              </el-tag>
            </el-space>

            <!-- Correct Answer -->
            <el-select
              v-model="blank.correctAnswer"
              placeholder="Select correct answer"
              style="width: 100%; margin-top: 8px"
              @change="emitUpdate"
            >
              <el-option
                v-for="option in blank.options"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
          </el-card>
        </div>

        <!-- Add Blank Button -->
        <el-button
          type="primary"
          :icon="Plus"
          @click="addBlank"
          style="width: 100%"
        >
          Add Blank
        </el-button>
      </el-space>
    </el-form-item>

    <!-- Validation Info -->
    <el-alert
      v-if="validationError"
      :title="validationError"
      type="error"
      :closable="false"
      style="margin-top: 12px"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  hint: props.metadata.hint || '',
  passage: props.metadata.passage || '',
  blanks: props.metadata.blanks || [],
})

watch(
  () => props.metadata,
  (newVal) => {
    if (newVal && Object.keys(newVal).length > 0) {
      localMetadata.value = {
        hint: newVal.hint || '',
        passage: newVal.passage || '',
        blanks: (newVal.blanks || []).map((blank) => ({
          ...blank,
          optionsInput: blank.options ? blank.options.join(', ') : '',
        })),
      }
    }
  },
  { deep: true }
)

const blankCount = computed(() => {
  return (localMetadata.value.passage.match(/___/g) || []).length
})

const validationError = computed(() => {
  if (!localMetadata.value.passage || localMetadata.value.passage.trim() === '') {
    return 'Passage is required'
  }

  if (!localMetadata.value.passage.includes('___')) {
    return 'Passage must include at least one "___" for blanks'
  }

  if (localMetadata.value.blanks.length === 0) {
    return 'Need at least 1 blank configuration'
  }

  const hasEmptyOptions = localMetadata.value.blanks.some(
    (b) => !b.options || b.options.length < 2
  )
  if (hasEmptyOptions) {
    return 'Each blank must have at least 2 options'
  }

  const hasEmptyAnswer = localMetadata.value.blanks.some(
    (b) => !b.correctAnswer || b.correctAnswer.trim() === ''
  )
  if (hasEmptyAnswer) {
    return 'Each blank must have a correct answer'
  }

  return null
})

const addBlank = () => {
  const nextPosition = localMetadata.value.blanks.length + 1
  localMetadata.value.blanks.push({
    position: nextPosition,
    options: [],
    optionsInput: '',
    correctAnswer: '',
  })
  emitUpdate()
}

const removeBlank = (index) => {
  localMetadata.value.blanks.splice(index, 1)

  // Re-index positions
  localMetadata.value.blanks.forEach((blank, idx) => {
    blank.position = idx + 1
  })

  emitUpdate()
}

const handleBlankOptionsInput = (index) => {
  const blank = localMetadata.value.blanks[index]
  const options = blank.optionsInput
    .split(',')
    .map((o) => o.trim())
    .filter((o) => o.length > 0)

  blank.options = options
  emitUpdate()
}

const emitUpdate = () => {
  // Clean data before emit
  const cleanedBlanks = localMetadata.value.blanks.map((blank) => ({
    position: blank.position,
    options: blank.options,
    correctAnswer: blank.correctAnswer,
  }))

  emit('update:metadata', {
    hint: localMetadata.value.hint,
    passage: localMetadata.value.passage,
    blanks: cleanedBlanks,
  })
}
</script>

<style scoped>
.reading-comprehension-form {
  padding: 8px 0;
}

.blank-item {
  width: 100%;
}

.blank-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
</style>
