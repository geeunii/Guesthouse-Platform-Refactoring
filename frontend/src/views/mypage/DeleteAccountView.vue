<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { deleteSelf } from '@/api/userClient'
import { logout } from '@/api/authClient'

const router = useRouter()

const currentStep = ref(1)
const isLoading = ref(false)

// Modal State
const showModal = ref(false)
const modalType = ref('confirm') // 'confirm', 'success', or 'error'
const modalTitle = ref('')
const modalMessage = ref('')

// Step 1: Confirmation Checkboxes
const confirmations = ref([
  { id: 1, label: '모든 개인정보가 삭제되며 복구할 수 없음을 이해했습니다', checked: false },
  { id: 2, label: '진행 중인 예약이 있는 경우 탈퇴가 제한될 수 있음을 확인했습니다', checked: false },
  { id: 3, label: '보유 중인 적립금과 쿠폰이 모두 소멸됨을 이해했습니다', checked: false },
  { id: 4, label: '탈퇴 후에는 되돌릴 수 없으며, 동일 계정으로 재가입이 필요함을 확인했습니다', checked: false }
])

const allConfirmed = computed(() => {
  return confirmations.value.every(c => c.checked)
})

// Step 2: Reason Selection
const reasons = ref([
  { id: 1, label: '더 이상 서비스를 이용하지 않아요' },
  { id: 2, label: '원하는 숙소를 찾기 어려워요' },
  { id: 3, label: '다른 플랫폼이 더 좋아요' },
  { id: 4, label: '가격이 비싸요' },
  { id: 5, label: '개인정보 보호가 걱정돼요' },
  { id: 6, label: '기타 (직접 입력)' }
])
const selectedReasons = ref([])
const otherReasonText = ref('')
const isOtherReasonSelected = computed(() => selectedReasons.value.includes(6))

const hasSelectedReason = computed(() => {
  if (selectedReasons.value.length === 0) return false
  if (isOtherReasonSelected.value && !otherReasonText.value.trim()) return false
  return true
})

const goToStep2 = () => {
  if (allConfirmed.value) {
    currentStep.value = 2
  }
}

const goBack = () => {
  if (currentStep.value === 2) {
    currentStep.value = 1
  } else {
    router.back()
  }
}

const openConfirmModal = () => {
  if (hasSelectedReason.value) {
    modalType.value = 'confirm'
    showModal.value = true
  }
}

const confirmDelete = async () => {
  isLoading.value = true
  console.log('회원 탈퇴 요청 시작')

  try {
    const reasonLabels = selectedReasons.value.map(id => reasons.value.find(r => r.id === id).label);
    console.log('탈퇴 사유:', reasonLabels, '기타:', otherReasonText.value)

    const response = await deleteSelf(reasonLabels, otherReasonText.value);
    console.log('탈퇴 API 응답:', response)

    if (response.ok) {
      console.log('탈퇴 성공')
      modalType.value = 'success'
    } else {
      // Handle known errors from the backend
      console.error('탈퇴 실패:', response.status, response.data)
      modalType.value = 'error';
      modalTitle.value = '탈퇴 처리 실패';

      // 더 상세한 에러 메시지 표시
      if (response.data && response.data.message) {
        modalMessage.value = response.data.message;
      } else if (response.status === 409) {
        modalMessage.value = '진행 중이거나 완료된 예약이 있어 탈퇴할 수 없습니다.';
      } else if (response.status === 401) {
        modalMessage.value = '로그인이 만료되었습니다. 다시 로그인해주세요.';
      } else if (response.status === 500) {
        modalMessage.value = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
      } else {
        modalMessage.value = `오류 코드: ${response.status}. 관리자에게 문의해주세요.`;
      }
    }
  } catch (error) {
    console.error("회원 탈퇴 중 예외 발생:", error);
    modalType.value = 'error';
    modalTitle.value = '요청 실패';
    modalMessage.value = '서버와 통신하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
  } finally {
    isLoading.value = false
    console.log('회원 탈퇴 요청 완료')
  }
}

const closeModal = () => {
  showModal.value = false;
}

const closeModalAndRedirect = () => {
  showModal.value = false
  logout() // Clear tokens
  router.push('/')
}
</script>

