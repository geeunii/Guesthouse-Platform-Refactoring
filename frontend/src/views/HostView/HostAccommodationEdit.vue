<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAccessToken } from '../../api/authClient'
import { requestAccommodationAiSuggestion } from '@/api/ai'
import { resizeImage } from '@/utils/imageUtils'

const route = useRoute()
const router = useRouter()
const accommodationId = route.params.id

// API Base URL (프록시 사용: /api -> /api)
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
// 이미지 URL용 서버 기본 경로
const SERVER_BASE_URL = import.meta.env.VITE_SERVER_BASE_URL || ''

// 이미지 URL을 전체 경로로 변환
const getFullImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('blob:') || url.startsWith('http')) return url
  return `${SERVER_BASE_URL}${url}`
}

// 로딩 상태
const isLoading = ref(true)
const loadError = ref('')

// 예약 정보 상태
const hasReservations = ref(false)
const isAiSuggesting = ref(false)

// 모달 상태
const showModal = ref(false)
const modalMessage = ref('')
const updateSuccess = ref(false)

const openModal = (message) => {
  modalMessage.value = message
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  if (updateSuccess.value) {
    router.push('/host/accommodation')
  }
}

// 카카오맵 관련
const mapContainer = ref(null)
const map = ref(null)
const marker = ref(null)
const geocoder = ref(null)

// 숙소 유형 매핑
const accommodationCategoryMap = {
  'GUESTHOUSE': '게스트하우스',
  'PENSION': '펜션',
  'HOTEL': '호텔',
  'MOTEL': '모텔',
  'RESORT': '리조트',
  'HANOK': '한옥',
  'CAMPING': '캠핑/글램핑'
}

// 역매핑 (한글 -> 영문)
const accommodationTypeReverseMap = {
  '게스트하우스': 'GUESTHOUSE',
  '펜션': 'PENSION',
  '호텔': 'HOTEL',
  '모텔': 'MOTEL',
  '리조트': 'RESORT',
  '한옥': 'HANOK',
  '캠핑/글램핑': 'CAMPING'
}

// 편의시설 옵션 (전체 목록)
const amenityOptions = [
  { id: 1, label: '무선 인터넷' },
  { id: 2, label: '에어컨' },
  { id: 3, label: '난방' },
  { id: 4, label: 'TV' }
]

// 테마 옵션 (API에서 동적으로 로드)
const themeOptions = ref({})
const themeList = ref([]) // 전체 테마 리스트 (id, themeName 매핑용)

// 카테고리 라벨 매핑
const categoryLabels = {
  'MEETING': '만남/소셜',
  'PERSONA': '페르소나/성향',
  'FACILITY': '시설/편의',
  'FOOD': '식사',
  'PLAY': '놀거리'
}

// 테마 목록 로드
const loadThemes = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/themes`)
    if (!response.ok) throw new Error('테마 로드 실패')
    const data = await response.json()
    themeList.value = data

    // 카테고리별로 그룹화
    const grouped = {}
    data.forEach(theme => {
      const category = theme.themeCategory || 'ETC'
      if (!grouped[category]) {
        grouped[category] = {
          label: categoryLabels[category] || category,
          items: []
        }
      }
      grouped[category].items.push(theme.themeName)
    })
    themeOptions.value = grouped
  } catch (error) {
    console.error('테마 로드 실패:', error)
    openModal('테마 목록을 불러오는 데 실패했습니다. 잠시 후 다시 시도해주세요.')
  }
}

// 테마 ID 매핑 (themeName -> themeId) - API에서 동적으로 생성
const getThemeId = (themeName) => {
  const theme = themeList.value.find(t => t.themeName === themeName)
  return theme ? theme.id : undefined
}

// 테마 ID에서 이름 가져오기
const getThemeNameById = (id) => {
  const theme = themeList.value.find(t => t.id === id)
  return theme ? theme.themeName : undefined
}

// 은행 목록
const bankList = ['국민은행', '신한은행', '우리은행', '하나은행', '농협', '카카오뱅크', '토스뱅크', '기업은행']

// Form data
const form = ref({
  // 기본정보 (Readonly)
  name: '',
  type: '',
  phone: '',
  email: '',
  businessRegistrationNumber: '',
  // 위치정보 (Readonly)
  city: '',
  district: '',
  township: '',
  address: '',
  latitude: null,
  longitude: null,
  // 수정 가능 필드
  description: '',
  shortDescription: '',
  transportInfo: '',
  checkInTime: '',
  checkOutTime: '',
  parkingInfo: '',
  sns: '',
  isActive: true,
  approvalStatus: 'PENDING', // 승인 상태: PENDING, APPROVED, REJECTED
  houseRules: '', // DB 스키마에 없으면 생략 가능하지만 UI엔 있었음
  // Readonly Lists
  amenities: [], // IDs
  themes: [], // Strings (Names) or IDs depending on logic. Register uses Strings for themes.
  // Images
  bannerImage: null,
  detailImages: [],
  // Bank (Readonly for now as per "accommodation info readonly" request, though usually bank is crucial)
  bankName: '',
  accountHolder: '',
  accountNumber: ''
})

// 객실 데이터
const rooms = ref([])

// 이미지 관련
// 이미지 관리 - 통합 State
// { id: number | string, url: string, file: File | null, isNew: boolean }
const displayImages = ref([])

// 배너 이미지 관련
const bannerFile = ref(null)
const bannerPreview = ref('')

// 체크인/체크아웃 시간 선택 관련
const checkInHour = ref('')
const checkInMinute = ref('')
const checkOutHour = ref('')
const checkOutMinute = ref('')

// 시간/분 옵션
const hourOptions = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'))
const minuteOptions = ['00', '30']

// 체크인 시간 watch - 시/분 변경 시 form.checkInTime 업데이트
watch([checkInHour, checkInMinute], ([hour, minute]) => {
  if (hour && minute) {
    form.value.checkInTime = `${hour}:${minute}`
  }
})

// 체크아웃 시간 watch - 시/분 변경 시 form.checkOutTime 업데이트
watch([checkOutHour, checkOutMinute], ([hour, minute]) => {
  if (hour && minute) {
    form.value.checkOutTime = `${hour}:${minute}`
  }
})

// 편의시설 토글
const toggleAmenity = (id) => {
  const index = form.value.amenities.indexOf(id)
  if (index === -1) {
    form.value.amenities.push(id)
  } else {
    form.value.amenities.splice(index, 1)
  }
}

// 테마 토글
const toggleTheme = (themeName) => {
  const index = form.value.themes.indexOf(themeName)
  if (index === -1) {
    if (form.value.themes.length >= 6) {
       openModal('테마는 최대 6개까지 선택 가능합니다.')
       return
    }
    form.value.themes.push(themeName)
  } else {
    form.value.themes.splice(index, 1)
  }
}

// 편의시설 체크 여부 확인
const isAmenityChecked = (id) => {
  return form.value.amenities.includes(id)
}

// 테마 체크 여부 확인
const isThemeChecked = (themeName) => {
  return form.value.themes.includes(themeName)
}

// 예약 정보 확인 (숙소 전체)
const checkHasReservations = async () => {
  try {
    const token = getAccessToken()
    const response = await fetch(`${API_BASE_URL}/reservations/accommodation/${accommodationId}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    if (response.ok) {
      const reservations = await response.json()
      // 예약 상태가 확정(2)인 경우만 체크 (3은 취소)
      const activeReservations = reservations.filter(r => r.reservationStatus === 2)
      hasReservations.value = activeReservations.length > 0
      return hasReservations.value
    }
    return false
  } catch (error) {
    console.error('예약 확인 오류:', error)
    return false
  }
}

