import { apiFetch } from './client'

export async function listNotifications(username) {
  const { data } = await apiFetch('/notifications/', { params: { username } })
  return Array.isArray(data) ? data : []
}

export async function unreadNotificationCount(username) {
  const { data } = await apiFetch('/notifications/unread-count/', { params: { username } })
  return typeof data?.count === 'number' ? data.count : 0
}

export async function markNotificationsRead({ username, ids, all }) {
  await apiFetch('/notifications/read/', {
    method: 'POST',
    body: { username, ids, all: Boolean(all) },
  })
}
