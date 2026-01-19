<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSearchStore } from '@/stores/search'
import { useHolidayStore } from '@/stores/holiday'
import { useListingFilters } from '@/composables/useListingFilters'
import { fetchThemes } from '@/api/theme'
import { fetchAccommodationDetail, fetchAccommodationAvailability } from '@/api/accommodation'
import { getReviewsByAccommodation } from '@/api/reviewApi'
import { getDownloadableCoupons, issueCoupon, getMyCoupons } from '@/api/couponApi'
import { isAuthenticated } from '@/api/authClient'

import ImageGallery from './room-detail/features/ImageGallery.vue'
import ReviewSection from './room-detail/features/ReviewSection.vue'
import MapSection from './room-detail/features/MapSection.vue'
import RoomDetailCalendar from './room-detail/features/RoomDetailCalendar.vue'
import RoomListSection from './room-detail/features/RoomListSection.vue'
import DetailSkeleton from '@/components/DetailSkeleton.vue'
import AiSummarySection from '@/components/accommodation/AiSummarySection.vue'
import { useCalendarStore } from '@/stores/calendar'

const router = useRouter()
const route = useRoute()
const searchStore = useSearchStore()
const holidayStore = useHolidayStore()
const { applyRouteFilters } = useListingFilters()

const DEFAULT_IMAGE = 'https://placehold.co/800x600'
const DEFAULT_HOST_IMAGE = 'https://picsum.photos/seed/host/100/100'


const toNumber = (value, fallback = 0) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

const hasFilterQuery = (query) => {
  if (!query) return false
  const keys = [
    'guestCount',
    'checkin',
    'checkout',
    'checkIn',
    'checkOut',
    'min',
    'max',
    'minPrice',
    'maxPrice',
    'themeIds',
    'keyword'
  ]
  return keys.some((key) => query[key] !== undefined)
}

const getAccommodationId = () => {
  const parsed = Number(route.params.id)
  return Number.isFinite(parsed) ? parsed : null
}

const buildFilterQuery = () => {
  const query = {}
  const minValue = route.query.min ?? route.query.minPrice
  const maxValue = route.query.max ?? route.query.maxPrice
  if (minValue !== undefined) query.min = String(minValue)
  if (maxValue !== undefined) query.max = String(maxValue)
  if (route.query.themeIds) query.themeIds = String(route.query.themeIds)
  if (route.query.guestCount) query.guestCount = String(route.query.guestCount)
  if (route.query.checkin) query.checkin = String(route.query.checkin)
  if (route.query.checkout) query.checkout = String(route.query.checkout)
  if (route.query.keyword) query.keyword = String(route.query.keyword)
  if (route.query.sort) query.sort = String(route.query.sort)
  return query
}

const goBack = () => {
  const from = route.query.from
  if (from === 'map') {
    router.push({ path: '/map', query: buildFilterQuery() })
    return
  }
  if (from === 'list') {
    // page 파라미터도 함께 전달하여 이전 페이지 유지
    const pageParam = route.query.page
    router.push({ path: '/list', query: { ...buildFilterQuery(), page: pageParam } })
    return
  }
  router.back()
}

const createEmptyGuesthouse = (id = null) => ({
  id,
  category: '',
  name: '',
  rating: '-',
  reviewCount: 0,
  address: '',
  description: '',
  minPrice: null,
  checkInTime: '',
  checkOutTime: '',
  transportInfo: '',
  parkingInfo: '',
  sns: '',
  phone: '',
  latitude: null,
  longitude: null,
  amenities: [],
  themes: [],
  host: {
    name: '호스트',
    joined: '',
    image: DEFAULT_HOST_IMAGE
  },
  images: [DEFAULT_IMAGE],
  rooms: [],
  reviews: []
})

const buildAddress = (data) => {
  return [data?.city, data?.district, data?.township, data?.addressDetail]
      .filter(Boolean)
      .join(' ')
}

const normalizeRooms = (rooms, fallbackPrice) => {
  if (!Array.isArray(rooms)) return []
  return rooms
      .filter((room) => room?.roomStatus === 1)
      .map((room) => {
        const price = toNumber(room?.price ?? room?.weekendPrice ?? fallbackPrice, fallbackPrice)
        const roomStatus = room?.roomStatus
        const available = roomStatus == null ? true : roomStatus === 1
        const imageUrl = room?.mainImageUrl || DEFAULT_IMAGE
      
      // 썸네일 URL (원본 그대로 사용)
      const thumbnailUrl = imageUrl

      return {
        id: room?.roomId ?? room?.id,
        name: room?.roomName ?? room?.name ?? '',
        introduction: room?.roomIntroduction ?? '',
        description: room?.roomDescription ?? room?.description ?? '',
        capacity: toNumber(room?.maxGuests ?? room?.capacity ?? 0, 0),
        minGuests: toNumber(room?.minGuests ?? 1, 1),
        price,
        available,
        imageUrl,
        thumbnailUrl
      }
    })
}

const normalizeAmenities = (data) => {
  if (Array.isArray(data?.amenityDetails)) {
    return data.amenityDetails
        .map((amenity) => ({
          name: amenity?.amenityName ?? amenity?.name ?? '',
          icon: amenity?.amenityIcon ?? amenity?.icon ?? ''
        }))
        .filter((amenity) => amenity.name)
  }
  if (Array.isArray(data?.amenities)) {
    return data.amenities
        .map((name) => ({ name, icon: '' }))
        .filter((amenity) => amenity.name)
  }
  return []
}

