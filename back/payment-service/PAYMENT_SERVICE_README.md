# 💳 PAYMENT-SERVICE - Servicio de Pasarela de Pagos

## 📋 Descripción General

El **PAYMENT-SERVICE** es el microservicio encargado de gestionar toda la lógica de pagos en PackedGo mediante integración con **Stripe**. Maneja la creación de sesiones de checkout, procesamiento de pagos, webhooks de confirmación y estadísticas de transacciones. Actúa como intermediario entre order-service y Stripe para garantizar transacciones seguras.

### 🎯 Características Principales

- 💳 **Integración con Stripe Checkout** para pagos seguros
- 🔔 **Webhooks de Stripe** para confirmación automática de pagos
- 🔐 **Autenticación JWT** en endpoints sensibles
- 📊 **Estadísticas de pagos** por administrador
- 🗄️ **Persistencia de transacciones** en base de datos PostgreSQL
- 🔄 **Verificación manual** de estado de pago
- 🛡️ **Verificación de firma** en webhooks para seguridad
- 💰 **Soporte multi-moneda** (principalmente ARS)

---

## 🚀 Configuración de Servicio

| Propiedad | Valor |
|-----------|-------|
| **Puerto HTTP** | 8085 |
| **Puerto Debug (JDWP)** | 5010 |
| **Context Path** | /api |
| **Base URL** | http://localhost:8085/api |

---

## 📦 Base de Datos

### Configuración PostgreSQL

| Propiedad | Valor |
|-----------|-------|
| **Nombre** | payment_db |
| **Puerto** | 5437 → 5432 (Docker) |
| **Usuario** | payment_user |
| **Contraseña** | payment_password |
| **Imagen Docker** | postgres:15-alpine |
| **Timezone** | America/Argentina/Buenos_Aires |

### 📊 Tabla Principal

#### `payments`
```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    order_id VARCHAR(255) UNIQUE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
    status VARCHAR(50) NOT NULL, -- PENDING, APPROVED, REJECTED, CANCELLED
    payment_method VARCHAR(100),
    payer_email VARCHAR(255),
    payer_name VARCHAR(255),
    description VARCHAR(500),
    
    -- Stripe fields
    stripe_session_id VARCHAR(255),
    stripe_payment_intent_id VARCHAR(255),
    payment_provider VARCHAR(50) DEFAULT 'STRIPE',
    transaction_amount DECIMAL(10,2),
    status_detail VARCHAR(100),
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    
    -- Indexes
    INDEX idx_payments_order_id (order_id),
    INDEX idx_payments_stripe_session_id (stripe_session_id),
    INDEX idx_payments_status (status)
);
```

### Enum: PaymentStatus

```java
public enum PaymentStatus {
    PENDING,     // Pago iniciado, esperando confirmación
    APPROVED,    // Pago aprobado por Stripe
    REJECTED,    // Pago rechazado
    CANCELLED    // Pago cancelado por el usuario
}
```

---

## 🛠 Tecnologías y Dependencias

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Java** | 17 | Lenguaje base |
| **Spring Boot** | 3.5.7 | Framework principal |
| **Spring Data JPA** | 3.5.7 | Persistencia de datos |
| **Spring Security** | 3.5.7 | Seguridad y autenticación |
| **Spring Validation** | 3.5.7 | Validación de datos |
| **Stripe Java SDK** | 26.7.0 | Integración con Stripe |
| **Gson** | Latest | Procesamiento JSON |
| **PostgreSQL Driver** | 42.x | Driver JDBC |
| **Lombok** | Latest | Reducción de boilerplate |
| **JWT** | Latest | Validación de tokens |

---

## 📡 API Endpoints

### 💳 Gestión de Pagos (`/api/payments`)

#### **POST** `/api/payments/create-checkout-stripe`
Crea una sesión de Stripe Checkout para procesar el pago.

**Headers:**
```http
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "adminId": 1,
  "orderId": "ORD-20251215-001",
  "amount": 15000.00,
  "description": "Compra de 2 tickets para Fiesta de Fin de Año"
}
```

