export const dashboardStats = [
  { label: '전체 플랫폼 이용자', value: '12,480명', sub: '이달 +4.2%', tone: 'primary', target: '/admin/users' },
  { label: '플랫폼 수익', value: '₩184,200,000', sub: '전월 대비 +6.8%', tone: 'success', target: '/admin/payments' },
  { label: '이번 달 거래 건수', value: '3,280건', sub: '전월 대비 +3.1%', tone: 'neutral', target: '/admin/bookings' },
  { label: '등록 숙소 수', value: '1,248 (활성 1,180)', sub: '승인 대기 18건', tone: 'accent', target: '/admin/accommodations' }
]

export const dashboardPendingListings = [
  {
    id: 'AC-1024',
    name: '해변 감성 스테이',
    host: '이호스트',
    location: '부산 해운대',
    type: '게스트하우스',
    rooms: 3,
    nightlyPrice: '₩180,000',
    contact: '010-2345-6789',
    submittedAt: '2025-01-06',
    status: '대기'
  },
  {
    id: 'AC-1019',
    name: '도심 스튜디오',
    host: '박호스트',
    location: '서울 강남',
    type: '오피스텔',
    rooms: 1,
    nightlyPrice: '₩95,000',
    contact: '010-3344-5533',
    submittedAt: '2025-01-05',
    status: '검토'
  },
  {
    id: 'AC-1012',
    name: '한옥 리트릿',
    host: '정호스트',
    location: '전주 한옥마을',
    type: '한옥',
    rooms: 4,
    nightlyPrice: '₩210,000',
    contact: '010-7788-1290',
    submittedAt: '2025-01-03',
    status: '대기'
  }
]

export const dashboardRevenueTrend = {
  months: ['8월', '9월', '10월', '11월', '12월', '1월'],
  values: [22, 25, 28, 30, 34, 37]
}

export const dashboardTransactions = {
  yearly: [320, 280, 360, 410, 380, 420, 460, 440, 400, 430, 470, 520],
  monthly: [12, 18, 16, 20, 22, 26, 28, 24, 30, 32, 29, 34]
}

export const dashboardAlerts = [
  {
    id: 'AL-901',
    title: '결제 실패율이 2.3%로 상승',
    meta: 'PG 점검 필요',
    time: '오늘 10:20',
    tone: 'warning'
  },
  {
    id: 'AL-894',
    title: '숙소 승인 대기 18건 누적',
    meta: '평균 처리 2.4일',
    time: '오늘 09:05',
    tone: 'accent'
  },
  {
    id: 'AL-887',
    title: '신고 처리 SLA 92% 달성',
    meta: '지난 7일 기준',
    time: '어제 17:40',
    tone: 'success'
  }
]

export const usersStats = {
  total: '24,200명',
  guests: '16,840명',
  hosts: '7,360명'
}

export const adminUsers = [
  {
    id: 'U-1021',
    name: '이서준',
    email: 'seojun@example.com',
    type: '게스트',
    joinedAt: '2024-11-12',
    lastLogin: '2025-01-14',
    activity: '최근 30일 내 3회 예약',
    status: '활성',
    phone: '010-1122-3344',
    reports: 0,
    risk: '낮음'
  },
  {
    id: 'U-1008',
    name: '김하늘',
    email: 'haneul@example.com',
    type: '호스트',
    joinedAt: '2024-08-21',
    lastLogin: '2025-01-12',
    activity: '신규 숙소 등록 2건',
    status: '활성',
    phone: '010-2222-4578',
    reports: 1,
    risk: '중간'
  },
  {
    id: 'U-0977',
    name: '박지민',
    email: 'jimin@example.com',
    type: '게스트',
    joinedAt: '2023-12-04',
    lastLogin: '2024-10-02',
    activity: '최근 90일 미접속',
    status: '휴면',
    phone: '010-3333-1458',
    reports: 0,
    risk: '낮음'
  },
  {
    id: 'U-0814',
    name: '최민서',
    email: 'minseo@example.com',
    type: '호스트',
    joinedAt: '2022-05-19',
    lastLogin: '2025-01-11',
    activity: '월 매출 4,200,000원',
    status: '활성',
    phone: '010-4444-9821',
    reports: 2,
    risk: '중간'
  },
  {
    id: 'U-0765',
    name: '서다희',
    email: 'dahee@example.com',
    type: '게스트',
    joinedAt: '2021-06-28',
    lastLogin: '2024-08-01',
    activity: '취소율 40% (최근 6개월)',
    status: '정지',
    phone: '010-5555-0102',
    reports: 4,
    risk: '높음'
  },
  {
    id: 'U-0692',
    name: '정유진',
    email: 'yujin@example.com',
    type: '호스트',
    joinedAt: '2023-02-11',
    lastLogin: '2025-01-13',
    activity: '정산 대기 2건',
    status: '활성',
    phone: '010-8899-1200',
    reports: 0,
    risk: '낮음'
  }
]

