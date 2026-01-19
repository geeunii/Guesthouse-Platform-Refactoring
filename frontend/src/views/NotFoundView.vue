<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
    router.push('/')
}
</script>

<template>
    <div class="not-found-container">
        <div class="content-wrapper">
            <div class="image-container">
                <img src="@/assets/images/404_house.png" alt="Page Not Found" class="not-found-image" />
            </div>
            <h1 class="title">페이지를 찾을 수 없습니다</h1>
            <p class="description">
                요청하신 페이지를 찾을 수 없어요.<br />
                입력하신 주소가 정확한지 다시 한번 확인해 주세요.
            </p>
            <button class="home-btn" @click="goHome">
                홈으로 돌아가기
            </button>
        </div>
    </div>
</template>

<style scoped>
.not-found-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 80vh;
    background-color: #fff;
    font-family: 'NanumSquareRound', -apple-system, BlinkMacSystemFont, system-ui, Roboto, sans-serif;
}

.content-wrapper {
    text-align: center;
    padding: 2rem;
    max-width: 400px;
}

.image-container {
    margin-bottom: 2rem;
}

.not-found-image {
    width: 180px;
    height: auto;
    object-fit: contain;
    /* Soft shadow for depth */
    /* filter: drop-shadow(0 10px 15px rgba(0, 0, 0, 0.05)); */
    animation: float 3s ease-in-out infinite;
}

.title {
    font-size: 1.5rem;
    font-weight: 700;
    color: #1a1a1a;
    margin-bottom: 1rem;
    letter-spacing: -0.02em;
}

.description {
    font-size: 1rem;
    color: #888;
    line-height: 1.5;
    margin-bottom: 2.5rem;
    word-break: keep-all;
}

.home-btn {
    background-color: #BFE7DF;
    color: #004d40;
    font-size: 1rem;
    font-weight: 700;
    padding: 14px 32px;
    border-radius: 50px;
    border: none;
    cursor: pointer;
    transition: all 0.2s ease;
    box-shadow: 0 4px 12px rgba(191, 231, 223, 0.5);
}

.home-btn:hover {
    background-color: #A0D1C8;
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(160, 209, 200, 0.6);
}

.home-btn:active {
    transform: translateY(0);
}

@keyframes float {
    0% {
        transform: translateY(0px);
    }
    50% {
        transform: translateY(-10px);
    }
    100% {
        transform: translateY(0px);
    }
}

@media (max-width: 480px) {
    .not-found-image {
        width: 150px;
    }
    .title {
        font-size: 1.35rem;
    }
    .description {
        font-size: 0.95rem;
    }
}
</style>
