# 🎯 PackedGo Backend - Sistema de Gestión de Eventos

> Plataforma completa de microservicios para la gestión de eventos, venta de tickets, procesamiento de pagos y analytics en tiempo real.

**Versión**: 2.1  
**Última Actualización**: 15 de Diciembre de 2025  
**Estado**: ✅ Sistema Completamente Operativo

---

## 📚 Documentación Principal

### 🔍 Inicio Rápido

**¿Primera vez en el proyecto?** Lee primero:
1. **[TECHNICAL_DOCUMENTATION.md](TECHNICAL_DOCUMENTATION.md)** - 📖 Documentación técnica completa del sistema

### 📁 Documentación por Microservicio

| Servicio | Puerto | README | Responsabilidad |
|----------|--------|--------|-----------------|
| **API Gateway** | 8080 | [README](api-gateway/API_GATEWAY_README.md) | Enrutamiento, JWT, CORS |
| **auth-service** | 8081 | [README](auth-service/AUTH_SERVICE_README.md) | Autenticación y usuarios |
| **users-service** | 8082 | [README](users-service/USERS_SERVICE_README.md) | Perfiles y empleados |
| **event-service** | 8086 | [README](event-service/EVENT_SERVICE_README.md) | Eventos y tickets |
| **order-service** | 8084 | [README](order-service/ORDER_SERVICE_README.md) | Carritos y órdenes |
| **payment-service** | 8085 | [README](payment-service/PAYMENT_SERVICE_README.md) | Pagos con Stripe |
| **analytics-service** | 8087 | [README](analytics-service/ANALYTICS_SERVICE_README.md) | Dashboard y estadísticas |

---

## 🏗️ Arquitectura del Sistema

```
Frontend (Angular - :3000)
         │
         ▼
    API Gateway (:8080) ← ✅ CORS aquí
    ┌────┴────┬────┬────┬────┬────┬────┐
    │         │    │    │    │    │    │
auth-s  users-s  event-s  order-s  payment-s  analytics-s
:8081   :8082    :8086    :8084    :8085      :8087
  │       │        │        │        │           │
auth-db users-db event-db order-db payment-db  (stateless)
:5433   :5434    :5435    :5436    :5437
```

### 🔑 Características Clave

- ✅ **API Gateway centralizado** con Spring Cloud Gateway
- ✅ **Autenticación JWT** (HS256, 1 hora expiración)
- ✅ **CORS configurado SOLO en Gateway** (localhost:3000)
- ✅ **Multi-tenant** por organizador
- ✅ **Procesamiento de pagos** con Stripe
- ✅ **Validación QR** de tickets
- ✅ **Analytics en tiempo real** sin base de datos
- ✅ **Docker Compose** para orquestación completa

---

## 🚀 Inicio Rápido

### Pre-requisitos

- Java 17+
- Maven 3.9+
- Docker Desktop
- PostgreSQL 15 (opcional, incluido en Docker)
- Node.js 18+ (para frontend)

### Instalación

```bash
# 1. Clonar repositorio
cd C:\Users\david\Documents\ps-packedgo\packedgo\back

# 2. Copiar archivo de configuración
cp .env.example .env
# Editar .env con tus credenciales (Stripe, SMTP, JWT_SECRET)

# 3. Iniciar bases de datos
docker-compose up -d auth-db users-db event-db order-db payment-db

# 4. Compilar servicios
cd auth-service && mvn clean package -DskipTests && cd ..
cd users-service && mvn clean package -DskipTests && cd ..
cd event-service && mvn clean package -DskipTests && cd ..
cd order-service && mvn clean package -DskipTests && cd ..
cd payment-service && mvn clean package -DskipTests && cd ..
cd analytics-service && mvn clean package -DskipTests && cd ..
cd api-gateway && mvn clean package -DskipTests && cd ..

# 5. Iniciar todos los servicios
docker-compose up -d

# 6. Verificar que están corriendo
docker-compose ps

# 7. Verificar health
curl http://localhost:8080/actuator/health
```

