<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { searchList } from '@/api/list'

const router = useRouter()
const items = ref([])
const isLoading = ref(true)

const fetchItems = async () => {
  try {
    // Fetch a list of accommodations. 
    // Using a small size to keep it lightweight.
    const response = await searchList({ page: 0, size: 15 })
    
    if (response.ok && response.data) {
      const rawItems = response.data.content || response.data.items || []
      items.value = rawItems.map(item => ({
        id: item.accommodationsId ?? item.accommodationId ?? item.id,
        name: item.accommodationsName ?? item.accommodationName ?? item.title ?? '',
        // Use thumbnailUrl if available, fallback to placeholder
        image: item.thumbnailUrl || item.imageUrl || 'https://placehold.co/100x100'
      })).filter(item => item.id && item.name)
    }
  } catch (e) {
    console.error('Failed to load marquee items', e)
  } finally {
    isLoading.value = false
  }
}

const handleItemClick = (id) => {
  router.push(`/room/${id}`)
}

onMounted(() => {
  // Execute after a short delay to prioritize main content loading
  setTimeout(() => {
    fetchItems()
  }, 500)
})
</script>

<template>
  <div v-if="items.length > 0" class="footer-marquee">
    <div class="marquee-track">
      <div class="marquee-content">
        <div 
          v-for="item in items" 
          :key="item.id" 
          class="marquee-card"
          @click="handleItemClick(item.id)"
        >
          <div class="image-wrapper">
            <img :src="item.image" :alt="item.name" loading="lazy" />
          </div>
          <span class="card-name">{{ item.name }}</span>
        </div>
      </div>
      <!-- Duplicated content for seamless infinite scroll -->
      <div class="marquee-content">
        <div 
          v-for="item in items" 
          :key="`dup-${item.id}`" 
          class="marquee-card"
          @click="handleItemClick(item.id)"
        >
          <div class="image-wrapper">
            <img :src="item.image" :alt="item.name" loading="lazy" />
          </div>
          <span class="card-name">{{ item.name }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.footer-marquee {
  width: 100%;
  background-color: #ffffff;
  border-top: 1px solid #f1f5f9;
  padding: 16px 0 0 0; /* Remove bottom padding */
  position: relative;
  overflow: hidden;
}

.marquee-track {
  display: flex;
  width: fit-content;
  animation: scroll 60s linear infinite;
}

.marquee-content {
  display: flex;
  align-items: center;
  gap: 24px; /* Slightly reduced gap */
  padding-right: 24px;
}

.marquee-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px; /* Reduced gap */
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease;
  width: 140px; /* Keep consistent width */
  flex-shrink: 0;
}

.marquee-card:hover {
  transform: translateY(-4px);
  opacity: 0.8;
}

.image-wrapper {
  width: 120px; /* Rectangular width */
  height: 80px; /* Rectangular height */
  border-radius: 8px; /* Rounded corners, not circle */
  overflow: hidden;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
  background-color: #f8fafc;
}

.image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-name {
  font-family: 'NanumSquareRound', sans-serif;
  font-size: 0.85rem;
  font-weight: 700;
  color: #475569;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

@keyframes scroll {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-50%);
  }
}

.footer-marquee:hover .marquee-track {
  animation-play-state: paused;
}

@media (max-width: 768px) {
  .footer-marquee {
    padding: 12px 0;
  }
  
  .marquee-content {
    gap: 16px;
    padding-right: 16px;
  }

  .marquee-card {
    width: 130px; /* Increased from 110px */
  }

  .image-wrapper {
    width: 110px; /* Increased from 90px */
    height: 74px; /* Increased from 60px */
  }

  .card-name {
    font-size: 0.85rem; /* Increased from 0.75rem */
  }
}
</style>
