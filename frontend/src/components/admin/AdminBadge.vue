<script setup>
defineProps({
  text: { type: String, required: true },
  variant: {
    type: String,
    default: 'neutral',
    validator: (v) => ['success', 'warning', 'neutral', 'accent', 'danger'].includes(v)
  }
})
</script>

<template>
  <span class="admin-badge" :class="`admin-badge--${variant}`">
    {{ text }}
  </span>
</template>

<style scoped>
.admin-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 10px;
  font-size: 0.85rem;
  font-weight: 700;
  border: 1px solid transparent;
}

.admin-badge--success {
  background: #ecfdf3;
  color: #0f6b4a;
  border-color: #c7f0d7;
}

.admin-badge--warning {
  background: #fff8e6;
  color: #c66f00;
  border-color: #ffe2a8;
}

.admin-badge--neutral {
  background: #f3f4f6;
  color: #4b5563;
  border-color: #e5e7eb;
}

.admin-badge--accent {
  background: #eef6ff;
  color: #1d4ed8;
  border-color: #cfe0ff;
}

.admin-badge--danger {
  background: #fef2f2;
  color: #b91c1c;
  border-color: #fecaca;
}
</style>
