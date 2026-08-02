# Frontend guide (read this before touching UI)

**Audience:** humans and coding agents. Frontend work is where agents most often go wrong — follow this doc literally. If something here conflicts with a skill or habit, **this doc wins**.

**Stack today:** React 19 · Vite 7 · JavaScript (not TypeScript) · React Router 7 · Tailwind CSS 4 · no UI component library · no state library · no test runner yet.

**Product:** Postfolio is a **web app** (responsive browser). Not React Native, not Capacitor, not a “mobile-first app shell.” Design for desktop first, then make narrow viewports work.

Related docs: [architecture.md](./architecture.md) · [api.md](./api.md) · [plan.md](./plan.md) · [open-questions.md](./open-questions.md)

---

## 1. Current state

| Area | Reality |
|------|---------|
| Pages | `home` (guest hero + authed feed), `login`, `signup`, `newpost`, `agent`, `account`, `notfound` — all implemented |
| API helpers | `src/api/` — `client.js` fetch wrapper + per-domain helpers; `constants.js` uses `import.meta.env.VITE_*` |
| Auth | `src/auth/` — `session.js` (localStorage `postfolio.session`), `AuthContext.jsx`, `useAuth.js` |
| Routes | All registered in `routes.jsx` incl. `path="*"` 404; `/post/new`, `/agent`, `/account` behind `RequireAuth` |
| Design tokens | In `index.css` `@theme` — see §12 for the actual values |
| Tests | Vitest (jsdom) — `npm test`; see `src/api/client.test.js`, `src/auth/session.test.js`, `src/app.test.jsx` |

The historical bugs listed in §11 are fixed — the table is kept as a regression checklist.

---

## 2. Directory conventions

Keep this shape. Do not invent `components/ui/button/Button.tsx` trees or feature-folder chaos unless the plan says so.

```
Frontend/src/
  api/                 # fetch client, constants, endpoint helpers
  assets/              # static images/svg only
  pages/               # route-level screens only (one folder per route)
    home/index.jsx
    login/index.jsx
    signup/index.jsx
    notfound/index.jsx
    # later: feed is home; post/new; agent; account
  components/          # shared presentational pieces (create when first shared)
  context/ or auth/    # session provider (when Phase 1 lands)
  app.jsx              # shell only — providers + <Routes />
  routes.jsx           # all Route declarations
  main.jsx             # createRoot
  index.css            # Tailwind import + CSS variables (design tokens)
```

### Rules

1. **One page folder = one route.** Default export from `index.jsx`.
2. **No business fetch logic inside giant JSX blobs.** Call `api/*` helpers.
3. **Shared UI** only when used by ≥2 pages. Until then, keep markup in the page.
4. **No new dependencies** without asking (no MUI, Chakra, shadcn dump, axios, Redux, Zustand, Framer unless approved in open-questions).
5. Prefer **JSX + Tailwind utility classes**. Co-locate tiny CSS only if Tailwind can’t express it; put tokens in `index.css`.

---

## 3. Routing

File: `src/routes.jsx`

| Path | Page | Auth | Notes |
|------|------|------|-------|
| `/` | Home / feed | Public for now; later may show CTA when logged out | Replace placeholder |
| `/login` | Login | Guest | Redirect to `/` if already logged in (once session exists) |
| `/signup` | Signup | Guest | Same |
| `/post/new` | Create investment post | Authed (Phase 2) | Not built |
| `/agent` | LLM agent trades | Authed (logged-in via localStorage) | Not built; shared house agent assumed |
| `/account` | Privacy / profile settings | Authed | Not built |
| `*` | NotFound | Public | **Must wire** `<Route path="*" element={<NotFoundPage />} />` |

Use React Router’s `<Navigate>` for auth redirects — do not `window.location = ...` except full logout edge cases.

---

## 4. Environment & API base URL

Vite only exposes env vars prefixed with `VITE_`.

**Broken today:**

```js
// src/api/constants.js — DO NOT KEEP THIS PATTERN
export const SERVER_URL = process.env.SERVER_URL
```

**Correct pattern:**

