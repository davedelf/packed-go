# 🛒 ORDER-SERVICE - Servicio de Carrito y Órdenes

## 📋 Descripción General

El **ORDER-SERVICE** gestiona el flujo completo de compras en PackedGo, desde la creación del carrito hasta la generación de órdenes listas para pago. Incluye gestión de sesiones con expiración automática, validación de stock en tiempo real, y limpieza programada de carritos antiguos.

### 🎯 Características Principales

- 🛍️ **Carrito de compra multi-item** con eventos y consumiciones
- ⏱️ **Expiración automática** de carritos (10 minutos de inactividad)
- 📦 **Validación de stock** en tiempo real con event-service
- 💳 **Generación de órdenes** con integración a payment-service
- 🧹 **Limpieza programada** de carritos expirados (cada 5 minutos)
- 📧 **Email de confirmación** de orden
- 🔐 **Validación JWT** en todos los endpoints
- 🔄 **Integración WebClient** con event-service y payment-service
- 🎟️ **Límite de 10 tickets** por grupo de compra

---

## 🚀 Configuración de Servicio

| Propiedad | Valor |
|-----------|-------|
| **Puerto HTTP** | 8084 |
| **Puerto Debug (JDWP)** | 5008 |
| **Context Path** | /api |
| **Base URL** | http://localhost:8084/api |

---

## 📦 Base de Datos

### Configuración PostgreSQL

| Propiedad | Valor |
|-----------|-------|
| **Nombre** | order_db |
| **Puerto** | 5436 → 5432 (Docker) |
| **Usuario** | order_user |
| **Contraseña** | order_password |
| **Imagen Docker** | postgres:15-alpine |
| **Timezone** | America/Argentina/Buenos_Aires |

### 📊 Tablas Principales

#### `shopping_carts`
```sql
CREATE TABLE shopping_carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL, -- ACTIVE, EXPIRED, CHECKED_OUT
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL, -- created_at + 10 minutos
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, status) WHERE status = 'ACTIVE'
);
```

#### `cart_items`
```sql
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT REFERENCES shopping_carts(id) ON DELETE CASCADE,
    event_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `cart_item_consumptions`
```sql
CREATE TABLE cart_item_consumptions (
    id BIGSERIAL PRIMARY KEY,
    cart_item_id BIGINT REFERENCES cart_items(id) ON DELETE CASCADE,
    consumption_id BIGINT NOT NULL,
    consumption_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `orders`
```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL, -- ORD-YYYYMMDD-XXX
    user_id BIGINT NOT NULL,
    cart_id BIGINT REFERENCES shopping_carts(id),
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, PAID, CANCELLED
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
| **Spring WebFlux** | 3.5.6 | Cliente HTTP reactivo |
| **Spring Mail** | 3.5.6 | Envío de emails |
| **Spring Scheduling** | 3.5.6 | Tareas programadas |
| **JWT** | Latest | Validación de tokens |
| **ModelMapper** | Latest | Mapeo DTOs ↔ Entidades |
| **PostgreSQL Driver** | 42.x | Driver JDBC |
| **Lombok** | Latest | Reducción de boilerplate |

---

## 📡 API Endpoints

### 🛒 Gestión de Carrito (`/api/cart`)

#### **POST** `/api/cart/add`
Agregar evento con consumos al carrito.

```http
POST /api/cart/add
Authorization: Bearer {token}
Content-Type: application/json

{
  "eventId": 1,
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
  "id": 1,
  "userId": 123,
  "status": "ACTIVE",
  "items": [
    {
      "id": 1,
      "eventId": 1,
      "eventName": "Fiesta de Año Nuevo 2025",
      "quantity": 2,
      "unitPrice": 5000.00,
      "subtotal": 10000.00,
      "consumptions": [
        {
          "id": 1,
          "consumptionId": 5,
          "consumptionName": "Cerveza Quilmes",
          "quantity": 4,
          "unitPrice": 800.00,
          "subtotal": 3200.00
        },
        {
          "id": 2,
          "consumptionId": 7,
          "consumptionName": "Hamburguesa Completa",
          "quantity": 2,
          "unitPrice": 1500.00,
          "subtotal": 3000.00
        }
      ]
    }
  ],
  "totalAmount": 16200.00,
  "createdAt": "2025-12-14T10:00:00",
  "expiresAt": "2025-12-14T10:10:00"
}
```

---

#### **GET** `/api/cart`
Obtener carrito activo del usuario.

```http
GET /api/cart
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "userId": 123,
  "status": "ACTIVE",
  "items": [
    {
      "id": 1,
      "eventId": 1,
      "eventName": "Fiesta de Año Nuevo 2025",
      "quantity": 2,
      "unitPrice": 5000.00,
      "subtotal": 10000.00,
      "consumptions": [...]
    }
  ],
  "totalAmount": 16200.00,
  "createdAt": "2025-12-14T10:00:00",
  "expiresAt": "2025-12-14T10:10:00"
}
```

**Response 404 NOT FOUND:**
```json
{
  "error": "No active cart found"
}
```

---

#### **DELETE** `/api/cart/items/{itemId}`
Eliminar un item del carrito.

```http
DELETE /api/cart/items/1
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "userId": 123,
  "items": [],
  "totalAmount": 0.00,
  "message": "Item removed from cart"
}
```

**Response 204 NO CONTENT** (si el carrito quedó vacío)

---

#### **DELETE** `/api/cart`
Vaciar completamente el carrito.

```http
DELETE /api/cart
Authorization: Bearer {token}
```

**Response 204 NO CONTENT**

---

#### **PUT** `/api/cart/items/{itemId}`
Actualizar cantidad de un item.

```http
PUT /api/cart/items/1
Authorization: Bearer {token}
Content-Type: application/json

