<script setup>
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getAccessToken, getCurrentUser, saveUserInfo } from '../../api/authClient'
import { requestAccommodationAiSuggestion } from '@/api/ai'
import { resizeImage } from '@/utils/imageUtils'

const router = useRouter()
const emit = defineEmits(['cancel', 'submit'])

// API Base URL (프록시 사용: /api -> http://localhost:8080/api)
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// ========== Business License Verification ==========
const isVerified = ref(false)
const businessLicenseImage = ref(null)
const businessLicenseFile = ref(null)
const businessLicensePreview = ref(null)
const businessRegistrationNumber = ref('')
const extractedText = ref('')
const isExtracting = ref(false)
const isVerifying = ref(false)
const isSubmitting = ref(false)
const isAiSuggesting = ref(false)

const handleLicenseUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    businessLicenseFile.value = file
    businessLicenseImage.value = file
    businessLicensePreview.value = URL.createObjectURL(file)
  }
}

// 사업자번호 유효성 검증 (체크섬 알고리즘)
const validateBusinessNumber = (bizNum) => {
  const numbers = bizNum.replace(/[^0-9]/g, '')
  if (numbers.length !== 10) return false

  const checkSum = [1, 3, 7, 1, 3, 7, 1, 3, 5]
  let sum = 0

  for (let i = 0; i < 9; i++) {
    sum += parseInt(numbers[i]) * checkSum[i]
  }
  sum += Math.floor((parseInt(numbers[8]) * 5) / 10)

  const remainder = (10 - (sum % 10)) % 10
  return remainder === parseInt(numbers[9])
}

// OCR로 사업자등록증 이미지에서 사업자번호 추출 및 검증
const verifyBusinessNumber = async () => {
  if (!businessLicenseImage.value) {
    openModal('사업자등록증 이미지를 먼저 선택해주세요.')
    return
  }

  isVerifying.value = true

  try {
    // 이미지를 Base64로 변환
    const reader = new FileReader()

    const base64Image = await new Promise((resolve, reject) => {
      reader.onload = () => resolve(reader.result)
      reader.onerror = reject
      reader.readAsDataURL(businessLicenseFile.value)
    })

    // 백엔드 OCR API 호출 (Google Cloud Vision)
    const token = getAccessToken()
    const response = await fetch(`${API_BASE_URL}/ocr/business-license`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        image: base64Image
      })
    })

    if (response.ok) {
      const data = await response.json()

      if (data.success && data.businessNumber) {
        // OCR 성공 - 사업자번호 추출됨
        businessRegistrationNumber.value = data.businessNumber
        isVerified.value = true

        let message = '사업자등록증이 확인되었습니다.'
        if (data.businessName) {
          message += `\n상호: ${data.businessName}`
        }
        if (data.representative) {
          message += `\n대표자: ${data.representative}`
        }
        message += '\n\n이제 숙소를 등록할 수 있습니다.'
        openModal(message)
      } else {
        // OCR 실패 - 에러 메시지 표시
        const errorMsg = data.errorMessage || '사업자번호를 인식할 수 없습니다.'
        openModal(`사업자등록 실패: ${errorMsg}`)
      }
    } else {
      // HTTP 오류
      openModal('사업자등록 실패: 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')
    }
  } catch (error) {
    console.error('사업자등록증 확인 오류:', error)
    openModal('사업자등록 실패: 네트워크 오류가 발생했습니다.')
  } finally {
    isVerifying.value = false
  }
}

// 카카오맵 관련
const mapContainer = ref(null)
const map = ref(null)
const marker = ref(null)
const geocoder = ref(null)

// 숙소 유형 매핑 (프론트엔드 표시명 -> 백엔드 ENUM)
const accommodationCategoryMap = {
  '게스트하우스': 'GUESTHOUSE',
  '펜션': 'PENSION',
  '호텔': 'HOTEL',
  '모텔': 'MOTEL',
  '리조트': 'RESORT',
  '한옥': 'HANOK',
  '캠핑/글램핑': 'CAMPING'
}

// Form data
const form = ref({
  // 기본정보
  name: '',
  type: '',
  description: '',
  phone: '',
  email: '',
  sns: '',
  // 위치정보
  city: '',
  district: '',
  township: '',
  address: '',
  latitude: null,
  longitude: null,
  // 교통정보
  transportInfo: '',
  // 운영 정책
  checkInTime: '15:00',
  checkOutTime: '11:00',
  cancelPolicy: '',
  // 정책 & 규정
  houseRules: '',
  // 주차정보
  parkingInfo: '',
  // 편의시설
  amenities: [],
  // 이미지
  bannerImage: null,
  bannerImageFile: null,
  detailImages: [],
  detailImageFiles: [],
  // 검색 최적화
  shortDescription: '',
  // 테마
  themes: [],
  // 정산 계좌
  bankName: '',
  accountHolder: '',
  accountNumber: '',
  // 상태
  isActive: true
})

// Time Picker Logic
const hourOptions = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'))
const minuteOptions = ['00', '30']

const checkInHour = computed({
  get: () => form.value.checkInTime ? form.value.checkInTime.split(':')[0] : '15',
  set: (val) => {
    const min = checkInMinute.value
    form.value.checkInTime = `${val}:${min}`
  }
})

const checkInMinute = computed({
  get: () => form.value.checkInTime ? form.value.checkInTime.split(':')[1] : '00',
  set: (val) => {
    const hour = checkInHour.value
    form.value.checkInTime = `${hour}:${val}`
  }
})

const checkOutHour = computed({
  get: () => form.value.checkOutTime ? form.value.checkOutTime.split(':')[0] : '11',
  set: (val) => {
    const min = checkOutMinute.value
    form.value.checkOutTime = `${val}:${min}`
  }
})

const checkOutMinute = computed({
  get: () => form.value.checkOutTime ? form.value.checkOutTime.split(':')[1] : '00',
  set: (val) => {
    const hour = checkOutHour.value
    form.value.checkOutTime = `${hour}:${val}`
  }
})

// ========== 유효성 검사 ==========
const errors = ref({})

// 이메일 형식 검증 (도메인 필수: .com, .co.kr, .net 등)
const isValidEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.(com|co\.kr|net|org|kr|io|me|info|biz|ac\.kr|go\.kr)$/i
  return emailRegex.test(email)
}

