<template>
  <el-tag :type="meta.type" size="small">{{ meta.label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { TIMER_STATUS } from '@/types/timer'
import { TASK_STATUS } from '@/types/task'

const props = defineProps<{ kind: 'timer' | 'task'; status: number }>()

const meta = computed(() => {
  const dict = props.kind === 'timer' ? TIMER_STATUS : TASK_STATUS
  return dict[props.status] ?? { label: `未知(${props.status})`, type: 'info' as const }
})
</script>