```js
export const IS_PROD = import.meta.env.PROD
export const SERVER_URL = import.meta.env.VITE_SERVER_URL ?? 'http://localhost:8080'
export const CLIENT_URL = import.meta.env.VITE_CLIENT_URL ?? 'http://localhost:5173'
```

Add `Frontend/.env.example`:

```
VITE_SERVER_URL=http://localhost:8080
VITE_CLIENT_URL=http://localhost:5173
```

Never commit real secrets. Frontend should not hold Finnhub/Ollama keys.

### Fetch rules

1. Always build URLs as `` `${SERVER_URL}/credentials/signup/` `` — never hardcode `http://8080` or omit hostname.
2. Trailing slashes matter for this backend — match existing mappings (`/signup/`, `/login/`, `/feed/`).
3. `Content-Type: application/json` on POST bodies.
4. Parse carefully: login success is `202` with a **plain username string**, not JSON object. Signup returns a `WebUser` JSON (password hashed — still don’t display it).
5. Surface user-visible errors in the form UI — **no `alert()`** for product flows.
6. CORS is locked to `http://localhost:5173` with credentials allowed. If using cookies later, set `credentials: 'include'`.

See [api.md](./api.md) for request/response shapes.

---

## 5. Auth UX (frontend contract) — **LOCKED: localStorage demo**

This project is **demo-only**. Do **not** build JWT or cookie sessions unless the human asks.

### Server reality

- Login: `POST /credentials/login/` → `202` + **plain-text** username, or `400` + `"Failed!"`
- Signup: `POST /credentials/signup/` → `200` + `WebUser` JSON (includes `id`)
- Spring Security permits `/credentials/**` and `/post/**` without auth
- `@AuthenticationPrincipal` on create-post is usually **null** — use the **demo username bridge** (send `username` from localStorage; backend resolves user). See [api.md](./api.md).

### localStorage session (required)

| Item | Value |
|------|--------|
| Key | `postfolio.session` |
| Value | `{"username":"tyler","id":1}` (`id` optional/null if unknown) |
| Set | After successful **login** or **signup** (auto-login) |
| Clear | Logout |
| API | `AuthContext`: `{ user, login, logout, isAuthenticated }` |

Rules:

1. One AuthContext — pages must not each invent their own storage keys.
2. `isAuthenticated === Boolean(user?.username)`.
3. Protected routes (`/post/new`, `/agent`, `/account`): if not authed → `<Navigate to="/login" />`.
4. No token refresh, no refresh cookies, no “secure auth” theater.
5. Do not store passwords in localStorage.

### Signup form (locked fields)

| UI field | Required | Maps to JSON |
|----------|----------|--------------|
| Username | yes | `username` |
| Email | yes | `email` |
| Password | yes (min 8) | `password` |
| First name | no | `firstName` if non-empty |
| Last name | no | `lastName` if non-empty |
| DOB | **do not show** | — |
| Public/private | **do not show** | always send `accountPublicStatus: true` |

**camelCase only.** Jackson will not map `first_name` / `dob_*` to Java fields.

```json
{
  "username": "tyler",
  "email": "t@example.com",
  "password": "password123",
  "firstName": "Tyler",
  "lastName": "Mestery",
  "accountPublicStatus": true
}
```

**After signup:** auto-login — write `postfolio.session` (include `id` from response) → navigate `/`.

**Login body:** `{ "username", "password" }` only. On `202`, store `{ username: text, id: null }` (or look up later if we add an endpoint).

---

## 6. Screens — what “done” looks like

### Login (`/login`)

- Brand name **Postfolio** visible as a primary identity signal (not only a tiny nav word).
- Fields: username, password.
- Submit → API → on success navigate `/`; on failure inline error (“Invalid username or password”).
- Link to `/signup`.
- Loading disabled button while request in flight.
- Heading must say login language, **not** “Create your account”.

### Signup (`/signup`)

- Fields: username, email, password required; first/last optional; **no DOB**.
- Always send `accountPublicStatus: true`.
- Submit → `POST /credentials/signup/` → **auto-login** (write localStorage) → `/`.
- Inline validation: required fields, basic email shape, password min length 8.
- Never hit the login endpoint for signup.
- Never use snake_case body keys.

### Home / feed (`/`)

