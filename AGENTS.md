# AllGenda Repository Guide

## Project layout

- `frontend/`: Nuxt 4 and Vue 3 application, written in TypeScript.
- `allgenda/`: Spring Boot backend using Java 25, Maven, Spring MVC, Spring Data JPA, and PostgreSQL.
- The frontend API client is configured in `frontend/app/plugins/axios.ts`.
- Backend endpoints are in `allgenda/src/main/java/com/allgenda/controller/`; business logic, persistence, DTOs, and models live in their correspondingly named packages.

## Common commands

Run frontend commands from `frontend/`:

- `npm run dev` starts the Nuxt development server.
- `npm run build` builds the frontend.

Run backend commands from `allgenda/`:

- `./mvnw spring-boot:run` starts the backend.
- `./mvnw test` runs backend tests.

The backend's local datasource configuration expects PostgreSQL at `localhost:5433` with a database named `allgenda`. Backend tests that load the application context may require that database to be available.

## Change guidance

- Keep frontend changes consistent with the existing Nuxt/Vue Composition API and TypeScript patterns.
- Keep backend responsibilities in their existing controller, service, repository, DTO, mapper, model, and exception layers.
- Preserve the existing Portuguese naming and user-facing language where appropriate.
- Prefer focused changes and run the relevant frontend build or backend tests for the code changed.