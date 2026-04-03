# 👥 USERS-SERVICE - Servicio de Gestión de Perfiles y Empleados

## 📋 Descripción General

El **USERS-SERVICE** es el microservicio encargado de gestionar los perfiles de usuario y el sistema de empleados de PackedGo. Complementa la autenticación del AUTH-SERVICE con información personal completa, gestiona empleados asignados a eventos, y actúa como proxy para las operaciones de validación de tickets y consumiciones realizadas por empleados.

### 🎯 Características Principales

- 👤 **Gestión completa de perfiles de usuario** con datos personales y demográficos
- 🔄 **Integración automática** con AUTH-SERVICE para creación de perfiles
- 👷 **Sistema de gestión de empleados** para administradores
- 📱 **Proxy de validación** de tickets y consumiciones hacia event-service
- 🗑️ **Soft delete** para preservación de datos históricos
- 🔍 **Consultas optimizadas** por estado activo
- 🔐 **WebClient configurado** con 5MB buffer para comunicación con event-service
- 📊 **Estadísticas de empleados** sobre validaciones y consumos registrados

---

## 🚀 Configuración de Servicio

| Propiedad | Valor |
|-----------|-------|
| **Puerto HTTP** | 8082 |
| **Puerto Debug (JDWP)** | 5006 |
| **Context Path** | /api |
| **Base URL** | http://localhost:8082/api |

---

## 📦 Base de Datos

### Configuración PostgreSQL

| Propiedad | Valor |
|-----------|-------|
| **Nombre** | users_db |
| **Puerto** | 5434 → 5432 (Docker) |
| **Usuario** | users_user |
| **Contraseña** | users_password |
| **Imagen Docker** | postgres:15-alpine |
| **Timezone** | America/Argentina/Buenos_Aires |

### 📊 Tablas Principales

#### `user_profiles`
```sql
CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    auth_user_id BIGINT UNIQUE NOT NULL, -- ID del usuario en auth-service
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document VARCHAR(20) UNIQUE NOT NULL,
    phone VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(100),
    province VARCHAR(100),
    country VARCHAR(100) DEFAULT 'Argentina',
    birth_date DATE,
    gender VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `employees`
```sql
CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    auth_user_id BIGINT NOT NULL, -- ID del empleado en auth-service
    admin_id BIGINT NOT NULL, -- ID del admin que lo creó
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    position VARCHAR(100), -- Ej: "Scanner", "Barra", "Seguridad"
    is_active BOOLEAN DEFAULT TRUE,
    assigned_events JSON, -- Array de IDs de eventos asignados: [1, 2, 3]
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(auth_user_id, admin_id)
);
```

---

## 🛠 Tecnologías y Dependencias

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Java** | 17 | Lenguaje base |
| **Spring Boot** | 3.5.6 | Framework principal |
| **Spring Data JPA** | 3.5.6 | Persistencia de datos |
| **Spring Security** | 3.5.6 | Seguridad y autenticación |
| **Spring WebFlux** | 3.5.6 | Cliente HTTP reactivo (WebClient) |
| **Spring Validation** | 3.5.6 | Validación de datos |
| **Spring Actuator** | 3.5.6 | Monitoreo y métricas |
| **ModelMapper** | 3.1.1 | Mapeo DTOs ↔ Entidades |
| **PostgreSQL Driver** | 42.x | Driver JDBC |
| **Lombok** | Latest | Reducción de boilerplate |
| **H2** | Latest | Base de datos en memoria para tests |

---

## 📡 API Endpoints

### 👤 Gestión de Perfiles de Usuario (`/api/user-profiles`)

#### **POST** `/api/user-profiles`
Crear nuevo perfil de usuario (uso interno).

```http
POST /api/user-profiles
Content-Type: application/json
Authorization: Bearer {token}

