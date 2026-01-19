import { useSearchStore } from '@/stores/search'

const parseNumberParam = (value) => {
  if (value === undefined || value === null || value === '') return null
  const raw = Array.isArray(value) ? value[0] : value
  const numberValue = Number(raw)
  return Number.isFinite(numberValue) ? numberValue : null
}

const formatDateParam = (date) => {
  if (!(date instanceof Date)) return null
  if (Number.isNaN(date.getTime())) return null
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const parseThemeIds = (value) => {
  if (!value) return []
  const raw = Array.isArray(value) ? value.join(',') : String(value)
  return raw
    .split(',')
    .map((item) => Number(item))
    .filter((item) => Number.isFinite(item))
}

const parseGuestCount = (value) => {
  const parsed = parseNumberParam(value)
  if (!Number.isFinite(parsed)) return 1
  return Math.max(1, parsed)
}

const parseDateParam = (value) => {
  if (!value) return null
  const raw = Array.isArray(value) ? value[0] : value
  if (!raw) return null
  const [year, month, day] = String(raw).trim().split('-')
  const yearNumber = Number(year)
  const monthNumber = Number(month)
  const dayNumber = Number(day)
  if (!Number.isFinite(yearNumber) || !Number.isFinite(monthNumber) || !Number.isFinite(dayNumber)) return null
  const date = new Date(yearNumber, monthNumber - 1, dayNumber)
  if (Number.isNaN(date.getTime())) return null
  return date
}

export const useListingFilters = () => {
  const searchStore = useSearchStore()

  const applyRouteFilters = (query = {}) => {
    const minValue = parseNumberParam(query.min ?? query.minPrice)
    const maxValue = parseNumberParam(query.max ?? query.maxPrice)
    const themeIds = parseThemeIds(query.themeIds)
    const guestCount = parseGuestCount(query.guestCount)
    const startDate = parseDateParam(query.checkin ?? query.checkIn)
    const endDate = parseDateParam(query.checkout ?? query.checkOut)

    searchStore.setPriceRange(minValue, maxValue)
    searchStore.setThemeIds(themeIds)
    searchStore.setGuestCount(guestCount)
    searchStore.setStartDate(startDate ?? null)
    searchStore.setEndDate(endDate ?? null)
  }

  const buildFilterQuery = (keywordOverride) => {
    const query = {}

    if (searchStore.minPrice !== null) query.min = String(searchStore.minPrice)
    if (searchStore.maxPrice !== null) query.max = String(searchStore.maxPrice)
    if (searchStore.themeIds.length) query.themeIds = searchStore.themeIds.join(',')
    if (searchStore.guestCount > 0) query.guestCount = String(searchStore.guestCount)

    const checkin = formatDateParam(searchStore.startDate)
    const checkout = formatDateParam(searchStore.endDate)
    if (checkin && checkout) {
      query.checkin = checkin
      query.checkout = checkout
    }

    const keyword = String(keywordOverride ?? searchStore.keyword ?? '').trim()
    if (keyword) query.keyword = keyword

    return query
  }

  return {
    applyRouteFilters,
    buildFilterQuery
  }
}
