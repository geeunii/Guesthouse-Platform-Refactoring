<script setup>
const props = defineProps({
  booking: { type: Object, required: true },
  primaryLabel: { type: String, default: '' },
  primaryClass: { type: String, default: '' },
  secondaryLabel: { type: String, default: '' },
  secondaryClass: { type: String, default: '' },
  scheduleRange: { type: String, default: '' },
  scheduleMeta: { type: String, default: '' },
  amountLabel: { type: String, default: '' },
  animationDelay: { type: String, default: '' }
})

const emit = defineEmits(['detail'])

const handleDetail = () => {
  emit('detail', props.booking)
}
</script>

<template>
  <article class="mobile-card reservation-card fade-item" :style="animationDelay ? { animationDelay } : undefined">
    <div class="card-top">
      <div>
        <p class="muted">#{{ booking.id ? booking.id.toString().padStart(4, '0') : '-' }}</p>
        <h3>{{ booking.guestName || '-' }}</h3>
      </div>
      <span class="pill" :class="primaryClass">{{ primaryLabel }}</span>
    </div>
    <div class="card-tags">
      <span v-if="secondaryLabel" class="transaction-chip" :class="secondaryClass">{{ secondaryLabel }}</span>
    </div>
    <p class="property">{{ booking.property || '-' }}</p>
    <p class="period">{{ scheduleRange }}</p>
    <p class="period-meta">{{ scheduleMeta }}</p>
    <p class="amount">{{ amountLabel }}</p>
    <button class="ghost-btn detail-btn" aria-label="예약 상세" @click="handleDetail"><span class="detail-btn__text"></span></button>
  </article>
</template>

<style scoped>
.mobile-card {
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 14px;
  padding: 1rem;
  background: var(--bg-white, #fff);
  box-shadow: var(--shadow-md, 0 4px 14px rgba(0, 0, 0, 0.04));
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-height: 44px;
}

.muted {
  color: #9ca3af;
  margin: 0;
  font-size: 0.9rem;
  font-weight: 700;
}

h3 {
  margin: 0.1rem 0 0;
  font-size: 1.12rem;
  font-weight: 900;
  color: var(--text-main, #0f172a);
}

.property {
  margin: 0.45rem 0 0.15rem;
  color: #374151;
  font-weight: 900;
}

.card-tags {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
  min-height: 26px;
}

.transaction-chip {
  display: inline-flex;
  align-items: center;
  margin: 0.35rem 0 0.2rem;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #475569;
  font-size: 0.75rem;
  font-weight: 800;
  width: fit-content;
}

.transaction-chip--paid {
  border-color: #bbf7d0;
  background: #ecfdf3;
  color: #166534;
}

.transaction-chip--pending {
  border-color: #e2e8f0;
  background: #f8fafc;
  color: #475569;
}

.transaction-chip--failed {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.transaction-chip--warn {
  border-color: #fde68a;
  background: #fffbeb;
  color: #b45309;
}

.transaction-chip--refund {
  border-color: #dbeafe;
  background: #eff6ff;
  color: #1d4ed8;
}

.period {
  margin: 0;
  color: var(--text-sub, #6b7280);
  font-size: 0.95rem;
  font-weight: 600;
}

.period-meta {
  margin: 0.2rem 0 0;
  color: var(--text-sub, #6b7280);
  font-size: 0.9rem;
  font-weight: 700;
}

.amount {
  margin: 0.45rem 0;
  font-weight: 900;
  color: var(--text-main, #0f172a);
  font-size: 1.05rem;
}

.ghost-btn {
  width: 100%;
  border: 1px solid var(--brand-border);
  background: transparent;
  color: var(--brand-accent);
  border-radius: 10px;
  padding: 0.6rem;
  font-weight: 900;
  min-height: 44px;
  cursor: pointer;
}

.detail-btn {
  min-width: 72px;
  width: auto;
  white-space: nowrap;
  padding: 0.55rem 0.75rem;
}

.detail-btn__text {
  display: inline-block;
}

.detail-btn__text::after {
  content: '예약 상세';
}

@media (max-width: 767px) {
  .detail-btn__text::after {
    content: '상세';
  }
}
</style>
