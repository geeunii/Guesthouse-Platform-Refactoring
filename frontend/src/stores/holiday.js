import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchHolidays } from '@/api/holiday'

const pad2 = (value) => String(value).padStart(2, '0')
const buildMonthKey = (year, month) => `${year}-${pad2(month)}`

export const useHolidayStore = defineStore('holiday', () => {
  const holidayByDate = ref({})
  const loadedMonths = ref({})
  const loadingMonths = ref({})

  const loadMonth = async (year, month) => {
    const key = buildMonthKey(year, month)
    if (loadedMonths.value[key] || loadingMonths.value[key]) return

    loadingMonths.value = { ...loadingMonths.value, [key]: true }
    try {
      const response = await fetchHolidays(year, month)
      if (response.ok && Array.isArray(response.data?.holidays)) {
        const next = { ...holidayByDate.value }
        response.data.holidays.forEach((item) => {
          if (!item?.date) return
          if (item.isHoliday !== true) return
          next[item.date] = {
            name: item.name || '',
            isHoliday: true
          }
        })
        holidayByDate.value = next
      }
      loadedMonths.value = { ...loadedMonths.value, [key]: true }
    } catch (error) {
      loadedMonths.value = { ...loadedMonths.value, [key]: true }
    } finally {
      const nextLoading = { ...loadingMonths.value }
      delete nextLoading[key]
      loadingMonths.value = nextLoading
    }
  }

  const getHolidayInfo = (dateKey) => {
    return holidayByDate.value[dateKey] || null
  }

  return {
    holidayByDate,
    loadedMonths,
    loadMonth,
    getHolidayInfo
  }
})
