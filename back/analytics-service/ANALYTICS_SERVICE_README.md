# 📊 ANALYTICS-SERVICE - Servicio de Estadísticas y Dashboard

## 📋 Descripción General

El **ANALYTICS-SERVICE** es el microservicio encargado de generar dashboards y estadísticas en tiempo real para organizadores de eventos en PackedGo. Consolida información de múltiples servicios (event-service, order-service, payment-service) para proporcionar métricas clave sobre eventos, ventas, ingresos y rendimiento.

### 🎯 Características Principales

- 📊 **Dashboard unificado** con métricas de eventos, ventas e ingresos
- 🔄 **Integración con múltiples servicios** mediante WebClient
- 🔐 **Autenticación JWT** con validación de roles (ADMIN/SUPER_ADMIN)
- 📈 **Estadísticas en tiempo real** sin almacenamiento persistente
- 👥 **Multi-tenant** por organizador
- 🎯 **Métricas consolidadas** de eventos, tickets, consumiciones y pagos

---

## 🚀 Configuración de Servicio

| Propiedad | Valor |
|-----------|-------|
| **Puerto HTTP** | 8087 |
| **Puerto Debug (JDWP)** | 5009 |
| **Context Path** | /api |
| **Base URL** | http://localhost:8087/api |

---

## 📦 Base de Datos

**NOTA IMPORTANTE**: Este servicio **NO TIENE BASE DE DATOS PROPIA**. Es un servicio stateless que obtiene todos los datos en tiempo real de otros microservicios:

- **event-service**: Información de eventos, passes, consumiciones
- **order-service**: Información de órdenes y carritos (futuro)
- **payment-service**: Información de pagos y transacciones

---

## 🛠 Tecnologías y Dependencias

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Java** | 17 | Lenguaje base |
| **Spring Boot** | 3.5.6 | Framework principal |
| **Spring WebFlux** | 3.5.6 | Cliente HTTP reactivo (WebClient) |
| **Spring Web** | 3.5.6 | Controladores REST |
| **JWT (jjwt)** | 0.12.6 | Validación de tokens |
| **Lombok** | Latest | Reducción de boilerplate |
| **SLF4J** | Latest | Logging |

### ⚠️ NOTA IMPORTANTE: Seguridad

**Este servicio NO tiene Spring Security configurado.** La autenticación y autorización es manejada completamente por el **API Gateway**:

- El API Gateway valida el JWT
- Extrae `userId` y `role` del token
- Inyecta headers `X-User-Id` y `X-User-Role` en las requests
- analytics-service **confía en estos headers** para identificar al usuario

**CORS está completamente deshabilitado** en este servicio:
- ❌ Sin anotaciones `@CrossOrigin` 
- ❌ Sin clase CorsConfig
- ✅ CORS gestionado únicamente por API Gateway

---

## 📡 API Endpoints

### 📊 Dashboard de Analytics (`/api/dashboard`)

#### **GET** `/api/dashboard`
Obtiene el dashboard completo para el organizador autenticado.

**Headers:**
```http
Authorization: Bearer {token}
```

**Response 200 OK:**
```json
{
  "organizerId": 1,
  "organizerName": "Juan Pérez",
  "totalEvents": 5,
  "activeEvents": 2,
  "completedEvents": 3,
  "totalTicketsSold": 1234,
  "totalRevenue": 450000.00,
  "averageTicketPrice": 365.00,
  "eventStats": [
    {
      "eventId": 1,
      "eventName": "Fiesta de Fin de Año 2025",
      "ticketsSold": 500,
      "capacity": 800,
      "occupancyRate": 62.5,
      "revenue": 200000.00,
      "status": "ACTIVE"
    }
  ],
  "recentOrders": [
    {
      "orderId": "ORD-20251215-001",
      "eventName": "Concierto Rock",
      "quantity": 2,
      "totalAmount": 15000.00,
      "purchaseDate": "2025-12-15T10:30:00"
    }
  ],
  "salesByMonth": {
    "2025-11": 120000.00,
    "2025-12": 330000.00
  },
  "topSellingEvents": [
    {
      "eventId": 1,
      "eventName": "Fiesta de Fin de Año 2025",
      "ticketsSold": 500
    }
  ]
}
```

**Response 403 FORBIDDEN:**
```json
{
  "error": "Acceso denegado. Solo usuarios con rol ADMIN pueden acceder."
}
```

---

#### **GET** `/api/dashboard/{organizerId}`
Obtiene el dashboard de un organizador específico (solo SUPER_ADMIN).

**Headers:**
```http
Authorization: Bearer {token}
```

**Path Parameters:**
- `organizerId` (Long): ID del organizador a consultar

**Response 200 OK:**
```json
{
  "organizerId": 3,
  "organizerName": "María González",
  "totalEvents": 8,
  "activeEvents": 3,
  ...
}
```

**Response 403 FORBIDDEN:**
```json
{
  "error": "Acceso denegado. Solo SUPER_ADMIN puede ver dashboards de otros organizadores."
}
```