export const accommodations = [
  {
    id: 'H-2201',
    name: '바다 전망 스위트',
    host: '이호스트',
    location: '부산 해운대',
    rating: 4.8,
    reviewCount: 124,
    bookings: 128,
    occupancy: '82%',
    revenue: '₩48,200,000',
    status: '승인됨',
    cancellationRate: '3.2%',
    registeredAt: '2024-10-02'
  },
  {
    id: 'H-2104',
    name: '한강 뷰 아파트',
    host: '김호스트',
    location: '서울 용산',
    rating: 4.6,
    reviewCount: 86,
    bookings: 94,
    occupancy: '76%',
    revenue: '₩35,100,000',
    status: '검토중',
    cancellationRate: '5.1%',
    registeredAt: '2024-12-14'
  },
  {
    id: 'H-1988',
    name: '숲속 캠핑돔',
    host: '박호스트',
    location: '가평',
    rating: 4.7,
    reviewCount: 62,
    bookings: 76,
    occupancy: '69%',
    revenue: '₩21,900,000',
    status: '승인됨',
    cancellationRate: '2.4%',
    registeredAt: '2024-08-30'
  },
  {
    id: 'H-1762',
    name: '도심 로프트',
    host: '서호스트',
    location: '대전 둔산',
    rating: 4.3,
    reviewCount: 38,
    bookings: 52,
    occupancy: '61%',
    revenue: '₩14,800,000',
    status: '보류',
    cancellationRate: '8.7%',
    registeredAt: '2025-01-02'
  }
]

export const bookings = [
  {
    id: 'BK-202501-341',
    accommodation: '바다 전망 스위트',
    host: '이호스트',
    guest: '최가람',
    checkIn: '2025-01-21',
    checkOut: '2025-01-24',
    people: 2,
    price: '₩520,000',
    status: '확정',
    paymentStatus: '완료',
    channel: '웹',
    createdAt: '2025-01-12'
  },
  {
    id: 'BK-202501-297',
    accommodation: '한강 뷰 아파트',
    host: '김호스트',
    guest: '박수진',
    checkIn: '2025-01-18',
    checkOut: '2025-01-20',
    people: 3,
    price: '₩410,000',
    status: '대기',
    paymentStatus: '보류',
    channel: '모바일',
    createdAt: '2025-01-10'
  },
  {
    id: 'BK-202501-255',
    accommodation: '숲속 캠핑돔',
    host: '박호스트',
    guest: '김민재',
    checkIn: '2025-01-25',
    checkOut: '2025-01-27',
    people: 4,
    price: '₩360,000',
    status: '체크인',
    paymentStatus: '완료',
    channel: '모바일',
    createdAt: '2025-01-08'
  },
  {
    id: 'BK-202501-214',
    accommodation: '도심 로프트',
    host: '서호스트',
    guest: '오지은',
    checkIn: '2025-01-11',
    checkOut: '2025-01-12',
    people: 1,
    price: '₩120,000',
    status: '취소',
    paymentStatus: '환불',
    channel: '웹',
    createdAt: '2025-01-05'
  }
]

export const paymentsStats = {
  totalVolume: '₩184,200,000',
  platformFee: '₩12,894,000',
  transactions: '3,280건'
}

export const payments = [
  {
    id: 'PM-98211',
    host: '김호스트',
    guest: '최가람',
    accommodation: '바다 전망 스위트',
    amount: '₩520,000',
    fee: '₩36,400',
    type: '예약',
    status: '완료',
    method: '카드',
    settlementDate: '2025-01-17',
    date: '2025-01-12'
  },
  {
    id: 'PM-98104',
    host: '이호스트',
    guest: '박수진',
    accommodation: '한강 뷰 아파트',
    amount: '₩410,000',
    fee: '₩28,700',
    type: '예약',
    status: '환불',
    method: '계좌이체',
    settlementDate: '2025-01-14',
    date: '2025-01-10'
  },
  {
    id: 'PM-97945',
    host: '박호스트',
    guest: '김민재',
    accommodation: '숲속 캠핑돔',
    amount: '₩360,000',
    fee: '₩25,200',
    type: '예약',
    status: '완료',
    method: '카드',
    settlementDate: '2025-01-12',
    date: '2025-01-08'
  },
  {
    id: 'PM-97702',
    host: '서호스트',
    guest: '오지은',
    accommodation: '도심 로프트',
    amount: '₩120,000',
    fee: '₩8,400',
    type: '환불',
    status: '보류',
    method: '카드',
    settlementDate: '2025-01-15',
    date: '2025-01-05'
  }
]

export const reportsStats = {
  pending: 6,
  inProgress: 3,
  resolved: 18
}

export const reports = [
  {
    id: 'RP-2025-104',
    reporter: '김유나',
    target: '박호스트',
    reason: '청결 문제',
    summary: '체크인 시 침구 상태 불량',
    createdAt: '2025-01-12 09:30',
    status: '대기',
    priority: '높음',
    assignee: 'CS-지영'
  },
  {
    id: 'RP-2025-099',
    reporter: '이도윤',
    target: '이호스트',
    reason: '환불 분쟁',
    summary: '취소 수수료 관련 문의',
    createdAt: '2025-01-10 15:10',
    status: '처리중',
    priority: '중간',
    assignee: 'CS-민수'
  },
  {
    id: 'RP-2025-093',
    reporter: '최수민',
    target: '김호스트',
    reason: '소음 신고',
    summary: '야간 소음 지속',
    createdAt: '2025-01-08 21:45',
    status: '완료',
    priority: '낮음',
    assignee: 'CS-지영'
  },
  {
    id: 'RP-2025-090',
    reporter: '정하윤',
    target: '서호스트',
    reason: '시설 파손',
    summary: '객실 내 가구 파손 확인',
    createdAt: '2025-01-06 11:20',
    status: '처리중',
    priority: '높음',
    assignee: 'CS-도현'
  }
]
