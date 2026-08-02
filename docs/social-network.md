# Social network layer — Follows, networked feed, notifications

**Status:** Implemented (follows, requests, networked feed, profiles, notifications).  
**Audience:** Coding agents + humans adding the missing “connections” product layer.  
**Related:** [architecture.md](./architecture.md) · [api.md](./api.md) · [frontend.md](./frontend.md) · [open-questions.md](./open-questions.md) · [plan.md](./plan.md)

---

## 1. Goal

Postfolio markets itself as a **social** investing app (“share with your connections”), but today the feed is a **global public timeline** with no graph, no profiles, and no notifications. This doc designs the networking layer that makes the product feel like a lightweight social network — without becoming a full Twitter clone.

**In scope for this track**

1. **Follow / unfollow** other users (subscribe to their public posts)
2. **Networked Home feed** — primarily people you follow (+ yourself), with a Discover fallback
3. **Public profiles** — view another user’s posts + follow CTA
4. **Notifications** — in-app inbox for follow events and activity from people you follow
5. **Unread badge** in the shell

**Out of scope (v1 of this track)**

- DMs, comments, likes, reposts  
- Push / email / SMS notifications  
- Real-time websockets (polling is fine for demo)  
- Recommendations ML / “people you may know” ranking  
- Blocking / muting / reporting (can add later)  
- Changing the locked demo auth model (`localStorage` + username bridge)

**In scope (privacy):** Private accounts use a **follow-request** flow (request → accept / decline). Public accounts stay one-click Follow.

---

## 2. Why this is missing (current state)

| Area | Today | Gap |
|------|-------|-----|
| Feed | `GET /post/feed/` → all posts from `accountPublicStatus=true` | No graph; “connections” is marketing-only |
| Users | Signup / account privacy toggle | No profile page, no follow edges |
| Discovery | Ticker search only | Can’t find people by username |
| Notifications | None | No reason to return to the app after posting |
| Shell | Home / Agent / Account | No notifications entry, no people surface |

Architecture already notes this: *“README mentions connections — follow graph is not built.”*

---

## 3. Locked design decisions (proposed)

These close **Q4 (feed model)** for the networking track. Treat as locked once this doc is approved; update [open-questions.md](./open-questions.md) accordingly.

| ID | Decision |
|----|----------|
| **S1** | **Follow graph is one-way**, with **status**: `pending` \| `accepted`. Public target → Follow creates `accepted` immediately. Private target → Follow creates `pending` request (Instagram-style). |
| **S2** | **Home feed = hybrid.** Default tab **Following**: posts from **accepted** followees **+ your own posts**. Second tab **Discover**: global feed of **public** accounts only. |
| **S3** | **Privacy:** Private accounts’ posts appear only for the owner and users with an **accepted** follow. Private authors are excluded from Discover. Pending requesters do **not** see posts. |
| **S3b** | **Request inbox:** Private account owners **accept** or **decline** requests. Decline deletes (or marks rejected) the edge; requester may send again later. |
| **S3c** | **Visibility transitions:** Public → private: existing **accepted** follows remain accepted; new followers must request. Private → public: all **pending** requests auto-accept (simplest demo UX). |
| **S4** | **Self-follow forbidden.** Following yourself is a 400. |
| **S5** | **Notifications are in-app only** — types in v1: `follow` (public instant), `follow_request`, `follow_accepted`, `followed_post`. |
| **S6** | **Demo auth bridge continues** — mutating endpoints accept `username` (actor) from the client session, same as create-post. Document clearly; not production security. |
| **S7** | **No websockets in v1.** FE polls notifications every ~30s while the app is open, and on focus / route enter. |
| **S8** | **User search** by username prefix for Find people (public + private usernames discoverable; private posts still gated). |

### Working defaults (tunable)

| Knob | Default |
|------|---------|
| Max follows per user (soft) | 500 |
| Notifications page size | 30 |
| Poll interval | 30s |
| `followed_post` notifications | **On** for each new post by someone you follow (demo-scale OK; no digest yet) |
| Empty Following feed | Show empty state + CTA to Discover / Find people — do **not** silently dump global posts into Following |

