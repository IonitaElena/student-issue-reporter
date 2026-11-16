# CampusVoice

Platformă inteligentă de e-Guvernare universitară pentru raportarea problemelor studențești.

## Arhitectură

- **Frontend:** React (folderul `frontend/`)
- **Backend:** microservicii Spring Boot (folderul `backend/`)
  - `api-gateway`: punct unic de acces, validare JWT
  - `user-service`: autentificare, utilizatori, roluri
  - `complaint-service`: sesizări, voturi, comentarii
  - `ai-service`: clasificare, prioritizare, duplicate (Python)
  - `analytics-notification-service`: statistici, notificări, rapoarte PDF
- **Baze de date:** PostgreSQL, câte una per serviciu (*Database per Service*)
- **Containerizare:** Docker Compose

## Structura repo-ului

```
student-issue-reporter/
├── backend/
│   ├── api-gateway/
│   ├── user-service/
│   ├── complaint-service/
│   ├── ai-service/
│   └── analytics-notification-service/
├── frontend/
├── docker-compose.yml
├── .env.example
└── README.md
```

## Cerințe (instalate)

| Tool | Versiune |
|---|---|
| Git | 2.44 |
| JDK | 21 (Temurin) |
| Node.js / npm | 26.x / 11.x |
| Python | 3.11 |
| Docker Desktop | 29.x |
| Editor | VS Code (Extension Pack for Java, Spring Boot Extension Pack, Python, Docker, ESLint, Prettier) |

Pe mașina de dezvoltare `java` din PATH este JDK 25, deci pentru Maven se setează JDK 21 în fiecare terminal nou:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
```

## Rulare locală

### 1. Variabile de mediu

```powershell
copy .env.example .env
```

Apoi se setează `POSTGRES_PASSWORD` în `.env` (fișierul nu se urcă în Git).

### 2. Bazele de date

```powershell
docker compose up -d
docker compose ps
```

| Container | Port local | Bază de date |
|---|---|---|
| user-db | 5433 | user_db |
| complaint-db (PostGIS) | 5434 | complaint_db |
| ai-db | 5435 | ai_db |
| analytics-db | 5436 | analytics_db |

Acces în consola SQL:

```powershell
docker exec -it user-db psql -U postgres -d user_db
```

Comenzi utile: `\dt` (tabele), `\d users` (structură), `\q` (ieșire).

Resetare completă a bazelor (șterge datele): `docker compose down -v`.

### 3. User Service

```powershell
cd backend\user-service
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
.\mvnw spring-boot:run
```

Rulează pe `http://localhost:8081`.