{
  "quantity": 3
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "userId": 123,
  "items": [
    {
      "id": 1,
      "eventId": 1,
      "quantity": 3,
      "unitPrice": 5000.00,
      "subtotal": 15000.00,
      "consumptions": [...]
    }
  ],
  "totalAmount": 24300.00
}
```

---

#### **PUT** `/api/cart/items/{itemId}/consumptions/{consumptionId}`
Actualizar cantidad de una consumición.

```http
PUT /api/cart/items/1/consumptions/5
Authorization: Bearer {token}
Content-Type: application/json

{
  "quantity": 6
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "userId": 123,
  "items": [
    {
      "id": 1,
      "consumptions": [
        {
          "id": 1,
          "consumptionId": 5,
          "consumptionName": "Cerveza Quilmes",
          "quantity": 6,
          "unitPrice": 800.00,
          "subtotal": 4800.00
        }
      ]
    }
  ],
  "totalAmount": 17800.00
}
```

---

#### **POST** `/api/cart/items/{itemId}/consumptions`
Agregar nueva consumición a un item existente.

```http
POST /api/cart/items/1/consumptions
Authorization: Bearer {token}
Content-Type: application/json

{
  "consumptionId": 8,
  "quantity": 2
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "userId": 123,
  "items": [
    {
      "id": 1,
      "consumptions": [
        {
          "id": 1,
          "consumptionId": 5,
          "consumptionName": "Cerveza Quilmes",
          "quantity": 4,
          "subtotal": 3200.00
        },
        {
          "id": 3,
          "consumptionId": 8,
          "consumptionName": "Fernet con Coca",
          "quantity": 2,
          "subtotal": 2400.00
        }
      ]
    }
  ],
  "totalAmount": 18600.00
}
```

---

#### **DELETE** `/api/cart/items/{itemId}/consumptions/{consumptionId}`
Eliminar una consumición de un item.

```http
DELETE /api/cart/items/1/consumptions/5
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "userId": 123,
  "items": [
    {
      "id": 1,
      "consumptions": [
        {
          "id": 2,
          "consumptionId": 7,
          "consumptionName": "Hamburguesa Completa",
          "quantity": 2,
          "subtotal": 3000.00
        }
      ]
    }
  ],
  "totalAmount": 13000.00
}
```

---

### 📦 Gestión de Órdenes (`/api/order`)

#### **POST** `/api/order/checkout`
Crear orden desde el carrito activo.

```http
POST /api/order/checkout
Authorization: Bearer {token}
Content-Type: application/json

