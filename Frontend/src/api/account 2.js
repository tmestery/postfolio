import { apiFetch } from './client'

export async function getAccountStatus(username) {
  const { data } = await apiFetch('/account/status/', { params: { username } })
  return data
}

export async function setAccountStatus({ username, accountPublic }) {
  const { data } = await apiFetch('/account/status/', {
    method: 'POST',
    body: { username, accountPublic },
  })
  return data
}
