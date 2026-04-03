# 🎉 EVENT-SERVICE - Servicio de Gestión de Eventos

## 📋 Descripción General

El **EVENT-SERVICE** es el núcleo de la gestión de eventos en PackedGo. Se encarga de la creación y administración completa de eventos, consumiciones, tickets, passes y la validación de accesos mediante códigos QR. Implementa la lógica de negocio principal para organizadores incluyendo control de stock en tiempo real y sistema de validación.

### 🎯 Características Principales

- 📅 **Gestión completa de eventos** (CRUD multi-tenant por organizador)
- 🍔 **Gestión de consumiciones** y categorización de productos
- 🎫 **Sistema de tickets y passes** pre-generados
- 📱 **Validación QR** para entrada única (single entry) y consumos
- 📊 **Control de stock** en tiempo real durante eventos
- 🔐 **Autenticación JWT** para operaciones sensibles
- 🖼️ **Gestión de imágenes** de eventos (almacenamiento en BD)
- 📈 **Estadísticas de eventos** para organizadores
- 🔒 **Control de acceso** basado en ownership de eventos

---

## 🚀 Configuración de Servicio

| Propiedad | Valor |
|-----------|-------|
| **Puerto HTTP** | 8086 |
| **Puerto Debug (JDWP)** | 5007 |
| **Context Path** | /api |
| **Base URL** | http://localhost:8086/api |

---

## 📦 Base de Datos

### Configuración PostgreSQL

| Propiedad | Valor |
|-----------|-------|
| **Nombre** | event_db |
| **Puerto** | 5435 → 5432 (Docker) |
| **Usuario** | event_user |
| **Contraseña** | event_password |
| **Imagen Docker** | postgres:15-alpine |

### 📊 Tablas Principales

