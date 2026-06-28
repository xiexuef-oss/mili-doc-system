<template>
  <div class="block-renderer">
    <template v-for="block in blocks" :key="block.id">
      <!-- Title -->
      <h1 v-if="block.type==='title'" class="br-title">{{ stripInline(block.content) }}</h1>
      <!-- Headings -->
      <h2 v-else-if="block.type==='h2'" class="br-h2">{{ stripInline(block.content) }}</h2>
      <h3 v-else-if="block.type==='h3'" class="br-h3">{{ stripInline(block.content) }}</h3>
      <h4 v-else-if="block.type==='h4'" class="br-h4">{{ stripInline(block.content) }}</h4>
      <!-- Paragraph with inline tags -->
      <p v-else-if="block.type==='p'" class="br-p" v-html="renderInline(block.content)"/>
      <!-- Lists -->
      <ul v-else-if="block.type==='ul'" class="br-ul">
        <li v-for="(item,i) in extractListItems(block.content)" :key="i" v-html="renderInline(item)"/>
      </ul>
      <ol v-else-if="block.type==='ol'" class="br-ol">
        <li v-for="(item,i) in extractListItems(block.content)" :key="i" v-html="renderInline(item)"/>
      </ol>
      <!-- Blockquote -->
      <blockquote v-else-if="block.type==='blockquote'" class="br-blockquote" v-html="renderInline(block.content)"/>
      <!-- Horizontal rule -->
      <hr v-else-if="block.type==='hr'" class="br-hr"/>
      <!-- Table -->
      <div v-else-if="block.type==='table'" class="br-table-wrap">
        <table class="br-table"><tbody>
          <tr v-for="(row,ri) in parseTableRows(block.content)" :key="ri">
            <td v-for="(cell,ci) in row" :key="ci" v-html="renderInline(cell)"/>
          </tr>
        </tbody></table>
      </div>
      <!-- Fallback -->
      <p v-else class="br-p">{{ block.content }}</p>
    </template>
    <div v-if="!blocks || blocks.length===0" class="br-empty">（无内容）</div>
  </div>
</template>

<script setup lang="ts">
import { sanitizeHtml } from '@/utils/sanitize'

defineProps<{ blocks: {id:number,type:string,content:string}[] }>()

function stripInline(text: string): string {
  return text.replace(/<[^>]+>/g, '')
}

function renderInline(text: string): string {
  const escaped = text
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/<strong>/g,'<strong>').replace(/<\/strong>/g,'</strong>')
    .replace(/<em>/g,'<em>').replace(/<\/em>/g,'</em>')
    .replace(/<del>/g,'<del>').replace(/<\/del>/g,'</del>')
    .replace(/<u>/g,'<u>').replace(/<\/u>/g,'</u>')
    .replace(/<code>/g,'<code>').replace(/<\/code>/g,'</code>')
  return sanitizeHtml(escaped)
}

function extractListItems(content: string): string[] {
  const items = content.match(/<li>.*?<\/li>/gs)
  return items ? items.map(i => i.replace(/<\/?li>/g,'')) : [content]
}

function parseTableRows(content: string): string[][] {
  const rows = content.match(/<tr>.*?<\/tr>/gs)
  if (!rows) return []
  return rows.map(r => {
    const cells = r.match(/<t[dh]>.*?<\/t[dh]>/gs)
    return cells ? cells.map(c => c.replace(/<\/?t[dh]>/g,'')) : [r]
  })
}
</script>

<style scoped>
.block-renderer { line-height:1.8; }
.br-title { font-size:20px; font-weight:700; margin:8px 0 16px; padding-bottom:8px; border-bottom:2px solid #303133; }
.br-h2 { font-size:16px; font-weight:700; margin:16px 0 8px; }
.br-h3 { font-size:15px; font-weight:700; margin:12px 0 6px; }
.br-h4 { font-size:14px; font-weight:600; margin:10px 0 4px; }
.br-p { margin:6px 0; text-indent:2em; }
.br-ul, .br-ol { margin:6px 0; padding-left:24px; }
.br-blockquote { margin:8px 0; padding:8px 16px; background:var(--el-fill-color-light); border-left:3px solid var(--el-color-primary); }
.br-hr { margin:12px 0; border:none; border-top:1px solid var(--el-border-color); }
.br-table-wrap { overflow-x:auto; margin:8px 0; }
.br-table { border-collapse:collapse; width:100%; font-size:13px; }
.br-table td { border:1px solid var(--el-border-color); padding:4px 8px; }
.br-empty { color:var(--el-text-color-placeholder); font-style:italic; text-align:center; padding:20px; }
</style>
