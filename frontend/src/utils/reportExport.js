import * as XLSX from 'xlsx'

const sanitizeValue = (value) => {
  if (value === null || value === undefined) return ''
  return String(value)
}

const csvEscape = (value) => {
  const text = sanitizeValue(value)
  if (/[",\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

const downloadBlob = (blob, filename) => {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 200)
}

const normalizeSheets = (sheets) => (Array.isArray(sheets) ? sheets : [])

const buildCsvSection = (sheet) => {
  const rows = Array.isArray(sheet.rows) ? sheet.rows : []
  const columns = Array.isArray(sheet.columns) ? sheet.columns : []
  const headerLine = columns.map((col) => csvEscape(col.label)).join(',')
  const lines = [sheet.name, headerLine]

  if (!rows.length) {
    lines.push('데이터 없음')
    lines.push('')
    return lines
  }

  rows.forEach((row) => {
    lines.push(columns.map((col) => csvEscape(row[col.key])).join(','))
  })
  lines.push('')
  return lines
}

const buildSheetRows = (sheet) => {
  const rows = Array.isArray(sheet.rows) ? sheet.rows : []
  const columns = Array.isArray(sheet.columns) ? sheet.columns : []
  const headerRow = columns.map((col) => col.label)
  const bodyRows = rows.map((row) => columns.map((col) => sanitizeValue(row[col.key])))
  return [headerRow, ...bodyRows]
}

export const exportCSV = ({ filename, sheets }) => {
  const lines = normalizeSheets(sheets).flatMap(buildCsvSection)
  const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
  downloadBlob(blob, filename)
}

export const exportXLSX = ({ filename, sheets }) => {
  const workbook = XLSX.utils.book_new()
  normalizeSheets(sheets).forEach((sheet) => {
    const data = buildSheetRows(sheet)
    const worksheet = XLSX.utils.aoa_to_sheet(data)
    XLSX.utils.book_append_sheet(workbook, worksheet, sheet.name)
  })
  XLSX.writeFile(workbook, filename)
}