**Response 201 CREATED:**
```json
{
  "success": true,
  "message": "Sesión de pago creada exitosamente",
  "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_...",
  "sessionId": "cs_test_a1b2c3d4e5f6...",
  "orderId": "ORD-20251215-001",
  "amount": 15000.00,
  "status": "PENDING"
}
```

**Errores:**
- `400 BAD REQUEST`: Datos inválidos en el request
- `401 UNAUTHORIZED`: Token JWT inválido o expirado
- `500 INTERNAL SERVER ERROR`: Error al crear sesión en Stripe

---

#### **POST** `/api/payments/verify/{orderId}`
Verifica manualmente el estado del pago (en caso de fallo de webhook).

**Path Parameters:**
- `orderId` (String): ID de la orden a verificar

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Pago verificado exitosamente",
  "orderId": "ORD-20251215-001",
  "status": "APPROVED",
  "amount": 15000.00,
  "paidAt": "2025-12-15T10:45:30"
}
```

**Posibles estados:**
- `PENDING`: Pago aún no completado
- `APPROVED`: Pago confirmado
- `REJECTED`: Pago rechazado
- `CANCELLED`: Pago cancelado

---

#### **GET** `/api/payments/stats`
Obtiene estadísticas de pagos del administrador autenticado.

**Headers:**
```http
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "totalPayments": 150,
  "totalRevenue": 2250000.00,
  "approvedPayments": 142,
  "pendingPayments": 5,
  "rejectedPayments": 3,
  "averageTicketValue": 15000.00,
  "lastUpdated": "2025-12-15T11:30:00"
}
```

---

#### **GET** `/api/payments/health`
Health check del servicio.

**Response 200 OK:**
```json
{
  "status": "UP",
  "service": "payment-gateway",
  "version": "2.0.0",
  "provider": "Stripe"
}
```

---

### 🔔 Webhooks de Stripe (`/api/webhooks`)

#### **POST** `/api/webhooks/stripe`
Endpoint para recibir notificaciones de Stripe (uso interno).

**IMPORTANTE**: Este endpoint NO requiere autenticación JWT, pero VERIFICA la firma de Stripe.

**Headers:**
```http
Stripe-Signature: {signature}
Content-Type: application/json
```

**Eventos soportados:**
- `checkout.session.completed`: Sesión de pago completada exitosamente

**Proceso:**
1. Stripe envía webhook con evento
2. StripeWebhookController verifica firma usando `webhookSecret`
3. Si es `checkout.session.completed`:
   - Extrae `sessionId` del evento
   - Llama a `paymentService.handleStripePaymentSuccess(sessionId)`
   - Actualiza payment status a `APPROVED`
   - Marca `paidAt` con timestamp actual
4. Retorna `200 OK` a Stripe

**Response:**
```
Webhook processed
```

---

## 🔐 Seguridad y Autenticación

### Validación JWT

```java
@Component
public class JwtTokenValidator {
    
    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        throw new RuntimeException("Token JWT no proporcionado");
    }
    
    public boolean validateToken(String token) {
        // Valida expiración y firma del token
    }
    
    public Long getUserIdFromToken(String token) {
        // Extrae el userId del claim
    }
}
```

### Configuración de Stripe

```java
@Configuration
public class StripeConfig {
    
    @Value("${stripe.api.key}")
    private String stripeApiKey;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }
}
```

---

## 🔄 Integración con Stripe

### Flujo de Pago Completo

```
1. Frontend/Order-Service llama POST /api/payments/create-checkout-stripe
   ↓
2. PaymentService crea Payment con status PENDING
   ↓
3. PaymentService llama Stripe API:
   - SessionCreateParams con line_items, success_url, cancel_url
   - Stripe.checkout.sessions.create()
   ↓
4. Stripe retorna Session con URL de checkout
   ↓
5. PaymentService guarda stripeSessionId en Payment
   ↓
6. Retorna PaymentResponse con checkoutUrl al frontend
   ↓
7. Frontend redirige usuario a Stripe Checkout
   ↓
8. Usuario completa pago en Stripe
   ↓
9. Stripe envía webhook POST /api/webhooks/stripe
   ↓
10. StripeWebhookController verifica firma
    ↓
