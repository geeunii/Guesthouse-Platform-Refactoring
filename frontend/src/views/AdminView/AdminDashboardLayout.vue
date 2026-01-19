<script setup>
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()

const tabs = [
  { to: '/admin/dashboard', label: '요약', exact: true },
  { to: '/admin/dashboard/issues', label: '운영 이슈 센터' },
  { to: '/admin/dashboard/weekly', label: '주간 리포트' },
  { to: '/admin/dashboard/audit', label: '감사 로그' }
]

const isActive = (to, exact = false) =>
  exact ? route.path === to : route.path.startsWith(to)
</script>

<template>
  <section class="admin-dashboard-shell">
    <nav class="admin-dashboard-tabs" aria-label="대시보드 탭">
      <RouterLink
        v-for="tab in tabs"
        :key="tab.to"
        :to="tab.to"
        class="admin-dashboard-tabs__item"
        :class="{ 'is-active': isActive(tab.to, tab.exact) }"
      >
        {{ tab.label }}
      </RouterLink>
    </nav>
    <div class="admin-dashboard-content">
      <RouterView v-slot="{ Component }">
        <KeepAlive>
          <component :is="Component" />
        </KeepAlive>
      </RouterView>
    </div>
  </section>
</template>

<style scoped>
.admin-dashboard-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.admin-dashboard-tabs {
  display: flex;
  gap: 8px;
  padding: 8px;
  background: #f0fdfa;
  border: 1px solid rgba(15, 118, 110, 0.15);
  border-radius: 14px;
  overflow-x: auto;
  scrollbar-width: thin;
}

.admin-dashboard-tabs__item {
  flex: 0 0 auto;
  text-decoration: none;
  color: #0f766e;
  font-weight: 800;
  padding: 10px 16px;
  border-radius: 12px;
  background: transparent;
  border: 1px solid transparent;
  white-space: nowrap;
}

.admin-dashboard-tabs__item.is-active {
  background: #0f766e;
  color: #f1fff9;
  border-color: #0c5c56;
}

.admin-dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
