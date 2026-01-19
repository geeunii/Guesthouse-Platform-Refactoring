<script setup>
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { ref } from 'vue'

const route = useRoute()

const navItems = [
  { to: '/admin/dashboard', label: '대시보드' },
  { to: '/admin/users', label: '회원 관리' },
  { to: '/admin/accommodations', label: '숙소 관리' },
  { to: '/admin/bookings', label: '예약 관리' },
  { to: '/admin/payments', label: '결제 관리' },
  { to: '/admin/reports', label: '신고 관리' }
]

const isActive = (to, exact = false) =>
  exact ? route.path === to : route.path.startsWith(to)

const roleSwitches = []

const isDarkMode = ref(sessionStorage.getItem('adminDarkMode') === 'true')

const toggleDarkMode = () => {
  isDarkMode.value = !isDarkMode.value
  sessionStorage.setItem('adminDarkMode', String(isDarkMode.value))
}
</script>

<template>
  <div class="admin-shell" :class="{ 'is-dark': isDarkMode }">
    <header class="admin-topbar">
      <div class="admin-topbar__brand">
        <div class="admin-logo-dot" />
        <div class="admin-brand-title">관리자 콘솔</div>
      </div>
      <nav class="admin-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="admin-nav__link"
          :class="{ 'is-active': isActive(item.to, item.exact) }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="admin-actions">
        <button class="admin-theme-toggle" type="button" @click="toggleDarkMode">
          <span class="toggle-dot" :class="{ 'is-on': isDarkMode }" />
          {{ isDarkMode ? '다크모드' : '라이트모드' }}
        </button>
      </div>
      <div v-if="roleSwitches.length" class="admin-role-switch">
        <RouterLink
          v-for="role in roleSwitches"
          :key="role.to"
          :to="role.to"
          class="admin-role-switch__btn"
          :class="{ 'is-active': isActive(role.to, role.to === '/admin') }"
        >
          {{ role.label }}
        </RouterLink>
      </div>
    </header>

    <main class="admin-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin-shell {
  background: var(--bg-body);
  min-height: calc(100vh - 120px);
}

.admin-topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #0f766e;
  color: #e7fffb;
  padding: 14px 18px;
  display: grid;
  grid-template-columns: 1fr 2fr auto;
  gap: 12px;
  align-items: center;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}

.admin-topbar__brand {
  display: flex;
  gap: 12px;
  align-items: center;
}

.admin-logo-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #d1fae5;
  box-shadow: 0 0 0 6px rgba(255, 255, 255, 0.12);
}

.admin-brand-title {
  font-weight: 900;
  font-size: 1.05rem;
  letter-spacing: -0.01em;
}

.admin-nav {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
}

.admin-nav__link {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  padding: 9px 10px;
  color: #e7fffb;
  border-radius: 10px;
  font-weight: 800;
  text-decoration: none;
  transition: background 0.2s ease, color 0.2s ease;
}

.admin-nav__link:hover {
  background: rgba(255, 255, 255, 0.12);
}

.admin-nav__link.is-active {
  background: #0b3b32;
  color: #f1fff9;
}

.admin-role-switch {
  justify-self: end;
  display: inline-flex;
  gap: 8px;
  background: rgba(255, 255, 255, 0.1);
  padding: 6px;
  border-radius: 12px;
  backdrop-filter: blur(6px);
}

.admin-role-switch__btn {
  color: #e7fffb;
  text-decoration: none;
  padding: 8px 10px;
  border-radius: 10px;
  font-weight: 800;
  border: 1px solid transparent;
}

.admin-role-switch__btn.is-active {
  background: #ecfdf3;
  color: #0f766e;
  border-color: #d8f5dd;
}

.admin-actions {
  justify-self: end;
}

.admin-theme-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.14);
  color: #e7fffb;
  border: 1px solid rgba(255, 255, 255, 0.25);
  padding: 8px 12px;
  border-radius: 999px;
  font-weight: 800;
}

.toggle-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #c7fff6;
  box-shadow: inset 0 0 0 2px rgba(15, 118, 110, 0.35);
}

.toggle-dot.is-on {
  background: #0ea5e9;
  box-shadow: inset 0 0 0 2px rgba(14, 165, 233, 0.35);
}

.admin-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 18px 16px 32px;
}


@media (max-width: 1024px) {
  .admin-topbar {
    grid-template-columns: 1fr;
  }

  .admin-nav {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }

  .admin-role-switch {
    justify-self: flex-start;
  }

  .admin-actions {
    justify-self: flex-start;
  }
}
</style>
