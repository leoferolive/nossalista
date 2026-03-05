import { readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'

const assetsDir = join(process.cwd(), 'dist', 'assets')
const maxMainChunkBytes = 550 * 1024

const files = readdirSync(assetsDir).filter((file) => file.endsWith('.js'))

if (files.length === 0) {
  throw new Error('No JS chunks were found under dist/assets')
}

let maxFile = { name: '', size: 0 }
for (const file of files) {
  const size = statSync(join(assetsDir, file)).size
  if (size > maxFile.size) {
    maxFile = { name: file, size }
  }
}

if (maxFile.size > maxMainChunkBytes) {
  throw new Error(
    `Largest JS chunk ${maxFile.name} is ${maxFile.size} bytes, above ${maxMainChunkBytes} bytes`
  )
}

console.log(
  `Bundle size check passed: largest JS chunk ${maxFile.name} with ${maxFile.size} bytes (limit ${maxMainChunkBytes})`
)
