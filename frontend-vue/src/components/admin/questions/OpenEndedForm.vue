<!-- src/components/admin/questions/OpenEndedForm.vue -->
<template>
  <div class="open-ended-form">
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

    <!-- Suggested Answer (Optional) -->
    <el-form-item label="Suggested Answer (Optional)">
      <el-input
        v-model="localMetadata.suggestedAnswer"
        type="textarea"
        :rows="4"
        placeholder="Enter a suggested answer for AI/teacher reference..."
        @input="emitUpdate"
      />
      <template #extra>
        <el-text size="small" type="info">
          This answer will be used by AI/teacher to evaluate student responses.
        </el-text>
      </template>
    </el-form-item>

    <!-- Time Limit (Optional) -->
    <el-form-item label="Time Limit (seconds)">
      <el-input-number
        v-model="localMetadata.timeLimitSeconds"
        :min="0"
        :max="3600"
        placeholder="No limit"
        style="width: 100%"
        @change="emitUpdate"
      />
      <template #extra>
        <el-text size="small" type="info">
          Set to 0 or leave empty for no time limit.
        </el-text>
      </template>
    </el-form-item>

    <!-- Word Count Range -->
    <el-row :gutter="12">
      <el-col :span="12">
        <el-form-item label="Minimum Words">
          <el-input-number
            v-model="localMetadata.minWord"
            :min="0"
            :max="localMetadata.maxWord || 1000"
            placeholder="No minimum"
            style="width: 100%"
            @change="emitUpdate"
          />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item label="Maximum Words">
          <el-input-number
            v-model="localMetadata.maxWord"
            :min="localMetadata.minWord || 0"
            :max="5000"
            placeholder="No maximum"
            style="width: 100%"
            @change="emitUpdate"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- Info Alert -->
    <el-alert
      title="Note: Open-Ended Questions"
      type="info"
      :closable="false"
      style="margin-top: 12px"
    >
      <template #default>
        <p>This question type requires AI or teacher evaluation.</p>
        <p>Students can submit free-form text answers.</p>
        <p>Suggested answer helps AI/teacher evaluate responses.</p>
      </template>
    </el-alert>
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
  suggestedAnswer: props.metadata.suggestedAnswer || '',
  timeLimitSeconds: props.metadata.timeLimitSeconds || null,
  minWord: props.metadata.minWord || null,
  maxWord: props.metadata.maxWord || null,
})

watch(
  () => props.metadata,
  (newVal) => {
    if (newVal && Object.keys(newVal).length > 0) {
      localMetadata.value = {
        hint: newVal.hint || '',
        suggestedAnswer: newVal.suggestedAnswer || '',
        timeLimitSeconds: newVal.timeLimitSeconds || null,
        minWord: newVal.minWord || null,
        maxWord: newVal.maxWord || null,
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
.open-ended-form {
  padding: 8px 0;
}
</style>
