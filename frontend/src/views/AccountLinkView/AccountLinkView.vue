<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { saveTokens, linkSocialAccount, saveUserInfo } from '@/api/authClient'

const router = useRouter()
const route = useRoute()
const isLoading = ref(false)
const error = ref(null)

// URL 파라미터
const email = ref('')
const provider = ref('')
const providerId = ref('')

// 소셜 제공자 표시 이름
const providerDisplayName = computed(() => {
  const names = {
    GOOGLE: '구글',
    KAKAO: '카카오',
    NAVER: '네이버'
  }
  return names[provider.value] || provider.value
})

// 소셜 제공자 아이콘/색상
const providerStyle = computed(() => {
  const styles = {
    GOOGLE: { bg: '#fff', color: '#4285f4', border: '#dadce0' },
    KAKAO: { bg: '#FEE500', color: '#000', border: '#FEE500' },
    NAVER: { bg: '#03C75A', color: '#fff', border: '#03C75A' }
  }
  return styles[provider.value] || { bg: '#f3f4f6', color: '#333', border: '#e5e7eb' }
})

onMounted(() => {
  const accessToken = route.query.accessToken
  const refreshToken = route.query.refreshToken

  if (accessToken && refreshToken) {
    // 토큰 저장
    saveTokens(accessToken, refreshToken)

    // URL 파라미터 추출
    email.value = route.query.email || ''
    provider.value = route.query.provider || ''
    providerId.value = route.query.providerId || ''

    if (!provider.value || !providerId.value) {
      error.value = '잘못된 접근입니다.'
    }
  } else {
    error.value = '토큰이 없습니다.'
    setTimeout(() => router.push('/login'), 2000)
  }
})

// 계정 연결 동의
const handleLink = async () => {
  isLoading.value = true
  error.value = null

  try {
    const response = await linkSocialAccount(provider.value, providerId.value)

    if (response.ok && response.data) {
      // 사용자 정보 저장
      saveUserInfo(response.data)

      // 성공 - 메인 페이지로 이동
      router.push('/')
    } else {
      error.value = response.error || '계정 연결에 실패했습니다.'
    }
  } catch (err) {
    console.error('계정 연결 오류:', err)
    error.value = '계정 연결 중 오류가 발생했습니다.'
  } finally {
    isLoading.value = false
  }
}

// 취소
const handleCancel = () => {
  // 토큰 삭제 후 로그인 페이지로
  sessionStorage.removeItem('accessToken')
  sessionStorage.removeItem('refreshToken')
  router.push('/login')
}
</script>

<template>
  <div class="account-link-page">
    <div class="account-link-container">
      <!-- Header -->
      <div class="page-header">
        <div class="icon-wrapper">
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
            <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
          </svg>
        </div>
        <h1>계정 연결</h1>
      </div>

      <!-- Error State -->
      <div v-if="error" class="error-state">
        <p>{{ error }}</p>
        <button class="btn-secondary" @click="handleCancel">로그인으로 돌아가기</button>
      </div>

      <!-- Main Content -->
      <template v-else>
        <div class="info-section">
          <p class="info-message">
            <strong>{{ email }}</strong>
            <span class="info-text">계정이 이미 존재합니다.</span>
          </p>
          <p class="sub-message">
            <span
              class="provider-badge"
              :style="{
                backgroundColor: providerStyle.bg,
                color: providerStyle.color,
                borderColor: providerStyle.border
              }"
            >
              {{ providerDisplayName }}
            </span>
            <span>계정과 연결하시겠습니까?</span>
          </p>
        </div>

        <div class="benefit-section">
          <h3>연결하면 이런 점이 좋아요</h3>
          <ul>
            <li>
              <span class="check-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>
              </span>
              기존 예약 내역, 찜 목록, 쿠폰을 그대로 사용할 수 있어요
            </li>
            <li>
              <span class="check-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>
              </span>
              이메일/비밀번호 또는 {{ providerDisplayName }}로 로그인할 수 있어요
            </li>
            <li>
              <span class="check-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>
              </span>
              더 빠르고 간편하게 로그인할 수 있어요
            </li>
          </ul>
        </div>

        <div class="action-buttons">
          <button
            class="btn-primary"
            @click="handleLink"
            :disabled="isLoading"
          >
            {{ isLoading ? '연결 중...' : '계정 연결하기' }}
          </button>
          <button
            class="btn-secondary"
            @click="handleCancel"
            :disabled="isLoading"
          >
            취소
          </button>
        </div>

        <p class="notice">
          취소하면 {{ providerDisplayName }} 계정으로 로그인되지 않습니다.<br/>
          기존 이메일/비밀번호로 로그인해 주세요.
        </p>
      </template>
    </div>
  </div>
</template>

