# CampusVoice

Platformă inteligentă de e-Guvernare universitară pentru raportarea problemelor studențești.

## Arhitectură

- **Aplicație mobilă (studenți):** React Native + Expo (folderul `mobile/`, planificat)
- **Aplicație web (administratori):** React + Vite (folderul `web-admin/`, planificat)
- **Backend:** microservicii Spring Boot (folderul `backend/`)
  - `api-gateway`: punct unic de acces, validare JWT
  - `user-service`: autentificare, utilizatori, roluri
  - `complaint-service`: sesizări, status, voturi, comentarii, imagini
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
├── start-backend.ps1
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

Setări specifice `complaint-service` (`application.yaml`):

| Setare | Rol |
|---|---|
| `spring.web.error.include-message: always` | afișează motivul erorilor în răspuns (Spring Boot 4; **doar în dezvoltare**) |
| `spring.servlet.multipart.max-file-size: 5MB` / `max-request-size: 6MB` | limite pentru încărcarea imaginilor |
| `app.upload-dir: uploads` | folderul în care se salvează imaginile (ignorat de Git) |

În `api-gateway`, `spring.servlet.multipart.enabled: false` este necesar pentru ca încărcările de fișiere să fie retransmise nealterate către servicii.

## Rulare locală

**Docker Desktop trebuie pornit** (`Engine running`) înainte de orice altceva. Dacă nu este, comenzile `docker` și conexiunile la baze eșuează (`dockerDesktopLinuxEngine`, `Connection refused`).

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
.\mvnw clean spring-boot:run
```

Ordine recomandată: bazele de date, `user-service`, `complaint-service`, `api-gateway`. Un serviciu se oprește cu `Ctrl+C`.

| Componentă | Port |
|---|---|
| API Gateway | 8080 |
| user-service | 8081 |
| complaint-service | 8082 |

Mesaje de succes: `Successfully applied N migration(s)` (Flyway) și `Started ...Application`.

`clean` șterge `target/` înainte de pornire. E necesar când apare `Unresolved compilation problem`: VS Code compilează în fundal în același folder și poate lăsa clase generate fără dependențe.

### Pornire rapidă

Scriptul `start-backend.ps1` (în rădăcină) pornește bazele de date, apoi `user-service`, `complaint-service` (așteaptă să asculte pe port) și la final `api-gateway`, fiecare într-o fereastră separată:

```powershell
.\start-backend.ps1
```

## Vizualizarea datelor din baze

```powershell
docker exec -it complaint-db psql -U postgres -d complaint_db -c "SELECT id, title, category, status, priority, faculty FROM complaints;"
docker exec -it user-db psql -U postgres -d user_db -c "SELECT id, email, role FROM users;"
```

Mod interactiv: `docker exec -it complaint-db psql -U postgres -d complaint_db`, apoi `\dt` (tabele), `\d complaints` (structură), `\x` (afișare verticală), `\q` (ieșire).

Parolele nu pot fi citite din baza de date: se stochează doar hash BCrypt (`$2a$10$...`). Pentru un cont de test uitat, se poate seta o parolă nouă în dezvoltare:

```powershell
docker exec -it user-db psql -U postgres -d user_db -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
docker exec -it user-db psql -U postgres -d user_db -c "UPDATE users SET password_hash = crypt('parola1234', gen_salt('bf', 10)) WHERE email = 'ana@stud.acs.upb.ro';"
```

Tabelele `spatial_ref_sys` și schema `tiger` din `complaint_db` vin de la imaginea PostGIS și nu se modifică.

## API Gateway

Punctul unic de acces către backend (Spring Cloud Gateway, varianta WebMVC). Clienții vorbesc doar cu el, nu direct cu serviciile.

- **Rutare:** `/api/auth/**` și `/api/users/**` către `user-service`, `/api/complaints/**` către `complaint-service`; prefixul `/api` este eliminat (`StripPrefix=1`)
- **Autentificare:** validează JWT-ul (`Authorization: Bearer <token>`); doar `/api/auth/**` este public
- **Identitate:** după validare adaugă headerele `X-User-Id`, `X-User-Email`, `X-User-Role`, citite de servicii fără alte verificări. Headerele cu aceste nume trimise de client sunt suprascrise
- **CORS:** permite originea web-admin (`app.cors.allowed-origins`, implicit `http://localhost:5173`); aplicația mobilă nativă nu are nevoie de CORS

| Fișier (`backend/api-gateway`) | Rol |
|---|---|
| `application.yaml` | rute, `jwt.secret`, CORS, multipart |
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

#### Sesizări

| Metodă | Endpoint | Acces | Descriere |
|---|---|---|---|
| POST | `/api/complaints` | token | creează o sesizare (utilizatorul din token) |
| GET | `/api/complaints` | token | listă paginată, cu filtre `category`, `status`, `faculty`, `dormitory`, `page`, `size` |
| GET | `/api/complaints/my` | token | sesizările utilizatorului logat |
| GET | `/api/complaints/{id}` | token | detaliile unei sesizări |
| PATCH | `/api/complaints/{id}/status` | `ADMIN` | schimbă statusul și, opțional, departamentul |
| GET | `/api/complaints/{id}/history` | token | istoricul modificărilor de status |

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

Exemplu `PATCH /api/complaints/{id}/status`:

```json
{
  "status": "IN_PROGRESS",
  "comment": "Preluat de echipa IT",
  "assignedDepartment": "Departament IT"
}
```

Reguli: doar `ADMIN` poate schimba statusul (`403` pentru student); `resolvedAt` se completează la `RESOLVED` și se golește dacă sesizarea este redeschisă; `400` dacă statusul este deja cel curent și nu se indică un departament. Fiecare schimbare (inclusiv crearea) este înregistrată în `complaint_status_history`.

#### Voturi

| Metodă | Endpoint | Descriere |
|---|---|---|
| POST | `/api/complaints/{id}/votes` | votează sesizarea |
| DELETE | `/api/complaints/{id}/votes` | retrage votul |
| GET | `/api/complaints/{id}/votes` | `voteCount` și `votedByMe` |

Reguli: un vot per utilizator și sesizare (constrângere unică în baza de date, `409` la repetare); nu se poate vota propria sesizare (`400`); retragerea unui vot inexistent dă `404`.

#### Comentarii

| Metodă | Endpoint | Descriere |
|---|---|---|
| POST | `/api/complaints/{id}/comments` | adaugă un comentariu (`{"content": "..."}`, max. 1000 caractere) |
| GET | `/api/complaints/{id}/comments` | lista comentariilor, în ordine cronologică |
| DELETE | `/api/complaints/{id}/comments/{commentId}` | șterge (autorul sau `ADMIN`; `403` altfel) |

#### Imagini

| Metodă | Endpoint | Descriere |
|---|---|---|
| POST | `/api/complaints/{id}/images` | încarcă o imagine (`multipart/form-data`, câmpul `file`; autorul sau `ADMIN`) |
| GET | `/api/complaints/{id}/images` | lista imaginilor |
| GET | `/api/complaints/{id}/images/{imageId}/content` | descarcă imaginea |
| DELETE | `/api/complaints/{id}/images/{imageId}` | șterge imaginea (autorul sau `ADMIN`) |

Reguli: JPEG, PNG sau WebP, verificate după conținut (primii octeți), nu după extensie sau `Content-Type`; maxim 5 MB (`413`) și 5 imagini per sesizare; fișierele se salvează în `uploads/` cu nume generat (UUID), iar numele original se păstrează doar în baza de date. La dockerizare, `uploads/` trebuie montat ca volum.

#### Hartă

| Metodă | Endpoint | Descriere |
|---|---|---|
| GET | `/api/complaints/map` | markere în zona `minLat`, `minLng`, `maxLat`, `maxLng`; filtre opționale `category`, `status`, `limit` (implicit 200, maxim 500) |
| GET | `/api/complaints/nearby` | markere în raza `radius` (metri, implicit 500, maxim 50000) în jurul punctului `lat`, `lng`, ordonate după distanță; aceleași filtre |

Răspunsul conține, pentru fiecare sesizare cu coordonate: `id`, `title`, `category`, `status`, `priority`, `latitude`, `longitude`, `voteCount` și, la `/nearby`, `distanceMeters`. Sesizările fără coordonate nu apar pe hartă. Limitele greșite (latitudine în afara intervalului -90..90, `min` mai mare decât `max`, rază 0) dau `400`.

Interogările folosesc PostGIS: coloana `complaints.location` (`geography(Point, 4326)`) este calculată automat din `latitude` și `longitude` și are index GiST.

#### Model de date

| Tabel | Conținut |
|---|---|
| `complaints` | sesizările |
| `complaint_status_history` | istoricul statusurilor (vechi, nou, autor, comentariu, dată) |
| `complaint_votes` | voturile (unic pe `complaint_id` + `user_id`) |
| `complaint_comments` | comentariile |
| `complaint_images` | metadatele imaginilor |

Câmpurile din `complaints`:

| Câmp | Detalii |
|---|---|
| `category` | `ADMINISTRATIVE`, `INFRASTRUCTURE`, `ACADEMIC`, `SOCIAL`, `DORMITORY`, `OTHER` |
| `status` | `NEW` (implicit), `IN_PROGRESS`, `RESOLVED`, `REJECTED` |
| `priority` | `LOW`, `MEDIUM` (implicit, va fi estimată de AI Service), `HIGH`, `URGENT` |
| `userId` | din headerul `X-User-Id`; fără foreign key (altă bază de date) |
| `faculty` | dedusă din emailul studentului (`stud.acs.upb.ro` dă `acs`) |
| `dormitory`, `building`, `locationText`, `latitude`, `longitude` | locația sesizării |
| `assignedDepartment` | departamentul către care este redirecționată (completat de administrator) |
| `createdAt`, `updatedAt`, `resolvedAt` | date de urmărire |
| `location` | coloană `geography(Point, 4326)` calculată din `latitude` și `longitude`, folosită la interogările pe hartă; nu se completează manual |

Structura codului (`backend/complaint-service`): `model` (entități și enum-uri), `repository`, `dto`, `service` (`ComplaintService`, `InteractionService` pentru voturi și comentarii, `ImageService`, `MapService` pentru hartă), `controller`. Migrările Flyway sunt în `src/main/resources/db/migration`:

| Migrare | Conținut |
|---|---|
| `V1__create_complaints_table.sql` | tabela `complaints` |
| `V2__create_status_history.sql` | istoricul statusurilor |
| `V3__create_votes_and_comments.sql` | voturi și comentarii |
| `V4__create_complaint_images.sql` | metadate imagini |
| `V5__add_complaint_location.sql` | coloana `location` (PostGIS) și indexul GiST |

Serviciul nu folosește Spring Security: identitatea (`X-User-Id`, `X-User-Email`, `X-User-Role`) vine din headerele puse de gateway, iar rolul `ADMIN` este verificat în logica de business.

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
| `PATCH .../status` cu token de student | `403` |
| vot pe propria sesizare | `400` |
| al doilea vot al aceluiași utilizator | `409` |
| încărcare imagine cu alt tip de fișier | `400` |
| `GET /api/complaints/map` cu limite valide | `200`, lista markerelor din zonă |
| `GET /api/complaints/map` fără parametri sau cu `minLat` mai mare decât `maxLat` | `400` |
| `GET /api/complaints/nearby?lat=44.4388&lng=26.0505&radius=1000` | `200`, ordonate după `distanceMeters` |

Pentru voturi și comentarii sunt necesare două conturi de student (autorul nu își poate vota sesizarea), plus un cont `admin@upb.ro` pentru statusuri. Pentru încărcarea unei imagini: metoda **POST**, **Body** > **form-data**, cheia `file` cu tipul **File**.

Pentru hartă este util să existe cel puțin două sesizări cu coordonate apropiate (de exemplu la 300 m una de alta) și una fără coordonate, care nu trebuie să apară în rezultate.

Notă: la schimbarea `JWT_SECRET`, tokenurile vechi devin invalide (se face login din nou).
