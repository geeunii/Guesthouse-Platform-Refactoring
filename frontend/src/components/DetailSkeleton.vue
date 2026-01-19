<script setup>
// 상세 페이지 스켈레톤 컴포넌트 - 로딩 중 표시
</script>

<template>
  <div class="detail-skeleton">
    <!-- 이미지 갤러리 스켈레톤 -->
    <div class="skeleton-gallery shimmer"></div>

    <!-- 정보 섹션 스켈레톤 -->
    <section class="skeleton-info-section">
      <div class="skeleton-title shimmer"></div>
      <div class="skeleton-meta">
        <div class="skeleton-rating shimmer"></div>
        <div class="skeleton-location shimmer"></div>
      </div>
      <div class="skeleton-description">
        <div class="skeleton-text shimmer"></div>
        <div class="skeleton-text shimmer" style="width: 80%"></div>
        <div class="skeleton-text shimmer" style="width: 60%"></div>
      </div>
    </section>

    <!-- 편의시설 스켈레톤 -->
    <section class="skeleton-amenity-section">
      <div class="skeleton-section-title shimmer"></div>
      <div class="skeleton-tags">
        <div v-for="n in 5" :key="n" class="skeleton-tag shimmer"></div>
      </div>
    </section>

    <hr class="skeleton-hr" />

    <!-- 객실 선택 스켈레톤 -->
    <section class="skeleton-room-section">
      <div class="skeleton-section-title shimmer"></div>
      <div class="skeleton-picker shimmer"></div>
      <div class="skeleton-room-list">
        <div v-for="n in 2" :key="n" class="skeleton-room-card">
          <div class="skeleton-room-image shimmer"></div>
          <div class="skeleton-room-content">
            <div class="skeleton-room-name shimmer"></div>
            <div class="skeleton-room-desc shimmer"></div>
            <div class="skeleton-room-price shimmer"></div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.detail-skeleton {
  padding: 0 1rem;
}

.skeleton-gallery {
  width: 100%;
  height: 300px;
  background: #e5e7eb;
  border-radius: 16px;
  margin-bottom: 1.5rem;
}

.skeleton-info-section {
  margin-bottom: 2rem;
}

.skeleton-title {
  height: 2rem;
  width: 60%;
  background: #e5e7eb;
  border-radius: 4px;
  margin-bottom: 1rem;
}

.skeleton-meta {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.skeleton-rating {
  height: 1rem;
  width: 100px;
  background: #e5e7eb;
  border-radius: 4px;
}

.skeleton-location {
  height: 1rem;
  width: 150px;
  background: #e5e7eb;
  border-radius: 4px;
}

.skeleton-description {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.skeleton-text {
  height: 0.875rem;
  width: 100%;
  background: #e5e7eb;
  border-radius: 4px;
}

.skeleton-amenity-section {
  margin-bottom: 1.5rem;
}

.skeleton-section-title {
  height: 1.5rem;
  width: 100px;
  background: #e5e7eb;
  border-radius: 4px;
  margin-bottom: 1rem;
}

.skeleton-tags {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.skeleton-tag {
  height: 2rem;
  width: 80px;
  background: #e5e7eb;
  border-radius: 999px;
}

.skeleton-hr {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 1.5rem 0;
}

.skeleton-room-section {
  margin-bottom: 2rem;
}

.skeleton-picker {
  height: 80px;
  background: #e5e7eb;
  border-radius: 12px;
  margin-bottom: 1.5rem;
}

.skeleton-room-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.skeleton-room-card {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  background: #f8fafc;
  border-radius: 12px;
}

.skeleton-room-image {
  width: 120px;
  height: 100px;
  background: #e5e7eb;
  border-radius: 8px;
  flex-shrink: 0;
}

.skeleton-room-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.skeleton-room-name {
  height: 1.25rem;
  width: 50%;
  background: #e5e7eb;
  border-radius: 4px;
}

.skeleton-room-desc {
  height: 0.875rem;
  width: 80%;
  background: #e5e7eb;
  border-radius: 4px;
}

.skeleton-room-price {
  height: 1.125rem;
  width: 30%;
  background: #e5e7eb;
  border-radius: 4px;
  margin-top: auto;
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
