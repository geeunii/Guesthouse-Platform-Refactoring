const weekDays = ['일', '월', '화', '수', '목', '금', '토']

export const formatCurrency = (value) => {
  const amount = Number(value ?? 0)
  if (!Number.isFinite(amount)) return '₩0'
  return `₩${Math.round(amount).toLocaleString()}`
}

export const formatNumber = (value) => {
  const amount = Number(value ?? 0)
  if (!Number.isFinite(amount)) return '0'
  return Math.round(amount).toLocaleString()
}

export const formatDateTime = (value, includeYear = false) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const label = `${month}.${day}(${weekDays[date.getDay()]}) ${hours}:${minutes}`
  return includeYear ? `${year}.${label}` : label
}

export const formatDate = (value, includeYear = false) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const label = `${month}.${day}(${weekDays[date.getDay()]})`
  return includeYear ? `${year}.${label}` : label
}

export const formatDateRange = (start, end, includeYear = false) => {
  const from = formatDateTime(start, includeYear)
  const to = formatDateTime(end, includeYear)
  if (!from && !to) return ''
  return `${from} → ${to}`
}

export const formatShortTime = (time) => {
  if (!time) return ''
  if (typeof time === 'string') {
    const parts = time.split(':')
    return parts.length >= 2 ? `${parts[0]}:${parts[1]}` : time
  }
  return String(time)
}
