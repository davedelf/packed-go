# 🔐 AUTH-SERVICE - Servicio de Autenticación y Autorización

## 📋 Descripción General

El **AUTH-SERVICE** es el microservicio central de autenticación y autorización de PackedGo. Gestiona el ciclo de vida completo de usuarios, sesiones, autenticación JWT y verificación de email. Implementa autenticación diferenciada por tipo de usuario (Admin/Customer/Employee/Super Admin) con seguridad robusta y auditoría completa.

### 🎯 Características Principales

- 🔑 **Autenticación JWT** con tokens de acceso y refresh tokens
- 👥 **Multi-tenant** por tipo de usuario (CUSTOMER, ADMIN, EMPLOYEE, SUPER_ADMIN)
- 📧 **Verificación de email** obligatoria con tokens de 24 horas
- 🔄 **Redireccionamiento inteligente** basado en rol tras verificación
- 🔒 **Recuperación de contraseñas** con tokens de reset
- 🛡️ **Protección contra fuerza bruta** (5 intentos, 30 min de bloqueo)
- 📊 **Auditoría completa** de intentos de login
- ✅ **Integración con Mailtrap** (desarrollo) / SendGrid (producción)
- 🔐 **Encriptación BCrypt** (strength 12)

---

## 🚀 Configuración de Servicio

| Propiedad | Valor |
|-----------|-------|
| **Puerto HTTP** | 8081 |
| **Puerto Debug (JDWP)** | 5005 |
| **Context Path** | /api |
| **Base URL** | http://localhost:8081/api |

---

## 📦 Base de Datos

### Configuración PostgreSQL

| Propiedad | Valor |
|-----------|-------|
| **Nombre** | auth_db |
| **Puerto** | 5433 → 5432 (Docker) |
| **Usuario** | auth_user |
| **Contraseña** | auth_password |
| **Imagen Docker** | postgres:15-alpine |
| **Timezone** | America/Argentina/Buenos_Aires |

### 📊 Tablas Principales

#### `auth_users`
```sql
CREATE TABLE auth_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE,
    document VARCHAR(20) UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL, -- CUSTOMER, ADMIN, EMPLOYEE, SUPER_ADMIN
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE,
    is_locked BOOLEAN DEFAULT FALSE,
    failed_login_attempts INT DEFAULT 0,
    lock_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `user_sessions`
```sql
CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES auth_users(id),
    token VARCHAR(500) NOT NULL,
    refresh_token VARCHAR(500),
    ip_address VARCHAR(50),
    user_agent VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);
```

#### `email_verification_tokens`
```sql
CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES auth_users(id),
    token VARCHAR(500) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE
);
```

#### `password_recovery_tokens`
```sql
CREATE TABLE password_recovery_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES auth_users(id),
    token VARCHAR(500) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE
);
```

#### `login_attempts`
```sql
CREATE TABLE login_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES auth_users(id),
    ip_address VARCHAR(50),
    user_agent VARCHAR(255),
    success BOOLEAN NOT NULL,
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    failure_reason VARCHAR(255)
);
```

---

## 🛠 Tecnologías y Dependencias

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Java** | 17 | Lenguaje base |
| **Spring Boot** | 3.5.6 | Framework principal |
| **Spring Security** | 3.5.6 | Autenticación y autorización |
| **Spring Data JPA** | 3.5.6 | Persistencia de datos |
| **Spring Mail** | 3.5.6 | Envío de emails |
| **Spring WebFlux** | 3.5.6 | Cliente HTTP reactivo |
| **JWT (jjwt)** | 0.12.5 | Generación y validación de tokens |
| **BCrypt** | (Spring Security) | Encriptación de contraseñas |
| **ModelMapper** | 3.2.0 | Mapeo DTOs ↔ Entidades |
| **PostgreSQL Driver** | 42.x | Driver JDBC |
| **SendGrid** | 4.10.2 | Envío de emails (producción) |
| **Lombok** | Latest | Reducción de boilerplate |
| **Validation API** | Jakarta | Validación de datos |

---

## 📡 API Endpoints

### 🔓 Autenticación Pública

#### **POST** `/api/auth/customer/login`
Autenticación de clientes usando DNI.

```http
POST /api/auth/customer/login
Content-Type: application/json

