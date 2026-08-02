# postfolio

Track your investments and trades with your connections. Includes an LLM agent trader that researches market news and simulates its own trades.

**Platform:** Web app (React + Spring Boot) — not a native mobile app. Target is responsive browser UI (desktop + phone browsers).

**Demo defaults:** localStorage auth · local PostgreSQL · see [docs/open-questions.md](docs/open-questions.md).

## Docs (start here)

| Doc | When |
|-----|------|
| [docs/README.md](docs/README.md) | Index |
| [docs/frontend.md](docs/frontend.md) | **Any UI work** — detailed FE rules |
| [docs/open-questions.md](docs/open-questions.md) | Decisions we still need from you |
| [docs/plan.md](docs/plan.md) | What to build next |
| [docs/architecture.md](docs/architecture.md) | How the system fits together |
| [docs/api.md](docs/api.md) | Backend HTTP contracts |
| [docs/setup.md](docs/setup.md) | Run locally |

Coding agents: read [`AGENTS.md`](AGENTS.md).

## Quick start

```bash
# Backend
cd Backend/postfolio
export FINNHUB_API_KEY=your_key
./mvnw spring-boot:run

# Frontend (separate terminal)
cd Frontend
npm install
npm run dev
```

Details: [docs/setup.md](docs/setup.md).

## Contributors

* **Mason Hart**
* **Tyler Mestery**
