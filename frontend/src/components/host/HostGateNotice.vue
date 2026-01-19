<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  title: { type: String, required: true },
  description: { type: String, required: true },
  reason: { type: String, default: '' },
  primaryText: { type: String, required: true },
  primaryTo: { type: String, required: true },
  secondaryText: { type: String, default: '' },
  secondaryTo: { type: String, default: '' }
})

const router = useRouter()

const goTo = (path) => {
  if (path) router.push(path)
}
</script>

<template>
  <section class="host-gate-notice">
    <div class="gate-card">
      <div class="gate-icon" aria-hidden="true">🔒</div>
      <div class="gate-body">
        <h3 class="gate-title">{{ title }}</h3>
        <p class="gate-desc">{{ description }}</p>
        <p v-if="reason" class="gate-reason">{{ reason }}</p>
        <div class="gate-actions">
          <button class="gate-btn primary" type="button" @click="goTo(primaryTo)">
            {{ primaryText }}
          </button>
          <button
            v-if="secondaryText && secondaryTo"
            class="gate-btn"
            type="button"
            @click="goTo(secondaryTo)"
          >
            {{ secondaryText }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.host-gate-notice {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 0.75rem 0;
}

.gate-card {
  width: min(520px, 100%);
  border-radius: 18px;
  padding: 1.2rem;
  background: #fff7ed;
  border: 1px solid #f5d695;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.9rem;
  color: #92400e;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.gate-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: #fef3c7;
  font-size: 1.2rem;
}

.gate-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 900;
}

.gate-desc {
  margin: 0.35rem 0 0;
  font-size: 0.92rem;
  font-weight: 700;
  color: #9a3412;
}

.gate-reason {
  margin: 0.45rem 0 0;
  font-size: 0.88rem;
  font-weight: 700;
  color: #c2410c;
}

.gate-actions {
  margin-top: 0.7rem;
  display: grid;
  gap: 0.55rem;
}

.gate-btn {
  width: 100%;
  min-height: 42px;
  border-radius: 12px;
  border: 1px solid #f59e0b;
  background: #fff;
  color: #92400e;
  font-weight: 800;
  cursor: pointer;
}

.gate-btn.primary {
  background: #f59e0b;
  color: #1f2937;
  border-color: transparent;
}

@media (min-width: 768px) {
  .gate-card {
    padding: 1.4rem 1.6rem;
  }

  .gate-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
