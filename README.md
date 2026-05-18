# padel-manager

[![CI](https://github.com/AimeJNka/padel-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/AimeJNka/padel-manager/actions/workflows/ci.yml)

Full-stack web app (Spring Boot + Angular + PostgreSQL + Docker) for multi-site padel court management: match creation, payments, penalties, slot generation and admin statistics. Academic project – EPHEC 2025-2026.

## Local development

Tests requiring a Spring context (e.g., `PadelManagerApplicationTests`) need a live Postgres on `localhost:5432`. Start it before running `./mvnw test`:

```bash
docker compose up -d
```

The CI workflow provides its own Postgres service and does not depend on local Docker.
