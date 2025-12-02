<!-- src/components/common/QuillRichEditor.vue - Enhanced Version -->
<template>
  <div class="quill-editor-wrapper">
    <QuillEditor
      ref="quillEditorRef"
      v-model:content="localContent"
      content-type="html"
      :theme="theme"
      :toolbar="customToolbar"
      :style="{ height: height }"
      @ready="onEditorReady"
      @update:content="handleContentUpdate"
      @text-change="handleTextChange"
    />

    <!-- Word count display -->
    <div v-if="showWordCount" class="editor-footer">
      <div class="word-count">
        <el-text size="small" type="info">
          {{ wordCount }} từ • {{ charCount }} ký tự
        </el-text>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'

// Props
const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  theme: {
    type: String,
    default: 'snow' // 'snow' | 'bubble'
  },
  height: {
    type: String,
    default: '400px'
  },
  placeholder: {
    type: String,
    default: 'Nhập nội dung...'
  },
  toolbar: {
    type: [Array, String],
    default: 'full' // 'full' | 'essential' | 'minimal' | 'lesson' | custom array
  },
  readOnly: {
    type: Boolean,
    default: false
  },
  showWordCount: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'text-change'])

// Refs
const quillEditorRef = ref(null)
const localContent = ref(props.modelValue || '')
const wordCount = ref(0)
const charCount = ref(0)

// ✅ Enhanced toolbar configurations
const toolbarConfigs = {
  // Complete toolbar with all features
  full: [
    [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
    [{ 'font': [] }],
    [{ 'size': ['small', false, 'large', 'huge'] }],
    ['bold', 'italic', 'underline', 'strike'],
    [{ 'color': [] }, { 'background': [] }],
    [{ 'script': 'sub'}, { 'script': 'super' }],
    [{ 'list': 'ordered'}, { 'list': 'bullet' }, { 'list': 'check' }],
    [{ 'indent': '-1'}, { 'indent': '+1' }],
    [{ 'direction': 'rtl' }],
    [{ 'align': [] }],
    ['blockquote', 'code-block'],
    ['link', 'image', 'video', 'formula'],
    ['clean']
  ],

  // ✅ NEW: Optimized for lesson content (grammar lessons, theory)
  lesson: [
    [{ 'header': [1, 2, 3, false] }],
    ['bold', 'italic', 'underline', 'strike'],
    [{ 'color': [] }, { 'background': [] }],
    [{ 'list': 'ordered'}, { 'list': 'bullet' }, { 'list': 'check' }],
    [{ 'indent': '-1'}, { 'indent': '+1' }],
    [{ 'align': [] }],
    ['blockquote', 'code-block'],
    [{ 'script': 'sub'}, { 'script': 'super' }],
    ['link', 'image'],
    ['clean']
  ],

  // Essential features for general content
  essential: [
    [{ 'header': [1, 2, 3, false] }],
    ['bold', 'italic', 'underline'],
    [{ 'list': 'ordered'}, { 'list': 'bullet' }],
    [{ 'color': [] }, { 'background': [] }],
    [{ 'align': [] }],
    ['link', 'image'],
    ['clean']
  ],

  // Minimal for simple text
  minimal: [
    ['bold', 'italic', 'underline'],
    [{ 'list': 'ordered'}, { 'list': 'bullet' }],
    ['link'],
    ['clean']
  ],

  // ✅ NEW: Question content (for question text)
  question: [
    [{ 'header': [2, 3, false] }],
    ['bold', 'italic', 'underline'],
    [{ 'color': [] }],
    [{ 'list': 'ordered'}, { 'list': 'bullet' }],
    ['code-block'],
    ['link', 'image'],
    ['clean']
  ],

  // ✅ NEW: Simple formatting (for descriptions)
  simple: [
    ['bold', 'italic'],
    [{ 'list': 'bullet' }],
    ['link'],
    ['clean']
  ]
}

// Computed toolbar based on prop
const customToolbar = computed(() => {
  if (Array.isArray(props.toolbar)) {
    return props.toolbar
  }
  return toolbarConfigs[props.toolbar] || toolbarConfigs.full
})

// Watch props.modelValue to sync with parent
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal !== localContent.value) {
      localContent.value = newVal || ''
      updateWordCount(newVal)
    }
  }
)

// Handle content update
const handleContentUpdate = (content) => {
  emit('update:modelValue', content)
  updateWordCount(content)
}

// Handle text change event
const handleTextChange = (delta, oldDelta, source) => {
  emit('text-change', { delta, oldDelta, source })
}

// Update word and character count
const updateWordCount = (html) => {
  if (!html) {
    wordCount.value = 0
    charCount.value = 0
    return
  }

  // Strip HTML tags to get plain text
  const div = document.createElement('div')
  div.innerHTML = html
  const text = div.textContent || div.innerText || ''

  // Count characters (excluding spaces)
  charCount.value = text.replace(/\s/g, '').length

  // Count words
  const words = text.trim().split(/\s+/).filter(word => word.length > 0)
  wordCount.value = words.length
}

