import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <section className="flex flex-col items-center gap-4 py-24 text-center">
      <p className="font-display text-6xl font-semibold text-ink">404</p>
      <p className="text-muted">That page doesn&apos;t exist.</p>
      <Link
        to="/"
        className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-deep"
      >
        Back to the feed
      </Link>
    </section>
  )
}
