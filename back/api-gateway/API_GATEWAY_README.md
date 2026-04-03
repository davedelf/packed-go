# 🚪 API-GATEWAY

**⚠️ Estado: ✅ ACTIVO Y FUNCIONAL**

Este servicio actúa como **punto de entrada único** para todos los microservicios de PackedGo, implementando enrutamiento centralizado, autenticación JWT, y gestión de CORS.

## 📋 Descripción

El **API Gateway** es el punto de entrada centralizado para toda la plataforma PackedGo. Implementa las siguientes funcionalidades:

### Funcionalidades Implementadas:
- ✅ **Enrutamiento Centralizado** - Redirección inteligente de peticiones a microservicios
- ✅ **Autenticación JWT** - Validación de tokens antes de enrutar
- ✅ **CORS Handling** - Gestión centralizada de CORS para frontend
- ✅ **Filtros Personalizados** - Filtros para endpoints públicos y protegidos
- ✅ **Request/Response Logging** - Auditoría de peticiones
- ✅ **Retry Logic** - Reintentos automáticos en caso de fallo

### Funcionalidades Planificadas:
- ⏳ **Rate Limiting** - Control de frecuencia de peticiones
- ⏳ **Circuit Breaker** - Resiliencia ante fallos de servicios
- ⏳ **Load Balancing** - Distribución de carga entre instancias

## 🎯 Estado Actual

| Componente | Estado | Notas |
|------------|--------|-------|
| **Estructura de Proyecto** | ✅ Completa | Spring Cloud Gateway implementado |
| **Dependencias** | ✅ Completa | JWT, Lombok, Actuator |
| **Configuración de Rutas** | ✅ Implementada | 7 rutas principales configuradas |
| **Filtros Globales** | ✅ Implementados | AuthenticationFilter, PublicEndpointFilter |
| **Docker** | ✅ Incluido | Incluido en docker-compose.yml |
| **Tests** | ⚠️ Pendiente | Tests unitarios pendientes |

## 🚀 Tecnologías

- **Java 17**
- **Spring Boot 3.5.6**
- **Spring Cloud Gateway** - Enrutamiento reactivo
- **JWT (jjwt 0.12.5)** - Validación de tokens
- **Lombok** - Reducción de boilerplate
- **Spring Actuator** - Health checks y métricas

## 🏗️ Arquitectura Implementada

```
┌─────────────┐
│   Cliente   │
│  (Angular)  │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────┐
│      API GATEWAY            │
│  (Puerto único: 8080)       │
│                             │
│  ┌──────────────────────┐  │
│  │ JWT Validation       │  │
│  │ CORS Handler         │  │
│  │ Route Filters        │  │
│  │ Retry Logic          │  │
│  └──────────────────────┘  │
└──────────┬──────────────────┘
           │
    ┌──────┴──────┬──────────┬─────────┬──────────┬──────────┐
    ▼             ▼          ▼         ▼          ▼          ▼
┌────────┐  ┌─────────┐ ┌────────┐ ┌──────┐ ┌─────────┐ ┌──────────┐
│  Auth  │  │  Users  │ │ Event  │ │Order │ │ Payment │ │Analytics │
│  8081  │  │  8082   │ │  8086  │ │ 8084 │ │  8085   │ │   8087   │
└────────┘  └─────────┘ └────────┘ └──────┘ └─────────┘ └──────────┘
```

## 📝 Rutas Configuradas

### Endpoints Públicos (Sin autenticación JWT)

| Ruta | Servicio Destino | Puerto | Filtro |
|------|------------------|--------|--------|
| `/api/auth/**` | auth-service | 8081 | PublicEndpointFilter |
| `/api/events` | event-service | 8086 | PublicEndpointFilter |
| `/api/events/{id}` | event-service | 8086 | PublicEndpointFilter |
| `/api/consumptions/event/**` | event-service | 8086 | PublicEndpointFilter |
| `/api/payments/health` | payment-service | 8085 | PublicEndpointFilter |
| `/api/webhooks/stripe` | payment-service | 8085 | PublicEndpointFilter |

### Endpoints Protegidos (Requieren JWT)

| Ruta | Servicio Destino | Puerto | Filtro |
|------|------------------|--------|--------|
| `/api/user-profiles/**` | users-service | 8082 | AuthenticationFilter |
| `/api/admin/employees/**` | users-service | 8082 | AuthenticationFilter |
| `/api/employee/**` | users-service | 8082 | AuthenticationFilter |
| `/api/events/**` | event-service | 8086 | AuthenticationFilter |
| `/api/event-categories/**` | event-service | 8086 | AuthenticationFilter |
| `/api/consumptions/**` | event-service | 8086 | AuthenticationFilter |
| `/api/passes/**` | event-service | 8086 | AuthenticationFilter |
| `/api/tickets/**` | event-service | 8086 | AuthenticationFilter |
| `/api/cart/**` | order-service | 8084 | AuthenticationFilter |
| `/api/orders/**` | order-service | 8084 | AuthenticationFilter |
| `/api/payments/**` | payment-service | 8085 | AuthenticationFilter |
| `/api/dashboard/**` | analytics-service | 8087 | AuthenticationFilter |

