<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import HeaderSearchBar from './header/HeaderSearchBar.vue'
import HeaderMobileMenu from './header/HeaderMobileMenu.vue'

const route = useRoute()
const isHostRoute = computed(() => route.path.startsWith('/host'))
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
</script>

<template>
  <header class="app-header">
    <div class="header-container">
      <div class="header-content">
        <div class="header-top">
          <!-- Logo linked to home -->
          <router-link to="/">
            <img src="@/assets/logo.png" alt="Logo" class="logo" />
          </router-link>

          <!-- Right Menu (Hamburger & Dropdown) -->
          <HeaderMobileMenu />
        </div>

        <!-- Search Bar (Hidden on Host/Admin pages) -->
        <HeaderSearchBar v-if="!isHostRoute && !isAdminRoute" />
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
  z-index: 1000; /* 200에서 1000으로 증가 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  isolation: isolate; /* 새로운 stacking context 생성 */
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

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  height: 40px;
  width: auto;
  flex-shrink: 0;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.logo:hover {
  opacity: 0.8;
}

/* Desktop Styles */
@media (min-width: 769px) {
  .header-container {
    padding: 8px 24px 4px;
  }

  .header-content {
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    align-items: center;
    column-gap: 16px;
  }

  .header-top {
    display: contents; /* Allows children (logo, menu) to participate in grid */
  }

  .logo {
    height: 48px;
    flex-shrink: 0;
    order: 1;
    justify-self: start;
    grid-column: 1;
  }

  /* Target child component root elements for grid placement */
  :deep(.search-bar) {
    max-width: 850px;
    width: min(850px, 100%);
    order: 2;
    justify-self: center;
    grid-column: 2;
  }

  :deep(.right-menu) {
    flex-shrink: 0;
    order: 3;
    justify-self: end;
    grid-column: 3;
  }
}

/* Mobile Styles */
@media (max-width: 768px) {
  .app-header {
    overflow: visible;
  }

  .header-container {
    padding: 8px 12px 4px;
    max-width: 100%;
    overflow: visible;
  }

  .header-content {
    gap: 0;
    max-width: 100%;
  }

  .header-top {
    width: 100%;
  }

  .logo {
    height: 36px;
  }
}
</style>