// 특정 객실의 예약 정보 확인
const checkRoomHasReservations = async (roomId) => {
  try {
    const token = getAccessToken()
    const response = await fetch(`${API_BASE_URL}/reservations/room/${roomId}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    if (response.ok) {
      const reservations = await response.json()
      // 예약 상태가 확정(2)인 경우만 체크 (3은 취소)
      const activeReservations = reservations.filter(r => r.reservationStatus === 2)
      return activeReservations.length > 0
    }
    return false
  } catch (error) {
    console.error('객실 예약 확인 오류:', error)
    return false
  }
}

// 데이터 로드
const loadAccommodation = async () => {
  isLoading.value = true
  loadError.value = ''

  try {
    const token = getAccessToken()
    const response = await fetch(`${API_BASE_URL}/accommodations/${accommodationId}`, {
         headers: {
            'Authorization': `Bearer ${token}`
         }
    })
    if (!response.ok) throw new Error('숙소 정보를 불러올 수 없습니다.')

    const data = await response.json()

    // 데이터 매핑
    form.value = {
      name: data.accommodationsName,
      type: accommodationCategoryMap[data.accommodationsCategory] || data.accommodationsCategory,
      phone: data.phone,
      email: data.email || '', // API response might not have email on root? Checked DTO, it does not have email. Using placeholder if missing.
      businessRegistrationNumber: data.businessRegistrationNumber,
      
      city: data.city,
      district: data.district,
      township: data.township,
      address: data.addressDetail,
      latitude: data.latitude,
      longitude: data.longitude,
      
      description: data.accommodationsDescription,
      shortDescription: data.shortDescription,
      transportInfo: data.transportInfo,
      parkingInfo: data.parkingInfo,
      sns: data.sns,
      checkInTime: data.checkInTime || '',
      checkOutTime: data.checkOutTime || '',
      isActive: data.accommodationStatus === 1,
      approvalStatus: data.approvalStatus || 'PENDING', // 승인 상태

      amenities: data.amenityIds || [], // IDs
      themes: data.themeIds ? data.themeIds.map(id => getThemeNameById(id)).filter(Boolean) : [],
      
      bankName: data.bankName,
      accountNumber: data.accountNumber,
      accountHolder: data.accountHolder,
      
      // Images (Just URLs for display)
      bannerImage: null, // Logic to extract banner from images list
      detailImages: []
    }

    // Images Mapping
    if (data.images) {
        const banner = data.images.find(img => img.imageType === 'banner')
        if (banner) {
             form.value.bannerImage = getFullImageUrl(banner.imageUrl)
        }

        const details = data.images
            .filter(img => img.imageType === 'detail')
            .sort((a, b) => a.sortOrder - b.sortOrder)

        // 기존 이미지 로드
        displayImages.value = details.map((img, idx) => ({
             id: img.id || idx,
             url: getFullImageUrl(img.imageUrl),
             file: null,
             isNew: false
        }))
    }

    // 체크인/체크아웃 시간 파싱 (HH:mm 형식)
    if (data.checkInTime) {
        const [hour, minute] = data.checkInTime.split(':')
        checkInHour.value = hour ? hour.padStart(2, '0') : ''
        checkInMinute.value = minute ? minute.padStart(2, '0') : '00'
        // Ensure form model is also set
        form.value.checkInTime = data.checkInTime
    }
    if (data.checkOutTime) {
        const [hour, minute] = data.checkOutTime.split(':')
        checkOutHour.value = hour ? hour.padStart(2, '0') : ''
        checkOutMinute.value = minute ? minute.padStart(2, '0') : '00'
        form.value.checkOutTime = data.checkOutTime
    }

    // 객실 매핑
    if (data.rooms) {
      rooms.value = data.rooms.map(room => ({
        id: room.roomId, // Keep ID for updates
        name: room.roomName,
        weekdayPrice: room.price,
        weekendPrice: room.weekendPrice,
        minGuests: room.minGuests,
        maxGuests: room.maxGuests,
        bedCount: room.bedCount,
        bathroomCount: room.bathroomCount,
        description: room.roomDescription,
        mainImageUrl: room.mainImageUrl,
        amenities: room.amenities || [], // String list
        isActive: room.roomStatus === 1
      }))
    }

  } catch (error) {
    console.error('Load Error:', error)
    loadError.value = error.message
  } finally {
    isLoading.value = false
    // 로딩 완료 후 DOM 렌더링을 기다린 다음 지도 초기화
    await nextTick()
    await waitForKakao()
    setTimeout(() => {
      initMap()
    }, 200)
  }
}

// 카카오맵
const initMap = () => {
    if (!window.kakao || !window.kakao.maps || !mapContainer.value) return

    window.kakao.maps.load(() => {
    geocoder.value = new window.kakao.maps.services.Geocoder()

    // latitude/longitude가 없으면 주소로 좌표 검색 (Fallback)
    const lat = parseFloat(form.value.latitude)
    const lng = parseFloat(form.value.longitude)

    if (isNaN(lat) || isNaN(lng)) {
        const fullAddress = `${form.value.city} ${form.value.district} ${form.value.township} ${form.value.address}`.trim()
        console.warn('Invalid coordinates, attempting fallback with address:', fullAddress)

        if (fullAddress) {
            geocoder.value.addressSearch(fullAddress, (result, status) => {
                 if (status === window.kakao.maps.services.Status.OK) {
                    const y = result[0].y
                    const x = result[0].x

                    form.value.latitude = y
                    form.value.longitude = x

                    const coords = new window.kakao.maps.LatLng(y, x)
                    const options = { center: coords, level: 3 }
                    map.value = new window.kakao.maps.Map(mapContainer.value, options)
                    marker.value = new window.kakao.maps.Marker({
                        position: coords,
                        map: map.value
                    })
                    // 지도 relayout (크기 재계산)
                    setTimeout(() => {
                        if (map.value) map.value.relayout()
                    }, 100)
                 } else {
                    console.error('Geocoding failed for address:', form.value.address)
                 }
            })
        }
        return
    }

    const coords = new window.kakao.maps.LatLng(lat, lng)
    const options = { center: coords, level: 3 }

    map.value = new window.kakao.maps.Map(mapContainer.value, options)
    marker.value = new window.kakao.maps.Marker({
      position: coords,
      map: map.value
    })
    // 지도 relayout (크기 재계산)
    setTimeout(() => {
        if (map.value) map.value.relayout()
    }, 100)
  })
}

// ========== 유효성 검사 (수정 가능 필드만) ==========
const errors = ref({})

const validateForm = () => {
    errors.value = {}
    let isValid = true
    const errorMessages = []

    if (!form.value.description?.trim()) {
        errors.value.description = '숙소 소개를 입력해주세요.'
        errorMessages.push('숙소 소개')
        isValid = false
    }
    
    if (!form.value.checkInTime) {
        errors.value.checkInTime = '체크인 시간을 입력해주세요.'
        errorMessages.push('체크인 시간')
        isValid = false
    }

    if (!form.value.checkOutTime) {
        errors.value.checkOutTime = '체크아웃 시간을 입력해주세요.'
        errorMessages.push('체크아웃 시간')
        isValid = false
    }

    // 테마 검사
    if (!form.value.themes || form.value.themes.length === 0) {
        errorMessages.push('테마 (최소 1개 선택)')
        isValid = false
    }

    // Room Validation at Submit
    // 유효성 검사 로직 보완
    if (rooms.value.length === 0) {
        errors.value.rooms = '등록된 객실이 없습니다.'
        // errorMessages.push('객실') // 중복 경고 방지
        isValid = false
    }

    return { isValid, errorMessages }
}

// Kakao Map Waiter
const waitForKakao = () => {
    return new Promise((resolve) => {
        if (window.kakao && window.kakao.maps) {
            resolve()
        } else {
            const checkKakao = setInterval(() => {
                if (window.kakao && window.kakao.maps) {
                    clearInterval(checkKakao)
                    resolve()
                }
            }, 100)
        }
    })
}

// 주소 검색 (Daum Postcode)
const openPostcode = () => {
  new window.daum.Postcode({
    oncomplete: function(data) {
      // 주소 연동
      form.value.city = data.sido
      form.value.district = data.sigungu
      form.value.township = data.bname
      form.value.address = data.buildingName ? `${data.address} (${data.buildingName})` : data.address
      
      // 좌표 변환
      if (geocoder.value) {
         geocoder.value.addressSearch(data.address, (result, status) => {
            if (status === window.kakao.maps.services.Status.OK) {
               form.value.latitude = result[0].y
               form.value.longitude = result[0].x
               
               const coords = new window.kakao.maps.LatLng(result[0].y, result[0].x)
               map.value.setCenter(coords)
               marker.value.setPosition(coords)
            }
         })
      }
    }
  }).open()
}

const handleUpdate = async () => {
    // roomForm이 열려있으면 경고
    if (showRoomForm.value) {
        openModal('작성 중인 객실 정보를 먼저 저장(등록/수정)해주세요.')
        return
    }

    const { isValid, errorMessages } = validateForm()
    if (!isValid) {
        openModal(`다음 항목을 확인해주세요:\n${errorMessages.join(', ')}`)
        return
    }

    // 객실 데이터 유효성 검사 (전체)
    for (const room of rooms.value) {
        if (!room.name || !room.name.trim()) {
             openModal('객실명이 비어있는 객실이 있습니다.')
             return
        }
        if (!room.weekdayPrice || parseInt(room.weekdayPrice) < 0) {
             openModal(`[${room.name}] 객실의 주중 요금을 확인해주세요.`)
             return
        }
        // 주말 요금은 0원일 수도 있다고 가정? 보통은 아니지만 0 이상 체크
        if (room.weekendPrice === undefined || room.weekendPrice === '' || parseInt(room.weekendPrice) < 0) {
             openModal(`[${room.name}] 객실의 주말 요금을 확인해주세요.`)
             return
        }
        if (!room.minGuests || parseInt(room.minGuests) < 1) {
             openModal(`[${room.name}] 객실의 최소 인원은 1명 이상이어야 합니다.`)
             return
        }
        if (!room.maxGuests || parseInt(room.maxGuests) < 1) {
             openModal(`[${room.name}] 객실의 최대 인원은 1명 이상이어야 합니다.`)
             return
        }
        if (parseInt(room.minGuests) > parseInt(room.maxGuests)) {
             openModal(`[${room.name}] 객실의 최대 인원은 최소 인원보다 커야 합니다.`)
             return
        }
    }

    try {
        const roomsData = await Promise.all(rooms.value.map(async (room) => {
            let imagePayload = null

            // 1. 새로 업로드된 이미지가 있으면 Base64로 변환
            if (room.representativeImage instanceof File) {
                imagePayload = await fileToBase64(room.representativeImage)
            }
            // 2. representativeImagePreview가 http URL이면 (기존 이미지) URL 그대로 사용
            else if (room.representativeImagePreview && room.representativeImagePreview.startsWith('http')) {
                imagePayload = room.representativeImagePreview
            }
             // 3. 기존 이미지 URL이 있으면 유지 (fallback)
            else if (room.mainImageUrl) {
                imagePayload = room.mainImageUrl
            }

            // DB ID (Long) vs Temporary ID (Timestamp > 10000000000)
            const isTempId = typeof room.id === 'number' && room.id > 10000000000;
            return {
                roomId: isTempId ? null : room.id,
                roomName: room.name,
                price: parseInt(room.weekdayPrice),
                weekendPrice: parseInt(room.weekendPrice),
                minGuests: parseInt(room.minGuests),
                maxGuests: parseInt(room.maxGuests),
                roomDescription: room.description || '',
                mainImageUrl: imagePayload,
                bathroomCount: parseInt(room.bathroomCount) || 0,
                roomType: 'STANDARD',
                bedCount: parseInt(room.bedCount) || 0,
                roomStatus: room.isActive ? 1 : 0,
                amenities: room.amenities || []
            }
        }))

        // 이미지 데이터 구성 (Base64 변환)
        const imageList = []
        
        // 1. Banner Image
        if (bannerFile.value) {
            // New Banner Uploaded
            const base64 = await fileToBase64(bannerFile.value)
            imageList.push({
                imageUrl: base64,
                imageType: 'banner',
                sortOrder: 0
            })
        } else if (form.value.bannerImage) {
            // Existing Banner (URL) - Send as is
            imageList.push({
                imageUrl: form.value.bannerImage,
                imageType: 'banner',
                sortOrder: 0
            })
        }

        // 2. Detail Images
        for (let i = 0; i < displayImages.value.length; i++) {
            const item = displayImages.value[i]
            if (item.isNew && item.file) {
                 const base64 = await fileToBase64(item.file)
                 imageList.push({
                     imageUrl: base64,
                     imageType: 'detail',
                     sortOrder: i + 1
                 })
            } else {
                 imageList.push({
                     imageUrl: item.url,
                     imageType: 'detail',
                     sortOrder: i + 1
                 })
            }
        }

        const requestData = {
            accommodationsName: form.value.name,
            accommodationsCategory: accommodationTypeReverseMap[form.value.type] || form.value.type,
            accommodationsDescription: form.value.description,
            shortDescription: form.value.shortDescription || '',
            transportInfo: form.value.transportInfo || '',
            accommodationStatus: form.value.isActive ? 1 : 0,
            parkingInfo: form.value.parkingInfo || '',
            sns: form.value.sns || '',
            phone: form.value.phone, 
            checkInTime: form.value.checkInTime,

            checkOutTime: form.value.checkOutTime,
            latitude: form.value.latitude,
            longitude: form.value.longitude,
            
            // Bank Info Added
            bankName: form.value.bankName,
            accountNumber: form.value.accountNumber,
            accountHolder: form.value.accountHolder,

            rooms: roomsData,
            images: imageList,
            amenityIds: form.value.amenities,
            themeIds: form.value.themes.map(name => getThemeId(name)).filter(id => id !== undefined)
        }

        console.log('Update Request:', requestData) // Debug Log

        const token = getAccessToken()
        const response = await fetch(`${API_BASE_URL}/accommodations/${accommodationId}`, {
            method: 'PUT',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(requestData)
        })

        if (response.ok) {
            updateSuccess.value = true
            try {
                const raw = sessionStorage.getItem('hostResubmitMap')
                const map = raw ? JSON.parse(raw) : {}
                map[String(accommodationId)] = Date.now()
                sessionStorage.setItem('hostResubmitMap', JSON.stringify(map))
            } catch (e) {
                console.warn('Failed to store resubmit timestamp', e)
            }
            openModal('숙소 정보가 수정되었습니다.')
        } else if (response.status === 401) {
            // Token Expired
            alert('로그인 세션이 만료되었습니다. 다시 로그인해주세요.')
            router.push('/login')
        } else {
            console.error('Update failed status:', response.status)
            openModal('수정에 실패했습니다. (Status: ' + response.status + ')')
        }
    } catch (e) {
        console.error('HandleUpdate Error:', e)
        openModal('오류가 발생했습니다: ' + e.message)
    }
}

// ========== 객실 관리 ==========
const showRoomForm = ref(false)
const roomForm = ref({
  name: '',
  weekdayPrice: '',
  weekendPrice: '',
  minGuests: '',
  maxGuests: '',
  bedCount: '',
  bathroomCount: '',
  description: '',
  amenities: [],
  representativeImage: null,
  representativeImagePreview: '',
  isActive: true
})
const roomErrors = ref({})
const editingRoomId = ref(null)

// 숫자 필터링 (한글 등 방지)
const filterNumberInput = (event) => {
  const value = event.target.value
  // 숫자가 아닌 문자가 포함되어 있다면 제거
  if (/[^0-9]/.test(value)) {
     event.target.value = value.replace(/[^0-9]/g, '')
     // v-model 업데이트 트리거
     event.target.dispatchEvent(new Event('input'))
  }
}

const validateRoomForm = () => {
    roomErrors.value = {}
    let isValid = true
    const errorFields = []

    if (!roomForm.value.name?.trim()) {
        roomErrors.value.name = '객실명을 입력해주세요.'
        errorFields.push('객실명')
        isValid = false
    }

    // 숫자 유효성 검사 (Regex for non-digits)
    const isNumeric = (val) => /^\d+$/.test(val)

    if (!roomForm.value.weekdayPrice || !isNumeric(String(roomForm.value.weekdayPrice)) || parseInt(roomForm.value.weekdayPrice) <= 0) {
        roomErrors.value.weekdayPrice = '주중 요금을 입력해주세요.'
        errorFields.push('주중 요금')
        isValid = false
    }
    if (!roomForm.value.weekendPrice || !isNumeric(String(roomForm.value.weekendPrice)) || parseInt(roomForm.value.weekendPrice) <= 0) {
        roomErrors.value.weekendPrice = '주말 요금을 입력해주세요.'
        errorFields.push('주말 요금')
        isValid = false
    }
    if (!roomForm.value.minGuests || !isNumeric(String(roomForm.value.minGuests)) || parseInt(roomForm.value.minGuests) < 1) {
        roomErrors.value.minGuests = '최소 인원을 입력해주세요.'
        errorFields.push('최소 인원')
        isValid = false
    }
    if (!roomForm.value.maxGuests || !isNumeric(String(roomForm.value.maxGuests)) || parseInt(roomForm.value.maxGuests) < 1) {
        roomErrors.value.maxGuests = '최대 인원을 입력해주세요.'
        errorFields.push('최대 인원')
        isValid = false
    }
    if (parseInt(roomForm.value.minGuests) > parseInt(roomForm.value.maxGuests)) {
        roomErrors.value.maxGuests = '최대 인원은 최소 인원보다 커야 합니다.'
        isValid = false
    }
    if (roomForm.value.bedCount && !isNumeric(String(roomForm.value.bedCount))) {
        roomErrors.value.bedCount = '침대 수는 숫자만 입력해주세요.'
        errorFields.push('침대 수')
        isValid = false
    }
    if (roomForm.value.bathroomCount && !isNumeric(String(roomForm.value.bathroomCount))) {
        roomErrors.value.bathroomCount = '욕실 수는 숫자만 입력해주세요.'
        errorFields.push('욕실 수')
        isValid = false
    }

    if (!roomForm.value.representativeImage && !roomForm.value.representativeImagePreview) {
        roomErrors.value.representativeImage = '대표 이미지를 등록해주세요.'
        errorFields.push('대표 이미지')
        isValid = false
    }

    // 에러 필드 목록 저장 (모달 메시지용)
    roomErrors.value._errorFields = errorFields

    return isValid
}

const handleRoomImageUpload = (event) => {
    const file = event.target.files[0]
    if (file) {
        roomForm.value.representativeImage = file
        roomForm.value.representativeImagePreview = URL.createObjectURL(file)
    }
}
const removeRoomImage = () => {
    roomForm.value.representativeImage = null
    roomForm.value.representativeImagePreview = ''
}

// 객실 추가/수정
const saveRoom = () => {
    if (!validateRoomForm()) {
        const errorFields = roomErrors.value._errorFields || []
        if (errorFields.length > 0) {
            openModal(`다음 항목을 확인해주세요: ${errorFields.join(', ')}`)
        } else {
            openModal('객실 정보를 확인해주세요.')
        }
        return
    }

    // 기존 객실의 mainImageUrl 유지
    let existingMainImageUrl = null
    if (editingRoomId.value) {
        const existingRoom = rooms.value.find(r => r.id === editingRoomId.value)
        if (existingRoom) {
            existingMainImageUrl = existingRoom.mainImageUrl
        }
    }

    const roomData = {
        id: editingRoomId.value || Date.now(),
        ...roomForm.value,
        weekdayPrice: parseInt(roomForm.value.weekdayPrice),
        weekendPrice: parseInt(roomForm.value.weekendPrice),
        minGuests: parseInt(roomForm.value.minGuests),
        maxGuests: parseInt(roomForm.value.maxGuests),
        bedCount: parseInt(roomForm.value.bedCount) || 0,
        bathroomCount: parseInt(roomForm.value.bathroomCount) || 0,
        amenities: [...roomForm.value.amenities],
        // 새 이미지가 없으면 기존 mainImageUrl 유지
        mainImageUrl: roomForm.value.representativeImage ? null : existingMainImageUrl,
        // **Fix: Persist local preview state**
        representativeImage: roomForm.value.representativeImage,
        representativeImagePreview: roomForm.value.representativeImagePreview
    }

    if (editingRoomId.value) {
        const index = rooms.value.findIndex(r => r.id === editingRoomId.value)
        if (index !== -1) rooms.value[index] = roomData
    } else {
        rooms.value.push(roomData)
    }

    showRoomForm.value = false
    resetRoomForm()
    openModal(editingRoomId.value ? '객실이 수정되었습니다.' : '객실이 추가되었습니다.')
    editingRoomId.value = null
}

const editRoom = (room) => {
    editingRoomId.value = room.id
    roomForm.value = { ...room }
    // Ensure image preview is set if it's a URL
    // Ensure image preview is set if it's a URL
    if (room.mainImageUrl) {
         roomForm.value.representativeImagePreview = room.mainImageUrl
    } else if (room.representativeImagePreview) {
         roomForm.value.representativeImagePreview = room.representativeImagePreview
    }
    showRoomForm.value = false  // 인라인 폼 사용
}

const cancelEditRoom = () => {
    editingRoomId.value = null
    resetRoomForm()
}

const showNewRoomForm = () => {
    resetRoomForm()
    editingRoomId.value = null
    showRoomForm.value = true
}

const deleteRoom = async (id) => {
    if(!confirm('정말 삭제하시겠습니까?')) {
        return
    }

    try {
        const token = getAccessToken()
        const response = await fetch(`${API_BASE_URL}/rooms/${accommodationId}/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })

        if (response.ok) {
            rooms.value = rooms.value.filter(r => r.id !== id)
            alert('객실이 삭제되었습니다.')
        } else {
            const errorData = await response.json().catch(() => ({}))
            alert(errorData.message || '객실 삭제에 실패했습니다.')
        }
    } catch (error) {
        console.error('객실 삭제 오류:', error)
        alert('객실 삭제 중 오류가 발생했습니다.')
    }
}

