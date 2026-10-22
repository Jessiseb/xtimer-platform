<template>
  <el-drawer v-model="visible" :title="isEdit ? '编辑定时器' : '新建定时器'" size="640px" @closed="onClosed">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="112px">
      <el-form-item label="应用标识" prop="app">
        <el-input v-model="form.app" :disabled="isEdit" placeholder="所属业务方，如 seckill" />
      </el-form-item>
      <el-form-item label="任务名称" prop="name">
        <el-input v-model="form.name" placeholder="便于识别的名称" />
      </el-form-item>
      <el-form-item label="Cron 表达式" prop="cron" required>
        <cron-helper v-model="form.cron" />
      </el-form-item>
      <el-divider content-position="left">回调配置</el-divider>
      <notify-param-form v-model="form.notifyHTTPParam" />
    </el-form>
    <template #footer>
      <div style="flex: auto">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import CronHelper from '@/components/CronHelper.vue'
import NotifyParamForm from '@/components/NotifyParamForm.vue'
import { createTimer, updateTimer } from '@/api/timer'
import { isValidCron } from '@/utils/cron'
import type { NotifyHTTPParam, TimerDTO } from '@/types/timer'

const props = defineProps<{
  modelValue: boolean
  timer: TimerDTO | null
  defaultApp?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

const isEdit = computed(() => !!props.timer)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const editingId = ref<number | null>(null)
const originalStatus = ref(0)

interface TimerFormState {
  app: string
  name: string
  cron: string
  notifyHTTPParam: NotifyHTTPParam
}

function defaultParam(): NotifyHTTPParam {
  return { method: 'POST', url: '', header: {}, body: '' }
}

function emptyForm(): TimerFormState {
  return { app: '', name: '', cron: '', notifyHTTPParam: defaultParam() }
}

const form = reactive<TimerFormState>(emptyForm())

const rules = {
  app: [
    { required: true, message: '请输入应用标识', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: '仅支持字母、数字、- 和 _', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  cron: [
    { required: true, message: '请输入 Cron 表达式', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (!value || isValidCron(value)) callback()
        else callback(new Error('Cron 表达式不合法'))
      },
      trigger: 'blur',
    },
  ],
}

function initForm() {
  if (props.timer) {
    editingId.value = props.timer.timerId ?? null
    originalStatus.value = props.timer.status ?? 0
    Object.assign(form, {
      app: props.timer.app,
      name: props.timer.name,
      cron: props.timer.cron,
      notifyHTTPParam: {
        method: props.timer.notifyHTTPParam?.method || 'POST',
        url: props.timer.notifyHTTPParam?.url || '',
        header: { ...(props.timer.notifyHTTPParam?.header ?? {}) },
        body: props.timer.notifyHTTPParam?.body || '',
      },
    })
  } else {
    editingId.value = null
    originalStatus.value = 0
    Object.assign(form, emptyForm())
    form.app = props.defaultApp || ''
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) initForm()
  },
)

function onClosed() {
  formRef.value?.clearValidate()
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  if (!/^https?:\/\//.test(form.notifyHTTPParam.url)) {
    ElMessage.error('回调地址必须以 http:// 或 https:// 开头')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value && editingId.value) {
      await updateTimer({
        timerId: editingId.value,
        app: form.app,
        name: form.name,
        status: originalStatus.value,
        cron: form.cron,
        notifyHTTPParam: form.notifyHTTPParam,
      })
    } else {
      await createTimer({
        app: form.app,
        name: form.name,
        status: 0,
        cron: form.cron,
        notifyHTTPParam: form.notifyHTTPParam,
      })
    }
    ElMessage.success('保存成功')
    visible.value = false
    emit('saved')
  } catch {
    // 错误提示已由请求层统一处理
  } finally {
    submitting.value = false
  }
}
</script>
