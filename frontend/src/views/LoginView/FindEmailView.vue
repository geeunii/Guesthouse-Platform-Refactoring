<script setup>
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { findEmail } from '@/api/authClient';

const router = useRouter();
const name = ref('');
const phone = ref('');
const foundEmail = ref('');
const errorMessage = ref('');
const isLoading = ref(false);

watch(phone, (newValue) => {
  let cleaned = newValue.replace(/\D/g, '');
  let formatted = '';

  if (cleaned.startsWith('010') && cleaned.length > 3) {
    if (cleaned.length < 8) {
      formatted = cleaned.substring(0, 3) + '-' + cleaned.substring(3);
    } else {
      formatted = cleaned.substring(0, 3) + '-' + cleaned.substring(3, 7) + '-' + cleaned.substring(7, 11);
    }
  } else if (cleaned.length > 0) {
    formatted = cleaned;
  }
  
  if (formatted !== newValue) {
    phone.value = formatted;
  }
});

const handleFindEmail = async () => {
    errorMessage.value = '';
    foundEmail.value = '';

    if (!name.value || !phone.value) {
        errorMessage.value = '이름과 전화번호를 모두 입력해주세요.';
        return;
    }

    isLoading.value = true;
    try {
        const response = await findEmail({ name: name.value, phone: phone.value });
        if (response.ok && response.data) {
            foundEmail.value = response.data.email;
        } else {
            errorMessage.value = response.data?.message || '일치하는 이메일을 찾을 수 없습니다.';
        }
    } catch (error) {
        console.error('이메일 찾기 오류:', error);
        errorMessage.value = '이메일 찾기 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
    } finally {
        isLoading.value = false;
    }
};

const goToLogin = () => {
    router.push('/login');
};
</script>

<template>
    <div class="find-email-page">
        <div class="find-email-container">
            <h1 class="page-title">이메일 찾기</h1>

            <div v-if="!foundEmail" class="form-section">
                <p class="description">회원가입 시 입력했던 <br/>이름과 전화번호를 입력해주세요.</p>
                <div class="input-group">
                    <label for="name">이름</label>
                    <input type="text" id="name" v-model="name" placeholder="이름을 입력하세요" />
                </div>
                <div class="input-group">
                    <label for="phone">전화번호</label>
                    <input type="tel" id="phone" v-model="phone" placeholder="010-1234-5678" />
                </div>

                <button class="submit-btn" @click="handleFindEmail" :disabled="isLoading">
                    {{ isLoading ? '찾는 중...' : '이메일 찾기' }}
                </button>
                <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
            </div>

            <div v-else class="result-section">
                <p class="success-message">회원님의 이메일 주소입니다.</p>
                <p class="found-email-display">{{ foundEmail }}</p>
                <button class="go-to-login-btn" @click="goToLogin">로그인 페이지로 이동</button>
            </div>
        </div>
    </div>
</template>

<style scoped>
.find-email-page {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: #f9fafb;
    padding: 1rem;
}

.find-email-container {
    background: white;
    max-width: 400px;
    width: 100%;
    border-radius: 16px;
    padding: 2rem;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    text-align: center;
}

.page-title {
    font-size: 1.8rem;
    font-weight: 700;
    color: #333;
    margin-bottom: 1.5rem;
}

.description {
    font-size: 0.95rem;
    color: #666;
    margin-bottom: 1.5rem;
}

.form-section {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
}

.input-group {
    text-align: left;
}

.input-group label {
    display: block;
    font-size: 0.9rem;
    font-weight: 600;
    color: #333;
    margin-bottom: 0.5rem;
}

.input-group input {
    width: 100%;
    padding: 0.8rem;
    border: 1px solid #ddd;
    border-radius: 8px;
    font-size: 1rem;
    outline: none;
}

.input-group input:focus {
    border-color: var(--primary);
}

.submit-btn {
    width: 100%;
    padding: 1rem;
    background: var(--primary);
    color: #004d40;
    border: none;
    border-radius: 8px;
    font-size: 1.1rem;
    font-weight: 700;
    cursor: pointer;
    transition: background 0.3s ease;
    margin-top: 1rem;
}

.submit-btn:hover {
    background: #004d40;
    color: white;
}

.submit-btn:disabled {
    background: #e5e7eb;
    color: #9ca3af;
    cursor: not-allowed;
}

.error-message {
    color: #dc2626;
    font-size: 0.9rem;
    margin-top: 1rem;
}

.result-section {
    margin-top: 2rem;
}

.success-message {
    font-size: 1rem;
    color: var(--primary);
    margin-bottom: 1rem;
    font-weight: 600;
}

.found-email-display {
    font-size: 1.3rem;
    font-weight: 700;
    color: #333;
    background: #f0f7f6;
    padding: 1rem;
    border-radius: 8px;
    margin-bottom: 2rem;
    word-break: break-all;
}

.go-to-login-btn {
    width: 100%;
    padding: 1rem;
    background: #333;
    color: white;
    border: none;
    border-radius: 8px;
    font-size: 1.1rem;
    font-weight: 700;
    cursor: pointer;
    transition: background 0.3s ease;
}

.go-to-login-btn:hover {
    background: #555;
}
</style>
