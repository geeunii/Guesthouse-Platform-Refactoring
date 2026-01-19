
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''


const ACCESS_TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'
const USER_INFO_KEY = 'userInfo'

export function getAccessToken() {
  return sessionStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken() {
  return sessionStorage.getItem(REFRESH_TOKEN_KEY)
}

export function getUserInfo() {
  const userInfo = sessionStorage.getItem(USER_INFO_KEY)
  return userInfo ? JSON.parse(userInfo) : null
}

export function getUserId() {
  const userInfo = getUserInfo()
  return userInfo?.userId || userInfo?.id || null
}


export function saveTokens(accessToken, refreshToken) {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  sessionStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}


export function saveUserInfo(userInfo) {
  sessionStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
}


export function clearAuth() {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  sessionStorage.removeItem(USER_INFO_KEY)
}


export function isAuthenticated() {
  return !!getAccessToken()
}


async function apiRequest(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }


  const accessToken = getAccessToken()
  if (accessToken && !options.skipAuth) {
    headers.Authorization = `Bearer ${accessToken}`
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers
    })

    let data = null;
    try {
      data = await response.json();
    } catch (e) {
      // Ignore JSON parsing errors for non-json responses
    }

    return {
      ok: response.ok,
      status: response.status,
      data
    }
  } catch (error) {
    console.error('API Request Error:', error)
    return {
      ok: false,
      status: 0,
      data: null,
      error: error.message
    }
  }
}

// 회원가입
export async function signup(signupData) {
  return apiRequest('/api/auth/signup', {
    method: 'POST',
    body: JSON.stringify(signupData),
    skipAuth: true
  })
}

// 로그인
export async function login(email, password) {
  const response = await apiRequest('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
    skipAuth: true
  })

  // 로그인 성공 시 토큰 및 사용자 정보 저장
  if (response.ok && response.data) {
    saveTokens(response.data.accessToken, response.data.refreshToken)
    saveUserInfo({
      email: email,
      role: response.data.role,
      userId: response.data.userId
    })
  }

  return response
}

// 이메일 중복 확인
export async function checkEmailDuplicate(email) {
  return apiRequest(`/api/auth/check-email?email=${encodeURIComponent(email)}`, {
    method: 'GET',
    skipAuth: true
  })
}

// 닉네임 중복 확인
export async function checkNicknameDuplicate(nickname) {
  return apiRequest(`/api/auth/check-nickname?nickname=${encodeURIComponent(nickname)}`, {
    method: 'GET',
    skipAuth: true
  });
}

// 이메일 인증 코드 전송
export async function sendVerificationEmail(email) {
  return apiRequest('/api/auth/send-verification', {
    method: 'POST',
    body: JSON.stringify({ email }),
    skipAuth: true
  })
}

// 이메일 인증 코드 확인
export async function verifyEmailCode(email, code) {
  return apiRequest('/api/auth/verify-code', {
    method: 'POST',
    body: JSON.stringify({ email, code }),
    skipAuth: true
  })
}

// 이메일 인증 코드 확인만 (삭제하지 않음) - 비밀번호 찾기용
export async function verifyEmailCodeOnly(email, code) {
  return apiRequest('/api/auth/verify-code-only', {
    method: 'POST',
    body: JSON.stringify({ email, code }),
    skipAuth: true
  })
}

// 이메일 찾기
export async function findEmail(findEmailData) {
  return apiRequest('/api/auth/find-email', {
    method: 'POST',
    body: JSON.stringify(findEmailData),
    skipAuth: true
  })
}

// 비밀번호 찾기 - 사용자 확인 및 인증 코드 전송
export async function findPassword(findPasswordData) {
  return apiRequest('/api/auth/find-password', {
    method: 'POST',
    body: JSON.stringify(findPasswordData),
    skipAuth: true
  })
}

// 비밀번호 재설정
export async function resetPassword(resetPasswordData) {
  return apiRequest('/api/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(resetPasswordData),
    skipAuth: true
  })
}

// 토큰 갱신
export async function refreshAccessToken() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return { ok: false, status: 401, data: null }
  }

  const response = await apiRequest('/api/auth/refresh', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${refreshToken}`
    },
    skipAuth: true
  })

  // 토큰 갱신 성공 시 새 토큰 저장
  if (response.ok && response.data) {
    saveTokens(response.data.accessToken, response.data.refreshToken)
  }

  return response
}

// 로그아웃
export function logout() {
  clearAuth()
}

// 인증이 필요한 API 요청
export async function authenticatedRequest(endpoint, options = {}) {
  let response = await apiRequest(endpoint, options)

  // 401 에러 시 토큰 갱신 시도
  if (response.status === 401) {
    const refreshResponse = await refreshAccessToken()

    if (refreshResponse.ok) {
      // 토큰 갱신 성공 시 원래 요청 재시도
      response = await apiRequest(endpoint, options)
    } else {
      // 토큰 갱신 실패 시 로그아웃
      clearAuth()
      window.location.href = '/login';
    }
  }

  return response
}

// 현재 로그인한 사용자 정보 조회
export async function getCurrentUser() {
  return authenticatedRequest('/api/users/me', {
    method: 'GET'
  })
}

// 소셜 회원가입 완료
export async function completeSocialSignup(signupData) {
  return authenticatedRequest('/api/auth/complete-social-signup', {
    method: 'POST',
    body: JSON.stringify(signupData)
  })
}

// 자사 계정과 소셜 계정 연결
export async function linkSocialAccount(provider, providerId) {
  return authenticatedRequest('/api/auth/link-account', {
    method: 'POST',
    body: JSON.stringify({ provider, providerId })
  })
}

// 토큰 유효성 검증
export async function validateToken() {
  const accessToken = getAccessToken()
  if (!accessToken) {
    return false
  }

  try {
    const response = await getCurrentUser()
    if (response.ok) {
      return true
    } else {
      // 토큰이 유효하지 않으면 로그아웃
      clearAuth()
      return false
    }
  } catch (error) {
    // 에러 발생 시 로그아웃
    clearAuth()
    return false
  }
}

export default {
  signup,
  login,
  findEmail,
  findPassword,
  resetPassword,
  sendVerificationEmail,
  verifyEmailCode,
  verifyEmailCodeOnly,
  logout,
  checkEmailDuplicate,
  checkNicknameDuplicate,
  refreshAccessToken,
  authenticatedRequest,
  getCurrentUser,
  completeSocialSignup,
  linkSocialAccount,
  getAccessToken,
  getRefreshToken,
  getUserInfo,
  saveUserInfo,
  clearAuth,
  isAuthenticated,
  validateToken
}
