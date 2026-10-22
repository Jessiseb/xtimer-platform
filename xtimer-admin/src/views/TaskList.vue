<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input
        v-model="query.app"
        placeholder="应用标识 app"
        clearable
        style="width: 180px"
        @keyup.enter="handleSearch"
      />
      <el-input-number
        v-model="query.timerId"
        placeholder="定时器 ID"
        :controls="false"
        style="width: 140px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.status" placeholder="执行状态" clearable style="width: 140px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-date-picker
        v-model="query.range"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="YYYY-MM-DD HH:mm:ss"
        style="width: 360px"
      />
      <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      <el-button :icon="Refresh" @click="handleReset">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="taskId" label="任务 ID" width="100" />
      <el-table-column prop="timerId" label="定时器 ID" width="110" />
      <el-table-column prop="app" label="应用" width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <status-tag kind="task" :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="计划执行时间" width="180">
        <template #default="{ row }">{{ formatRunTimer(row.runTimer) }}</template>
      </el-table-column>
      <el-table-column label="耗时" width="110">
        <template #default="{ row }">{{ formatCostTime(row.costTime) }}</template>
      </el-table-column>
      <el-table-column label="输出" min-width="220">
        <template #default="{ row }">
          <span v-if="row.output" class="output-cell" @click="openOutput(row)">{{ row.output }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <template #empty>
        <span class="empty-tip">暂无执行记录</span>
      </template>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pager"
      @size-change="fetchList"
      @current-change="fetchList"
    />

    <el-dialog v-model="outputVisible" title="执行输出" width="640px">
      <pre class="output-full">{{ outputText }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getTaskList } from '@/api/task'
import { TASK_STATUS_OPTIONS } from '@/types/task'
import type { TaskDTO } from '@/types/task'
import { formatCostTime, formatDateTime, formatRunTimer } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

const statusOptions = TASK_STATUS_OPTIONS
const loading = ref(false)
const list = ref<TaskDTO[]>([])
const total = ref(0)
const query = reactive({
  app: '',
  timerId: undefined as number | undefined,
  status: undefined as number | undefined,
  range: [] as string[],
  page: 1,
  pageSize: 10,
})
const outputVisible = ref(false)
const outputText = ref('')

async function fetchList() {
  loading.value = true
  try {
    const res = await getTaskList({
      app: query.app || undefined,
      timerId: query.timerId ?? undefined,
      status: query.status,
      startTime: query.range?.[0],
      endTime: query.range?.[1],
      page: query.page,
      pageSize: query.pageSize,
    })
    list.value = res?.list ?? []
    total.value = res?.total ?? 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  fetchList()
}

function handleReset() {
  query.app = ''
  query.timerId = undefined
  query.status = undefined
  query.range = []
  query.page = 1
  fetchList()
}

function openOutput(row: TaskDTO) {
  outputText.value = row.output
  outputVisible.value = true
}

onMounted(fetchList)
</script>
