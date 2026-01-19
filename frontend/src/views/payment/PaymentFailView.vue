<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const errorCode = ref('')
const errorMessage = ref('')

onMounted(() => {
  errorCode.value = route.query.code || 'UNKNOWN'
  errorMessage.value = route.query.message || '결제가 취소되었거나 실패했습니다.'
})

const goBack = () => router.back()
const goHome = () => router.push('/')
</script>

<template>
  <div class="payment-fail-page">
    <div class="fail-container">
      <div class="error-icon">✕</div>
      <h1>결제에 실패했습니다</h1>
      <p class="error-code">에러 코드: {{ errorCode }}</p>
      <p class="error-message">{{ errorMessage }}</p>
      <div class="actions">
        <button class="btn secondary" @click="goBack">다시 시도</button>
        <button class="btn primary" @click="goHome">홈으로</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.payment-fail-page {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.fail-container {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
  text-align: center;
  padding: 2.5rem;
  width: 90%;
  max-width: 400px;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.error-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #EF4444 0%, #DC2626 100%);
  color: white;
  border-radius: 50%;
  font-size: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
}

h1 {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  color: #333;
}

.error-code {
  color: #888;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.error-message {
  color: #e11d48;
  margin-bottom: 2rem;
}

.actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.btn {
  padding: 0.8rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
}

.btn.primary {
  background: #0064FF;
  color: white;
}

.btn.secondary {
  background: white;
  color: #333;
  border: 1px solid #ddd;
}
</style>