- Logged-out: short value prop + CTAs (Log in / Sign up). Do **not** dump stats, feature grids, or marketing card walls in the first viewport.
- Logged-in: chronological list of investment posts from `GET /post/feed/`.
- Each post row/block shows: username, ticker, shares, invested amount (or price × shares), date invested, date posted.
- Empty state: one sentence + CTA to create a post.
- Error state: one sentence + retry.
- Search by ticker can be a simple input above the list (Phase 2.3) — not a separate “dashboard widget cluster.”

### Create post (`/post/new`)

- Authed only (localStorage).
- Fields: `stock`, `shares`, `investedAmount`, `dateInvested` (`yyyy-MM-DD`), plus demo bridge field `username` from session (or header — match [api.md](./api.md)).
- Client-side: ticker uppercase; shares > 0; amount > 0; date not in the future.
- Success → navigate to feed and refetch.

### Agent (`/agent`)

- Authed only (localStorage). Shared house agent for demo.
- Trigger research: `GET /trade/stock/test/` → map of `ticker → shares`.
- Execute: `GET /trade/stock/execute/` → richer object (prices, allowance).
- These calls can take **tens of seconds** (Ollama). Mandatory: loading state, cancel/leave messaging, never block the whole app shell without feedback.
- Show results as a simple list/table — not a trading terminal parody.

### Account

- Toggle public/private via account API once backend package bug is fixed.
- Show username; do not show password hash.

### 404

- Simple message + link home. Wire the catch-all route.

---

## 7. Visual design rules (non-negotiable for agents)

These exist because AI frontends converge on the same generic look. **Avoid** that look.

### Composition

- First viewport = **one composition**, not a dashboard.
- On branded/marketing surfaces: **Postfolio** is hero-level, not an eyebrow.
- Hero budget: brand, one headline, one short sentence, one CTA group, optional one dominant visual. No stats strips, schedule chips, promo pills, or metadata rows in the first viewport.
- **No cards in the hero.** Cards only when they wrap a real interaction (form, feed item actions). If removing border/shadow/radius doesn’t hurt understanding, don’t use a card.
- One job per section: one purpose, one headline, usually one supporting sentence.

### Look & color

- Define CSS variables in `index.css` (background, surface, text, muted, accent, danger, border, font-display, font-body).
- **Do not** default to: purple-on-white / purple→indigo gradients; warm cream `#F4F1EA` + terracotta + serif; broadsheet hairline newspaper layouts; dark-mode-by-default; glow effects; `rounded-full` pill clusters; multi-layer shadows; emoji as decoration.
- Prefer a clear direction once chosen in [open-questions.md](./open-questions.md). Until then, use a restrained light theme with a single accent and strong typography — document the chosen tokens in this file when decided.
- Backgrounds: subtle gradient or soft texture OK; flat pure white/gray only is weak; abstract gradient ≠ “the” visual idea for a marketing hero.

### Typography

- Do **not** use Inter, Roboto, Arial, or system-ui as the expressive brand face.
- Load 1 display + 1 body font via `@font-face` or a privacy-friendly host; set variables.
- Type scale: few sizes (e.g. display, title, body, small). No random `text-large` custom class unless defined.

### Motion

- For visually led pages: 2–3 intentional motions (e.g. fade/slide on hero CTAs, subtle feed item enter). No continuous ambient particle noise.

### Layout & responsive

- Max content width for reading/forms (~28–40rem for auth forms; wider for feed ~40–56rem).
- Mobile: single column; tap targets ≥ 44px; no horizontal scroll.
- Do not ship a bottom-tab “app” chrome unless we explicitly decide to. Prefer top bar: brand left, nav/actions right.

### Tailwind

- Tailwind v4 via `@tailwindcss/vite` — configure tokens in CSS (`@theme`) rather than a giant `tailwind.config.js` unless needed.
- Prefer semantic classes mapped to tokens (`bg-background`, `text-muted`) over raw `bg-slate-50` soup once tokens exist.
- Invalid utilities like `w-200` / `text-large` (present in signup/login) must be fixed to real Tailwind or custom theme keys.

---

## 8. Component & React patterns

