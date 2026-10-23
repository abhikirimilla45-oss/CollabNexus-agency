# CollabNexus

**Where Brand Meets Influence**

CollabNexus is a two-sided influencer marketing marketplace for brands, agencies, and creators. It is a portfolio-grade Java Full Stack application built as a modular monolith: one Spring Boot process serves both the JSON API and the HTML/CSS/vanilla-JavaScript interface.

This repository is delivered in ten phases and currently contains the complete **Phase 1-10** implementation: foundation, interface, authentication and profiles, campaign marketplace, applications and collaborations, deliverables and contracts, payments and analytics, messaging and notifications, reviews and administration, plus production-readiness work (MySQL profile, Flyway migrations, Docker, automated tests, and documentation).

Everything described below exists and runs today. Optional items that are deliberately not implemented are listed in [Section 13](#13-deliberate-limitations).

---

## 1. Feature checklist

### Public website

- [x] Landing page with hero, value proposition, featured creators and campaigns, workflow, benefits, sample testimonials, FAQ, and calls to action
- [x] Features, How It Works, Pricing, About, Contact, and FAQ pages
- [x] Public campaign discovery with keyword, category, platform, budget, sorting, pagination, loading, and empty states
- [x] Public creator discovery with privacy-aware previews, niche, platform, country, availability, sorting, and pagination
- [x] Login and registration for brands and creators
- [x] Privacy Policy and Terms of Service pages
- [x] Responsive navigation, keyboard-friendly controls, reduced-motion support, toasts, modals, and accessible form errors

### Brand workspace

- [x] Company profile editing with validation and ownership checks
- [x] Campaign drafts with the full brief: objectives, audience, platforms, niches, formats, dates, budget, compensation model, capacity, deliverables, and review rules
- [x] Publish, pause, and close campaigns with validated lifecycle transitions
- [x] Application review queue with search, status filtering, pagination, shortlist, accept, and reject
- [x] Capacity enforcement, including a concurrency test proving the limit cannot be exceeded
- [x] Collaboration records created automatically from accepted applications
- [x] Contract terms, version history, send-for-acknowledgement, printable summary, and two-party acknowledgement
- [x] Deliverable creation, revision requests, approval/rejection, deadline and overdue tracking
- [x] Payment milestone records with validated status transitions and financial summaries
- [x] Campaign analytics, application funnel, budget allocation, deliverable completion, and manually entered campaign results
- [x] Brand reviews for eligible completed collaborations

### Creator workspace

- [x] Creator profile with biography, niche, platforms, languages, audience location, formats, indicative rates, portfolio, and availability
- [x] Social channel records and portfolio items
- [x] Saved campaign shortlist
- [x] Campaign discovery and application submission with proposal, price, availability, and portfolio link
- [x] Application history and withdrawal before the deadline
- [x] Collaboration and deliverable workspace with submission URLs, optional supporting files, revisions, and feedback
- [x] Contract review and acknowledgement
- [x] Recorded earnings and outstanding payment records
- [x] Creator reviews for eligible completed collaborations

### Administration

- [x] Platform overview statistics
- [x] User search, role filtering, suspension, and reactivation
- [x] Campaign oversight and administrative close
- [x] Report and dispute queue with resolution notes
- [x] Review moderation (published, hidden, under review)
- [x] Audit log for administrative and financial actions
- [x] Campaign category and platform-setting management
- [x] Contact-message handling
- [x] Platform analytics

### Messaging and notifications

- [x] Collaboration-linked conversations with participant access control
- [x] Read/unread tracking, pagination, and safe text rendering
- [x] In-app notifications for applications, collaboration events, deliverables, payments, contracts, and messages
- [ ] Email delivery through SMTP (deliberately deferred; see limitations)

### Security and quality

- [x] BCrypt password hashing (cost 11)
- [x] HTTP-only session cookies with SameSite=Lax and secure cookies in production
- [x] CSRF protection for cookie-authenticated writes
- [x] Role-based access control and server-side ownership checks
- [x] Input validation with Jakarta Bean Validation and centralized JSON errors
- [x] Authentication rate limiting and account-suspension checks
- [x] Safe output rendering with DOM `textContent` and allow-listed URLs
- [x] File upload size/type restrictions and non-executable storage
- [x] JPA parameter binding (no string-built SQL)
- [x] Audit logging and generic external errors (no stack traces)
- [x] 64 automated tests, including the complete twelve-step workflow

---

## 2. Architecture in plain language

```text
Browser (HTML + CSS + vanilla JS, no build step)
        |
        v
Spring Security filter chain
  session cookie, CSRF, roles, account status, rate limits
        |
        v
Controller -> Service -> Repository -> H2 or MySQL
        |
        v
Flyway schema migrations
```

Why these choices:

- **One deployable process.** Spring Boot serves the API and static files. There is no Node.js runtime or bundler to operate.
- **Fetch API, not server-side templates.** The browser requests JSON and renders it with safe DOM APIs. The same API could serve a mobile client later.
- **Layered packages by feature.** Each business area owns its controller, service, repository, entities, and DTOs.
- **DTOs leave the API.** JPA entities are never serialized directly, so password hashes and internal fields cannot leak.
- **Money uses `BigDecimal`.** Financial values never use floating-point types.
- **Explicit state machines.** Campaign, application, collaboration, deliverable, contract, and payment transitions are validated on the server.
- **Flyway owns the schema.** Hibernate runs with `ddl-auto=validate`; migrations live under `db/migration/h2` and `db/migration/mysql`.

---

## 3. Prerequisites

Install:

1. **JDK 21 (LTS)** from Eclipse Temurin, Microsoft OpenJDK, or Oracle JDK.
2. **VS Code** with the Extension Pack for Java and Spring Boot Extension Pack.
3. No standalone Maven installation is required. The project includes `mvnw` and `mvnw.cmd`.

Verify in PowerShell:

```powershell
java -version
.\mvnw.cmd -version
```

Expected: Java 21 and Maven 3.9.x. If Java is not found, set `JAVA_HOME` and add `%JAVA_HOME%\bin` to `Path`, then reopen the terminal.

Optional tools: Docker Desktop for the MySQL/Compose workflow, and Postman or curl for API testing.

---

## 4. Run the application

### Windows 11 (PowerShell)

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

### macOS or Linux

```bash
./mvnw clean package
./mvnw spring-boot:run
```

Open <http://localhost:8080>. The first build downloads dependencies and can take several minutes.

Alternative after packaging:

```powershell
java -jar target\collabnexus-0.1.0-SNAPSHOT.jar
```

Expected startup output includes:

```text
The following 1 profile is active: "local"
Tomcat started on port 8080 (http)
Started CollabNexusApplication
```

The H2 database is created automatically at `./data/collabnexus.mv.db`. Uploaded supporting files go to `./uploads`. Both directories are git-ignored.

Stop the application with `Ctrl+C`.

### Demonstration accounts

When `CN_DEMO_DATA=true` (the default) and the database is empty, the application creates clearly labelled demonstration data. Password for all three accounts:

```text
Demo!Pass1234
```

| Role | Email |
|------|-------|
| Administrator | `admin@collabnexus.demo` |
| Brand | `brand@collabnexus.demo` |
| Creator | `creator@collabnexus.demo` |

The seed also creates three published campaigns, an in-progress collaboration with deliverables, payments, and messages, plus a completed collaboration with reviews. All sample content is labelled as demonstration data. Set `CN_DEMO_DATA=false` to start with an empty database.

### H2 console (local only)

Open <http://localhost:8080/h2-console>.

```text
JDBC URL: jdbc:h2:file:./data/collabnexus
User Name: sa
Password: (leave blank)
```

The console is disabled by the production and MySQL profiles.

---

## 5. Browser verification checklist

1. The landing page loads with the navy/gold identity and the text "Where brand ambition meets creative influence."
2. Discover pages at `/campaigns.html` and `/creators.html` load sample cards with demonstration labels and working filters.
3. Register a new brand or creator; the session is created and the correct dashboard opens.
4. Log in as `creator@collabnexus.demo` and open **My applications**, **Collaborations**, **Deliverables**, and **Contracts**.
5. Submit a deliverable URL or supporting file; then log in as the brand and request a revision or approve it.
6. Record a payment milestone; the UI states that a record is not proof of an external transfer.
7. Open **Messages** and exchange a message. Open **Notifications** and mark one as read.
8. Complete the demonstration collaboration, then submit a review.
9. Log in as the administrator and inspect **Users**, **Reports & disputes**, **Reviews**, **Audit log**, and **Catalog & settings**.
10. Resize to phone width; navigation, cards, tables, and forms remain usable.
11. `GET /api/health` returns `success: true`, `database: "UP"`, and `phase: "Phase 10 - Production readiness"`.
12. `GET /actuator/health` returns `{"status":"UP"}`.

Public page smoke tests:

```bash
curl -I http://localhost:8080/
curl -I http://localhost:8080/campaigns.html
curl -I http://localhost:8080/creators.html
curl http://localhost:8080/api/public/info
```

---

## 6. API overview

All JSON responses use a consistent envelope: `success`, `data`, `message`, and `timestamp`; errors use `success`, `error`, `message`, `path`, and `timestamp`.

State-changing requests require a session and the CSRF token. Fetch the token from `GET /api/auth/csrf` and send it in the `X-XSRF-TOKEN` header (the browser client does this automatically).

### Authentication and public endpoints

| Method | Path | Access | Purpose |
|--------|------|--------|---------|
| POST | `/api/auth/register` | Public | Register a brand or creator |
| POST | `/api/auth/login` | Public | Start a session |
| POST | `/api/auth/logout` | Authenticated | Invalidate the session |
| GET | `/api/auth/me` | Authenticated | Current user and profile |
| GET | `/api/auth/csrf` | Public | CSRF token |
| GET | `/api/health` | Public | Application and database health |
| GET | `/api/public/info` | Public | Platform metadata |
| GET | `/api/public/categories`, `/api/public/settings` | Public | Public catalog values |
| POST | `/api/public/contact` | Public | Contact/report message with consent |
| GET | `/api/campaigns`, `/api/campaigns/{id}` | Public | Campaign discovery |
| GET | `/api/creators`, `/api/creators/{id}` | Public | Privacy-aware creator directory |
| GET | `/api/reviews/creator/{id}` and `/summary` | Public | Published creator reviews |
| GET | `/actuator/health`, `/actuator/info` | Public | Operations endpoints |

### Brand endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET/PUT | `/api/brand/profile` | Company profile |
| GET/POST | `/api/brand/campaigns` | List and create campaigns |
| GET/PUT | `/api/brand/campaigns/{id}` | Read and edit drafts |
| POST | `/api/brand/campaigns/{id}/publish|pause|close` | Lifecycle transitions |
| GET | `/api/brand/applications` | Application queue with filters |
| POST | `/api/brand/applications/{id}/shortlist|accept|reject` | Review actions |
| GET | `/api/collaborations`, `/api/collaborations/{id}` | Collaboration list/detail |
| POST | `/api/brand/collaborations/{id}/deliverables` | Create a deliverable |
| POST | `/api/brand/deliverables/{id}/approve|request-revision|reject` | Review a submission |
| PUT/POST | `/api/brand/contracts/{id}`, `/{id}/send` | Edit terms and send for acknowledgement |
| POST | `/api/brand/collaborations/{id}/payments` | Record a payment milestone |
| GET/POST | `/api/brand/analytics/overview`, `/api/brand/campaigns/{id}/metrics` | Analytics and manual results |

### Creator endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET/PUT | `/api/creator/profile` | Creator profile |
| GET/POST/PUT/DELETE | `/api/creator/social-channels`, `/api/creator/portfolio-items` | Profile assets |
| GET/POST/DELETE | `/api/creator/saved-campaigns` | Campaign shortlist |
| POST | `/api/campaigns/{id}/applications` | Apply before the deadline |
| GET | `/api/creator/applications` | Application history |
| POST | `/api/creator/applications/{id}/withdraw` | Withdraw when allowed |
| GET | `/api/collaborations`, `/api/collaborations/{id}` | Collaboration workspace |
| POST | `/api/deliverables/{id}/start` | Start work |
| POST | `/api/deliverables/{id}/submissions` | Multipart submission (URL, notes, optional file) |
| GET | `/api/deliverables/{id}/submissions/{submissionId}/file` | Authorised file download |
| GET | `/api/creator/analytics/overview` | Creator metrics |
| GET | `/api/payments`, `/api/payments/summary` | Recorded earnings |

### Shared collaboration, messaging, reviews

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/collaborations`, `/api/collaborations/{id}/deliverables` | Participant-scoped data |
| POST | `/api/collaborations/{id}/start|complete|cancel` | Validated transitions |
| GET | `/api/contracts`, `/api/contracts/{id}` | Contract list and printable detail |
| POST | `/api/contracts/{id}/acknowledge` | Record participant acknowledgement |
| GET | `/api/conversations`, `/{id}/messages` | Conversations and messages |
| POST | `/api/conversations`, `/{id}/messages`, `/{id}/read` | Create/send/read |
| GET | `/api/notifications`, `/api/notifications/unread-count` | In-app notifications |
| POST | `/api/notifications/{id}/read`, `/read-all` | Mark read |
| POST | `/api/collaborations/{id}/reviews` | Review an eligible completed collaboration |
| GET | `/api/reviews/mine`, `/api/reviews/received` | Own and received reviews |
| POST | `/api/reports` | Report content or behaviour |

### Administrator endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/admin/overview`, `/api/admin/analytics/overview` | Platform statistics |
| GET | `/api/admin/users` | User search and filtering |
| POST | `/api/admin/users/{id}/suspend|activate` | Account status |
| GET | `/api/admin/campaigns` | Campaign oversight |
| POST | `/api/admin/campaigns/{id}/close` | Administrative close |
| GET | `/api/admin/reports` | Moderation queue |
| POST | `/api/admin/reports/{id}/resolve` | Resolve with notes |
| GET | `/api/admin/reviews` | Review moderation queue |
| POST | `/api/admin/reviews/{id}/status` | Publish, hide, or flag |
| GET | `/api/admin/audit-logs` | Audit history |
| GET/POST/PUT | `/api/admin/categories`, `/api/admin/settings` | Catalog configuration |
| GET/POST | `/api/admin/contact-messages` | Contact queue |

List endpoints accept `page` and `size`; most add status, search, and domain filters. Response data uses `content`, `page`, `size`, `totalElements`, and `totalPages`.

---

## 7. Technology stack

| Layer | Choice |
|-------|--------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 |
| Build | Maven Wrapper (Maven 3.9.x) |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA, Hibernate |
| Security | Spring Security 6, BCrypt, session cookies, CSRF |
| Validation | Jakarta Bean Validation |
| Database | H2 file database (local), MySQL 8.4 (profile) |
| Migrations | Flyway |
| Frontend | HTML5, CSS3, ES6+ JavaScript, Fetch API, Chart.js (bundled locally) |
| Testing | JUnit 5, Spring Boot Test, MockMvc, Spring Security Test |
| Operations | Spring Boot Actuator, Docker, Docker Compose |

---

## 8. Configuration

Main file: `src/main/resources/application.properties`.

| Variable | Default | Meaning |
|----------|---------|---------|
| `CN_PROFILE` | `local` | Active profile: `local`, `prod`, or `mysql` |
| `SERVER_PORT` | `8080` | HTTP port |
| `CN_DATA_DIR` | `./data` | H2 database directory |
| `CN_DEMO_DATA` | `true` | Create demonstration data on an empty database |
| `CN_RATE_LIMIT` | `true` | Enable authentication rate limiting |
| `CN_UPLOAD_DIR` | `./uploads` | Supporting-file storage directory |
| `CN_UPLOAD_MAX_FILE_SIZE` | `10MB` | Per-file limit |
| `CN_UPLOAD_MAX_REQUEST_SIZE` | `12MB` | Request limit |
| `MYSQL_HOST`, `MYSQL_PORT` | `localhost`, `3306` | MySQL connection (mysql profile) |
| `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD` | none | MySQL credentials (mysql profile) |

`.env.example` documents every variable. Spring Boot does not read `.env` automatically; export the variables in your shell, IDE run configuration, Docker Compose, or hosting platform. Never commit real credentials.

Example:

```powershell
$env:SERVER_PORT = "8081"
$env:CN_DATA_DIR = "D:\collabnexus-data"
.\mvnw.cmd spring-boot:run
```

---

## 9. Tests

Run the full suite:

```powershell
.\mvnw.cmd test
```

Expected result:

```text
Tests run: 64, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Coverage includes:

- Context startup, health, Actuator, public pages, and JSON error envelopes.
- Registration, login, logout, suspended accounts, validation, and rate-limit behaviour.
- Role authorization and ownership isolation between brands and creators.
- Campaign lifecycle, discovery filters, budget/date validation, and pagination.
- Duplicate applications, invalid transitions, capacity limits, and concurrent acceptance.
- Deliverable submissions, optional files, revisions, approvals, deadlines, and overdue lists.
- Contract versioning, acknowledgement, locking, and printable summaries.
- Payment transitions, participant scope, summaries, and idempotency checks.
- Messaging scope, unread counts, notification flows, review eligibility, moderation, reports, and audit logs.
- `FullWorkflowApiTest` executes the brief's twelve-step journey: register brand, publish campaign, register creator, apply, accept, create collaboration, acknowledge contract, submit deliverable, request/resolve revision, approve, record payment, complete collaboration, and submit eligible reviews.

Tests use an in-memory H2 database and a temporary upload directory, so they never modify `data/` or `uploads/`.

---

## 10. Docker and MySQL

### MySQL profile on its own

```bash
docker compose up -d db
export CN_PROFILE=mysql
export MYSQL_HOST=localhost
export MYSQL_DATABASE=collabnexus
export MYSQL_USER=collabnexus
export MYSQL_PASSWORD='replace-me'
export CN_DEMO_DATA=true
./mvnw spring-boot:run
```

The MySQL profile uses `db/migration/mysql` and validates the schema on startup.

### Full stack with Docker Compose

Create `.env` from `.env.example`, set strong MySQL passwords, then:

```bash
docker compose up --build
```

- The application is available at <http://localhost:8080>.
- MySQL data is stored in the `mysql-data` volume.
- Supporting uploads are stored in the `uploads-data` volume.
- The database health check gates application startup.
- The application health check calls `/api/health`.

Stop the stack with `docker compose down` (add `-v` only when you intentionally want to delete the data volumes).

---

## 11. Production deployment checklist

1. **Provision a managed MySQL 8 database** and a JVM host or container platform.
2. **Set secrets as environment variables**: `CN_PROFILE=mysql`, `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `CN_DEMO_DATA=false`, and a strong database password. Never bake them into the image.
3. **Use HTTPS.** Terminate TLS at a reverse proxy or load balancer and forward to the application. The `prod` profile marks session cookies `Secure`, so plain HTTP will not keep a login session.
4. **Persist `/app/uploads`** on a volume or object storage adapter; do not rely on the container filesystem.
5. **Restrict Actuator exposure** to health and info (the default) and protect the management port at the network layer.
6. **Run database migrations** through Flyway on startup. Back up the database before the first production migration.
7. **Set resource limits** and review the JVM options in `Dockerfile` (`MaxRAMPercentage=75`, container support enabled).
8. **Confirm the health endpoint** `GET /api/health` reports `UP` for both application and database.
9. **Enable log aggregation** and alerting. Application logs never include passwords, tokens, or full payment credentials.
10. **Disable demonstration data** and review seeded/manual data before opening the site to real users.
11. **Retain audit logs** and define a data-retention policy for contact, report, and review content.
12. **Re-run `./mvnw test`** on the release commit and record the result.

The application is a production-shaped prototype, not a certified financial or legal system. See the limitations below before handling real money or personal data.

---

## 12. Project structure

```text
collabnexus/
  pom.xml
  mvnw, mvnw.cmd, .mvn/
  Dockerfile, docker-compose.yml, .dockerignore
  .env.example, .gitignore
  README.md
  src/main/java/com/collabnexus/
    CollabNexusApplication.java
    admin/            administration, categories, settings
    analytics/        dashboards and manual campaign metrics
    applications/     applications, shortlisting, acceptance
    auth/             registration, login, logout, rate limiting
    brands/           brand profiles
    campaigns/        campaign lifecycle and discovery
    collaborations/   collaboration records and transitions
    common/           response envelope, errors, paging, URL validation
    config/           security configuration and demo seeders
    contracts/        contracts, versions, acknowledgements
    creators/         profiles, channels, portfolio, saved campaigns
    deliverables/     deliverables, submissions, reviews, file storage
    messaging/        conversations and messages
    moderation/       reports, suspension, audit log
    notifications/    in-app notifications
    payments/         payment milestones and summaries
    publicapi/        public info and catalog endpoints
    reviews/          reviews and moderation
    security/         filters, handlers, user details
    support/          contact messages
    users/            user entity, roles, status
  src/main/resources/
    application*.properties
    db/migration/h2/  Flyway migrations (baseline + V2-V5)
    db/migration/mysql/
    static/           public pages, dashboard, CSS, JS, bundled Chart.js
  src/test/java/com/collabnexus/
    AbstractApiTest.java
    FullWorkflowApiTest.java
    ...domain integration tests
```

---

## 13. Deliberate limitations

These items are explicitly out of scope for the first version and are not simulated as if they were real:

- **No payment gateway.** Payment records are manual bookkeeping only. The UI and API state that no money is transferred.
- **No e-signature provider.** Contract acknowledgement records a timestamped acceptance event; it is not a legally binding electronic signature in every jurisdiction.
- **No email or SMS delivery.** Notifications are in-app. SMTP settings can be added later without changing notification records.
- **No password-reset email flow.** It requires a mail provider and token-delivery configuration; do not enable a fake reset endpoint.
- **No optional AGENCY role.** The schema and role checks currently support ADMIN, BRAND, and CREATOR. Agency support would add an agency profile, client links, and delegated permissions.
- **No social-platform API integrations.** Audience figures and campaign results are sample/manual data, clearly labelled. The system never claims that unverified metrics are verified.
- **No external file/object storage.** Local disk storage is used behind a service boundary so it can be replaced with S3-compatible storage.
- **No real-time messaging.** Messages are persisted and fetched through paginated REST calls.

---

## 14. Common problems and fixes

| Symptom | Fix |
|---------|-----|
| `JAVA_HOME` is not defined or `java` is not found | Install JDK 21 and point `JAVA_HOME` at it; reopen the terminal. |
| `Port 8080 was already in use` | Set `SERVER_PORT` to another port or stop the other process. |
| `Table ... not found` on startup | Do not delete migration files; let Flyway run, or recreate the local data directory during development. |
| Login works but the session disappears | In the `prod` profile, cookies are `Secure`; use HTTPS or the `local`/`mysql` profile for local HTTP. |
| CSRF `403` on an API call | Fetch `/api/auth/csrf` and send the `X-XSRF-TOKEN` header with the session cookie. |
| Database file locked | Only one application instance may open the file-based H2 database at a time. |
| Upload rejected | Check the file type and size; the defaults are 10 MB per file and 12 MB per request. |
| Docker app exits while MySQL starts | Wait for the database health check, or inspect `docker compose logs db`. |
| Tests fail after changing entities | Add a new Flyway migration and keep `ddl-auto=validate`; never rely on schema auto-update. |

---

## 15. Git workflow

```powershell
git status
git add .
git commit -m "feat: complete the CollabNexus collaboration workflow"
git push
```

Do not commit `data/`, `uploads/`, `.env`, logs, or `target/`; they are already ignored.
