<!-- src/components/admin/questions/ConversationForm.vue -->
<template>
  <div class="conversation-form">
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

    <!-- Conversation Context -->
    <el-form-item label="Conversation Context" required>
      <el-input
        v-model="localMetadata.conversationContext"
        type="textarea"
        :rows="4"
        placeholder="Enter the conversation context with ___ for the blank (e.g., A: How are you? B: ___ A: That's great!)"
        @input="emitUpdate"
      />
      <template #extra>
        <el-text size="small" type="info">
          Use "___" to mark where users should fill in the response.
        </el-text>
      </template>
    </el-form-item>

    <!-- Options -->
    <el-form-item label="Response Options" required>
      <el-input
        v-model="optionsInput"
        placeholder="Enter options separated by commas (e.g., I'm fine, Not bad, Great)"
        @input="handleOptionsInput"
      />
    </el-form-item>

    <!-- Options Preview -->
    <el-form-item label="Options Preview" v-if="localMetadata.options.length > 0">
      <el-space wrap>
        <el-tag
          v-for="(option, index) in localMetadata.options"
          :key="index"
          :type="option === localMetadata.correctAnswer ? 'success' : 'info'"
          closable
          @close="removeOption(index)"
          @click="setCorrectAnswer(option)"
          style="cursor: pointer"
        >
          {{ option }}
          <el-icon v-if="option === localMetadata.correctAnswer"><Check /></el-icon>
        </el-tag>
      </el-space>
      <template #extra>
        <el-text size="small" type="info">
          Click on an option to mark it as correct answer.
        </el-text>
      </template>
    </el-form-item>

    <!-- Correct Answer -->
    <el-form-item label="Correct Answer" required>
      <el-select
        v-model="localMetadata.correctAnswer"
        placeholder="Select correct answer"
        style="width: 100%"
        @change="emitUpdate"
      >
        <el-option
          v-for="option in localMetadata.options"
          :key="option"
          :label="option"
          :value="option"
        />
      </el-select>
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
import { Check } from '@element-plus/icons-vue'

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  hint: props.metadata.hint || '',
  conversationContext: props.metadata.conversationContext || '',
  options: props.metadata.options || [],
  correctAnswer: props.metadata.correctAnswer || '',
})

const optionsInput = ref(
  props.metadata.options ? props.metadata.options.join(', ') : ''
)

watch(
  () => props.metadata,
  (newVal) => {
    if (newVal && Object.keys(newVal).length > 0) {
      localMetadata.value = {
        hint: newVal.hint || '',
        conversationContext: newVal.conversationContext || '',
        options: newVal.options || [],
        correctAnswer: newVal.correctAnswer || '',
      }
      optionsInput.value = newVal.options ? newVal.options.join(', ') : ''
    }
  },
  { deep: true }
)

const validationError = computed(() => {
  if (!localMetadata.value.conversationContext || localMetadata.value.conversationContext.trim() === '') {
    return 'Conversation context is required'
  }

  if (!localMetadata.value.conversationContext.includes('___')) {
    return 'Conversation context must include "___" to mark the blank'
  }

  if (!localMetadata.value.options || localMetadata.value.options.length < 2) {
    return 'Need at least 2 response options'
  }

  if (!localMetadata.value.correctAnswer || localMetadata.value.correctAnswer.trim() === '') {
    return 'Correct answer must be selected'
  }

  return null
})

const handleOptionsInput = () => {
  const options = optionsInput.value
    .split(',')
    .map((o) => o.trim())
    .filter((o) => o.length > 0)

  localMetadata.value.options = options
  emitUpdate()
}

const removeOption = (index) => {
  const removedOption = localMetadata.value.options[index]
  localMetadata.value.options.splice(index, 1)

  // If removed option was correct answer, clear it
  if (localMetadata.value.correctAnswer === removedOption) {
    localMetadata.value.correctAnswer = ''
  }

  optionsInput.value = localMetadata.value.options.join(', ')
  emitUpdate()
}

const setCorrectAnswer = (option) => {
  localMetadata.value.correctAnswer = option
  emitUpdate()
}

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.conversation-form {
  padding: 8px 0;
}
</style>
