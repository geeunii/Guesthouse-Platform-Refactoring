export const extractItems = (payload) => {
  if (Array.isArray(payload)) return payload
  return payload?.items ?? payload?.content ?? payload?.data ?? []
}

export const extractPageMeta = (payload) => {
  const meta = payload?.page ?? {}
  return {
    page: meta?.number ?? payload?.page ?? 0,
    size: meta?.size ?? payload?.size ?? 20,
    totalElements: meta?.totalElements ?? payload?.totalElements ?? 0,
    totalPages: meta?.totalPages ?? payload?.totalPages ?? 0
  }
}

export const toQueryParams = (query) => {
  const params = {}
  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '' || value === 'all') return
    params[key] = value
  })
  return params
}