---

## 4. Product UX

### 4.1 Shell additions

Keep the existing top bar; add:

| Entry | Placement | Notes |
|-------|-----------|-------|
| **Notifications** | Nav icon / link with unread count badge | Route `/notifications` |
| **Find people** | Link near Home or inside empty feed | Route `/people` (search) |
| Profile avatar | Already → `/account` for self; posts/usernames → `/u/:username` | |

Mobile: unread badge on Notifications; keep the floating compose `+`.

### 4.2 Home feed (`/`)

Two tabs inside the existing feed surface:

```
[ Following ]  [ Discover ]
```

- **Following** — chronological posts from followees + self. Empty copy: “Follow people to build your feed” + links to Discover / Find people.
- **Discover** — today’s global public feed (rename of current behavior) + ticker search stays here.
- Composer strip (“What did you buy?”) stays on both tabs.

### 4.3 Profile (`/u/:username`)

Profile page (not the same as `/account` settings):

- Avatar initial, username, optional name, lock hint if private
- Follower / following counts (**accepted** edges only)
- Relationship CTA (hidden on own profile; own profile links to Account):

| Viewer state | Button |
|--------------|--------|
| Not following | **Follow** (public) or **Request** (private) |
| Pending outbound | **Requested** (click → cancel request) |
| Accepted | **Following** (click → unfollow confirm) |
| Incoming pending (you are the private owner viewing requester’s profile) | Optional; primary accept/decline lives in Notifications |

- Posts list: shown if account is public **or** viewer is owner **or** viewer has **accepted** follow  
- Otherwise: “This account is private. Request to follow to see their trades.”

### 4.4 Find people (`/people`)

Simple search:

- Input: username query
- Results: username, avatar, follow/request state, private badge, link to profile
- Optional “Suggested”: recent **public** posters not yet followed — nice-to-have slice

### 4.5 Notifications (`/notifications`)

Timeline list, newest first. Follow-request rows include **Accept** / **Decline** actions inline.

| Type | Copy example | Actions / deep link |
|------|--------------|---------------------|
| `follow` | **@alex** followed you | → `/u/alex` |
| `follow_request` | **@alex** requested to follow you | **Accept** · **Decline** · → `/u/alex` |
| `follow_accepted` | **@alex** accepted your follow request | → `/u/alex` |
| `followed_post` | **@alex** posted **NVDA** | → `/` or future `/post/:id` |
| `system` (optional) | Agent run completed | → `/agent` |

Row states: unread (soft accent background) vs read. Actions:

- Click row (non-request) → mark read + navigate  
- Accept / Decline on `follow_request` → update edge + mark that notification read  
- **Mark all read** (does **not** auto-accept requests)  
- Unread count in nav = `GET /notifications/unread-count/`

No cards fetish — same timeline language as the feed ([frontend.md](./frontend.md) §7).

---

## 5. Data model (Postgres)

### 5.1 `user_follow`

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGSERIAL / sequence | PK |
| `follower_id` | BIGINT FK → `web_user` | Who clicked Follow / Request |
| `followee_id` | BIGINT FK → `web_user` | Target account |
| `status` | VARCHAR | `pending` \| `accepted` |
| `created_at` | TIMESTAMP | When requested / followed |
| `responded_at` | TIMESTAMP nullable | When accept happened (null if pending) |

**Constraints**

- `UNIQUE (follower_id, followee_id)`
- `CHECK (follower_id <> followee_id)`
- Indexes on `follower_id`, `followee_id`, `(followee_id, status)` for request inbox

**Decline behavior:** delete the row (requester can request again). No long-lived `rejected` status required for v1.

