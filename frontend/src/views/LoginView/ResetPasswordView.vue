<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { resetPassword } from '@/api/authClient';

const router = useRouter();
const route = useRoute();

const email = ref('');
const code = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const errorMessage = ref('');
const isLoading = ref(false);

// 비밀번호 유효성 검사
const passwordRequirements = ref({
    minLength: false,
    hasLetter: false,
    hasNumber: false,
    hasSpecial: false
});

const validatePassword = (password) => {
    passwordRequirements.value.minLength = password.length >= 8;
    passwordRequirements.value.hasLetter = /[A-Za-z]/.test(password);
    passwordRequirements.value.hasNumber = /\d/.test(password);
    passwordRequirements.value.hasSpecial = /[@$!%*#?&]/.test(password);
};

const isPasswordValid = () => {
    return Object.values(passwordRequirements.value).every(req => req);
};

// 비밀번호 입력 시 유효성 검사
const handlePasswordInput = () => {
    validatePassword(newPassword.value);
};

// 비밀번호 재설정
const handleResetPassword = async () => {
    errorMessage.value = '';

    if (!newPassword.value || !confirmPassword.value) {
        errorMessage.value = '모든 필드를 입력해주세요.';
        return;
    }

    if (!isPasswordValid()) {
        errorMessage.value = '비밀번호가 요구사항을 충족하지 않습니다.';
        return;
    }

    if (newPassword.value !== confirmPassword.value) {
        errorMessage.value = '비밀번호가 일치하지 않습니다.';
        return;
    }

    isLoading.value = true;
    try {
        const response = await resetPassword({
            email: email.value,
            code: code.value,
            newPassword: newPassword.value
        });

        if (response.ok) {
            alert('비밀번호가 성공적으로 변경되었습니다.');
            router.push('/login');
        } else {
            errorMessage.value = response.data?.message || '비밀번호 재설정 중 오류가 발생했습니다.';
        }
    } catch (error) {
        console.error('비밀번호 재설정 오류:', error);
        errorMessage.value = '비밀번호 재설정 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
    } finally {
        isLoading.value = false;
    }
};

// 페이지 로드 시 쿼리 파라미터에서 이메일과 코드 가져오기
onMounted(() => {
    email.value = route.query.email || '';
    code.value = route.query.code || '';

    if (!email.value || !code.value) {
        alert('잘못된 접근입니다.');
        router.push('/find-password');
    }
});
</script>

<template>
    <div class="reset-password-page">
        <div class="reset-password-container">
            <h1 class="page-title">비밀번호 재설정</h1>

            <div class="form-section">
                <p class="description">새로운 비밀번호를 입력해주세요.</p>

                <div class="input-group">
                    <label for="newPassword">새 비밀번호</label>
                    <input
                        type="password"
                        id="newPassword"
                        v-model="newPassword"
                        @input="handlePasswordInput"
                        placeholder="새 비밀번호"
                    />
                </div>

                <div class="password-requirements">
                    <p class="requirement-title">비밀번호 요구사항:</p>
                    <ul>
                        <li :class="{ valid: passwordRequirements.minLength }">
                            8자 이상
                        </li>
                        <li :class="{ valid: passwordRequirements.hasLetter }">
                            영문 포함
                        </li>
                        <li :class="{ valid: passwordRequirements.hasNumber }">
                            숫자 포함
                        </li>
                        <li :class="{ valid: passwordRequirements.hasSpecial }">
                            특수문자 포함 (@$!%*#?&)
                        </li>
                    </ul>
                </div>

                <div class="input-group">
                    <label for="confirmPassword">비밀번호 확인</label>
                    <input
                        type="password"
                        id="confirmPassword"
                        v-model="confirmPassword"
                        placeholder="비밀번호 확인"
                    />
                </div>

                <button class="submit-btn" @click="handleResetPassword" :disabled="isLoading || !isPasswordValid()">
                    {{ isLoading ? '변경 중...' : '비밀번호 변경' }}
                </button>

                <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
            </div>
        </div>
    </div>
</template>

<style scoped>
.reset-password-page {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: #f9fafb;
    padding: 1rem;
}

.reset-password-container {
    background: white;
    max-width: 450px;
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

.password-requirements {
    text-align: left;
    background: #f9fafb;
    padding: 1rem;
    border-radius: 8px;
    margin-top: -0.5rem;
}

.requirement-title {
    font-size: 0.85rem;
    font-weight: 600;
    color: #333;
    margin-bottom: 0.5rem;
}

.password-requirements ul {
    list-style: none;
    padding: 0;
    margin: 0;
}

.password-requirements li {
    font-size: 0.85rem;
    color: #9ca3af;
    padding: 0.25rem 0;
    transition: color 0.3s ease;
}

.password-requirements li::before {
    content: '✗ ';
    color: #dc2626;
    font-weight: bold;
    margin-right: 0.5rem;
}

.password-requirements li.valid {
    color: #059669;
}

.password-requirements li.valid::before {
    content: '✓ ';
    color: #059669;
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
}

.submit-btn:hover:not(:disabled) {
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
}
</style>
