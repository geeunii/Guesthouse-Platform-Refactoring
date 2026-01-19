<script setup>
import AdminBadge from './AdminBadge.vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: String, required: true },
  sub: { type: String, default: '' },
  badge: { type: String, default: '' },
  tone: { type: String, default: 'primary' },
  clickable: { type: Boolean, default: false }
})

const toneClass = `admin-stat-card--${props.tone}`
const badgeVariant = props.tone === 'primary' ? 'accent' : props.tone
</script>

<template>
  <component
    :is="clickable ? 'button' : 'div'"
    class="admin-stat-card"
    :class="[toneClass, { 'is-clickable': clickable }]"
    :type="clickable ? 'button' : undefined"
  >
    <div class="admin-stat-card__label">
      <span>{{ label }}</span>
      <AdminBadge v-if="badge" :text="badge" :variant="badgeVariant" />
    </div>
    <div class="admin-stat-card__value">{{ value }}</div>
    <div v-if="sub" class="admin-stat-card__sub">{{ sub }}</div>
    <div v-if="$slots.icon" class="admin-stat-card__icon">
      <slot name="icon" />
    </div>
  </component>
</template>

<style scoped>
.admin-stat-card {
  position: relative;
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px 18px;
  background: var(--bg-white);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
  text-align: left;
  width: 100%;
}

.admin-stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--tone-color, #e5e7eb);
}

.admin-stat-card__label {
  font-size: 0.95rem;
  color: var(--text-sub);
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.admin-stat-card__value {
  font-size: 1.5rem;
  font-weight: 800;
  color: #0b3b32;
}

.admin-stat-card__sub {
  font-size: 0.9rem;
  color: var(--text-sub);
  font-weight: 600;
}

.admin-stat-card__icon {
  position: absolute;
  right: 14px;
  top: 14px;
  color: #0f766e;
  opacity: 0.4;
}

.admin-stat-card.is-clickable {
  cursor: pointer;
  border-color: #cfe7e1;
}

.admin-stat-card.is-clickable:focus-visible {
  outline: 2px solid #0f766e;
  outline-offset: 2px;
}

.admin-stat-card--primary {
  border-color: #e5e7eb;
  --tone-color: #14b8a6;
}

.admin-stat-card--success {
  border-color: #e5e7eb;
  --tone-color: #22c55e;
}

.admin-stat-card--neutral {
  border-color: #e5e7eb;
  --tone-color: #94a3b8;
}

.admin-stat-card--accent {
  border-color: #e5e7eb;
  --tone-color: #3b82f6;
}

.admin-stat-card--warning {
  border-color: #e5e7eb;
  --tone-color: #f59e0b;
}
</style>
