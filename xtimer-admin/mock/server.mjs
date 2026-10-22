import http from 'node:http'

const PORT = Number(process.env.MOCK_PORT || 8082)

const pad = (n) => String(n).padStart(2, '0')

function now() {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(
    d.getMinutes(),
  )}:${pad(d.getSeconds())}`
}

let timerSeq = 1001
let taskSeq = 5001
const timers = []
const tasks = []

function seedTimer(app, name, cron, status, url) {
  const timer = {
    timerId: timerSeq++,
    app,
    name,
    status,
    cron,
    notifyHTTPParam: {
      method: 'POST',
      url,
      header: { 'Content-Type': 'application/json' },
      body: '{"bizId":1}',
    },
    createTime: '2026-10-20 09:12:33',
    modifyTime: '2026-10-21 18:40:02',
  }
  timers.push(timer)
  return timer
}

function addTasks(timerId, app, count) {
  const base = Date.now() - count * 3600000
  for (let i = 0; i < count; i += 1) {
    const failed = i === count - 2
    const runTimer = base + i * 3600000
    tasks.push({
      taskId: taskSeq++,
      timerId,
      app,
      status: failed ? 3 : 2,
      runTimer,
      costTime: failed ? 0 : 12 + ((i * 7) % 40),
      output: failed
        ? 'ConnectException: Connection refused (127.0.0.1:8083)'
        : '<200 OK,{"code":0,"message":"ok"}>',
      createTime: now(),
      modifyTime: now(),
    })
  }
}

const t1 = seedTimer('seckill', '秒杀库存预热', '0 0 9 * * ?', 2, 'http://127.0.0.1:8083/seckill/preheat')
const t2 = seedTimer('seckill', '秒杀订单对账', '0 30 2 * * ?', 2, 'http://127.0.0.1:8083/seckill/reconcile')
const t3 = seedTimer('lottery', '抽奖池补货', '0 */10 * * * ?', 1, 'http://127.0.0.1:8084/lottery/refill')

addTasks(t1.timerId, t1.app, 6)
addTasks(t2.timerId, t2.app, 4)
addTasks(t3.timerId, t3.app, 3)

const ok = (data) => ({ code: 0, message: 'ok', datetime: now(), data })
const fail = (message) => ({ code: 1, message, datetime: now(), data: null })

function toMs(value) {
  if (!value) return null
  const ms = new Date(String(value).replace(' ', 'T')).getTime()
  return Number.isNaN(ms) ? null : ms
}

function paginate(list, params) {
  const page = Math.max(1, Number(params.get('page') || 1))
  const size = Math.max(1, Number(params.get('pageSize') || 10))
  const start = (page - 1) * size
  return { list: list.slice(start, start + size), total: list.length }
}

function readBody(req) {
  return new Promise((resolve) => {
    let raw = ''
    req.on('data', (chunk) => {
      raw += chunk
    })
    req.on('end', () => {
      try {
        resolve(raw ? JSON.parse(raw) : {})
      } catch {
        resolve({})
      }
    })
  })
}

function filterTimers(params) {
  const app = params.get('app')
  const status = params.get('status')
  const name = params.get('name')
  return timers
    .filter((t) => (app ? t.app === app : true))
    .filter((t) => (status ? t.status === Number(status) : true))
    .filter((t) => (name ? t.name.includes(name) : true))
    .sort((a, b) => b.timerId - a.timerId)
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`)
  const path = url.pathname.replace(/^\/xtimer/, '')
  const params = url.searchParams

  const send = (body, code = 200) => {
    res.writeHead(code, {
      'Content-Type': 'application/json; charset=utf-8',
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Headers': '*',
      'Access-Control-Allow-Methods': '*',
    })
    res.end(JSON.stringify(body))
  }

  if (req.method === 'OPTIONS') {
    res.writeHead(204, { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': '*' })
    res.end()
    return
  }

  if (req.method === 'POST' && path === '/createTimer') {
    const body = await readBody(req)
    if (!body.cron) return send(fail('invalid cron'))
    const timer = {
      timerId: timerSeq++,
      app: body.app,
      name: body.name,
      status: body.status ?? 0,
      cron: body.cron,
      notifyHTTPParam: body.notifyHTTPParam ?? { method: 'POST', url: '', header: {}, body: '' },
      createTime: now(),
      modifyTime: now(),
    }
    timers.push(timer)
    return send(ok(timer.timerId))
  }

  if (req.method === 'POST' && path === '/updateTimer') {
    const body = await readBody(req)
    const timer = timers.find((t) => t.timerId === Number(body.timerId))
    if (!timer) return send(fail('timer not found'))
    Object.assign(timer, {
      app: body.app ?? timer.app,
      name: body.name ?? timer.name,
      cron: body.cron ?? timer.cron,
      notifyHTTPParam: body.notifyHTTPParam ?? timer.notifyHTTPParam,
      modifyTime: now(),
    })
    return send(ok(null))
  }

  if (path === '/getAppTimers') {
    return send(ok(paginate(filterTimers(params), params)))
  }

  if (path === '/getTimer') {
    const timer = timers.find((t) => t.timerId === Number(params.get('timerId')))
    return send(timer ? ok(timer) : fail('timer not found'))
  }

  if (path === '/enableTimer') {
    const timer = timers.find((t) => t.timerId === Number(params.get('timerId')))
    if (!timer) return send(fail('timer not found'))
    timer.status = 2
    timer.modifyTime = now()
    return send(ok('ok'))
  }

  if (path === '/unEnableTimer') {
    const timer = timers.find((t) => t.timerId === Number(params.get('timerId')))
    if (!timer) return send(fail('timer not found'))
    timer.status = 1
    timer.modifyTime = now()
    return send(ok('ok'))
  }

  if (path === '/deleteTimer') {
    const id = Number(params.get('timerId'))
    const index = timers.findIndex((t) => t.timerId === id)
    if (index < 0) return send(fail('timer not found'))
    timers.splice(index, 1)
    for (let i = tasks.length - 1; i >= 0; i -= 1) {
      if (tasks[i].timerId === id) tasks.splice(i, 1)
    }
    return send(ok(null))
  }

  if (path === '/getTaskList') {
    const app = params.get('app')
    const timerId = params.get('timerId')
    const status = params.get('status')
    const start = toMs(params.get('startTime'))
    const end = toMs(params.get('endTime'))
    const list = tasks
      .filter((t) => (app ? t.app === app : true))
      .filter((t) => (timerId ? t.timerId === Number(timerId) : true))
      .filter((t) => (status ? t.status === Number(status) : true))
      .filter((t) => (start ? t.runTimer >= start : true))
      .filter((t) => (end ? t.runTimer <= end : true))
      .sort((a, b) => b.runTimer - a.runTimer)
    return send(ok(paginate(list, params)))
  }

  if (path === '/getTaskStats') {
    const app = params.get('app')
    const scoped = app ? tasks.filter((t) => t.app === app) : tasks
    const scopedTimers = app ? timers.filter((t) => t.app === app) : timers
    const succeeded = scoped.filter((t) => t.status === 2).length
    return send(
      ok({
        total: scopedTimers.length,
        enabled: scopedTimers.filter((t) => t.status === 2).length,
        disabled: scopedTimers.filter((t) => t.status !== 2).length,
        todayRun: scoped.length,
        successRate: scoped.length ? Number(((succeeded / scoped.length) * 100).toFixed(1)) : 0,
        recentFailed: scoped.filter((t) => t.status === 3).slice(0, 5),
      }),
    )
  }

  send(fail(`mock 未实现：${req.method} ${path}`), 404)
})

server.listen(PORT, () => {
  console.log(`[mock] xtimer stub listening on http://localhost:${PORT}/xtimer`)
})
