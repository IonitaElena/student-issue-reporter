# CampusVoice

Platformă inteligentă de e-Guvernare universitară pentru raportarea problemelor studențești.

## Arhitectură

- **Aplicație mobilă (studenți):** React Native + Expo (folderul `mobile/`, planificat)
- **Aplicație web (administratori):** React + Vite (folderul `web-admin/`, planificat)
- **Backend:** microservicii Spring Boot (folderul `backend/`)
  - `api-gateway`: punct unic de acces, validare JWT
  - `user-service`: autentificare, utilizatori, roluri
  - `complaint-service`: sesizări, status, voturi, comentarii
  - `ai-service`: clasificare, prioritizare, duplicate (Python, planificat)
  - `analytics-notification-service`: statistici, notificări, rapoarte PDF (planificat)
- **Baze de date:** PostgreSQL, câte una per serviciu (*Database per Service*)
- **Containerizare:** Docker Compose

## Structura repo-ului

```
student-issue-reporter/
├── backend/
│   ├── pom.xml                 (agregator Maven, pentru VS Code)
│   ├── api-gateway/
│   ├── user-service/
│   ├── complaint-service/
│   ├── ai-service/
│   └── analytics-notification-service/
├── mobile/
├── web-admin/
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

## Configurare și secrete

Toate valorile secrete stau în `.env` din rădăcina repo-ului (ignorat de Git). `.env.example` conține doar valori de exemplu.

```powershell
copy .env.example .env
```

Variabile:

| Variabilă | Rol |
|---|---|
| `POSTGRES_USER`, `POSTGRES_PASSWORD` | utilizator și parolă pentru bazele de date |
| `USER_DB_NAME`, `COMPLAINT_DB_NAME`, `AI_DB_NAME`, `ANALYTICS_DB_NAME` | numele bazelor |
| `JWT_SECRET` | cheia de semnare JWT (minim 32 de caractere), **identică** pentru gateway și user-service |

Serviciile Spring citesc `.env` prin `spring.config.import: optional:file:../../.env[.properties]` (cale relativă la `backend/<serviciu>`), deci trebuie pornite din folderul serviciului. Nu există valori implicite pentru parole sau secrete: dacă o variabilă lipsește, serviciul nu pornește (`Could not resolve placeholder`). Alternativ, variabilele pot fi setate în terminal (`$env:JWT_SECRET = "..."`).

## Rulare locală

### 1. Bazele de date

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

Resetare completă a bazelor (șterge datele): `docker compose down -v`.

### 2. Serviciile backend

Fiecare serviciu rulează ca proces separat, într-un terminal separat:

```powershell
cd backend\user-service        # apoi complaint-service, apoi api-gateway
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
.\mvnw spring-boot:run
```

Ordine recomandată: bazele de date, `user-service`, `complaint-service`, `api-gateway`. Un serviciu se oprește cu `Ctrl+C`.

| Componentă | Port |
|---|---|
| API Gateway | 8080 |
| user-service | 8081 |
| complaint-service | 8082 |

Mesaje de succes: `Successfully applied N migration(s)` (Flyway) și `Started ...Application`.

### Pornire rapidă (opțional)

Script `start-backend.ps1` în rădăcină, care pornește bazele de date și deschide câte o fereastră per serviciu:

```powershell
.\start-backend.ps1
```

## Vizualizarea datelor din baze

```powershell
docker exec -it complaint-db psql -U postgres -d complaint_db -c "SELECT id, title, category, status, priority, faculty FROM complaints;"
docker exec -it user-db psql -U postgres -d user_db -c "SELECT id, email, role FROM users;"
```

Mod interactiv: `docker exec -it complaint-db psql -U postgres -d complaint_db`, apoi `\dt` (tabele), `\d complaints` (structură), `\x` (afișare verticală), `\q` (ieșire).

## API Gateway

Punctul unic de acces către backend (Spring Cloud Gateway, varianta WebMVC). Clienții vorbesc doar cu el, nu direct cu serviciile.

- **Rutare:** `/api/auth/**` și `/api/users/**` către `user-service`, `/api/complaints/**` către `complaint-service`; prefixul `/api` este eliminat (`StripPrefix=1`)
- **Autentificare:** validează JWT-ul (`Authorization: Bearer <token>`); doar `/api/auth/**` este public
- **Identitate:** după validare adaugă headerele `X-User-Id`, `X-User-Email`, `X-User-Role`, citite de servicii fără alte verificări. Headerele cu aceste nume trimise de client sunt suprascrise
- **CORS:** permite originea web-admin (`app.cors.allowed-origins`, implicit `http://localhost:5173`); aplicația mobilă nativă nu are nevoie de CORS

| Fișier (`backend/api-gateway`) | Rol |
|---|---|
| `application.yaml` | rute, `jwt.secret`, CORS |
| `security/JwtAuthFilter.java` | validare JWT, răspuns `401` |
| `security/IdentityHeaderRequest.java` | suprascrie headerele de identitate |
| `config/CorsConfig.java` | configurare CORS |

Răspunsuri ale gateway-ului: `401` cu „Token lipsă" sau „Token invalid sau expirat".

## API

Toate cererile se trimit **prin gateway** (`http://localhost:8080/api/...`).

### User Service

| Metodă | Endpoint | Acces | Descriere |
|---|---|---|---|
| POST | `/api/auth/register` | public | creează cont, returnează JWT |
| POST | `/api/auth/login` | public | autentificare, returnează JWT |
| GET | `/api/users/me` | token | datele utilizatorului logat |

Exemplu `POST /api/auth/register`:

```json
{
  "email": "elena@stud.acs.upb.ro",
  "password": "parola1234",
  "firstName": "Elena",
  "lastName": "Ioniță"
}
```

Reguli de înregistrare:
- **Student:** email `nume@stud.<facultate>.upb.ro` (ex. `stud.acs.upb.ro`), rol `STUDENT`
- **Administrator:** email `nume@upb.ro`, rol `ADMIN`, doar dacă `app.admin-registration-enabled` este `true`
- Parola: minim 8 caractere, stocată ca hash BCrypt
- Coduri: `201` creat, `400` date invalide sau email neinstituțional, `401` login greșit, `403` înregistrare admin dezactivată, `409` email deja folosit

JWT-ul conține: `sub` (email), `userId`, `role`, `iat`, `exp` (valabil 24 h).

### Complaint Service

| Metodă | Endpoint | Descriere |
|---|---|---|
| POST | `/api/complaints` | creează o sesizare (utilizatorul din token) |
| GET | `/api/complaints` | listă paginată, cu filtre `category`, `status`, `faculty`, `dormitory`, `page`, `size` |
| GET | `/api/complaints/my` | sesizările utilizatorului logat |
| GET | `/api/complaints/{id}` | detaliile unei sesizări |

Exemplu `POST /api/complaints`:

```json
{
  "title": "Proiector defect în sala AN 031",
  "description": "Proiectorul nu pornește de două săptămâni.",
  "category": "INFRASTRUCTURE",
  "building": "Corp A",
  "locationText": "Sala AN 031",
  "latitude": 44.4388,
  "longitude": 26.0505
}
```

Model de date (`complaints`):

| Câmp | Detalii |
|---|---|
| `category` | `ADMINISTRATIVE`, `INFRASTRUCTURE`, `ACADEMIC`, `SOCIAL`, `DORMITORY`, `OTHER` |
| `status` | `NEW` (implicit), `IN_PROGRESS`, `RESOLVED`, `REJECTED` |
| `priority` | `LOW`, `MEDIUM` (implicit, va fi estimată de AI Service), `HIGH`, `URGENT` |
| `userId` | din headerul `X-User-Id`; fără foreign key (altă bază de date) |
| `faculty` | dedusă din emailul studentului (`stud.acs.upb.ro` dă `acs`) |
| `dormitory`, `building`, `locationText`, `latitude`, `longitude` | locația sesizării |
| `assignedDepartment` | departamentul către care este redirecționată (de completat de administrator) |
| `createdAt`, `updatedAt`, `resolvedAt` | date de urmărire |

Structura codului (`backend/complaint-service`): `model` (entități și enum-uri), `repository`, `dto`, `service` (logica de business, filtrare cu `Specification`), `controller`. Migrările Flyway sunt în `src/main/resources/db/migration` (`V1__create_complaints_table.sql`). Serviciul nu folosește Spring Security: identitatea vine din headerele puse de gateway.

## Testare cu Postman

1. `POST http://localhost:8080/api/auth/login` (body JSON cu `email` și `password`); se copiază valoarea din `token`
2. Pentru cereri protejate: tab-ul **Authorization** > **Bearer Token** > se lipește tokenul
3. `POST http://localhost:8080/api/complaints` creează o sesizare, `GET /api/complaints` o listează

| Cerere | Rezultat așteptat |
|---|---|
| protejată, fără token | `401` „Token lipsă" |
| `Bearer abc` | `401` „Token invalid sau expirat" |
| token valid | `200` / `201` |
| sesizare cu `title` gol sau `category` inexistentă | `400` |

Notă: la schimbarea `JWT_SECRET`, tokenurile vechi devin invalide (se face login din nou).
