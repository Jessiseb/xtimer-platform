import fs from 'node:fs/promises'
import path from 'node:path'

const ROOT = 'D:/学习笔记/训练营/定时微服务/bitstorm-java-project-241010'
const BASE = 'C:/Users/O/AppData/Local/Temp/bsrestore/bitstorm-java-project-241010'
const SKIP_DIRS = new Set(['node_modules', '.git', 'target', 'dist', '.workbuddy', 'logs'])
const LINE_RE = /^(\s*)([-\w.]+)\s*:\s*(.*)$/

async function walk(dir, out = []) {
  for (const entry of await fs.readdir(dir, { withFileTypes: true })) {
    if (SKIP_DIRS.has(entry.name)) continue
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) await walk(full, out)
    else if (entry.name === 'application.yml') out.push(full)
  }
  return out
}

function indexByKey(lines) {
  const map = new Map()
  for (const line of lines) {
    const m = line.match(LINE_RE)
    if (!m) continue
    const key = m[2]
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(m[3].trim())
  }
  return map
}

const files = await walk(ROOT)
let patched = 0
const unresolved = []

for (const file of files) {
  const rel = path.relative(ROOT, file).split(path.sep).join('/')
  const text = await fs.readFile(file, 'utf8')
  if (!text.includes('change_me') && !text.includes('127.0.0.1')) continue

  const baseFile = path.join(BASE, rel)
  let baseText
  try {
    baseText = await fs.readFile(baseFile, 'utf8')
  } catch {
    unresolved.push(`${rel}（基线包中无此文件）`)
    continue
  }

  const baseMap = indexByKey(baseText.split(/\r?\n/))
  const pointer = new Map()
  let changed = 0

  const lines = text.split(/\r?\n/).map((line) => {
    const m = line.match(LINE_RE)
    if (!m) return line
    const [full, indent, key, value] = m
    const trimmed = value.trim()
    const idx = pointer.get(key) ?? 0
    pointer.set(key, idx + 1)

    const candidates = baseMap.get(key)
    const baseValue = candidates?.[idx]

    const isMasked = trimmed === 'change_me'
    const isIpMasked =
      /^127\.0\.0\.1(?::\d+)?$/.test(trimmed) && !!baseValue && /^192\.168\./.test(baseValue)
    if (!isMasked && !isIpMasked) return line
    if (baseValue === undefined) {
      unresolved.push(`${rel} -> ${key}（第 ${idx + 1} 处无基线值）`)
      return line
    }
    const next = `${indent}${key}: ${baseValue}`
    if (next !== full) changed += 1
    return next
  })

  if (changed > 0) {
    await fs.writeFile(file, lines.join('\n'), 'utf8')
    patched += changed
    console.log(`修复 ${changed} 行  ${rel}`)
  }
}

console.log(`\n共修复 ${patched} 行`)
for (const item of unresolved) console.log(`未修复: ${item}`)
