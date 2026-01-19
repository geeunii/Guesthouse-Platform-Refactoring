<script setup>
import { ref, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { findPassword, resetPassword, verifyEmailCodeOnly } from '@/api/authClient';

const router = useRouter();

// Step 1: Request Code Phase
const email = ref('');
const phone = ref('');
const isCodeSent = ref(false); // Flag to show Step 2 UI
const isVerified = ref(false); // Flag to show Step 3 UI

// Step 2: Verify Code Phase
const verificationCode = ref('');
const timer = ref(0);
let timerId = null;

// Step 3: Reset Password Phase
const newPassword = ref('');
const newPasswordConfirm = ref('');
const showPassword = ref(false);
const showPasswordConfirm = ref(false);

// Validation States
const errorMessage = ref('');
const isLoading = ref(false);

// Password criteria
const passwordCriteria = ref([
    { label: '영문', valid: false, regex: /[a-zA-Z]/ },
    { label: '숫자', valid: false, regex: /[0-9]/ },
    { label: '특수문자', valid: false, regex: /[!@#$%^&*(),.?":{}|<>]/ },
    { label: '8자 이상 20자 이하', valid: false, regex: /^.{8,20}$/ }
]);
const allPasswordCriteriaMet = computed(() => passwordCriteria.value.every(c => c.valid));

const passwordMatchState = computed(() => {
    if (!newPasswordConfirm.value) {
        return null;
    }
    if (newPassword.value === newPasswordConfirm.value) {
        return { valid: true, message: '비밀번호가 일치합니다.' };
    } else {
        return { valid: false, message: '비밀번호가 일치하지 않습니다.' };
    }
});

watch(newPassword, (newValue) => {
    passwordCriteria.value.forEach(criterion => {
        criterion.valid = criterion.regex.test(newValue);
    });
});

// Phone number auto-formatting
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

const formattedTimer = computed(() => {
    const minutes = Math.floor(timer.value / 60);
    const seconds = timer.value % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
});

const startTimer = () => {
    stopTimer();
    timer.value = 180; // 3 minutes
    timerId = setInterval(() => {
        if (timer.value > 0) {
            timer.value--;
        } else {
            stopTimer();
            // Optionally, show a message that code expired
        }
    }, 1000);
};

const stopTimer = () => {
    if (timerId) {
        clearInterval(timerId);
        timerId = null;
    }
};

const handleSendCode = async () => {
    errorMessage.value = '';
    if (!email.value || !phone.value) {
        errorMessage.value = '이메일과 전화번호를 모두 입력해주세요.';
        return;
    }

    isLoading.value = true;
    try {
        // findPassword API가 이미 사용자 확인 후 인증 코드를 전송함
        const response = await findPassword({ email: email.value, phone: phone.value });
        if (response.ok) {
            isCodeSent.value = true;
            startTimer();
            openModal('인증번호가 발송되었습니다. 이메일을 확인해주세요.', 'success');
        } else {
            errorMessage.value = response.data?.message || '사용자 정보를 찾을 수 없습니다.';
            openModal(errorMessage.value, 'error');
        }
    } catch (error) {
        console.error('비밀번호 찾기 요청 오류:', error);
        errorMessage.value = '비밀번호 찾기 요청 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
        openModal(errorMessage.value, 'error');
    } finally {
        isLoading.value = false;
    }
};

const handleVerifyCode = async () => {
    console.log('========= handleVerifyCode 호출됨 =========');
    console.log('email:', email.value);
    console.log('verificationCode:', verificationCode.value);

    errorMessage.value = '';
    if (!verificationCode.value) {
        errorMessage.value = '인증번호를 입력해주세요.';
        return;
    }

    if (verificationCode.value.length !== 6) {
        errorMessage.value = '인증번호는 6자리입니다.';
        return;
    }

    console.log('API 호출 직전...');
    isLoading.value = true;
    try {
        // 인증번호 검증 (삭제하지 않음)
        console.log('verifyEmailCodeOnly 호출 시작');
        const response = await verifyEmailCodeOnly(email.value, verificationCode.value);
        console.log('========= API 응답 받음 =========');
        console.log('인증번호 확인 응답:', response);
        console.log('response.ok:', response.ok);
        console.log('response.data:', response.data);
        console.log('response.data === true:', response.data === true);

        if (response.ok && response.data === true) {
            console.log('인증 성공! isVerified를 true로 변경');
            stopTimer();
            isVerified.value = true;
            openModal('인증이 완료되었습니다. 새 비밀번호를 설정해주세요.', 'success');
        } else {
            console.log('인증 실패 - response:', response);
            errorMessage.value = '인증번호가 올바르지 않거나 만료되었습니다.';
            openModal(errorMessage.value, 'error');
        }
    } catch (error) {
        console.error('========= 인증번호 확인 오류 =========');
        console.error('에러:', error);
        errorMessage.value = '인증번호 확인 중 오류가 발생했습니다.';
        openModal(errorMessage.value, 'error');
    } finally {
        isLoading.value = false;
        console.log('========= handleVerifyCode 종료 =========');
    }
};

const handleResetPassword = async () => {
    errorMessage.value = '';
    if (!newPassword.value || !newPasswordConfirm.value) {
        errorMessage.value = '새 비밀번호를 모두 입력해주세요.';
        return;
    }
    if (newPassword.value !== newPasswordConfirm.value) {
        errorMessage.value = '새 비밀번호가 일치하지 않습니다.';
        return;
    }
    if (!allPasswordCriteriaMet.value) {
        errorMessage.value = '비밀번호 정책을 확인해주세요.';
        return;
    }

    isLoading.value = true;
    try {
        const response = await resetPassword({
            email: email.value,
            code: verificationCode.value,
            newPassword: newPassword.value
        });
        if (response.ok) {
            openModal('비밀번호가 성공적으로 변경되었습니다.', 'success', () => {
                router.push('/login');
            });
        } else {
            errorMessage.value = response.data?.message || '비밀번호 변경에 실패했습니다.';
            openModal(errorMessage.value, 'error');
        }
    } catch (error) {
        console.error('비밀번호 재설정 오류:', error);
        errorMessage.value = '비밀번호 재설정 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
        openModal(errorMessage.value, 'error');
    } finally {
        isLoading.value = false;
    }
};

// Modal Logic (reused from RegisterView)
const showModal = ref(false);
const modalMessage = ref('');
const modalType = ref('info');
const modalCallback = ref(null);

const openModal = (message, type = 'info', callback = null) => {
    modalMessage.value = message;
    modalType.value = type;
    modalCallback.value = callback;
    showModal.value = true;
};

const closeModal = () => {
    showModal.value = false;
    if (modalCallback.value) {
        modalCallback.value();
        modalCallback.value = null;
    }
};

const goBack = () => {
    router.back();
};
</script>

<template>
    <div class="find-password-page">
        <div class="find-password-container">
            <div class="page-header">
                <button class="back-btn" @click="goBack">←</button>
                <h1 class="page-title">비밀번호 찾기</h1>
            </div>

            <!-- Phase 1: Request Code -->
            <div v-if="!isCodeSent" class="form-section">
                <p class="description">회원가입 시 입력했던<br/>이메일과 전화번호를 입력해주세요.</p>
                <div class="input-group">
                    <label for="email">이메일</label>
                    <input type="email" id="email" v-model="email" placeholder="example@email.com" />
                </div>
                <div class="input-group">
                    <label for="phone">전화번호</label>
                    <input type="tel" id="phone" v-model="phone" placeholder="010-1234-5678" />
                </div>
                <button class="submit-btn" @click="handleSendCode" :disabled="isLoading">
                    {{ isLoading ? '전송 중...' : '인증 코드 전송' }}
                </button>
            </div>

            <!-- Phase 2: Verify Code -->
            <div v-else-if="isCodeSent && !isVerified" class="form-section">
                <p class="description">이메일로 전송된 인증번호를 입력해주세요.</p>
                <div class="input-group">
                    <label for="verificationCode">인증번호</label>
                    <div class="input-row">
                        <input type="text" id="verificationCode" v-model="verificationCode" placeholder="인증번호 6자리" maxlength="6" />
                        <span v-if="timer > 0" class="timer">{{ formattedTimer }}</span>
                    </div>
                </div>
                <button class="submit-btn" @click="handleVerifyCode" :disabled="isLoading || !verificationCode">
                    {{ isLoading ? '확인 중...' : '인증 확인' }}
                </button>
                <button class="resend-btn" @click="handleSendCode" :disabled="isLoading">
                    인증 코드 재전송
                </button>
            </div>

            <!-- Phase 3: Reset Password -->
            <div v-else-if="isVerified" class="form-section">
                <p class="description">새로운 비밀번호를 설정해주세요.</p>
                <div class="input-group">
                    <label for="newPassword">새 비밀번호</label>
                    <div class="input-wrapper">
                        <input :type="showPassword ? 'text' : 'password'" id="newPassword" v-model="newPassword" placeholder="새 비밀번호를 입력하세요" />
                        <button type="button" class="toggle-btn" @click="showPassword = !showPassword">
                            <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>
                            <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-10-7-10-7a13.16 13.16 0 0 1 1.67-2.68L4 11"/><path d="M12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68M17.94 17.94 4 4"/></svg>
                        </button>
                    </div>
                    <div class="password-criteria">
                        <span v-for="criterion in passwordCriteria" :key="criterion.label" :class="{ 'is-valid': criterion.valid }">
                            <svg v-if="criterion.valid" class="check-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                            <span class="criteria-label">{{ criterion.label }}</span>
                        </span>
                    </div>
                </div>

                <div class="input-group">
                    <label for="newPasswordConfirm">새 비밀번호 확인</label>
                    <div class="input-wrapper">
                        <input :type="showPasswordConfirm ? 'text' : 'password'" id="newPasswordConfirm" v-model="newPasswordConfirm" placeholder="새 비밀번호를 다시 입력하세요" />
                        <button type="button" class="toggle-btn" @click="showPasswordConfirm = !showPasswordConfirm">
                            <svg v-if="showPasswordConfirm" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>
                            <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-10-7-10-7a13.16 13.16 0 0 1 1.67-2.68L4 11"/><path d="M12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68M17.94 17.94 4 4"/></svg>
                        </button>
                    </div>
                    <span v-if="passwordMatchState" class="email-check-message" :class="{ 'success-match': passwordMatchState.valid, 'error': !passwordMatchState.valid }">
                        {{ passwordMatchState.message }}
                    </span>
                </div>
                <button class="submit-btn" @click="handleResetPassword" :disabled="isLoading || !allPasswordCriteriaMet || (passwordMatchState && !passwordMatchState.valid)">
                    {{ isLoading ? '변경 중...' : '비밀번호 변경' }}
                </button>
            </div>

            <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        </div>

        <!-- Modal -->
        <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
            <div class="modal-content">
                <div class="modal-icon" :class="modalType">
                    <span v-if="modalType === 'success'">✓</span>
                    <span v-else-if="modalType === 'error'">!</span>
                    <span v-else>i</span>
                </div>
                <p class="modal-message">{{ modalMessage }}</p>
                <button class="modal-btn" @click="closeModal">확인</button>
            </div>
        </div>
    </div>
</template>

<style scoped>
.find-password-page {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: #f9fafb;
    padding: 1rem;
}

.find-password-container {
    background: white;
    max-width: 400px;
    width: 100%;
    border-radius: 16px;
    padding: 2rem;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    text-align: center;
}

.page-header {
    display: flex;
    align-items: center;
    margin-bottom: 2rem;
}

.back-btn {
    background: none;
    border: none;
    font-size: 1.5rem;
    cursor: pointer;
    padding: 0;
    margin-right: 1rem;
    color: #333;
}

.page-title {
    font-size: 1.8rem;
    font-weight: 700;
    color: #333;
    flex-grow: 1; /* Center title */
    text-align: center;
    margin: 0;
}

.description {
    font-size: 0.95rem;
    color: #666;
    margin-bottom: 1.5rem;
    line-height: 1.5;
}

.form-section {
    display: flex;
    flex-direction: column;
    gap: 1.5rem; /* Good spacing between input groups */
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

.input-row {
    display: flex;
    gap: 0.5rem; /* Spacing for input and button */
    align-items: center;
}

.input-group input[type="email"],
.input-group input[type="tel"],
.input-group input[type="text"] {
    width: 100%;
    padding: 0.8rem;
    border: 1px solid #ddd;
    border-radius: 8px;
    font-size: 1rem;
    outline: none;
}

.input-row input[type="text"] {
    flex: 1;
    width: auto;
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

.resend-btn {
    width: 100%;
    padding: 0.8rem;
    background: #f3f4f6;
    color: #333;
    border: 1px solid #d1d5db;
    border-radius: 8px;
    font-size: 0.95rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    margin-top: -0.5rem;
}

.resend-btn:hover {
    background: #e5e7eb;
}

.resend-btn:disabled {
    background: #f9fafb;
    color: #9ca3af;
    cursor: not-allowed;
}

.timer {
    font-size: 0.95rem;
    color: #ef4444; /* Red color for timer */
    white-space: nowrap;
    padding: 0 0.5rem;
}

.error-message {
    color: #dc2626;
    font-size: 0.9rem;
    margin-top: 1rem;
    text-align: center;
}

/* Password input specific styles */
.input-wrapper {
    display: flex;
    align-items: center;
    border: 1px solid #ddd;
    border-radius: 8px;
    overflow: hidden;
}

.input-wrapper input {
    border: none;
    border-radius: 0;
    padding: 0.8rem;
    flex: 1;
    width: 100%;
}

.toggle-btn {
    background: none;
    border: none;
    padding: 0 0.8rem; /* Adjust padding */
    cursor: pointer;
    color: #9ca3af;
    display: flex; /* For SVG centering */
    align-items: center;
}

.toggle-btn:hover {
    color: #374151;
}

.password-criteria {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
    font-size: 0.85rem;
    font-weight: 600;
    color: #a1a1aa;
    margin-top: 0.5rem;
}

.password-criteria > span {
    display: flex;
    align-items: center;
    gap: 0.3rem;
    white-space: nowrap;
}

.password-criteria .is-valid {
    color: var(--primary); /* Dark green for valid */
}

.check-icon {
    width: 14px; /* Smaller check icon */
    height: 14px;
    stroke-width: 2.5;
}

.email-check-message { /* Reusing class for password match state */
    font-size: 0.85rem;
    margin-top: 0.5rem;
    display: block;
}

.email-check-message.success-match {
    color: var(--primary);
}

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
    max-width: 320px;
    width: 90%;
    text-align: center;
}

.modal-icon {
    width: 50px;
    height: 50px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 1rem;
    font-size: 1.5rem;
    font-weight: bold;
}

.modal-icon.success {
    background: var(--primary);
    color: #004d40;
}

.modal-icon.error {
    background: #fee2e2;
    color: #dc2626;
}

.modal-icon.info {
    background: #e0f2fe;
    color: #0284c7;
}

.modal-message {
    font-size: 1rem;
    color: #333;
    margin-bottom: 1.5rem;
    line-height: 1.5;
}

.modal-btn {
    width: 100%;
    padding: 0.8rem;
    background: var(--primary);
    color: #004d40;
    border: none;
    border-radius: 8px;
    font-size: 0.95rem;
    font-weight: 600;
    cursor: pointer;
}
</style>