// 승인 상태 확인 (APPROVED인지)
const isApproved = () => form.value.approvalStatus === 'APPROVED'

// 숙소 운영상태 토글
const toggleAccommodationStatus = () => {
    // 승인 전에는 토글 불가
    if (!isApproved()) {
        openModal('관리자 승인 후 운영 상태를 변경할 수 있습니다.')
        return
    }
    // 운영 중지로 변경하려는 경우 알럿 표시
    if (form.value.isActive) {
        alert('숙소 운영 상태가 비활성화 되었습니다')
    }
    form.value.isActive = !form.value.isActive
}

// 객실 운영상태 토글
const toggleRoomStatus = (room) => {
    // 승인 전에는 토글 불가
    if (!isApproved()) {
        openModal('관리자 승인 후 객실 운영 상태를 변경할 수 있습니다.')
        return
    }
    // 운영 중지로 변경하려는 경우 알럿 표시
    if (room.isActive) {
        alert('객실 운영 상태가 비활성화 되었습니다 ')
    }
    room.isActive = !room.isActive
}

// 객실 폼 운영상태 토글 (수정 모드에서)
const toggleRoomFormStatus = () => {
    // 승인 전에는 토글 불가
    if (!isApproved()) {
        openModal('관리자 승인 후 객실 운영 상태를 변경할 수 있습니다.')
        return
    }
    // 운영 중지로 변경하려는 경우 알럿 표시
    if (roomForm.value.isActive) {
        alert('객실 사용 중지')
    }
    roomForm.value.isActive = !roomForm.value.isActive
}

