import fs from 'node:fs/promises'
import path from 'node:path'

const ROOT = 'D:/学习笔记/训练营/定时微服务/bitstorm-java-project-241010'
const SKIP_DIRS = new Set(['node_modules', '.git', 'target', 'dist', '.workbuddy', 'logs'])
const CONFIG_RE = /^application(-[\w-]+)?\.ya?ml$/
const SENSITIVE_RE =
  /^(\s*)([\w.-]*(?:password|secret|token|access-?key|secret-?key|private-?key)[\w.-]*)(\s*:\s*)(.+)$/i

async function walk(dir, out = []) {
  for (const entry of await fs.readdir(dir, { withFileTypes: true })) {
    if (SKIP_DIRS.has(entry.name)) continue
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) await walk(full, out)
    else if (CONFIG_RE.test(entry.name) && !entry.name.includes('-example')) out.push(full)
  }
  return out
}

function mask(text) {
  return text
    .split(/\r?\n/)
    .map((line) =>
      line
        .replace(SENSITIVE_RE, (_m, indent, key, sep) => `${indent}${key}${sep}change_me`)
        .replace(/\b192\.168\.\d+\.\d+\b/g, '127.0.0.1'),
    )
    .join('\n')
}

const files = await walk(ROOT)
let count = 0

for (const file of files) {
  const base = path.basename(file)
  const suffix = base.match(CONFIG_RE)?.[1] ?? ''
  const target = path.join(path.dirname(file), `application${suffix}-example.yml`)

  if (!target.endsWith('-example.yml')) {
    throw new Error(`拒绝写入非示例文件: ${target}`)
  }

  const raw = await fs.readFile(file, 'utf8')
  await fs.writeFile(target, mask(raw), 'utf8')
  count += 1
  console.log('example:', path.relative(ROOT, target).split(path.sep).join('/'))
}

console.log(`total ${count}`)