#### `events`
```sql
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    location VARCHAR(255) NOT NULL,
    location_name VARCHAR(255),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    start_time TIME,
    end_time TIME,
    max_capacity INTEGER NOT NULL,
    available_passes INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    event_category_id BIGINT REFERENCES event_categories(id),
    created_by BIGINT NOT NULL, -- ID del admin organizador
    image_data BYTEA, -- Imagen del evento
    image_content_type VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `event_categories`
```sql
CREATE TABLE event_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_by BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `consumptions`
```sql
CREATE TABLE consumptions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock INTEGER NOT NULL,
    consumption_category_id BIGINT REFERENCES consumption_categories(id),
    event_id BIGINT REFERENCES events(id),
    image_data BYTEA,
    image_content_type VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `consumption_categories`
```sql
CREATE TABLE consumption_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_by BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `passes`
```sql
CREATE TABLE passes (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT REFERENCES events(id) NOT NULL,
    qr_code VARCHAR(500) UNIQUE NOT NULL,
    is_sold BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `tickets`
```sql
CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    pass_id BIGINT REFERENCES passes(id) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL, -- ID del comprador
    event_id BIGINT REFERENCES events(id) NOT NULL,
    order_id VARCHAR(255) NOT NULL,
    qr_code VARCHAR(500) UNIQUE NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP,
    purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_price DECIMAL(10,2) NOT NULL
);
```

#### `ticket_consumptions`
```sql
CREATE TABLE ticket_consumptions (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT REFERENCES tickets(id),
    consumption_id BIGINT REFERENCES consumptions(id),
    quantity INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `ticket_consumption_details`
```sql
CREATE TABLE ticket_consumption_details (
    id BIGSERIAL PRIMARY KEY,
    ticket_consumption_id BIGINT REFERENCES ticket_consumptions(id),
    redeemed_quantity INTEGER DEFAULT 0,
    last_redeemed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🛠 Tecnologías y Dependencias

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Java** | 17 | Lenguaje base |
| **Spring Boot** | 3.5.6 | Framework principal |
| **Spring Data JPA** | 3.5.6 | Persistencia de datos |
| **Spring Web** | 3.5.6 | API REST |
| **Spring Actuator** | 3.5.6 | Monitoreo y métricas |
| **JWT (jjwt)** | 0.11.5 | Validación de tokens |
| **PostgreSQL Driver** | 42.x | Driver JDBC |
| **Lombok** | Latest | Reducción de boilerplate |
| **HikariCP** | Latest | Connection pooling |

---

## 📡 API Endpoints

### 📅 Gestión de Eventos (`/api/event-service/event`)

#### **GET** `/api/event-service/event`
Listar todos los eventos (público).

```http
GET /api/event-service/event
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "name": "Fiesta de Año Nuevo 2025",
    "description": "La mejor fiesta para recibir el año",
    "location": "Av. Corrientes 1234, CABA",
    "locationName": "Club Central",
    "startDate": "2025-12-31T22:00:00",
    "endDate": "2026-01-01T06:00:00",
    "startTime": "22:00:00",
    "endTime": "06:00:00",
    "maxCapacity": 500,
    "availablePasses": 320,
    "price": 5000.00,
    "eventCategoryId": 1,
    "eventCategoryName": "Fiestas",
    "createdBy": 456,
    "isActive": true,
    "imageUrl": "/api/event-service/event/1/image",
    "createdAt": "2025-11-01T10:00:00"
  }
]
```

---

#### **GET** `/api/event-service/event/{id}`
Obtener evento por ID (público).

```http
GET /api/event-service/event/1
```

**Response 200 OK:**
```json
{
  "id": 1,
  "name": "Fiesta de Año Nuevo 2025",
  "description": "La mejor fiesta para recibir el año",
  "location": "Av. Corrientes 1234, CABA",
  "locationName": "Club Central",
  "startDate": "2025-12-31T22:00:00",
  "endDate": "2026-01-01T06:00:00",
  "maxCapacity": 500,
  "availablePasses": 320,
  "price": 5000.00,
  "consumptions": [
    {
      "id": 5,
      "name": "Cerveza Quilmes",
      "price": 800.00,
      "stock": 200
    }
  ],
  "eventCategoryName": "Fiestas",
  "createdBy": 456,
  "isActive": true
}
```

---

#### **GET** `/api/event-service/event/my-events`
Listar eventos del organizador autenticado.

```http
GET /api/event-service/event/my-events
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
    "maxCapacity": 500,
    "availablePasses": 320,
    "price": 5000.00,
    "createdBy": 456,
    "isActive": true
  }
]
```

---

#### **POST** `/api/event-service/event`
Crear nuevo evento (valida ownership).

```http
POST /api/event-service/event
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Concierto Rock Nacional",
  "description": "Los mejores artistas del rock argentino",
  "location": "Av. del Libertador 7890, CABA",
  "locationName": "Luna Park",
  "startDate": "2025-12-20T20:00:00",
  "endDate": "2025-12-21T01:00:00",
  "startTime": "20:00:00",
  "endTime": "01:00:00",
  "maxCapacity": 8000,
  "price": 12000.00,
  "eventCategoryId": 2,
  "createdBy": 456,
  "consumptions": [
    {
      "consumptionId": 5,
      "quantity": 10
    },
    {
      "consumptionId": 7,
      "quantity": 5
    }
  ]
}
```

**Response 201 CREATED:**
```json
{
  "id": 2,
  "name": "Concierto Rock Nacional",
  "location": "Av. del Libertador 7890, CABA",
  "locationName": "Luna Park",
  "startDate": "2025-12-20T20:00:00",
  "maxCapacity": 8000,
  "availablePasses": 8000,
  "price": 12000.00,
  "createdBy": 456,
  "isActive": true,
  "createdAt": "2025-12-14T10:00:00"
}
```

---

#### **PUT** `/api/event-service/event/{id}`
Actualizar evento (valida ownership).

```http
PUT /api/event-service/event/1
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Fiesta de Año Nuevo 2025 - AGOTADO",
  "description": "La mejor fiesta para recibir el año - ÚLTIMAS ENTRADAS",
  "price": 6000.00
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "name": "Fiesta de Año Nuevo 2025 - AGOTADO",
  "description": "La mejor fiesta para recibir el año - ÚLTIMAS ENTRADAS",
  "price": 6000.00,
  "updatedAt": "2025-12-14T11:00:00"
}
```

---

#### **DELETE** `/api/event-service/event/{id}`
Eliminar evento físicamente (solo el creador).

```http
DELETE /api/event-service/event/1
Authorization: Bearer {token}
```

**Response 204 NO CONTENT**

---

#### **DELETE** `/api/event-service/event/logical/{id}`
Desactivar evento (solo el creador).

```http
DELETE /api/event-service/event/logical/1
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "name": "Fiesta de Año Nuevo 2025",
  "isActive": false,
  "updatedAt": "2025-12-14T11:30:00"
}
```

---

#### **POST** `/api/event-service/event/by-ids`
Obtener múltiples eventos por IDs (usado por users-service).

```http
POST /api/event-service/event/by-ids
Content-Type: application/json

