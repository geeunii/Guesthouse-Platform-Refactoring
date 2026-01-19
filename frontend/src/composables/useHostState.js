export const normalizeApprovalStatus = (status) => {
  if (status === null || status === undefined) return 'unknown'
  const value = String(status).trim().toLowerCase()
  if (value === 'approved' || value === 'approve') return 'approved'
  if (
    value === 'pending' ||
    value === 'inspection' ||
    value === 'review' ||
    value === 'reviewing' ||
    value === 'under_review' ||
    value === 'underreview' ||
    value === 'recheck' ||
    value === 're-check' ||
    value === 're-review' ||
    value === 'resubmit' ||
    value === 'resubmitted' ||
    value === '재검토중' ||
    value === '재심사중' ||
    value === '재심사' ||
    value === '검수중'
  ) {
    return 'pending'
  }
  if (value === 'rejected' || value === 'reject' || value === 'denied') return 'rejected'
  if (
    value === 'active' ||
    value === 'inactive' ||
    value === 'operating' ||
    value === 'stopped' ||
    value === 'stop' ||
    value === 'paused'
  ) {
    return 'approved'
  }
  if (value === '1' || value === 'true') return 'approved'
  if (value === '2') return 'pending'
  if (value === '3' || value === '0') return 'rejected'
  return 'unknown'
}

export const deriveHostState = (accommodations = []) => {
  const list = Array.isArray(accommodations) ? accommodations : []
  const counts = {
    total: list.length,
    approved: 0,
    pending: 0,
    rejected: 0,
    unknown: 0
  }

  list.forEach((item) => {
    const statusSource = item?.approvalStatus ?? item?.status ?? item?.accommodationStatus ?? item?.reviewStatus
    const normalized = normalizeApprovalStatus(statusSource)
    if (normalized === 'approved') counts.approved += 1
    else if (normalized === 'pending') counts.pending += 1
    else if (normalized === 'rejected') counts.rejected += 1
    else counts.unknown += 1
  })

  let hostState = 'empty'
  if (counts.total === 0) hostState = 'empty'
  else if (counts.approved > 0) hostState = 'active'
  else if (counts.pending > 0) hostState = 'pending-only'
  else if (counts.rejected > 0) hostState = 'rejected'
  else hostState = 'pending-only'

  return { hostState, counts }
}

export const deriveHostGateInfo = (accommodations = []) => {
  const list = Array.isArray(accommodations) ? accommodations : []
  const readResubmitMap = () => {
    try {
      const raw = sessionStorage.getItem('hostResubmitMap')
      return raw ? JSON.parse(raw) : {}
    } catch {
      return {}
    }
  }
  const resubmitMap = typeof window !== 'undefined' ? readResubmitMap() : {}
  const resubmitWindowMs = 24 * 60 * 60 * 1000
  const isResubmittedRecently = (id) => {
    if (!id) return false
    const timestamp = resubmitMap[String(id)]
    if (!timestamp) return false
    return Date.now() - Number(timestamp) <= resubmitWindowMs
  }

  const normalized = list.map((item) => {
    const statusSource = item?.approvalStatus ?? item?.status ?? item?.accommodationStatus ?? item?.reviewStatus
    let normalizedStatus = normalizeApprovalStatus(statusSource)
    const isResubmitted = item?.isResubmitted === 1 || item?.isResubmitted === true
    const id = item?.accommodationsId ?? item?.accommodationId ?? item?.id
    if (isResubmitted) {
      normalizedStatus = 'pending'
    }
    return {
      approvalStatus: normalizedStatus === 'rejected' && isResubmittedRecently(id) ? 'pending' : normalizedStatus,
      rejectReason: item?.rejectionReason ?? item?.rejectReason ?? item?.approvalReason ?? item?.reason ?? ''
    }
  })
  const snapshot = deriveHostState(normalized)
  const rejectedItem = normalized.find((item) => item.approvalStatus === 'rejected')
  const recheckItem = normalized.find((item) => item.approvalStatus === 'pending' && item.rejectReason)
  const nextState = snapshot.hostState === 'pending-only' && recheckItem ? 'recheck' : snapshot.hostState
  const hasAnyAccommodation = snapshot.counts.total > 0
  const hasApprovedAccommodation = snapshot.counts.approved > 0
  const hasPendingLike = snapshot.counts.pending > 0
  const hasRejectedLike = snapshot.counts.rejected > 0
  const gateState = !hasAnyAccommodation
    ? 'NO_ACCOMMODATION'
    : hasApprovedAccommodation
      ? 'APPROVED'
      : 'NEEDS_APPROVAL'
  return {
    hostState: nextState,
    counts: snapshot.counts,
    rejectedReason: rejectedItem?.rejectReason ?? '',
    recheckReason: recheckItem?.rejectReason ?? '',
    hasAnyAccommodation,
    hasApprovedAccommodation,
    hasPendingLike,
    hasRejectedLike,
    gateState
  }
}

export const buildHostGateNotice = (gateInfo) => {
  const state = gateInfo?.hostState ?? 'empty'
  const isEmpty = state === 'empty'
  const isRejected = state === 'rejected'
  const isRecheck = state === 'recheck'
  const gateState = gateInfo?.gateState ?? (isEmpty ? 'NO_ACCOMMODATION' : 'NEEDS_APPROVAL')
  const title = gateState === 'NO_ACCOMMODATION'
    ? '숙소를 등록해보세요!'
    : '호스트 기능은 승인 후 이용 가능해요'
  const description = gateState === 'NO_ACCOMMODATION'
    ? '숙소를 등록하면 예약/매출/리뷰/리포트를 한 곳에서 관리할 수 있습니다.'
    : '숙소 등록 및 심사가 완료되면 예약/매출/리뷰/리포트를 사용할 수 있습니다.'
  const reason = isRejected
    ? (gateInfo?.rejectedReason ? `반려 사유: ${gateInfo.rejectedReason}` : '반려 사유를 확인할 수 없습니다.')
    : (isRecheck && gateInfo?.recheckReason ? `이전 반려 사유: ${gateInfo.recheckReason}` : '')

  return {
    title,
    description,
    reason,
    primaryText: gateState === 'NO_ACCOMMODATION' ? '숙소 등록하러 가기' : '숙소 관리로 이동',
    primaryTo: gateState === 'NO_ACCOMMODATION' ? '/host/accommodation/register' : '/host/accommodation',
    secondaryText: '닫기',
    secondaryTo: '/host'
  }
}
