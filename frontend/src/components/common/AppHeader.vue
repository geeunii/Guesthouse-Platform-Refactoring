<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const isMenuOpen = ref(false)
const isSearchExpanded = ref(false)

const isMobile = () => window.innerWidth <= 768

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value
}

const toggleSearch = () => {
  if (isMobile()) {
    isSearchExpanded.value = !isSearchExpanded.value
  }
}

const handleClickOutside = (e) => {
  if (!e.target.closest('.right-menu')) {
    isMenuOpen.value = false
  }
}

const handleResize = () => {
  if (!isMobile()) {
    isSearchExpanded.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <header class="app-header">
    <div class="header-container">
      <div class="header-content">
        <div class="logo">Travel</div>

        <div
          class="search-bar"
          :class="{ expanded: isSearchExpanded }"
          @click="toggleSearch"
        >
          <div class="search-item">
            <label>여행지</label>
            <input type="text" placeholder="어디로 갈까?">
          </div>

          <div class="search-divider"></div>

          <div class="search-item">
            <label>날짜</label>
            <input type="text" placeholder="날짜 선택">
          </div>

          <div class="search-divider"></div>

          <div class="search-item">
            <label>여행자</label>
            <input type="text" placeholder="게스트 추가">
          </div>

          <button class="search-btn" aria-label="검색">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>
          </button>
        </div>

        <div class="right-menu">
          <button class="hamburger-btn" @click.stop="toggleMenu" aria-label="메뉴">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
            </svg>
          </button>

          <div class="mobile-menu" :class="{ active: isMenuOpen }" @click.stop>
            <router-link class="menu-link" to="/events" @click="isMenuOpen = false">
              이벤트
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  background: white;
  border-bottom: 1px solid #e8ecf0;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.header-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 10px 16px;
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.logo {
  font-size: 22px;
  font-weight: 700;
  color: #6DC3BB;
  letter-spacing: -0.5px;
  flex-shrink: 0;
  cursor: pointer;
  transition: color 0.2s ease;
  font-family: 'Poppins', sans-serif;
}

.logo:hover {
  color: #5aaca3;
}

.search-bar {
  width: 100%;
  background: white;
  border: 1px solid #e0e6eb;
  border-radius: 12px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.search-bar:hover {
  border-color: #6DC3BB;
  box-shadow: 0 4px 12px rgba(109, 195, 187, 0.1);
}

.search-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 6px 8px;
  border-radius: 6px;
  min-width: 0;
}

.search-item:hover {
  background-color: #f0f7f6;
}

.search-item label {
  font-size: 10px;
  font-weight: 600;
  color: #8a92a0;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  margin-bottom: 2px;
  display: block;
  font-family: 'Poppins', sans-serif;
  white-space: nowrap;
}

.search-item input {
  background: transparent;
  border: none;
  font-size: 13px;
  color: #1a1f36;
  outline: none;
  font-weight: 500;
  font-family: 'Noto Sans KR', sans-serif;
  width: 100%;
}

.search-item input::placeholder {
  color: #c5cdd4;
  font-weight: 400;
}

.search-divider {
  width: 1px;
  height: 20px;
  background-color: #e8ecf0;
  flex-shrink: 0;
}

.search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background-color: #6DC3BB;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: white;
  transition: all 0.3s ease;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(109, 195, 187, 0.2);
}

.search-btn:hover {
  background-color: #5aaca3;
  box-shadow: 0 4px 16px rgba(109, 195, 187, 0.3);
  transform: scale(1.05);
}

.search-btn:active {
  transform: scale(0.95);
}

.search-btn svg {
  width: 16px;
  height: 16px;
}

.right-menu {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
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

.mobile-menu {
  display: none;
  position: absolute;
  top: 100%;
  right: 16px;
  background: white;
  border: 1px solid #e8ecf0;
  border-radius: 12px;
  min-width: 180px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  margin-top: 8px;
  overflow: hidden;
  z-index: 1000;
}

.mobile-menu.active {
  display: block;
}

.mobile-menu .menu-link {
  display: block;
  width: 100%;
  padding: 12px 16px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  color: #3d4452;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  border-bottom: 1px solid #f0f4f8;
  font-family: 'Noto Sans KR', sans-serif;
  text-decoration: none;
}

.mobile-menu .menu-link:last-child {
  border-bottom: none;
}

.mobile-menu .menu-link:hover {
  background-color: #f0f7f6;
  color: #6DC3BB;
  padding-left: 20px;
}

/* Desktop styles */
@media (min-width: 769px) {
  .header-container {
    padding: 16px 40px;
  }

  .header-content {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 32px;
  }

  .logo {
    font-size: 28px;
    flex-shrink: 0;
  }

  .search-bar {
    max-width: 850px;
    flex: 1;
    border-radius: 40px;
    padding: 14px 28px;
    gap: 24px;
  }

  .search-item {
    padding: 10px 16px;
    min-width: 150px;
  }

  .search-item label {
    font-size: 12px;
    display: block !important;
  }

  .search-item input {
    font-size: 14px;
  }

  .search-divider {
    display: block !important;
    height: 32px;
  }

  .search-btn {
    width: 48px;
    height: 48px;
  }

  .search-btn svg {
    width: 20px;
    height: 20px;
  }

  .right-menu {
    flex-shrink: 0;
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

/* Mobile styles */
@media (max-width: 768px) {
  .header-container {
    padding: 8px 12px;
  }

  .header-content {
    gap: 12px;
  }

  .logo {
    font-size: 18px;
  }

  .right-menu {
    position: absolute;
    top: 8px;
    right: 12px;
  }

  .search-bar.expanded {
    flex-direction: column;
    gap: 0;
    padding: 12px;
  }

  .search-item {
    padding: 10px 0;
  }

  .search-bar:not(.expanded) .search-item label {
    display: none;
  }

  .search-bar:not(.expanded) .search-item input {
    font-size: 12px;
  }

  .search-bar.expanded .search-item {
    border-bottom: 1px solid #e8ecf0;
  }

  .search-bar.expanded .search-item:last-of-type {
    border-bottom: none;
  }

  .search-item:hover {
    background-color: transparent;
  }

  .search-bar:not(.expanded) .search-divider {
    display: none;
  }

  .search-divider {
    display: none;
  }

  .search-btn {
    width: 36px;
    height: 36px;
  }

  .search-bar.expanded .search-btn {
    width: 100%;
    border-radius: 8px;
    margin-top: 8px;
  }
}
</style>