[1, 3, 5]
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "name": "Fiesta de Año Nuevo 2025",
    "location": "Club Central",
    "startDate": "2025-12-31T22:00:00"
  },
  {
    "id": 3,
    "name": "Concierto Rock",
    "location": "Luna Park",
    "startDate": "2025-12-20T20:00:00"
  }
]
```

---

#### **GET** `/api/event-service/event/{eventId}/consumptions`
Obtener consumiciones de un evento (público).

```http
GET /api/event-service/event/1/consumptions
```

**Response 200 OK:**
```json
[
  {
    "id": 5,
    "name": "Cerveza Quilmes",
    "description": "Cerveza 1L",
    "price": 800.00,
    "stock": 200,
    "consumptionCategoryName": "Bebidas Alcohólicas",
    "eventId": 1,
    "isActive": true
  },
  {
    "id": 7,
    "name": "Hamburguesa Completa",
    "description": "Con queso, lechuga, tomate",
    "price": 1500.00,
    "stock": 100,
    "consumptionCategoryName": "Comidas",
    "eventId": 1,
    "isActive": true
  }
]
```

---

#### **POST** `/api/event-service/event/{id}/image`
Subir imagen para un evento (valida ownership).

```http
POST /api/event-service/event/1/image
Authorization: Bearer {token}
Content-Type: multipart/form-data

image: [archivo PNG/JPEG, max 5MB]
```

**Response 200 OK:**
```json
{
  "message": "Imagen subida correctamente"
}
```

---

#### **GET** `/api/event-service/event/{id}/image`
Obtener imagen de un evento (público).

```http
GET /api/event-service/event/1/image
```

**Response 200 OK:**
```
Content-Type: image/png
[imagen binaria]
```

---

#### **GET** `/api/event-service/event/stats`
Obtener estadísticas de eventos del admin autenticado.

```http
GET /api/event-service/event/stats
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "totalEvents": 15,
  "activeEvents": 12,
  "upcomingEvents": 5,
  "pastEvents": 7,
  "totalCapacity": 50000,
  "availableCapacity": 12500,
  "totalTicketsSold": 37500,
  "occupancyRate": 75.0
}
```

---

### 🍔 Gestión de Consumiciones (`/api/event-service/consumption`)

#### **GET** `/api/event-service/consumption`
Listar todas las consumiciones.

```http
GET /api/event-service/consumption
```

**Response 200 OK:**
```json
[
  {
    "id": 5,
    "name": "Cerveza Quilmes",
    "description": "Cerveza 1L",
    "price": 800.00,
    "stock": 200,
    "consumptionCategoryId": 1,
    "consumptionCategoryName": "Bebidas Alcohólicas",
    "eventId": 1,
    "isActive": true
  }
]
```

---

#### **POST** `/api/event-service/consumption`
Crear nueva consumición.

```http
POST /api/event-service/consumption
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Fernet con Coca",
  "description": "Fernet Branca con Coca-Cola",
  "price": 1200.00,
  "stock": 150,
  "consumptionCategoryId": 1,
  "eventId": 1
}
```

**Response 201 CREATED:**
```json
{
  "id": 8,
  "name": "Fernet con Coca",
  "description": "Fernet Branca con Coca-Cola",
  "price": 1200.00,
  "stock": 150,
  "consumptionCategoryId": 1,
  "eventId": 1,
  "isActive": true,
  "createdAt": "2025-12-14T10:00:00"
}
```

---

#### **PUT** `/api/event-service/consumption/{id}`
Actualizar consumición.

```http
PUT /api/event-service/consumption/5
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Cerveza Quilmes 1L",
  "price": 900.00,
  "stock": 250
}
```

**Response 200 OK:**
```json
{
  "id": 5,
  "name": "Cerveza Quilmes 1L",
  "price": 900.00,
  "stock": 250,
  "updatedAt": "2025-12-14T11:00:00"
}
```

---

#### **DELETE** `/api/event-service/consumption/{id}`
Eliminar consumición.

```http
DELETE /api/event-service/consumption/5
Authorization: Bearer {token}
```

**Response 204 NO CONTENT**

---

### 🎫 Gestión de Tickets (`/api/event-service/ticket`)

#### **POST** `/api/event-service/ticket`
Crear ticket (usado por payment-service tras confirmación de pago).

```http
POST /api/event-service/ticket
Content-Type: application/json

