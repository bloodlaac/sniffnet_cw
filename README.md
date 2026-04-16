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

SQL-описание структуры БД:

```text
database/schema.sql
```

При первом создании контейнера PostgreSQL этот скрипт монтируется в
`/docker-entrypoint-initdb.d/01-schema.sql` и выполняется entrypoint-ом образа
`postgres:16`.

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