{
  "authUserId": 123,
  "firstName": "Juan",
  "lastName": "Pérez",
  "document": "12345678",
  "phone": "+54911123456",
  "address": "Av. Corrientes 1234",
  "city": "Buenos Aires",
  "province": "CABA",
  "country": "Argentina",
  "birthDate": "1990-05-15",
  "gender": "MALE"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "authUserId": 123,
  "firstName": "Juan",
  "lastName": "Pérez",
  "document": "12345678",
  "phone": "+54911123456",
  "address": "Av. Corrientes 1234",
  "city": "Buenos Aires",
  "province": "CABA",
  "country": "Argentina",
  "birthDate": "1990-05-15",
  "gender": "MALE",
  "isActive": true,
  "createdAt": "2025-12-14T10:00:00",
  "updatedAt": "2025-12-14T10:00:00"
}
```

---

#### **POST** `/api/user-profiles/from-auth`
Crear perfil desde auth-service (endpoint especializado para integración).

```http
POST /api/user-profiles/from-auth
Content-Type: application/json

{
  "authUserId": 123,
  "firstName": "Juan",
  "lastName": "Pérez",
  "document": "12345678",
  "email": "juan@example.com"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "authUserId": 123,
  "firstName": "Juan",
  "lastName": "Pérez",
  "document": "12345678",
  "isActive": true,
  "createdAt": "2025-12-14T10:00:00"
}
```

---

#### **GET** `/api/user-profiles/{authUserId}`
Obtener perfil por authUserId (valida ownership o rol ADMIN).

```http
GET /api/user-profiles/123
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "authUserId": 123,
  "firstName": "Juan",
  "lastName": "Pérez",
  "document": "12345678",
  "phone": "+54911123456",
  "address": "Av. Corrientes 1234",
  "city": "Buenos Aires",
  "province": "CABA",
  "country": "Argentina",
  "birthDate": "1990-05-15",
  "gender": "MALE",
  "isActive": true
}
```

---

#### **GET** `/api/user-profiles/my-profile`
Obtener perfil del usuario autenticado.

```http
GET /api/user-profiles/my-profile
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "authUserId": 123,
  "firstName": "Juan",
  "lastName": "Pérez",
  "document": "12345678",
  "phone": "+54911123456",
  "isActive": true
}
```

---

#### **GET** `/api/user-profiles`
Obtener todos los perfiles (solo ADMIN).

```http
GET /api/user-profiles
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "authUserId": 123,
    "firstName": "Juan",
    "lastName": "Pérez",
    "document": "12345678",
    "isActive": true
  },
  {
    "id": 2,
    "authUserId": 456,
    "firstName": "María",
    "lastName": "González",
    "document": "87654321",
    "isActive": true
  }
]
```

---

#### **PUT** `/api/user-profiles/{authUserId}`
Actualizar perfil (valida ownership).

```http
PUT /api/user-profiles/123
Authorization: Bearer {token}
Content-Type: application/json

{
  "firstName": "Juan Carlos",
  "lastName": "Pérez García",
  "phone": "+54911999888",
  "address": "Av. Santa Fe 5678",
  "city": "Buenos Aires"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "authUserId": 123,
  "firstName": "Juan Carlos",
  "lastName": "Pérez García",
  "phone": "+54911999888",
  "address": "Av. Santa Fe 5678",
  "city": "Buenos Aires",
  "isActive": true,
  "updatedAt": "2025-12-14T11:00:00"
}
```

---

#### **DELETE** `/api/user-profiles/{authUserId}`
Eliminar perfil físicamente (valida ownership).

```http
DELETE /api/user-profiles/123
Authorization: Bearer {token}
```

**Response 204 NO CONTENT**

---

#### **DELETE** `/api/user-profiles/logical/{id}`
Eliminar perfil lógicamente (soft delete).

```http
DELETE /api/user-profiles/logical/1
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "authUserId": 123,
  "firstName": "Juan",
  "lastName": "Pérez",
  "isActive": false,
  "updatedAt": "2025-12-14T11:30:00"
}
```

---

#### **GET** `/api/user-profiles/active`
Obtener todos los perfiles activos.

```http
GET /api/user-profiles/active
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "authUserId": 123,
    "firstName": "Juan",
    "lastName": "Pérez",
    "isActive": true
  }
]
```

---

#### **GET** `/api/user-profiles/active/{id}`
Obtener perfil activo por ID.

```http
GET /api/user-profiles/active/1
Authorization: Bearer {token}
```

---

#### **GET** `/api/user-profiles/active/document/{document}`
Obtener perfil activo por documento.

```http
GET /api/user-profiles/active/document/12345678
Authorization: Bearer {token}
```

---

### 👔 Gestión de Empleados - Admin (`/api/admin/employees`)

#### **POST** `/api/admin/employees`
Crear nuevo empleado.

```http
POST /api/admin/employees
Authorization: Bearer {token}
Content-Type: application/json

