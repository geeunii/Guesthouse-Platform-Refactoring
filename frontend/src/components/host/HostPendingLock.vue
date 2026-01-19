<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  title: { type: String, required: true },
  description: { type: String, required: true },
  primaryCtaText: { type: String, required: true },
  primaryCtaTo: { type: String, required: true },
  secondaryCtaText: { type: String, required: true },
  secondaryCtaTo: { type: String, required: true },
  badgeText: { type: String, default: '' }
})

const router = useRouter()

const goTo = (path) => {
  if (path) router.push(path)
}
</script>

<template>
  <section class="host-pending-lock">
    <div class="lock-card">
      <div class="lock-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" role="img" aria-hidden="true">
          <path
            d="M7 10V8a5 5 0 0 1 10 0v2h1.5A2.5 2.5 0 0 1 21 12.5v6A2.5 2.5 0 0 1 18.5 21h-13A2.5 2.5 0 0 1 3 18.5v-6A2.5 2.5 0 0 1 5.5 10H7zm2 0h6V8a3 3 0 1 0-6 0v2zm3 4a1.5 1.5 0 0 0-.75 2.8V18a.75.75 0 0 0 1.5 0v-1.2A1.5 1.5 0 0 0 12 14z"
          />
        </svg>
      </div>
      <div class="lock-body">
        <div class="lock-head">
          <p v-if="badgeText" class="lock-badge">{{ badgeText }}</p>
          <h3>{{ title }}</h3>
        </div>
        <p class="lock-desc">{{ description }}</p>
        <div class="lock-actions">
          <button class="lock-btn primary" type="button" @click="goTo(primaryCtaTo)">
            {{ primaryCtaText }}
          </button>
          <button class="lock-btn" type="button" @click="goTo(secondaryCtaTo)">
            {{ secondaryCtaText }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.host-pending-lock {
  display: flex;
  justify-content: center;
  width: 100%;
  padding: 0.75rem 0;
}

.lock-card {
  width: 100%;
  max-width: 520px;
  border: 1px solid #f5d695;
  background: #fff7ed;
  color: #92400e;
  border-radius: 18px;
  padding: 1.1rem;
  display: flex;
  gap: 1rem;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.lock-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #fef3c7;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.lock-icon svg {
  width: 22px;
  height: 22px;
  fill: #92400e;
}

.lock-body {
  display: grid;
  gap: 0.6rem;
}

.lock-head h3 {
  margin: 0.25rem 0 0;
  font-size: 1.05rem;
  font-weight: 900;
}

.lock-badge {
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-weight: 800;
  font-size: 0.85rem;
  color: #92400e;
}

.lock-desc {
  margin: 0;
  font-size: 0.92rem;
  font-weight: 700;
  color: #9a3412;
}

.lock-actions {
  display: grid;
  gap: 0.5rem;
}

.lock-btn {
  width: 100%;
  border: 1px solid #f59e0b;
  background: white;
  color: #92400e;
  border-radius: 12px;
  min-height: 44px;
  padding: 0.6rem 0.9rem;
  font-weight: 800;
  white-space: nowrap;
  word-break: keep-all;
  cursor: pointer;
}

.lock-btn.primary {
  background: #f59e0b;
  color: #1f2937;
}

@media (min-width: 768px) {
  .lock-card {
    padding: 1.4rem 1.6rem;
  }

  .lock-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
