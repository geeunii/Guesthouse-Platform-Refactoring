<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useSearchStore } from '@/stores/search'
import { useHolidayStore } from '@/stores/holiday'
import { useCalendarStore } from '@/stores/calendar'

const props = defineProps({
  mode: {
    type: String,
    default: 'desktop', // 'desktop' | 'mobile'
    validator: (value) => ['desktop', 'mobile'].includes(value)
  }
})

const searchStore = useSearchStore()
const holidayStore = useHolidayStore()
const calendarStore = useCalendarStore()

// Calendar state (local)
const currentDate = ref(new Date())

// Computed properties
const currentYear = computed(() => currentDate.value.getFullYear())
const currentMonth = computed(() => currentDate.value.getMonth())

const nextMonthDate = computed(() => new Date(currentYear.value, currentMonth.value + 1, 1))
const nextMonthYear = computed(() => nextMonthDate.value.getFullYear())
const nextMonthMonth = computed(() => nextMonthDate.value.getMonth())

const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']
const weekDays = ['일', '월', '화', '수', '목', '금', '토']

// Helpers
const toDateKey = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getHolidayInfo = (date) => holidayStore.getHolidayInfo(toDateKey(date))

const isSameDay = (date1, date2) => {
  return date1.getFullYear() === date2.getFullYear() &&
    date1.getMonth() === date2.getMonth() &&
    date1.getDate() === date2.getDate()
}

const isDateInRange = (date) => {
  if (!searchStore.startDate || !searchStore.endDate) return false
  const time = date.getTime()
  return time > searchStore.startDate.getTime() && time < searchStore.endDate.getTime()
}

// Generate days
const getCalendarDays = (year, month) => {
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const daysInMonth = lastDay.getDate()
  const startingDay = firstDay.getDay()
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  
  const maxDate = new Date()
  maxDate.setHours(0, 0, 0, 0)
  maxDate.setDate(today.getDate() + 365)
  
  const days = []
  
  // Empty cells
  for (let i = 0; i < startingDay; i++) {
    days.push({ day: '', isEmpty: true })
  }
  
  // Days
  for (let day = 1; day <= daysInMonth; day++) {
    const date = new Date(year, month, day)
    const dayOfWeek = date.getDay()
    const isStartDate = searchStore.startDate && isSameDay(date, searchStore.startDate)
    const isEndDate = searchStore.endDate && isSameDay(date, searchStore.endDate)
    const isInRange = isDateInRange(date)
    const hasEndDate = searchStore.endDate !== null
    const holidayInfo = getHolidayInfo(date)
    const isDisabled = date.getTime() < today.getTime() || date.getTime() > maxDate.getTime()
    
    days.push({
      day,
      isEmpty: false,
      date,
      isToday: isSameDay(date, new Date()),
      isSaturday: dayOfWeek === 6,
      isSunday: dayOfWeek === 0,
      isHoliday: Boolean(holidayInfo),
      holidayName: holidayInfo?.name || '',
      isStartDate,
      isEndDate,
      isInRange,
      hasEndDate,
      isDisabled
    })
  }
  
  return days
}

const calendarDays = computed(() => getCalendarDays(currentYear.value, currentMonth.value))
const nextMonthDays = computed(() => getCalendarDays(nextMonthYear.value, nextMonthMonth.value))

// Actions
const prevMonth = () => {
  currentDate.value = new Date(currentYear.value, currentMonth.value - 1, 1)
}

const nextMonth = () => {
  currentDate.value = new Date(currentYear.value, currentMonth.value + 1, 1)
}

const selectDate = (dayObj) => {
  if (dayObj.isEmpty || dayObj.isDisabled) return
  
  const clickedDate = dayObj.date
  
  // If no start date or both dates are set, start fresh
  if (!searchStore.startDate || (searchStore.startDate && searchStore.endDate)) {
    searchStore.setStartDate(clickedDate)
    searchStore.setEndDate(null)
  } else {
    // Set end date - must be AFTER start date (not same day)
    if (clickedDate.getTime() <= searchStore.startDate.getTime()) {
      searchStore.setStartDate(clickedDate)
      searchStore.setEndDate(null)
    } else {
      searchStore.setEndDate(clickedDate)
    }
  }
}

