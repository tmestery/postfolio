import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { getProfile, getUserPosts } from '../../api/users'
import { followUser, unfollowUser } from '../../api/social'
import { deletePost } from '../../api/posts'
import Avatar from '../../components/Avatar'
import PostCard from '../../components/PostCard'

export default function ProfilePage() {
  const { username } = useParams()
  const { user, isAuthenticated } = useAuth()
  const viewer = user?.username
  const [profile, setProfile] = useState(null)
  const [posts, setPosts] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      const p = await getProfile(username, viewer)
      setProfile(p)
      if (p.canViewPosts) {
        setPosts(await getUserPosts(username, viewer))
      } else {
        setPosts([])
      }
      setStatus('ready')
    } catch (err) {
      setError(err.message)
      setStatus('error')
    }
  }, [username, viewer])

  useEffect(() => {
    load()
  }, [load])

  async function handleFollowAction() {
    if (!isAuthenticated || !viewer || !profile) return
    setBusy(true)
    setError('')
    try {
      const rel = profile.viewerRelationship
      if (rel === 'accepted' || rel === 'pending') {
        await unfollowUser({ username: viewer, targetUsername: username })
      } else {
        await followUser({ username: viewer, targetUsername: username })
      }
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (status === 'loading') {
    return <p className="py-10 text-center text-sm text-muted">Loading profile…</p>
  }
  if (status === 'error') {
    return (
      <p aria-live="polite" className="rounded-2xl border border-line bg-surface px-4 py-6 text-sm text-danger">
        {error}
      </p>
    )
  }

  const isSelf = profile.viewerRelationship === 'self' || viewer === username
  const rel = profile.viewerRelationship
  let followLabel = profile.accountPublicStatus ? 'Follow' : 'Request'
  if (rel === 'pending') followLabel = 'Requested'
  if (rel === 'accepted') followLabel = 'Following'

  return (
    <section className="overflow-hidden rounded-2xl border border-line bg-surface shadow-[0_1px_0_rgba(20,24,22,0.04)]">
      <div className="border-b border-line px-5 py-5">
        <div className="flex items-start gap-3">
          <Avatar name={profile.username} size="lg" />
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="font-display text-2xl font-semibold text-ink">@{profile.username}</h1>
              {!profile.accountPublicStatus && (
                <span className="text-xs font-medium uppercase tracking-wide text-muted">Private</span>
              )}
            </div>
            {(profile.firstName || profile.lastName) && (
              <p className="text-sm text-muted">
                {[profile.firstName, profile.lastName].filter(Boolean).join(' ')}
              </p>
            )}
            <p className="mt-2 text-sm text-muted">
              <span className="font-semibold text-ink">{profile.followerCount}</span> followers ·{' '}
              <span className="font-semibold text-ink">{profile.followingCount}</span> following
            </p>
          </div>
          {isSelf ? (
            <Link
              to="/account"
              className="rounded-md border border-line px-3 py-1.5 text-sm font-medium text-ink hover:border-accent"
            >
              Edit
            </Link>
          ) : isAuthenticated ? (
            <button
              type="button"
              disabled={busy}
              onClick={handleFollowAction}
              className={`rounded-md px-3 py-1.5 text-sm font-medium disabled:opacity-50 ${
                rel === 'accepted' || rel === 'pending'
                  ? 'border border-line text-ink hover:border-danger hover:text-danger'
                  : 'bg-accent text-white hover:bg-accent-deep'
              }`}
            >
              {busy ? '…' : followLabel}
            </button>
          ) : (
            <Link to="/login" className="text-sm font-medium text-accent-deep hover:underline">
              Log in to follow
            </Link>
          )}
        </div>
        {error && (
          <p aria-live="polite" className="mt-3 text-sm text-danger">
            {error}
          </p>
        )}
      </div>

      {!profile.canViewPosts ? (
        <p className="px-5 py-10 text-center text-sm text-muted">
          This account is private. Request to follow to see their trades.
        </p>
      ) : posts.length === 0 ? (
        <p className="px-5 py-10 text-center text-sm text-muted">No trades posted yet.</p>
      ) : (
        posts.map((post) => (
          <PostCard
            key={post.id}
            post={post}
            currentUsername={viewer}
            onDelete={
              isSelf
                ? async (postId) => {
                    await deletePost({ postId, username: viewer })
                    setPosts((current) => current.filter((p) => p.id !== postId))
                  }
                : undefined
            }
          />
        ))
      )}
    </section>
  )
}
