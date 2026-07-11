<template>
  <div class="rich-text-editor">
    <div v-if="editor" class="rt-toolbar">
      <a-space size="small" wrap>
        <a-button size="small" :type="editor.isActive('bold') ? 'primary' : 'default'" @click="editor.chain().focus().toggleBold().run()">B</a-button>
        <a-button size="small" :type="editor.isActive('italic') ? 'primary' : 'default'" @click="editor.chain().focus().toggleItalic().run()"><i>I</i></a-button>
        <a-button size="small" :type="editor.isActive('heading', { level: 2 }) ? 'primary' : 'default'" @click="editor.chain().focus().toggleHeading({ level: 2 }).run()">H2</a-button>
        <a-button size="small" :type="editor.isActive('heading', { level: 3 }) ? 'primary' : 'default'" @click="editor.chain().focus().toggleHeading({ level: 3 }).run()">H3</a-button>
        <a-button size="small" @click="editor.chain().focus().toggleBulletList().run()">• 列表</a-button>
        <a-button size="small" @click="editor.chain().focus().toggleOrderedList().run()">1. 列表</a-button>
        <a-button size="small" @click="editor.chain().focus().toggleBlockquote().run()">引用</a-button>
        <a-button size="small" @click="editor.chain().focus().toggleCodeBlock().run()">代码块</a-button>
        <a-button size="small" @click="editor.chain().focus().undo().run()">↶</a-button>
        <a-button size="small" @click="editor.chain().focus().redo().run()">↷</a-button>
      </a-space>
    </div>
    <editor-content :editor="editor" class="rt-content" />
  </div>
</template>

<script setup>
import { onBeforeUnmount, watch } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'

const props = defineProps({ html: { type: String, default: '' } })
const emit = defineEmits(['update:html'])

const editor = useEditor({
  content: props.html,
  extensions: [StarterKit, Image],
  onUpdate({ editor }) {
    emit('update:html', editor.getHTML())
  }
})

watch(() => props.html, (val) => {
  if (editor.value && val !== editor.value.getHTML()) {
    editor.value.commands.setContent(val || '', false)
  }
})

onBeforeUnmount(() => editor.value?.destroy())
</script>

<style scoped>
.rich-text-editor { border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.rt-toolbar { border-bottom: 1px solid #f0f0f0; padding: 8px; }
.rt-content { padding: 12px 16px; min-height: 400px; line-height: 1.7; }
:deep(.ProseMirror) { outline: none; min-height: 380px; }
:deep(.ProseMirror p) { margin: 0.6em 0; }
:deep(.ProseMirror h2) { font-size: 20px; margin: 1em 0 0.4em; font-weight: 700; }
:deep(.ProseMirror h3) { font-size: 17px; margin: 0.8em 0 0.4em; font-weight: 600; }
:deep(.ProseMirror blockquote) { border-left: 4px solid #ff2442; padding: 6px 12px; color: #555; margin: 0.8em 0; background: #f8f8f8; }
:deep(.ProseMirror pre) { background: #1f1f1f; color: #f5f5f5; padding: 12px 16px; border-radius: 6px; overflow-x: auto; font-size: 13px; }
:deep(.ProseMirror code) { background: #f6f8fa; padding: 2px 6px; border-radius: 4px; font-size: 13px; }
:deep(.ProseMirror img) { max-width: 100%; }
</style>