// Editor ready callback
const onEditorReady = (editor) => {
  console.log('✅ Quill editor ready')

  // Set placeholder
  if (props.placeholder) {
    editor.root.dataset.placeholder = props.placeholder
  }

  // Set readOnly
  if (props.readOnly) {
    editor.enable(false)
  }

  // Initial word count
  updateWordCount(localContent.value)
}

// Public methods (expose to parent if needed)
defineExpose({
  getEditor: () => quillEditorRef.value?.getQuill(),
  setContent: (content) => {
    localContent.value = content
    updateWordCount(content)
  },
  clear: () => {
    localContent.value = ''
    wordCount.value = 0
    charCount.value = 0
  },
  getWordCount: () => wordCount.value,
  getCharCount: () => charCount.value
})
</script>

<style scoped>
.quill-editor-wrapper {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  overflow: hidden;
  transition: all 0.3s;
  background: var(--el-bg-color);
}

.quill-editor-wrapper:hover {
  border-color: var(--el-color-primary-light-5);
}

.quill-editor-wrapper:focus-within {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 3px var(--el-color-primary-light-9);
}

/* Editor footer for word count */
.editor-footer {
  border-top: 1px solid var(--el-border-color-lighter);
  padding: 6px 12px;
  background: var(--el-fill-color-lighter);
  display: flex;
  justify-content: flex-end;
}

