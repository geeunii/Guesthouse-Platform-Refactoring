<script setup>
import { computed, ref, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import NavHome from '@/components/nav-icons/NavHome.vue'
import NavStay from '@/components/nav-icons/NavStay.vue'
import NavReservation from '@/components/nav-icons/NavReservation.vue'
import NavSales from '@/components/nav-icons/NavSales.vue'
import NavReview from '@/components/nav-icons/NavReview.vue'

const router = useRouter()
const route = useRoute()

const topTabs = [
  { id: 'dashboard', label: '대시보드', path: '/host' },
  { id: 'property', label: '숙소', path: '/host/accommodation' },
  { id: 'booking', label: '예약', path: '/host/booking' },
  { id: 'revenue', label: '매출', path: '/host/revenue' },
  { id: 'review', label: '리뷰', path: '/host/review' },
  { id: 'report', label: 'AI 리포트', path: '/host/report' }
]

const bottomTabs = [
  { id: 'dashboard', label: '대시보드', icon: NavHome, path: '/host' },
  { id: 'property', label: '숙소', icon: NavStay, path: '/host/accommodation' },
  { id: 'booking', label: '예약', icon: NavReservation, path: '/host/booking' },
  { id: 'revenue', label: '매출', icon: NavSales, path: '/host/revenue' },
  { id: 'more', label: '더보기', icon: NavReview, path: null }
]

const activeTab = computed(() => {
  const path = route.path
  if (path === '/host') return 'dashboard'
  if (
    path === '/host/accommodation' ||
    path.startsWith('/host/accommodation/') ||
    path === '/host/accmmodation' ||
    path.startsWith('/host/accmmodation/')
  ) {
    return 'property'
  }
  if (path === '/host/booking' || path.startsWith('/host/booking/')) return 'booking'
  if (path === '/host/revenue' || path.startsWith('/host/revenue/')) return 'revenue'
  if (path === '/host/review' || path.startsWith('/host/review/')) return 'review'
  if (path === '/host/report' || path.startsWith('/host/report/')) return 'report'
  return 'dashboard'
})

const moreOpen = ref(false)

const setTab = (path) => {
  if (!path) {
    moreOpen.value = !moreOpen.value
    return
  }
  moreOpen.value = false
  router.push(path)
}

const goToMore = (path) => {
  moreOpen.value = false
  router.push(path)
}

const closeMore = () => {
  moreOpen.value = false
}

const handleKeydown = (event) => {
  if (event.key === 'Escape') {
    moreOpen.value = false
  }
}

if (typeof window !== 'undefined') {
  window.addEventListener('keydown', handleKeydown)
  onBeforeUnmount(() => window.removeEventListener('keydown', handleKeydown))
}

watch(
  () => route.path,
  () => {
    moreOpen.value = false
  }
)
</script>

<template>
  <div class="host-dashboard">
    <!-- Desktop top navigation -->
    <header class="top-nav">
      <div class="top-nav__inner">
        <router-link class="brand" to="/host">Host Center</router-link>
        <nav class="top-menu">
          <button
              v-for="tab in topTabs"
              :key="tab.id"
              class="top-menu__item"
              :class="[{ active: activeTab === tab.id }, { 'nav-allowed': tab.id === 'property' || tab.id === 'dashboard' }]"
              @click="setTab(tab.path)"
          >
            {{ tab.label }}
          </button>
        </nav>
      </div>
    </header>

    <!-- Main Content -->
    <main class="content">
      <router-view />
    </main>

    <!-- Mobile bottom navigation -->
    <nav class="bottom-nav">
      <button
          v-for="tab in bottomTabs"
          :key="tab.id"
          class="nav-item"
          :class="[{ active: activeTab === tab.id || (tab.id === 'more' && moreOpen) }, { 'nav-allowed': tab.id === 'property' || tab.id === 'dashboard' }]"
          type="button"
          aria-haspopup="menu"
          :aria-expanded="tab.id === 'more' ? String(moreOpen) : null"
          @click="setTab(tab.path)"
      >
        <component :is="tab.icon" class="nav-icon" />
        <span class="nav-label">{{ tab.label }}</span>
      </button>
    </nav>

    <teleport to="body">
      <div v-if="moreOpen" class="more-overlay" role="presentation" @click="closeMore">
        <div class="more-sheet" role="menu" @click.stop>
          <button class="more-item" type="button" role="menuitem" @click="goToMore('/host/review')">리뷰</button>
          <button class="more-item" type="button" role="menuitem" @click="goToMore('/host/report')">AI 리포트</button>
        </div>
      </div>
    </teleport>
  </div>
</template>

<style scoped>
/* ✅ merge-safe: 이 파일 내부에서만 하단 네비 토큰 관리 */
.host-dashboard {
  min-height: 100vh;
  background: var(--brand-bg);

  /* 하단 네비 사이즈 토큰(모바일 퍼스트) */
  --bn-h: 56px;      /* bar base height(패딩 제외) */
  --bn-pad: 6px;     /* 위/아래 패딩 */
  --bn-icon: 20px;
  --bn-label: 11px;
  --bn-gap: 2px;
  --bn-stroke: 1.9;
  --host-bottom-nav-h: calc(var(--bn-h) + (var(--bn-pad) * 2));

  /* ✅ 하단 네비가 있는 동안 컨텐츠가 가려지지 않도록 */
  padding-bottom: calc(var(--bn-h) + (var(--bn-pad) * 2) + env(safe-area-inset-bottom));
}

/* ===================
   Top Nav (>=1024)
   =================== */
.top-nav {
  position: sticky;
  top: 0;
  z-index: 20;
  background: var(--bg-white);
  border-bottom: 1px solid var(--brand-border);
  display: none; /* 기본 숨김 */
}

.top-nav__inner {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0.75rem 1.25rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.brand {
  font-weight: 800;
  font-size: 1rem;
  color: var(--brand-accent);
  letter-spacing: -0.01em;
  text-decoration: none;
}

.top-menu {
  display: flex;
  gap: 0.75rem;
}

.top-menu__item {
  border: none;
  background: transparent;
  padding: 0.5rem 0.9rem;
  border-radius: 999px;
  color: var(--text-default);
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s ease;
}

.top-menu__item:hover {
  background: var(--brand-primary);
  color: var(--brand-accent);
}

.top-menu__item.active {
  background: var(--brand-primary);
  color: var(--brand-accent);
}

/* ===================
   Content
   =================== */
.content {
  max-width: 1180px;
  margin: 0 auto;
  padding: 1.5rem 1rem calc(2.5rem + var(--bn-h) + (var(--bn-pad) * 2) + env(safe-area-inset-bottom));
}

/* ===================
   Bottom Nav (<1024)
   =================== */
.bottom-nav {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;

  background: var(--bg-white);
  border-top: 1px solid var(--brand-border);

  display: grid;
  grid-template-columns: repeat(5, 1fr);
  z-index: 50;

  box-sizing: border-box;
  height: calc(var(--bn-h) + (var(--bn-pad) * 2) + env(safe-area-inset-bottom));
  padding: var(--bn-pad) 0 calc(var(--bn-pad) + env(safe-area-inset-bottom));

  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
}

.nav-item {
  padding: 0;
  min-height: 44px; /* ✅ 터치 타겟 확보 */
  background: none;
  border: none;
  text-align: center;
  color: var(--text-default);
  font-weight: 800;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--bn-gap);
}

.nav-item.active {
  background: var(--brand-primary);
  color: var(--brand-accent);
  border-radius: 12px;
}

.nav-icon {
  width: var(--bn-icon);
  height: var(--bn-icon);
  display: block;
  color: inherit;
}

/* ✅ 아이콘 컴포넌트 내부 svg 크기 강제 */
.nav-icon :deep(svg),
.nav-icon :deep(img) {
  width: 100% !important;
  height: 100% !important;
  display: block;
}

/* ✅ stroke 아이콘(선) 두께까지 제어 */
.nav-icon :deep(svg) {
  stroke-width: var(--bn-stroke) !important;
}

.nav-label {
  font-size: var(--bn-label);
  line-height: 1;
  margin-top: 0;
  font-weight: 800;
}

.more-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.28);
  z-index: 60;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  bottom: calc(var(--host-bottom-nav-h, 68px) + env(safe-area-inset-bottom));
}