## 🔧 Configuración

### Variables de Entorno

Crear archivo `.env` basado en `.env.example`:

```bash
# JWT Secret (debe coincidir con auth-service)
JWT_SECRET=your_super_secret_key_change_this_in_production_2024

# URLs de servicios (auto-configuradas en Docker)
AUTH_SERVICE_URL=http://auth-service:8081
USERS_SERVICE_URL=http://users-service:8082
EVENT_SERVICE_URL=http://event-service:8086
ORDER_SERVICE_URL=http://order-service:8084
PAYMENT_SERVICE_URL=http://payment-service:8085
ANALYTICS_SERVICE_URL=http://analytics-service:8087
```

### Filtros Implementados

#### AuthenticationFilter
- **Propósito**: Validar JWT en endpoints protegidos
- **Lógica**:
  1. Extrae token del header `Authorization: Bearer <token>`
  2. Valida expiración y firma del token
  3. Agrega headers `X-User-Id` y `X-User-Role` al request downstream
  4. Retorna 401 si el token es inválido

#### PublicEndpointFilter
- **Propósito**: Permitir acceso sin autenticación
- **Uso**: Login, registro, webhooks, listados públicos

### CORS

```yaml
# Configurado para permitir frontend Angular
globalcors:
  cors-configurations:
    '[/**]':
      allowedOrigins: "http://localhost:4200"
      allowedMethods: "*"
      allowedHeaders: "*"
      allowCredentials: true
```

## 🚀 Uso

### Compilar

```bash
cd api-gateway
./mvnw clean package
```

### Ejecutar Localmente

```bash
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```

### Ejecutar con Docker

```bash
# Desde la carpeta back/
docker-compose up -d api-gateway
```

### Probar Endpoint Público

```bash
# Listar eventos (sin autenticación)
curl http://localhost:8080/api/events
```

### Probar Endpoint Protegido

```bash
# 1. Login para obtener token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/customer/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}' \
  | jq -r '.token')

# 2. Usar token para acceso protegido
curl http://localhost:8080/api/user-profiles/me \
  -H "Authorization: Bearer $TOKEN"
```

## ⚙️ Servicios a Enrutar

| Servicio | Puerto | Prefijo de Ruta |
|----------|--------|----------------|
| Auth Service | 8081 | `/api/auth/**` |
| Users Service | 8082 | `/api/users/**` |
| Event Service | 8086 | `/api/events/**` |
| Order Service | 8084 | `/api/orders/**` |
| Payment Service | 8085 | `/api/payments/**` |
| Analytics Service | 8087 | `/api/analytics/**` |

## 📚 Referencias

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [Spring Cloud Gateway Samples](https://github.com/spring-cloud-samples/spring-cloud-gateway-sample)

---

## 🔒 Configuración de CORS

### ⚠️ IMPORTANTE: CORS Centralizado

**El API Gateway es el ÚNICO punto que gestiona CORS.** Esta configuración es crítica para evitar el error de headers duplicados.

#### Configuración en API Gateway

```yaml
# application.yml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "http://localhost:3000"  # Frontend Angular
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - PATCH
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600
```

#### Estado en Microservicios

**TODOS los microservicios tienen CORS DESHABILITADO:**

✅ **analytics-service**: 
- ❌ Sin Spring Security
- ❌ Sin @CrossOrigin en DashboardController

✅ **payment-service**:
- `.cors(cors -> cors.disable())` en SecurityConfig
- ❌ Sin @CrossOrigin en controladores

✅ **users-service**:
- `.cors(cors -> cors.disable())` en SecurityConfig
- ❌ Sin @CrossOrigin en controladores
- ❌ CorsConfig.java ELIMINADO

✅ **event-service**:
- ❌ CorsConfig.java ELIMINADO

✅ **order-service**:
- ❌ CorsConfig.java ELIMINADO
- ❌ Sin @CrossOrigin en OrderController
- ❌ Sin @CrossOrigin en CartController

✅ **auth-service**:
- `.cors(cors -> cors.disable())` en SecurityConfig

**Beneficio**: Evita el error "Access-Control-Allow-Origin: http://localhost:3000, http://localhost:3000" (headers duplicados).

---

## 💡 Beneficios Esperados

1. **Punto de Entrada Único** - Frontend solo necesita conocer un endpoint
2. **Seguridad Centralizada** - Validación JWT en un solo lugar
3. **Mejor Observabilidad** - Logs y métricas centralizadas
4. **Resiliencia** - Circuit breakers y fallbacks automáticos
5. **Escalabilidad** - Load balancing integrado
