import { apiFetch } from './client'

export async function followUser({ username, targetUsername }) {
  const { data, status } = await apiFetch('/social/follow/', {
    method: 'POST',
    body: { username, targetUsername },
  })
  return { data, status }
}

export async function unfollowUser({ username, targetUsername }) {
  await apiFetch('/social/unfollow/', {
    method: 'POST',
    body: { username, targetUsername },
  })
}

export async function acceptFollowRequest({ username, requesterUsername }) {
  const { data } = await apiFetch('/social/follow/accept/', {
    method: 'POST',
    body: { username, requesterUsername },
  })
  return data
}

export async function declineFollowRequest({ username, requesterUsername }) {
  await apiFetch('/social/follow/decline/', {
    method: 'POST',
    body: { username, requesterUsername },
  })
}

export async function getFollowStatus({ username, targetUsername }) {
  const { data } = await apiFetch('/social/follows/status/', {
    params: { username, targetUsername },
  })
  return data?.status ?? 'none'
}
