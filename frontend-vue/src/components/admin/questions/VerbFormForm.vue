<template>
  <div class="verb-form-question">
    <el-alert title="Hướng dẫn" type="info" :closable="false" show-icon class="mb-4">
      <template #default>
        1. Trong phần "Nội dung câu hỏi" (Bước 1), hãy viết động từ cần chia trong ngoặc đơn. VD: <b>She (go) ___ to
          school.</b><br>
        2. Nhập các dạng chia đúng của động từ đó vào bên dưới.
      </template>
    </el-alert>

    <el-form-item label="Cấu hình đáp án" required>
      <div class="verbs-container">
        <transition-group name="list">
          <div v-for="(blank, index) in localMetadata.blanks" :key="index" class="verb-item mb-3">
            <el-card shadow="hover" :body-style="{ padding: '12px' }">
              <div class="verb-header mb-2">
                <el-tag effect="dark" size="small" type="success">Động từ #{{ index + 1 }}</el-tag>
                <el-button type="danger" link icon="Delete" @click="removeBlank(index)"
                  :disabled="localMetadata.blanks.length <= 1">
                  Xóa
                </el-button>
              </div>

              <el-row :gutter="12">
                <el-col :span="8">
                  <el-input v-model="blank.verb" placeholder="Động từ gốc (VD: go)" @input="emitUpdate">
                    <template #prepend>(V)</template>
                  </el-input>
                </el-col>

                <el-col :span="16">
                  <el-select v-model="blank.correctAnswers" multiple filterable allow-create default-first-option
                    :reserve-keyword="false" placeholder="Nhập đáp án đúng (VD: goes, is going)..." style="width: 100%"
                    @change="emitUpdate">
                    <template #empty>
                      <div class="p-2 text-gray-400 text-xs text-center">
                        Gõ đáp án và ấn Enter
                      </div>
                    </template>
                  </el-select>
                </el-col>
              </el-row>
            </el-card>
          </div>
        </transition-group>

        <el-button type="primary" plain icon="Plus" class="w-full mt-2 dashed-btn" @click="addBlank">
          Thêm động từ
        </el-button>
      </div>
    </el-form-item>

    <el-form-item label="Giải thích (Optional)">
      <el-input v-model="localMetadata.explanation" type="textarea" :rows="2" placeholder="Giải thích thì/cấu trúc..."
        @input="emitUpdate" />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ metadata: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:metadata'])

const localMetadata = ref({
  blanks: props.metadata?.blanks || [{ position: 1, verb: '', correctAnswers: [] }],
  explanation: props.metadata?.explanation || ''
})

watch(() => props.metadata, (newVal) => {
  if (newVal && newVal.blanks) localMetadata.value = newVal
}, { deep: true })

const addBlank = () => {
  localMetadata.value.blanks.push({ position: localMetadata.value.blanks.length + 1, verb: '', correctAnswers: [] })
  emitUpdate()
}

const removeBlank = (index) => {
  if (localMetadata.value.blanks.length <= 1) return
  localMetadata.value.blanks.splice(index, 1)
  emitUpdate()
}

const emitUpdate = () => emit('update:metadata', { ...localMetadata.value })
</script>

<style scoped>
.verb-form-question {
  padding: 10px 0;
}

.verb-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.w-full {
  width: 100%;
}

.dashed-btn {
  border-style: dashed;
}

.mb-2 {
  margin-bottom: 8px;
}

.mb-3 {
  margin-bottom: 12px;
}
</style>
