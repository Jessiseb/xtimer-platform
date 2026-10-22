export interface TaskDTO {
  taskId: number
  timerId: number
  app: string
  status: number
  runTimer: number
  costTime: number
  output: string
  createTime?: string
  modifyTime?: string
}

export const TASK_STATUS: Record<
  number,
  { label: string; type: 'success' | 'warning' | 'danger' | 'info' }
> = {
  0: { label: '未运行', type: 'info' },
  1: { label: '运行中', type: 'warning' },
  2: { label: '成功', type: 'success' },
  3: { label: '失败', type: 'danger' },
}

export const TASK_STATUS_OPTIONS = [
  { value: 0, label: '未运行' },
  { value: 1, label: '运行中' },
  { value: 2, label: '成功' },
  { value: 3, label: '失败' },
]
