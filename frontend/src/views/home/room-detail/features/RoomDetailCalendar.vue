<script setup>
import { computed, ref, watch } from 'vue'
import { useSearchStore } from '@/stores/search'
import { useHolidayStore } from '@/stores/holiday'
import { useCalendarStore } from '@/stores/calendar'

const searchStore = useSearchStore()
const holidayStore = useHolidayStore()
const calendarStore = useCalendarStore()

const isCalendarOpen = computed(() => calendarStore.activeCalendar === 'room-detail')
const currentDate = ref(new Date())

const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']
const weekDays = ['일', '월', '화', '수', '목', '금', '토']

const toDateKey = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getHolidayInfo = (date) => holidayStore.getHolidayInfo(toDateKey(date))

const currentYear = computed(() => currentDate.value.getFullYear())
const currentMonth = computed(() => currentDate.value.getMonth())
const nextMonthDate = computed(() => new Date(currentYear.value, currentMonth.value + 1, 1))
const nextMonthYear = computed(() => nextMonthDate.value.getFullYear())
const nextMonthMonth = computed(() => nextMonthDate.value.getMonth())

const isSameDay = (date1, date2) => {
  return date1.getFullYear() === date2.getFullYear()
    && date1.getMonth() === date2.getMonth()
    && date1.getDate() === date2.getDate()
}

const isDateInRange = (date) => {
  if (!searchStore.startDate || !searchStore.endDate) return false
  const time = date.getTime()
  return time > searchStore.startDate.getTime() && time < searchStore.endDate.getTime()
}

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
  for (let i = 0; i < startingDay; i += 1) {
    days.push({ day: '', isEmpty: true })
  }
  for (let day = 1; day <= daysInMonth; day += 1) {
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
      isStartDate,
      isEndDate,
      isInRange,
      hasEndDate,
      isDisabled,
      isSunday: dayOfWeek === 0,
      isSaturday: dayOfWeek === 6,
      isHoliday: Boolean(holidayInfo),
      holidayName: holidayInfo?.name || ''
    })
  }
  return days
}

const calendarDays = computed(() => getCalendarDays(currentYear.value, currentMonth.value))
const nextMonthDays = computed(() => getCalendarDays(nextMonthYear.value, nextMonthMonth.value))

watch(
  [currentYear, currentMonth],
  () => {
    holidayStore.loadMonth(currentYear.value, currentMonth.value + 1)
    holidayStore.loadMonth(nextMonthYear.value, nextMonthMonth.value + 1)
  },
  { immediate: true }
)

const prevMonth = () => {
  currentDate.value = new Date(currentYear.value, currentMonth.value - 1, 1)
}

const nextMonth = () => {
  currentDate.value = new Date(currentYear.value, currentMonth.value + 1, 1)
}

const selectDate = (dayObj) => {
  if (dayObj.isEmpty || dayObj.isDisabled) return

  const clickedDate = dayObj.date
  if (!searchStore.startDate || (searchStore.startDate && searchStore.endDate)) {
    searchStore.setStartDate(clickedDate)
    searchStore.setEndDate(null)
    return
  }

  if (clickedDate.getTime() < searchStore.startDate.getTime()) {
    searchStore.setEndDate(searchStore.startDate)
    searchStore.setStartDate(clickedDate)
    calendarStore.closeCalendar('room-detail')
    return
  }

  searchStore.setEndDate(clickedDate)
  calendarStore.closeCalendar('room-detail')
}
</script>

