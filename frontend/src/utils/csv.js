export function downloadCsv(filename, rows) {
  const csv = rows.map((row) => row.map(escapeCsvCell).join(',')).join('\n')
  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  window.setTimeout(() => {
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  }, 60000)
}

export function parseCsv(text) {
  const rows = []
  let row = []
  let cell = ''
  let inQuotes = false

  for (let index = 0; index < text.length; index += 1) {
    const char = text[index]
    const next = text[index + 1]

    if (char === '"') {
      if (inQuotes && next === '"') {
        cell += '"'
        index += 1
      } else {
        inQuotes = !inQuotes
      }
    } else if (char === ',' && !inQuotes) {
      row.push(cell)
      cell = ''
    } else if ((char === '\n' || char === '\r') && !inQuotes) {
      if (char === '\r' && next === '\n') {
        index += 1
      }
      row.push(cell)
      addRow(rows, row)
      row = []
      cell = ''
    } else {
      cell += char
    }
  }

  row.push(cell)
  addRow(rows, row)

  if (rows.length < 2) {
    return []
  }
  const headers = rows[0].map((item) => item.trim())
  return rows.slice(1).map((values) => {
    return Object.fromEntries(headers.map((header, index) => [header, values[index]?.trim() || '']))
  })
}

function escapeCsvCell(value) {
  return `"${String(value ?? '').replace(/"/g, '""')}"`
}

function addRow(rows, row) {
  if (row.some((cell) => cell.trim() !== '')) {
    rows.push(row)
  }
}