11. Si evento es checkout.session.completed:
    - Busca Payment por stripeSessionId
    - Actualiza status a APPROVED
    - Guarda paidAt timestamp
    - Persiste en BD
    ↓
12. Stripe redirige usuario a success_url
    ↓
13. Frontend puede verificar pago llamando POST /payments/verify/{orderId}
```

### Configuración de Stripe en application.yml

```yaml
stripe:
  api:
    key: ${STRIPE_API_KEY:sk_test_...}
  webhook:
    secret: ${STRIPE_WEBHOOK_SECRET:whsec_...}
  
  # URLs de redirección
  success-url: ${FRONTEND_URL:http://localhost:4200}/payment/success?session_id={CHECKOUT_SESSION_ID}
  cancel-url: ${FRONTEND_URL:http://localhost:4200}/payment/cancel
```

---

## 📊 DTOs Principales

### PaymentRequest

```java
@Data
@Builder
public class PaymentRequest {
    @NotNull(message = "Admin ID es requerido")
    private Long adminId;
    
    @NotBlank(message = "Order ID es requerido")
    private String orderId;
    
    @NotNull(message = "Monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;
    
    private String description;
}
```

### PaymentResponse

```java
@Data
@Builder
public class PaymentResponse {
    private Boolean success;
    private String message;
    private String checkoutUrl;
    private String sessionId;
    private String orderId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paidAt;
}
```

### PaymentStatsDTO

```java
@Data
@Builder
public class PaymentStatsDTO {
    private Integer totalPayments;
    private BigDecimal totalRevenue;
    private Integer approvedPayments;
    private Integer pendingPayments;
    private Integer rejectedPayments;
    private BigDecimal averageTicketValue;
    private LocalDateTime lastUpdated;
}
```

---

## 🐳 Configuración Docker

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
ARG JAR_FILE=/workspace/app/target/*.jar
COPY --from=build ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### docker-compose.yml

```yaml
payment-service:
  build:
    context: ./payment-service
    dockerfile: Dockerfile
  ports:
    - "8085:8085"
    - "5010:5010"  # Debug port
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - STRIPE_API_KEY=${STRIPE_API_KEY}
    - STRIPE_WEBHOOK_SECRET=${STRIPE_WEBHOOK_SECRET}
    - FRONTEND_URL=http://localhost:4200
    - JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5010
  depends_on:
    payment-db:
      condition: service_healthy
  networks:
    - packedgo-network

payment-db:
  image: postgres:15-alpine
  environment:
    POSTGRES_DB: payment_db
    POSTGRES_USER: payment_user
    POSTGRES_PASSWORD: payment_password
  ports:
    - "5437:5432"
  volumes:
    - payment_db_data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U payment_user -d payment_db"]
    interval: 30s
    timeout: 10s
    retries: 3
  networks:
    - packedgo-network
```

---

## ⚙️ Variables de Entorno

| Variable | Descripción | Ejemplo | Requerida |
|----------|-------------|---------|-----------|
| `STRIPE_API_KEY` | API Key de Stripe (secret key) | `sk_test_...` | ✅ |
| `STRIPE_WEBHOOK_SECRET` | Secret para validar webhooks | `whsec_...` | ✅ |
| `FRONTEND_URL` | URL del frontend para redirecciones | `http://localhost:4200` | ✅ |
| `JWT_SECRET` | Secret para validar JWT | `your-secret-key` | ✅ |
| `SPRING_DATASOURCE_URL` | URL de PostgreSQL | `jdbc:postgresql://...` | ✅ |

---

## 🧪 Testing

### Casos de Prueba

#### Creación de Pago
- [ ] Crear sesión Stripe con datos válidos retorna checkoutUrl
- [ ] Request sin adminId retorna 400
- [ ] Request sin orderId retorna 400
- [ ] Request con amount = 0 retorna 400
- [ ] Token JWT inválido retorna 401
- [ ] Error de Stripe retorna 500 con mensaje descriptivo

#### Webhooks
- [ ] Webhook con firma válida se procesa correctamente
- [ ] Webhook sin header Stripe-Signature retorna 400
- [ ] Webhook con firma inválida retorna 400
- [ ] Evento checkout.session.completed actualiza payment a APPROVED
- [ ] Eventos no soportados se ignoran sin error

#### Verificación
- [ ] Verificar pago PENDING retorna estado correcto
- [ ] Verificar pago APPROVED retorna paidAt
- [ ] Verificar orderId inexistente retorna error

#### Estadísticas
- [ ] Stats retorna contadores correctos
- [ ] Stats filtra por adminId del token JWT
- [ ] Stats sin token retorna 401

---

## 🔍 Troubleshooting

### Problemas Comunes

#### Error: "Invalid Stripe API Key"
**Solución**: 
1. Verifica que `STRIPE_API_KEY` esté configurado
2. Usa `sk_test_...` para testing, `sk_live_...` para producción
3. Revisa que la clave sea válida en Stripe Dashboard

#### Error: "Webhook signature verification failed"
**Solución**:
1. Verifica `STRIPE_WEBHOOK_SECRET` en variables de entorno
2. En Stripe Dashboard → Developers → Webhooks → Secret
3. Usa `whsec_...` proporcionado por Stripe
4. En local, usa Stripe CLI para túnel: `stripe listen --forward-to localhost:8085/api/webhooks/stripe`

#### Pago aprobado pero no se actualiza en BD
**Solución**:
1. Revisa logs de `StripeWebhookController`
2. Verifica que webhook esté configurado en Stripe
3. URL del webhook: `https://your-domain.com/api/webhooks/stripe`
4. Evento requerido: `checkout.session.completed`
5. Usa `POST /payments/verify/{orderId}` como fallback

#### CheckoutUrl no funciona
**Solución**:
1. Verifica que `FRONTEND_URL` esté configurado
2. success_url debe ser accesible desde el navegador
3. Prueba la URL manualmente

---

## 📝 Configuración de Stripe Dashboard

### 1. Crear cuenta en Stripe
- Regístrate en https://stripe.com
- Activa modo Test

### 2. Obtener API Keys
- Dashboard → Developers → API Keys
- Copia **Secret Key** (`sk_test_...`)
- Copia **Publishable Key** (`pk_test_...`) para frontend

### 3. Configurar Webhook
- Dashboard → Developers → Webhooks
- Add endpoint: `https://your-domain.com/api/webhooks/stripe`
- Seleccionar evento: `checkout.session.completed`
- Copiar **Signing secret** (`whsec_...`)

### 4. Testing Local con Stripe CLI
```bash
# Instalar Stripe CLI
brew install stripe/stripe-cli/stripe  # macOS
# o descargar desde https://stripe.com/docs/stripe-cli

# Login
stripe login

# Forward webhooks a local
stripe listen --forward-to localhost:8085/api/webhooks/stripe

# Trigger test webhook
stripe trigger checkout.session.completed
```

---

## 📚 Referencias

- [Stripe Checkout Documentation](https://stripe.com/docs/payments/checkout)
- [Stripe Webhooks Guide](https://stripe.com/docs/webhooks)
- [Stripe Java SDK](https://github.com/stripe/stripe-java)
- [Spring Boot with Stripe](https://stripe.com/docs/payments/accept-a-payment?platform=web&ui=checkout)

---

## 🔮 Próximas Mejoras

### Fase 1: Métodos de Pago Adicionales
- [ ] Agregar soporte para MercadoPago (Argentina)
- [ ] Agregar soporte para transferencias bancarias
- [ ] QR de pago con billeteras digitales

### Fase 2: Reportes
- [ ] Exportar reporte de transacciones a PDF
- [ ] Exportar reporte a Excel
- [ ] Gráficos de ingresos mensuales

### Fase 3: Reembolsos
- [ ] Implementar reembolsos parciales
- [ ] Implementar reembolsos totales
- [ ] Notificaciones de reembolso por email

### Fase 4: Seguridad
- [ ] Rate limiting en endpoints de creación de pago
- [ ] Detección de fraude básica
- [ ] Logs de auditoría de transacciones
- [ ] Alertas de pagos sospechosos

---

## 👥 Equipo de Desarrollo

Desarrollado como parte del ecosistema de microservicios **PackedGo**.

**Última actualización**: 15 de diciembre de 2025
