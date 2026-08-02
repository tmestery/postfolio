import { describe, expect, it } from 'vitest'
import { act } from 'react'
import { createRoot } from 'react-dom/client'
import App from './app'

// React's act() environment flag for test renders.
globalThis.IS_REACT_ACT_ENVIRONMENT = true

describe('app shell', () => {
  it('renders the guest landing page without crashing', async () => {
    window.localStorage.clear()
    const container = document.createElement('div')
    document.body.appendChild(container)

    await act(async () => {
      createRoot(container).render(<App />)
    })

    expect(container.textContent).toContain('Postfolio')
    expect(container.textContent).toContain('Share your trades')
    expect(container.textContent).toContain('Log in')

    container.remove()
  })
})
