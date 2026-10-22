<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input
        v-model="query.app"
        placeholder="应用标识 app"
        clearable
        style="width: 200px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-input
        v-model="query.name"
        placeholder="任务名称"
        clearable
        style="width: 200px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      <div class="spacer" />
      <el-button type="primary" :icon="Plus" @click="openCreate">新建定时器</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="timerId" label="ID" width="90" />
      <el-table-column prop="app" label="应用" width="150" show-overflow-tooltip />
      <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="Cron" min-width="150">
        <template #default="{ row }">
          <span class="mono">{{ row.cron }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <status-tag kind="timer" :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status !== 2" link type="success" @click="handleEnable(row)">启用</el-button>
          <el-button v-else link type="warning" @click="handleUnEnable(row)">暂停</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <span class="empty-tip">{{ query.app ? '暂无数据' : '请输入应用标识 app 查询，或直接新建定时器' }}</span>
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

    <timer-form-drawer v-model="drawerVisible" :timer="editingTimer" :default-app="query.app" @saved="fetchList" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { deleteTimer, enableTimer, getAppTimers, unEnableTimer } from '@/api/timer'
import { TIMER_STATUS_OPTIONS } from '@/types/timer'
import type { TimerDTO } from '@/types/timer'
import { formatDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import TimerFormDrawer from '@/views/TimerFormDrawer.vue'

const statusOptions = TIMER_STATUS_OPTIONS
const loading = ref(false)
const list = ref<TimerDTO[]>([])
const total = ref(0)
const query = reactive({
  app: '',
  status: undefined as number | undefined,
  name: '',
  page: 1,
  pageSize: 10,
})
const drawerVisible = ref(false)
const editingTimer = ref<TimerDTO | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const res = await getAppTimers({
      app: query.app,
      status: query.status,
      name: query.name,
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
  query.status = undefined
  query.name = ''
  query.page = 1
  fetchList()
}

function openCreate() {
  editingTimer.value = null
  drawerVisible.value = true
}

function openEdit(row: TimerDTO) {
  editingTimer.value = row
  drawerVisible.value = true
}

async function confirmAction(message: string, title = '提示'): Promise<boolean> {
  try {
    await ElMessageBox.confirm(message, title, { type: 'warning' })
    return true
  } catch {
    return false
  }
}

async function handleEnable(row: TimerDTO) {
  if (!(await confirmAction(`确定启用定时器「${row.name}」吗？`))) return
  await enableTimer(row.app, row.timerId as number)
  ElMessage.success('已启用')
  fetchList()
}

async function handleUnEnable(row: TimerDTO) {
  if (!(await confirmAction(`确定暂停定时器「${row.name}」吗？暂停后不再触发回调。`))) return
  await unEnableTimer(row.app, row.timerId as number)
  ElMessage.success('已暂停')
  fetchList()
}

async function handleDelete(row: TimerDTO) {
  if (!(await confirmAction(`确定删除定时器「${row.name}」吗？删除后不可恢复。`, '危险操作'))) return
  await deleteTimer(row.app, row.timerId as number)
  ElMessage.success('已删除')
  fetchList()
}

onMounted(fetchList)
</script>
