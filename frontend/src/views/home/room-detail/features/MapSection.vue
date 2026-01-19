<script setup>
import { computed, nextTick, onMounted, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  latitude: {
    type: [Number, String],
    default: null
  },
  longitude: {
    type: [Number, String],
    default: null
  },
  address: {
    type: String,
    default: ''
  },
  name: {
    type: String,
    default: ''
  },
  transportInfo: {
    type: String,
    default: ''
  }
})

const kakaoMapRef = ref(null)
const mapError = ref(false)
const resolvedAddressCoords = ref(null)

const mapAddress = computed(() => (props.address || '').trim())
const hasCoordinates = computed(() => {
  const latitude = props.latitude
  const longitude = props.longitude
  if (latitude == null || longitude == null || latitude === '' || longitude === '') return false
  return Number.isFinite(Number(latitude)) && Number.isFinite(Number(longitude))
})
const hasMapLocation = computed(() => hasCoordinates.value || mapAddress.value.length > 0)
const transportText = computed(() => props.transportInfo || '교통 정보가 없습니다.')

let kakaoMap = null
let kakaoMarker = null
let kakaoMapPromise = null

const loadKakaoMap = () => {
  if (window.kakao?.maps?.load) {
    return Promise.resolve()
  }
  if (kakaoMapPromise) return kakaoMapPromise

  kakaoMapPromise = new Promise((resolve, reject) => {
    const loadMaps = () => {
      if (window.kakao?.maps?.load) {
        resolve()
        return
      }
      reject(new Error('Kakao map sdk not ready'))
    }

    if (window.kakao?.maps) {
      loadMaps()
      return
    }

    const appKey = 'edff5897f4250925f0de3f4ca22d615f'
    if (!appKey) {
      reject(new Error('Kakao map key is missing'))
      return
    }

    const script = document.createElement('script')
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${appKey}&libraries=services&autoload=false`
    script.async = true
    script.onload = loadMaps
    script.onerror = reject
    document.head.appendChild(script)
  })

  return kakaoMapPromise
}

const resolveMapCenter = async () => {
  const rawLatitude = props.latitude
  const rawLongitude = props.longitude
  if (rawLatitude != null && rawLongitude != null && rawLatitude !== '' && rawLongitude !== '') {
    const latitude = Number(rawLatitude)
    const longitude = Number(rawLongitude)
    if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
      return { latitude, longitude }
    }
  }

  if (!mapAddress.value) return null
  if (resolvedAddressCoords.value) return resolvedAddressCoords.value
  if (!window.kakao?.maps?.services?.Geocoder) return null

  const geocoder = new window.kakao.maps.services.Geocoder()
  const coords = await new Promise((resolve) => {
    geocoder.addressSearch(mapAddress.value, (result, status) => {
      if (status === window.kakao.maps.services.Status.OK && Array.isArray(result) && result.length) {
        const latitude = Number(result[0].y)
        const longitude = Number(result[0].x)
        if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
          resolve({ latitude, longitude })
          return
        }
      }
      resolve(null)
    })
  })

  if (coords) {
    resolvedAddressCoords.value = coords
  }

  return coords
}

const renderKakaoMap = async () => {
  mapError.value = false
  if (!hasMapLocation.value) return
  try {
    await loadKakaoMap()
  } catch (error) {
    console.error('Kakao map load failed', error)
    mapError.value = true
    return
  }
  await nextTick()
  if (!kakaoMapRef.value || !window.kakao?.maps?.load) return
  window.kakao.maps.load(async () => {
    const coords = await resolveMapCenter()
    if (!coords) {
      mapError.value = true
      return
    }

    const center = new window.kakao.maps.LatLng(coords.latitude, coords.longitude)
    const needsNewMap = !kakaoMap
      || (typeof kakaoMap.getContainer === 'function' && kakaoMap.getContainer() !== kakaoMapRef.value)

    if (needsNewMap) {
      kakaoMap = new window.kakao.maps.Map(kakaoMapRef.value, {
        center,
        level: 3
      })
      kakaoMarker = new window.kakao.maps.Marker({ position: center })
      kakaoMarker.setMap(kakaoMap)
    } else {
      kakaoMap.setCenter(center)
      kakaoMarker?.setPosition(center)
    }
  })
}

let observer = null

onMounted(() => {
  if (!kakaoMapRef.value) return

  observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting) {
      renderKakaoMap()
      observer.disconnect()
      observer = null
    }
  }, { rootMargin: '200px' }) // 미리 로드하기 위해 200px 여유

  observer.observe(kakaoMapRef.value)
})

onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
  // 메모리 누수 방지를 위해 지도 인스턴스 파괴
  // (Kakao Maps API 문서 권장 사항)
  /*
    참고: Kakao Maps SDK 버전에 따라 destroy 메서드가 없을 수도 있으므로,
    존재 여부를 확인 후 호출하거나 try-catch로 감싸는 것이 안전할 수 있습니다.
    여기서는 일반적인 패턴인 존재 여부 확인 후 호출 방식을 사용하지 않고,
    제공된 레퍼런스 코드를 따르되 안전장치를 살짝 고려합니다.
   */
  if (kakaoMap && typeof kakaoMap.destroy === 'function') {
      // 3. destroy() 메서드 존재 시 호출
      kakaoMap.destroy(); 
  }
  kakaoMap = null;
  kakaoMarker = null;
})

watch(
  () => [props.latitude, props.longitude, mapAddress.value],
  () => {
    resolvedAddressCoords.value = null
    renderKakaoMap()
  }
)

const openKakaoNavi = () => {
  const latitude = props.latitude
  const longitude = props.longitude
  const targetName = props.name || '숙소'

  // 1. 위도, 경도가 있는 경우: 좌표로 바로 길찾기 연결
  if (latitude && longitude && Number.isFinite(Number(latitude)) && Number.isFinite(Number(longitude))) {
    window.open(`https://map.kakao.com/link/to/${targetName},${latitude},${longitude}`, '_blank')
    return
  }

  // 2. 위/경도가 없고 주소가 있는 경우: 주소 검색 결과로 연결
  if (mapAddress.value) {
    window.open(`https://map.kakao.com/link/search/${mapAddress.value}`, '_blank')
    return
  }
  
  // 3. 둘 다 없는 경우 (이름으로라도 시도)
  if (targetName !== '숙소') {
    window.open(`https://map.kakao.com/link/search/${targetName}`, '_blank')
  } else {
    alert('위치 정보가 없습니다.')
  }
}
</script>

<template>
  <section class="section map-section">
    <h2>숙소 위치</h2>
    <div v-if="hasMapLocation && !mapError" ref="kakaoMapRef" class="map-container"></div>
    <div v-else class="map-placeholder">
      <div class="map-text">위치 정보가 없습니다.</div>
    </div>
    
    <button type="button" class="kakaomap-btn" @click="openKakaoNavi">
      <!-- SVG Pin Icon -->
      <svg class="kakaomap-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" />
      </svg>
      카카오맵으로 길찾기
    </button>
    

  </section>
</template>

<style scoped>
.section {
  padding: 1.5rem 0;
}
h2 {
  font-size: 1.4rem;
  margin-bottom: 1rem;
}
.map-placeholder {
  background: #eee;
  width: 100%;
  max-width: 520px;
  aspect-ratio: 1 / 1;
  border-radius: var(--radius-md);
  border: 1px solid #ddd;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  color: #888;
  padding: 0 1rem;
  text-align: center;
  margin: 0 auto;
}
.map-text {
  color: var(--text-main);
  font-weight: 600;
}
.map-container {
  width: 100%;
  max-width: 520px;
  aspect-ratio: 1 / 1;
  border-radius: var(--radius-md);
  border: 1px solid #ddd;
  overflow: hidden;
  margin: 0 auto;
}
.map-info {
  margin-top: 0.5rem;
}
@media (min-width: 769px) {
  .map-placeholder,
  .map-container {
    max-width: 100%;
    aspect-ratio: auto;
    height: 480px;
  }
}

.kakaomap-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  max-width: 520px;
  margin: 1.2rem auto 0;
  padding: 0.9rem;
  background-color: #FAE100; /* 카카오 옐로우 */
  color: #3C1E1E; /* 카카오 브라운 */
  border: none;
  border-radius: 12px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 0 2px 5px rgba(0,0,0,0.06);
  letter-spacing: -0.01em;
}
.kakaomap-btn:hover {
  background-color: #FFEB00;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0,0,0,0.12);
}
.kakaomap-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 4px rgba(0,0,0,0.06);
}
.kakaomap-icon {
  width: 20px;
  height: 20px;
  fill: currentColor;
}
</style>
