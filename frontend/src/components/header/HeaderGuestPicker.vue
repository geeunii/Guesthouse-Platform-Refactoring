<script setup>
import { useSearchStore } from '@/stores/search'

const props = defineProps({
  mode: {
    type: String,
    default: 'desktop', // 'desktop' | 'mobile'
    validator: (value) => ['desktop', 'mobile'].includes(value)
  }
})

const searchStore = useSearchStore()

const increaseGuest = () => {
  searchStore.increaseGuest()
}

const decreaseGuest = () => {
  searchStore.decreaseGuest()
}
</script>

<template>
  <!-- Mobile Guest Picker -->
  <div v-if="mode === 'mobile'" class="mobile-guest-popup" @click.stop>
    <div class="guest-header">인원수를 입력하세요</div>
    <div class="guest-row">
      <div class="guest-info">
        <span class="guest-type">게스트</span>
      </div>
      <div class="guest-controls">
        <button class="guest-btn" @click.stop="decreaseGuest" :disabled="searchStore.guestCount <= 1">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"></path>
          </svg>
        </button>
        <span class="guest-count">{{ searchStore.guestCount }}</span>
        <button class="guest-btn" @click.stop="increaseGuest">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
          </svg>
        </button>
      </div>
    </div>
  </div>

  <!-- Desktop Guest Picker -->
  <div v-else class="guest-popup" @click.stop>
    <div class="guest-header">인원수를 입력하세요</div>
    <div class="guest-row">
      <div class="guest-info">
        <span class="guest-type">게스트</span>
      </div>
      <div class="guest-controls">
        <button class="guest-btn" @click.stop="decreaseGuest" :disabled="searchStore.guestCount <= 1">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"></path>
          </svg>
        </button>
        <span class="guest-count">{{ searchStore.guestCount }}</span>
        <button class="guest-btn" @click.stop="increaseGuest">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Mobile Guest Popup */
.mobile-guest-popup {
  background: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  margin: 8px 0;
  font-family: 'Noto Sans KR', sans-serif;
}

.mobile-guest-popup .guest-header {
  font-size: 16px;
  margin-bottom: 12px;
}

.mobile-guest-popup .guest-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: none;
}

.mobile-guest-popup .guest-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-guest-popup .guest-type {
  font-size: 16px;
  font-weight: 600;
  color: #1a1f36;
}

.mobile-guest-popup .guest-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.guest-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid #e0e6eb;
  background: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #374151;
  transition: all 0.2s ease;
}

.guest-btn:hover:not(:disabled) {
  border-color: #6DC3BB;
  color: #6DC3BB;
}

.guest-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.guest-count {
  font-size: 16px;
  font-weight: 600;
  color: #1a1f36;
  min-width: 24px;
  text-align: center;
}

/* Desktop Guest Popup */
.guest-popup {
  position: absolute;
  top: calc(100% + 12px);
  left: 50%;
  transform: translateX(-50%);
  width: 320px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  padding: 24px;
  z-index: 90;
  font-family: 'Noto Sans KR', sans-serif;
  border: 1px solid #f0f0f0;
  animation: guestFadeIn 0.2s ease;
}

.guest-header {
  font-size: 20px;
  font-weight: 700;
  color: #1a1f36;
  margin-bottom: 20px;
}

.guest-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.guest-row:last-child {
  border-bottom: none;
}

.guest-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.guest-type {
  font-size: 16px;
  font-weight: 600;
  color: #1a1f36;
}

@keyframes guestFadeIn {
  from { opacity: 0; transform: translateX(-50%) translateY(-10px); }
  to { opacity: 1; transform: translateX(-50%) translateY(0); }
}

@media (max-width: 768px) {
  .guest-popup {
    width: 280px; left: 0; transform: translateX(0); padding: 20px;
  }
  
  @keyframes guestFadeIn {
    from { opacity: 0; transform: translateY(-10px); }
    to { opacity: 1; transform: translateY(0); }
  }
}
</style>
