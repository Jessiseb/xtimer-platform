export interface NotifyHTTPParam {
  method: string
  url: string
  header: Record<string, string>
  body: string
}

export interface TimerDTO {
  timerId?: number
  app: string
  name: string
  status: number
  cron: string
  notifyHTTPParam: NotifyHTTPParam
  createTime?: string
  modifyTime?: string
}

export const TIMER_STATUS: Record<number, { label: string; type: 'success' | 'info' }> = {
  1: { label: '未激活', type: 'info' },
  2: { label: '运行中', type: 'success' },
}

export const TIMER_STATUS_OPTIONS = [
  { value: 1, label: '未激活' },
  { value: 2, label: '运行中' },
]

export const HTTP_METHODS = ['POST', 'GET', 'PUT', 'DELETE']