{
  "document": "12345678",
  "password": "miPassword123"
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Customer login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 123,
    "username": "juan_perez",
    "email": "juan@example.com",
    "role": "CUSTOMER",
    "expiresIn": 86400
  }
}
```

---

#### **POST** `/api/auth/admin/login`
Autenticación de administradores usando email.

```http
POST /api/auth/admin/login
Content-Type: application/json

{
  "email": "admin@packedgo.com",
  "password": "adminPassword123"
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Admin login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 456,
    "username": "admin_user",
    "email": "admin@packedgo.com",
    "role": "ADMIN",
    "expiresIn": 86400
  }
}
```

---

#### **POST** `/api/auth/employee/login`
Autenticación de empleados usando email.

```http
POST /api/auth/employee/login
Content-Type: application/json

{
  "email": "employee@packedgo.com",
  "password": "employeePassword123"
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Employee login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 789,
    "username": "employee_user",
    "email": "employee@packedgo.com",
    "role": "EMPLOYEE",
    "expiresIn": 86400
  }
}
```

---

### 📝 Registro de Usuarios

#### **POST** `/api/auth/customer/register`
Registro de nuevos clientes.

```http
POST /api/auth/customer/register
Content-Type: application/json

{
  "username": "nuevo_usuario",
  "email": "nuevo@example.com",
  "document": "98765432",
  "password": "Password123!",
  "confirmPassword": "Password123!"
}
```

**Response 201 CREATED:**
```json
{
  "success": true,
  "message": "Customer registered successfully. Please verify your email.",
  "data": null
}
```

---

#### **POST** `/api/auth/admin/register`
Registro de nuevos administradores (requiere código de autorización).

```http
POST /api/auth/admin/register
Content-Type: application/json

{
  "username": "nuevo_admin",
  "email": "nuevo_admin@packedgo.com",
  "password": "AdminPassword123!",
  "confirmPassword": "AdminPassword123!",
  "authorizationCode": "ADMIN-2025-SECRET"
}
```

**Response 201 CREATED:**
```json
{
  "success": true,
  "message": "Admin registration request received. Awaiting approval.",
  "data": null
}
```

---

### ✅ Verificación de Email

#### **GET** `/api/auth/verify-email?token={token}`
Verifica el email del usuario y retorna su rol para redireccionamiento.

```http
GET /api/auth/verify-email?token=abc123xyz456
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Email verified successfully",
  "data": {
    "success": true,
    "message": "Email verified successfully",
    "role": "CUSTOMER"
  }
}
```

**Lógica de Redireccionamiento:**
- `CUSTOMER` → Redirige a `/customer/login`
- `ADMIN` o `SUPER_ADMIN` → Redirige a `/admin/login`
- `EMPLOYEE` → Redirige a `/employee/login`

---

#### **POST** `/api/auth/resend-verification`
Reenvía el email de verificación.

```http
POST /api/auth/resend-verification
Content-Type: application/json

{
  "email": "usuario@example.com"
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Verification email resent successfully",
  "data": null
}
```

---

### 🔄 Gestión de Tokens

#### **POST** `/api/auth/refresh`
Renueva el access token usando el refresh token.

```http
POST /api/auth/refresh
Content-Type: text/plain

eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refreshToken...
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.newToken..."
}
```

---

#### **POST** `/api/auth/validate`
Valida un token JWT.

```http
POST /api/auth/validate
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Token validation completed",
  "data": {
    "valid": true,
    "userId": 123,
    "username": "usuario",
    "role": "CUSTOMER",
    "expiresAt": "2025-12-15T10:30:00"
  }
}
```

---

#### **POST** `/api/auth/logout`
Cierra la sesión del usuario.

```http
POST /api/auth/logout
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Logout successful",
  "data": null
}
```

---

### 🔑 Recuperación de Contraseñas

#### **POST** `/api/auth/forgot-password`
Solicita un token de recuperación de contraseña.

```http
POST /api/auth/forgot-password
Content-Type: application/json

{
  "email": "usuario@example.com"
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Password reset email sent if email exists",
  "data": null
}
```

---

#### **POST** `/api/auth/reset-password`
Restablece la contraseña usando el token de recuperación.

```http
POST /api/auth/reset-password
Content-Type: application/json

