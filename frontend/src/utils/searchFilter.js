export const normalizeKeyword = (value) => {
  return String(value ?? '').trim().toLowerCase()
}

export const matchesKeyword = (item, keyword) => {
  const normalized = normalizeKeyword(keyword)
  if (!normalized) return true
  const title = item?.title ?? ''
  const location = item?.location ?? ''
  const haystack = `${title} ${location}`.toLowerCase()
  return haystack.includes(normalized)
}