- Functional components only.
- Local `useState` for forms; lift to context only for auth/session.
- Do **not** add `useMemo` / `useCallback` by default.
- Effects: fetch on mount for feed; cleanup ignore flag or abort controller for race safety.
- Lists: stable `key={post.id}` — never array index for feed items.
- Accessibility: label every input (`htmlFor` + `id`); errors with `aria-live="polite"`; buttons have clear text (not icon-only without aria-label).
- Do not leave `console.error` as the only failure mode.

### Suggested feed item structure (not a card fetish)

```text
[username]  ·  [relative or short date]
TICKER   shares × amount (or price/share)
Invested [date]
[Delete]  (only if owner — once auth exists)
```

Typography hierarchy > boxed chrome.

---

## 9. Anti-patterns (agents: stop)

1. Rebuilding the app with Next.js / Remix / TypeScript “while you’re here.”
2. Installing shadcn + 40 Radix primitives for three forms.
3. Purple gradient SaaS landing with feature card grid.
4. Fake charts / sparkline decoration with no data.
5. Hardcoding `localhost:8080` in page files.
6. Using `alert()` / `prompt()` for UX.
7. Duplicating fetch logic per page.
8. Massive `App.jsx` with all screens inline.
9. “Dashboard” home with 6 widgets before feed works.
10. Dark mode toggle before core flows work.
11. Animating everything.
12. Committing `.env` with secrets.
13. Matching backend bugs by inventing snake_case JSON that never maps.
14. Treating this as a native mobile project.

---

## 10. Implementation checklist (every FE PR)

- [ ] Touches only the slice in [plan.md](./plan.md)
- [ ] Uses `VITE_*` + `SERVER_URL` helper
- [ ] Loading + empty + error states for async views
- [ ] No `alert()`
- [ ] Works at 375px width and 1280px width
- [ ] No new dependency unless approved
- [ ] Follows visual rules in §7
- [ ] 1 positive + 3 negative tests when a test harness exists; until then, manual test notes in PR

---

## 11. Historical bugs (all fixed — do not reintroduce)

| Bug | File | Fix |
|-----|------|-----|
| Signup URL `http://8080/...` | `pages/signup` | ✅ Uses `SERVER_URL` via `api/auth.js` |
| Signup calls `/credentials/login/` | `pages/signup` | ✅ Calls `/credentials/signup/` |
| Snake_case body fields | `pages/signup` | ✅ camelCase matching `WebUser` |
| Login heading wrong | `pages/login` | ✅ Login copy + working inputs |
| `process.env` in Vite | `api/constants.js` | ✅ `import.meta.env.VITE_*` |
| 404 not routed | `routes.jsx` | ✅ `path="*"` registered |
| Invalid Tailwind (`w-200`, `text-large`) | login/signup | ✅ Real utilities + tokens |

---

## 12. Design tokens (chosen — “soft product” direction, Q7 working assumption)

Direction: light warm theme, one deep market-green accent, serif display type. No purple gradients, no dark-SaaS clone. Fonts load from Google Fonts in `index.html`: **Fraunces** (display) + **Instrument Sans** (body).

Actual values in `src/index.css`:

```css
@import 'tailwindcss';

@theme {
  --color-paper: #faf9f6;      /* page background */
  --color-surface: #ffffff;    /* inputs, raised surfaces */
  --color-ink: #1c1d1f;        /* primary text */
  --color-muted: #6e6a61;      /* secondary text */
  --color-line: #e7e4dc;       /* borders/dividers */
  --color-accent: #1e6b4f;     /* brand green — buttons, links */
  --color-accent-deep: #14503a;/* hover state */
  --color-accent-soft: #e7f0eb;/* tinted backgrounds */
  --color-danger: #b3261e;
  --color-danger-soft: #f9e9e8;

  --font-display: 'Fraunces', Georgia, serif;
  --font-sans: 'Instrument Sans', system-ui, sans-serif;
  --font-mono: ui-monospace, 'SF Mono', Menlo, monospace; /* tickers */
}
```

Usage rules: tickers always `font-mono font-bold`; dollar amounts in `text-accent-deep`; headings `font-display`; body defaults to `font-sans` via `body` styles. If the palette changes, update this section and `index.css` together.
