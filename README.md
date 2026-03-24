# SniffNet CW

Backend MVP на `Java 17 + Spring Boot 4` для курсового проекта SniffNet.

Локальный запуск:

```bash
./mvnw spring-boot:run
```

Docker PostgreSQL:

```bash
docker compose -f docker/docker-compose.yml up -d postgres
```

Тестовые пользователи после старта:

- `admin / admin123`
- `demo / demo123`

Swagger UI:

`http://localhost:8080/swagger-ui.html`
