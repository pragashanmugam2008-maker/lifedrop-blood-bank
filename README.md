# 🩸 Blood Bank Management System - Backend (Java Spring Boot 3)

An enterprise-grade, secure, and production-ready **Blood Bank Management System Backend** developed with **Java 21**, **Spring Boot 3**, **Spring Data JPA**, **Spring Security (JWT + RBAC)**, and **SpringDoc OpenAPI (Swagger UI)**.

---

## 🚀 Key Features

1. **🔐 Authentication & RBAC**:
   - JWT stateless authentication with Bearer tokens.
   - Role-Based Access Control: `ROLE_ADMIN`, `ROLE_STAFF`, `ROLE_DONOR`, `ROLE_HOSPITAL`.
   - Passwords securely hashed with `BCryptPasswordEncoder`.

2. **🩸 Blood Inventory & Batch Tracking**:
   - Full support for all 8 blood groups: `A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, `O+`, `O-`.
   - Component management: *Whole Blood (35 days)*, *PRBC (42 days)*, *Platelets (5 days)*, *Fresh Frozen Plasma (365 days)*, and *Cryoprecipitate*.
   - Automated shelf life and expiration calculations.
   - Storage locations tracking (Refrigerator ID, Shelf ID).

3. **🧬 Automated ABO/Rh Cross-Match Compatibility Engine**:
   - Biologically accurate red blood cell compatibility ($O^-$ universal donor, $AB^+$ universal recipient).
   - Biologically accurate plasma compatibility ($AB$ universal plasma donor, $O$ universal recipient).
   - Automated stock allocation matching FIFO (nearest expiry first).

4. **🧑‍🤝‍🧑 Donor Management & Screening**:
   - Medical screening checklists: Hemoglobin, Blood Pressure, Pulse, Weight.
   - Infectious disease screening (HIV, Hepatitis B, Hepatitis C, Syphilis, Malaria).
   - Automatic eligibility engine (18-65 age validation, 50kg+ weight, 90-day cooldown period).

5. **🏥 Blood Requests & Emergency Dispatches**:
   - Hospital and recipient blood requests with urgency levels: `ROUTINE`, `URGENT`, `CRITICAL_EMERGENCY`.
   - Automated inventory reservation, transfusion fulfillment, and cancellation releasing.

6. **⛺ Blood Donation Drives & Camps**:
   - Schedule and manage blood donation camps across cities with target unit tracking and pre-registration.

7. **🚨 Emergency Shortage Alerts**:
   - Automated and manual emergency broadcasts alerting matching eligible donors when stock levels fall critically low.

8. **📊 Real-time Dashboard Analytics & Audit Logs**:
   - Real-time stock counts by group and component, pending critical requests, and audit trail of every unit transaction.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 LTS |
| **Framework** | Spring Boot 3.3.4 |
| **Database** | Embedded H2 Database (with H2 Web Console), easily switchable to MySQL/PostgreSQL |
| **ORM** | Spring Data JPA / Hibernate |
| **Security** | Spring Security 6, JJWT 0.12.6, BCrypt |
| **API Docs** | SpringDoc OpenAPI 3 / Swagger UI 2.6.0 |
| **Build Tool** | Apache Maven 3.9.x |

---

## ⚙️ Quick Start & Running Locally

### 1. Prerequisites
- Java 21 LTS installed and configured (`JAVA_HOME`).
- Maven installed (or use `mvn`).

### 2. Run the Application
```bash
# Build and run with Maven
mvn spring-boot:run
```

The server will start on: **`http://localhost:8080`**

---

## 📖 API Documentation & Web Consoles

- **Interactive Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **H2 Database Web Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - JDBC URL: `jdbc:h2:mem:bloodbankdb`
  - Username: `sa`
  - Password: *(leave blank)*

---

## 🔑 Default Seed Accounts

The system automatically initializes default accounts for testing upon startup:

| Role | Email | Password | Purpose |
|---|---|---|---|
| **Admin** | `admin@bloodbank.org` | `Admin@123` | Full administrative control & audit logs |
| **Staff** | `staff@bloodbank.org` | `Staff@123` | Record donations, manage inventory & dispatches |
| **Hospital** | `hospital@citycare.org` | `Hospital@123` | Request blood units & track order statuses |
| **Donor** | `john.doe@gmail.com` | `Donor@123` | Registered $O^-$ donor profile |
| **Donor** | `emily.watson@gmail.com` | `Donor@123` | Registered $A^+$ donor profile |

---

## 📡 Core API Endpoints

### 1. Authentication (`/api/v1/auth`)
- `POST /api/v1/auth/login`: Authenticate and obtain JWT token.
- `POST /api/v1/auth/register`: Register new user (`ADMIN`, `STAFF`, `DONOR`, `HOSPITAL`).
- `GET /api/v1/auth/me`: Get current authenticated user details.

### 2. Blood Inventory (`/api/v1/inventory`)
- `GET /api/v1/inventory`: List all blood units with filter options.
- `GET /api/v1/inventory/{id}`: Get specific unit details.
- `GET /api/v1/inventory/barcode/{barcode}`: Look up unit by barcode / bag ID.
- `GET /api/v1/inventory/summary`: Real-time stock counts by blood group & component.
- `GET /api/v1/inventory/expiring?days=7`: Get units expiring within $N$ days.
- `POST /api/v1/inventory`: Add a unit to stock *(Staff/Admin)*.
- `PATCH /api/v1/inventory/{id}/status`: Update unit status *(AVAILABLE, RESERVED, TRANSFUSED, EXPIRED, etc.)*.
- `POST /api/v1/inventory/expire-check`: Auto-scan and mark expired units.

### 3. Blood Compatibility Engine (`/api/v1/compatibility`)
- `GET /api/v1/compatibility/check?bloodGroup=A_POSITIVE&component=WHOLE_BLOOD&unitsRequired=2`: Check cross-matching and find available inventory units.

### 4. Donors & Donations (`/api/v1/donors`, `/api/v1/donations`)
- `GET /api/v1/donors`: List donors with eligibility status.
- `POST /api/v1/donors/profile`: Create or update donor profile.
- `POST /api/v1/donations`: Log a donation (records vitals, tests, and auto-adds tested unit to inventory).
- `GET /api/v1/donations/donor/{donorId}`: View donation history for a donor.

### 5. Blood Requests (`/api/v1/requests`)
- `POST /api/v1/requests`: Submit blood request (supports auto-allocation & emergency alert creation).
- `GET /api/v1/requests`: List requests with status and urgency filters.
- `POST /api/v1/requests/{id}/allocate`: Auto-allocate compatible units.
- `POST /api/v1/requests/{id}/fulfill`: Mark request fulfilled and transfuse reserved units.
- `POST /api/v1/requests/{id}/cancel`: Cancel request and release reserved units back to inventory.

### 6. Donation Camps (`/api/v1/camps`)
- `GET /api/v1/camps`: List all donation drives.
- `GET /api/v1/camps/upcoming`: List upcoming camps.
- `POST /api/v1/camps`: Schedule a new donation drive *(Staff/Admin)*.
- `POST /api/v1/camps/{id}/register`: Donor pre-registration for camp.

### 7. Emergency Alerts (`/api/v1/alerts`)
- `GET /api/v1/alerts`: List active shortage alerts.
- `POST /api/v1/alerts`: Broadcast emergency alert *(Staff/Admin)*.
- `PATCH /api/v1/alerts/{id}/deactivate`: Deactivate alert.

### 8. Analytics & Audit Logs (`/api/v1/analytics`)
- `GET /api/v1/analytics/dashboard`: System summary metrics.
- `GET /api/v1/analytics/audit-logs`: Audit logs of stock movements *(Admin only)*.

---

## 🗄️ Switching to MySQL or PostgreSQL

To use MySQL or PostgreSQL instead of the default embedded H2 database, update `src/main/resources/application.properties`:

### For MySQL:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bloodbank_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_mysql_password
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
```

### For PostgreSQL:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bloodbank_db
spring.datasource.username=postgres
spring.datasource.password=your_postgres_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
```

---

## 🧪 Testing

Run automated tests:
```bash
mvn test
```