---

## 🔐 Seguridad y Autenticación

### ⚠️ Arquitectura de Seguridad

**Este servicio NO realiza validación JWT directamente.** La autenticación es completamente gestionada por el **API Gateway**:

1. **Flujo de Autenticación:**
   ```
   Frontend → API Gateway → Analytics Service
      │            │              │
      │            ├─ Valida JWT
      │            ├─ Extrae userId y role
      │            └─ Inyecta headers:
      │                 X-User-Id: 2
      │                 X-User-Role: ADMIN
      │            
      └─────────────────────────────→ Confía en headers
   ```

2. **Headers Recibidos:**
   - `X-User-Id`: ID del usuario autenticado
   - `X-User-Role`: Rol (ADMIN, CUSTOMER, EMPLOYEE, SUPER_ADMIN)
   - `Authorization`: Token JWT completo (para extracción de claims adicionales si es necesario)

3. **JwtTokenValidator (Opcional):**
   ```java
   // Se mantiene para extraer claims adicionales del token si es necesario
   // PERO la validación de firma y expiración ya fue hecha por el Gateway
   @Component
   public class JwtTokenValidator {
       public Long getUserIdFromToken(String token) {
           // Extrae userId del token (ya validado por Gateway)
       }
       
       public String getRoleFromToken(String token) {
           // Extrae role del token (ya validado por Gateway)
       }
   }
   ```

### Control de Acceso

| Endpoint | Roles Permitidos | Validación |
|----------|-----------------|------------|
| `GET /api/dashboard` | ADMIN, SUPER_ADMIN | Lee X-User-Role header |
| `GET /api/dashboard/{organizerId}` | SUPER_ADMIN | Lee X-User-Role header |

**Ejemplo de Validación en Controller:**
```java
@GetMapping
public ResponseEntity<DashboardDTO> getDashboard(
        @RequestHeader("Authorization") String authorizationHeader
) {
    String token = jwtTokenValidator.extractTokenFromHeader(authorizationHeader);
    Long organizerId = jwtTokenValidator.getUserIdFromToken(token);
    String role = jwtTokenValidator.getRoleFromToken(token);
    
    // El token YA fue validado por el Gateway
    // Solo extraemos los datos necesarios
    
    return ResponseEntity.ok(analyticsService.getDashboard(organizerId));
}
```

---

## 🔄 Integración con Otros Servicios

### Event Service

**Base URL**: `http://event-service:8086`

**Endpoints utilizados:**
- `GET /api/events/organizer/{organizerId}` - Lista todos los eventos del organizador
- `GET /api/events/{eventId}` - Detalles de un evento específico
- `GET /api/passes/event/{eventId}/stats` - Estadísticas de passes vendidos
- `GET /api/consumptions/event/{eventId}/revenue` - Ingresos por consumiciones

### Order Service (Futuro)

**Base URL**: `http://order-service:8084`

**Endpoints planificados:**
- `GET /api/orders/organizer/{organizerId}` - Órdenes del organizador
- `GET /api/orders/recent` - Órdenes recientes
- `GET /api/orders/stats/monthly` - Ventas mensuales

### Payment Service

**Base URL**: `http://payment-service:8085`

**Endpoints utilizados:**
- `GET /api/payments/stats?adminId={organizerId}` - Estadísticas de pagos

---

## 📊 DTOs Principales

### DashboardDTO

```java
@Data
@Builder
public class DashboardDTO {
    private Long organizerId;
    private String organizerName;
    
    // Métricas generales
    private Integer totalEvents;
    private Integer activeEvents;
    private Integer completedEvents;
    
    // Métricas de ventas
    private Integer totalTicketsSold;
    private BigDecimal totalRevenue;
    private BigDecimal averageTicketPrice;
    
    // Estadísticas detalladas
    private List<EventStatsDTO> eventStats;
    private List<RecentOrderDTO> recentOrders;
    private Map<String, BigDecimal> salesByMonth;
    private List<TopEventDTO> topSellingEvents;
}
```

### EventStatsDTO

```java
@Data
@Builder
public class EventStatsDTO {
    private Long eventId;
    private String eventName;
    private Integer ticketsSold;
    private Integer capacity;
    private Double occupancyRate;
    private BigDecimal revenue;
    private String status; // ACTIVE, COMPLETED, CANCELLED
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
analytics-service:
  build:
    context: ./analytics-service
    dockerfile: Dockerfile
  ports:
    - "8087:8087"
    - "5009:5009"  # Debug port
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - EVENT_SERVICE_URL=http://event-service:8086
    - ORDER_SERVICE_URL=http://order-service:8084
    - PAYMENT_SERVICE_URL=http://payment-service:8085
    - JWT_SECRET=${JWT_SECRET}
    - JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5009
  depends_on:
    - event-service
    - payment-service
  networks:
    - packedgo-network
```

---

## ⚙️ Configuración

### application.yml

