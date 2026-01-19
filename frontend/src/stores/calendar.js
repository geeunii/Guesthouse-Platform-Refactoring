import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useCalendarStore = defineStore('calendar', () => {
  const activeCalendar = ref(null)

  const openCalendar = (id) => {
    activeCalendar.value = id
  }

  const closeCalendar = (id) => {
    if (!id || activeCalendar.value === id) {
      activeCalendar.value = null
    }
  }

  const toggleCalendar = (id) => {
    activeCalendar.value = activeCalendar.value === id ? null : id
  }

  return {
    activeCalendar,
    openCalendar,
    closeCalendar,
    toggleCalendar
  }
})
