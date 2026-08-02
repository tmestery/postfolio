# Decisions & open questions

## Locked decisions (do not reopen without asking)

| ID | Decision | Detail |
|----|----------|--------|
| **Q1** | **localStorage demo auth** | After login, store session in `localStorage`. Not secure — acceptable because this app is for **demo only**. No JWT / Spring session work unless requested later. |
| **Q2** | **Signup fields (chosen for this app)** | See below. |
| **Q3** | **Auto-login after signup** | On successful signup, write localStorage session and navigate to `/` (feed). Better demo UX than bouncing to login. |
| **Q11** | **Postgres from the start (local)** | Local Postgres is the DB. No H2. Configure Spring datasource for Postgres in setup. |
| **Q13** | **Priority rank (chosen)** | See [Ranked implementation order](#ranked-implementation-order) below. |

### Q2 — Signup fields (locked)

| Field | Signup UI | Sent to API | Notes |
|-------|-----------|-------------|-------|
| `username` | **Required** | yes | Public identity on feed |
| `email` | **Required** | yes | Account recovery / uniqueness later |
| `password` | **Required** | yes | Min 8 chars client-side |
| `firstName` | **Optional** | yes if filled | Nice-to-have; not shown as required |
| `lastName` | **Optional** | yes if filled | Same |
| `dateOfBirth` | **Omit** | no | Skip for demo (friction + privacy) |
| `accountPublicStatus` | Hidden | always `true` | Default public so feed demos work; toggle later on `/account` |

Signup JSON shape:

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

Omit null/empty optional names rather than sending empty strings if easier.

### Q1 — localStorage contract (locked)

| Key | Value |
|-----|--------|
| Storage key | `postfolio.session` |
| Shape | JSON string: `{ "username": "<string>", "id": <number|null> }` |
| Set on | Successful login **or** successful signup (auto-login) |
| Clear on | Logout |
| AuthContext | `{ user, login, logout, isAuthenticated }` reads/writes this key |

`id` may be null until login/signup responses reliably return it. Prefer storing `id` from signup response (`WebUser.id`) when present; login today returns only username string — username is enough for demo gating.

**Demo backend bridge:** Because there is no real server session, create-post cannot rely on `@AuthenticationPrincipal`. Plan includes accepting `username` (from localStorage) on create-post for attribution during the demo. Documented in [api.md](./api.md) / [plan.md](./plan.md).

---

## Ranked implementation order (Q13)

Optimize for a **demoable** path: auth works → data persists in Postgres → feed + posts → agent wow.

| Rank | Work | Why |
|------|------|-----|
| **1** | **Postgres local + Spring datasource** | Everything else needs durable data |
| **2** | **Fix signup/login FE + API client + `VITE_*` + 404 route** | Foundation UX |
| **3** | **AuthContext + localStorage session** | Gate “logged in” UI for demo |
| **4** | **Demo post attribution** (API accepts username when no principal) | Unblocks create-post without JWT |
| **5** | **Feed UI** on `/` | Core product visible |
| **6** | **Create post UI** `/post/new` | Completes social loop |
| **7** | **Agent page** `/agent` + loading states | Demo differentiator |
| **8** | Search + delete + account privacy | Nice-to-have for fuller demo |
| **9** | CI JDK 21, security harden, polish | After demo path works |

Visual direction (Q7) still open — use temporary soft-product defaults in [frontend.md](./frontend.md) until answered; don’t block ranks 1–6 on brand.

---

## Still open (answer when ready)

Legend: **P1** = helps polished FE · **P2** = can wait

### Q4. Feed model — **P1**
Global public / following graph / hybrid?  
**Working assumption (proposed in [social-network.md](./social-network.md)):** **hybrid** — Home has **Following** (accepted followees + self) and **Discover** (global public). One-way follows; **private accounts use follow requests** (accept/decline).

**Answered:** Hybrid Following + Discover; private accounts use follow requests (see [social-network.md](./social-network.md)).

### Q5. Who runs/sees the LLM agent? — **P1**
Shared house agent / per-user / admin-only?  
**Working assumption:** shared agent; any logged-in user can open `/agent` and trigger.

**Your answer:** _TBD_

### Q6. Simulated trades only? — **P2**  
**Working assumption:** yes, simulated only forever for this project.

**Your answer:** _TBD_

### Q7. Visual direction — **P1**
Terminal finance / editorial money / soft product / custom? Logo or text-only?  
**Working assumption:** soft product — light theme, one accent, distinctive fonts (not Inter), no purple; text-only “Postfolio”.

**Your answer:** _TBD_

### Q8. Motion — **P2**  
**Working assumption:** 2–3 tasteful transitions on home/auth.

**Your answer:** _TBD_

### Q9. TypeScript? — **P2**  
**Working assumption:** stay JavaScript.

**Your answer:** _TBD_

### Q10. Component library? — **P1**  
**Working assumption:** Tailwind only; no component library.

**Your answer:** _TBD_

### Q12. Agent schedule — **P2**  
**Working assumption:** manual trigger only for demo; weekly cron later if asked.

**Your answer:** _TBD_

---

## How to answer remaining items

```text
Q4: A
Q7: B — navy + gold, serif display + sans body, text-only wordmark
```

Mark `**Answered:**` here and copy brand tokens into [frontend.md](./frontend.md) §12 when Q7 lands.