// 전화번호 형식 검증 (다양한 형식 허용)
// 010-XXXX-XXXX (11자리), 02-XXX-XXXX (9~10자리), 0507-XXXX-XXXX (12자리) 등
const isValidPhone = (phone) => {
  const digitsOnly = phone.replace(/[^0-9]/g, '')
  // 9자리(02 지역번호) ~ 12자리(0507 인터넷전화) 허용
  return digitsOnly.length >= 9 && digitsOnly.length <= 12
}

// 예금주 형식 검증 (한글 완성형 또는 영어만 허용, 자음/모음만 불가)
const isValidAccountHolder = (name) => {
  // 한글 완성형(가-힣) 또는 영문(a-zA-Z)과 공백만 허용
  const validNameRegex = /^[가-힣a-zA-Z\s]+$/
  // 자음만 있는지 체크 (ㄱ-ㅎ)
  const consonantOnly = /^[ㄱ-ㅎ\s]+$/
  // 모음만 있는지 체크 (ㅏ-ㅣ)
  const vowelOnly = /^[ㅏ-ㅣ\s]+$/
  // 자음이나 모음이 포함되어 있는지 체크
  const hasConsonantOrVowel = /[ㄱ-ㅎㅏ-ㅣ]/

  if (!validNameRegex.test(name)) return false
  if (consonantOnly.test(name) || vowelOnly.test(name)) return false
  if (hasConsonantOrVowel.test(name)) return false

  return true
}

// 숫자만 입력되도록 필터링
const filterNumberInput = (event) => {
  const value = event.target.value
  event.target.value = value.replace(/[^0-9]/g, '')
}

// 필드별 유효성 검사
const validateField = (fieldName, value) => {
  switch (fieldName) {
    case 'name':
      if (!value || !value.trim()) {
        errors.value.name = '숙소명을 입력해주세요.'
      } else {
        delete errors.value.name
      }
      break
    case 'type':
      if (!value) {
        errors.value.type = '숙소유형을 선택해주세요.'
      } else {
        delete errors.value.type
      }
      break
    case 'description':
      if (!value || !value.trim()) {
        errors.value.description = '숙소 소개를 입력해주세요.'
      } else {
        delete errors.value.description
      }
      break
    case 'phone':
      if (!value || !value.trim()) {
        errors.value.phone = '연락처를 입력해주세요.'
      } else if (!isValidPhone(value)) {
        errors.value.phone = '올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678, 02-123-4567)'
      } else {
        delete errors.value.phone
      }
      break
    case 'email':
      if (!value || !value.trim()) {
        errors.value.email = '이메일을 입력해주세요.'
      } else if (!isValidEmail(value)) {
        errors.value.email = '올바른 이메일 형식이 아닙니다. (예: example@email.com)'
      } else {
        delete errors.value.email
      }
      break
    case 'city':
      if (!value || !value.trim()) {
        errors.value.city = '시/도를 입력해주세요.'
      } else {
        delete errors.value.city
      }
      break
    case 'district':
      if (!value || !value.trim()) {
        errors.value.district = '구/군을 입력해주세요.'
      } else {
        delete errors.value.district
      }
      break
    case 'address':
      if (!value || !value.trim()) {
        errors.value.address = '상세주소를 입력해주세요.'
      } else {
        delete errors.value.address
      }
      break
    case 'bankName':
      if (!value) {
        errors.value.bankName = '은행을 선택해주세요.'
      } else {
        delete errors.value.bankName
      }
      break
    case 'accountHolder':
      if (!value || !value.trim()) {
        errors.value.accountHolder = '예금주를 입력해주세요.'
      } else if (!isValidAccountHolder(value)) {
        errors.value.accountHolder = '예금주는 한글 또는 영문만 입력해주세요.'
      } else {
        delete errors.value.accountHolder
      }
      break
    case 'accountNumber':
      if (!value || !value.trim()) {
        errors.value.accountNumber = '계좌번호를 입력해주세요.'
      } else if (!/^\d+$/.test(value)) {
        errors.value.accountNumber = '숫자만 입력해주세요.'
      } else {
        delete errors.value.accountNumber
      }
      break
  }
}

// 전체 폼 유효성 검사
const validateForm = () => {
  errors.value = {}
  let isValid = true
  const errorMessages = []

  // 기본정보 검사
  if (!form.value.name?.trim()) {
    errors.value.name = '숙소명을 입력해주세요.'
    errorMessages.push('숙소명')
    isValid = false
  }
  if (!form.value.type) {
    errors.value.type = '숙소유형을 선택해주세요.'
    errorMessages.push('숙소유형')
    isValid = false
  }
  if (!form.value.description?.trim()) {
    errors.value.description = '숙소 소개를 입력해주세요.'
    errorMessages.push('숙소 소개')
    isValid = false
  }
  if (!form.value.phone?.trim()) {
    errors.value.phone = '연락처를 입력해주세요.'
    errorMessages.push('연락처')
    isValid = false
  } else if (!isValidPhone(form.value.phone)) {
    errors.value.phone = '올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678, 02-123-4567)'
    errorMessages.push('전화번호 형식')
    isValid = false
  }
  if (!form.value.email?.trim()) {
    errors.value.email = '이메일을 입력해주세요.'
    errorMessages.push('이메일')
    isValid = false
  } else if (!isValidEmail(form.value.email)) {
    errors.value.email = '올바른 이메일 형식이 아닙니다. (예: example@email.com)'
    errorMessages.push('이메일 형식')
    isValid = false
  }

  // 위치정보 검사
  if (!form.value.city?.trim()) {
    errors.value.city = '시/도를 입력해주세요.'
    errorMessages.push('시/도')
    isValid = false
  }
  if (!form.value.district?.trim()) {
    errors.value.district = '구/군을 입력해주세요.'
    errorMessages.push('구/군')
    isValid = false
  }
  if (!form.value.address?.trim()) {
    errors.value.address = '상세주소를 입력해주세요.'
    errorMessages.push('상세주소')
    isValid = false
  }
  if (!form.value.latitude || !form.value.longitude) {
    errors.value.location = '위치 확인 버튼을 클릭하여 위치를 확인해주세요.'
    errorMessages.push('위치 확인')
    isValid = false
  }

  // 이미지 검사
  if (!form.value.bannerImageFile) {
    errors.value.bannerImage = '배너 이미지를 등록해주세요.'
    errorMessages.push('배너 이미지')
    isValid = false
  }

  // 객실 검사
  if (rooms.value.length === 0) {
    errors.value.rooms = '최소 1개의 객실을 등록해야 합니다.'
    errorMessages.push('객실')
    isValid = false
  }

  // 정산 계좌 검사
  if (!form.value.bankName) {
    errors.value.bankName = '은행을 선택해주세요.'
    errorMessages.push('은행명')
    isValid = false
  }
  if (!form.value.accountHolder?.trim()) {
    errors.value.accountHolder = '예금주를 입력해주세요.'
    errorMessages.push('예금주')
    isValid = false
  } else if (!isValidAccountHolder(form.value.accountHolder)) {
    errors.value.accountHolder = '예금주는 한글 또는 영문만 입력해주세요.'
    errorMessages.push('예금주 형식')
    isValid = false
  }
  if (!form.value.accountNumber?.trim()) {
    errors.value.accountNumber = '계좌번호를 입력해주세요.'
    errorMessages.push('계좌번호')
    isValid = false
    errors.value.accountNumber = '계좌번호는 숫자만 입력해주세요.'
    errorMessages.push('계좌번호 형식')
    isValid = false
  }

  // 테마 검사
  if (!form.value.themes || form.value.themes.length === 0) {
    // errors object might not have 'themes' key defined in ref, but Vue handles new props reactivity usually or we can rely on errorMessages
    // Assuming errors ref is just {}
    errorMessages.push('테마 (최소 1개 선택)')
    isValid = false
  }

  return { isValid, errorMessages }
}

