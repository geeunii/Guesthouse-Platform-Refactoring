<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUser, updateUserProfile } from '@/api/userClient'
import DefaultAvatar from '@/assets/default-avatar.svg'; // Import the default avatar SVG

const router = useRouter()
const user = ref({
  name: '',
  email: '',
  phone: '',
  nickname: '',
  gender: ''
})
const editableUser = ref({})
const isLoading = ref(true)
const isEditMode = ref(false)
const error = ref('')
const validationErrors = ref({ nickname: '', phone: '' })
const updateMessage = ref('')
const updateMessageType = ref('') // 'success' or 'error'

watch(() => editableUser.value.phone, (newVal, oldVal) => {
  if (typeof newVal !== 'string') return;
  // This watcher handles auto-hyphenation
  const digits = newVal.replace(/\D/g, '').slice(0, 11);
  let formatted = digits;
  if (digits.length > 3 && digits.length <= 7) {
    formatted = `${digits.slice(0, 3)}-${digits.slice(3)}`;
  } else if (digits.length > 7) {
    formatted = `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
  }
  // To avoid infinite loops, only update if the formatted value is different
  if (formatted !== newVal) {
    editableUser.value.phone = formatted;
  }
});


const loadUserData = async () => {
  try {
    isLoading.value = true
    const response = await getCurrentUser()

    if (response.ok && response.data) {
      user.value = {
        name: response.data.name || '',
        email: response.data.email || '',
        phone: response.data.phone || '정보 없음',
        nickname: response.data.nickname || '',
        gender: response.data.gender || ''
      }
    } else {
      error.value = '사용자 정보를 불러오지 못했습니다.'
    }
  } catch (err) {
    console.error('Failed to load user data:', err)
    error.value = '사용자 정보를 불러오는 중 오류가 발생했습니다.'
  } finally {
    isLoading.value = false
  }
}

const startEdit = () => {
  editableUser.value = { ...user.value }
  validationErrors.value = { nickname: '', phone: '' } // Clear previous errors
  updateMessage.value = '' // Clear update message
  updateMessageType.value = ''
  isEditMode.value = true
}

const cancelEdit = () => {
  updateMessage.value = '' // Clear update message
  updateMessageType.value = ''
  isEditMode.value = false
}

const validateProfile = () => {
  let isValid = true;
  validationErrors.value = { nickname: '', phone: '' }; // Reset errors

  // Nickname validation
  const nickname = editableUser.value.nickname.trim();
  if (nickname.length < 2 || nickname.length > 10) {
    validationErrors.value.nickname = '닉네임은 2자 이상 10자 이하로 입력해주세요.';
    isValid = false;
  }

  // Phone number validation
  const phoneRegex = /^010-\d{4}-\d{4}$/;
  if (!phoneRegex.test(editableUser.value.phone)) {
    validationErrors.value.phone = '올바른 전화번호 형식이 아닙니다. (010-1234-5678)';
    isValid = false;
  }

  return isValid;
}

const handleProfileUpdate = async () => {
  updateMessage.value = '' // Clear previous message
  updateMessageType.value = ''

  if (!validateProfile()) {
    // If validation fails, do not proceed
    return;
  }

  try {
    const response = await updateUserProfile({
      nickname: editableUser.value.nickname,
      phone: editableUser.value.phone,
      gender: editableUser.value.gender,
    })

    if (response.ok) {
      user.value.nickname = editableUser.value.nickname
      user.value.phone = editableUser.value.phone
      user.value.gender = editableUser.value.gender
      updateMessage.value = '변경에 성공했습니다.'
      updateMessageType.value = 'success'
      // 2초 후 편집 모드 종료
      setTimeout(() => {
        isEditMode.value = false
        updateMessage.value = ''
        updateMessageType.value = ''
      }, 2000)
    } else {
      // 백엔드에서 보낸 에러 메시지 표시
      const errorMessage = response.data?.message || '프로필 업데이트에 실패했습니다.'
      updateMessage.value = errorMessage
      updateMessageType.value = 'error'
    }
  } catch (err) {
    console.error('Failed to update profile:', err)
    updateMessage.value = '프로필 업데이트 중 오류가 발생했습니다.'
    updateMessageType.value = 'error'
  }
}

const getGenderText = (gender) => {
  if (gender === 'MALE') return '남성'
  if (gender === 'FEMALE') return '여성'
  return ''
}

const goBack = () => {
  if (isEditMode.value) {
    cancelEdit()
  } else {
    router.back()
  }
}

onMounted(() => {
  loadUserData()
})
</script>

<template>
  <div class="profile-page">
    <!-- Header -->
    <div class="header-wrapper">
      <div class="header-content">
        <button class="back-btn" @click="goBack">
          <span class="back-icon">←</span>
        </button>
        <h1 class="page-title">프로필</h1>
        <div class="header-actions">
          <template v-if="!isEditMode">
            <button class="icon-btn edit-btn" @click="startEdit">
              <span>✏️</span>
            </button>
          </template>
          <template v-else>
            <button class="action-btn cancel-btn" @click="cancelEdit">취소</button>
            <button class="action-btn save-btn" @click="handleProfileUpdate">저장</button>
          </template>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="loading-state">
      <div class="spinner"></div>
      <p>로딩 중...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <span class="error-icon">⚠️</span>
      <p>{{ error }}</p>
    </div>

    <!-- Profile Content -->
    <div v-else class="profile-content">
      <!-- Avatar Section -->
      <div class="avatar-section">
        <div class="avatar-container">
          <div class="avatar-circle">
            <img :src="DefaultAvatar" alt="Default Avatar" class="profile-avatar-img" />
          </div>
          <div class="avatar-badge" :class="{ 'edit-mode': isEditMode }">
            {{ isEditMode ? '수정 중' : '활성' }}
          </div>
        </div>
        <div class="user-name-display">
          <h2>{{ user.name || '사용자' }}</h2>
          <p class="user-nickname">@{{ user.nickname || 'nickname' }}</p>
        </div>
      </div>

      <!-- Update Message -->
      <transition name="fade">
        <div v-if="updateMessage" class="update-message-card" :class="updateMessageType">
          <span class="message-icon">{{ updateMessageType === 'success' ? '✓' : '✕' }}</span>
          <span>{{ updateMessage }}</span>
        </div>
      </transition>

      <!-- Info Cards -->
      <div class="info-cards">
        <!-- Name Card -->
        <div class="info-card">
          <div class="card-icon">👤</div>
          <div class="card-content">
            <label class="card-label">이름</label>
            <div class="card-value">{{ user.name || '정보 없음' }}</div>
          </div>
        </div>

        <!-- Email Card -->
        <div class="info-card">
          <div class="card-icon">📧</div>
          <div class="card-content">
            <label class="card-label">이메일</label>
            <div class="card-value">{{ user.email || '정보 없음' }}</div>
            <p class="card-hint">변경할 수 없습니다</p>
          </div>
        </div>

        <!-- Nickname Card -->
        <div class="info-card" :class="{ 'editing': isEditMode }">
          <div class="card-icon">🏷️</div>
          <div class="card-content">
            <label for="nickname-input" class="card-label">닉네임</label>
            <div v-if="!isEditMode" class="card-value">{{ user.nickname || '정보 없음' }}</div>
            <template v-else>
              <input
                id="nickname-input"
                v-model="editableUser.nickname"
                type="text"
                class="card-input"
                placeholder="닉네임을 입력하세요"
              />
              <p v-if="validationErrors.nickname" class="error-text">{{ validationErrors.nickname }}</p>
            </template>
          </div>
        </div>

        <!-- Phone Card -->
        <div class="info-card" :class="{ 'editing': isEditMode }">
          <div class="card-icon">📱</div>
          <div class="card-content">
            <label for="phone-input" class="card-label">전화번호</label>
            <div v-if="!isEditMode" class="card-value">{{ user.phone || '정보 없음' }}</div>
            <template v-else>
              <input
                id="phone-input"
                v-model="editableUser.phone"
                type="tel"
                class="card-input"
                placeholder="010-1234-5678"
                maxlength="13"
              />
              <p v-if="validationErrors.phone" class="error-text">{{ validationErrors.phone }}</p>
            </template>
          </div>
        </div>

        <!-- Gender Card -->
        <div class="info-card" :class="{ 'editing': isEditMode }">
          <div class="card-icon">{{ user.gender === 'MALE' ? '♂️' : user.gender === 'FEMALE' ? '♀️' : '⚧' }}</div>
          <div class="card-content">
            <label for="gender-select" class="card-label">성별</label>
            <div v-if="!isEditMode" class="card-value">{{ getGenderText(user.gender) || '정보 없음' }}</div>
            <template v-else>
              <select id="gender-select" v-model="editableUser.gender" class="card-select">
                <option value="MALE">남성</option>
                <option value="FEMALE">여성</option>
              </select>
            </template>
          </div>
        </div>
      </div>

      <!-- Delete Account Section -->
      <div v-if="!isEditMode" class="danger-zone">
        <button class="delete-account-btn" @click="router.push('/delete-account')">
          <span class="delete-icon">🗑️</span>
          <span>회원 탈퇴</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Reset & Base Styles */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.profile-page {
  min-height: 100vh;
  background: #ffffff;
  padding-bottom: 2rem;
}

/* Header */
.header-wrapper {
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid #f0f0f0;
}

.header-content {
  max-width: 600px;
  margin: 0 auto;
  padding: 1rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #333;
  flex: 1;
  text-align: center;
}

.back-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
  transition: transform 0.2s;
}

.back-btn:hover {
  transform: translateX(-4px);
}

.back-icon {
  font-size: 1.5rem;
  color: #666;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.icon-btn {
  background: #BFE7DF;
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.icon-btn:hover {
  background: #a8d6cc;
  transform: translateY(-2px);
}

.icon-btn span {
  font-size: 1.2rem;
}

.action-btn {
  padding: 0.6rem 1.2rem;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 0.9rem;
}

.save-btn {
  background: #BFE7DF;
  color: #333;
}

.save-btn:hover {
  background: #a8d6cc;
  transform: translateY(-2px);
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.cancel-btn:hover {
  background: #e8e8e8;
}

/* Loading & Error States */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  color: #666;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f0f0f0;
  border-top-color: #BFE7DF;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state p {
  font-size: 1.1rem;
  font-weight: 500;
}

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4rem 2rem;
  color: #666;
}

.error-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.error-state p {
  font-size: 1.1rem;
}

/* Profile Content */
.profile-content {
  max-width: 600px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

/* Avatar Section */
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 2rem;
}

.avatar-container {
  position: relative;
  margin-bottom: 1rem;
}

.avatar-circle {
  width: 120px;
  height: 120px;
  /* background: #BFE7DF; Removed, as SVG provides background */
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 3px solid #ffffff;
  overflow: hidden; /* Ensure SVG is clipped to circle */
}

.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover; /* Cover the circle area */
  border-radius: 50%; /* Apply border-radius to the image itself for consistency */
}

/* .avatar-emoji is no longer used for the image, remove its font-size if it was specific to the emoji */

.avatar-badge {
  position: absolute;
  bottom: 0;
  right: -5px;
  background: #A4DD6E;
  color: #333;
  padding: 0.3rem 0.8rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.avatar-badge.edit-mode {
  background: #FFB84D;
  color: #333;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.user-name-display {
  text-align: center;
  color: #333;
}

.user-name-display h2 {
  font-size: 1.8rem;
  font-weight: 700;
  margin-bottom: 0.3rem;
}

.user-nickname {
  font-size: 1rem;
  color: #666;
  font-weight: 500;
}

/* Update Message Card */
.update-message-card {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 1rem 1.5rem;
  border-radius: 12px;
  margin-bottom: 1.5rem;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.update-message-card.success {
  background: #BFE7DF;
  color: #333;
  border: 1px solid #a8d6cc;
}

.update-message-card.error {
  background: #ffe5e5;
  color: #d32f2f;
  border: 1px solid #ffcccc;
}

.message-icon {
  font-size: 1.5rem;
  font-weight: bold;
}

/* Fade Transition */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Info Cards */
.info-cards {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.info-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 1.5rem;
  display: flex;
  gap: 1rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
  transition: all 0.3s;
}

.info-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-color: #BFE7DF;
}

.info-card.editing {
  border: 2px solid #BFE7DF;
  box-shadow: 0 2px 8px rgba(191, 231, 223, 0.3);
}

.card-icon {
  font-size: 2rem;
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f8f8;
  border-radius: 12px;
  flex-shrink: 0;
}

.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.card-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #888;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.card-value {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
}

.card-hint {
  font-size: 0.8rem;
  color: #999;
  font-style: italic;
}

.card-input,
.card-select {
  width: 100%;
  padding: 0.8rem 1rem;
  font-size: 1rem;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  background: #fafafa;
  transition: all 0.3s;
  font-weight: 500;
  color: #333;
}

.card-input:focus,
.card-select:focus {
  outline: none;
  border-color: #BFE7DF;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(191, 231, 223, 0.2);
}

.card-select {
  cursor: pointer;
}

.error-text {
  color: #d32f2f;
  font-size: 0.85rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.error-text::before {
  content: '⚠️';
}

/* Danger Zone */
.danger-zone {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid #f0f0f0;
  text-align: center;
}

.delete-account-btn {
  background: #ffffff;
  color: #d32f2f;
  border: 2px solid #ffcccc;
  padding: 1rem 2rem;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s;
}

.delete-account-btn:hover {
  background: #ffe5e5;
  border-color: #d32f2f;
  transform: translateY(-2px);
}

.delete-icon {
  font-size: 1.2rem;
}

/* Responsive Design */
@media (max-width: 640px) {
  .profile-content {
    padding: 1.5rem 1rem;
  }

  .header-content {
    padding: 1rem;
  }

  .page-title {
    font-size: 1.3rem;
  }

  .avatar-circle {
    width: 100px;
    height: 100px;
  }

  .profile-avatar-img {
    width: 100%;
    height: 100%;
  }

  .user-name-display h2 {
    font-size: 1.5rem;
  }

  .info-card {
    padding: 1.2rem;
  }

  .card-icon {
    font-size: 1.8rem;
    width: 45px;
    height: 45px;
  }
}
</style>
