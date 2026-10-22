import dayjs from 'dayjs'

export function formatDateTime(value?: string | number | null): string {
  if (value === undefined || value === null || value === '') return '-'
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}

export function formatRunTimer(ms?: number): string {
  if (!ms) return '-'
  return dayjs(ms).format('YYYY-MM-DD HH:mm:ss')
}

export function formatCostTime(ms?: number): string {
  if (ms === undefined || ms === null) return '-'
  if (ms < 1000) return `${ms} ms`
  return `${(ms / 1000).toFixed(2)} s`
}
