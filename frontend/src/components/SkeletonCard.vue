<script setup>
// 스켈레톤 카드 컴포넌트 - 로딩 중 표시
</script>

<template>
  <div class="skeleton-card">
    <div class="skeleton-image shimmer"></div>
    <div class="skeleton-content">
      <div class="skeleton-title shimmer"></div>
      <div class="skeleton-location shimmer"></div>
      <div class="skeleton-rating shimmer"></div>
      <div class="skeleton-price shimmer"></div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-card {
  flex: 0 0 var(--card-width, 280px);
  background: transparent;
  border-radius: var(--radius-md);
  overflow: hidden;
}

/* 모바일에서 카드 크기 축소 */
@media (max-width: 768px) {
  .skeleton-card {
    flex: 0 0 230px;
  }
}

.skeleton-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #e5e7eb;
  border-radius: var(--radius-md);
}

.skeleton-content {
  padding: 0.15rem 0.5rem 0;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.skeleton-title {
  height: 0.95rem;
  width: 80%;
  background: #e5e7eb;
  border-radius: 4px;
}

.skeleton-location {
  height: 0.86rem;
  width: 50%;
  background: #e5e7eb;
  border-radius: 4px;
}

.skeleton-rating {
  height: 0.85rem;
  width: 30%;
  background: #e5e7eb;
  border-radius: 4px;
}

.skeleton-price {
  height: 0.9rem;
  width: 40%;
  background: #e5e7eb;
  border-radius: 4px;
}

/* 쉬머 애니메이션 효과 */
.shimmer {
  position: relative;
  overflow: hidden;
}

.shimmer::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.5) 50%,
    transparent 100%
  );
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}
</style>
