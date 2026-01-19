import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useHostAccommodationsStore = defineStore('hostAccommodations', () => {
  const accommodations = ref([
    {
      id: 1,
      name: '제주도 감성 숙소',
      status: 'active',
      location: '제주시 애월읍',
      maxGuests: 4,
      roomCount: 2,
      price: 120000,
      images: [
        'https://picsum.photos/id/49/400/300',
        'https://picsum.photos/id/50/400/300',
        'https://picsum.photos/id/51/400/300',
        'https://picsum.photos/id/52/400/300'
      ]
    },
    {
      id: 2,
      name: '강릉 오션뷰 펜션',
      status: 'active',
      location: '강원도 강릉시',
      maxGuests: 6,
      roomCount: 3,
      price: 180000,
      images: [
        'https://picsum.photos/id/53/400/300',
        'https://picsum.photos/id/54/400/300',
        'https://picsum.photos/id/55/400/300',
        'https://picsum.photos/id/56/400/300'
      ]
    },
    {
      id: 3,
      name: '한옥 게스트하우스',
      status: 'inactive',
      location: '전주시 완산구',
      maxGuests: 8,
      roomCount: 4,
      price: 95000,
      images: [
        'https://picsum.photos/id/57/400/300',
        'https://picsum.photos/id/58/400/300',
        'https://picsum.photos/id/59/400/300',
        'https://picsum.photos/id/60/400/300'
      ]
    }
  ])

  const addAccommodation = (payload) => {
    const nextId = Math.max(0, ...accommodations.value.map((item) => item.id || 0)) + 1
    accommodations.value.unshift({
      id: payload.id ?? nextId,
      status: payload.status ?? 'active',
      ...payload
    })
  }

  const removeAccommodation = (id) => {
    accommodations.value = accommodations.value.filter((item) => item.id !== id)
  }

  return {
    accommodations,
    addAccommodation,
    removeAccommodation
  }
})
