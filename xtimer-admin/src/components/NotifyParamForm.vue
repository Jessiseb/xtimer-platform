<template>
  <div class="notify-form">
    <el-form-item label="回调方法">
      <el-radio-group v-model="form.method" @change="emitChange">
        <el-radio-button v-for="m in methods" :key="m" :value="m">{{ m }}</el-radio-button>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="回调地址">
      <el-input v-model="form.url" placeholder="到点后由 Executor 回调的 HTTP 地址" @input="emitChange" />
    </el-form-item>
    <el-form-item label="请求头">
      <div v-for="(row, i) in headerRows" :key="i" class="kv-row">
        <el-input v-model="row.key" placeholder="名称，如 Content-Type" @input="emitChange" />
        <el-input v-model="row.value" placeholder="值" @input="emitChange" />
        <el-button :icon="Delete" link type="danger" @click="removeHeader(i)" />
      </div>
      <el-button :icon="Plus" link type="primary" @click="addHeader">添加请求头</el-button>
    </el-form-item>
    <el-form-item v-if="hasBody" label="请求体">
      <el-input
        v-model="form.body"
        type="textarea"
        :rows="4"
        placeholder='JSON 字符串，如 {"bizId":123}'
        @input="emitChange"
        @blur="checkBody"
      />
      <div v-if="bodyError" class="body-error">{{ bodyError }}</div>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'
import { HTTP_METHODS } from '@/types/timer'
import type { NotifyHTTPParam } from '@/types/timer'

const props = defineProps<{ modelValue: NotifyHTTPParam }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: NotifyHTTPParam): void }>()

const methods = HTTP_METHODS
const form = reactive<{ method: string; url: string; body: string }>({
  method: 'POST',
  url: '',
  body: '',
})
const headerRows = ref<{ key: string; value: string }[]>([])
const bodyError = ref('')

const hasBody = computed(() => form.method !== 'GET' && form.method !== 'DELETE')

function emitChange() {
  const header: Record<string, string> = {}
  for (const row of headerRows.value) {
    if (row.key) header[row.key] = row.value
  }
  emit('update:modelValue', { method: form.method, url: form.url, header, body: form.body })
}

function checkBody() {
  bodyError.value = ''
  if (form.body && hasBody.value) {
    try {
      JSON.parse(form.body)
    } catch {
      bodyError.value = '不是合法的 JSON'
    }
  }
}

function addHeader() {
  headerRows.value.push({ key: '', value: '' })
}

function removeHeader(i: number) {
  headerRows.value.splice(i, 1)
  emitChange()
}

watch(
  () => props.modelValue,
  (v) => {
    form.method = v.method || 'POST'
    form.url = v.url || ''
    form.body = v.body || ''
    const rows = Object.entries(v.header ?? {}).map(([key, value]) => ({ key, value }))
    headerRows.value = rows.length ? rows : [{ key: '', value: '' }]
    bodyError.value = ''
  },
  { immediate: true },
)
</script>
