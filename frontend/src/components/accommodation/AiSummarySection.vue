<script setup>
import { ref, onUnmounted, computed } from 'vue'
import { fetchAiSummary } from '@/api/accommodation'

defineOptions({
  name: 'AiSummarySection'
})

const props = defineProps({
  accommodationId: {
    type: [Number, String],
    required: true
  }
})

const fullSummaryHtml = ref('')
const displayedSummary = ref('')
const isLoading = ref(false)
const isError = ref(false)
const isLoaded = ref(false)
const isExpanded = ref(false)
let typeWriterInterval = null

const contentStyle = computed(() => ({
  maxHeight: isExpanded.value ? '1000px' : '180px',
  overflow: 'hidden',
  transition: 'max-height 0.5s ease-in-out',
  position: 'relative'
}))

const typeWriterEffect = (htmlContent) => {
  let i = 0;
  displayedSummary.value = '';

  if (typeWriterInterval) clearInterval(typeWriterInterval);

  typeWriterInterval = setInterval(() => {
    if (i >= htmlContent.length) {
      clearInterval(typeWriterInterval);
      return;
    }

    if (htmlContent[i] === '<') {
      const tagEndIndex = htmlContent.indexOf('>', i);
      if (tagEndIndex !== -1) {
        displayedSummary.value += htmlContent.substring(i, tagEndIndex + 1);
        i = tagEndIndex + 1;
        return;
      }
    }

    displayedSummary.value += htmlContent[i];
    i++;
  }, 20);
}

const buildSummaryHtml = (data) => {
  const { accommodationName, locationTag, keywords, moodDescription, tip, reviewCount } = data;

  const keywordsHtml = keywords.join(' ');
  footerData.value = { reviewCount };

  return `<strong>${accommodationName}</strong>은(는) <strong>${locationTag}</strong>에 위치한 매력적인 숙소입니다.<br><br>` +
         `🔑 <strong>핵심 키워드</strong>: ${keywordsHtml} #제주감성<br><br>` +
         `🏡 <strong>분위기 & 특징</strong><br>${moodDescription}<br><br>` +
         `💡 <strong>AI의 이용 꿀팁</strong><br>${tip}`;
}

const footerData = ref(null);

const loadSummary = async () => {
  if (isLoading.value || isLoaded.value) return

  isLoading.value = true
  isError.value = false

  try {
    const response = await fetchAiSummary(props.accommodationId)
    if (response.ok && response.data) {
      fullSummaryHtml.value = buildSummaryHtml(response.data)
      isLoaded.value = true
      typeWriterEffect(fullSummaryHtml.value)
    } else {
      throw new Error('Failed to load summary')
    }
  } catch (error) {
    console.error('AI Summary Error:', error)
    isError.value = true
  } finally {
    isLoading.value = false
  }
}

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value;
  if (isExpanded.value && displayedSummary.value.length < fullSummaryHtml.value.length) {
    if (typeWriterInterval) clearInterval(typeWriterInterval);
    displayedSummary.value = fullSummaryHtml.value;
  }
}

onUnmounted(() => {
  if (typeWriterInterval) clearInterval(typeWriterInterval);
})
</script>

