# ODA Media Service
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/OpenDonationAssistant/oda-media-service)
![Sonar Tech Debt](https://img.shields.io/sonar/tech_debt/OpenDonationAssistant_oda-media-service?server=https%3A%2F%2Fsonarcloud.io)
![Sonar Violations](https://img.shields.io/sonar/violations/OpenDonationAssistant_oda-media-service?server=https%3A%2F%2Fsonarcloud.io)
![Sonar Tests](https://img.shields.io/sonar/tests/OpenDonationAssistant_oda-media-service?server=https%3A%2F%2Fsonarcloud.io)
![Sonar Coverage](https://img.shields.io/sonar/coverage/OpenDonationAssistant_oda-media-service?server=https%3A%2F%2Fsonarcloud.io)

## Running with Docker

### Using GitHub Container Registry

```bash
docker run -d \
  --name oda-media-service \
  -p 8080:8080 \
  -e JDBC_URL=jdbc:postgresql://postgres:5432/oda \
  -e JDBC_USER=postgres \
  -e JDBC_PASSWORD=your_password \
  -e RABBITMQ_HOST=rabbitmq \
  -e YOUTUBE_KEY=your_youtube_api_key \
  ghcr.io/opendonationassistant/media-service:latest
```

### Required Environment Variables

| Variable | Description |
|----------|-------------|
| `JDBC_URL` | PostgreSQL JDBC URL (e.g., `jdbc:postgresql://postgres:5432/oda`) |
| `JDBC_USER` | PostgreSQL username |
| `JDBC_PASSWORD` | PostgreSQL password |
| `RABBITMQ_HOST` | RabbitMQ host address |
| `YOUTUBE_KEY` | YouTube Data API key |

### Optional Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `MICRONAUT_SECURITY_TOKEN_JWT_SIGNATURES_JWKS_KEYCLOAK_URL` | `https://auth.oda.digital/realms/ODA/protocol/openid-connect/certs` | Keycloak JWKS URL for JWT validation |

### Docker Compose Example

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: oda
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: your_password
    volumes:
      - postgres_data:/var/lib/postgresql/data

  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "15672:15672"

  oda-media-service:
    image: ghcr.io/opendonationassistant/media-service:latest
    depends_on:
      - postgres
      - rabbitmq
    environment:
      JDBC_URL: jdbc:postgresql://postgres:5432/oda
      JDBC_USER: postgres
      JDBC_PASSWORD: your_password
      RABBITMQ_HOST: rabbitmq
      YOUTUBE_KEY: your_youtube_api_key
    ports:
      - "8080:8080"

volumes:
  postgres_data:
```

Run with:
```bash
docker compose up -d
```