{
  "authUserId": 789,
  "firstName": "Carlos",
  "lastName": "Rodríguez",
  "email": "carlos@packedgo.com",
  "phone": "+54911555444",
  "position": "Scanner",
  "assignedEvents": [1, 3, 5]
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "authUserId": 789,
  "adminId": 456,
  "firstName": "Carlos",
  "lastName": "Rodríguez",
  "email": "carlos@packedgo.com",
  "phone": "+54911555444",
  "position": "Scanner",
  "assignedEvents": [1, 3, 5],
  "isActive": true,
  "createdAt": "2025-12-14T10:00:00"
}
```

---

#### **GET** `/api/admin/employees`
Listar empleados del admin.

```http
GET /api/admin/employees
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "authUserId": 789,
    "adminId": 456,
    "firstName": "Carlos",
    "lastName": "Rodríguez",
    "email": "carlos@packedgo.com",
    "phone": "+54911555444",
    "position": "Scanner",
    "assignedEvents": [1, 3, 5],
    "isActive": true
  }
]
```

---

#### **GET** `/api/admin/employees/{id}`
Obtener empleado por ID.

```http
GET /api/admin/employees/1
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "authUserId": 789,
  "adminId": 456,
  "firstName": "Carlos",
  "lastName": "Rodríguez",
  "email": "carlos@packedgo.com",
  "position": "Scanner",
  "assignedEvents": [1, 3, 5],
  "isActive": true
}
```

---

#### **PUT** `/api/admin/employees/{id}`
Actualizar empleado.

```http
PUT /api/admin/employees/1
Authorization: Bearer {token}
Content-Type: application/json

{
  "firstName": "Carlos Alberto",
  "lastName": "Rodríguez",
  "phone": "+54911666555",
  "position": "Supervisor",
  "assignedEvents": [1, 2, 3, 4]
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "authUserId": 789,
  "adminId": 456,
  "firstName": "Carlos Alberto",
  "lastName": "Rodríguez",
  "phone": "+54911666555",
  "position": "Supervisor",
  "assignedEvents": [1, 2, 3, 4],
  "isActive": true,
  "updatedAt": "2025-12-14T11:00:00"
}
```

---

#### **DELETE** `/api/admin/employees/{id}`
Eliminar empleado.

```http
DELETE /api/admin/employees/1
Authorization: Bearer {token}
```

**Response 204 NO CONTENT**

---

#### **PATCH** `/api/admin/employees/{id}/toggle-status`
Activar/Desactivar empleado.

```http
PATCH /api/admin/employees/1/toggle-status
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "authUserId": 789,
  "firstName": "Carlos",
  "lastName": "Rodríguez",
  "isActive": false,
  "updatedAt": "2025-12-14T11:30:00"
}
```

---

### 👨‍💼 Operaciones de Empleado (`/api/employee`)

#### **GET** `/api/employee/assigned-events`
Obtener eventos asignados al empleado autenticado.

```http
GET /api/employee/assigned-events
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "name": "Fiesta de Año Nuevo 2025",
    "location": "Club Central",
    "startDate": "2025-12-31T22:00:00",
    "endDate": "2026-01-01T06:00:00",
    "imageUrl": "/api/event-service/event/1/image",
    "createdBy": 456
  },
  {
    "id": 3,
    "name": "Concierto Rock",
    "location": "Estadio Luna Park",
    "startDate": "2025-12-20T20:00:00",
    "endDate": "2025-12-21T01:00:00",
    "imageUrl": "/api/event-service/event/3/image",
    "createdBy": 456
  }
]
```

---

#### **POST** `/api/employee/validate-ticket`
Validar ticket de entrada (proxy a event-service).

```http
POST /api/employee/validate-ticket
Authorization: Bearer {token}
Content-Type: application/json