<template>
  <div class="delete-account-page container">
    <!-- Header -->
    <div class="page-header">
      <button class="back-btn" @click="goBack">←</button>
      <h1>회원 탈퇴</h1>
    </div>

    <!-- Progress Steps -->
    <div class="progress-steps">
      <div class="step" :class="{ active: currentStep >= 1 }">
        <span class="step-num">1</span>
      </div>
      <div class="step-line" :class="{ active: currentStep >= 2 }"></div>
      <div class="step" :class="{ active: currentStep >= 2 }">
        <span class="step-num">2</span>
      </div>
    </div>
    <div class="step-labels">
      <span>주의사항 확인</span>
      <span>탈퇴 사유</span>
    </div>

    <!-- Step 1: Confirmation -->
    <template v-if="currentStep === 1">
      <div class="warning-box">
        <h3>⚠️ 탈퇴 전 꼭 확인해주세요</h3>
        <p>회원 탈퇴 시 다음과 같은 정보가 영구적으로 삭제됩니다:</p>
        <ul>
          <li>회원님의 개인정보 및 프로필 정보</li>
          <li>예약 내역 및 리뷰 (진행 중인 예약 제외)</li>
          <li>보유하신 적립금 및 쿠폰</li>
          <li>찜한 숙소 목록 및 활동 기록</li>
        </ul>
      </div>

      <div class="confirmation-box">
        <h3>탈퇴 확인 사항</h3>
        <p class="sub-text">아래 항목을 모두 확인하고 동의해주세요</p>
        
        <div class="checkbox-list">
          <label v-for="item in confirmations" :key="item.id" class="checkbox-row">
            <input type="checkbox" v-model="item.checked" />
            <span>{{ item.label }}</span>
          </label>
        </div>
      </div>

      <div class="action-buttons">
        <button class="btn outline" @click="router.back()">취소</button>
        <button class="btn primary" :disabled="!allConfirmed" @click="goToStep2">다음 단계</button>
      </div>
    </template>

    <!-- Step 2: Reason Selection -->
    <template v-if="currentStep === 2">
      <div class="reason-box">
        <h3>탈퇴 사유를 알려주세요</h3>
        <p class="sub-text">더 나은 서비스를 위해 탈퇴 사유를 알려주시면 감사하겠습니다</p>
        
        <div class="reason-list">
          <label v-for="reason in reasons" :key="reason.id" class="reason-row">
            <input type="checkbox" :value="reason.id" v-model="selectedReasons" />
            <span>{{ reason.label }}</span>
          </label>
        </div>

        <div v-if="isOtherReasonSelected" class="other-reason-input">
          <textarea v-model="otherReasonText" placeholder="탈퇴하시는 구체적인 이유를 알려주세요." rows="3"></textarea>
        </div>
      </div>

      <div class="info-box">
        <h4>💡 잠깐만요!</h4>
        <ul>
          <li>고객센터를 통해 불편 사항을 개선할 수 있습니다</li>
          <li>재가입 시 기존 혜택을 다시 받기 어려울 수 있습니다</li>
        </ul>
      </div>

      <div class="action-buttons">
        <button class="btn outline" @click="currentStep = 1">이전</button>
        <button class="btn danger" :disabled="!hasSelectedReason" @click="openConfirmModal">회원 탈퇴하기</button>
      </div>
    </template>

    <!-- Custom Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <!-- Confirm Modal -->
        <template v-if="modalType === 'confirm'">
          <div class="modal-icon">⚠️</div>
          <h2>정말로 탈퇴하시겠습니까?</h2>
          <p>탈퇴 후에는 모든 데이터가 삭제되며<br>복구할 수 없습니다.</p>
          <div class="modal-actions">
            <button class="btn outline" @click="closeModal">취소</button>
            <button class="btn danger" @click="confirmDelete" :disabled="isLoading">
              {{ isLoading ? '처리 중...' : '탈퇴하기' }}
            </button>
          </div>
        </template>

        <!-- Success Modal -->
        <template v-if="modalType === 'success'">
          <div class="modal-icon success">✓</div>
          <h2>회원 탈퇴가 완료되었습니다</h2>
          <p>그동안 이용해 주셔서 감사합니다.<br>더 좋은 서비스로 다시 만나뵙길 바랍니다.</p>
          <div class="modal-actions">
            <button class="btn primary full" @click="closeModalAndRedirect">홈으로 이동</button>
          </div>
        </template>

        <!-- Error Modal -->
        <template v-if="modalType === 'error'">
          <div class="modal-icon error">!</div>
          <h2>{{ modalTitle }}</h2>
          <p>{{ modalMessage }}</p>
          <div class="modal-actions">
            <button class="btn primary full" @click="closeModal">확인</button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.delete-account-page {
  padding-top: 1rem;
  padding-bottom: 4rem;
  max-width: 600px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2rem;
}

