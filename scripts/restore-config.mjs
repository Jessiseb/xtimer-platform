import fs from 'node:fs/promises'
import path from 'node:path'

const ROOT = 'D:/学习笔记/训练营/定时微服务/bitstorm-java-project-241010'
const SIBLING = 'D:/学习笔记/训练营/Message/bitstorm-java-project-main 250704'
const SKIP_DIRS = new Set(['node_modules', '.git', 'target', 'dist', '.workbuddy', 'logs'])
const SENSITIVE_RE =
  /^(\s*)([\w.-]*(?:password|secret|token|access-?key|secret-?key|private-?key)[\w.-]*)(\s*:\s*)(.+)$/i

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

async function findConfigs(dir, out = []) {
  let entries = []
  try {
    entries = await fs.readdir(dir, { withFileTypes: true })
  } catch {
    return out
  }
  for (const entry of entries) {
    if (SKIP_DIRS.has(entry.name)) continue
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      await findConfigs(full, out)
    } else if (entry.name === 'application.yml') {
      out.push(full)
    }
  }
  return out
}

const current = await findConfigs(ROOT)
let restored = 0
const failed = []

for (const file of current) {
  const text = await fs.readFile(file, 'utf8')
  if (!text.includes('change_me')) continue

  let rel = path.relative(ROOT, file).replace(/\\/g, '/')
  let siblingFile = path.join(SIBLING, rel)

  let siblingText = null
  try {
    siblingText = await fs.readFile(siblingFile, 'utf8')
  } catch {
    // 目录名可能不同（如 asyncflow/flowsvr），退化为按模块名+文件名搜索
    const moduleName = rel.split('/')[0]
    const candidates = await findConfigs(SIBLING)
    const hit = candidates.find((f) => f.replace(/\\/g, '/').includes(moduleName))
    if (hit) {
      siblingFile = hit
      siblingText = await fs.readFile(hit, 'utf8')
    }
  }

  if (!siblingText) {
    failed.push(`${rel} -> 副本中未找到对应文件`)
    continue
  }

  if (mask(siblingText) === text) {
    await fs.writeFile(file, siblingText, 'utf8')
    restored += 1
    console.log(`OK   ${rel}`)
  } else {
    failed.push(`${rel} -> 与副本内容结构不一致，未覆盖`)
  }
}

console.log(`\n已还原 ${restored} 个`)
for (const item of failed) console.log(`待人工处理: ${item}`)