{
  "qrCode": "TICKET-123456-ABC",
  "eventId": 1
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Ticket validado correctamente",
  "ticketId": 123,
  "eventName": "Fiesta de Año Nuevo 2025",
  "customerName": "Juan Pérez",
  "validatedAt": "2025-12-31T22:15:00"
}
```

---

#### **POST** `/api/employee/register-consumption`
Registrar consumo (proxy a event-service).

```http
POST /api/employee/register-consumption
Authorization: Bearer {token}
Content-Type: application/json

{
  "qrCode": "TICKET-123456-ABC",
  "eventId": 1,
  "consumptionId": 5,
  "quantity": 2
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Consumo registrado correctamente",
  "ticketId": 123,
  "consumptionName": "Cerveza Quilmes",
  "quantity": 2,
  "remainingQuantity": 3,
  "registeredAt": "2025-12-31T23:00:00"
}
```

---

#### **GET** `/api/employee/stats`
Obtener estadísticas del empleado.

```http
GET /api/employee/stats
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "totalTicketsValidated": 150,
  "totalConsumptionsRegistered": 320,
  "eventsWorked": 12,
  "lastActivity": "2025-12-14T11:00:00"
}
```

---

### 🔧 API Interna (`/api/internal/employees`)

#### **GET** `/api/internal/employees/validate/{authUserId}`
Validar si un usuario es empleado (usado internamente por otros servicios).

```http
GET /api/internal/employees/validate/789
```

**Response 200 OK:**
```json
{
  "isEmployee": true,
  "employeeId": 1,
  "adminId": 456,
  "isActive": true
}
```

---

## ⚙️ Variables de Entorno

### 📄 Archivo `.env`

```properties
# Server Configuration
SERVER_PORT=8082

# Database Configuration
DATABASE_URL=jdbc:postgresql://users-db:5432/users_db
DATABASE_USER=users_user
DATABASE_PASSWORD=users_password

# JWT Configuration
JWT_SECRET=mySecretKey123456789PackedGoAuth2025VerySecureKey

# External Services
AUTH_SERVICE_URL=http://auth-service:8081/api
EVENT_SERVICE_URL=http://event-service:8086/api

# Logging
LOGGING_LEVEL_USERS=DEBUG
```

### 📋 Descripción de Variables

| Variable | Descripción | Valor por Defecto |
|----------|-------------|-------------------|
| `SERVER_PORT` | Puerto HTTP del servicio | 8082 |
| `DATABASE_URL` | URL de conexión PostgreSQL | jdbc:postgresql://users-db:5432/users_db |
| `DATABASE_USER` | Usuario de base de datos | users_user |
| `DATABASE_PASSWORD` | Contraseña de base de datos | users_password |
| `JWT_SECRET` | Clave secreta para validar tokens JWT | (debe coincidir con auth-service) |
| `AUTH_SERVICE_URL` | URL de auth-service | http://auth-service:8081/api |
| `EVENT_SERVICE_URL` | URL de event-service | http://event-service:8086/api |
| `LOGGING_LEVEL_USERS` | Nivel de logging del servicio | DEBUG |

---

## 🔐 Seguridad

### 🛡️ Validación JWT

- Todos los endpoints requieren JWT válido
- Validación de ownership para operaciones de perfil
- Solo ADMIN puede acceder a `/api/user-profiles` (listado completo)
- Los empleados solo pueden acceder a sus propios recursos

### 🔒 Control de Acceso

- **Perfiles:** Solo el owner o ADMIN puede ver/editar perfiles
- **Empleados (Admin):** Solo el admin que lo creó puede gestionarlo
- **Empleados (Operaciones):** Solo pueden validar tickets de eventos asignados

---

## 🔄 Integración con Otros Servicios

### Auth Service
- **URL:** `http://auth-service:8081/api`
- **Función:** Recibe llamadas para crear perfiles tras registro
- **Método:** Recibe POST en `/api/user-profiles/from-auth`