// 객실 폼 유효성 검사
const roomErrors = ref({})

const validateRoomForm = () => {
  roomErrors.value = {}
  let isValid = true

  // 객실명 검사
  if (!roomForm.value.name?.trim()) {
    roomErrors.value.name = '객실명을 입력해주세요.'
    isValid = false
  }

  // 주중 요금 검사
  if (!roomForm.value.weekdayPrice && roomForm.value.weekdayPrice !== 0) {
    roomErrors.value.weekdayPrice = '주중 요금을 입력해주세요.'
    isValid = false
  } else if (isNaN(parseInt(roomForm.value.weekdayPrice)) || parseInt(roomForm.value.weekdayPrice) <= 0) {
    roomErrors.value.weekdayPrice = '0보다 큰 숫자를 입력해주세요.'
    isValid = false
  }

  // 주말 요금 검사
  if (!roomForm.value.weekendPrice && roomForm.value.weekendPrice !== 0) {
    roomErrors.value.weekendPrice = '주말 요금을 입력해주세요.'
    isValid = false
  } else if (isNaN(parseInt(roomForm.value.weekendPrice)) || parseInt(roomForm.value.weekendPrice) <= 0) {
    roomErrors.value.weekendPrice = '0보다 큰 숫자를 입력해주세요.'
    isValid = false
  }

  // 대표 이미지 검사
  if (!roomForm.value.representativeImage) {
    roomErrors.value.representativeImage = '객실 대표 이미지를 등록해주세요.'
    isValid = false
  }

  // 인원 검사
  const minGuests = parseInt(roomForm.value.minGuests) || 0
  const maxGuests = parseInt(roomForm.value.maxGuests) || 0

  if (roomForm.value.minGuests && minGuests <= 0) {
    roomErrors.value.minGuests = '1명 이상 입력해주세요.'
    isValid = false
  }

  if (roomForm.value.maxGuests && maxGuests <= 0) {
    roomErrors.value.maxGuests = '1명 이상 입력해주세요.'
    isValid = false
  }

  if (minGuests > 0 && maxGuests > 0 && minGuests > maxGuests) {
    roomErrors.value.maxGuests = '최대 인원은 최소 인원보다 크거나 같아야 합니다.'
    roomErrors.value.minGuests = '최소 인원은 최대 인원보다 작거나 같아야 합니다.'
    isValid = false
  }

  // 침대, 욕실 개수 검사 (음수 방지)
  if (roomForm.value.bedCount && parseInt(roomForm.value.bedCount) < 0) {
    roomErrors.value.bedCount = '0 이상의 숫자를 입력해주세요.'
    isValid = false
  }

  if (roomForm.value.bathroomCount && parseInt(roomForm.value.bathroomCount) < 0) {
    roomErrors.value.bathroomCount = '0 이상의 숫자를 입력해주세요.'
    isValid = false
  }

  return isValid
}

// 객실 폼 에러 초기화
const resetRoomErrors = () => {
  roomErrors.value = {}
}

// 은행 목록
const bankList = ['국민은행', '신한은행', '우리은행', '하나은행', '농협', '카카오뱅크', '토스뱅크', '기업은행']

// Accommodation types
const accommodationTypes = [
  '게스트하우스',
  '펜션',
  '호텔',
  '모텔',
  '리조트',
  '한옥',
  '캠핑/글램핑'
]


// 편의시설 옵션 (4개만)
const amenityOptions = [
  { id: 'wifi', label: '무선 인터넷' },
  { id: 'aircon', label: '에어컨' },
  { id: 'heating', label: '난방' },
  { id: 'tv', label: 'TV' }
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

const toggleAmenity = (id) => {
  const index = form.value.amenities.indexOf(id)
  if (index > -1) {
    form.value.amenities.splice(index, 1)
  } else {
    form.value.amenities.push(id)
  }
}

const toggleTheme = (theme) => {
  const index = form.value.themes.indexOf(theme)
  if (index > -1) {
    form.value.themes.splice(index, 1)
  } else {
    form.value.themes.push(theme)
  }
}

const handleBannerUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    form.value.bannerImage = URL.createObjectURL(file)
    form.value.bannerImageFile = file
  }
}

const handleRemoveBanner = () => {
  if (form.value.bannerImage) {
    URL.revokeObjectURL(form.value.bannerImage)
  }
  form.value.bannerImage = null
  form.value.bannerImageFile = null
}

const handleDetailImagesUpload = (event) => {
  const files = Array.from(event.target.files)
  const remainingSlots = 5 - form.value.detailImages.length
  const filesToAdd = files.slice(0, remainingSlots)

  filesToAdd.forEach(file => {
    form.value.detailImages.push(URL.createObjectURL(file))
    form.value.detailImageFiles.push(file)
  })
  
  // 입력값 초기화
  event.target.value = ''
}

