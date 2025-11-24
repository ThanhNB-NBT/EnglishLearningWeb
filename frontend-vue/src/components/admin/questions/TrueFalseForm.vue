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
      <el-radio-group v-model="localMetadata.correctAnswer" @change="emitUpdate">
        <el-radio :label="true">
          <el-text tag="b">True</el-text>
        </el-radio>
        <el-radio :label="false">
          <el-text tag="b">False</el-text>
        </el-radio>
      </el-radio-group>
    </el-form-item>

    <!-- Preview -->
    <el-alert
      :title="`Correct Answer: ${localMetadata.correctAnswer ? 'True' : 'False'}`"
      type="success"
      :closable="false"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  metadata: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  hint: props.metadata.hint || '',
  correctAnswer: props.metadata.correctAnswer ?? true,
})

watch(
  () => props.metadata,
  (newVal) => {
    if (newVal && Object.keys(newVal).length > 0) {
      localMetadata.value = {
        hint: newVal.hint || '',
        correctAnswer: newVal.correctAnswer ?? true,
      }
    }
  },
  { deep: true }
)

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.true-false-form {
  padding: 8px 0;
}
</style>