.more-sheet {
  width: min(420px, 100%);
  background: var(--bg-white);
  border-top-left-radius: 18px;
  border-top-right-radius: 18px;
  padding: 12px 16px 16px;
  box-shadow: 0 -12px 30px rgba(15, 23, 42, 0.18);
  display: grid;
  gap: 10px;
  position: fixed;
  left: 0;
  right: 0;
  bottom: calc(var(--host-bottom-nav-h, 68px) + env(safe-area-inset-bottom));
  margin: 0 auto;
}

.more-item {
  border: 1px solid var(--brand-border);
  background: #f8fafc;
  border-radius: 12px;
  padding: 12px 14px;
  font-weight: 700;
  color: #0b3b32;
  text-align: left;
}

.more-item:focus-visible {
  outline: 2px solid rgba(15, 118, 110, 0.35);
  outline-offset: 2px;
}


/* ===================
   Mobile tuning
   =================== */

/* 대부분 폰 (iPhone 12/13/14/15, 갤럭시 기본 폭 포함) */
@media (max-width: 430px) {
  .host-dashboard {
    --bn-h: 54px;
    --bn-pad: 5px;
    --bn-icon: 19px;
    --bn-label: 11px;
    --bn-stroke: 1.85;
  }
}

/* 작은 폰 (iPhone SE급/작은 안드로이드) */
@media (max-width: 360px) {
  .host-dashboard {
    --bn-h: 50px;
    --bn-pad: 3px; /* 50 - (3+3) = 44 유지 */
    --bn-icon: 18px;
    --bn-label: 10px;
    --bn-stroke: 1.75;
  }
}

/* ===================
   Desktop rule
   =================== */
@media (min-width: 1024px) {
  /* 데스크탑: 상단 네비, 하단 네비 숨김 */
  .bottom-nav {
    display: none;
  }

  .top-nav {
    display: block;
  }

  .more-overlay {
    display: none;
  }

  /* 하단 네비가 없으니 padding-bottom 제거 */
  .host-dashboard {
    padding-bottom: 0;
  }

  .content {
    padding-top: 2rem;
  }
}
</style>