// Watchers
watch(
  [currentYear, currentMonth],
  () => {
    holidayStore.loadMonth(currentYear.value, currentMonth.value + 1)
    holidayStore.loadMonth(nextMonthYear.value, nextMonthMonth.value + 1)
  },
  { immediate: true }
)
</script>

<template>
  <!-- Mobile Calendar -->
  <div v-if="mode === 'mobile'" class="mobile-calendar-popup" @click.stop>
    <div class="mobile-calendar-header">
      <button class="calendar-nav-btn" @click.stop="prevMonth">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="20" height="20">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
        </svg>
      </button>
      <span class="calendar-month-title">{{ currentYear }}년 {{ monthNames[currentMonth] }}</span>
      <button class="calendar-nav-btn" @click.stop="nextMonth">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="20" height="20">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
        </svg>
      </button>
    </div>
    
    <div class="calendar-weekdays">
      <span
        v-for="(day, index) in weekDays"
        :key="'mobile-current-' + day"
        class="weekday"
        :class="{ sunday: index === 0, saturday: index === 6 }"
      >{{ day }}</span>
    </div>
    <div class="calendar-days">
      <span
        v-for="(dayObj, index) in calendarDays"
        :key="'mobile-current-day-' + index"
        class="calendar-day"
        :class="{
          'empty': dayObj.isEmpty,
          'today': dayObj.isToday,
          'weekend-sat': dayObj.isSaturday,
          'weekend-sun': dayObj.isSunday,
          'range-start': dayObj.isStartDate,
          'range-end': dayObj.isEndDate,
          'in-range': dayObj.isInRange,
          'has-end': dayObj.isStartDate && dayObj.hasEndDate,
          'disabled': dayObj.isDisabled,
          'sunday': dayObj.isSunday,
          'saturday': dayObj.isSaturday,
          'holiday': dayObj.isHoliday
        }"
        :title="dayObj.holidayName || null"
        @click.stop="selectDate(dayObj)"
      >
        {{ dayObj.day }}
      </span>
    </div>
  </div>

  <!-- Desktop Calendar -->
  <div v-else class="calendar-popup" @click.stop>
    <div class="calendar-container">
      <!-- Navigation Prev -->
      <button class="calendar-nav-btn nav-prev" @click.stop="prevMonth">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="20" height="20">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
        </svg>
      </button>

      <!-- Current Month -->
      <div class="calendar-month">
        <div class="calendar-month-title">{{ currentYear }}년 {{ monthNames[currentMonth] }}</div>
        <div class="calendar-weekdays">
          <span
            v-for="(day, index) in weekDays"
            :key="'current-' + day"
            class="weekday"
            :class="{ sunday: index === 0, saturday: index === 6 }"
          >{{ day }}</span>
        </div>
        <div class="calendar-days">
          <span
            v-for="(dayObj, index) in calendarDays"
            :key="'current-day-' + index"
            class="calendar-day"
            :class="{
              'empty': dayObj.isEmpty,
              'today': dayObj.isToday,
              'weekend-sat': dayObj.isSaturday,
              'weekend-sun': dayObj.isSunday,
              'range-start': dayObj.isStartDate,
              'range-end': dayObj.isEndDate,
              'in-range': dayObj.isInRange,
              'has-end': dayObj.isStartDate && dayObj.hasEndDate,
              'disabled': dayObj.isDisabled,
              'sunday': dayObj.isSunday,
              'saturday': dayObj.isSaturday,
              'holiday': dayObj.isHoliday
            }"
            :title="dayObj.holidayName || null"
            @click="selectDate(dayObj)"
          >
            {{ dayObj.day }}
          </span>
        </div>
      </div>

      <!-- Next Month -->
      <div class="calendar-month">
        <div class="calendar-month-title">{{ nextMonthYear }}년 {{ monthNames[nextMonthMonth] }}</div>
        <div class="calendar-weekdays">
          <span
            v-for="(day, index) in weekDays"
            :key="'next-' + day"
            class="weekday"
            :class="{ sunday: index === 0, saturday: index === 6 }"
          >{{ day }}</span>
        </div>
        <div class="calendar-days">
          <span
            v-for="(dayObj, index) in nextMonthDays"
            :key="'next-day-' + index"
            class="calendar-day"
            :class="{
              'empty': dayObj.isEmpty,
              'today': dayObj.isToday,
              'weekend-sat': dayObj.isSaturday,
              'weekend-sun': dayObj.isSunday,
              'range-start': dayObj.isStartDate,
              'range-end': dayObj.isEndDate,
              'in-range': dayObj.isInRange,
              'has-end': dayObj.isStartDate && dayObj.hasEndDate,
              'disabled': dayObj.isDisabled,
              'sunday': dayObj.isSunday,
              'saturday': dayObj.isSaturday,
              'holiday': dayObj.isHoliday
            }"
            :title="dayObj.holidayName || null"
            @click="selectDate(dayObj)"
          >
            {{ dayObj.day }}
          </span>
        </div>
      </div>

      <!-- Navigation Next -->
      <button class="calendar-nav-btn nav-next" @click.stop="nextMonth">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="20" height="20">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
        </svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