### Verificación Rápida

```bash
# Test de endpoint público
curl http://localhost:8080/api/events

# Login de admin
curl -X POST http://localhost:8080/api/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@test.com", "password": "Admin123!"}'

# Copiar el token de la respuesta y usarlo:
TOKEN="eyJhbGc..."

# Test de dashboard
curl http://localhost:8080/api/dashboard \
  -H "Authorization: Bearer $TOKEN"
```

**Respuesta esperada**: 200 OK con JSON del dashboard.

---

## 🔧 Configuración Importante

### ⚠️ CORS - Configuración Crítica

**IMPORTANTE**: CORS está configurado ÚNICAMENTE en el API Gateway.

| Componente | CORS | Configuración |
|------------|------|---------------|
| **API Gateway** | ✅ Habilitado | `allowedOrigins: http://localhost:3000` |
| auth-service | ❌ Deshabilitado | `.cors(cors -> cors.disable())` |
| users-service | ❌ Deshabilitado | `.cors(cors -> cors.disable())` |
| payment-service | ❌ Deshabilitado | `.cors(cors -> cors.disable())` |
| event-service | ❌ Deshabilitado | Sin CorsConfig.java |
| order-service | ❌ Deshabilitado | Sin CorsConfig.java |
| analytics-service | ❌ Deshabilitado | Sin Spring Security |

**No agregar**:
- ❌ Archivos `CorsConfig.java` en microservicios
- ❌ Anotaciones `@CrossOrigin` en controllers
- ❌ Configuración CORS en `SecurityConfig` de microservicios

### 🔐 Seguridad - Flujo de Autenticación

```
1. Frontend → POST /api/auth/admin/login
2. auth-service → Valida credenciales → Genera JWT
3. Frontend → Almacena token → Envía en header Authorization

4. Frontend → GET /api/dashboard con Authorization: Bearer {token}
5. API Gateway → Valida JWT → Inyecta headers X-User-Id, X-User-Role
6. analytics-service → Lee headers → Aplica lógica de negocio → Responde
```

**Spring Security por servicio**:
- ✅ auth-service: Habilitado (maneja login)
- ✅ users-service: Habilitado (gestión de perfiles)
- ✅ payment-service: Habilitado (procesamiento de pagos)
- ❌ event-service: No habilitado
- ❌ order-service: No habilitado
- ❌ **analytics-service: SIN Spring Security** (confía 100% en Gateway)

---

## 🐳 Docker Commands

```bash
# Iniciar todo
docker-compose up -d

# Ver logs
docker-compose logs -f
docker logs back-analytics-service-1 --tail 100 -f

# Reiniciar servicio
docker-compose restart analytics-service

# Rebuild servicio
docker-compose build --no-cache analytics-service
docker-compose up -d analytics-service

# Detener todo
docker-compose down

# Limpiar todo (⚠️ elimina datos)
docker-compose down -v
```

---

## 🔍 Troubleshooting

### Error: Duplicate CORS headers

**Síntoma**: `Access-Control-Allow-Origin: http://localhost:3000, http://localhost:3000`

