import { request } from '@/utils/request'
import type { PageResult } from '@/types/response'
import type { TimerDTO } from '@/types/timer'

export interface TimerQuery {
  app: string
  status?: number
  name?: string
  page: number
  pageSize: number
}

export function getAppTimers(params: TimerQuery) {
  return request<PageResult<TimerDTO>>({ url: '/getAppTimers', method: 'get', params })
}

export function getTimer(app: string, timerId: number) {
  return request<TimerDTO>({ url: '/getTimer', method: 'get', params: { app, timerId } })
}

export function createTimer(data: TimerDTO) {
  return request<number>({ url: '/createTimer', method: 'post', data })
}

export function updateTimer(data: TimerDTO) {
  return request<void>({ url: '/updateTimer', method: 'post', data })
}

export function enableTimer(app: string, timerId: number) {
  return request<string>({ url: '/enableTimer', method: 'get', params: { app, timerId } })
}

export function unEnableTimer(app: string, timerId: number) {
  return request<string>({ url: '/unEnableTimer', method: 'get', params: { app, timerId } })
}

export function deleteTimer(app: string, timerId: number) {
  return request<void>({ url: '/deleteTimer', method: 'get', params: { app, timerId } })
}
