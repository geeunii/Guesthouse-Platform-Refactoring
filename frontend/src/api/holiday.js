import { hostGet } from './adminClient'

export async function fetchHolidays(year, month) {
  return hostGet('/public/holidays', { year, month })
}