**Solución**: 
- CORS debe estar SOLO en API Gateway
- Deshabilitar CORS en TODOS los microservicios
- Ver [TECHNICAL_DOCUMENTATION.md](TECHNICAL_DOCUMENTATION.md#troubleshooting) para detalles

### Error: 403 Forbidden en /api/dashboard

**Causas posibles**:
1. Spring Security bloqueando (ver logs para "Using generated security password")
2. Anotación `@CrossOrigin` con puerto incorrecto
3. JWT expirado o inválido

**Solución**: Ver sección completa de troubleshooting en [TECHNICAL_DOCUMENTATION.md](TECHNICAL_DOCUMENTATION.md#troubleshooting)

### Error: Cannot connect to database

**Solución**:
```bash
# Verificar que bases de datos están corriendo
docker-compose ps | grep db

# Reiniciar bases de datos
docker-compose restart auth-db users-db event-db order-db payment-db
```

---

## 📊 Endpoints Principales

### Públicos (sin JWT)

- `POST /api/auth/customer/login` - Login de clientes
- `POST /api/auth/admin/login` - Login de administradores
- `POST /api/auth/customer/register` - Registro de clientes
- `POST /api/auth/admin/register` - Registro de administradores
- `GET /api/events` - Listado de eventos públicos
- `GET /api/events/{id}` - Detalle de evento
- `POST /api/webhooks/stripe` - Webhook de Stripe

### Protegidos (requieren JWT)

- `GET /api/dashboard` - Dashboard de analytics (ADMIN)
- `POST /api/events` - Crear evento (ADMIN)
- `GET /api/user-profiles/me` - Perfil del usuario autenticado
- `POST /api/cart/add` - Agregar al carrito (CUSTOMER)
- `POST /api/payments/create-payment-intent` - Crear intención de pago

Ver documentación completa de endpoints en cada README de servicio.

---

## 🧪 Testing

```bash
# Unit tests de un servicio
cd auth-service
mvn test

# Integration tests
mvn verify

# Cobertura de código
mvn test jacoco:report
```

---

## 📈 Monitoreo

### Actuator Endpoints

```bash
curl http://localhost:8081/actuator/health  # auth-service
curl http://localhost:8082/actuator/health  # users-service
curl http://localhost:8086/actuator/health  # event-service
curl http://localhost:8084/actuator/health  # order-service
curl http://localhost:8085/actuator/health  # payment-service
curl http://localhost:8087/actuator/health  # analytics-service
curl http://localhost:8080/actuator/health  # api-gateway
```

### Logs

```bash
# Ver logs en tiempo real
docker-compose logs -f analytics-service

# Últimas 100 líneas
docker logs back-analytics-service-1 --tail 100
```

---

## 📚 Recursos Adicionales

### Documentación Técnica

- **[TECHNICAL_DOCUMENTATION.md](TECHNICAL_DOCUMENTATION.md)** - Documentación completa del sistema (incluye arquitectura, despliegue, seguridad, troubleshooting y roadmap)

### READMEs de Servicios

- [API Gateway](api-gateway/API_GATEWAY_README.md)
- [Auth Service](auth-service/AUTH_SERVICE_README.md)
- [Users Service](users-service/USERS_SERVICE_README.md)
- [Event Service](event-service/EVENT_SERVICE_README.md)
- [Order Service](order-service/ORDER_SERVICE_README.md)
- [Payment Service](payment-service/PAYMENT_SERVICE_README.md)
- [Analytics Service](analytics-service/ANALYTICS_SERVICE_README.md)

### Enlaces Externos

- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [JWT.io](https://jwt.io/)
- [Stripe API](https://stripe.com/docs/api)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)
- [Docker Compose](https://docs.docker.com/compose/)

---

## 🎯 Roadmap

### ✅ Completado (Diciembre 2025)

- API Gateway con enrutamiento completo
- CORS centralizado (sin duplicación)
- JWT validation en Gateway
- Analytics sin Spring Security
- Header injection (X-User-Id, X-User-Role)
- Documentación completa actualizada
- Troubleshooting guide

### 🔜 Próximas Mejoras

- Rate limiting en API Gateway
- Circuit breaker con Resilience4j
- Métricas con Prometheus + Grafana
- Distributed tracing (Zipkin/Jaeger)
- Redis para caché
- Alta disponibilidad con réplicas
- Tests end-to-end

Ver roadmap completo en [TECHNICAL_DOCUMENTATION.md](TECHNICAL_DOCUMENTATION.md#próximas-mejoras-del-sistema)

---

##  Licencia

Propiedad de PackedGo. Todos los derechos reservados.
