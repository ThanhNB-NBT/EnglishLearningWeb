<template>
  <div class="conversation-form">
    <el-alert title="Tạo hội thoại" type="success" :closable="false" class="mb-4">
      <template #default>
        Thêm các dòng thoại cho nhân vật A và B. Để tạo chỗ trống cần điền, hãy viết từ đó trong ngoặc vuông. <br>
        Ví dụ: <b>Hello, how [are] you?</b> (Người dùng sẽ phải điền từ "are").
      </template>
    </el-alert>

    <div class="chat-container">
      <transition-group name="list">
        <div v-for="(line, index) in localMetadata.dialogue" :key="index" class="chat-line mb-3"
          :class="{ 'is-right': line.speaker === 'B' }">
          <div class="speaker-avatar" :class="line.speaker === 'A' ? 'bg-primary' : 'bg-warning'">
            {{ line.speaker }}
          </div>

          <div class="chat-bubble">
            <div class="bubble-header">
              <span class="text-xs text-gray-400">Dòng #{{ index + 1 }}</span>
              <el-button type="danger" link icon="Delete" size="small" @click="removeLine(index)" />
            </div>

            <el-input v-model="line.text" type="textarea" :rows="2"
              placeholder="Nhập lời thoại... VD: Nice to [meet] you." @input="emitUpdate" />
          </div>
        </div>
      </transition-group>
    </div>

    <div class="actions-bar mt-4">
      <el-button type="primary" plain @click="addLine('A')">+ Thêm thoại A</el-button>
      <el-button type="warning" plain @click="addLine('B')">+ Thêm thoại B</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  metadata: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:metadata'])

// Init data
const localMetadata = ref({
  dialogue: props.metadata?.dialogue || [
    { speaker: 'A', text: '' },
    { speaker: 'B', text: '' }
  ]
})

watch(() => props.metadata, (newVal) => {
  if (newVal && newVal.dialogue) {
    localMetadata.value.dialogue = newVal.dialogue
  }
}, { deep: true })

// Actions
const addLine = (speaker) => {
  localMetadata.value.dialogue.push({ speaker, text: '' })
  emitUpdate()
}

const removeLine = (index) => {
  localMetadata.value.dialogue.splice(index, 1)
  emitUpdate()
}

const emitUpdate = () => {
  emit('update:metadata', { ...localMetadata.value })
}
</script>

<style scoped>
.conversation-form {
  padding: 10px 0;
}

.chat-container {
  background: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #eee;
  max-height: 500px;
  overflow-y: auto;
}

.chat-line {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  max-width: 85%;
}

.chat-line.is-right {
  margin-left: auto;
  flex-direction: row-reverse;
}

.speaker-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  flex-shrink: 0;
}

.bg-primary {
  background-color: #409eff;
}

.bg-warning {
  background-color: #e6a23c;
}

.chat-bubble {
  background: white;
  padding: 10px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  flex: 1;
  position: relative;
}

.chat-line.is-right .chat-bubble {
  background: #ecf5ff;
  /* Màu xanh nhạt cho B */
}

.bubble-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.actions-bar {
  display: flex;
  justify-content: center;
  gap: 20px;
}

/* Transition */
.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
