<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchMyWishlist, removeWishlist } from '@/api/wishlist'
import wishlistActiveIcon from '@/assets/wishlist1.png'

const router = useRouter()
const wishlist = ref([])
const isLoading = ref(true)

const loadWishlist = async () => {
  isLoading.value = true
  try {
    const res = await fetchMyWishlist()
    if (res.ok) {
      console.log('Wishlist API Response:', res.data) // Debugging log
      wishlist.value = res.data.map(item => ({
        id: item.accommodationsId,
        title: item.accommodationsName,
        location: [item.city, item.district, item.township].filter(Boolean).join(' '),
        rating: item.rating || 0.0,
        image: getThumbnailUrl(item.mainImageUrl) || 'https://placehold.co/300x200',
        price: item.minPrice,
        themes: item.themes || []
      }))
    }
  } catch (e) {
    console.error('Failed to load wishlist', e)
  } finally {
    isLoading.value = false
  }
}

// 이미지 URL (원본 사용)
const getThumbnailUrl = (url) => {
  if (!url) return ''
  return url
}

const handleRemove = async (e, id) => {
  e.stopPropagation() // 카드 클릭 이벤트 방지
  if (!confirm('위시리스트에서 삭제하시겠습니까?')) return

  try {
    const res = await removeWishlist(id)
    if (res.ok) {
      wishlist.value = wishlist.value.filter(item => item.id !== id)
    }
  } catch (err) {
    console.error(err)
    alert('삭제에 실패했습니다.')
  }
}

const goToDetail = (id) => {
  router.push(`/room/${id}`)
}

onMounted(() => {
  loadWishlist()
})
</script>

<template>
  <div class="wishlist-page container">
    <h1 class="page-title">위시리스트</h1>

    <div v-if="wishlist.length === 0" class="empty-state">
      위시리스트가 비어있습니다.
    </div>

    <div v-else class="wish-grid">
      <div 
        v-for="item in wishlist" 
        :key="item.id" 
        class="wish-card"
        @click="goToDetail(item.id)"
      >
        <div class="card-image-wrapper">
          <img :src="item.image" alt="thumbnail" class="card-img" />
          <button class="heart-btn" @click="(e) => handleRemove(e, item.id)">
            <img :src="wishlistActiveIcon" alt="찜한 숙소" class="heart-icon" />
          </button>
        </div>
        
        <div class="card-info">
          <div class="row-top">
            <h3 class="title">{{ item.title }}</h3>
            <span class="rating">★ {{ item.rating ? item.rating.toFixed(1) : '0.0' }}</span>
          </div>
          <p class="location">{{ item.location }}</p>
          <div class="meta-row">
             <span class="price" v-if="item.price">{{ Number(item.price).toLocaleString() }}원</span>
             <span class="price" v-else>가격 정보 없음</span>
          </div>
          <div class="themes" v-if="item.themes && item.themes.length">
             <span v-for="(theme, idx) in item.themes" :key="idx" class="theme-tag">#{{ theme }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wishlist-page {
  padding-top: 2rem;
  padding-bottom: 4rem;
  max-width: 600px;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 800;
  margin-bottom: 2rem;
}

.empty-state {
  text-align: center;
  padding: 4rem 0;
  color: #888;
  font-size: 1.1rem;
}

.wish-grid {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.wish-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border: 1px solid #eee;
  cursor: pointer;
  transition: transform 0.2s;
}

.wish-card:hover {
  transform: translateY(-2px);
}

.card-image-wrapper {
  position: relative;
  height: 200px;
}

.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  /* 이미지 축소 시 품질 개선 */
  image-rendering: -webkit-optimize-contrast;
  image-rendering: smooth;
  transform: translateZ(0);
}

.heart-btn {
  position: absolute;
  top: 12px;
  left: 12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  cursor: pointer;
}

.heart-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.card-info {
  padding: 1.2rem;
}

.row-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.title {
  font-size: 1.1rem;
  font-weight: 800;
  color: #333;
}

.rating {
  font-size: 0.95rem;
  font-weight: bold;
  color: #333;
}

.location {
  font-size: 0.9rem;
  color: #666;
  margin-bottom: 0.5rem;
}

.meta-row {
  margin-bottom: 0.5rem;
}

.price {
  font-weight: 700;
  font-size: 1.05rem;
  color: #111;
}

.themes {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.theme-tag {
  font-size: 0.8rem;
  color: #555;
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
