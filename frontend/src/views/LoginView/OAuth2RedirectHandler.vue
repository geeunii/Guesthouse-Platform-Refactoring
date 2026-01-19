<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { saveUserInfo } from '@/api/authClient'

const router = useRouter()
const loading = ref(true)
const error = ref(null)

// JWT 토큰 디코딩 함수
const decodeJWT = (token) => {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch (e) {
    console.error('JWT 디코딩 실패:', e)
    return null
  }
}

onMounted(async () => {
  try {
    // URL에서 토큰 파라미터 추출
    const urlParams = new URLSearchParams(window.location.search)
    const accessToken = urlParams.get('accessToken')
    const refreshToken = urlParams.get('refreshToken')
    const redirectParam = urlParams.get('redirect')
    const storedRedirect = sessionStorage.getItem('postLoginRedirect')

    if (!accessToken || !refreshToken) {
      throw new Error('토큰을 받지 못했습니다.')
    }

    // 토큰 저장
    sessionStorage.setItem('accessToken', accessToken)
    sessionStorage.setItem('refreshToken', refreshToken)

    // 서버에서 사용자 정보 가져오기 (userId 포함)
    const { getCurrentUser } = await import('@/api/authClient')
    const userResponse = await getCurrentUser()
    if (userResponse.ok && userResponse.data) {
      saveUserInfo(userResponse.data)
    } else {
      // 서버 응답 실패 시 JWT에서 기본 정보라도 추출
      const decoded = decodeJWT(accessToken)
      if (decoded) {
        const userInfo = {
          email: decoded.sub,
          role: decoded.authorities?.[0]?.replace('ROLE_', '') || 'USER',
          userId: decoded.userId || decoded.id || null
        }
        saveUserInfo(userInfo)
      }
    }

    // 로그인 성공 - 홈으로 리다이렉트
    const redirectTarget =
      (redirectParam && redirectParam.startsWith('/') ? redirectParam : '') ||
      (storedRedirect && storedRedirect.startsWith('/') ? storedRedirect : '') ||
      '/'
    sessionStorage.removeItem('postLoginRedirect')
    setTimeout(() => {
      router.push(redirectTarget)
    }, 1000)
  } catch (err) {
    console.error('OAuth2 로그인 처리 중 오류:', err)
    error.value = err.message || '소셜 로그인에 실패했습니다.'

    // 3초 후 로그인 페이지로 리다이렉트
    setTimeout(() => {
      sessionStorage.removeItem('postLoginRedirect')
      router.push('/login')
    }, 3000)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="oauth2-redirect-page">
    <div class="redirect-container">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <h2>로그인 처리 중...</h2>
        <p>잠시만 기다려주세요</p>
      </div>

      <div v-else-if="error" class="error-state">
        <div class="error-icon">✕</div>
        <h2>로그인 실패</h2>
        <p>{{ error }}</p>
        <p class="redirect-info">로그인 페이지로 이동합니다...</p>
      </div>

      <div v-else class="success-state">
        <div class="success-icon">✓</div>
        <h2>로그인 성공!</h2>
        <p>메인 페이지로 이동합니다...</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.oauth2-redirect-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
}

.redirect-container {
  background: white;
  padding: 3rem 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  max-width: 400px;
  width: 100%;
  text-align: center;
}

.loading-state,
.error-state,
.success-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #00796b;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.success-icon,
.error-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  font-weight: bold;
}

.success-icon {
  background: #a5d6a7;
  color: #2e7d32;
}

.error-icon {
  background: #fee2e2;
  color: #dc2626;
}

h2 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #333;
  margin: 0;
}

p {
  font-size: 0.95rem;
  color: #666;
  margin: 0;
}

.redirect-info {
  margin-top: 1rem;
  font-size: 0.85rem;
  color: #888;
}
</style>
