import dayjs from 'dayjs'
import { parseExpression } from 'cron-parser'

function quartzToStandard(expr: string): string {
  const fields = expr.trim().split(/\s+/)
  if (fields.length < 6) return expr
  const [sec, min, hour, dom, mon, dow] = fields.slice(0, 6)
  return [sec, min, hour, dom === '?' ? '*' : dom, mon, dow === '?' ? '*' : dow].join(' ')
}

function toDate(value: Date | { toDate(): Date }): Date {
  return value instanceof Date ? value : value.toDate()
}

export function nextRuns(expr: string, count = 3): string[] {
  try {
    const iterator = parseExpression(quartzToStandard(expr), { currentDate: new Date() })
    const runs: string[] = []
    for (let i = 0; i < count; i += 1) {
      runs.push(dayjs(toDate(iterator.next())).format('YYYY-MM-DD HH:mm:ss'))
    }
    return runs
  } catch {
    return []
  }
}

export function isValidCron(expr: string): boolean {
  return nextRuns(expr, 1).length > 0
}
