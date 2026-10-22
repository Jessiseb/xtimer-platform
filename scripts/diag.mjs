import { spawn } from 'node:child_process'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'

const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const PORT = 9334
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

const profile = await fs.mkdtemp(path.join(os.tmpdir(), 'diag-'))
const chrome = spawn(CHROME, [
  '--headless=new',
  `--remote-debugging-port=${PORT}`,
  `--user-data-dir=${profile}`,
  '--no-proxy-server',
  '--disable-gpu',
  '--no-first-run',
  'about:blank',
], { stdio: 'ignore' })

let version = null
for (let i = 0; i < 60; i += 1) {
  try {
    const res = await fetch(`http://127.0.0.1:${PORT}/json/version`)
    version = await res.json()
    break
  } catch {
    await sleep(200)
  }
}
console.log('chrome:', version ? version.Browser : 'not reachable')

const ws = new WebSocket(version.webSocketDebuggerUrl)
await new Promise((r) => ws.addEventListener('open', r, { once: true }))

let seq = 0
const pending = new Map()
const events = []
ws.addEventListener('message', (ev) => {
  const msg = JSON.parse(ev.data)
  if (msg.id && pending.has(msg.id)) {
    const { resolve, reject } = pending.get(msg.id)
    pending.delete(msg.id)
    if (msg.error) reject(new Error(msg.error.message))
    else resolve(msg.result)
  } else if (msg.method === 'Runtime.exceptionThrown' || msg.method === 'Runtime.consoleAPICalled' || msg.method === 'Network.loadingFailed') {
    events.push(msg)
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
await send('Runtime.enable', {}, sessionId)
await send('Network.enable', {}, sessionId)

const nav = await send('Page.navigate', { url: 'http://localhost:5173/timers' }, sessionId)
console.log('navigate result:', JSON.stringify(nav))
await sleep(4000)

const js = async (expression) => {
  const { result } = await send('Runtime.evaluate', { expression, returnByValue: true }, sessionId)
  return result.value
}

console.log('href:', await js('location.href'))
console.log('title:', await js('document.title'))
console.log('rows:', await js(`document.querySelectorAll('.el-table__row').length`))
console.log('body:', String(await js('document.body.innerText')).slice(0, 400).replace(/\n/g, ' | '))
console.log('events:', events.length)
for (const e of events.slice(0, 8)) {
  console.log(' -', e.method, JSON.stringify(e.params).slice(0, 200))
}

ws.close()
chrome.kill()
await fs.rm(profile, { recursive: true, force: true })
