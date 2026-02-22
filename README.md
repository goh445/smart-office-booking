# Smart Office Booking
<img width="2513" height="1425" alt="image" src="https://github.com/user-attachments/assets/7e4a2c84-d1b6-4a98-bdda-e4b4fad41422" />
<img width="2487" height="1410" alt="image" src="https://github.com/user-attachments/assets/dd4c954a-8c0a-4a43-bdd2-bf50256bbd88" />
Smart Office Booking is a Spring Boot web application for office resource booking, approvals, analytics, and AI-assisted workflows.

## 1) Language, Stack, and Architecture

- **Backend**
  - Java + Spring Boot (MVC + REST)
  - Spring Security (session-based login)
  - Spring Data JPA + Hibernate
  - H2 database (file mode for local runtime, in-memory for tests)
- **Frontend**
  - Thymeleaf server-rendered templates
  - Vanilla JavaScript for page interactions and API calls
  - Shared CSS theme (`app.css`)
- **AI**
  - Spring AI + Google GenAI integration
  - AI parse/recommend/QA + parse-preview + parse-and-submit booking flow

### Architecture Style

- **Modular monolith**
  - `config` for security/bootstrap
  - `domain` for entities/enums
  - `repository` for persistence
  - `service` for business logic
  - `api` for REST endpoints
  - `web` for page routing (Thymeleaf)

---

## 2) Core Business Features

- User login/logout and profile role resolution
- Registration request + admin approval
- Resource listing with filtering and pagination
- Booking creation, availability checks, and personal booking views
- Approval queue for bookings that hit policy thresholds
- Dashboard analytics:
  - Utilization heatmap by floor/hour
  - Peak-load early warning for next week
- AI assistant:
  - Natural-language booking parsing
  - Recommendation mode
  - General office assistant Q&A
  - Parse preview + manual confirm, or auto submit booking

---

## 3) Environment Setup

## Prerequisites

- JDK (recommended **17+** for Spring Boot 3.x projects)
- Maven Wrapper (already included)

## Required Environment Variables

- `GOOGLE_GENAI_API_KEY` (required for real AI calls)
- Optional:
  - `GOOGLE_GENAI_PROJECT_ID`
  - `GOOGLE_GENAI_LOCATION`

> Recommended: do **not** keep real keys in source-controlled properties.

## Application Configuration

Main runtime config: `src/main/resources/application.properties`
- App port: `8080`
- H2 file DB: `jdbc:h2:file:./data/smartoffice;AUTO_SERVER=TRUE`
- H2 console: `/h2-console`
- Approval thresholds:
  - `app.approval.duration-hours-threshold`
  - `app.approval.capacity-threshold`

Test config: `src/test/resources/application.properties`
- In-memory H2
- AI autoconfiguration excluded for tests

---

## 4) Run Locally

## Windows

```bash
.\mvnw.cmd spring-boot:run
```

## macOS / Linux

```bash
./mvnw spring-boot:run
```

App URL: `http://localhost:8080`

---

## 5) Build and Test

## Run tests

```bash
./mvnw test
```

(Windows: `.\mvnw.cmd test`)

## Build artifact

```bash
./mvnw clean package
```

---

## 6) Default Seed Data

On startup, seed data creates:
- Roles: `ADMIN`, `APPROVER`, `EMPLOYEE`
- Sample users: `admin`, `approver`, `employee`
- Sample resources (meeting rooms, desks, devices)

See `DataSeeder` for exact records.

---

## 7) Main Pages

- `/dashboard` – navigation, resource list, analytics
- `/bookings/create` – create booking
- `/bookings` – my bookings
- `/ai` – AI assistant
- `/approvals` – approval queue (APPROVER/ADMIN)
- `/admin/registrations` – registration approvals (ADMIN)

---

## 8) API Overview

See full endpoint list in `API_ENDPOINTS.txt`, including:
- Auth
- Registration + admin review
- Resources
- Bookings
- Approvals
- Analytics
- AI endpoints (`/api/ai/*`)

---

## 9) Security Notes

- Session-based authentication with route-level authorization
- Public routes include login/register/static assets/H2 console
- Admin/Approver APIs restricted by role
- CSRF currently disabled in the configured filter chain (review before production)