const removeDetailImage = (index) => {
  const previewUrl = form.value.detailImages[index]
  if (previewUrl) {
    URL.revokeObjectURL(previewUrl)
  }
  form.value.detailImages.splice(index, 1)
  form.value.detailImageFiles.splice(index, 1)
}

// 모달 상태
const showModal = ref(false)
const modalMessage = ref('')
const registrationSuccess = ref(false)

const openModal = (message) => {
  modalMessage.value = message
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  if (registrationSuccess.value) {
    router.push('/host/accommodation')
  }
}

// 객실 관련
const rooms = ref([])
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

// 객실 이미지 업로드 처리
const handleRoomImageUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    if (!file.type.startsWith('image/')) {
      openModal('이미지 파일만 업로드 가능합니다.')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      openModal('파일 크기는 5MB 이하여야 합니다.')
      return
    }
    roomForm.value.representativeImage = file
    roomForm.value.representativeImagePreview = URL.createObjectURL(file)
  }
}

const removeRoomImage = () => {
  roomForm.value.representativeImage = null
  roomForm.value.representativeImagePreview = ''
}



const addRoom = () => {
  if (!validateRoomForm()) {
    const firstError = Object.values(roomErrors.value)[0]
    openModal(firstError || '객실 정보를 확인해주세요.')
    return
  }

  rooms.value.push({
    id: Date.now(),
    ...roomForm.value
  })

  // 폼 초기화
  roomForm.value = {
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
  }
  showRoomForm.value = false
  openModal('객실이 추가되었습니다.')
}

const deleteRoom = (id) => {
  rooms.value = rooms.value.filter(r => r.id !== id)
}

const toggleRoomActive = (id) => {
  const room = rooms.value.find(r => r.id === id)
  if (room) {
    room.isActive = !room.isActive
  }
}

// 카카오맵 SDK 로딩 대기
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

// 카카오맵 초기화
const initMap = async () => {
  await waitForKakao()

  window.kakao.maps.load(() => {
    const container = mapContainer.value
    if (!container) return

    const options = {
      center: new window.kakao.maps.LatLng(37.5665, 126.9780), // 서울시청 기본 좌표
      level: 3
    }

    map.value = new window.kakao.maps.Map(container, options)
    geocoder.value = new window.kakao.maps.services.Geocoder()

    // 마커 생성
    marker.value = new window.kakao.maps.Marker({
      position: options.center,
      map: map.value
    })
    marker.value.setVisible(false)

    // 지도 relayout (크기 재계산)
    setTimeout(() => {
      if (map.value) map.value.relayout()
    }, 100)
  })
}

// 주소로 좌표 검색
const handleLocationCheck = () => {
  // 주소 조합: 시/도 + 구/군 + 읍면동 + 상세주소
  const addressParts = [
    form.value.city,
    form.value.district,
    form.value.township,
    form.value.address
  ].filter(part => part && part.trim())

  const fullAddress = addressParts.join(' ').trim()

  if (!fullAddress || fullAddress.length < 5) {
    openModal('주소를 입력해주세요.')
    return
  }

  if (!geocoder.value) {
    openModal('지도가 초기화되지 않았습니다. 잠시 후 다시 시도해주세요.')
    return
  }

  geocoder.value.addressSearch(fullAddress, (result, status) => {
    if (status === window.kakao.maps.services.Status.OK) {
      const coords = new window.kakao.maps.LatLng(result[0].y, result[0].x)

      // 좌표 저장
      form.value.latitude = parseFloat(result[0].y)
      form.value.longitude = parseFloat(result[0].x)

      // 지도 중심 이동
      map.value.setCenter(coords)

      // 마커 위치 변경 및 표시
      marker.value.setPosition(coords)
      marker.value.setVisible(true)

      openModal('위치가 확인되었습니다!')
    } else {
      openModal('주소를 찾을 수 없습니다. 정확한 주소를 입력해주세요.')
    }
  })
}

// isVerified가 true가 되면 지도 초기화
watch(() => isVerified.value, async (newVal) => {
  if (newVal) {
    await nextTick()
    setTimeout(() => {
      initMap()
    }, 100)
  }
})





const handleTempSave = () => {
  // 로컬스토리지에 임시 저장
  const tempData = {
    form: form.value,
    rooms: rooms.value,
    businessRegistrationNumber: businessRegistrationNumber.value,
    savedAt: new Date().toISOString()
  }
  sessionStorage.setItem('accommodationDraft', JSON.stringify(tempData))
  openModal('임시 저장되었습니다.')
}

// 파일을 Base64로 변환하는 함수
const fileToBase64 = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

const getAllImageFiles = () => {
  const images = []
  if (form.value.bannerImageFile) {
    images.push(form.value.bannerImageFile)
  }
  if (form.value.detailImageFiles && form.value.detailImageFiles.length > 0) {
    images.push(...form.value.detailImageFiles)
  }
  return images
}