.word-count {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Toolbar styling */
:deep(.ql-toolbar) {
  background-color: var(--el-fill-color-lighter);
  border: none;
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

:deep(.ql-toolbar .ql-formats) {
  margin-right: 8px;
}

:deep(.ql-toolbar .ql-stroke) {
  stroke: var(--el-text-color-primary);
  transition: stroke 0.2s;
}

:deep(.ql-toolbar .ql-fill) {
  fill: var(--el-text-color-primary);
  transition: fill 0.2s;
}

:deep(.ql-toolbar .ql-picker-label) {
  color: var(--el-text-color-primary);
}

:deep(.ql-toolbar button) {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  transition: all 0.2s;
}

:deep(.ql-toolbar button:hover) {
  background-color: var(--el-color-primary-light-9);
}

:deep(.ql-toolbar button:hover .ql-stroke) {
  stroke: var(--el-color-primary);
}

:deep(.ql-toolbar button:hover .ql-fill) {
  fill: var(--el-color-primary);
}

:deep(.ql-toolbar button.ql-active) {
  background-color: var(--el-color-primary-light-8);
}

:deep(.ql-toolbar button.ql-active .ql-stroke) {
  stroke: var(--el-color-primary);
}

:deep(.ql-toolbar button.ql-active .ql-fill) {
  fill: var(--el-color-primary);
}

/* Picker dropdowns */
:deep(.ql-toolbar .ql-picker) {
  color: var(--el-text-color-primary);
}

:deep(.ql-toolbar .ql-picker:hover) {
  color: var(--el-color-primary);
}

:deep(.ql-toolbar .ql-picker-options) {
  background-color: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  box-shadow: var(--el-box-shadow-light);
}

/* Container styling */
:deep(.ql-container) {
  font-family: inherit;
  font-size: 15px;
  background-color: var(--el-bg-color);
  border: none;
}

:deep(.ql-editor) {
  min-height: 250px;
  max-height: 600px;
  overflow-y: auto;
  padding: 20px;
  line-height: 1.8;
  color: var(--el-text-color-primary);
}

:deep(.ql-editor:focus) {
  outline: none;
}

/* Placeholder styling */
:deep(.ql-editor.ql-blank::before) {
  color: var(--el-text-color-placeholder);
  font-style: italic;
  font-size: 14px;
}

/* Scrollbar styling */
:deep(.ql-editor::-webkit-scrollbar) {
  width: 8px;
}

:deep(.ql-editor::-webkit-scrollbar-track) {
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
}

:deep(.ql-editor::-webkit-scrollbar-thumb) {
  background: var(--el-border-color);
  border-radius: 4px;
  transition: background 0.2s;
}

:deep(.ql-editor::-webkit-scrollbar-thumb:hover) {
  background: var(--el-text-color-secondary);
}

/* ✅ Enhanced content styling */
:deep(.ql-editor h1) {
  font-size: 2em;
  font-weight: 700;
  margin: 0.8em 0 0.5em;
  padding-bottom: 0.3em;
  border-bottom: 2px solid var(--el-border-color-lighter);
  color: var(--el-text-color-primary);
}

:deep(.ql-editor h2) {
  font-size: 1.6em;
  font-weight: 600;
  margin: 0.7em 0 0.4em;
  color: var(--el-text-color-primary);
}

:deep(.ql-editor h3) {
  font-size: 1.3em;
  font-weight: 600;
  margin: 0.6em 0 0.3em;
  color: var(--el-text-color-primary);
}

:deep(.ql-editor h4) {
  font-size: 1.1em;
  font-weight: 600;
  margin: 0.5em 0 0.3em;
  color: var(--el-text-color-regular);
}

:deep(.ql-editor p) {
  margin: 0.6em 0;
  line-height: 1.8;
}

:deep(.ql-editor strong) {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

:deep(.ql-editor em) {
  font-style: italic;
}

:deep(.ql-editor u) {
  text-decoration: underline;
}

:deep(.ql-editor s) {
  text-decoration: line-through;
}

/* Lists */
:deep(.ql-editor ul),
:deep(.ql-editor ol) {
  padding-left: 1.5em;
  margin: 0.8em 0;
}

:deep(.ql-editor li) {
  margin: 0.4em 0;
  line-height: 1.6;
}

:deep(.ql-editor ul > li::marker) {
  color: var(--el-color-primary);
}

:deep(.ql-editor ol > li::marker) {
  color: var(--el-color-primary);
  font-weight: 600;
}

/* Checklist */
:deep(.ql-editor .ql-list[data-list="check"]) {
  list-style: none;
  padding-left: 0;
}

:deep(.ql-editor .ql-list[data-list="check"] > li) {
  padding-left: 1.8em;
  position: relative;
}

:deep(.ql-editor .ql-list[data-list="check"] > li::before) {
  content: '☐';
  position: absolute;
  left: 0;
  color: var(--el-color-primary);
  font-size: 1.2em;
}

:deep(.ql-editor .ql-list[data-list="check"] > li[data-checked="true"]::before) {
  content: '☑';
  color: var(--el-color-success);
}

/* Code styling */
:deep(.ql-editor code) {
  background-color: var(--el-fill-color-light);
  color: var(--el-color-danger);
  padding: 3px 6px;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.9em;
}

:deep(.ql-editor pre.ql-syntax) {
  background-color: #282c34;
  color: #abb2bf;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.9em;
  line-height: 1.6;
  margin: 1em 0;
  border: 1px solid var(--el-border-color);
}

/* Blockquote */
:deep(.ql-editor blockquote) {
  border-left: 4px solid var(--el-color-primary);
  background-color: var(--el-fill-color-light);
  padding: 12px 16px;
  margin: 1em 0;
  border-radius: 0 4px 4px 0;
  color: var(--el-text-color-secondary);
  font-style: italic;
}

/* Links */
:deep(.ql-editor a) {
  color: var(--el-color-primary);
  text-decoration: underline;
  transition: color 0.2s;
}

:deep(.ql-editor a:hover) {
  color: var(--el-color-primary-dark-2);
}

/* Images */
:deep(.ql-editor img) {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
  margin: 1em 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* Videos */
:deep(.ql-editor iframe) {
  max-width: 100%;
  border-radius: 6px;
  margin: 1em 0;
}

/* Alignment */
:deep(.ql-editor .ql-align-center) {
  text-align: center;
}

:deep(.ql-editor .ql-align-right) {
  text-align: right;
}

:deep(.ql-editor .ql-align-justify) {
  text-align: justify;
}

/* Indentation */
:deep(.ql-editor .ql-indent-1) {
  padding-left: 3em;
}

:deep(.ql-editor .ql-indent-2) {
  padding-left: 6em;
}

:deep(.ql-editor .ql-indent-3) {
  padding-left: 9em;
}

/* Subscript & Superscript */
:deep(.ql-editor sub) {
  vertical-align: sub;
  font-size: 0.8em;
}

:deep(.ql-editor sup) {
  vertical-align: super;
  font-size: 0.8em;
}

/* Dark mode support */
html.dark :deep(.ql-toolbar) {
  background-color: var(--el-bg-color-overlay);
  border-bottom-color: var(--el-border-color-darker);
}

html.dark :deep(.ql-container) {
  background-color: var(--el-bg-color);
}

html.dark :deep(.ql-editor) {
  color: var(--el-text-color-primary);
}

html.dark :deep(.ql-toolbar .ql-stroke) {
  stroke: var(--el-text-color-regular);
}

html.dark :deep(.ql-toolbar .ql-fill) {
  fill: var(--el-text-color-regular);
}

html.dark :deep(.ql-editor pre.ql-syntax) {
  background-color: #1e1e1e;
  border-color: var(--el-border-color-darker);
}

html.dark :deep(.ql-editor blockquote) {
  background-color: var(--el-fill-color-dark);
  border-left-color: var(--el-color-primary);
}

html.dark :deep(.ql-editor code) {
  background-color: var(--el-fill-color-dark);
}

/* ✅ Responsive */
@media (max-width: 768px) {
  :deep(.ql-toolbar) {
    padding: 8px;
  }

  :deep(.ql-toolbar button) {
    width: 24px;
    height: 24px;
  }

  :deep(.ql-editor) {
    padding: 16px;
    font-size: 14px;
    min-height: 200px;
  }

  :deep(.ql-editor h1) {
    font-size: 1.6em;
  }

  :deep(.ql-editor h2) {
    font-size: 1.4em;
  }

  :deep(.ql-editor h3) {
    font-size: 1.2em;
  }

  .editor-footer {
    padding: 4px 8px;
  }
}

@media (max-width: 480px) {
  :deep(.ql-toolbar) {
    padding: 6px;
  }

  :deep(.ql-toolbar .ql-formats) {
    margin-right: 4px;
  }

  :deep(.ql-editor) {
    padding: 12px;
    font-size: 13px;
  }
}
</style>
