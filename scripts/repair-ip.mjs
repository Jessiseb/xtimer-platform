import fs from 'node:fs/promises'
import path from 'node:path'

const ROOT = 'D:/学习笔记/训练营/定时微服务/bitstorm-java-project-241010'
const BASE = 'C:/Users/O/AppData/Local/Temp/bsrestore/bitstorm-java-project-241010'
const SKIP_DIRS = new Set(['node_modules', '.git', 'target', 'dist', '.workbuddy', 'logs'])
const IP_RE = /\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}/g

async function walk(dir, out = []) {
  for (const entry of await fs.readdir(dir, { withFileTypes: true })) {
    if (SKIP_DIRS.has(entry.name)) continue
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) await walk(full, out)
    else if (entry.name === 'application.yml') out.push(full)
  }
  return out
}

const normalize = (line) => line.replace(IP_RE, 'IP')

let fixed = 0
const files = await walk(ROOT)

for (const file of files) {
  const rel = path.relative(ROOT, file).split(path.sep).join('/')
  let baseText
  try {
    baseText = await fs.readFile(path.join(BASE, rel), 'utf8')
  } catch {
    continue
  }
  const text = await fs.readFile(file, 'utf8')
  if (!text.includes('127.0.0.1')) continue

  const baseLines = baseText.split(/\r?\n/)
  const used = new Set()
  let changed = 0

  const lines = text.split(/\r?\n/).map((line) => {
    if (!line.includes('127.0.0.1')) return line
    const target = normalize(line)
    const idx = baseLines.findIndex(
      (b, i) => !used.has(i) && b.includes('192.168.') && normalize(b) === target,
    )
    if (idx < 0) return line
    used.add(idx)
    if (baseLines[idx] === line) return line
    changed += 1
    return baseLines[idx]
  })

  if (changed > 0) {
    await fs.writeFile(file, lines.join('\n'), 'utf8')
    fixed += changed
    console.log(`恢复 ${changed} 行  ${rel}`)
  }
}

console.log(`\n共恢复 ${fixed} 行 IP`)