### 5.2 `notification`

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGSERIAL | PK |
| `recipient_id` | BIGINT FK → `web_user` | Who sees it |
| `actor_id` | BIGINT FK → `web_user` nullable | Who caused it |
| `type` | VARCHAR | `follow` \| `follow_request` \| `follow_accepted` \| `followed_post` \| `system` |
| `post_id` | BIGINT FK nullable | For `followed_post` |
| `follow_id` | BIGINT FK nullable | For request accept/decline wiring |
| `message` | VARCHAR | Pre-rendered short text for demo simplicity |
| `read_at` | TIMESTAMP nullable | null = unread |
| `created_at` | TIMESTAMP | |

Indexes: `(recipient_id, created_at DESC)`, `(recipient_id, read_at)` for unread counts.

### 5.3 Entities / packages

```
models/follow/Follow.java
models/follow/FollowRepository.java
models/follow/FollowService.java
models/notification/Notification.java
models/notification/NotificationRepository.java
models/notification/NotificationService.java
controllers/social/FollowController.java
controllers/social/NotificationController.java
controllers/social/ProfileController.java   # or fold into account/user
```

JPA `ddl-auto=update` remains fine for local demo (same as rest of app).

---

## 6. API contracts (target)

All mutating calls use the **demo username bridge** unless noted. Prefer query/body field `username` = actor (from `postfolio.session`).

### 6.1 Follow & requests

#### `POST /social/follow/`

```json
{ "username": "me", "targetUsername": "alex" }
```

| Target | Result |
|--------|--------|
| Public | Edge `status=accepted`; notify alex with `follow` |
| Private | Edge `status=pending`; notify alex with `follow_request` |

| Status | Meaning |
|--------|---------|
| `201` | Created — body `{ follower, followee, status, createdAt }` |
| `200` | Already pending or accepted (idempotent; return current status) |
| `400` | Self-follow / missing fields |
| `404` | Unknown user |

#### `POST /social/unfollow/` (also cancels a pending request)

```json
{ "username": "me", "targetUsername": "alex" }
```

Idempotent `204`. Deletes pending or accepted edge.

#### `POST /social/follow/accept/`

```json
{ "username": "alex", "requesterUsername": "me" }
```

Only the **followee** (private owner) may accept. Sets `status=accepted`, `responded_at=now`, notifies requester with `follow_accepted`.  
`200` + edge; `404` if no pending request; `403` if caller ≠ followee.

#### `POST /social/follow/decline/`

```json
{ "username": "alex", "requesterUsername": "me" }
```

Deletes pending edge; marks related `follow_request` notification read. Idempotent `204`.

#### `GET /social/follow/requests/?username=alex`

Pending inbound requests for `alex`: `[{ followerUsername, createdAt, followId }]`.

#### `GET /social/following/?username=me`

**Accepted** followees only: `[{ id, username, firstName, lastName }]`.

#### `GET /social/followers/?username=alex`

**Accepted** followers. Allowed if `alex` is public, or viewer is `alex`, or viewer has accepted follow on `alex`.

#### `GET /social/follows/status/?username=me&targetUsername=alex`

```json
{ "status": "none" | "pending" | "accepted" }
```

### 6.2 Feed

#### `GET /post/feed/?username=me&mode=following`

- `mode=following` (default when `username` present): posts by **accepted** followees ∪ self (includes private followees’ posts once accepted), newest first  
- `mode=discover`: posts from **public** accounts only (legacy global feed)  

Keep existing path for FE compatibility; **add query params** rather than a new URL.

Response: same `Post[]` shape as today.

#### `GET /post/feed/` (no username)

Discover-only (public posts).

### 6.3 Profiles & people

#### `GET /users/{username}/`

```json
{
  "username": "alex",
  "firstName": "Alex",
  "lastName": "Ng",
  "accountPublicStatus": false,
  "followerCount": 12,
  "followingCount": 4,
  "viewerRelationship": "none",
  "canViewPosts": false
}
```

`viewerRelationship`: `none` | `pending` | `accepted` | `self`.  
`canViewPosts`: true if public **or** self **or** accepted follow.

Optional query `?viewer=me` under demo auth.

#### `GET /users/{username}/posts/`

