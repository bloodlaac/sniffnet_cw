# SniffNet CW

Учебный проект SniffNet:

- `backend` на `Java 17 + Spring Boot 4`
- `frontend` на `Angular 21 + TypeScript 5.9`

## Backend

Локальный запуск:

```bash
./mvnw spring-boot:run
```

Docker PostgreSQL:

```bash
docker compose -f docker/docker-compose.yml up -d postgres
```

Swagger UI:

`http://localhost:8080/swagger-ui.html`

## Frontend

Локальный запуск:

```bash
cd frontend
npm install
npm start
```

## Тестовые пользователи

- `admin / admin123`
- `demo / demo123`
