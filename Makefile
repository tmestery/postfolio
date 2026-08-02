# Common local commands. Run from the repo root.
#   make help

.PHONY: help env up down logs ps postgres backend frontend test-be test-fe test clean

help:
	@echo "Postfolio targets:"
	@echo "  make env        Create .env + Frontend/.env from .env.example (no overwrite)"
	@echo "  make up         docker compose up --build (postgres + backend + frontend)"
	@echo "  make down       docker compose down"
	@echo "  make postgres   Start only Postgres in Docker"
	@echo "  make backend    Run Spring Boot on the host (needs Postgres)"
	@echo "  make frontend   Run Vite on the host"
	@echo "  make test       Backend + frontend tests"
	@echo "  make test-be    Backend Maven tests"
	@echo "  make test-fe    Frontend Vitest"
	@echo "  make logs       Tail compose logs"
	@echo "  make clean      Stop compose and remove local build artifacts"

env:
	@test -f .env || cp .env.example .env
	@test -f Frontend/.env || (grep -E '^(VITE_|#)' .env.example > Frontend/.env && echo "Wrote Frontend/.env")
	@echo "Edit .env and put GROQ_API_KEY there."

up: env
	docker compose up --build

down:
	docker compose down

postgres: env
	docker compose up -d postgres

logs:
	docker compose logs -f

ps:
	docker compose ps

backend: env
	cd Backend/postfolio && ./mvnw spring-boot:run

frontend: env
	cd Frontend && npm install && npm run dev

test-be:
	cd Backend/postfolio && ./mvnw -q test

test-fe:
	cd Frontend && npm test

test: test-be test-fe

clean:
	docker compose down -v --remove-orphans || true
	cd Backend/postfolio && ./mvnw -q clean || true
	rm -rf Frontend/dist Frontend/node_modules
