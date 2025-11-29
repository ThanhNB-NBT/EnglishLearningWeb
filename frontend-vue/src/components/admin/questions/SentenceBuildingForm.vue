<!-- src/components/admin/questions/SentenceBuildingForm.vue -->
<template>
  <div class="sentence-building-form">
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

    <!-- Words (Scrambled) -->
    <el-form-item label="Words (Scrambled)" required>
      <el-input
        v-model="wordsInput"
        placeholder="Enter words separated by commas (e.g., is, This, a, sentence)"
        @input="handleWordsInput"
      />
      <template #extra>
        <el-text size="small" type="info">
          Separate words with commas. These will be shuffled for users to arrange.
        </el-text>
      </template>
    </el-form-item>

    <!-- Words Preview -->
    <el-form-item label="Words Preview" v-if="localMetadata.words.length > 0">
      <el-space wrap>
        <el-tag
          v-for="(word, index) in localMetadata.words"
          :key="index"
          closable
          @close="removeWord(index)"
        >
          {{ word }}
        </el-tag>
      </el-space>
    </el-form-item>

    <!-- Correct Sentence -->
    <el-form-item label="Correct Sentence" required>
      <el-input
        v-model="localMetadata.correctSentence"
        placeholder="Enter the correct sentence (e.g., This is a sentence)"
        @input="emitUpdate"
      />
      <template #extra>
        <el-text size="small" type="info">
          The correct sentence that users must build from the scrambled words.
        </el-text>
      </template>
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

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  hint: props.metadata.hint || '',
  words: props.metadata.words || [],
  correctSentence: props.metadata.correctSentence || '',
})

const wordsInput = ref(
  props.metadata.words ? props.metadata.words.join(', ') : ''
)

watch(
  () => props.metadata,
  (newVal) => {
    if (newVal && Object.keys(newVal).length > 0) {
      localMetadata.value = {
        hint: newVal.hint || '',
        words: newVal.words || [],
        correctSentence: newVal.correctSentence || '',
      }
      wordsInput.value = newVal.words ? newVal.words.join(', ') : ''
    }
  },
  { deep: true }
)

const validationError = computed(() => {
  if (!localMetadata.value.words || localMetadata.value.words.length < 2) {
    return 'Need at least 2 words'
  }

  if (!localMetadata.value.correctSentence || localMetadata.value.correctSentence.trim() === '') {
    return 'Correct sentence is required'
  }

  return null
})

const handleWordsInput = () => {
  const words = wordsInput.value
    .split(',')
    .map((w) => w.trim())
    .filter((w) => w.length > 0)

  localMetadata.value.words = words
  emitUpdate()
}

const removeWord = (index) => {
  localMetadata.value.words.splice(index, 1)
  wordsInput.value = localMetadata.value.words.join(', ')
  emitUpdate()
}

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.sentence-building-form {
  padding: 8px 0;
}
</style>
