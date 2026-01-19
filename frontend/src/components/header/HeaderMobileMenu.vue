<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { isAuthenticated, logout, validateToken, getUserInfo, getAccessToken, getCurrentUser, saveUserInfo } from '@/api/authClient'

// Props (if needed in future, currently using hooks)
// const props = defineProps({})

const router = useRouter()
const route = useRoute()

// State
const isMenuOpen = ref(false)
const isLoggedIn = ref(isAuthenticated())

// Computed
const isHostRoute = computed(() => route.path.startsWith('/host'))
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const userInfo = computed(() => getUserInfo())
const isUserHost = computed(() => {
  return (
    userInfo.value?.role === 'HOST' ||
    userInfo.value?.role === 'ROLE_HOST' ||
    userInfo.value?.hostApproved === true
  )
})

// Actions
const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value
}

const toggleHostMode = async () => {
  if (!isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath || '/host' } });
    isMenuOpen.value = false;
    return;
  }

  let hostAllowed = isUserHost.value
  if (!hostAllowed && getAccessToken()) {
    const response = await getCurrentUser()
    if (response.ok && response.data) {
      saveUserInfo(response.data)
      hostAllowed = response.data.role === 'HOST' ||
        response.data.role === 'ROLE_HOST' ||
        response.data.hostApproved === true
    }
  }

  if (hostAllowed) {
    // 실제 호스트인 경우, 호스트/게스트 뷰 토글
    if (isHostRoute.value) {
      router.push('/');
    } else {
      router.push('/host');
    }
  } else {
    router.push('/host')
  }
  isMenuOpen.value = false;
}

const handleLogin = () => {
  router.push('/login')
  isMenuOpen.value = false
}

const handleLogout = () => {
  logout()
  isLoggedIn.value = false
  isMenuOpen.value = false
  
  // 공개 페이지 패턴 (로그아웃 후에도 유지 가능한 페이지)
  const publicPathPatterns = [
    /^\/$/,           // 메인
    /^\/list/,        // 리스트
    /^\/map/,         // 지도
    /^\/room\//,      // 숙소 상세
    /^\/events/,      // 이벤트
    /^\/search/       // 검색
  ]
  
  const currentPath = route.path
  const isPublicPage = publicPathPatterns.some(pattern => pattern.test(currentPath))
  
  if (isPublicPage) {
    // 공개 페이지면 현재 페이지 새로고침 (상태 초기화)
    window.location.reload()
  } else {
    // 보호된 페이지면 메인으로 이동
    window.location.href = '/'
  }
}

// Click Outside Logic
const handleClickOutside = (e) => {
  if (!e.target.closest('.right-menu')) {
    isMenuOpen.value = false
  }
}

// Lifecycle
onMounted(async () => {
  document.addEventListener('click', handleClickOutside)

  // 페이지 로드 시 토큰 유효성 검증 (admin 라우트는 제외)
  if (isAuthenticated() && !isAdminRoute.value) {
    const isValid = await validateToken()
    isLoggedIn.value = isValid
  } else {
    isLoggedIn.value = !isAdminRoute.value && isAuthenticated()
  }

  // 페이지 이동 후 로그인 상태 업데이트
  router.afterEach(() => {
    isLoggedIn.value = isAuthenticated()
  })
})
</script>

