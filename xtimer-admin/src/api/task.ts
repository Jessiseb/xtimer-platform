import { request } from '@/utils/request'
import type { PageResult } from '@/types/response'
import type { TaskDTO } from '@/types/task'

export interface TaskQuery {
  app?: string
  timerId?: number
  status?: number
  startTime?: string
  endTime?: string
  page: number
  pageSize: number
}

export function getTaskList(params: TaskQuery) {
  return request<PageResult<TaskDTO>>({ url: '/getTaskList', method: 'get', params })
}
