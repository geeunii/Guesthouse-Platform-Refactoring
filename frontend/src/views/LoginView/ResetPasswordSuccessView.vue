<template>
    <div class="reset-success-page">
        <div class="reset-success-container">
            <div class="icon-wrapper success">
                <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-8.1"></path>
                    <path d="M22 4L12 14.01l-3-3"></path>
                </svg>
            </div>
            <h1 class="page-title">비밀번호 변경 완료</h1>
            <p class="description">성공적으로 비밀번호가 변경되었습니다.</p>
            <p class="description">새로운 비밀번호로 로그인해주세요.</p>
            <button class="go-to-login-btn" @click="goToLogin">로그인 페이지로 이동</button>
        </div>
    </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const goToLogin = () => {
    router.push('/login');
};
</script>

<style scoped>
.reset-success-page {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: #f9fafb;
    padding: 1rem;
}

.reset-success-container {
    background: white;
    max-width: 400px;
    width: 100%;
    border-radius: 16px;
    padding: 2.5rem;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    text-align: center;
}

.icon-wrapper {
    margin: 0 auto 1.5rem;
    width: 64px;
    height: 64px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}

.icon-wrapper.success {
    background-color: var(--primary); /* Light green/teal */
    color: white;
}

.page-title {
    font-size: 1.8rem;
    font-weight: 700;
    color: #333;
    margin-bottom: 1rem;
}

.description {
    font-size: 0.95rem;
    color: #666;
    margin-bottom: 0.5rem;
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
    margin-top: 2rem;
}

.go-to-login-btn:hover {
    background: #555;
}
</style>
