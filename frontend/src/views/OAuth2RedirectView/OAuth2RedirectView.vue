<script setup>
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { saveTokens, getCurrentUser, saveUserInfo } from '@/api/authClient'

const router = useRouter()
const route = useRoute()

onMounted(async () => {
  // URL 파라미터에서 토큰 추출
  const accessToken = route.query.accessToken
  const refreshToken = route.query.refreshToken

  if (!accessToken || !refreshToken) {
    console.error('토큰이 없습니다.')
    router.push('/login')
    return
  }

  // 토큰 저장
  saveTokens(accessToken, refreshToken)

  try {
    // 사용자 정보 가져오기
    const response = await getCurrentUser()
    if (response.ok) {
      saveUserInfo(response.data)
      // 메인 페이지로 이동
      router.push('/')
    } else {
      // 사용자 정보를 가져오는데 실패하면 로그인 페이지로 리디렉션
      console.error('사용자 정보를 가져오는데 실패했습니다.')
      router.push('/login')
    }
  } catch (error) {
    console.error('사용자 정보 처리 중 오류 발생:', error)
    router.push('/login')
  }
})
</script>

<template>
  <div class="redirect-page">
    <div class="loading-container">
      <div class="spinner"></div>
      <p>로그인 처리 중...</p>
    </div>
  </div>
</template>

<style scoped>
.redirect-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
}

.loading-container {
  text-align: center;
}

.spinner {
  width: 50px;
  height: 50px;
  margin: 0 auto 1rem;
  border: 4px solid #e5e7eb;
  border-top: 4px solid #333;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-container p {
  font-size: 1rem;
  color: #666;
}
</style>