.back-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
}

.page-header h1 {
  font-size: 1.3rem;
  font-weight: 700;
}

/* Progress Steps */
.progress-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 0.5rem;
}

.step {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: #888;
}

.step.active {
  background: #333;
  color: white;
}

.step-line {
  width: 60px;
  height: 2px;
  background: #ddd;
}

.step-line.active {
  background: #333;
}

.step-labels {
  display: flex;
  justify-content: space-between;
  max-width: 220px;
  margin: 0 auto 2rem;
  font-size: 0.85rem;
  color: #666;
}

/* Warning Box */
.warning-box {
  background: #fff5f5;
  border: 1px solid #fecaca;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}

.warning-box h3 {
  font-size: 1rem;
  margin-bottom: 1rem;
  color: #333;
}

.warning-box p {
  font-size: 0.9rem;
  margin-bottom: 0.8rem;
  color: #555;
}

.warning-box ul {
  list-style: disc inside;
  font-size: 0.85rem;
  color: #555;
  line-height: 1.6;
}

/* Confirmation Box */
.confirmation-box {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 2rem;
}

.confirmation-box h3 {
  font-size: 1rem;
  margin-bottom: 0.5rem;
}

.sub-text {
  font-size: 0.85rem;
  color: #888;
  margin-bottom: 1rem;
}

.checkbox-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.checkbox-row {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  cursor: pointer;
  font-size: 0.95rem;
}

.checkbox-row input {
  width: 20px;
  height: 20px;
  accent-color: var(--primary);
  flex-shrink: 0;
  margin-top: 2px;
}

/* Reason Box */
.reason-box {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}

.reason-box h3 {
  font-size: 1rem;
  margin-bottom: 0.5rem;
}

.reason-list {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.reason-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  font-size: 0.95rem;
}

.reason-row input {
  width: 18px;
  height: 18px;
  accent-color: var(--primary);
}

.other-reason-input {
  margin-top: 1rem;
}

.other-reason-input textarea {
  width: 100%;
  padding: 0.75rem 1rem;
  font-size: 0.95rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  resize: vertical;
}

.other-reason-input textarea:focus {
  outline: none;
  border-color: var(--primary);
}


/* Info Box */
.info-box {
  background: #f9fafb;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 2rem;
}

.info-box h4 {
  font-size: 0.95rem;
  margin-bottom: 0.8rem;
}

.info-box ul {
  list-style: disc inside;
  font-size: 0.85rem;
  color: #555;
  line-height: 1.6;
}

/* Buttons */
.action-buttons {
  display: flex;
  gap: 1rem;
}

.btn {
  flex: 1;
  padding: 1rem;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
}

.btn.outline {
  background: white;
  border: 1px solid #ddd;
  color: #333;
}

.btn.primary {
  background: var(--primary);
  border: none;
  color: #004d40;
}

.btn.primary:disabled,
.btn.danger:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}

.btn.danger {
  background: var(--primary);
  border: none;
  color: #004d40;
}

.btn.full {
  width: 100%;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  max-width: 400px;
  width: 90%;
  text-align: center;
}

.modal-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.modal-icon.success {
  width: 60px;
  height: 60px;
  background: var(--primary);
  color: #004d40;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
  font-size: 1.5rem;
}

.modal-icon.error {
  width: 60px;
  height: 60px;
  background: #fee2e2;
  color: #dc2626;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
  font-size: 2rem;
  font-weight: bold;
}


.modal-content h2 {
  font-size: 1.2rem;
  margin-bottom: 0.8rem;
}

.modal-content p {
  font-size: 0.9rem;
  color: #666;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  gap: 0.8rem;
}

.modal-actions .btn {
  flex: 1;
}
</style>