/* Mobile Calendar Styles */
.mobile-calendar-popup {
  background: #fff;
  border: 1px solid #f0f0f0;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  border-radius: 16px;
  padding: 16px;
  margin: 8px 0;
  font-family: 'Noto Sans KR', sans-serif;
  animation: calendarFadeIn 0.2s ease;
}

.mobile-calendar-popup .calendar-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}

.mobile-calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.mobile-calendar-header .calendar-month-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1f36;
}

.mobile-calendar-popup .calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 8px;
}

.mobile-calendar-popup .weekday {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
  padding: 8px 0;
}

.mobile-calendar-popup .weekday.sunday { color: #ef4444; }
.mobile-calendar-popup .weekday.saturday { color: #2563eb; }

.mobile-calendar-popup .calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.mobile-calendar-popup .calendar-day {
  text-align: center;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  padding: 12px 0;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s ease;
}

.mobile-calendar-popup .calendar-day.sunday:not(.range-start):not(.range-end):not(.disabled) { color: #ef4444; }
.mobile-calendar-popup .calendar-day.saturday:not(.range-start):not(.range-end):not(.disabled) { color: #2563eb; }
.mobile-calendar-popup .calendar-day.holiday:not(.range-start):not(.range-end):not(.disabled) {
  color: #ef4444; font-weight: 600;
}
.mobile-calendar-popup .calendar-day.today:not(.range-start):not(.range-end):not(.in-range) {
  color: #6DC3BB; font-weight: 700;
}
.mobile-calendar-popup .calendar-day:not(.empty):not(.disabled):hover {
  background-color: #f0f7f6; color: #6DC3BB;
}
.mobile-calendar-popup .calendar-day.in-range {
  background-color: #BFE7DF; color: #2d7a73; border-radius: 0;
}
.mobile-calendar-popup .calendar-day.range-start,
.mobile-calendar-popup .calendar-day.range-end {
  background-color: #5CC5B3; color: #fff; font-weight: 700;
}
.mobile-calendar-popup .calendar-day.range-start:hover,
.mobile-calendar-popup .calendar-day.range-end:hover {
  background-color: #49B5A3; color: #fff;
}

/* Desktop Calendar Styles */
.calendar-popup {
  position: absolute;
  top: calc(100% + 12px);
  left: 50%;
  transform: translateX(-50%);
  width: 680px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  padding: 24px;
  z-index: 90;
  font-family: 'Noto Sans KR', sans-serif;
  border: 1px solid #f0f0f0;
  animation: calendarFadeIn 0.2s ease;
}

.calendar-container {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.calendar-month { flex: 1; min-width: 280px; }

.calendar-month-title {
  font-size: 16px; font-weight: 700; color: #1a1f36; text-align: center; margin-bottom: 16px;
}

.calendar-nav-btn {
  background: transparent; border: none; cursor: pointer; padding: 8px; border-radius: 8px;
  color: #6b7280; transition: all 0.2s ease; display: flex; align-items: center; justify-content: center;
}
.calendar-nav-btn:hover { background-color: #f0f7f6; color: #6DC3BB; }

.calendar-weekdays {
  display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; margin-bottom: 8px;
}

.weekday {
  text-align: center; font-size: 12px; font-weight: 600; color: #9ca3af; padding: 8px 0;
}
.weekday.sunday { color: #ef4444; }
.weekday.saturday { color: #2563eb; }

.calendar-days {
  display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px;
}

.calendar-day {
  text-align: center; font-size: 14px; font-weight: 500; color: #374151; padding: 12px 0;
  border-radius: 8px; cursor: pointer; transition: all 0.2s ease; position: relative;
}
.calendar-day.disabled { color: #b4b8bf; cursor: not-allowed; pointer-events: none; }
.calendar-day.sunday:not(.range-start):not(.range-end):not(.disabled) { color: #ef4444; }
.calendar-day.holiday:not(.range-start):not(.range-end):not(.disabled) { color: #ef4444; font-weight: 600; }
.calendar-day.saturday:not(.range-start):not(.range-end):not(.disabled) { color: #2563eb; }
.calendar-day:not(.empty):not(.disabled):hover { background-color: #f0f7f6; color: #6DC3BB; }
.calendar-day.empty { cursor: default; }
.calendar-day.today:not(.range-start):not(.range-end):not(.in-range):not(.disabled) { color: #6DC3BB; font-weight: 700; }

.calendar-day.range-start,
.calendar-day.range-end {
  background-color: #5CC5B3; color: white; font-weight: 700; position: relative;
}

.calendar-day.range-start.has-end::after {
  content: ''; position: absolute; top: 0; right: 0; width: 50%; height: 100%;
  background-color: #BFE7DF; z-index: -1;
}
.calendar-day.range-end::before {
  content: ''; position: absolute; top: 0; left: 0; width: 50%; height: 100%;
  background-color: #BFE7DF; z-index: -1;
}

.calendar-day.range-start:hover,
.calendar-day.range-end:hover { background-color: #49B5A3; color: white; }

.calendar-day.in-range {
  background-color: #BFE7DF; color: #2d7a73; border-radius: 0;
}

@keyframes calendarFadeIn {
  from { opacity: 0; transform: translateX(-50%) translateY(-10px); }
  to { opacity: 1; transform: translateX(-50%) translateY(0); }
}

@media (max-width: 768px) {
  .calendar-popup {
    width: 320px; left: 0; transform: translateX(0); padding: 16px;
  }
  .calendar-container { flex-direction: column; gap: 24px; }
  .calendar-month { min-width: 100%; }
  .nav-prev, .nav-next { position: absolute; top: 16px; }
  .nav-prev { left: 8px; }
  .nav-next { right: 8px; }
  
  @keyframes calendarFadeIn {
    from { opacity: 0; transform: translateY(-10px); }
    to { opacity: 1; transform: translateY(0); }
  }
  
  .calendar-day { padding: 10px 0; font-size: 13px; }
  .calendar-day.range-start.has-end::after,
  .calendar-day.range-end::before { display: none; }
}

@media (min-width: 769px) {
  .nav-prev, .nav-next { flex-shrink: 0; }
}
</style>
