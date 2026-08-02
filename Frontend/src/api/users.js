import { apiFetch } from './client'

export async function getProfile(username, viewer) {
  const params = viewer ? { viewer } : undefined
  const { data } = await apiFetch(`/users/${encodeURIComponent(username)}/`, { params })
  return data
}

export async function getUserPosts(username, viewer) {
  const params = viewer ? { viewer } : undefined
  const { data } = await apiFetch(`/users/${encodeURIComponent(username)}/posts/`, { params })
  return Array.isArray(data) ? data : []
}

export async function searchUsers(q, username) {
  const params = { q }
  if (username) params.username = username
  const { data } = await apiFetch('/users/search/', { params })
  return Array.isArray(data) ? data : []
}
