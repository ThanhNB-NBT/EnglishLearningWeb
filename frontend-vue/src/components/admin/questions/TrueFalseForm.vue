<template>
  <div class="true-false-form">
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

    <!-- Correct Answer -->
    <el-form-item label="Correct Answer" required>
      <el-radio-group v-model="correctAnswerValue" @change="handleAnswerChange">
        <el-radio :value="true">
          <el-text tag="b">True</el-text>
        </el-radio>
        <el-radio :value="false">
          <el-text tag="b">False</el-text>
        </el-radio>
      </el-radio-group>
    </el-form-item>

    <!-- Preview -->
    <el-alert
      v-if="correctAnswerValue !== null"
      :title="`Correct Answer: ${correctAnswerValue ? 'True' : 'False'}`"
      type="success"
      :closable="false"
    />

    <!-- Validation -->
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

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:metadata'])

// Extract correctAnswer từ options (nếu có)
const getCorrectAnswerFromOptions = (options) => {
  if (!options || options.length === 0) return null
  const trueOption = options.find(opt => opt.text === 'True')
  return trueOption ? trueOption.isCorrect : null
}

// State: correctAnswerValue (boolean hoặc null)
const correctAnswerValue = ref(
  props.metadata.correctAnswer !== null && props.metadata.correctAnswer !== undefined
    ? props.metadata.correctAnswer
    : getCorrectAnswerFromOptions(props.metadata.options)
)

const localMetadata = ref({
  hint: props.metadata.hint || '',
})

// Watch props.metadata changes
watch(
  () => props.metadata,
  (newVal) => {
    if (newVal && Object.keys(newVal).length > 0) {
      localMetadata.value.hint = newVal.hint || ''

      // Extract correctAnswer từ options hoặc correctAnswer field
      const extracted = newVal.correctAnswer !== null && newVal.correctAnswer !== undefined
        ? newVal.correctAnswer
        : getCorrectAnswerFromOptions(newVal.options)

      correctAnswerValue.value = extracted
    }
  },
  { deep: true }
)

// Validation
const validationError = computed(() => {
  if (correctAnswerValue.value === null || correctAnswerValue.value === undefined) {
    return 'Please select True or False'
  }
  return null
})

// Handle answer change
const handleAnswerChange = () => {
  emitUpdate()
}

// Emit metadata với cấu trúc correctAnswer (backend sẽ convert sang options)
const emitUpdate = () => {
  emit('update:metadata', {
    hint: localMetadata.value.hint,
    correctAnswer: correctAnswerValue.value, // ← Gửi correctAnswer
  })
}
</script>

<style scoped>
.true-false-form {
  padding: 8px 0;
}
</style>