const formatReviewDate = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}.${month}.${day}`
}

const normalizeReviews = (reviews) => {
  if (!Array.isArray(reviews)) return []
  return reviews.map((review) => {
    const ratingValue = Number(review?.rating)
    const rating = Number.isFinite(ratingValue)
        ? Math.min(Math.max(Math.round(ratingValue * 2) / 2, 0), 5)
        : 0
    const images = Array.isArray(review?.images) ? review.images : []
    const imageUrls = images
        .map((image) => image?.imageUrl ?? image)
        .filter((url) => typeof url === 'string' && url.trim())
    const tags = Array.isArray(review?.tags) ? review.tags : []
    return {
      id: review?.reviewId ?? review?.id,
      author: review?.authorName ?? review?.reviewerEmail ?? review?.author ?? '',
      date: formatReviewDate(review?.createdAt ?? review?.date),
      rating,
      content: review?.content ?? '',
      image: imageUrls[0] ?? null,
      images: imageUrls,
      tags: tags
          .map((tag) => tag?.reviewTagName ?? tag?.name ?? tag)
          .filter((tag) => typeof tag === 'string' && tag.trim())
    }
  })
}

const normalizeDetail = (data) => {
  const imageUrls = Array.isArray(data?.images)
      ? data.images.map((image) => image?.imageUrl).filter(Boolean)
      : []
  const fallbackPrice = toNumber(data?.minPrice ?? 0, 0)
  const ratingValue = Number(data?.rating)
  const reviews = normalizeReviews(data?.reviews)
  // Fallback to snake_case if camelCase is missing
  const category = data?.accommodationsCategory || data?.accommodations_category || ''

  return {
    id: data?.accommodationsId ?? null,
    category,
    name: data?.accommodationsName ?? '',
    rating: Number.isFinite(ratingValue) ? ratingValue.toFixed(2) : '-',
    reviewCount: toNumber(data?.reviewCount ?? reviews.length, 0),
    address: buildAddress(data),
    description: data?.accommodationsDescription ?? data?.shortDescription ?? '',
    minPrice: data?.minPrice ?? null,
    checkInTime: data?.checkInTime ?? '',
    checkOutTime: data?.checkOutTime ?? '',
    transportInfo: data?.transportInfo ?? '',
    parkingInfo: data?.parkingInfo ?? '',
    sns: data?.sns ?? '',
    phone: data?.phone ?? '',
    latitude: data?.latitude ?? null,
    longitude: data?.longitude ?? null,
    amenities: normalizeAmenities(data),
    themes: Array.isArray(data?.themes) ? data.themes : [],
    host: {
      name: '호스트',
      joined: '',
      image: DEFAULT_HOST_IMAGE
    },
    images: imageUrls.length ? imageUrls : [DEFAULT_IMAGE],
    rooms: normalizeRooms(data?.rooms, fallbackPrice),
    reviews
  }
}

const guesthouse = ref(createEmptyGuesthouse(getAccommodationId()))
const selectedRoom = ref(null)
const availableRoomIds = ref(null)
const isAvailabilityLoading = ref(false)
const calendarStore = useCalendarStore()
const isCalendarOpen = computed(() => calendarStore.activeCalendar === 'room-detail')
// showAllRooms, currentDate removed
const datePickerRef = ref(null)
let availabilityRequestId = 0
const availableCoupons = ref([])
const isCouponModalOpen = ref(false)
const downloadedCouponIds = ref(new Set())
const themeCatalog = ref([])
const isThemeCatalogLoading = ref(false)
const isDataLoading = ref(true)
// isUnavailableModalOpen removed

const canBook = computed(() => {
  return Boolean(selectedRoom.value && searchStore.startDate && searchStore.endDate)
})

// 숙박 일수 계산
const stayNights = computed(() => {
  if (!searchStore.startDate || !searchStore.endDate) return 1
  const diffTime = searchStore.endDate.getTime() - searchStore.startDate.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  return diffDays > 0 ? diffDays : 1
})

// 총 가격 계산 (객실 가격 × 숙박 일수)
const totalPrice = computed(() => {
  if (!selectedRoom.value) return 0
  let price = selectedRoom.value.price * stayNights.value
  // minGuests가 1인 경우 (게스트하우스 도미토리 등) 인원수 비례 요금 적용
  if (selectedRoom.value.minGuests === 1) {
    const count = Number(searchStore.guestCount)
    price *= Math.max(1, Number.isFinite(count) ? count : 1)
  }
  return price
})

const hasDateRange = computed(() => Boolean(searchStore.startDate && searchStore.endDate))

const filteredRooms = computed(() => {
  const rooms = guesthouse.value.rooms || []
  const guestCount = searchStore.guestCount
  const hasGuestFilter = guestCount > 0
  const applyAvailability = hasDateRange.value && availableRoomIds.value instanceof Set

  return rooms.filter((room) => {
    if (hasGuestFilter && room.capacity < guestCount) return false
    if (applyAvailability && !availableRoomIds.value.has(room.id)) return false
    return true
  })
})



const toggleCalendar = () => {
  if (isCalendarOpen.value) {
    calendarStore.closeCalendar('room-detail')
  } else {
    calendarStore.openCalendar('room-detail')
  }
}
// Calendar logic moved to RoomDetailCalendar.vue

const loadAvailability = async () => {
  const accommodationsId = getAccommodationId()
  if (!accommodationsId) {
    availableRoomIds.value = null
    return
  }
  if (!searchStore.startDate || !searchStore.endDate) {
    availableRoomIds.value = null
    return
  }

  const currentRequest = ++availabilityRequestId
  isAvailabilityLoading.value = true
  try {
    const response = await fetchAccommodationAvailability(accommodationsId, {
      checkin: searchStore.startDate,
      checkout: searchStore.endDate,
      guestCount: searchStore.guestCount
    })
    if (currentRequest !== availabilityRequestId) return
    if (response.ok && Array.isArray(response.data?.availableRoomIds)) {
      availableRoomIds.value = new Set(response.data.availableRoomIds)
    } else {
      availableRoomIds.value = new Set()
    }
  } catch (error) {
    console.error('Failed to load room availability', error)
    if (currentRequest === availabilityRequestId) {
      availableRoomIds.value = null
    }
  } finally {
    if (currentRequest === availabilityRequestId) {
      isAvailabilityLoading.value = false
    }
  }
}

const loadThemeCatalog = async () => {
  if (isThemeCatalogLoading.value) return
  isThemeCatalogLoading.value = true
  try {
    const response = await fetchThemes()
    if (response.ok && Array.isArray(response.data)) {
      themeCatalog.value = response.data
    }
  } catch (error) {
    console.error('Failed to load theme catalog', error)
  } finally {
    isThemeCatalogLoading.value = false
  }
}

const loadAccommodation = async () => {
  const accommodationsId = getAccommodationId()
  if (!accommodationsId) {
    guesthouse.value = createEmptyGuesthouse()
    isDataLoading.value = false
    return
  }

  isDataLoading.value = true
  selectedRoom.value = null
  availableRoomIds.value = null
  isAvailabilityLoading.value = false
  guesthouse.value = createEmptyGuesthouse(accommodationsId)
  downloadedCouponIds.value = new Set() // Reset set

  try {
    // 숙소 상세 정보와 리뷰를 병렬로 조회
    const [detailResponse, reviewsData, couponsData] = await Promise.all([
      fetchAccommodationDetail(accommodationsId),
      getReviewsByAccommodation(accommodationsId).catch(() => []),
      getDownloadableCoupons(accommodationsId).catch(() => [])
    ])

    if (!detailResponse.ok || !detailResponse.data) {
      router.replace({ name: 'not-found' })
      return
    }

    // 숙소 상세 데이터에 리뷰 데이터 병합
    const detailWithReviews = {
      ...detailResponse.data,
      reviews: reviewsData
    }
    guesthouse.value = normalizeDetail(detailWithReviews)
    availableCoupons.value = couponsData || []

    // 내 쿠폰 목록 조회 (모든 상태: ISSUED, USED, EXPIRED 등)
    try {
      const myCoupons = await getMyCoupons('ALL')
      // 내 쿠폰 중, 현재 다운로드 가능한 쿠폰과 ID가 같은 것들을 찾아 Set에 추가
      myCoupons.forEach(userCoupon => {
        // UserCouponResponseDto는 flatten된 구조 (couponId)
        if (userCoupon.couponId) {
          downloadedCouponIds.value.add(String(userCoupon.couponId))
        }
      })
    } catch (e) {
      console.warn('내 쿠폰 목록 조회 실패 (비로그인 상태일 수 있음):', e)
    }
  } catch (error) {
    console.error('Failed to load accommodation detail', error)
    router.replace({ name: 'not-found' })
  } finally {
    isDataLoading.value = false
  }
}

const selectRoom = (room) => {
  if (selectedRoom.value?.id === room?.id) {
    selectedRoom.value = null
    return
  }
  selectedRoom.value = room
}

const formatPrice = (price) => {
  if (price == null) return '-'
  const parsed = Number(price)
  if (!Number.isFinite(parsed)) return '-'
  return parsed.toLocaleString()
}

const formatDateParam = (date) => {
  if (!date) return null
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getSnsType = (line, url) => {
  const lower = line.toLowerCase()
  const target = `${lower} ${String(url).toLowerCase()}`
  if (target.includes('instagram') || target.includes('insta') || line.includes('인스타')) return 'instagram'
  if (target.includes('blog') || line.includes('블로그') || target.includes('naver')) return 'blog'
  if (target.includes('youtube') || line.includes('유튜브')) return 'youtube'
  if (target.includes('facebook') || line.includes('페이스북')) return 'facebook'
  return 'link'
}

const getSnsLabel = (type, url) => {
  if (type === 'instagram') return 'Instagram'
  if (type === 'blog') return 'Blog'
  if (type === 'youtube') return 'YouTube'
  if (type === 'facebook') return 'Facebook'
  try {
    const host = new URL(url).hostname.replace(/^www\./, '')
    return host || '링크'
  } catch (error) {
    return '링크'
  }
}



const buildSnsLinks = (sns) => {
  if (!sns) return []
  const lines = String(sns)
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean)

  const results = []
  const seen = new Set()

  lines.forEach((line) => {
    const match = line.match(/https?:\/\/[^\s]+/i)
    if (!match) return
    const url = match[0]
    if (seen.has(url)) return
    seen.add(url)
    const type = getSnsType(line, url)
    results.push({ url, type, label: getSnsLabel(type, url) })
  })

  return results
}

const snsLinks = computed(() => buildSnsLinks(guesthouse.value.sns))

const themeIconMap = computed(() => {
  const map = new Map()
  themeCatalog.value.forEach((theme) => {
    const name = theme?.themeName ? String(theme.themeName).trim() : ''
    if (!name) return
    map.set(name, theme.themeImageUrl || '')
  })
  return map
})

const themeTags = computed(() => {
  const themes = Array.isArray(guesthouse.value.themes) ? guesthouse.value.themes : []
  const map = themeIconMap.value
  return themes
      .map((theme) => {
        const name = theme == null ? '' : String(theme).trim()
        if (!name) return null
        return { name, imageUrl: map.get(name) || '' }
      })
      .filter(Boolean)
})

const increaseGuest = () => {
  searchStore.increaseGuest()
}

const decreaseGuest = () => {
  searchStore.decreaseGuest()
}

// Navigate to booking with all data
const goToBooking = () => {
  if (!canBook.value) return

  const accommodationsId = guesthouse.value.id ?? getAccommodationId()
  if (!accommodationsId) return

  if (!isAuthenticated()) {
    const shouldLogin = confirm('로그인 후 예약 가능한 서비스입니다.\n로그인 페이지로 이동할까요?')
    if (shouldLogin) {
      router.push({ path: '/login', query: { redirect: route.fullPath } })
    }
    return
  }

  // Format dates for display
  let dateDisplay = '날짜를 선택하세요'
  if (searchStore.startDate && searchStore.endDate) {
    const formatDate = (date) => {
      const year = date.getFullYear()
      const month = date.getMonth() + 1
      const day = date.getDate()
      return `${year}년 ${month}월 ${day}일`
    }
    dateDisplay = `${formatDate(searchStore.startDate)} ~ ${formatDate(searchStore.endDate)}`
  }

  const guestCount = searchStore.guestCount || 1

  router.push({
    path: `/booking/${accommodationsId}`,
    query: {
      roomId: selectedRoom.value.id,
      guestCount,
      checkin: formatDateParam(searchStore.startDate),
      checkout: formatDateParam(searchStore.endDate)
    }
  })
}

const openCalendarFromHint = (event) => {
  event.stopPropagation()
  if (datePickerRef.value?.scrollIntoView) {
    datePickerRef.value.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
  calendarStore.openCalendar('room-detail')
}

const handleClickOutside = (event) => {
  if (event.target.closest('.date-picker-wrapper')) return
  if (event.target.closest('.booking-hint')) return
  calendarStore.closeCalendar('room-detail')
}

// Top Button Logic
const showTopBtn = ref(false)

const handleScroll = () => {
  showTopBtn.value = window.scrollY > 300
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

const handleDownloadCoupon = async (coupon) => {
  const cId = String(coupon.couponId)
  if (downloadedCouponIds.value.has(cId)) return;
  if (!confirm(`${coupon.name}\n\n이 쿠폰을 다운로드 하시겠습니까?`)) return

  try {
    await issueCoupon(coupon.couponId)
    downloadedCouponIds.value.add(cId)
    alert('쿠폰이 발급되었습니다. 마이페이지에서 확인하세요!')
  } catch (error) {
    if (error.message.includes('이미 발급')) {
      downloadedCouponIds.value.add(cId)
      alert('이미 발급받은 쿠폰입니다. 마이페이지에서 확인하세요!')
      // 내 쿠폰 목록 조회 (모든 상태: ISSUED, USED, EXPIRED 등)
      try {
        const myCoupons = await getMyCoupons('ALL')
        // 내 쿠폰 중, 현재 다운로드 가능한 쿠폰과 ID가 같은 것들을 찾아 Set에 추가
        myCoupons.forEach(userCoupon => {
          // UserCouponResponseDto는 flatten된 구조 (couponId)
          // Type mismatch 방지를 위해 String으로 변환하여 저장
          if (userCoupon.couponId) {
            downloadedCouponIds.value.add(String(userCoupon.couponId))
          }
        })
      } catch (e) {
        console.warn('내 쿠폰 목록 조회 실패 (비로그인 상태일 수 있음):', e)
      }
    } else {
      console.error('쿠폰 발급 실패:', error)
      alert('쿠폰 발급에 실패했습니다. 다시 시도해 주세요.')
    }
  }
}


const formatDate = (dateStr) => {
  if(!dateStr) return '';
  const date = new Date(dateStr);
  return `${date.getFullYear()}.${date.getMonth()+1}.${date.getDate()}`;
}

onMounted(() => {
  loadThemeCatalog()
  loadAccommodation()
  if (hasFilterQuery(route.query)) {
    applyRouteFilters(route.query)
  }
})
watch(() => route.params.id, loadAccommodation)
watch(
    () => route.query,
    (query) => {
      if (hasFilterQuery(query)) {
        applyRouteFilters(query)
      }
    }
)
// watch logic for calendar removed
watch(
    () => [route.params.id, searchStore.startDate, searchStore.endDate, searchStore.guestCount],
    () => {
      loadAvailability()
    },
    { immediate: true }
)
watch(filteredRooms, (rooms) => {
  if (!selectedRoom.value) return
  const stillAvailable = rooms.some((room) => room.id === selectedRoom.value.id)
  if (!stillAvailable) {
    selectedRoom.value = null
  }
})
</script>

<template>
  <div class="room-detail container">
    <!-- Header with Back Button -->
    <div class="detail-header">
      <button class="back-btn" @click="goBack">← 뒤로가기</button>
    </div>

    <!-- 스켈레톤 로딩 UI -->
    <DetailSkeleton v-if="isDataLoading" />

    <!-- 실제 컨텐츠 -->
    <template v-else>
      <!-- Image Grid -->
      <ImageGallery :images="guesthouse.images" :name="guesthouse.name" />

      <!-- Title & Info -->
      <section class="section info-section">
        <h1>{{ guesthouse.name }}</h1>
        <div class="meta">
          <span class="rating">★ {{ guesthouse.rating }} (리뷰 {{ guesthouse.reviewCount }}개)</span>
          <span class="location">{{ guesthouse.address }}</span>
        </div>
        <div class="description-row">
          <h2 class="info-title">소개</h2>

          <AiSummarySection :accommodation-id="guesthouse.id" />

          <p class="description" v-if="guesthouse.description">
            {{ guesthouse.description }}
          </p>
        </div>
        <div class="transport-info" v-if="guesthouse.transportInfo">
          <h2 class="info-title">교통 정보</h2>
          <p>{{ guesthouse.transportInfo }}</p>
        </div>
        <div class="contact-info" v-if="guesthouse.phone">
          <h2 class="info-title">
            연락처
          </h2>
          <p>{{ guesthouse.phone }}</p>
        </div>
      </section>

      <section class="section amenity-section">
        <h2 class="info-title">편의시설</h2>
        <div class="tag-list amenity-list">
        <span
            v-for="(amenity, idx) in guesthouse.amenities"
            :key="amenity.name || idx"
            class="tag amenity-tag"
        >
          <span v-if="amenity.icon" class="amenity-icon" v-html="amenity.icon"></span>
          <span class="amenity-text">{{ amenity.name }}</span>
        </span>
          <span v-if="!guesthouse.amenities.length" class="tag empty">등록된 정보 없음</span>
        </div>
        <h2 class="info-title">테마</h2>
        <div class="tag-list theme-tag-list">
        <span v-for="theme in themeTags" :key="theme.name" class="tag theme-tag">
          <img v-if="theme.imageUrl" :src="theme.imageUrl" :alt="theme.name" class="theme-tag__icon"/>
          <span v-else class="theme-tag__icon theme-tag__icon--empty"></span>
          <span class="theme-tag__label">{{ theme.name }}</span>
        </span>
          <span v-if="!themeTags.length" class="tag empty">등록된 정보 없음</span>
        </div>
      </section>

      <hr/>

      <section class="section extra-info-section">
        <h2>추가 정보</h2>
        <dl class="info-list">
          <div class="info-row">
            <dt>체크인</dt>
            <dd>{{ guesthouse.checkInTime || '정보 없음' }}</dd>
          </div>
          <div class="info-row">
            <dt>체크아웃</dt>
            <dd>{{ guesthouse.checkOutTime || '정보 없음' }}</dd>
          </div>
          <div class="info-row">
            <dt>주차</dt>
            <dd>{{ guesthouse.parkingInfo || '정보 없음' }}</dd>
          </div>
          <div class="info-row">
            <dt>SNS</dt>
            <dd>
              <template v-if="snsLinks.length">
                <a
                    v-for="link in snsLinks"
                    :key="link.url"
                    :href="link.url"
                    class="sns-link"
                    target="_blank"
                    rel="noopener noreferrer"
                    :aria-label="link.label"
                >
                  <svg v-if="link.type === 'instagram'" viewBox="0 0 24 24" class="sns-icon" aria-hidden="true">
                    <rect x="3" y="3" width="18" height="18" rx="5" ry="5" fill="none" stroke="currentColor"
                          stroke-width="2"/>
                    <circle cx="12" cy="12" r="3.5" fill="none" stroke="currentColor" stroke-width="2"/>
                    <circle cx="17.5" cy="6.5" r="1" fill="currentColor"/>
                  </svg>
                  <svg v-else-if="link.type === 'youtube'" viewBox="0 0 24 24" class="sns-icon" aria-hidden="true">
                    <rect x="3" y="6" width="18" height="12" rx="3" ry="3" fill="none" stroke="currentColor"
                          stroke-width="2"/>
                    <path d="M10 9l5 3-5 3z" fill="currentColor"/>
                  </svg>
                  <svg v-else-if="link.type === 'blog'" viewBox="0 0 24 24" class="sns-icon" aria-hidden="true">
                    <path d="M6 3h9l5 5v13a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z" fill="none"
                          stroke="currentColor" stroke-width="2"/>
                    <path d="M14 3v5h5" fill="none" stroke="currentColor" stroke-width="2"/>
                    <path d="M8 13h8M8 17h6" fill="none" stroke="currentColor" stroke-width="2"/>
                  </svg>
                  <svg v-else-if="link.type === 'facebook'" viewBox="0 0 24 24" class="sns-icon" aria-hidden="true">
                    <path d="M15 8h3V5h-3c-2 0-4 2-4 4v3H8v3h3v6h3v-6h3l1-3h-4V9c0-.6.4-1 1-1z" fill="currentColor"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" class="sns-icon" aria-hidden="true">
                    <path d="M10 13a5 5 0 0 0 7.07 0l2.83-2.83a5 5 0 0 0-7.07-7.07L11 4" fill="none"
                          stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M14 11a5 5 0 0 0-7.07 0L4.1 13.83a5 5 0 0 0 7.07 7.07L13 20" fill="none"
                          stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  <span class="sns-text">
                  {{
                      link.type === 'blog' && link.label.toLowerCase().includes('naver') ? '네이버 블로그 바로가기' : `${link.label} 바로가기`
                    }}
                </span>
                </a>
              </template>
              <span v-else>{{ guesthouse.sns || '정보 없음' }}</span>
            </dd>
          </div>
        </dl>
      </section>

      <hr/>

      <!-- Room Selection -->
      <section class="section room-selection">
        <div class="room-selection-header">
          <h2>객실 선택</h2>
          <!-- Coupon Button (Moved Here) -->
          <button
              v-if="availableCoupons.length > 0"
              type="button"
              class="coupon-header-btn"
              @click="isCouponModalOpen = true"
          >
            쿠폰함
          </button>
        </div>

        <!-- Date & Guest Picker Mock -->
        <div class="picker-box">
          <div class="picker-row">
            <div ref="datePickerRef" class="picker-field date-picker-wrapper" @click.stop>
              <label>체크인 / 체크아웃</label>
              <button
                  type="button"
                  class="date-display"
                  :aria-expanded="isCalendarOpen"
                  @click="toggleCalendar"
              >
                {{ searchStore.checkInOutText }}
              </button>

              <RoomDetailCalendar/>
            </div>
            <div class="picker-field">
              <label>투숙 인원</label>
              <div class="guest-control">
                <button @click="decreaseGuest" :disabled="searchStore.guestCount <= 1">-</button>
                <span>게스트 {{ searchStore.guestCount || 1 }}명</span>
                <button @click="increaseGuest">+</button>
              </div>
            </div>

            <!-- Square Coupon Button Removed -->

          </div>
        </div>

        <!-- Room List -->
        <RoomListSection
            :rooms="filteredRooms"
            :selected-room="selectedRoom"
            @update:selected-room="selectedRoom = $event"
        />
      </section>

      <hr/>

      <!-- Reviews -->
      <ReviewSection :reviews="guesthouse.reviews" :name="guesthouse.name"/>

      <!-- Map -->
      <MapSection
          :latitude="guesthouse.latitude"
          :longitude="guesthouse.longitude"
          :address="guesthouse.address"
          :name="guesthouse.name"
          :transport-info="guesthouse.transportInfo"
      />

      <!-- Rules -->
      <section class="section rules-section">
        <div class="rule-box">
          <h3>환불 규정</h3>
          <ul>
            <li>체크인 7일 전 취소: 결제 금액 100% 환불</li>
            <li>체크인 5~6일 전 취소: 결제 금액의 90% 환불</li>
            <li>체크인 3~4일 전 취소: 결제 금액의 70% 환불</li>
            <li>체크인 1~2일 전 취소: 결제 금액의 50% 환불</li>
            <li>체크인 당일 취소 또는 노쇼(No-show): 환불 불가</li>
          </ul>
        </div>
      </section>

      <!-- Floating Bottom Bar -->
      <div class="bottom-bar">
        <div class="selection-summary">
          <span v-if="selectedRoom">선택한 객실: {{ selectedRoom.name }}</span>
          <span v-else>객실을 선택해주세요</span>
          <div class="total-price" v-if="selectedRoom">
          <span class="price-detail" v-if="hasDateRange">
            ₩{{ formatPrice(selectedRoom.price) }} × {{ stayNights }}박<span
              v-if="['GUESTHOUSE', '게스트하우스'].includes((guesthouse.category || '').toUpperCase())"> × {{
              searchStore.guestCount
            }}명</span> =
          </span>
            <span class="price-amount">₩{{ formatPrice(hasDateRange ? totalPrice : selectedRoom.price) }}</span>
            <span class="price-nights" v-if="!hasDateRange"> / 1박</span>
          </div>
        </div>
        <button class="book-btn" :disabled="!canBook" @click="goToBooking">예약하기</button>
      </div>
      <div v-if="selectedRoom && !canBook" class="booking-hint">
        날짜를 선택해주세요.
        <button type="button" class="booking-hint-btn" @click="openCalendarFromHint">
          날짜 선택하기
        </button>
      </div>


      <!-- Coupon Modal -->
      <div v-if="isCouponModalOpen" class="modal-overlay" @click.self="isCouponModalOpen = false">
        <div class="modal-content coupon-modal">
          <h3>사용 가능한 쿠폰</h3>
          <p class="modal-desc">이 숙소에서 사용할 수 있는 쿠폰을 다운로드하세요.</p>
          <div class="coupon-list-container">
            <ul class="coupon-list" v-if="availableCoupons.length > 0">
              <li v-for="coupon in availableCoupons" :key="coupon.couponId" class="coupon-item">
                <div class="coupon-info">
                  <div class="coupon-name">{{ coupon.name }}</div>
                  <div class="coupon-desc">{{ coupon.description }}</div>
                  <div class="coupon-meta">
                    <span>{{
                        coupon.discountType === 'PERCENT' ? coupon.discountValue + '%' : coupon.discountValue.toLocaleString() + '원'
                      }} 할인</span>
                    <span v-if="coupon.minPrice">({{ coupon.minPrice.toLocaleString() }}원 이상)</span>
                  </div>
                </div>

                <button
                    class="download-btn"
                    :class="{ 'downloaded': downloadedCouponIds.has(String(coupon.couponId)) }"
                    :disabled="downloadedCouponIds.has(String(coupon.couponId))"
                    @click="handleDownloadCoupon(coupon)"
                >
                  {{ downloadedCouponIds.has(String(coupon.couponId)) ? '발급완료' : '다운로드' }}
                </button>
              </li>
            </ul>
            <p v-else class="no-coupon">다운로드 가능한 쿠폰이 없습니다.</p>
          </div>
          <button class="close-modal-btn" @click="isCouponModalOpen = false">닫기</button>
        </div>
      </div>

      <!-- Unavailable Modal and Room Image Modal moved to properties/RoomListSection -->

      <!-- Top Button -->
      <button
          v-show="showTopBtn"
          class="top-btn"
          @click="scrollToTop"
          aria-label="맨 위로"
      >
        ↑
      </button>
    </template>
  </div>
</template>

<style scoped>
.room-detail {
  padding-bottom: 2rem;
  max-width: 1200px;
}

.detail-header {
  padding: 1rem 0;
  margin-bottom: 0.5rem;
}

.back-btn {
  background: none;
  border: none;
  font-size: 1rem;
  color: #333;
  cursor: pointer;
  padding: 0.5rem 0;
}

.back-btn:hover {
  color: var(--primary);
}

/* Sections */
.section {
  padding: 1.5rem 0;
}

.info-section {
  padding-top: 0;
  padding-bottom: 0.75rem;
}

hr {
  border: 0;
  border-top: 1px solid #eee;
  margin: 0;
}

h1 {
  font-size: 1.8rem;
  margin-bottom: 0.5rem;
}

h2 {
  font-size: 1.4rem;
  margin-bottom: 1rem;
}

h3 {
  font-size: 1.1rem;
  margin-bottom: 0.5rem;
}

/* Info */
.meta {
  color: var(--text-sub);
  margin-bottom: 1rem;
  font-size: 0.95rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.description-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.5rem;
}

.description {
  line-height: 1.6;
  flex: 1;
  margin: 0;
  white-space: pre-line;
  width: 100%;
  box-sizing: border-box;
  max-height: calc(1.6em * 12);
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 0.6rem 0.7rem;
  background: #fff;
  border: 1px solid #d1d5db;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.08);
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0;
  color: var(--text-sub);
  font-size: 0.9rem;
}

.meta-item {
  background: #f3f4f6;
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
}

.transport-info {
  margin-top: 1rem;
}

.transport-info h3 {
  margin-bottom: 0.4rem;
}

.transport-info p {
  margin: 0;
  color: var(--text-sub);
  line-height: 1.5;
}

.info-title {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.info-icon {
  width: 18px;
  height: 18px;
  display: inline-flex;
  color: #0f4c44;
}

.info-icon svg {
  width: 18px;
  height: 18px;
  display: block;
  fill: currentColor;
}

.contact-info {
  margin-top: 1rem;
}

.contact-info h3 {
  margin-bottom: 0.4rem;
}

.contact-info p {
  margin: 0;
  color: var(--text-sub);
  line-height: 1.5;
}

/* Host */
.host-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.host-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
}

/* Amenities */
.amenity-section {
  padding-top: 0.75rem;
  padding-bottom: 0.75rem;
}

.amenity-section h2 {
  margin-bottom: 0.6rem;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.tag {
  background: #f3f4f6;
  color: var(--text-main);
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  font-size: 0.85rem;
}

.tag.empty {
  color: var(--text-sub);
}

.theme-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.theme-tag-list {
  margin-bottom: 0.5rem;
}

.theme-tag__icon {
  width: 18px;
  height: 18px;
  object-fit: contain;
  border-radius: 4px;
  flex-shrink: 0;
}

.theme-tag__icon--empty {
  background: #e5e7eb;
}

.theme-tag__label {
  line-height: 1;
}

.amenity-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.amenity-icon {
  display: inline-flex;
  width: 18px;
  height: 18px;
  color: var(--text-main);
  flex-shrink: 0;
}

.amenity-text {
  line-height: 1;
}

:deep(.amenity-icon svg) {
  width: 18px;
  height: 18px;
  display: block;
}

/* Extra info */
.extra-info-section {
  padding-top: 0.75rem;
}

.extra-info-section h2 {
  margin-bottom: 0.8rem;
}

.info-list {
  display: grid;
  gap: 0.75rem;
  margin: 0;
}

.info-row {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 0.75rem;
  align-items: start;
}

.info-row dt {
  font-weight: 600;
  color: var(--text-main);
}

.info-row dd {
  margin: 0;
  color: var(--text-sub);
  word-break: break-word;
}

.sns-link {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.75rem;
  border-radius: 999px;
  background: #f3f4f6;
  color: var(--text-main);
  text-decoration: none;
  margin-right: 0.6rem;
  transition: background 0.2s ease;
}

.sns-link:last-child {
  margin-right: 0;
}

.sns-link:hover {
  background: #e5e7eb;
}

.sns-icon {
  width: 18px;
  height: 18px;
}

.sns-text {
  font-size: 0.85rem;
  color: var(--text-main);
  white-space: nowrap;
}

/* Room Selection */
.picker-box {
  border: 1px solid #ddd;
  border-radius: var(--radius-md);
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}

.picker-row {
  display: flex;
  gap: 1rem;
}
.picker-field {
  flex: 1;
}

.picker-field label {
  display: block;
  font-size: 0.8rem;
  font-weight: bold;
  margin-bottom: 0.5rem;
}
.date-display, .guest-control {
  border: 1px solid #ddd;
  padding: 0 0.8rem;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  background: #fff;
}

.date-picker-wrapper {
  position: relative;
}

.date-display {
  width: 100%;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.guest-control button {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid #ddd;
  background: white;
}


/* Rules */
.rules-section {
  padding-top: 0;
  padding-bottom: 0;
}

.rule-box h3 {
  margin-bottom: 0.8rem;
}

.rule-box ul {
  list-style: inside disc;
  color: var(--text-sub);
  font-size: 0.9rem;
  line-height: 1.6;
  padding-left: 0.75rem;
}

/* Bottom Bar */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-top: 1px solid #ddd;
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 2rem;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
  z-index: 100;
}

.selection-summary {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.total-price {
  font-weight: bold;
  font-size: 1.2rem;
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.price-detail {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-sub);
}

.price-amount {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--text-main);
  font-family: 'NanumSquareRound', sans-serif !important;
}

.price-nights {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--text-sub);
}

.book-btn {
  background: var(--primary);
  color: #004d40;
  padding: 0.8rem 2rem;
  border-radius: 8px;
  font-weight: 800;
  font-size: 1rem;
  border: none;
  cursor: pointer;
  min-width: 140px;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: 'NanumSquareRound', sans-serif !important;
}

.book-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  opacity: 1;
}

.booking-hint {
  position: fixed;
  bottom: 84px;
  left: 50%;
  transform: translateX(-50%);
  background: #fff7ed;
  border: 1px solid #fed7aa;
  color: #9a3412;
  padding: 0.6rem 1rem;
  border-radius: 10px;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  z-index: 101;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.booking-hint-btn {
  background: var(--primary);
  color: #004d40;
  border: none;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  font-weight: 800;
  cursor: pointer;
  font-size: 0.9rem; /* Match parent's font size */
  font-family: 'NanumSquareRound', sans-serif !important;
}

@media (max-width: 768px) {
  .room-detail {
    max-width: 100%;
    padding: 0 16px;
  }

  .picker-row {
    flex-direction: column;
  }

  .calendar-container {
    flex-direction: column;
  }

  .calendar-month {
    width: 100%;
  }

  .room-card {
    flex-direction: column;
    padding: 1rem;
    gap: 0.75rem;
  }

  .room-media {
    width: 100%;
    height: 180px;
    max-width: 100%;
    flex: none;
  }

  .room-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.25rem;
    max-width: 100%;
  }

  .room-info h3 {
    margin: 0;
  }

  .room-info p {
    margin: 0;
    line-height: 1.25;
  }

  .capacity {
    padding: 1px 5px;
    margin-top: 0.15rem;
  }

  .room-action {
    width: 100%;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    margin-top: 0.15rem;
  }

  .booking-hint {
    width: calc(100% - 32px);
    left: 16px;
    transform: none;
    justify-content: space-between;
  }
}

/* Coupon Styles */
.room-selection-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.room-selection-header h2 {
  margin-bottom: 0;
}

.coupon-header-btn {
  background: var(--primary);
  color: #004d40;
  border: none;
  padding: 0.4rem 0.8rem;
  border-radius: 8px;
  font-weight: 800;
  font-size: 0.95rem;
  cursor: pointer;
  transition: opacity 0.2s;
  font-family: 'NanumSquareRound', sans-serif !important;
}

.coupon-header-btn:hover {
  opacity: 0.9;
}

.download-btn.downloaded {
  background: #ccc;
  color: #666;
  cursor: not-allowed;
  filter: none;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content.coupon-modal {
  background: white;
  padding: 2rem;
  border-radius: 16px;
  width: 90%;
  max-width: 450px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.modal-desc {
  color: #666;
  font-size: 0.95rem;
  margin-bottom: 1.5rem;
}

.coupon-list-container {
  overflow-y: auto;
  margin-bottom: 1rem;
  flex: 1;
}

.coupon-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.coupon-item {
  border: 1px solid #ddd;
  border-radius: 12px;
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  background: #fdfdfd;
}

.coupon-info {
  flex: 1;
}

.coupon-name {
  font-weight: bold;
  font-size: 1.05rem;
  margin-bottom: 0.25rem;
}

.coupon-desc {
  font-size: 0.85rem;
  color: #666;
  margin-bottom: 0.5rem;
}

.coupon-meta {
  font-size: 0.85rem;
  color: #ff5722;
  font-weight: 600;
}

.download-btn {
  background: var(--primary);
  color: #004d40;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
  white-space: nowrap;
}

.download-btn:hover {
  filter: brightness(0.95);
}

.close-modal-btn {
  width: 100%;
  padding: 0.8rem;
  border: 1px solid #ddd;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  color: #333;
}

.close-modal-btn:hover {
  background: #f5f5f5;
}


/* Top Button Styles */
.top-btn {
  position: fixed;
  bottom: 100px; /* booking-hint 위쪽, 혹은 바닥 여유 */
  right: 40px;
  width: 50px;
  height: 50px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  color: var(--text-main);
  font-size: 1.5rem;
  font-weight: bold;
  cursor: pointer;
  z-index: 99;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  opacity: 0.9;
}

.top-btn:hover {
  background: var(--primary);
  color: #004d40;
  border-color: var(--primary);
  transform: translateY(-3px);
  opacity: 1;
}

@media (max-width: 768px) {
  .top-btn {
    display: none; /* 모바일에서는 숨김 */
  }
}
</style>
