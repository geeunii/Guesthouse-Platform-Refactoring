<script setup>
defineProps({
  title: { type: String, default: '' }
})
</script>

<template>
  <div class="admin-table-card">
    <div v-if="title || $slots.actions" class="admin-table-card__header">
      <div class="admin-table-card__title">{{ title }}</div>
      <div class="admin-table-card__actions">
        <slot name="actions" />
      </div>
    </div>
    <div class="admin-table-wrap">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.admin-table-card {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: var(--shadow-md);
  padding: 16px;
}

.admin-table-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 8px;
}

.admin-table-card__title {
  font-weight: 800;
  color: #0b3b32;
}

.admin-table-card__actions {
  display: flex;
  gap: 8px;
}

.admin-table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 900px;
}

th,
td {
  text-align: left;
  padding: 12px 10px;
  border-bottom: 1px solid #eef1f4;
  vertical-align: middle;
}

th {
  font-size: 0.95rem;
  color: #4b5563;
  font-weight: 800;
  background: #f8fafc;
}

td {
  font-size: 0.95rem;
  color: #1f2937;
}

:deep(.admin-table--nowrap th),
:deep(.admin-table--nowrap td) {
  white-space: nowrap;
}

:deep(.admin-table--tight th),
:deep(.admin-table--tight td) {
  padding: 10px 8px;
}
</style>