Posts newest first when `canViewPosts`; otherwise `403` + `{ "error": "This account is private" }`. `404` if user missing.

#### `GET /users/search/?q=al&username=me`

Up to 20 users matching username prefix (public and private); include `viewerRelationship` + `accountPublicStatus`.

### 6.4 Notifications

#### `GET /notifications/?username=me&limit=30`

```json
[
  {
    "id": 1,
    "type": "follow_request",
    "message": "@alex requested to follow you",
    "actorUsername": "alex",
    "followId": 9,
    "postId": null,
    "read": false,
    "createdAt": "2026-08-02T03:11:00"
  }
]
```

#### `GET /notifications/unread-count/?username=me`

`{ "count": 3 }`

#### `POST /notifications/read/`

```json
{ "username": "me", "ids": [1, 2] }
```

or `{ "username": "me", "all": true }` → `204`  
(`all` does not accept follow requests.)

### 6.5 Notification write paths (server-side only)

| Event | Who gets notified |
|-------|-------------------|
| A follows public B | B ← `follow` |
| A requests private B | B ← `follow_request` |
| B accepts A | A ← `follow_accepted` |
| A creates a post | Each **accepted** follower of A ← `followed_post` |

Do **not** notify on Discover browsing. Cap fan-out mentally for demo (hundreds of followers max).

---

## 7. Frontend map

| Route | Page | Auth |
|-------|------|------|
| `/` | Home with Following / Discover tabs | Authed feed; guest hero unchanged |
| `/u/:username` | Profile | Public page; Follow requires auth |
| `/people` | Find people | Authed |
| `/notifications` | Notification inbox | Authed |
| `/account` | Settings (privacy) only | Authed |

### API helpers (`Frontend/src/api/`)

- `social.js` — follow, unfollow, status, lists  
- `users.js` — profile, search, user posts  
- `notifications.js` — list, unread count, mark read  

### Shell

- Notifications nav item + badge (`useNotifications` hook: poll unread count)  
- Clickable `@username` on `PostCard` → `/u/:username`  
- Profile Follow button wired to social API  

### Visual rules

Still governed by [frontend.md](./frontend.md): one composition, no notification “widget dashboard,” no purple SaaS, timeline over card grids. Badge = small numeric accent, not a pill cluster.

---

## 8. Sequence diagrams

### Follow public account

```
FE                     API                      DB
│ POST /social/follow/  │                        │
│ {me, alex}            │                        │
│──────────────────────▶│ alex is public         │
│                       │ insert follow ACCEPTED │
│                       │ notify alex: follow    │
│◀──── 201 status=accepted ─────────────────────│
```

### Request private account + accept

```
FE                     API                      DB
│ POST /social/follow/ {me, privatePat}          │
│──────────────────────▶│ insert follow PENDING  │
│                       │ notify pat: request    │
│◀──── 201 status=pending ──────────────────────│
│                                                │
│ POST /social/follow/accept/                    │
│ {username:pat, requesterUsername:me}           │
│──────────────────────▶│ status → ACCEPTED      │
│                       │ notify me: accepted    │
│◀──── 200 ─────────────│                        │
```

### Home Following feed

```
FE                     API                      DB
│ GET /post/feed/?username=me&mode=following     │
│──────────────────────▶│ accepted followee ids  │
│                       │ posts where user IN (…)│
│                       │ OR user = me           │
│◀──── Post[] ──────────│                        │
```

### New post → fan-out notifications

```
FE                     API                      DB
│ POST /post/stock/     │                        │
│──────────────────────▶│ save post              │
│                       │ accepted followers only│
│                       │ insert N notifications │
│◀──── 201 post ────────│                        │
```

---

## 9. Implementation slices

Each slice: code + **1 positive + 3 negative** tests + demoable checkpoint. Prefer small PRs.

### Slice 0 — Design doc (this file)
- **Done when:** Approved; Q4 updated in open-questions; plan.md links here.