{
  "token": "reset-token-xyz",
  "newPassword": "NewPassword123!",
  "confirmPassword": "NewPassword123!"
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Password reset successfully",
  "data": null
}
```

---

### 👤 Gestión de Perfiles

#### **GET** `/api/auth/user/{userId}`
Obtiene el perfil del usuario autenticado.

```http
GET /api/auth/user/123
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "User profile retrieved successfully",
  "data": {
    "userId": 123,
    "username": "juan_perez",
    "email": "juan@example.com",
    "document": "12345678",
    "role": "CUSTOMER",
    "isVerified": true,
    "isActive": true,
    "createdAt": "2025-01-01T10:00:00"
  }
}
```

---

#### **PUT** `/api/auth/user/{userId}`
Actualiza el perfil del usuario.

```http
PUT /api/auth/user/123
Authorization: Bearer {token}
Content-Type: application/json

{
  "username": "nuevo_username",
  "email": "nuevo_email@example.com"
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "User profile updated successfully",
  "data": null
}
```

---

#### **POST** `/api/auth/change-password/{userId}`
Cambia la contraseña del usuario autenticado.

```http
POST /api/auth/change-password/123
Authorization: Bearer {token}
Content-Type: application/json

{
  "currentPassword": "OldPassword123",
  "newPassword": "NewPassword456!",
  "confirmPassword": "NewPassword456!"
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Password changed successfully",
  "data": null
}
```

---

## ⚙️ Variables de Entorno

### 📄 Archivo `.env`

```properties
# Server Configuration
SERVER_PORT=8081

# Database Configuration
DATABASE_URL=jdbc:postgresql://auth-db:5432/auth_db
DATABASE_USER=auth_user
DATABASE_PASSWORD=auth_password

# JWT Configuration
JWT_SECRET=mySecretKey123456789PackedGoAuth2025VerySecureKey
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Email Configuration (Mailtrap - Development)
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=your_mailtrap_username
MAIL_PASSWORD=your_mailtrap_password
EMAIL_FROM=noreply@packedgo.com

# Frontend Configuration
FRONTEND_BASE_URL=http://localhost:4200

# External Services
USERS_SERVICE_URL=http://users-service:8082/api

# Logging
LOGGING_LEVEL_AUTH=DEBUG
LOGGING_LEVEL_SECURITY=INFO
```

### 📋 Descripción de Variables

| Variable | Descripción | Valor por Defecto |
|----------|-------------|-------------------|
| `SERVER_PORT` | Puerto HTTP del servicio | 8081 |
| `DATABASE_URL` | URL de conexión PostgreSQL | jdbc:postgresql://auth-db:5432/auth_db |
| `DATABASE_USER` | Usuario de base de datos | auth_user |
| `DATABASE_PASSWORD` | Contraseña de base de datos | auth_password |
| `JWT_SECRET` | Clave secreta para firmar tokens JWT | (debe ser segura en producción) |
| `JWT_EXPIRATION` | Tiempo de expiración del access token (ms) | 86400000 (24h) |
| `JWT_REFRESH_EXPIRATION` | Tiempo de expiración del refresh token (ms) | 604800000 (7 días) |
| `MAIL_HOST` | Host SMTP para envío de emails | sandbox.smtp.mailtrap.io |
| `MAIL_PORT` | Puerto SMTP | 2525 |
| `MAIL_USERNAME` | Usuario SMTP | - |
| `MAIL_PASSWORD` | Contraseña SMTP | - |
| `EMAIL_FROM` | Email remitente | noreply@packedgo.com |
| `FRONTEND_BASE_URL` | URL base del frontend | http://localhost:4200 |
| `USERS_SERVICE_URL` | URL de users-service | http://users-service:8082/api |
| `LOGGING_LEVEL_AUTH` | Nivel de logging del servicio | DEBUG |
| `LOGGING_LEVEL_SECURITY` | Nivel de logging de Spring Security | INFO |

---

## 🔐 Seguridad

### 🛡️ Características de Seguridad

1. **Encriptación de Contraseñas:**
   - BCrypt con strength 12
   - Salt automático por usuario

2. **Protección contra Fuerza Bruta:**
   - Máximo 5 intentos fallidos
   - Bloqueo de cuenta por 30 minutos
   - Registro de todos los intentos

3. **Tokens JWT:**
   - Firmados con HS256
   - Incluyen: userId, username, role, expiración
   - Refresh tokens para renovación

4. **Verificación de Email:**
   - Tokens únicos de 24 horas
   - Obligatorio para activar cuenta
   - Invalidación tras uso

5. **CORS:**
   - Configurado para `http://localhost:4200` (desarrollo)
   - Debe configurarse específicamente en producción