{
  "userId": 123,
  "eventId": 1,
  "orderId": "ORD-20251214-001",
  "quantity": 2,
  "consumptions": [
    {
      "consumptionId": 5,
      "quantity": 4
    },
    {
      "consumptionId": 7,
      "quantity": 2
    }
  ]
}
```

**Response 201 CREATED:**
```json
{
  "tickets": [
    {
      "id": 100,
      "qrCode": "TICKET-20251214-001-1",
      "eventId": 1,
      "eventName": "Fiesta de Año Nuevo 2025",
      "userId": 123,
      "orderId": "ORD-20251214-001",
      "isUsed": false,
      "purchasedAt": "2025-12-14T10:00:00",
      "consumptions": [
        {
          "id": 5,
          "name": "Cerveza Quilmes",
          "quantity": 4,
          "redeemedQuantity": 0
        },
        {
          "id": 7,
          "name": "Hamburguesa Completa",
          "quantity": 2,
          "redeemedQuantity": 0
        }
      ]
    },
    {
      "id": 101,
      "qrCode": "TICKET-20251214-001-2",
      "eventId": 1,
      "userId": 123,
      "orderId": "ORD-20251214-001",
      "isUsed": false,
      "purchasedAt": "2025-12-14T10:00:00",
      "consumptions": [...]
    }
  ]
}
```

---

#### **GET** `/api/event-service/ticket/{id}`
Obtener ticket por ID.

```http
GET /api/event-service/ticket/100
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "id": 100,
  "qrCode": "TICKET-20251214-001-1",
  "eventId": 1,
  "eventName": "Fiesta de Año Nuevo 2025",
  "userId": 123,
  "orderId": "ORD-20251214-001",
  "isUsed": false,
  "usedAt": null,
  "purchasedAt": "2025-12-14T10:00:00",
  "consumptions": [
    {
      "id": 5,
      "name": "Cerveza Quilmes",
      "quantity": 4,
      "redeemedQuantity": 2
    }
  ]
}
```

---

#### **GET** `/api/event-service/ticket/user/{userId}`
Obtener tickets de un usuario.

```http
GET /api/event-service/ticket/user/123
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
[
  {
    "id": 100,
    "qrCode": "TICKET-20251214-001-1",
    "eventId": 1,
    "eventName": "Fiesta de Año Nuevo 2025",
    "isUsed": true,
    "usedAt": "2025-12-31T22:15:00",
    "purchasedAt": "2025-12-14T10:00:00"
  },
  {
    "id": 105,
    "qrCode": "TICKET-20251215-002-1",
    "eventId": 2,
    "eventName": "Concierto Rock Nacional",
    "isUsed": false,
    "usedAt": null,
    "purchasedAt": "2025-12-15T14:00:00"
  }
]
```

---

### 📱 Validación QR (`/api/event-service/qr-validation`)

#### **POST** `/api/event-service/qr-validation/validate-entry`
Validar entrada al evento (single entry - usado por users-service).

```http
POST /api/event-service/qr-validation/validate-entry
Authorization: Bearer {token}
Content-Type: application/json

{
  "qrCode": "TICKET-20251214-001-1",
  "eventId": 1
}
```

**Response 200 OK (Primera entrada):**
```json
{
  "success": true,
  "message": "Ticket validado correctamente - ENTRADA PERMITIDA",
  "ticketId": 100,
  "eventName": "Fiesta de Año Nuevo 2025",
  "customerName": "Juan Pérez",
  "validatedAt": "2025-12-31T22:15:00",
  "isUsed": true
}
```

**Response 400 BAD REQUEST (Ya usado):**
```json
{
  "success": false,
  "message": "Este ticket ya fue utilizado el 31/12/2025 a las 22:15",
  "ticketId": 100,
  "usedAt": "2025-12-31T22:15:00"
}
```

---

#### **POST** `/api/event-service/qr-validation/validate-consumption`
Validar/Canjear consumición (usado por users-service).

```http
POST /api/event-service/qr-validation/validate-consumption
Authorization: Bearer {token}
Content-Type: application/json