const applyAiSuggestion = async () => {
  if (isAiSuggesting.value) return
  const imageFiles = getAllImageFiles()
  if (imageFiles.length === 0) {
    openModal('AI 추천을 사용하려면 먼저 배너 또는 상세 이미지를 업로드해주세요.')
    return
  }
  isAiSuggesting.value = true
  try {
    const base64Images = await Promise.all(imageFiles.map(file => fileToBase64(file)))
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

// 편의시설 ID 매핑 (프론트 id -> 백엔드 amenityId)
const amenityIdMap = {
  'wifi': 1,
  'aircon': 2,
  'heating': 3,
  'tv': 4
}

// 테마 ID 매핑 (themeName -> themeId) - API에서 동적으로 생성
const getThemeId = (themeName) => {
  const theme = themeList.value.find(t => t.themeName === themeName)
  return theme ? theme.id : undefined
}

const handleSubmit = async () => {
  // 중복 제출 방지
  if (isSubmitting.value) return

  // 전체 폼 유효성 검사
  const { isValid, errorMessages } = validateForm()

  if (!isValid) {
    if (errorMessages.length <= 3) {
      openModal(`다음 항목을 확인해주세요:\n${errorMessages.join(', ')}`)
    } else {
      openModal(`입력되지 않은 필수 항목이 ${errorMessages.length}개 있습니다.\n빨간색으로 표시된 항목을 확인해주세요.`)
    }
    return
  }

  isSubmitting.value = true

  try {
    // 이미지 Base64 변환
    const images = []

    // 배너 이미지
    if (form.value.bannerImageFile) {
      images.push({
        imageUrl: await fileToBase64(form.value.bannerImageFile),
        imageType: 'banner',
        sortOrder: 0
      })
    }

    // 상세 이미지들
    for (let i = 0; i < form.value.detailImageFiles.length; i++) {
      images.push({
        imageUrl: await fileToBase64(form.value.detailImageFiles[i]),
        imageType: 'detail',
        sortOrder: i + 1
      })
    }

    // 사업자등록증 이미지 Base64 변환
    let businessRegistrationImageBase64 = ''
    if (businessLicenseFile.value) {
      businessRegistrationImageBase64 = await fileToBase64(businessLicenseFile.value)
    }

    // 객실 데이터 변환 (프론트엔드 -> 백엔드 형식) - 이미지 Base64 변환 포함
    const roomsData = await Promise.all(rooms.value.map(async (room) => {
      let mainImageBase64 = ''
      if (room.representativeImage) {
        mainImageBase64 = await fileToBase64(room.representativeImage)
      }
      return {
        roomName: room.name,
        price: parseInt(room.weekdayPrice) || 0,
        weekendPrice: parseInt(room.weekendPrice) || 0,
        minGuests: parseInt(room.minGuests) || 1,
        maxGuests: parseInt(room.maxGuests) || 2,
        roomDescription: room.description || '',
        mainImageUrl: mainImageBase64,
        bathroomCount: parseInt(room.bathroomCount) || 1,
        roomType: 'STANDARD',
        bedCount: parseInt(room.bedCount) || 1
      }
    }))

    // 편의시설 ID 변환
    const amenityIds = form.value.amenities
      .map(a => amenityIdMap[a])
      .filter(id => id !== undefined)

    // 테마 ID 변환
    const themeIds = form.value.themes
      .map(t => getThemeId(t))
      .filter(id => id !== undefined)

    // 백엔드 API 요청 데이터 구성
    const requestData = {
      // 정산계좌 정보 (백엔드에서 계좌 등록 후 숙소와 연결)
      bankName: form.value.bankName,
      accountNumber: form.value.accountNumber,
      accountHolder: form.value.accountHolder,
      // 숙소 정보
      accommodationsName: form.value.name,
      accommodationsCategory: accommodationCategoryMap[form.value.type] || 'GUESTHOUSE',
      accommodationsDescription: form.value.description,
      shortDescription: form.value.shortDescription || '',
      city: form.value.city,
      district: form.value.district,
      township: form.value.township || '',
      addressDetail: form.value.address,
      latitude: form.value.latitude,
      longitude: form.value.longitude,
      transportInfo: form.value.transportInfo || '',
      businessRegistrationNumber: businessRegistrationNumber.value.replace(/[^0-9]/g, ''),
      businessRegistrationImage: businessRegistrationImageBase64,
      parkingInfo: form.value.parkingInfo || '',
      sns: form.value.sns || '',
      phone: form.value.phone,
      checkInTime: form.value.checkInTime,
      checkOutTime: form.value.checkOutTime,
      // 편의시설/테마 (IDs)
      amenityIds,
      themeIds,
      rooms: roomsData,
      images: images  // 숙소 이미지 (배너, 디테일)
    }

    console.log('Submitting to API:', requestData)

    // API 호출
    const token = getAccessToken()
    const response = await fetch(`${API_BASE_URL}/accommodations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(requestData)
    })

    if (response.ok) {
      const accommodationId = await response.json()
      console.log('숙소 등록 완료, ID:', accommodationId)

      // 임시 저장 데이터 삭제
      sessionStorage.removeItem('accommodationDraft')

      const meResponse = await getCurrentUser()
      if (meResponse.ok && meResponse.data) {
        saveUserInfo(meResponse.data)
      }

      registrationSuccess.value = true
      openModal('숙소가 성공적으로 등록되었습니다.')
    } else if (response.status === 401) {
      alert('로그인 세션이 만료되었습니다. 다시 로그인해주세요.')
      router.push('/login')
    } else {
      let errorData;
      try {
        errorData = await response.json();
      } catch (e) {
        errorData = await response.text(); // JSON 파싱 실패 시 텍스트로 읽기
      }
      console.error('숙소 등록 실패:', errorData)
      openModal('숙소 등록에 실패했습니다: ' + (typeof errorData === 'object' ? JSON.stringify(errorData) : errorData))
    }
  } catch (error) {
    console.error('숙소 등록 오류:', error)
    openModal('숙소 등록 중 오류가 발생했습니다.')
  } finally {
    isSubmitting.value = false
  }
}

// 컴포넌트 마운트 시 테마 로드
onMounted(() => {
  loadThemes()
})
</script>

<template>
  <div class="register-page">
    <!-- Page Header (Only after verification) -->
    <div v-if="isVerified" class="page-header">
      <div class="header-top">
        <div class="title-area">
          <h1>숙소 등록</h1>
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
      
      <!-- Progress Bar -->
      <div class="progress-wrapper">
        <div class="progress-bar">
          <div class="progress-fill" style="width: 30%"></div>
        </div>
        <span class="progress-text">진행 중</span>
      </div>

      <!-- Toggle & Actions -->
      <div class="header-controls">
        <div class="toggle-wrapper">
          <span class="toggle-label">숙소 운영</span>
          <div class="toggle-switch" :class="{ active: form.isActive }" @click="form.isActive = !form.isActive">
            <div class="toggle-slider"></div>
          </div>
        </div>
        
        <div class="action-buttons">
          <button class="btn-primary" @click="handleSubmit" :disabled="isSubmitting">{{ isSubmitting ? '등록 중...' : '저장하기' }}</button>
        </div>
      </div>
    </div>

    <!-- ========== Verification Step ========== -->
    <div v-if="!isVerified" class="verification-step">
      <div class="verification-card">
        <h2 class="verification-title">사업자등록증 확인</h2>
        <p class="verification-desc">사업자등록증 이미지를 업로드하여 사업자번호를 확인하세요.</p>
        
        <div class="license-upload-area">
          <div v-if="businessLicensePreview" class="license-preview">
            <img :src="businessLicensePreview" alt="사업자등록증" />
            <button class="remove-btn" @click="businessLicensePreview = null; businessLicenseImage = null; businessLicenseFile = null">&times;</button>
          </div>
          <label v-else class="upload-box">
            <input type="file" accept="image/*" @change="handleLicenseUpload" hidden />
            <span class="upload-text">사업자등록증 이미지 선택</span>
          </label>
        </div>

        <div class="verification-actions">
          <button
            class="btn-verify"
            @click="verifyBusinessNumber"
            :disabled="isVerifying || !businessLicenseImage"
          >
            {{ isVerifying ? '확인 중...' : '사업자등록증 확인' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ========== Registration Form (Only after verification) ========== -->
    <div v-else class="form-content">
      <!-- Section: 숙소 등록 -->
      <section class="form-section">
        <h2 class="section-title">숙소 등록</h2>
        <p class="section-desc">숙소의 기본 정보를 입력해주세요.</p>


    <!-- Image & AI Section -->
    <div class="image-ai-container">
      <!-- Section: Images (Restructured) -->
      <section class="form-section image-section">
        <h3 class="subsection-title">숙소 이미지 <span class="required">*</span></h3>
        <p class="section-desc">멋진 숙소 사진을 올려주세요. AI가 사진을 분석해 소개글을 만들어드려요.</p>

        <div class="image-grid-layout">
          <!-- Banner Image (Large) -->
          <div class="banner-upload-area" :class="{ 'has-error': errors.bannerImage }">
            <div v-if="form.bannerImage" class="banner-preview-wrapper">
              <img :src="form.bannerImage" class="banner-preview" />
              <button type="button" class="remove-image-btn" @click="handleRemoveBanner" aria-label="대표 이미지 삭제">
                ×
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
            <div v-for="(img, idx) in form.detailImages" :key="idx" class="detail-image-item">
              <img :src="img" />
              <button type="button" class="remove-image-btn" @click="removeDetailImage(idx)" aria-label="상세 이미지 삭제">
                ×
              </button>
            </div>
            
            <label v-if="form.detailImages.length < 5" class="upload-placeholder detail-placeholder">
              <input type="file" accept="image/*" multiple @change="handleDetailImagesUpload" hidden />
              <div class="placeholder-content">
                <span class="icon">＋</span>
                <span class="text">추가</span>
              </div>
            </label>
          </div>
        </div>
        <span v-if="errors.bannerImage" class="error-text">{{ errors.bannerImage }}</span>
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
          <input
            v-model="form.name"
            type="text"
            placeholder="숙소 이름을 입력하세요"
            :class="{ 'input-error': errors.name }"
            @input="validateField('name', form.name)"
          />
          <span v-if="errors.name" class="error-message">{{ errors.name }}</span>
        </div>

        <div class="form-group">
          <label>숙소유형 <span class="required">*</span></label>
          <select
            v-model="form.type"
            :class="{ 'input-error': errors.type }"
            @change="validateField('type', form.type)"
          >
            <option value="" disabled>선택해주세요</option>
            <option v-for="type in accommodationTypes" :key="type" :value="type">
              {{ type }}
            </option>
          </select>
          <span v-if="errors.type" class="error-message">{{ errors.type }}</span>
        </div>

        <div class="form-group">
          <label>숙소 소개(상세설명) <span class="required">*</span></label>
          <textarea
            v-model="form.description"
            rows="5"
            placeholder="숙소의 매력 포인트, 주변 환경, 호스팅 스타일 등을 상세히 적어주세요."
            :class="{ 'input-error': errors.description }"
            @input="validateField('description', form.description)"
          ></textarea>
          <span v-if="errors.description" class="error-message">{{ errors.description }}</span>
        </div>

        <div class="form-group">
          <label>대표 연락처 <span class="required">*</span></label>
          <input
            v-model="form.phone"
            type="tel"
            placeholder="010-1234-5678"
            :class="{ 'input-error': errors.phone }"
            @input="validateField('phone', form.phone)"
          />
          <span v-if="errors.phone" class="error-message">{{ errors.phone }}</span>
        </div>

        <div class="form-group">
          <label>이메일 주소 <span class="required">*</span></label>
          <input
            v-model="form.email"
            type="email"
            placeholder="example@email.com"
            :class="{ 'input-error': errors.email }"
            @input="validateField('email', form.email)"
          />
          <span v-if="errors.email" class="error-message">{{ errors.email }}</span>
        </div>

        <div class="form-group">
          <label>SNS</label>
          <input
            v-model="form.sns"
            type="text"
            placeholder="예: @guesthouse_official (인스타그램, 블로그 등)"
          />
        </div>
      </section>

      <!-- Section: 위치 정보 -->
      <section class="form-section">
        <h3 class="subsection-title">위치 정보</h3>

        <div class="form-group">
          <label>시/도 <span class="required">*</span></label>
          <input
            v-model="form.city"
            type="text"
            placeholder="예: 서울특별시"
            :class="{ 'input-error': errors.city }"
            @input="validateField('city', form.city)"
          />
          <span v-if="errors.city" class="error-message">{{ errors.city }}</span>
        </div>

        <div class="form-group">
          <label>구/군 <span class="required">*</span></label>
          <input
            v-model="form.district"
            type="text"
            placeholder="예: 강남구"
            :class="{ 'input-error': errors.district }"
            @input="validateField('district', form.district)"
          />
          <span v-if="errors.district" class="error-message">{{ errors.district }}</span>
        </div>

        <div class="form-group">
          <label>읍/면/동</label>
          <input
            v-model="form.township"
            type="text"
            placeholder="예: 역삼동"
          />
        </div>

        <div class="form-group">
          <label>상세주소 <span class="required">*</span></label>
          <input
            v-model="form.address"
            type="text"
            placeholder="예: 테헤란로 123, 456호"
            :class="{ 'input-error': errors.address }"
            @input="validateField('address', form.address)"
          />
          <span v-if="errors.address" class="error-message">{{ errors.address }}</span>
        </div>

        <button class="btn-location" @click="handleLocationCheck">위치 확인</button>
        <span v-if="errors.location" class="error-message">{{ errors.location }}</span>
      </section>

      <!-- Section: 지도 -->
      <section class="form-section">
        <h3 class="subsection-title">지도 <span class="required">*</span></h3>

        <div ref="mapContainer" class="kakao-map"></div>
        <p class="help-text">위치 확인 버튼을 클릭하면 지도에 마커가 표시됩니다</p>
      </section>

      <!-- Section: 교통 및 주변 정보 -->
      <section class="form-section">
        <h3 class="subsection-title">교통 및 주변 정보</h3>
        
        <div class="form-group">
          <textarea 
            v-model="form.transportInfo" 
            rows="4"
            placeholder="지하철역, 버스정류장, 주요 교통편, 길 안내, 랜드마크, 주변 관광지 정보를 입력해주세요."
          ></textarea>
        </div>
      </section>

      <!-- Section: 운영 정책 정보 -->
      <section class="form-section">
        <h3 class="subsection-title">체크인 정보</h3>
        
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
        </div>
      </section>

      <!-- Section: 정책 & 규정 (하우스 룰) -->
      <section class="form-section">
        <h3 class="subsection-title">정책 & 규정 (하우스 룰)</h3>
        
        <div class="form-group">
          <textarea 
            v-model="form.houseRules" 
            rows="4"
            placeholder="소음 제한, 파티 금지, 흡연 금지 등 숙소 이용 규정을 입력해주세요."
          ></textarea>
        </div>
      </section>

      <!-- Section: 주차정보 -->
      <section class="form-section">
        <h3 class="subsection-title">주차정보</h3>
        
        <div class="form-group">
          <label>주차정보</label>
          <textarea 
            v-model="form.parkingInfo" 
            rows="4"
            placeholder="주차 가능 여부, 주차 요금, 주차장 위치, 예약 방법 등을 상세히 입력해주세요."
          ></textarea>
        </div>
      </section>

      <!-- Section: 편의 시설 정보 -->
      <section class="form-section">
        <h3 class="subsection-title">편의 시설 정보</h3>
        
        <div class="amenities-grid">
          <label 
            v-for="amenity in amenityOptions" 
            :key="amenity.id" 
            class="amenity-checkbox"
            :class="{ checked: form.amenities.includes(amenity.id) }"
          >
            <input 
              type="checkbox" 
              :checked="form.amenities.includes(amenity.id)"
              @change="toggleAmenity(amenity.id)"
            />
            <span class="checkmark"></span>
            <span class="amenity-label">{{ amenity.label }}</span>
          </label>
        </div>
      </section>

      <!-- Section: 검색 최적화 정보 -->
      <section class="form-section">
        <h3 class="subsection-title">검색 최적화 정보</h3>
        
        <div class="form-group">
          <label>한 줄 설명 (짧은 숙소 소개 문구)</label>
          <input 
            v-model="form.shortDescription" 
            type="text" 
            placeholder="예: 바다 전망이 있는 포근한 스튜디오"
          />
        </div>
      </section>

      <!-- Section: 테마 (중복 선택 가능) -->
      <section class="form-section">
        <h3 class="subsection-title">테마 (최대 6개선택 가능)</h3>
        
        <div v-for="(category, key) in themeOptions" :key="key" class="theme-category">
          <div class="theme-category-title">
            {{ category.label }}
          </div>
          <div class="theme-tags">
            <label 
              v-for="item in category.items" 
              :key="item" 
              class="theme-tag"
              :class="{ selected: form.themes.includes(item) }"
            >
              <input 
                type="checkbox" 
                :checked="form.themes.includes(item)"
                @change="toggleTheme(item)"
              />
              {{ item }}
            </label>
          </div>
        </div>
        
        <p class="help-text">여러 테마를 선택할 수 있습니다</p>
      </section>

      <!-- Section: 정산 계좌 -->
      <section class="form-section">
        <h3 class="subsection-title">정산 계좌</h3>

        <div class="form-group">
          <label>은행명 <span class="required">*</span></label>
          <select
            v-model="form.bankName"
            :class="{ 'input-error': errors.bankName }"
            @change="validateField('bankName', form.bankName)"
          >
            <option value="" disabled>선택해주세요</option>
            <option v-for="bank in bankList" :key="bank" :value="bank">
              {{ bank }}
            </option>
          </select>
          <span v-if="errors.bankName" class="error-message">{{ errors.bankName }}</span>
        </div>

        <div class="form-group">
          <label>예금주 <span class="required">*</span></label>
          <input
            v-model="form.accountHolder"
            type="text"
            placeholder="예금주명을 입력해주세요"
            :class="{ 'input-error': errors.accountHolder }"
            @input="validateField('accountHolder', form.accountHolder)"
          />
          <span v-if="errors.accountHolder" class="error-message">{{ errors.accountHolder }}</span>
        </div>

        <div class="form-group">
          <label>계좌번호 <span class="required">*</span></label>
          <input
            v-model="form.accountNumber"
            type="text"
            placeholder="'-' 없이 숫자만 입력"
            :class="{ 'input-error': errors.accountNumber }"
            @input="(e) => { filterNumberInput(e); validateField('accountNumber', form.accountNumber) }"
          />
          <span v-if="errors.accountNumber" class="error-message">{{ errors.accountNumber }}</span>
        </div>
      </section>

      <!-- Section: 등록된 객실 -->
      <section class="form-section">
        <h3 class="subsection-title" style="color: #BFE7DF;">등록된 객실</h3>
        
        <!-- 등록된 객실 목록 -->
        <div v-if="rooms.length > 0" class="room-list">
          <div v-for="room in rooms" :key="room.id" class="room-card">
            <div class="room-info">
              <h4 class="room-name">{{ room.name }}</h4>
              <div class="room-details">
              <div class="detail-row">
                <span class="detail-label">주중 요금</span>
                <span class="detail-value">₩{{ Number(room.weekdayPrice).toLocaleString() }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">주말 요금 (금~토)</span>
                <span class="detail-value">₩{{ Number(room.weekendPrice).toLocaleString() }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">인원</span>
                <span class="detail-value">{{ room.minGuests }}~{{ room.maxGuests }}명</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">침대/욕실</span>
                <span class="detail-value">침대 {{ room.bedCount }}개 | 욕실 {{ room.bathroomCount }}개</span>
              </div>
            </div>
              <div class="room-toggle">
                <span>{{ room.isActive ? 'ON' : 'OFF' }}</span>
                <div 
                  class="toggle-switch small" 
                  :class="{ active: room.isActive }"
                  @click="toggleRoomActive(room.id)"
                >
                  <div class="toggle-slider"></div>
                </div>
              </div>
            </div>
            <div class="room-actions">
              <button class="room-btn" @click="deleteRoom(room.id)">삭제</button>
            </div>
          </div>
        </div>
        
        <p v-else class="no-rooms" :class="{ 'no-rooms-error': errors.rooms }">
          등록된 객실이 없습니다.
          <span v-if="errors.rooms" class="error-message">{{ errors.rooms }}</span>
        </p>
        
        <!-- 객실 추가 버튼 -->
        <button class="add-room-btn" @click="showRoomForm = true; resetRoomErrors()" v-if="!showRoomForm">
          + 객실 추가하기
        </button>
        
        <!-- 객실 추가 폼 -->
        <div v-if="showRoomForm" class="room-form">
          <h4 class="room-form-title">새 객실 정보</h4>

          <div class="form-group">
            <label>객실명 <span class="required">*</span></label>
            <input
              v-model="roomForm.name"
              type="text"
              placeholder="예: 스탠다드 더블룸"
              :class="{ 'input-error': roomErrors.name }"
            />
            <span v-if="roomErrors.name" class="error-message">{{ roomErrors.name }}</span>
          </div>

          <div class="form-group">
            <label>객실 대표 이미지 <span class="required">*</span></label>
            <div class="room-image-upload-area" :class="{ 'upload-error': roomErrors.representativeImage }">
              <div v-if="roomForm.representativeImagePreview" class="room-image-preview">
                <img :src="roomForm.representativeImagePreview" alt="객실 대표 이미지" />
                <button type="button" class="room-remove-image-btn" @click="removeRoomImage">
                  ✕
                </button>
              </div>
              <label v-else class="room-upload-box">
                <input
                  type="file"
                  accept="image/*"
                  @change="handleRoomImageUpload"
                  class="hidden-file-input"
                />
                <div class="room-upload-content">
                  <span class="room-upload-icon">📷</span>
                  <span class="room-upload-text">이미지 업로드</span>
                  <span class="room-upload-hint">JPG, PNG (최대 5MB)</span>
                </div>
              </label>
            </div>
            <span v-if="roomErrors.representativeImage" class="error-message">{{ roomErrors.representativeImage }}</span>
          </div>

          <div class="form-row two-col">
            <div class="form-group">
              <label>주중 요금 (일~목) <span class="required">*</span></label>
              <div class="input-with-unit">
                <input
                  v-model="roomForm.weekdayPrice"
                  type="number"
                  placeholder="50000"
                  :class="{ 'input-error': roomErrors.weekdayPrice }"
                  @input="filterNumberInput"
                  min="0"
                />
                <span class="unit">원</span>
              </div>
              <span v-if="roomErrors.weekdayPrice" class="error-message">{{ roomErrors.weekdayPrice }}</span>
            </div>
            <div class="form-group">
              <label>주말 요금 (금~토) <span class="required">*</span></label>
              <div class="input-with-unit">
                <input
                  v-model="roomForm.weekendPrice"
                  type="number"
                  placeholder="70000"
                  :class="{ 'input-error': roomErrors.weekendPrice }"
                  @input="filterNumberInput"
                  min="0"
                />
                <span class="unit">원</span>
              </div>
              <span v-if="roomErrors.weekendPrice" class="error-message">{{ roomErrors.weekendPrice }}</span>
            </div>
          </div>

          <div class="form-row two-col">
            <div class="form-group">
              <label>최소 인원</label>
              <input
                v-model="roomForm.minGuests"
                type="number"
                placeholder="명"
                :class="{ 'input-error': roomErrors.minGuests }"
                @input="filterNumberInput"
                min="1"
              />
              <span v-if="roomErrors.minGuests" class="error-message">{{ roomErrors.minGuests }}</span>
            </div>
            <div class="form-group">
              <label>최대 인원</label>
              <input
                v-model="roomForm.maxGuests"
                type="number"
                placeholder="명"
                :class="{ 'input-error': roomErrors.maxGuests }"
                @input="filterNumberInput"
                min="1"
              />
              <span v-if="roomErrors.maxGuests" class="error-message">{{ roomErrors.maxGuests }}</span>
            </div>
          </div>

          <div class="form-row two-col">
            <div class="form-group">
              <label>침대 개수</label>
              <input
                v-model="roomForm.bedCount"
                type="number"
                placeholder="개"
                :class="{ 'input-error': roomErrors.bedCount }"
                @input="filterNumberInput"
                min="0"
              />
              <span v-if="roomErrors.bedCount" class="error-message">{{ roomErrors.bedCount }}</span>
            </div>
            <div class="form-group">
              <label>욕실 개수</label>
              <input
                v-model="roomForm.bathroomCount"
                type="number"
                placeholder="개"
                :class="{ 'input-error': roomErrors.bathroomCount }"
                @input="filterNumberInput"
                min="0"
              />
              <span v-if="roomErrors.bathroomCount" class="error-message">{{ roomErrors.bathroomCount }}</span>
            </div>
          </div>

          <div class="form-group">
            <label>객실 설명</label>
            <textarea
              v-model="roomForm.description"
              rows="3"
              placeholder="객실의 특징, 편의시설, 전망 등을 상세히 입력해 주세요."
            ></textarea>
          </div>
          
          <!-- 객실 편의시설 -->
          
          <div class="room-form-actions">
            <button class="btn-outline" @click="showRoomForm = false; resetRoomErrors()">취소</button>
            <button class="btn-primary" @click="addRoom">등록</button>
          </div>
        </div>
      </section>

      <!-- Bottom Actions -->
      <div class="bottom-actions">
        <button class="btn-cancel" @click="$router.push('/host')">취소</button>
        <button class="btn-submit" @click="handleSubmit" :disabled="isSubmitting">{{ isSubmitting ? '등록 중...' : '등록 완료' }}</button>
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
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.7);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  line-height: 1;
  z-index: 2;
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
    appearance: none; 
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

/* Detail Images Preview Code copied from Edit page */
.detail-images-container {
  width: 100%;
}

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

.add-detail-image {
  width: 100%;
  aspect-ratio: 1;
  border: 2px dashed #BFE7DF;
  border-radius: 8px;
  display: flex !important;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 2rem;
  color: #BFE7DF;
  transition: all 0.2s;
  background: white;
  padding-bottom: 4px; /* Center alignment tweak */
  line-height: 1;
}

.add-detail-image:hover {
  background: #f0fcfa;
}

.add-detail-image input {
  display: none;
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