### Event Service
- **URL:** `http://event-service:8086/api`
- **Función:** Proxy para validación de tickets y consumiciones
- **Métodos:**
  - `POST /api/qr-validation/validate-entry`
  - `POST /api/qr-validation/validate-consumption`
  - `POST /api/event/by-ids` (obtener eventos asignados)

**WebClient Configuration:**
```java
@Bean
public WebClient eventServiceWebClient() {
    return WebClient.builder()
        .baseUrl(eventServiceUrl)
        .codecs(configurer -> configurer
            .defaultCodecs()
            .maxInMemorySize(5 * 1024 * 1024)) // 5MB buffer
        .build();
}
```

---

## 🐳 Docker

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/users-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082 5006
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

```yaml
users-service:
  build:
    context: ./users-service
    dockerfile: Dockerfile
  ports:
    - "8082:8082"
    - "5006:5006"
  env_file:
    - ./users-service/.env
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006
    - EVENT_SERVICE_URL=http://event-service:8086/api
  depends_on:
    users-db:
      condition: service_healthy
  networks:
    - packedgo-network

users-db:
  image: postgres:15-alpine
  environment:
    POSTGRES_DB: users_db
    POSTGRES_USER: users_user
    POSTGRES_PASSWORD: users_password
  ports:
    - "5434:5432"
  volumes:
    - users_db_data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U users_user -d users_db"]
    interval: 30s
    timeout: 10s
    retries: 3
  networks:
    - packedgo-network
```

---

## 🚀 Ejecución Local

### Requisitos
- Java 17+
- Maven 3.8+
- PostgreSQL 15+

### 1. Configurar Base de Datos

```sql
CREATE DATABASE users_db;
CREATE USER users_user WITH PASSWORD 'users_password';
GRANT ALL PRIVILEGES ON DATABASE users_db TO users_user;
```

### 2. Compilar y Ejecutar

```bash
cd users-service
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

### 3. Verificar

```bash
curl http://localhost:8082/api/actuator/health
```

---

## 🐳 Ejecución con Docker

```bash
# Compilar
cd users-service
./mvnw clean package -DskipTests

# Levantar con Docker Compose (desde /back)
cd ..
docker-compose up -d users-db
docker-compose up -d --build users-service

# Ver logs
docker-compose logs -f users-service
```

---

## 🧪 Testing

### Ejecutar Tests

```bash
./mvnw test
```

### Tests Principales
- ✅ Creación de perfiles desde auth-service
- ✅ Gestión CRUD de perfiles
- ✅ Soft delete de perfiles
- ✅ Creación y gestión de empleados
- ✅ Validación de ownership
- ✅ Integración con event-service (proxy)

---

## 🔍 Troubleshooting

### Error: "Cannot access other user's resources"
**Causa:** Intento de acceder a recursos de otro usuario  
**Solución:** Verificar que el userId del token coincida con el solicitado

### Error: "Connection refused to event-service"
**Causa:** Event-service no está disponible  
**Solución:** Verificar que event-service esté corriendo y accesible

### Error: "Employee not found"
**Causa:** El empleado no existe o fue eliminado  
**Solución:** Verificar que el empleado esté activo y asignado al admin correcto

### Error: "Event not assigned to employee"
**Causa:** El empleado intenta validar tickets de un evento no asignado  
**Solución:** El admin debe asignar el evento al empleado

---

## 📚 Documentación Adicional

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [ModelMapper](http://modelmapper.org/)

---

## 📞 Contacto

Para reportar problemas o sugerencias relacionadas con USERS-SERVICE, contacta al equipo de desarrollo de PackedGo.

---

**Última actualización:** Diciembre 2025  
**Versión:** 1.0.0