```yaml
server:
  port: 8087
  servlet:
    context-path: /api

spring:
  application:
    name: analytics-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

# URLs de servicios externos
services:
  event:
    url: ${EVENT_SERVICE_URL:http://localhost:8086}
  order:
    url: ${ORDER_SERVICE_URL:http://localhost:8084}
  payment:
    url: ${PAYMENT_SERVICE_URL:http://localhost:8085}

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:your-secret-key-here}
  expiration: 86400000 # 24 horas

# Logging
logging:
  level:
    com.packed_go.analytics_service: INFO
    org.springframework.web: INFO
```

---

## 🚦 Flujo de Operación

### Generación de Dashboard

```
1. Frontend envía GET /api/dashboard con JWT
   ↓
2. DashboardController extrae y valida token
   ↓
3. Verifica rol ADMIN o SUPER_ADMIN
   ↓
4. AnalyticsService.generateDashboard(organizerId, token)
   ↓
5. WebClient hace llamadas paralelas:
   - event-service: GET /api/events/organizer/{organizerId}
   - payment-service: GET /api/payments/stats?adminId={organizerId}
   ↓
6. Procesa y agrega datos:
   - Calcula totalEvents, activeEvents, completedEvents
   - Suma totalTicketsSold, totalRevenue
   - Calcula averageTicketPrice
   - Genera eventStats con occupancyRate
   ↓
7. Retorna DashboardDTO con todos los datos
```

---

## 📈 Métricas Calculadas

### Métricas Básicas

| Métrica | Cálculo | Fuente |
|---------|---------|--------|
| **Total Events** | COUNT(eventos) | event-service |
| **Active Events** | COUNT(eventos WHERE status=ACTIVE) | event-service |
| **Completed Events** | COUNT(eventos WHERE status=COMPLETED) | event-service |
| **Total Tickets Sold** | SUM(tickets vendidos) | event-service |
| **Total Revenue** | SUM(ingresos pagos) | payment-service |
| **Average Ticket Price** | totalRevenue / totalTicketsSold | Calculado |

### Métricas Avanzadas

| Métrica | Cálculo | Descripción |
|---------|---------|-------------|
| **Occupancy Rate** | (ticketsSold / capacity) * 100 | % de ocupación por evento |
| **Sales Trend** | Agrupación por mes | Ingresos mensuales |
| **Top Events** | ORDER BY ticketsSold DESC | Eventos más vendidos |

---

## 🧪 Testing

### Casos de Prueba

#### Autenticación
- [ ] Usuario con rol ADMIN puede acceder a su dashboard
- [ ] Usuario con rol SUPER_ADMIN puede acceder a cualquier dashboard
- [ ] Usuario con rol CUSTOMER no puede acceder
- [ ] Token inválido retorna 403
- [ ] Token expirado retorna 403

#### Funcionalidad
- [ ] Dashboard retorna datos correctos del organizador
- [ ] Métricas se calculan correctamente
- [ ] Integración con event-service funciona
- [ ] Integración con payment-service funciona
- [ ] Maneja errores de servicios externos

#### Performance
- [ ] Respuesta en < 2 segundos con 10 eventos
- [ ] Respuesta en < 5 segundos con 100 eventos
- [ ] Llamadas paralelas a servicios externos

---

## 🔍 Troubleshooting

### Problemas Comunes

#### Error: "Token JWT no proporcionado"
**Solución**: Asegúrate de incluir el header `Authorization: Bearer {token}`

#### Error: "Acceso denegado"
**Solución**: Verifica que el usuario tenga rol ADMIN o SUPER_ADMIN

#### Error: "Service unavailable"
**Solución**: 
1. Verifica que event-service esté corriendo
2. Verifica que payment-service esté corriendo
3. Revisa las URLs de servicios en application.yml

#### Dashboard vacío o con datos incorrectos
**Solución**:
1. Verifica que el organizador tenga eventos creados
2. Revisa logs de analytics-service
3. Prueba endpoints de event-service directamente

---

## 📝 Próximas Mejoras

### Fase 1: Caché
- [ ] Implementar Redis para cachear resultados
- [ ] TTL de 5 minutos para dashboards
- [ ] Invalidación de caché al crear/actualizar eventos

### Fase 2: Métricas Avanzadas
- [ ] Análisis de tendencias de ventas
- [ ] Predicción de ocupación
- [ ] Comparativa entre eventos similares
- [ ] Heatmap de horarios de compra

### Fase 3: Exportación
- [ ] Exportar dashboard a PDF
- [ ] Exportar estadísticas a Excel
- [ ] Envío programado de reportes por email

### Fase 4: Monitoreo
- [ ] Integrar con Spring Actuator
- [ ] Métricas de latencia de llamadas externas
- [ ] Health checks de servicios dependientes
- [ ] Alertas por degradación de servicios

---

## 📚 Referencias

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [Spring Security](https://spring.io/projects/spring-security)

---

## 👥 Equipo de Desarrollo

Desarrollado como parte del ecosistema de microservicios **PackedGo**.

**Última actualización**: 15 de diciembre de 2025