<template>
  <div class="right-menu">
    <button class="hamburger-btn" @click.stop="toggleMenu" aria-label="메뉴">
      <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
      </svg>
    </button>

    <!-- Enhanced Mobile Menu (Dropdown) -->
    <div class="mobile-menu-dropdown" :class="{ active: isMenuOpen }" @click.stop>
      <div class="menu-header">
        <span class="menu-title">메뉴</span>
        <button class="close-btn" @click="isMenuOpen = false">×</button>
      </div>

      <div class="host-toggle-card" @click="toggleHostMode">
        <div class="toggle-info">
          <div class="toggle-title">{{ isHostRoute ? '게스트 모드로 전환' : '호스트 모드로 전환' }}</div>
          <div class="toggle-status">현재: {{ !isLoggedIn ? '비회원' : (isHostRoute ? '호스트 모드' : '게스트 모드') }}</div>
        </div>
        <div class="toggle-icon">›</div>
      </div>

      <ul class="menu-list">
        <li><router-link to="/profile" @click="isMenuOpen = false">프로필 정보</router-link></li>
        <li><router-link to="/reservations" @click="isMenuOpen = false">예약 내역</router-link></li>
        <li><router-link to="/reviews" @click="isMenuOpen = false">리뷰 내역</router-link></li>
        <li><router-link to="/wishlist" @click="isMenuOpen = false">위시리스트</router-link></li>
        <li><router-link to="/coupons" @click="isMenuOpen = false">쿠폰</router-link></li>
        <li><router-link to="/events" @click="isMenuOpen = false">이벤트</router-link></li>
      </ul>

      <div class="menu-footer">
        <button v-if="isLoggedIn" class="logout-btn" @click="handleLogout">로그아웃</button>
        <button v-else class="login-btn" @click="handleLogin">로그인</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Right Menu Area */
.right-menu {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  position: relative; /* Anchor for dropdown */
}

.hamburger-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: #3d4452;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
}

.hamburger-btn:hover {
  background-color: #f0f4f8;
  color: #1a1f36;
}

.hamburger-btn svg {
  width: 22px;
  height: 22px;
}

/* Enhanced Mobile Menu Dropdown */
.mobile-menu-dropdown {
  display: none;
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 300px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.12);
  padding: 24px;
  z-index: 1000;
  font-family: 'Noto Sans KR', sans-serif;
  border: 1px solid #f0f0f0;
}

.mobile-menu-dropdown.active {
  display: block;
}

.menu-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.menu-title {
  font-size: 18px;
  font-weight: 700;
  color: #111;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: #333;
}

/* Host Toggle Card */
.host-toggle-card {
  background: #EFF6FF; /* Very light blue */
  padding: 16px 20px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  cursor: pointer;
  transition: all 0.2s;
}

.host-toggle-card:hover {
  background: #DBEAFE;
  transform: translateX(4px);
}

.host-toggle-card:active {
  transform: translateX(2px);
}

.toggle-title {
  font-size: 14px;
  font-weight: 700;
  color: #111;
  margin-bottom: 4px;
}

.toggle-status {
  font-size: 12px;
  color: #6B7280;
}

.toggle-icon {
  font-size: 24px;
  color: #6B7280;
  font-weight: bold;
  transition: transform 0.2s;
}

.host-toggle-card:hover .toggle-icon {
  transform: translateX(4px);
  color: #00796b;
}

/* Menu List */
.menu-list {
  list-style: none;
  padding: 0;
  margin: 0 0 24px;
}

.menu-list li {
  margin-bottom: 12px;
}

.menu-list li:last-child {
  margin-bottom: 0;
}

.menu-list a {
  text-decoration: none;
  color: #374151;
  font-size: 15px;
  font-weight: 500;
  display: block;
  padding: 8px 0;
  transition: color 0.2s;
}

.menu-list a:hover {
  color: #6DC3BB;
  padding-left: 4px;
}

/* Menu Footer (Logout) */
.menu-footer {
  border-top: 1px solid #F3F4F6;
  padding-top: 20px;
}

.logout-btn {
  background: none;
  border: none;
  color: #EF4444; /* Red */
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
}

.logout-btn:hover {
  text-decoration: underline;
}

.login-btn {
  background: none;
  border: none;
  color: #00796b; /* Teal */
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
}

.login-btn:hover {
  text-decoration: underline;
}

@media (min-width: 769px) {
  .right-menu {
    flex-shrink: 0;
    order: 3;
    justify-self: end;
    grid-column: 3;
  }
  
  .hamburger-btn {
    width: 44px;
    height: 44px;
  }
  
  .hamburger-btn svg {
    width: 24px;
    height: 24px;
  }
}

@media (max-width: 768px) {
  .right-menu { position: relative; }
  .mobile-menu-dropdown { right: -8px; width: 280px; }
}
</style>
