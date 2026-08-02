import { apiFetch } from './client'

export async function getFeed() {
  const { data } = await apiFetch('/post/feed/')
  return Array.isArray(data) ? data : []
}

/**
 * `username` is the demo session bridge — required while the API has no
 * server-side session (see docs/api.md).
 */
export async function createPost({ username, stock, shares, investedAmount, dateInvested }) {
  const { data } = await apiFetch('/post/stock/', {
    method: 'POST',
    body: { username, stock, shares, investedAmount, dateInvested },
  })
  return data
}

export async function searchPosts(stockName) {
  const { data } = await apiFetch('/post/stock/search/', {
    method: 'POST',
    params: { stockName },
  })
  return Array.isArray(data) ? data : []
}

export async function deletePost({ postId, username }) {
  await apiFetch('/post/delete/', { method: 'POST', params: { postId, username } })
}