{
  "qrCode": "TICKET-20251214-001-1",
  "eventId": 1,
  "consumptionId": 5,
  "quantity": 1
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Consumo canjeado correctamente",
  "ticketId": 100,
  "consumptionName": "Cerveza Quilmes",
  "quantityRedeemed": 1,
  "remainingQuantity": 3,
  "redeemedAt": "2025-12-31T23:00:00"
}
```

**Response 400 BAD REQUEST (Sin stock):**
```json
{
  "success": false,
  "message": "Ya canjeaste todas tus unidades de este producto (4/4)",
  "ticketId": 100,
  "consumptionName": "Cerveza Quilmes",
  "totalQuantity": 4,
  "redeemedQuantity": 4
}
```

---

### 🎟️ Gestión de Passes (`/api/event-service/pass`)

#### **POST** `/api/event-service/pass/generate-for-event/{eventId}`
Generar passes para un evento (solo organizador del evento).

```http
POST /api/event-service/pass/generate-for-event/1
Authorization: Bearer {token}
Content-Type: application/json

{
  "quantity": 500
}
```

**Response 201 CREATED:**
```json
{
  "message": "500 passes generados correctamente para el evento 1",
  "eventId": 1,
  "totalPasses": 500
}
```

---

#### **GET** `/api/event-service/pass/event/{eventId}`
Obtener passes de un evento.

```http
GET /api/event-service/pass/event/1
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "eventId": 1,
    "qrCode": "PASS-EVENT1-001",
    "isSold": false,
    "createdAt": "2025-11-01T10:00:00"
  },
  {
    "id": 2,
    "eventId": 1,
    "qrCode": "PASS-EVENT1-002",
    "isSold": true,
    "createdAt": "2025-11-01T10:00:00"
  }
]
```

---

### 🏷️ Categorías de Eventos (`/api/event-service/event-category`)

#### **GET** `/api/event-service/event-category`
Listar categorías de eventos.

```http
GET /api/event-service/event-category
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "name": "Fiestas",
    "description": "Eventos de entretenimiento nocturno",
    "createdBy": 456,
    "isActive": true
  },
  {
    "id": 2,
    "name": "Conciertos",
    "description": "Shows musicales en vivo",
    "createdBy": 456,
    "isActive": true
  }
]
```

---

#### **POST** `/api/event-service/event-category`
Crear categoría de evento.

```http
POST /api/event-service/event-category
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Deportes",
  "description": "Eventos deportivos y competencias",
  "createdBy": 456
}
```

**Response 201 CREATED:**
```json
{
  "id": 3,
  "name": "Deportes",
  "description": "Eventos deportivos y competencias",
  "createdBy": 456,
  "isActive": true,
  "createdAt": "2025-12-14T10:00:00"
}
```

---

### 🏷️ Categorías de Consumiciones (`/api/event-service/consumption-category`)

#### **GET** `/api/event-service/consumption-category`
Listar categorías de consumiciones.

```http
GET /api/event-service/consumption-category
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "name": "Bebidas Alcohólicas",
    "description": "Cerveza, vino, tragos",
    "createdBy": 456,
    "isActive": true
  },
  {
    "id": 2,
    "name": "Comidas",
    "description": "Hamburguesas, pizzas, pancho",
    "createdBy": 456,
    "isActive": true
  }
]
```

---

#### **POST** `/api/event-service/consumption-category`
Crear categoría de consumición.

```http
POST /api/event-service/consumption-category
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Bebidas Sin Alcohol",
  "description": "Gaseosas, jugos, agua",
  "createdBy": 456
}
```

**Response 201 CREATED:**
```json
{
  "id": 3,
  "name": "Bebidas Sin Alcohol",
  "description": "Gaseosas, jugos, agua",
  "createdBy": 456,
  "isActive": true,
  "createdAt": "2025-12-14T10:00:00"
}
```

---

## ⚙️ Variables de Entorno

### 📄 Archivo `.env`

```properties
# Server Configuration (local)
SERVER_PORT=8086

# Database Configuration (local)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5435/event_db
SPRING_DATASOURCE_USERNAME=event_user
SPRING_DATASOURCE_PASSWORD=event_password

