<template>
  <div class="master-data-section">
    <el-divider content-position="left">
      <span class="section-title">{{ section.label }}</span>
      <el-tag v-if="section.description" size="small" type="info" style="margin-left:8px">{{ section.description }}</el-tag>
    </el-divider>

    <el-form-item
      v-for="field in section.fields"
      :key="field.name"
      :label="field.label"
      :prop="`${prefix}.${field.name}`"
      :required="field.required"
    >
      <!-- Text input -->
      <el-input
        v-if="field.type === 'text' || field.type === 'textarea'"
        v-model="model[field.name]"
        :type="field.type === 'textarea' ? 'textarea' : 'text'"
        :placeholder="field.placeholder || `请输入${field.label}`"
        :rows="field.type === 'textarea' ? 3 : 1"
      />

      <!-- Select -->
      <el-select
        v-else-if="field.type === 'select'"
        v-model="model[field.name]"
        :placeholder="field.placeholder || `请选择${field.label}`"
        style="width:100%"
      >
        <el-option
          v-for="opt in (field.options || [])"
          :key="typeof opt === 'string' ? opt : (opt as any).value"
          :label="typeof opt === 'string' ? opt : (opt as any).label"
          :value="typeof opt === 'string' ? opt : (opt as any).value"
        />
      </el-select>

      <!-- Date -->
      <el-date-picker
        v-else-if="field.type === 'date'"
        v-model="model[field.name]"
        type="date"
        style="width:100%"
        :placeholder="field.placeholder || '选择日期'"
        value-format="YYYY-MM-DD"
      />

      <!-- Number -->
      <el-input-number
        v-else-if="field.type === 'number'"
        v-model="model[field.name]"
        style="width:100%"
        :placeholder="field.placeholder"
      />

      <!-- Boolean switch -->
      <el-switch
        v-else-if="field.type === 'boolean'"
        v-model="model[field.name]"
      />

      <span v-if="field.tip" class="field-tip">{{ field.tip }}</span>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  section: {
    key: string
    label: string
    description?: string
    fields: Array<{
      name: string
      label: string
      type: string
      required?: boolean
      placeholder?: string
      options?: Array<string | { value: string; label: string }>
      tip?: string
    }>
  }
  model: Record<string, any>
  prefix: string
}>()
</script>

<style scoped>
.master-data-section { margin-bottom: 8px; }
.section-title { font-weight: 600; font-size: 14px; }
.field-tip { font-size: 11px; color: #909399; margin-top: 4px; display: block; }
</style>