6. **Auditoría:**
   - Todos los intentos de login registrados
   - IP y User-Agent capturados
   - Timestamp de cada operación

---

## 🔄 Integración con Otros Servicios

### Users Service
- **URL:** `http://users-service:8082/api`
- **Función:** Creación automática de perfil tras registro exitoso
- **Método:** `POST /api/user-profiles/from-auth`

**Flujo de Integración:**
```
1. Usuario se registra en auth-service
2. Auth-service valida datos y crea AuthUser
3. Auth-service llama a users-service para crear UserProfile
4. Users-service retorna confirmación
5. Auth-service envía email de verificación
```

---

## 🐳 Docker

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/auth-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081 5005
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

```yaml
auth-service:
  build:
    context: ./auth-service
    dockerfile: Dockerfile
  ports:
    - "8081:8081"
    - "5005:5005"
  env_file:
    - ./auth-service/.env
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
  depends_on:
    auth-db:
      condition: service_healthy
    users-service:
      condition: service_started
  networks:
    - packedgo-network

auth-db:
  image: postgres:15-alpine
  environment:
    POSTGRES_DB: auth_db
    POSTGRES_USER: auth_user
    POSTGRES_PASSWORD: auth_password
  ports:
    - "5433:5432"
  volumes:
    - auth_db_data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U auth_user -d auth_db"]
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
CREATE DATABASE auth_db;
CREATE USER auth_user WITH PASSWORD 'auth_password';
GRANT ALL PRIVILEGES ON DATABASE auth_db TO auth_user;
```

### 2. Compilar el Proyecto

```bash
cd auth-service
./mvnw clean package -DskipTests
```

### 3. Ejecutar

```bash
# Usando Maven
./mvnw spring-boot:run

# Usando JAR
java -jar target/auth-service-0.0.1-SNAPSHOT.jar
```

### 4. Verificar

```bash
curl http://localhost:8081/api/auth/health
```

---

## 🐳 Ejecución con Docker

```bash
# Compilar
cd auth-service
./mvnw clean package -DskipTests

# Levantar con Docker Compose (desde /back)
cd ..
docker-compose up -d auth-db
docker-compose up -d --build auth-service

# Ver logs
docker-compose logs -f auth-service
```

---

## 🧪 Testing

### Ejecutar Tests

```bash
./mvnw test
```

### Tests Principales
- ✅ Registro de usuarios (Customer/Admin/Employee)
- ✅ Login con diferentes credenciales
- ✅ Generación y validación de JWT
- ✅ Verificación de email
- ✅ Recuperación de contraseñas
- ✅ Protección contra fuerza bruta
- ✅ Integración con users-service

---

## 🔍 Troubleshooting

### Error: "Invalid JWT token"
**Causa:** Token expirado o inválido  
**Solución:** Usar `/api/auth/refresh` para obtener nuevo token

### Error: "Account is locked"
**Causa:** Más de 5 intentos fallidos  
**Solución:** Esperar 30 minutos o contactar administrador

### Error: "Email not verified"
**Causa:** Usuario no ha verificado su email  
**Solución:** Usar `/api/auth/resend-verification`

### Error: "Connection refused to users-service"
**Causa:** Users-service no está disponible  
**Solución:** Verificar que users-service esté corriendo

### Error: "Email sending failed"
**Causa:** Configuración SMTP incorrecta  
**Solución:** Verificar credenciales de Mailtrap/SendGrid

---

## 📚 Documentación Adicional

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html)
- [JWT.io](https://jwt.io/)
- [Mailtrap Documentation](https://mailtrap.io/docs/)
- [SendGrid API](https://docs.sendgrid.com/)

---

## 📞 Contacto

Para reportar problemas o sugerencias relacionadas con AUTH-SERVICE, contacta al equipo de desarrollo de PackedGo.

---

**Última actualización:** Diciembre 2025  
**Versión:** 1.0.0
