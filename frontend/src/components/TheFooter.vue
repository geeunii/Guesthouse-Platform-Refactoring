<script setup>
</script>

<template>
  <footer class="app-footer">
    <div class="footer-container">
      <div class="footer-content">
        <div class="footer-section">
          <h3>ABOUT</h3>
          <ul>
            <li><a href="#">회사 소개</a></li>
            <li><a href="#">4 POINT APPROACH</a></li>
            <li><a href="#">NEWSLETTER</a></li>
            <li><a href="#">CAREERS</a></li>
          </ul>
        </div>

        <div class="footer-section">
          <h3>고객센터</h3>
          <p>평일 10:00 ~ 19:00 / 주말 및 공휴일 제외</p>
          <ul>
            <li><a href="#">입점 문의</a></li>
            <li><a href="#">제휴 문의</a></li>
            <li><a href="#">B2B 구매</a></li>
            <li><a href="#">1:1 문의</a></li>
          </ul>
        </div>

        <div class="footer-section">
          <h3>이용약관</h3>
          <ul>
            <li><router-link to="/policy?tab=privacy">개인정보 처리방침</router-link></li>
            <li><router-link to="/policy?tab=terms">이용약관</router-link></li>
            <li><router-link to="/policy?tab=refund">환불 정책</router-link></li>
          </ul>
        </div>

        <div class="footer-section">
          <h3>소셜미디어</h3>
          <div class="social-links">
            <a href="#" class="social-link">
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 2h-3a5 5 0 00-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 011-1h3z"/>
              </svg>
            </a>
            <a href="#" class="social-link">
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 8a6 6 0 016 6v7h-4v-7a2 2 0 00-2-2 2 2 0 00-2 2v7h-4v-7a6 6 0 016-6zM2 9h4v12H2z"/>
                <circle cx="4" cy="4" r="2" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"/>
              </svg>
            </a>
            <a href="#" class="social-link">
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M23 3a10.9 10.9 0 01-3.14 1.53 4.48 4.48 0 00-7.86 3v1A10.66 10.66 0 013 4s-4 9 5 13a11.64 11.64 0 01-7 2c9 5 20 0 20-11.5a4.5 4.5 0 00-.08-.83A7.72 7.72 0 0023 3z"/>
              </svg>
            </a>
            <a href="#" class="social-link">
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
            </a>
          </div>
        </div>
      </div>

      <div class="footer-divider"></div>

      <div class="footer-bottom">
        <p>(주)Travel은 통신판매 중개자로서 통신판매의 당사자가 아니며 상품의 예약, 이용 및 환불 등과 관련한 의무와 책임은 각 판매자에게 있습니다.</p>
        <p>Copyright©Travel</p>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  background: white;
  color: #1a1a1a;
  margin-top: auto;
}

.footer-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  font-family: 'TMoneyDungunbaram', sans-serif;
}

.footer-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 32px;
  margin-bottom: 24px;
}

.footer-section h3 {
  color: #1a1a1a;
  font-size: 14px;
  font-family: 'TMoneyDungunbaram', sans-serif;
  font-weight: 600;
  margin-bottom: 12px;
}

.footer-section p {
  font-size: 14px;
  color: #737373;
  line-height: 1.6;
  margin-bottom: 12px;
}

.footer-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-section li {
  margin-bottom: 10px;
}

.footer-section a {
  color: #737373;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s ease;
}

.footer-section a:hover {
  color: #6DC3BB;
}

.social-links {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.social-link {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: rgba(109, 195, 187, 0.1);
  color: #6DC3BB;
  display: flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  transition: all 0.2s;
}

.social-link:hover {
  background-color: #6DC3BB;
  color: white;
}

.social-link svg {
  width: 16px;
  height: 16px;
  stroke: currentColor;
}

.footer-divider {
  height: 1px;
  background: #e5e5e5;
  margin: 20px 0;
}

.footer-bottom {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  padding-top: 20px;
  color: #737373;
  line-height: 1.6;
}

.footer-bottom p {
  margin: 0;
}

/* Desktop styles */
@media (min-width: 769px) {
  .footer-content {
    display: grid !important;
    grid-template-columns: repeat(4, 1fr);
    gap: 48px;
  }

  .footer-container {
    padding: 30px 40px 40px;
  }

  .footer-section h3 {
    font-size: 14px;
    margin-bottom: 16px;
  }

  .footer-section a {
    font-size: 14px;
  }

  .footer-section li {
    margin-bottom: 12px;
  }

  .footer-bottom {
    align-items: flex-start;
    font-size: 12px;
  }
}

/* Mobile styles */
@media (max-width: 768px) {
  .footer-content {
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
    font-size: 12px;
  }

  .footer-section h3 {
    font-size: 12px;
    margin-bottom: 8px;
  }

  .footer-section a,
  .footer-section p {
    font-size: 11px;
    line-height: 1.5;
  }

  .footer-section li {
    margin-bottom: 6px;
  }

  .footer-section p {
    margin-bottom: 8px;
  }

  .social-links {
    gap: 8px;
    margin-top: 8px;
  }

  .social-link {
    width: 28px;
    height: 28px;
  }

  .social-link svg {
    width: 14px;
    height: 14px;
  }

  .footer-container {
    padding: 16px 12px;
  }

  .footer-divider {
    margin: 16px 0;
  }

  .footer-bottom {
    flex-direction: column;
    text-align: left;
    gap: 6px;
    font-size: 10px;
    padding-top: 12px;
  }
}
</style>