// 숙소 삭제
const deleteAccommodation = async () => {
    // 예약 정보 확인
    const hasActiveReservations = await checkHasReservations()
    if (hasActiveReservations) {
        alert('예약된 정보가 있어 삭제할 수 없습니다.')
        return
    }

    if(confirm('정말 숙소를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
        try {
            const token = getAccessToken()
            const response = await fetch(`${API_BASE_URL}/accommodations/${accommodationId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            })
            if (response.ok) {
                alert('숙소가 삭제되었습니다.')
                router.push('/host/accommodation')
            } else {
                alert('숙소 삭제에 실패했습니다.')
            }
        } catch (error) {
            console.error('숙소 삭제 오류:', error)
            alert('숙소 삭제 중 오류가 발생했습니다.')
        }
    }
}

const resetRoomForm = () => {
    roomForm.value = {
        name: '', weekdayPrice: '', weekendPrice: '', minGuests: '', maxGuests: '',
        bedCount: '', bathroomCount: '', description: '', amenities: [],
        representativeImage: null, representativeImagePreview: '', isActive: true // Default to active
    }
    roomErrors.value = {}
}

// Base64 Util
const fileToBase64 = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

const fetchImageAsBase64 = async (url) => {
  if (!url) return null
  const absoluteUrl = getFullImageUrl(url)
  const response = await fetch(absoluteUrl)
  if (!response.ok) {
    throw new Error('이미지를 불러오지 못했습니다.')
  }
  const blob = await response.blob()
  return await fileToBase64(blob)
}

const resolveAllAiImageBase64 = async () => {
  const base64Images = []

  // 배너 이미지
  if (bannerFile.value) {
    // 새로 업로드한 파일은 리사이즈하여 Base64로 변환
    try {
      base64Images.push(await resizeImage(bannerFile.value))
    } catch (e) {
      console.warn('Banner resize failed', e)
      base64Images.push(await fileToBase64(bannerFile.value))
    }
  } else if (form.value.bannerImage) {
    // 기존 이미지는 URL을 그대로 전달 (백엔드가 다운로드 처리)
    base64Images.push(form.value.bannerImage)
  }

  // 상세 이미지들
  for (const item of displayImages.value) {
    if (item.file) {
      // 새로 업로드한 파일은 리사이즈하여 Base64로 변환
      try {
        base64Images.push(await resizeImage(item.file))
      } catch (e) {
        console.warn('Detail image resize failed', e)
        base64Images.push(await fileToBase64(item.file))
      }
    } else if (item.url) {
      // 기존 이미지는 URL을 그대로 전달 (백엔드가 다운로드 처리)
      base64Images.push(item.url)
    }
  }

  return base64Images
}

const applyAiSuggestion = async () => {
  if (isAiSuggesting.value) return
  try {
    const base64Images = await resolveAllAiImageBase64()
    if (base64Images.length === 0) {
      openModal('AI 추천을 사용하려면 먼저 이미지를 업로드하거나 기존 이미지를 유지해주세요.')
      return
    }
    isAiSuggesting.value = true
    const payload = {
      images: base64Images,
      language: 'ko',
      context: {
        city: form.value.city,
        district: form.value.district,
        township: form.value.township,
        stayType: form.value.type,
        themes: form.value.themes,
        existingName: form.value.name,
        existingDescription: form.value.description
      }
    }
    const result = await requestAccommodationAiSuggestion(payload)
    if (!result.ok) {
      const message = result?.data?.message || 'AI 추천을 불러오지 못했습니다.'
      throw new Error(message)
    }
    const data = result.data || {}
    if (data.name) {
      form.value.name = data.name
    }
    if (data.description) {
      form.value.description = data.description
    }
    openModal('AI 추천 결과를 적용했습니다.')
  } catch (error) {
    console.error('AI 추천 실패:', error)
    openModal(`AI 추천 실패: ${error?.message || '잠시 후 다시 시도해주세요.'}`)
  } finally {
    isAiSuggesting.value = false
  }
}

// 배너 이미지 업로드
const handleBannerUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    bannerFile.value = file
    const url = URL.createObjectURL(file)
    bannerPreview.value = url
    // form.value.bannerImage acts as preview for banner in template? 
    // Template uses form.bannerImage for existing and if null? 
    // Wait, lets check template. 
    // Template: <img v-if="!bannerPreview && form.bannerImage" :src="form.bannerImage">
    //           <img v-if="bannerPreview" :src="bannerPreview">
  }
}

// 배너 이미지 삭제
const removeBannerImage = () => {
  bannerFile.value = null
  bannerPreview.value = ''
  form.value.bannerImage = null
}

// 상세 이미지 업로드
const handleDetailImagesUpload = (event) => {
  const files = Array.from(event.target.files)
  const remaining = 5 - displayImages.value.length

  files.slice(0, remaining).forEach(file => {
      displayImages.value.push({
          id: Date.now() + Math.random(),
          url: URL.createObjectURL(file), // Preview URL
          file: file,
          isNew: true
      })
  })
  
  // 입력값 초기화
  event.target.value = ''
}

// 상세 이미지 삭제
const removeDetailImage = (idx) => {
  displayImages.value.splice(idx, 1)
}




onMounted(async () => {
  await loadThemes() // 테마 목록 먼저 로드
  loadAccommodation()
})
</script>

<template>
  <div class="register-page" :class="{ loading: isLoading }">
    <div v-if="isLoading" class="loading-spinner">Loading...</div>

    <!-- Page Header -->
    <div v-else class="page-header">
      <div class="header-top">
        <div class="title-area">
          <div class="title-row">
            <h1>숙소 정보 수정</h1>
            <RouterLink
              class="policy-link"
              :to="{ path: '/policy', query: { tab: 'host' } }"
              target="_blank"
              rel="noopener"
            >
              숙소등록 이용약관 바로가기
            </RouterLink>
          </div>
        </div>
      </div>
      
      <!-- Toggle & Actions -->
      <div class="header-controls">
        <!-- 승인 상태 배지 -->
        <div class="approval-status-badge" :class="form.approvalStatus.toLowerCase()">
          {{ form.approvalStatus === 'APPROVED' ? '승인완료' : form.approvalStatus === 'PENDING' ? '승인대기' : '승인거절' }}
        </div>

        <div class="toggle-wrapper" :class="{ disabled: form.approvalStatus !== 'APPROVED' }">
          <span class="toggle-label">{{ form.isActive ? '운영 중' : '운영 중지' }}</span>
          <div
            class="toggle-switch"
            :class="{ active: form.isActive, disabled: form.approvalStatus !== 'APPROVED' }"
            @click="toggleAccommodationStatus"
          >
            <div class="toggle-slider"></div>
          </div>
        </div>

      </div>
    </div>

    <!-- ========== Form Content ========== -->
    <div v-if="!isLoading" class="form-content">
      
      <!-- Section: 숙소 수정 -->
      <section class="form-section">
        <h2 class="section-title">숙소 수정</h2>
        <p class="section-desc">숙소의 정보를 수정해주세요.</p>

        <!-- Image & AI Section -->
        <div class="image-ai-container">
          <!-- Section: Images (Restructured) -->
          <section class="form-section image-section">
            <h3 class="subsection-title">숙소 이미지 <span class="required">*</span></h3>
            <p class="section-desc">멋진 숙소 사진을 올려주세요. AI가 사진을 분석해 소개글을 만들어드려요.</p>

            <div class="image-grid-layout">
              <!-- Banner Image (Large) -->
              <div class="banner-upload-area" :class="{ 'has-error': false }">
                <div v-if="bannerPreview || form.bannerImage" class="banner-preview-wrapper">
                  <img :src="bannerPreview ? bannerPreview : form.bannerImage" class="banner-preview" />
                  <button type="button" class="remove-btn" @click="removeBannerImage">
                    <i class="fas fa-times"></i>
                  </button>
                  <span class="badge-banner">대표 이미지</span>
                </div>
                <label v-else class="upload-placeholder banner-placeholder">
                  <input type="file" accept="image/*" @change="handleBannerUpload" hidden />
                  <div class="placeholder-content">
                    <span class="icon">📷</span>
                    <span class="text">대표 이미지 업로드</span>
                    <span class="sub-text">1920x600 권장</span>
                  </div>
                </label>
              </div>

              <!-- Detail Images (Grid) -->
              <div class="detail-upload-grid">
                <div v-for="(img, idx) in displayImages" :key="img.id || idx" class="detail-image-item">
                  <img :src="img.url" />
                  <button type="button" class="remove-btn" @click="removeDetailImage(idx)">
                    <i class="fas fa-times"></i>
                  </button>
                </div>
                
                <label v-if="displayImages.length < 5" class="upload-placeholder detail-placeholder">
                  <input type="file" accept="image/*" multiple @change="handleDetailImagesUpload" hidden />
                  <div class="placeholder-content">
                    <span class="icon">＋</span>
                    <span class="text">추가</span>
                  </div>
                </label>
              </div>
            </div>
          </section>

          <!-- AI Suggestion Button (Clean & Simple) -->
          <div class="ai-action-area">
            <button 
              type="button" 
              class="ai-magic-btn" 
              @click="applyAiSuggestion" 
              :disabled="isAiSuggesting"
              :class="{ 'is-loading': isAiSuggesting }"
            >
              <span class="icon">✨</span>
              <span class="label">{{ isAiSuggesting ? 'AI가 분석중...' : 'AI로 소개글 완성하기' }}</span>
            </button>
            <p class="ai-hint">이미지를 올리고 버튼을 누르면 제목과 소개를 자동으로 작성해드려요.</p>
          </div>
        </div>

        <h3 class="subsection-title">기본정보</h3>

        <div class="form-group">
          <label>숙소명 <span class="required">*</span></label>
          <input type="text" v-model="form.name" placeholder="숙소명을 입력해주세요" />
        </div>

        <div class="form-group">
          <label>숙소유형</label>
          <input type="text" v-model="form.type" readonly class="readonly-input" />
        </div>

        <div class="form-group">
          <label>대표 연락처</label>
          <input type="text" v-model="form.phone" readonly class="readonly-input" />
        </div>

        <div class="form-group">
          <label>사업자등록번호</label>
          <input type="text" v-model="form.businessRegistrationNumber" readonly class="readonly-input" />
        </div>
      </section>

      <!-- Section: 상세 정보 (수정 가능) -->
      <section class="form-section">
        <h3 class="subsection-title">숙소 상세 정보</h3>
        
        <div class="form-group">
          <label>한 줄 설명</label>
          <input type="text" v-model="form.shortDescription" placeholder="숙소 리스트에 표시될 짧은 소개글입니다." />
        </div>

        <div class="form-group">
          <label>숙소 소개(상세설명) <span class="required">*</span></label>
          <textarea
            v-model="form.description"
            rows="5"
            :class="{ 'input-error': errors.description }"
            placeholder="숙소의 매력 포인트, 주변 환경, 호스팅 스타일 등을 상세히 적어주세요."
          ></textarea>
          <span v-if="errors.description" class="error-message">{{ errors.description }}</span>
        </div>

        <div class="form-group">
          <label>SNS</label>
          <input type="text" v-model="form.sns" placeholder="@instagram_id" />
        </div>
      </section>

      <!-- Section: 위치 정보 (수정 불가) -->
      <section class="form-section">
        <h3 class="subsection-title">위치 정보 (수정 불가)</h3>

        <div class="form-group">
          <label>시/도</label>
          <input type="text" v-model="form.city" readonly class="readonly-input" />
        </div>

        <div class="form-group">
          <label>구/군</label>
          <input type="text" v-model="form.district" readonly class="readonly-input" />
        </div>

        <div class="form-group">
          <label>읍/면/동</label>
          <input type="text" v-model="form.township" readonly class="readonly-input" />
        </div>

        <div class="form-group">
          <label>상세주소</label>
          <input type="text" v-model="form.address" readonly class="readonly-input" />
        </div>

        <div class="form-group">
            <div ref="mapContainer" class="kakao-map"></div>
        </div>
      </section>

      <!-- Section: 교통 및 주차 (수정 가능) -->
      <section class="form-section">
        <h3 class="subsection-title">교통 및 주차 정보</h3>
        


        <div class="form-group">
          <label>주차정보</label>
          <textarea v-model="form.parkingInfo" rows="3" placeholder="예: 건물 내 무료 주차 가능"></textarea>
        </div>
      </section>

      <!-- Section: 운영 정책 (수정 가능) -->
      <section class="form-section">
        <h3 class="subsection-title">체크인/아웃 정보</h3>
        
        <div class="form-row two-col">
          <div class="form-group">
            <label>체크인 시간 <span class="required">*</span></label>
            <div class="time-selector-group">
                <select v-model="checkInHour" class="time-select">
                    <option v-for="h in hourOptions" :key="h" :value="h">{{ h }}시</option>
                </select>
                <span class="time-separator">:</span>
                <select v-model="checkInMinute" class="time-select">
                    <option v-for="m in minuteOptions" :key="m" :value="m">{{ m }}분</option>
                </select>
            </div>
            <span v-if="errors.checkInTime" class="error-message">{{ errors.checkInTime }}</span>
          </div>
          
          <div class="form-group">
            <label>체크아웃 시간 <span class="required">*</span></label>
            <div class="time-selector-group">
                <select v-model="checkOutHour" class="time-select">
                    <option v-for="h in hourOptions" :key="h" :value="h">{{ h }}시</option>
                </select>
                <span class="time-separator">:</span>
                <select v-model="checkOutMinute" class="time-select">
                    <option v-for="m in minuteOptions" :key="m" :value="m">{{ m }}분</option>
                </select>
            </div>
            <span v-if="errors.checkOutTime" class="error-message">{{ errors.checkOutTime }}</span>
          </div>
        </div>
      </section>

      <!-- Section: 편의 시설 & 테마 -->
      <section class="form-section">
        <h3 class="subsection-title">편의 시설 & 테마</h3>

        <div class="form-group">
          <label class="mb-2">편의 시설</label>
          <div class="amenities-grid">
            <label
              v-for="amenity in amenityOptions"
              :key="amenity.id"
              class="amenity-checkbox"
              :class="{ checked: isAmenityChecked(amenity.id) }"
            >
              <input
                type="checkbox"
                :checked="isAmenityChecked(amenity.id)"
                @change="toggleAmenity(amenity.id)"
              />
              <span class="checkmark"></span>
              <span class="amenity-label">{{ amenity.label }}</span>
            </label>
          </div>
        </div>

        <div class="form-group mt-4">
           <label class="mb-2">테마 (최대 6개)</label>
           <div v-for="(category, key) in themeOptions" :key="key" class="theme-category">
             <div class="theme-category-title">{{ category.label }}</div>
             <div class="theme-tags">
               <label
                 v-for="item in category.items"
                 :key="item"
                 class="theme-tag"
                 :class="{ selected: isThemeChecked(item) }"
               >
                 <input
                   type="checkbox"
                   :checked="isThemeChecked(item)"
                   @change="toggleTheme(item)"
                 />
                 {{ item }}
               </label>
             </div>
           </div>
        </div>
      </section>
      
      <!-- Section: 정산 계좌 (수정 가능) -->
      <section class="form-section">
         <h3 class="subsection-title">정산 계좌</h3>
         <div class="form-group">
            <label>은행명</label>
            <select v-model="form.bankName">
               <option value="" disabled>선택해주세요</option>
               <option v-for="bank in bankList" :key="bank" :value="bank">{{ bank }}</option>
            </select>
         </div>
         <div class="form-group">
            <label>예금주</label>
            <input type="text" v-model="form.accountHolder" placeholder="예금주명을 입력해주세요" />
         </div>
         <div class="form-group">
            <label>계좌번호</label>
            <input type="text" v-model="form.accountNumber" placeholder="'-' 없이 숫자만 입력" @input="filterNumberInput" />
         </div>
      </section>

      <!-- Section: 객실 관리 (수정 가능) -->
      <section class="form-section">
        <h3 class="subsection-title">객실 관리</h3>

        <!-- Room List -->
        <div v-if="rooms.length > 0" class="room-list">
          <div v-for="room in rooms" :key="room.id" class="room-card">
            <!-- 수정 모드가 아닐 때: 카드 표시 -->
            <template v-if="editingRoomId !== room.id">
              <div class="room-header">
                <h4 class="room-name">{{ room.name }}</h4>
                <div class="room-toggle" :class="{ disabled: form.approvalStatus !== 'APPROVED' }">
                  <span class="toggle-label-small">{{ room.isActive ? '운영 중' : '운영 중지' }}</span>
                  <div
                    class="toggle-switch small"
                    :class="{ active: room.isActive, disabled: form.approvalStatus !== 'APPROVED' }"
                    @click="toggleRoomStatus(room)"
                  >
                    <div class="toggle-slider"></div>
                  </div>
                </div>
              </div>
              <div class="room-details">
                <div class="detail-row">
                  <span class="detail-label">주중 요금</span>
                  <span class="detail-value">₩{{ Number(room.weekdayPrice).toLocaleString() }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">주말 요금</span>
                  <span class="detail-value">₩{{ Number(room.weekendPrice).toLocaleString() }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">인원</span>
                  <span class="detail-value">{{ room.minGuests }}~{{ room.maxGuests }}명</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">침대/욕실</span>
                  <span class="detail-value">침대 {{ room.bedCount || 0 }}개 | 욕실 {{ room.bathroomCount || 0 }}개</span>
                </div>
              </div>
              <div class="room-actions">
                <button class="room-btn edit" @click="editRoom(room)">수정</button>
                <button class="room-btn delete" @click="deleteRoom(room.id)">삭제</button>
              </div>
            </template>

            <!-- 수정 모드일 때: 폼 표시 (해당 카드 위치에서) -->
            <div v-else class="room-form-inline">
              <div class="editing-room-header">
                 <span class="editing-badge">수정 중</span>
                 <h4 class="editing-target-name">{{ room.name }}</h4>
              </div>

              <div class="form-group">
                <label>객실명 <span class="required">*</span></label>
                <input type="text" v-model="roomForm.name" :class="{ 'input-error': roomErrors.name }" placeholder="객실 이름을 입력하세요" />
                <span v-if="roomErrors.name" class="error-message">{{ roomErrors.name }}</span>
              </div>

              <!-- 객실 이미지 -->
              <div class="form-group">
                <label>객실 대표 이미지 <span class="required">*</span></label>
                <div class="room-image-upload-area" :class="{ 'upload-error': roomErrors.representativeImage }">
                  <div v-if="roomForm.representativeImagePreview" class="room-image-preview">
                    <img :src="roomForm.representativeImagePreview" alt="객실 대표 이미지" />
                    <button type="button" class="room-remove-image-btn" @click="removeRoomImage">✕</button>
                  </div>
                  <label v-else class="room-upload-box">
                    <input type="file" accept="image/*" @change="handleRoomImageUpload" class="hidden-file-input" />
                    <div class="room-upload-content">
                      <span class="room-upload-icon">📷</span>
                      <span class="room-upload-text">이미지 업로드</span>
                    </div>
                  </label>
                </div>
                <span v-if="roomErrors.representativeImage" class="error-message">{{ roomErrors.representativeImage }}</span>
              </div>

              <!-- 운영상태 토글 -->
              <div class="form-group">
                <label>운영 상태</label>
                <div class="room-status-toggle" :class="{ disabled: form.approvalStatus !== 'APPROVED' }">
                  <span class="toggle-label-small">{{ roomForm.isActive ? '운영 중' : '운영 중지' }}</span>
                  <div
                    class="toggle-switch small"
                    :class="{ active: roomForm.isActive, disabled: form.approvalStatus !== 'APPROVED' }"
                    @click="toggleRoomFormStatus"
                  >
                    <div class="toggle-slider"></div>
                  </div>
                  <span v-if="form.approvalStatus !== 'APPROVED'" class="toggle-hint-small">승인 후 변경 가능</span>
                </div>
              </div>

              <div class="form-row two-col">
                <div class="form-group">
                  <label>주중 요금 <span class="required">*</span></label>
                  <input type="number" v-model="roomForm.weekdayPrice" :class="{ 'input-error': roomErrors.weekdayPrice }" @input="filterNumberInput" />
                  <span v-if="roomErrors.weekdayPrice" class="error-message">{{ roomErrors.weekdayPrice }}</span>
                </div>
                <div class="form-group">
                  <label>주말 요금 <span class="required">*</span></label>
                  <input type="number" v-model="roomForm.weekendPrice" :class="{ 'input-error': roomErrors.weekendPrice }" @input="filterNumberInput" />
                  <span v-if="roomErrors.weekendPrice" class="error-message">{{ roomErrors.weekendPrice }}</span>
                </div>
              </div>

              <div class="form-row two-col">
                <div class="form-group">
                  <label>최소 인원</label>
                  <input type="number" v-model="roomForm.minGuests" :class="{ 'input-error': roomErrors.minGuests }" @input="filterNumberInput" />
                  <span v-if="roomErrors.minGuests" class="error-message">{{ roomErrors.minGuests }}</span>
                </div>
                <div class="form-group">
                  <label>최대 인원</label>
                  <input type="number" v-model="roomForm.maxGuests" :class="{ 'input-error': roomErrors.maxGuests }" @input="filterNumberInput" />
                  <span v-if="roomErrors.maxGuests" class="error-message">{{ roomErrors.maxGuests }}</span>
                </div>
              </div>

              <div class="form-row two-col">
                <div class="form-group">
                  <label>침대 개수</label>
                  <input v-model="roomForm.bedCount" type="number" @input="filterNumberInput" />
                </div>
                <div class="form-group">
                  <label>욕실 개수</label>
                  <input v-model="roomForm.bathroomCount" type="number" @input="filterNumberInput" />
                </div>
              </div>

              <div class="form-group">
                <label>객실 설명</label>
                <textarea v-model="roomForm.description" rows="3"></textarea>
              </div>

              <!-- Room Amenities -->
              <div class="room-amenities-section">
                <div v-for="(cat, key) in roomAmenityOptions" :key="key" class="room-amenity-category">
                  <div class="room-amenity-label">{{ cat.label }}</div>
                  <div class="room-amenity-tags">
                    <label v-for="item in cat.items" :key="item" class="room-amenity-tag" :class="{ selected: roomForm.amenities.includes(item) }">
                      <input type="checkbox" :checked="roomForm.amenities.includes(item)" @change="toggleRoomAmenity(item)" />
                      {{ item }}
                    </label>
                  </div>
                </div>
              </div>

              <div class="room-form-actions">
                <button class="btn-outline" @click="cancelEditRoom">취소</button>
                <button class="btn-primary" @click="saveRoom">수정</button>
              </div>
            </div>
          </div>
        </div>

        <p v-else class="no-rooms" :class="{ 'no-rooms-error': errors.rooms }">
            등록된 객실이 없습니다.
            <span v-if="errors.rooms" class="error-message">{{ errors.rooms }}</span>
        </p>

        <button class="add-room-btn" @click="showNewRoomForm" v-if="!showRoomForm && !editingRoomId">
          + 객실 추가하기
        </button>

        <!-- 새 객실 추가 폼 (맨 밑에) -->
        <div v-if="showRoomForm && !editingRoomId" class="room-form">
           <h4 class="room-form-title">{{ editingRoomId ? '객실 수정' : '새 객실 추가' }}</h4>

           <div class="form-group">
             <label>객실명 <span class="required">*</span></label>
             <input type="text" v-model="roomForm.name" :class="{ 'input-error': roomErrors.name }" placeholder="객실 이름을 입력하세요" />
             <span v-if="roomErrors.name" class="error-message">{{ roomErrors.name }}</span>
           </div>

           <!-- 객실 이미지 -->
           <div class="form-group">
             <label>객실 대표 이미지 <span class="required">*</span></label>
             <div class="room-image-upload-area" :class="{ 'upload-error': roomErrors.representativeImage }">
               <div v-if="roomForm.representativeImagePreview" class="room-image-preview">
                 <img :src="roomForm.representativeImagePreview" alt="객실 대표 이미지" />
                 <button type="button" class="room-remove-image-btn" @click="removeRoomImage">✕</button>
               </div>
               <label v-else class="room-upload-box">
                 <input type="file" accept="image/*" @change="handleRoomImageUpload" class="hidden-file-input" />
                 <div class="room-upload-content">
                   <span class="room-upload-icon">📷</span>
                   <span class="room-upload-text">이미지 업로드</span>
                 </div>
               </label>
             </div>
             <span v-if="roomErrors.representativeImage" class="error-message">{{ roomErrors.representativeImage }}</span>
           </div>

           <div class="form-row two-col">
             <div class="form-group">
               <label>주중 요금 <span class="required">*</span></label>
               <input type="number" v-model="roomForm.weekdayPrice" :class="{ 'input-error': roomErrors.weekdayPrice }" @input="filterNumberInput" />
               <span v-if="roomErrors.weekdayPrice" class="error-message">{{ roomErrors.weekdayPrice }}</span>
             </div>
             <div class="form-group">
               <label>주말 요금 <span class="required">*</span></label>
               <input type="number" v-model="roomForm.weekendPrice" :class="{ 'input-error': roomErrors.weekendPrice }" @input="filterNumberInput" />
               <span v-if="roomErrors.weekendPrice" class="error-message">{{ roomErrors.weekendPrice }}</span>
             </div>
           </div>

           <div class="form-row two-col">
             <div class="form-group">
               <label>최소 인원</label>
               <input type="number" v-model="roomForm.minGuests" :class="{ 'input-error': roomErrors.minGuests }" @input="filterNumberInput" />
               <span v-if="roomErrors.minGuests" class="error-message">{{ roomErrors.minGuests }}</span>
             </div>
             <div class="form-group">
               <label>최대 인원</label>
               <input type="number" v-model="roomForm.maxGuests" :class="{ 'input-error': roomErrors.maxGuests }" @input="filterNumberInput" />
               <span v-if="roomErrors.maxGuests" class="error-message">{{ roomErrors.maxGuests }}</span>
              </div>
            </div>

           <div class="form-row two-col">
            <div class="form-group">
              <label>침대 개수</label>
              <input v-model="roomForm.bedCount" type="number" @input="filterNumberInput" />
            </div>
            <div class="form-group">
              <label>욕실 개수</label>
              <input v-model="roomForm.bathroomCount" type="number" @input="filterNumberInput" />
            </div>
          </div>

          <div class="form-group">
            <label>객실 설명</label>
            <textarea v-model="roomForm.description" rows="3"></textarea>
          </div>



          <div class="room-form-actions">
            <button class="btn-outline" @click="showRoomForm = false">취소</button>
            <button class="btn-primary" @click="saveRoom">{{ editingRoomId ? '수정' : '등록' }}</button>
          </div>
        </div>
      </section>

      <!-- Bottom Actions -->
      <div class="bottom-actions">
        <button class="btn-cancel" @click="$router.push('/host/accommodation')">취소</button>
        <button class="btn-delete" @click="deleteAccommodation">숙소 삭제</button>
        <button class="btn-submit" @click="handleUpdate">수정 완료</button>
      </div>
    </div>
    
    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <p class="modal-message">{{ modalMessage }}</p>
        <button class="modal-btn" @click="closeModal">확인</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  background: #f8f9fa;
  min-height: 100vh;
  padding-bottom: 2rem;
}

/* Page Header */
.page-header {
  background: white;
  padding: 1.5rem;
  margin: 1rem;
  max-width: 570px;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  position: sticky;
  top: 1rem;
  z-index: 10;
  overflow: hidden;
}

@media (min-width: 768px) {
  .page-header {
    margin: 1rem auto;
  }
}

.header-top {
  margin-bottom: 1rem;
}

.title-area {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.5rem;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.policy-link {
  font-size: 0.9rem;
  color: #00875a;
  text-decoration: none;
  font-weight: 600;
}

.policy-link:hover {
  text-decoration: underline;
}

.logo-badge {
  width: 36px;
  height: 36px;
  background: #BFE7DF;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: #004d40;
  font-size: 1.1rem;
}

.page-header h1 {
  font-size: 1.25rem;
  font-weight: 700;
  color: #BFE7DF;
  margin: 0;
}

/* Progress Bar */
.progress-wrapper {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.progress-bar {
  position: relative;
  height: 6px;
  background: #e0e0e0;
  border-radius: 3px;
  flex: 1;
}

.progress-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: #BFE7DF;
  border-radius: 3px;
}

.progress-text {
  position: static;
  font-size: 0.75rem;
  color: #888;
  white-space: nowrap;
  margin-left: 0.5rem;
  flex-shrink: 0;
}

/* Header Controls */
.header-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1.5rem;
}

.toggle-wrapper {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 25px;
}

.toggle-label {
  font-size: 0.9rem;
  color: #333;
}

.toggle-switch {
  width: 44px;
  height: 24px;
  background: #D1D5DB;
  border-radius: 12px;
  position: relative;
  cursor: pointer;
  transition: background 0.3s;
}

.toggle-switch.active {
  background: #BFE7DF;
}

.toggle-slider {
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  position: absolute;
  top: 2px;
  left: 2px;
  transition: left 0.3s;
  box-shadow: 0 1px 2px rgba(0,0,0,0.2);
}

.toggle-switch.active .toggle-slider {
  left: 22px;
}

/* 비활성화된 토글 스타일 */
.toggle-switch.disabled {
  background: #e0e0e0;
  cursor: not-allowed;
  opacity: 0.6;
}

.toggle-wrapper.disabled {
  opacity: 0.7;
}

.room-toggle.disabled,
.room-status-toggle.disabled {
  opacity: 0.7;
}

.toggle-hint {
  font-size: 0.75rem;
  color: #f57c00;
  margin-left: 8px;
}

.toggle-hint-small {
  font-size: 0.7rem;
  color: #f57c00;
  margin-left: 6px;
}

/* 승인 상태 배지 */
.approval-status-badge {
  padding: 0.4rem 0.8rem;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 600;
  margin-right: 1rem;
}

.approval-status-badge.approved {
  background: #e8f5e9;
  color: #2e7d32;
}

.approval-status-badge.pending {
  background: #fff3e0;
  color: #f57c00;
}

.approval-status-badge.rejected {
  background: #ffebee;
  color: #c62828;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}

.btn-outline {
  padding: 0.6rem 1rem;
  border: 1px solid #ddd;
  background: white;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  color: #333;
  cursor: pointer;
}

.btn-primary {
  padding: 0.6rem 1rem;
  border: none;
  background: #BFE7DF;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  color: #004d40;
  cursor: pointer;
}

.btn-primary:hover {
  background: #a8ddd2;
}

.btn-danger {
  padding: 0.6rem 1rem;
  border: none;
  background: #ff5252;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  color: white;
  cursor: pointer;
}

.btn-danger:hover {
  background: #ff1744;
}

/* Form Content */
.form-content {
  padding: 0 1rem 1rem;
  max-width: 600px;
  margin: 0 auto;
}

.form-section {
  background: white;
  border-radius: 16px;
  padding: 1.5rem;
  margin-bottom: 1rem;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

.section-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #BFE7DF;
  margin: 0 0 0.5rem;
}

.section-desc {
  font-size: 0.9rem;
  color: #888;
  margin: 0 0 1.5rem;
}

.subsection-title {
  font-size: 1rem;
  font-weight: 700;
  color: #222;
  margin: 0 0 1rem;
}

/* Form Groups */
.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  font-size: 0.9rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.5rem;
}

.required {
  color: #BFE7DF;
}

input[type="text"],
input[type="tel"],
input[type="email"],
select,
textarea {
  width: 100%;
  padding: 0.875rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.95rem;
  background: white;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

input:focus,
select:focus,
textarea:focus {
  outline: none;
  border-color: #BFE7DF;
}

input::placeholder,
textarea::placeholder {
  color: #aaa;
}

select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23666' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 1rem center;
  cursor: pointer;
}

/* Location Button */
.btn-location {
  width: 100%;
  padding: 0.875rem;
  background: #BFE7DF;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: #004d40;
  cursor: pointer;
  margin-top: 0.5rem;
}

.btn-location:hover {
  background: #a8ddd2;
}

/* Kakao Map */
.kakao-map {
  width: 100%;
  height: 280px;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
}

.coords-info {
  margin-top: 0.5rem;
  font-size: 0.8rem;
  color: #666;
  background: #f0f0f0;
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
}

.help-text {
  font-size: 0.8rem;
  color: #888;
  margin-top: 0.75rem;
}

/* Bottom Actions */
.bottom-actions {
  display: flex;
  gap: 1rem;
  margin-top: 1.5rem;
}

.btn-cancel {
  flex: 1;
  padding: 1rem;
  background: #f5f5f5;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: #666;
  cursor: pointer;
}

.btn-delete {
  flex: 1;
  padding: 1rem;
  background: #fff5f5;
  border: 1px solid #fecaca;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: #ef4444;
  cursor: pointer;
}

.btn-delete:hover {
  background: #fee2e2;
}

.btn-submit {
  flex: 2;
  padding: 1rem;
  background: #BFE7DF;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: #004d40;
  cursor: pointer;
}

.btn-submit:hover {
  background: #a8ddd2;
}

/* Mobile Responsive */
@media (max-width: 480px) {
  .header-controls {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }
  
  .action-buttons {
    width: 100%;
  }
  
  .btn-outline,
  .btn-primary {
    flex: 1;
  }
}

/* Time Input */
.time-input {
  position: relative;
}

.time-input input[type="time"] {
  width: 100%;
  padding: 0.875rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.95rem;
  background: white;
}

.time-input input[type="time"]:focus {
  outline: none;
  border-color: #BFE7DF;
}

/* Time Selector */
.time-selector-group {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.time-select {
    flex: 1;
    padding: 0.875rem 1rem;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    background: white;
    font-size: 0.95rem;
    cursor: pointer;
    appearance: none; /* Custom arrow can be added if needed, but keeping simple for now */
    text-align: center;
}

.time-select:focus {
    outline: none;
    border-color: #BFE7DF;
}

.time-separator {
    font-weight: bold;
    color: #333;
}

/* Amenities Grid */
.amenities-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.amenity-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.amenity-checkbox:hover {
  border-color: #BFE7DF;
}

.amenity-checkbox.checked {
  border-color: #BFE7DF;
  background: #f0fcfa;
}

.amenity-checkbox input[type="checkbox"] {
  width: 18px;
  height: 18px;
  accent-color: #BFE7DF;
  cursor: pointer;
}

.amenity-label {
  font-size: 0.9rem;
  color: #333;
}

/* Upload Box */
.upload-box {
  position: relative;
  border: 2px dashed #e0e0e0;
  border-radius: 12px;
  padding: 2rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  overflow: hidden;
}

.upload-box:hover {
  border-color: #BFE7DF;
  background: #f9fefe;
}

.upload-box input[type="file"] {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.upload-text {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
}

.upload-info {
  font-size: 0.85rem;
  color: #888;
}

.upload-hint {
  font-size: 0.8rem;
  color: #aaa;
  padding: 0.25rem 0.75rem;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  margin-top: 0.5rem;
}

.banner-preview {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 8px;
}

/* Detail Images Preview */
.detail-images-preview {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 0.75rem;
  margin-top: 1rem;
}

.detail-image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
}

.detail-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0,0,0,0.5);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
}

/* Theme Categories */
.theme-category {
  margin-bottom: 1.5rem;
}

.theme-category-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.75rem;
}

.theme-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.theme-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.5rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  font-size: 0.9rem;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.theme-tag input[type="checkbox"] {
  display: none;
}

.theme-tag:hover {
  border-color: #BFE7DF;
}

.theme-tag.selected {
  border-color: #BFE7DF;
  background: #f0fcfa;
  color: #004d40;
}

/* Room List */
.room-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.room-card {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 1rem;
}

.room-info {
  margin-bottom: 1rem;
}

.room-name {
  font-size: 1.1rem;
  font-weight: 700;
  color: #222;
  margin: 0 0 0.5rem;
}

.room-details {
  font-size: 0.85rem;
  color: #666;
  margin: 0 0 0.75rem;
}

.room-toggle {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  font-weight: 600;
}

.toggle-switch.small {
  width: 36px;
  height: 20px;
}

.toggle-switch.small .toggle-slider {
  width: 16px;
  height: 16px;
}

.toggle-switch.small.active .toggle-slider {
  left: 18px;
}

.room-actions {
  display: flex;
  gap: 0.5rem;
}

.room-btn {
  flex: 1;
  padding: 0.6rem;
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
}

.room-card-image {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
}

.room-card-image img {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #eee;
}

.room-btn:hover {
  background: #f5f5f5;
}

.no-rooms {
  text-align: center;
  color: #888;
  padding: 2rem;
}

/* Add Room Button */
.add-room-btn {
  width: 100%;
  padding: 1rem;
  border: 2px dashed #BFE7DF;
  background: transparent;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  color: #BFE7DF;
  cursor: pointer;
  transition: all 0.2s;
}

.add-room-btn:hover {
  background: #f5fcfa;
}

/* Form Helper Classes */
.form-row {
  display: flex;
  gap: 1rem;
}

.form-row.two-col > * {
  flex: 1;
}

.input-with-unit {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-unit input {
  padding-right: 2.5rem;
}

.unit {
  position: absolute;
  right: 1rem;
  color: #666;
  font-size: 0.9rem;
}

/* Room Form */
.room-content {
  background: white;
  border-radius: 8px;
  padding: 1rem;
}
/* Modal */
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

.modal-content {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  max-width: 320px;
  width: 90%;
  text-align: center;
}

.modal-message {
  font-size: 1rem;
  color: #333;
  margin: 0 0 1.5rem;
  line-height: 1.5;
}

.modal-btn {
  width: 100%;
  padding: 0.875rem;
  background: #BFE7DF;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: #004d40;
  cursor: pointer;
}

.modal-btn:hover {
  background: #a8ddd2;
}

/* Number Input Fix */
input[type="number"] {
  width: 100%;
  padding: 0.875rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.95rem;
  background: white;
  box-sizing: border-box;
  -moz-appearance: textfield;
}

input[type="number"]::-webkit-outer-spin-button,
input[type="number"]::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

input[type="number"]:focus {
  outline: none;
  border-color: #BFE7DF;
}

/* Room Amenities Section */
.room-amenities-section {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e0e0e0;
}

.room-amenities-title {
  font-size: 1rem;
  font-weight: 700;
  color: #222;
  margin: 0 0 1rem;
}

.room-amenity-category {
  margin-bottom: 1.25rem;
}

.room-amenity-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #555;
  margin-bottom: 0.5rem;
}

.room-amenity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.room-amenity-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.85rem;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.room-amenity-tag input[type="checkbox"] {
  width: 16px;
  height: 16px;
  margin-right: 0.4rem;
  accent-color: #BFE7DF;
}

.room-amenity-tag:hover {
  border-color: #BFE7DF;
}

.room-amenity-tag.selected {
  border-color: #BFE7DF;
  background: #f0fcfa;
}

/* ========== Verification Step ========== */
.verification-step {
  padding: 2rem 1rem;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}

.verification-card {
  background: white;
  border-radius: 20px;
  padding: 2.5rem;
  max-width: 500px;
  width: 100%;
  text-align: center;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
}

.verification-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #333;
  margin: 0 0 0.5rem;
}

.verification-desc {
  font-size: 0.95rem;
  color: #666;
  margin: 0 0 2rem;
}

.license-upload-area {
  margin-bottom: 1.5rem;
}

.upload-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 2rem;
  border: 2px dashed #ccc;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

/* Room Image Upload Styles */
.room-image-upload-area {
  width: 100%;
}

.room-upload-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 150px;
  border: 2px dashed #BFE7DF;
  border-radius: 12px;
  background: #f8fffe;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-box:hover {
  border-color: #BFE7DF;
  background: #f9fcfb;
}

.upload-text {
  font-size: 1rem;
  color: #666;
}

.license-preview {
  position: relative;
  display: inline-block;
}

.license-preview img {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.license-preview .remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #ff5252;
  color: white;
  border: none;
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.room-upload-box:hover {
  background: #f0fbf9;
  border-color: #8fd4c7;
}

.hidden-file-input {
  display: none;
}

.room-upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.room-upload-icon {
  font-size: 2rem;
}

.room-upload-text {
  font-size: 0.95rem;
  font-weight: 600;
  color: #333;
}

.room-upload-hint {
  font-size: 0.8rem;
  color: #888;
}

.room-image-preview {
  position: relative;
  width: 100%;
  max-width: 200px;
}

.room-image-preview img {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid #e0e0e0;
}

.room-remove-image-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border-radius: 50%;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.extracted-text-box {
  background: #f9f9f9;
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 1.5rem;
  text-align: left;
}

.extracted-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #555;
  margin: 0 0 0.5rem;
}

.extracted-content {
  font-size: 0.9rem;
  color: #333;
  margin: 0;
  white-space: pre-wrap;
  font-family: inherit;
}

.verification-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.btn-extract,
.btn-verify {
  padding: 0.875rem 1.5rem;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-extract {
  background: #00875A;
  color: white;
}

.btn-extract:hover:not(:disabled) {
  background: #006644;
}

.btn-verify {
  background: #BFE7DF;
  color: #004d40;
}

.btn-verify:hover:not(:disabled) {
  background: #a8ddd2;
}

.btn-extract:disabled,
.btn-verify:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.room-remove-image-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

/* ========== 유효성 검사 에러 스타일 ========== */
.error-message {
  display: block;
  color: #e53935;
  font-size: 0.8rem;
  margin-top: 0.4rem;
  padding-left: 0.2rem;
}

.input-error {
  border-color: #e53935 !important;
  background-color: #fff5f5 !important;
}

.input-error:focus {
  border-color: #e53935 !important;
  box-shadow: 0 0 0 2px rgba(229, 57, 53, 0.2);
}

.upload-error {
  border-color: #e53935 !important;
  background-color: #fff5f5 !important;
}

.no-rooms-error {
  color: #e53935;
  border: 1px dashed #e53935;
  border-radius: 8px;
  background-color: #fff5f5;
}

/* 객실 폼 에러 스타일 */
.room-form .input-error {
  border-color: #e53935 !important;
}

.room-form .error-message {
  display: block;
  margin-top: 0.3rem;
}

/* 입력 필드 에러 시 흔들림 애니메이션 */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

.input-error {
  animation: shake 0.3s ease-in-out;
}

/* Readonly Input */
.readonly-input {
  background-color: #f5f5f5 !important;
  color: #666 !important;
  cursor: not-allowed;
}

/* Readonly Section */
.readonly-section {
  opacity: 0.7;
  pointer-events: none;
}

/* Disabled Checkbox & Tag */
.amenity-checkbox.disabled,
.theme-tag.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

/* Room Header */
.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.room-header .room-name {
  margin: 0;
}

.room-header .room-toggle {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.toggle-label-small {
  font-size: 0.8rem;
  color: #666;
}

/* Detail Row */
.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 0.25rem 0;
  font-size: 0.85rem;
}

.detail-label {
  color: #888;
}

.detail-value {
  color: #333;
  font-weight: 500;
}

/* Form Row Three Col */
.form-row.three-col {
  display: flex;
  gap: 1rem;
}

.form-row.three-col > * {
  flex: 1;
}

/* Margin helpers */
.mb-2 {
  margin-bottom: 0.5rem;
}

.mt-4 {
  margin-top: 1rem;
}

/* Banner Upload */
.banner-upload-area {
  width: 100%;
}

.banner-preview-wrapper {
  position: relative;
  width: 100%;
}

/* Remove Button */
.remove-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.remove-btn:hover {
  background: rgba(0, 0, 0, 0.7);
}

.upload-icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

/* Detail Images */
.detail-images-container {
  width: 100%;
}

.add-detail-image {
  width: 100px;
  height: 100px;
  border: 2px dashed #BFE7DF;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 2rem;
  color: #BFE7DF;
  transition: all 0.2s;
  padding-bottom: 4px; /* Centering tweak */
  line-height: 1;
}

.add-detail-image:hover {
  background: #f0fcfa;
}

.add-detail-image input {
  display: none;
}

/* 기본정보 섹션 수정 불가 필드 추가 라벨 */
.form-group label .readonly-badge {
  font-size: 0.75rem;
  color: #888;
  margin-left: 0.5rem;
}

/* Room Form Inline */
.room-form-inline {
  padding: 1rem;
  background: #f9fffe;
  border-radius: 8px;
}

.room-form-inline .room-form-title {
  font-size: 1rem;
  font-weight: 700;
  color: #222;
  margin: 0 0 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #e0e0e0;
}

/* Room Status Toggle */
.room-status-toggle {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0;
}

/* Room Form Actions */
.room-form-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid #e0e0e0;
}

.room-form-actions .btn-outline,
.room-form-actions .btn-primary {
  flex: 1;
  padding: 0.75rem;
}


.kakao-map {
  width: 100%;
  height: 400px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}
/* Image AI Layout */
.image-ai-container {
  margin-bottom: 2rem;
}

.image-grid-layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-top: 1rem;
}

@media (max-width: 768px) {
  .image-grid-layout {
    grid-template-columns: 1fr;
  }
}

/* Banner Area */
.banner-upload-area {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 12px;
  overflow: hidden;
  background-color: #f8fafc;
  border: 2px dashed #cbd5e1;
  transition: all 0.2s;
}

.banner-upload-area:hover {
  border-color: #94a3b8;
  background-color: #f1f5f9;
}

.banner-upload-area.has-error {
  border-color: #ef4444;
}

.banner-preview-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
}

.banner-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.badge-banner {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(0,0,0,0.6);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 500;
}

/* Detail Grid */
.detail-upload-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 10px;
  height: 100%;
}

.detail-image-item {
  position: relative;
  width: 100%;
  height: 100%;
  aspect-ratio: 1; /* Square */
  border-radius: 8px;
  overflow: hidden;
}

.detail-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Placeholders */
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  cursor: pointer;
  color: #64748b;
}

.banner-placeholder {
  height: 100%;
}

.detail-placeholder {
  background-color: #f8fafc;
  border: 2px dashed #cbd5e1;
  border-radius: 8px;
  aspect-ratio: 1;
  transition: all 0.2s;
}

.detail-placeholder:hover {
  background-color: #f1f5f9;
  border-color: #94a3b8;
}

.placeholder-content {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.placeholder-content .icon {
  font-size: 1.5rem;
  margin-bottom: 4px;
}

.placeholder-content .text {
  font-weight: 600;
  font-size: 0.95rem;
}

.placeholder-content .sub-text {
  font-size: 0.8rem;
  opacity: 0.8;
}

/* AI Action Area */
.ai-action-area {
  margin-top: 1.5rem;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.ai-magic-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%); /* Indigo theme */
  color: white;
  border: none;
  padding: 12px 20px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ai-magic-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.4);
}

.ai-magic-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.ai-magic-btn:disabled {
  opacity: 0.7;
  cursor: wait;
}

.ai-hint {
  font-size: 0.85rem;
  color: #64748b;
  margin-left: 4px;
}

</style>
