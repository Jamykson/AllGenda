# AllGenda Repository Guide

## Project layout

- `frontend/`: Nuxt 4 and Vue 3 application, written in TypeScript.
- `allgenda/`: Spring Boot backend using Java 25, Maven, Spring MVC, Spring Data JPA, and PostgreSQL.
- The frontend API client is configured in `frontend/app/plugins/axios.ts`.
- Backend endpoints are in `allgenda/src/main/java/com/allgenda/controller/`; business logic, persistence, DTOs, and models live in their correspondingly named packages.

## Tooling

The root `mise.toml` sets up the dev environment with [mise](https://mise.jdx.dev): it pins tool versions (Java 25, Node 24), defines env vars (`DB_*`), and declares tasks. Run `mise trust && mise install` once, or run `./iniciar-allgenda.sh`, which also installs mise and runs `mise run setup`. Put local overrides, such as `NUXT_PUBLIC_API_BASE`, in a gitignored `mise.local.toml`.

## Common commands

Run these from anywhere in the repo (`mise tasks` lists them all):

- `mise run setup` starts the database, installs frontend dependencies, and compiles the backend.
- `mise run dev` starts the database, the backend, and the frontend together.
- `mise run backend:dev` / `mise run frontend:dev` start one side only.
- `mise run test` runs backend tests. They use an in-memory H2 database, so no PostgreSQL is needed.
- `mise run build` builds the backend jar and the frontend.
- `mise run db:up`, `db:down`, `db:psql`, `db:logs`, and `db:reset` manage the local database.

The underlying commands still work directly, as long as mise's tools and env are active: `npm run dev` / `npm run build` from `frontend/`, and `./mvnw spring-boot:run` / `./mvnw test` from `allgenda/`.

PostgreSQL runs in Docker via `compose.yaml` at `localhost:5433`, with a database named `allgenda`. `application.properties` reads the connection from the `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD` env vars set in `mise.toml`, falling back to the same values. Backend tests run against an in-memory H2 database with their own JWT secret (`allgenda/src/test/resources/application.properties`), so they do not need PostgreSQL. The backend requires `JWT_SECRET` (at least 32 bytes); `mise.toml` sets a development-only value, which can be overridden in `mise.local.toml` or the environment.

## Change guidance

- Keep frontend changes consistent with the existing Nuxt/Vue Composition API and TypeScript patterns.
- Keep backend responsibilities in their existing controller, service, repository, DTO, mapper, model, and exception layers.
- Preserve the existing Portuguese naming and user-facing language where appropriate.
- Prefer focused changes and run the relevant frontend build or backend tests for the code changed.