{
  "email": "usuario@example.com"
}
```

**Response 201 CREATED:**
```json
{
  "orderNumber": "ORD-20251214-001",
  "userId": 123,
  "totalAmount": 16200.00,
  "status": "PENDING",
  "items": [
    {
      "eventId": 1,
      "eventName": "Fiesta de Año Nuevo 2025",
      "quantity": 2,
      "unitPrice": 5000.00,
      "subtotal": 10000.00,
      "consumptions": [...]
    }
  ],
  "createdAt": "2025-12-14T10:05:00",
  "paymentUrl": "https://checkout.stripe.com/pay/cs_test_..."
}
```

**Response 400 BAD REQUEST:**
```json
{
  "error": "Cart is empty"
}
```

**Response 400 BAD REQUEST:**
```json
{
  "error": "Ticket limit exceeded. Maximum 10 tickets per order."
}
```

---

#### **GET** `/api/order/{orderNumber}`
Obtener orden por número.

```http
GET /api/order/ORD-20251214-001
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "orderNumber": "ORD-20251214-001",
  "userId": 123,
  "totalAmount": 16200.00,
  "status": "PENDING",
  "items": [...],
  "createdAt": "2025-12-14T10:05:00"
}
```

---

#### **GET** `/api/order/user`
Obtener órdenes del usuario autenticado.

```http
GET /api/order/user
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
[
  {
    "orderNumber": "ORD-20251214-001",
    "totalAmount": 16200.00,
    "status": "PAID",
    "createdAt": "2025-12-14T10:05:00"
  },
  {
    "orderNumber": "ORD-20251210-012",
    "totalAmount": 8500.00,
    "status": "PAID",
    "createdAt": "2025-12-10T15:30:00"
  }
]
```

---

#### **PATCH** `/api/order/{orderNumber}/status`
Actualizar estado de orden (usado por payment-service tras pago).

```http
PATCH /api/order/ORD-20251214-001/status
Content-Type: application/json

{
  "status": "PAID"
}
```

**Response 200 OK:**
```json
{
  "orderNumber": "ORD-20251214-001",
  "status": "PAID",
  "updatedAt": "2025-12-14T10:10:00"
}
```

---

## ⚙️ Variables de Entorno

### 📄 Archivo `.env`

```properties
# Server Configuration
SERVER_PORT=8084

# Database Configuration
DATABASE_URL=jdbc:postgresql://order-db:5432/order_db
DATABASE_USER=order_user
DATABASE_PASSWORD=order_password

# JWT Configuration
JWT_SECRET=mySecretKey123456789PackedGoAuth2025VerySecureKey

# External Services
EVENT_SERVICE_URL=http://event-service:8086/api
AUTH_SERVICE_URL=http://auth-service:8081/api
PAYMENT_SERVICE_URL=http://payment-service:8085/api

# Cart Configuration
CART_EXPIRATION_MINUTES=10
CART_CLEANUP_INTERVAL_MINUTES=5

# Email Configuration
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=your_mailtrap_username
MAIL_PASSWORD=your_mailtrap_password

# Logging
LOG_LEVEL_ROOT=INFO
LOGGING_LEVEL_ORDER=DEBUG

# JPA Configuration
JPA_SHOW_SQL=false
JPA_FORMAT_SQL=false
```

### 📋 Descripción de Variables

| Variable | Descripción | Valor por Defecto |
|----------|-------------|-------------------|
| `SERVER_PORT` | Puerto HTTP del servicio | 8084 |
| `DATABASE_URL` | URL de conexión PostgreSQL | jdbc:postgresql://order-db:5432/order_db |
| `DATABASE_USER` | Usuario de base de datos | order_user |
| `DATABASE_PASSWORD` | Contraseña de base de datos | order_password |
| `JWT_SECRET` | Clave secreta para validar tokens JWT | (debe coincidir con auth-service) |
| `EVENT_SERVICE_URL` | URL de event-service | http://event-service:8086/api |
| `PAYMENT_SERVICE_URL` | URL de payment-service | http://payment-service:8085/api |
| `CART_EXPIRATION_MINUTES` | Tiempo de expiración del carrito | 10 |
| `CART_CLEANUP_INTERVAL_MINUTES` | Intervalo de limpieza automática | 5 |
| `MAIL_HOST` | Host SMTP | sandbox.smtp.mailtrap.io |
| `MAIL_PORT` | Puerto SMTP | 2525 |
| `LOGGING_LEVEL_ORDER` | Nivel de logging del servicio | DEBUG |

---

## 🔐 Seguridad

### 🛡️ Validación JWT

- Todos los endpoints requieren JWT válido
- Validación de userId en token vs operaciones de carrito
- Solo el owner puede acceder a su carrito y órdenes

### ⏱️ Expiración de Carritos

- Carritos expiran automáticamente a los 10 minutos
- Limpieza programada cada 5 minutos
- Estado cambia a `EXPIRED` automáticamente

---

## 🔄 Integración con Otros Servicios

### Event Service
- **URL:** `http://event-service:8086/api`
- **Función:** Validar stock y obtener precios de eventos/consumiciones
- **Métodos:**
  - `GET /api/event-service/event/{id}`
  - `GET /api/event-service/event/{eventId}/consumptions`