<template>
  <div class="ai-summary-section">
    <button
      v-if="!isLoaded && !isLoading && !isError"
      class="ai-btn"
      @click="loadSummary"
      aria-label="AI 숙소 요약 보기"
    >
      ✨ AI 숙소 요약 보기
    </button>

    <div v-if="isLoading" class="skeleton-loader" role="status" aria-label="요약 정보를 불러오는 중입니다">
      <div class="skeleton-line title"></div>
      <div class="skeleton-line"></div>
      <div class="skeleton-line"></div>
      <div class="skeleton-line short"></div>
    </div>

    <div v-if="isError" class="error-box" role="alert">
      <p>요약 정보를 불러오는데 실패했습니다.</p>
      <button class="retry-btn" @click="loadSummary">재시도</button>
    </div>

    <div v-if="isLoaded" class="summary-box">
      <div class="summary-header">
        <span class="ai-icon" aria-hidden="true">✨</span>
        <span class="ai-title">AI 숙소 요약</span>
      </div>

      <div
        id="summary-content"
        class="summary-content"
        :style="contentStyle"
        :aria-expanded="isExpanded"
      >
        <p class="summary-text">
          <span v-html="displayedSummary"></span>
          <span class="cursor" v-if="displayedSummary.length < fullSummaryHtml.length" aria-hidden="true">|</span>
        </p>
        <div v-if="!isExpanded" class="fade-out" aria-hidden="true"></div>
      </div>

      <button
        class="expand-btn"
        @click="toggleExpand"
        :aria-expanded="isExpanded"
        aria-controls="summary-content"
        :aria-label="isExpanded ? '요약 내용 접기' : '요약 내용 더보기'"
      >
        {{ isExpanded ? '접기' : '⌄ 더보기' }}
      </button>

      <div class="summary-footer" v-if="footerData">
        <span v-if="footerData.reviewCount > 0">
          🔍 최근 <strong>{{ footerData.reviewCount }}건</strong>의 실제 방문자 리뷰와 <br class="mobile-break">데이터를 기반으로 분석했습니다.
        </span>
        <span v-else>
          🔍 숙소 상세 정보를 기반으로 분석했습니다.
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-summary-section {
  width: 100%;
  margin: 1.5rem 0;
}

.ai-btn {
  width: 100%;
  padding: 1rem;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  border: none;
  border-radius: 16px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.ai-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.4);
}

.summary-box {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.01);
  animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  margin-top: 1rem;
  overflow: hidden;
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 1.5rem 1.5rem 1rem 1.5rem;
  font-weight: 800;
  font-size: 1.15rem;
  color: #1f2937;
  border-bottom: 2px solid #f3f4f6;
}

.summary-content {
  padding: 1.2rem 1.5rem 0 1.5rem;
  position: relative;
}

.summary-text {
  line-height: 1.8;
  color: #4b5563;
  font-size: 15px;
  margin: 0;
  letter-spacing: -0.01em;
}

.fade-out {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 60px;
  background: linear-gradient(to bottom, rgba(255,255,255,0), rgba(255,255,255,1));
  pointer-events: none;
}

.expand-btn {
  width: 100%;
  padding: 0.8rem;
  background: transparent;
  border: none;
  color: #6366f1;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  transition: color 0.2s;
}
.expand-btn:hover {
  color: #4f46e5;
  background-color: #f9fafb;
}

.summary-footer {
  background-color: #f9fafb;
  border-top: 1px solid #e5e7eb;
  padding: 1rem;
  text-align: center;
  font-size: 13px;
  color: #9ca3af;
  line-height: 1.5;
}

.mobile-break {
  display: none;
}

.cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background-color: #6366f1;
  margin-left: 2px;
  vertical-align: middle;
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.skeleton-loader {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 1.5rem;
  margin-top: 1rem;
}

.skeleton-line {
  height: 15px;
  background-color: #e5e7eb;
  border-radius: 6px;
  margin-bottom: 10px;
  animation: pulse 1.5s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
.skeleton-line.title { width: 40%; height: 20px; margin-bottom: 1rem; }
.skeleton-line.short { width: 70%; }
.skeleton-line:last-child { margin-bottom: 0; }

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .5; }
}

.error-box {
  margin-top: 1rem;
  padding: 1.5rem;
  background: #fef2f2;
  border: 1px solid #fee2e2;
  border-radius: 12px;
  text-align: center;
  color: #991b1b;
}

.retry-btn {
  margin-top: 0.5rem;
  padding: 0.5rem 1rem;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}
.retry-btn:hover { background: #dc2626; }

/* Mobile Styles */
@media (max-width: 768px) {
  .summary-content {
    padding: 1rem 1rem 0 1rem;
  }
  .summary-header {
    padding: 1.2rem 1rem 0.8rem 1rem;
  }
  .summary-text {
    font-size: 14px;
  }
  .mobile-break {
    display: block;
  }
}
</style>
