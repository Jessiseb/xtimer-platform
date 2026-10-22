import { spawn } from 'node:child_process'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'

const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const OUT_DIR = 'D:/学习笔记/训练营/定时微服务/bitstorm-java-project-241010/docs/images'
const PORT = 9333
const BASE = 'http://localhost:5173'

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function withTimeout(fn, timeout = 20000, gap = 200) {
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    const value = await fn()
    if (value) return value
    await sleep(gap)
  }
  throw new Error('timeout')
}

const profile = await fs.mkdtemp(path.join(os.tmpdir(), 'shot-'))
const chrome = spawn(
  CHROME,
  [
    '--headless=new',
    `--remote-debugging-port=${PORT}`,
    `--user-data-dir=${profile}`,
    '--disable-gpu',
    '--no-proxy-server',
    '--no-first-run',
    '--no-default-browser-check',
    '--hide-scrollbars',
    '--force-device-scale-factor=2',
    'about:blank',
  ],
  { stdio: 'ignore' },
)

const version = await withTimeout(async () => {
  try {
    const res = await fetch(`http://127.0.0.1:${PORT}/json/version`)
    return await res.json()
  } catch {
    return null
  }
})

const ws = new WebSocket(version.webSocketDebuggerUrl)
await new Promise((resolve, reject) => {
  ws.addEventListener('open', resolve, { once: true })
  ws.addEventListener('error', reject, { once: true })
})

let seq = 0
const pending = new Map()
ws.addEventListener('message', (event) => {
  const msg = JSON.parse(event.data)
  if (msg.id && pending.has(msg.id)) {
    const { resolve, reject } = pending.get(msg.id)
    pending.delete(msg.id)
    if (msg.error) reject(new Error(msg.error.message))
    else resolve(msg.result)
  }
})

function send(method, params = {}, sessionId) {
  const id = ++seq
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject })
    ws.send(JSON.stringify({ id, method, params, sessionId }))
  })
}

const { targetId } = await send('Target.createTarget', { url: 'about:blank' })
const { sessionId } = await send('Target.attachToTarget', { targetId, flatten: true })

await send('Page.enable', {}, sessionId)
await send('Emulation.setDeviceMetricsOverride',
  { width: 1440, height: 900, deviceScaleFactor: 2, mobile: false }, sessionId)

async function evaluate(expression) {
  const { result } = await send('Runtime.evaluate', { expression, returnByValue: true }, sessionId)
  return result.value
}

async function goto(url) {
  await send('Page.navigate', { url }, sessionId)
  await sleep(1000)
}

async function waitRows(min = 1) {
  return withTimeout(async () => {
    const count = await evaluate(`document.querySelectorAll('.el-table__row').length`)
    return count >= min ? count : 0
  }, 10000, 250)
}

async function shot(name) {
  const { data } = await send('Page.captureScreenshot', { format: 'png' }, sessionId)
  await fs.writeFile(path.join(OUT_DIR, name), Buffer.from(data, 'base64'))
  console.log(`saved ${name}`)
}

await goto(`${BASE}/timers`)
await waitRows(1)
await sleep(400)
await shot('01-timers.png')

await evaluate(`[...document.querySelectorAll('button')].find(b => b.textContent.includes('新建定时器'))?.click()`)
await sleep(1000)
await shot('02-timer-create.png')

await evaluate(`document.querySelector('.el-drawer__close-btn')?.click()`)
await sleep(600)

await goto(`${BASE}/tasks`)
await waitRows(1)
await sleep(400)
await shot('03-tasks.png')

await goto(`${BASE}/tasks`)
await sleep(300)
await evaluate(`document.querySelector('.output-cell')?.click()`)
await sleep(800)
await shot('04-task-output.png')

ws.close()
chrome.kill()
try {
  await fs.rm(profile, { recursive: true, force: true })
} catch {
  // 临时目录可能被 Chrome 残留句柄占用，忽略
}
console.log('done')
