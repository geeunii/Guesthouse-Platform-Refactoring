export const toISODate = (date) => {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const buildDateRange = (fromISO, toISO) => {
  if (!fromISO || !toISO) return []
  const start = new Date(`${fromISO}T00:00:00`)
  const end = new Date(`${toISO}T00:00:00`)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return []
  const dates = []
  const cursor = new Date(start)
  while (cursor <= end) {
    dates.push(toISODate(cursor))
    cursor.setDate(cursor.getDate() + 1)
  }
  return dates
}

export const fillSeriesByDate = (datesISO, items, getDate, getValue) => {
  const map = new Map(
    (items ?? []).map((item) => [String(getDate(item) ?? ''), Number(getValue(item) ?? 0)])
  )
  return datesISO.map((date) => map.get(date) ?? 0)
}

export const formatKRW = (value) => `₩${Number(value ?? 0).toLocaleString()}`