### Slice 1 — Follow entity + public follow API
- **Goal:** Persist edges with `status`; public follow / unfollow / status / lists.  
- **Done when:** Curl can follow a public user as `accepted`; self-follow rejected.  
- **Tests:** follow success; self-follow 400; duplicate idempotent; unfollow idempotent.

### Slice 1b — Private follow requests
- **Goal:** Pending edges; accept / decline / list inbound requests.  
- **Done when:** Requesting a private user yields `pending`; accept → `accepted` + `follow_accepted` notif.  
- **Tests:** request creates pending; accept by owner; decline by owner; non-owner accept → 403.

### Slice 2 — Networked feed modes
- **Goal:** `mode=following|discover` on `/post/feed/`.  
- **Done when:** Following uses **accepted** edges ∪ self (incl. private followees); Discover = public only.  
- **Tests:** accepted private followee posts appear; pending does not; empty following; discover excludes private.

### Slice 3 — Profiles + user search
- **Goal:** `GET /users/{username}/`, posts, search; FE `/u/:username` + `/people`.  
- **Done when:** Profile shows Request / Requested / Following; private posts gated.  
- **Tests:** public profile; private hides posts until accepted; search prefix; unknown 404.

### Slice 4 — Notification persistence + APIs
- **Goal:** Create on follow / request / accept / post; list / unread / mark read.  
- **Done when:** Request shows Accept/Decline payload (`followId`); posting fans out to accepted followers.  
- **Tests:** follow_request created; accept notifies requester; post fans out; mark all read leaves requests unresolved.

### Slice 5 — FE Home tabs + shell badge + request actions
- **Goal:** Following/Discover tabs; notifications with Accept/Decline; badge poll; username links.  
- **Done when:** Demo path below works in the UI.  
- **Tests:** tab empty state; badge count; accept from inbox; guest cannot open `/notifications`.

### Slice 6 — Docs cleanup
- Update api.md, architecture.md, frontend.md §6, README social pillar, plan.md checklist.

---

## 10. Demo script (definition of done)

1. User **A** and **B** sign up (both public).  
2. **A** finds **B** → **Follow** → **B** gets `follow` notification.  
3. **B** posts NVDA → **A** sees it under Following + `followed_post` notif.  
4. **C** signs up and goes **private**.  
5. **A** opens **C** → **Request** → button shows **Requested**; **A** cannot see **C**’s posts yet.  
6. **C** opens Notifications → **Accept** → **A** gets `follow_accepted`.  
7. **C** posts a trade → **A** sees it under Following (still absent from Discover).  
8. **D** requests **C** → **C** **Declines** → **D** stays blocked from posts; can request again later.  
9. CI green without real auth tokens.

---

## 11. Risks & mitigations

| Risk | Mitigation |
|------|------------|
| Empty graph makes Following boring | Discover tab + Find people + empty-state CTAs |
| Fan-out on popular users | Soft max follows; demo data stays small |
| Username-bridge spoofing | Accepted for demo (same as create-post); document only |
| Notification spam | One notif per follow edge create; one per post to each follower; no email |
| Scope creep (likes/comments) | Explicitly out of scope until this track ships |

---

## 12. Open items (non-blocking defaults)

| Item | Default in this doc | Change if you want |
|------|---------------------|--------------------|
| Private follow model | **Request → accept/decline** | — |
| Private → public pending requests | **Auto-accept all pending** | Leave pending |
| Show follower counts on private profiles | **Yes** | Hide counts |
| `followed_post` notifications | **On** (accepted followers only) | Following feed only, no notif |
| Suggested people module | Stretch in Slice 3 | Skip |
| Deep link to single post | Feed / profile for now | Add `/post/:id` later |
| Agent run notifications | Optional later | Add type `agent_run` |

---

## 13. Reading order for implementers

1. This document (§3–§6 especially)  
2. [frontend.md](./frontend.md) before any UI  
3. [api.md](./api.md) — update as slices land  
4. Existing `PostService` / `PostCard` / `Layout` — extend, don’t fork a second feed stack  

**Do not implement until this design is approved** (or the user says to proceed).