# JWT Configuration
JWT_SECRET=mySecretKey123456789PackedGoAuth2025VerySecureKey
```

### Docker Profile

```properties
# Activado automáticamente en Docker con SPRING_PROFILES_ACTIVE=docker
spring.datasource.url=jdbc:postgresql://event-db:5432/event_db
```

---

## 🔐 Seguridad

### 🛡️ Control de Acceso

- **Endpoints Públicos:**
  - `GET /api/event-service/event` (listado)
  - `GET /api/event-service/event/{id}` (detalle)
  - `GET /api/event-service/event/{eventId}/consumptions`
  - `GET /api/event-service/event/{id}/image`
  - `POST /api/event-service/event/by-ids` (interno)

- **Endpoints Protegidos (Requieren JWT):**
  - `POST`, `PUT`, `DELETE` en `/api/event-service/event`
  - Gestión de consumiciones
  - Gestión de categorías
  - Validación QR
  - Generación de passes

- **Validación de Ownership:**
  - Solo el organizador (`createdBy`) puede editar/eliminar sus eventos
  - Solo el organizador puede gestionar consumiciones de sus eventos
  - Solo el organizador puede generar passes

---

## 🔄 Integración con Otros Servicios

### Order Service
- **Función:** Consulta eventos y consumiciones para checkout
- **Métodos:**
  - `GET /api/event-service/event/{id}`
  - `GET /api/event-service/event/{eventId}/consumptions`

### Payment Service
- **Función:** Crea tickets tras confirmación de pago
- **Método:** `POST /api/event-service/ticket`

### Users Service
- **Función:** Proxy de validación QR para empleados
- **Métodos:**
  - `POST /api/event-service/qr-validation/validate-entry`
  - `POST /api/event-service/qr-validation/validate-consumption`
  - `POST /api/event-service/event/by-ids`

---

## 🐳 Docker

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/event-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8086 5007
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

```yaml
event-service:
  build:
    context: ./event-service
    dockerfile: Dockerfile
  ports:
    - "8086:8086"
    - "5007:5007"
  env_file:
    - ./event-service/.env
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5007
  depends_on:
    event-db:
      condition: service_healthy
  networks:
    - packedgo-network

event-db:
  image: postgres:15-alpine
  environment:
    POSTGRES_DB: event_db
    POSTGRES_USER: event_user
    POSTGRES_PASSWORD: event_password
  ports:
    - "5435:5432"
  volumes:
    - event_db_data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U event_user -d event_db"]
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
CREATE DATABASE event_db;
CREATE USER event_user WITH PASSWORD 'event_password';
GRANT ALL PRIVILEGES ON DATABASE event_db TO event_user;
```

### 2. Compilar y Ejecutar

```bash
cd event-service
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

### 3. Verificar

```bash
curl http://localhost:8086/api/actuator/health
```

---

## 🐳 Ejecución con Docker

```bash
# Compilar
cd event-service
./mvnw clean package -DskipTests

# Levantar con Docker Compose (desde /back)
cd ..
docker-compose up -d event-db
docker-compose up -d --build event-service

# Ver logs
docker-compose logs -f event-service
```

---

## 🧪 Testing

### Ejecutar Tests

```bash
./mvnw test
```

### Tests Principales
- ✅ CRUD de eventos
- ✅ Validación de ownership
- ✅ Generación de passes
- ✅ Creación de tickets
- ✅ Validación QR (entrada única)
- ✅ Canje de consumiciones
- ✅ Control de stock

---

## 🔍 Troubleshooting

### Error: "Event not found"
**Causa:** Evento no existe o fue eliminado  
**Solución:** Verificar ID del evento

### Error: "Insufficient passes available"
**Causa:** No hay passes suficientes para la compra  
**Solución:** Generar más passes o reducir cantidad

### Error: "Ticket already used"
**Causa:** El ticket ya fue utilizado para ingresar  
**Solución:** Validar que sea la primera entrada (single entry)

### Error: "Insufficient consumption quantity"
**Causa:** Se intenta canjear más de lo asignado  
**Solución:** Verificar cantidad restante en el ticket

### Error: "Cannot edit event from another organizer"
**Causa:** Intento de editar evento de otro organizador  
**Solución:** Solo el creador puede editar sus eventos

---

## 📚 Documentación Adicional

- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

## 📞 Contacto

Para reportar problemas o sugerencias relacionadas con EVENT-SERVICE, contacta al equipo de desarrollo de PackedGo.

---

**Última actualización:** Diciembre 2025  
**Versión:** 1.0.0
