import { authenticatedRequest } from './authClient';

// 현재 로그인한 사용자 정보 조회
export async function getCurrentUser() {
  return authenticatedRequest('/api/users/me', {
    method: 'GET',
  });
}

// 사용자 프로필(닉네임, 전화번호) 업데이트
export async function updateUserProfile(profileData) {
  return authenticatedRequest('/api/users/me/profile', {
    method: 'PATCH',
    body: JSON.stringify(profileData),
  });
}

// 본인 계정 삭제
export async function deleteSelf(reasons, otherReason) {
  return authenticatedRequest('/api/users/me', {
    method: 'DELETE',
    body: JSON.stringify({ reasons, otherReason }),
  });
}