<style scoped>
.account-link-page {
  min-height: 100vh;
  background: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.account-link-container {
  background: white;
  width: 100%;
  max-width: 460px;
  border-radius: 20px;
  padding: 2.5rem 2rem;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
}

.page-header {
  text-align: center;
  margin-bottom: 2rem;
}

.icon-wrapper {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #BFE7DF 0%, #a8ddd2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.25rem;
  color: #004d40;
}

.page-header h1 {
  font-size: 1.6rem;
  font-weight: 800;
  color: #1f2937;
  margin: 0;
}

.error-state {
  text-align: center;
  padding: 2rem 0;
}

.error-state p {
  color: #dc2626;
  font-size: 1rem;
  margin-bottom: 1.5rem;
}

.info-section {
  text-align: center;
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: #f0fdf9;
  border-radius: 12px;
  border: 1px solid #BFE7DF;
}

.info-message {
  font-size: 1.1rem;
  color: #1f2937;
  margin: 0 0 0.75rem;
  line-height: 1.6;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
}

.info-message strong {
  color: #004d40;
  word-break: break-all;
}

.info-message .info-text {
  color: #1f2937;
}

.sub-message {
  font-size: 1rem;
  color: #4b5563;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.provider-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.35rem 0.75rem;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 600;
  border: 1px solid;
}

.benefit-section {
  margin-bottom: 2rem;
}

.benefit-section h3 {
  font-size: 1rem;
  font-weight: 700;
  color: #374151;
  margin: 0 0 1rem;
}

.benefit-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.benefit-section li {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.75rem 0;
  font-size: 0.95rem;
  color: #4b5563;
  line-height: 1.5;
}

.check-icon {
  width: 22px;
  height: 22px;
  background: #BFE7DF;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #004d40;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.btn-primary, .btn-secondary {
  width: 100%;
  padding: 1rem;
  border-radius: 12px;
  font-size: 1.05rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.btn-primary {
  background: #BFE7DF;
  color: #004d40;
  box-shadow: 0 4px 14px rgba(191, 231, 223, 0.4);
}

.btn-primary:hover:not(:disabled) {
  background: #a8ddd2;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(191, 231, 223, 0.5);
}

.btn-primary:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
  box-shadow: none;
}

.btn-secondary {
  background: white;
  color: #4b5563;
  border-color: #e5e7eb;
}

.btn-secondary:hover:not(:disabled) {
  background: #f9fafb;
  border-color: #d1d5db;
}

.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.notice {
  text-align: center;
  font-size: 0.85rem;
  color: #9ca3af;
  line-height: 1.6;
  margin: 0;
}

/* 모바일 반응형 */
@media (max-width: 480px) {
  .account-link-page {
    padding: 0;
    align-items: flex-start;
  }

  .account-link-container {
    border-radius: 0;
    padding: 1.5rem 1.25rem;
    min-height: 100vh;
    box-shadow: none;
  }

  .page-header {
    margin-bottom: 1.5rem;
  }

  .icon-wrapper {
    width: 64px;
    height: 64px;
    margin-bottom: 1rem;
  }

  .icon-wrapper svg {
    width: 36px;
    height: 36px;
  }

  .page-header h1 {
    font-size: 1.4rem;
  }

  .info-section {
    padding: 1.25rem 1rem;
    margin-bottom: 1.5rem;
  }

  .info-message {
    font-size: 1rem;
  }

  .info-message strong {
    font-size: 0.95rem;
  }

  .sub-message {
    font-size: 0.95rem;
    flex-direction: column;
    gap: 0.5rem;
  }

  .provider-badge {
    font-size: 0.85rem;
    padding: 0.3rem 0.6rem;
  }

  .benefit-section {
    margin-bottom: 1.5rem;
  }

  .benefit-section h3 {
    font-size: 0.95rem;
    margin-bottom: 0.75rem;
  }

  .benefit-section li {
    font-size: 0.9rem;
    padding: 0.6rem 0;
    gap: 0.6rem;
  }

  .check-icon {
    width: 20px;
    height: 20px;
  }

  .check-icon svg {
    width: 12px;
    height: 12px;
  }

  .action-buttons {
    margin-bottom: 1.25rem;
  }

  .btn-primary, .btn-secondary {
    padding: 0.9rem;
    font-size: 1rem;
    border-radius: 10px;
  }

  .notice {
    font-size: 0.8rem;
  }
}

/* 아주 작은 화면 (320px) */
@media (max-width: 360px) {
  .account-link-container {
    padding: 1.25rem 1rem;
  }

  .icon-wrapper {
    width: 56px;
    height: 56px;
  }

  .icon-wrapper svg {
    width: 32px;
    height: 32px;
  }

  .page-header h1 {
    font-size: 1.25rem;
  }

  .info-message {
    font-size: 0.95rem;
  }

  .benefit-section li {
    font-size: 0.85rem;
  }

  .btn-primary, .btn-secondary {
    padding: 0.85rem;
    font-size: 0.95rem;
  }
}
</style>