### Payment Service
- **URL:** `http://payment-service:8085/api`
- **Función:** Crear sesión de pago con Stripe
- **Método:** `POST /api/payments/create-checkout-stripe`

### Auth Service
- **URL:** `http://auth-service:8081/api`
- **Función:** Validación de JWT (implícita)

**Flujo de Checkout:**
```
1. Usuario agrega items al carrito (cart-service)
2. Usuario hace checkout (order-service)
3. Order-service valida stock con event-service
4. Order-service crea orden y llama a payment-service
5. Payment-service genera sesión de Stripe
6. Order-service retorna URL de pago
7. Usuario paga en Stripe
8. Stripe webhook actualiza orden a PAID
9. Payment-service crea tickets en event-service
```

---

## 🕒 Tareas Programadas

### Limpieza de Carritos

```java
@Scheduled(fixedRateString = "${app.cart.cleanup-interval-minutes:5}", timeUnit = TimeUnit.MINUTES)
public void cleanupExpiredCarts() {
    LocalDateTime now = LocalDateTime.now();
    List<ShoppingCart> expiredCarts = shoppingCartRepository
        .findByStatusAndExpiresAtBefore("ACTIVE", now);
    
    expiredCarts.forEach(cart -> {
        cart.setStatus("EXPIRED");
        shoppingCartRepository.save(cart);
    });
    
    log.info("Cleaned up {} expired carts", expiredCarts.size());
}
```

---

## 🐳 Docker

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/order-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8084 5008
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

```yaml
order-service:
  build:
    context: ./order-service
    dockerfile: Dockerfile
  ports:
    - "8084:8084"
    - "5008:5008"
  env_file:
    - ./order-service/.env
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5008
  depends_on:
    order-db:
      condition: service_healthy
    event-service:
      condition: service_started
    auth-service:
      condition: service_started
  networks:
    - packedgo-network

order-db:
  image: postgres:15-alpine
  environment:
    POSTGRES_DB: order_db
    POSTGRES_USER: order_user
    POSTGRES_PASSWORD: order_password
  ports:
    - "5436:5432"
  volumes:
    - order_db_data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U order_user -d order_db"]
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
CREATE DATABASE order_db;
CREATE USER order_user WITH PASSWORD 'order_password';
GRANT ALL PRIVILEGES ON DATABASE order_db TO order_user;
```

### 2. Compilar y Ejecutar

```bash
cd order-service
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

### 3. Verificar

```bash
curl http://localhost:8084/api/actuator/health
```

---

## 🐳 Ejecución con Docker

```bash
# Compilar
cd order-service
./mvnw clean package -DskipTests

# Levantar con Docker Compose (desde /back)
cd ..
docker-compose up -d order-db
docker-compose up -d --build order-service

# Ver logs
docker-compose logs -f order-service
```

---

## 🧪 Testing

### Ejecutar Tests

```bash
./mvnw test
```

### Tests Principales
- ✅ Creación de carrito
- ✅ Agregar/eliminar items
- ✅ Expiración automática de carritos
- ✅ Validación de stock
- ✅ Generación de órdenes
- ✅ Límite de 10 tickets
- ✅ Integración con payment-service

---

## 🔍 Troubleshooting

### Error: "Cart expired"
**Causa:** El carrito superó los 10 minutos de inactividad  
**Solución:** Crear nuevo carrito y agregar items nuevamente

### Error: "Insufficient stock"
**Causa:** No hay stock suficiente en event-service  
**Solución:** Reducir cantidad o seleccionar otro evento

### Error: "Ticket limit exceeded"
**Causa:** Se intentaron comprar más de 10 tickets en una orden  
**Solución:** Dividir la compra en múltiples órdenes

### Error: "No active cart found"
**Causa:** No existe carrito activo para el usuario  
**Solución:** Crear carrito agregando items

### Error: "Cart is empty"
**Causa:** Se intentó hacer checkout con carrito vacío  
**Solución:** Agregar items al carrito antes de checkout

---

## 📚 Documentación Adicional

- [Spring Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [Spring WebFlux WebClient](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

---

## 📞 Contacto

Para reportar problemas o sugerencias relacionadas con ORDER-SERVICE, contacta al equipo de desarrollo de PackedGo.

---

**Última actualización:** Diciembre 2025  
**Versión:** 1.0.0
