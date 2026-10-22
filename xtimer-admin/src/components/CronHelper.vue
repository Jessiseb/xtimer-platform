<template>
  <div class="cron-helper">
    <div class="cron-row">
      <el-select v-model="preset" placeholder="常用表达式" clearable style="width: 200px" @change="onPresetChange">
        <el-option v-for="p in presets" :key="p.value" :label="p.label" :value="p.value" />
      </el-select>
      <el-input v-model="cron" placeholder="Quartz 表达式，如 0 0 0 * * ?" class="cron-input">
        <template #append>{{ validText }}</template>
      </el-input>
    </div>
    <div class="cron-next">
      <template v-if="runs.length">接下来执行：{{ runs.join(' / ') }}</template>
      <template v-else-if="cron">表达式无法解析，请检查格式</template>
      <template v-else>支持 6 位（秒 分 时 日 月 周）或 7 位（含年），日/周其一可用 ? 占位</template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { isValidCron, nextRuns } from '@/utils/cron'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const presets = [
  { label: '每分钟', value: '0 * * * * ?' },
  { label: '每 5 分钟', value: '0 */5 * * * ?' },
  { label: '每小时', value: '0 0 * * * ?' },
  { label: '每天 00:00', value: '0 0 0 * * ?' },
  { label: '每天 12:00', value: '0 0 12 * * ?' },
  { label: '每周一 09:00', value: '0 0 9 ? * 1' },
  { label: '每月 1 日 00:00', value: '0 0 0 1 * ?' },
  { label: '每年 1 月 1 日 00:00', value: '0 0 0 1 1 ?' },
]

const preset = ref('')

const cron = computed({
  get: () => props.modelValue,
  set: (v: string) => emit('update:modelValue', v),
})

const valid = computed(() => (cron.value ? isValidCron(cron.value) : false))
const runs = computed(() => (cron.value ? nextRuns(cron.value) : []))
const validText = computed(() => {
  if (!cron.value) return '待输入'
  return valid.value ? '合法' : '非法'
})

function onPresetChange(value: string) {
  if (value) cron.value = value
}
</script>
