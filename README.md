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
| Testare API | Postman (cererile se păstrează doar în aplicație) |

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

Exemplu de verificare a utilizatorilor:

```powershell
docker exec -it user-db psql -U postgres -d user_db -c "SELECT id, email, role FROM users;"
```

Resetare completă a bazelor (șterge datele): `docker compose down -v`.

### 3. User Service

```powershell
cd backend\user-service
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
.\mvnw spring-boot:run
```

Rulează pe `http://localhost:8081`. Se oprește cu `Ctrl+C`.

Configurare (`src/main/resources/application.yaml`):

| Setare | Rol |
|---|---|
| `spring.datasource.*` | conexiune la `user-db` (port 5433); parola din `DB_PASSWORD` |
| `jwt.secret` | cheia de semnare JWT (min. 32 caractere); în producție din `JWT_SECRET` |
| `jwt.expiration-ms` | valabilitatea tokenului (86400000 = 24 h) |
| `app.admin-registration-enabled` | permite înregistrarea de administratori (`true` doar în dezvoltare) |

## API User Service

| Metodă | Endpoint | Descriere |
|---|---|---|
| POST | `/auth/register` | creează cont, returnează JWT |
| POST | `/auth/login` | autentificare, returnează JWT |

Exemplu `POST /auth/register`:

```json
{
  "email": "elena@stud.acs.upb.ro",
  "password": "parola1234",
  "firstName": "Elena",
  "lastName": "Ioniță"
}
```

Răspuns: `token`, `userId`, `email`, `role`.

### Reguli de înregistrare

- **Student:** email `nume@stud.<facultate>.upb.ro` (ex. `stud.acs.upb.ro`, `stud.etti.upb.ro`), rol `STUDENT`
- **Administrator:** email `nume@upb.ro`, rol `ADMIN`, doar dacă `app.admin-registration-enabled` este `true`
- Parola: minim 8 caractere, stocată ca hash BCrypt
- Coduri de răspuns: `201` creat, `400` date invalide sau email neinstituțional, `401` login greșit, `403` înregistrare admin dezactivată, `409` email deja folosit

JWT-ul conține: `sub` (email), `userId`, `role`, `iat`, `exp`.

### 4. API Gateway

```powershell
cd backend\api-gateway
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
.\mvnw spring-boot:run
```

Rulează pe `http://localhost:8080`. Pornesc după `user-service`.

### Porturi

| Componentă | Port |
|---|---|
| API Gateway | 8080 |
| user-service | 8081 |
| complaint-service (planificat) | 8082 |
| user-db / complaint-db / ai-db / analytics-db | 5433 / 5434 / 5435 / 5436 |

## Testare cu Postman

De acum cererile se trimit **prin gateway** (`8080`, cu prefixul `/api`), nu direct la servicii:

1. `POST http://localhost:8080/api/auth/login` (body JSON cu `email` și `password`); se copiază valoarea din `token`
2. Pentru cereri protejate: tab-ul **Authorization** > **Bearer Token** > se lipește tokenul
3. `GET http://localhost:8080/api/users/me` returnează datele utilizatorului logat

| Cerere | Rezultat așteptat |
|---|---|
| protejată, fără token | `401` „Token lipsă" |
| `Bearer abc` | `401` „Token invalid sau expirat" |
| token valid | `200` |