<template>
  <div class="calendar-popup" v-if="isCalendarOpen">
    <div class="calendar-container">
      <button class="calendar-nav-btn nav-prev" type="button" @click="prevMonth">
        &lsaquo;
      </button>

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
              empty: dayObj.isEmpty,
              today: dayObj.isToday,
              'range-start': dayObj.isStartDate,
              'range-end': dayObj.isEndDate,
              'in-range': dayObj.isInRange,
              'has-end': dayObj.isStartDate && dayObj.hasEndDate,
              disabled: dayObj.isDisabled,
              sunday: dayObj.isSunday,
              saturday: dayObj.isSaturday,
              holiday: dayObj.isHoliday
            }"
            :title="dayObj.holidayName || null"
            @click="selectDate(dayObj)"
          >
            {{ dayObj.day }}
          </span>
        </div>
      </div>

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
              empty: dayObj.isEmpty,
              today: dayObj.isToday,
              'range-start': dayObj.isStartDate,
              'range-end': dayObj.isEndDate,
              'in-range': dayObj.isInRange,
              'has-end': dayObj.isStartDate && dayObj.hasEndDate,
              disabled: dayObj.isDisabled,
              sunday: dayObj.isSunday,
              saturday: dayObj.isSaturday,
              holiday: dayObj.isHoliday
            }"
            :title="dayObj.holidayName || null"
            @click="selectDate(dayObj)"
          >
            {{ dayObj.day }}
          </span>
        </div>
      </div>

      <button class="calendar-nav-btn nav-next" type="button" @click="nextMonth">
        &rsaquo;
      </button>
    </div>
  </div>
</template>

<style scoped>
.calendar-popup {
  position: absolute;
  top: calc(100% + 12px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 90;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  padding: 24px;
  width: min(680px, 100%);
  max-width: 100%;
  font-family: 'Noto Sans KR', sans-serif;
  animation: calendarFadeIn 0.2s ease;
  box-sizing: border-box;
}

@keyframes calendarFadeIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

.calendar-container {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.calendar-month {
  flex: 1 1 0;
  min-width: 0;
  max-width: 100%;
}

.calendar-month-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1f36;
  text-align: center;
  margin-bottom: 16px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 8px;
}

.weekday {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
  padding: 8px 0;
}

.weekday.sunday {
  color: #ef4444;
}

.weekday.saturday {
  color: #2563eb;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.calendar-day {
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

.calendar-day.disabled {
  color: #b4b8bf;
  cursor: not-allowed;
  pointer-events: none;
}

.calendar-day.sunday:not(.range-start):not(.range-end):not(.disabled) {
  color: #ef4444;
}

.calendar-day.holiday:not(.range-start):not(.range-end):not(.disabled) {
  color: #ef4444;
  font-weight: 600;
}

.calendar-day.saturday:not(.range-start):not(.range-end):not(.disabled) {
  color: #2563eb;
}

.calendar-day:not(.empty):not(.disabled):hover {
  background-color: #f0f7f6;
  color: #6DC3BB;
}

.calendar-day.empty {
  cursor: default;
}

.calendar-day.today:not(.range-start):not(.range-end):not(.in-range):not(.disabled) {
  color: #6DC3BB;
  font-weight: 700;
}

.calendar-day.in-range {
  background-color: #BFE7DF;
  color: #2d7a73;
  border-radius: 0;
}

.calendar-day.range-start,
.calendar-day.range-end {
  background-color: #5CC5B3;
  color: #fff;
  font-weight: 700;
  position: relative;
}

.calendar-day.range-start.has-end::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 50%;
  height: 100%;
  background-color: #BFE7DF;
  z-index: -1;
}

.calendar-day.range-end::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 50%;
  height: 100%;
  background-color: #BFE7DF;
  z-index: -1;
}

.calendar-day.range-start:hover,
.calendar-day.range-end:hover {
  background-color: #49B5A3;
  color: #fff;
}

.calendar-nav-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  color: #6b7280;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  flex-shrink: 0;
}

.calendar-nav-btn:hover {
  background-color: #f0f7f6;
  color: #6DC3BB;
}

@media (max-width: 768px) {
  .calendar-container {
    flex-direction: column;
  }

  .calendar-month {
    width: 100%;
  }

  .calendar-popup {
    width: 100%;
    left: 0;
    transform: none;
    padding: 16px;
    animation: calendarFadeInMobile 0.2s ease;
  }

  .calendar-day {
    padding: 10px 0;
    font-size: 13px;
  }

  .calendar-day.range-start.has-end::after,
  .calendar-day.range-end::before {
    display: none;
  }
}

@keyframes calendarFadeInMobile {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
