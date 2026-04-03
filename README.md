# 🎯 PackedGo - Sistema de Gestión de Eventos

> Plataforma completa de microservicios para la gestión de eventos, venta de tickets, procesamiento de pagos y analytics en tiempo real.

**Versión**: 2.1  
**Última Actualización**: 15 de Diciembre de 2025  
**Estado**: ✅ Sistema Completamente Operativo

---

## 📋 Tabla de Contenidos

1. [Resumen del Proyecto](#resumen-del-proyecto)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Funcionalidades Principales](#funcionalidades-principales)
4. [Stack Tecnológico](#stack-tecnológico)
5. [Estructura del Proyecto](#estructura-del-proyecto)
6. [Instalación y Configuración](#instalación-y-configuración)
7. [Guía de Uso](#guía-de-uso)
8. [Guía de Contribución](#guía-de-contribución)
9. [Licencia y Créditos](#licencia-y-créditos)

---

## 📝 Resumen del Proyecto

**PackedGo** es una plataforma integral de gestión de eventos desarrollada con arquitectura de microservicios que permite a organizadores crear eventos, vender tickets, gestionar consumiciones y analizar métricas en tiempo real.

### Características Principales

- 🎫 **Gestión de Eventos**: CRUD completo de eventos multi-tenant
- 💳 **Sistema de Pagos**: Integración con Stripe para pagos seguros
- 🛒 **Carrito de Compras**: Sistema de carrito con expiración automática
- 📊 **Analytics en Tiempo Real**: Dashboard con métricas de negocio
- 👷 **Gestión de Empleados**: Sistema de empleados para validación de tickets
- 🔐 **Autenticación JWT**: Sistema robusto con roles diferenciados
- 📱 **Validación QR**: Sistema de validación mediante códigos QR
- 🏢 **Multi-tenant**: Soporte para múltiples organizadores independientes

### Roles de Usuario

| Rol | Descripción | Permisos |
|-----|-------------|----------|
| **CUSTOMER** | Cliente que compra tickets | Comprar tickets, ver perfil, ver eventos |
| **ADMIN** | Organizador de eventos | CRUD eventos, ver estadísticas, gestionar empleados |
| **EMPLOYEE** | Empleado de eventos | Validar tickets, registrar consumos |
| **SUPER_ADMIN** | Administrador del sistema | Acceso completo, ver datos de todos los admins |

---

## 🏗️ Arquitectura del Sistema

### Diagrama de Arquitectura

```
┌──────────────────────────────────────────────────────────────────┐
│                      FRONTEND (Angular)                          │
│                      http://localhost:3000                       │
└────────────────────────────┬─────────────────────────────────────┘
                             │ HTTP + JWT Bearer Token
                             ▼
          ┌────────────────────────────────────────────┐
          │          API GATEWAY (Spring Cloud)         │
          │              Puerto: 8080                   │
          │                                              │
          │  ✅ CORS: allowedOrigins: localhost:3000   │
          │  ✅ JWT Validation (firma + expiración)    │
          │  ✅ Header Injection (X-User-Id, X-User-Role)│
          │  ✅ Public Endpoint Filter                  │
          │  ✅ Route Predicates a microservicios      │
          └─────────────────┬──────────────────────────┘
                            │
     ┌──────────┬───────────┼───────────┬──────────┬─────────┐
     │          │           │           │          │         │
┌────▼───┐ ┌────▼───┐ ┌────▼───┐ ┌────▼───┐ ┌────▼───┐ ┌──────▼────┐
│  Auth  │ │ Users  │ │ Event  │ │ Order  │ │Payment │ │ Analytics │
│Service │ │Service │ │Service │ │Service │ │Service │ │ Service   │
│ :8081  │ │ :8082  │ │ :8086  │ │ :8084  │ │ :8085  │ │   :8087   │
│        │ │        │ │        │ │        │ │        │ │            │
│ ✅ CORS │ │ ✅ CORS│ │ ❌ CORS│ │ ❌ CORS│ │ ✅ CORS│ │ ❌ CORS   │
│disabled│ │disabled│ │disabled│ │disabled│ │disabled│ │ disabled   │
│        │ │        │ │        │ │        │ │        │ │            │
│ ✅ Spring│ │ ✅Spring│ │ ❌ No  │ │ ❌ No  │ │ ✅Spring│ │ ❌ No     │
│Security│ │Security│ │Security│ │Security│ │Security│ │ Security  │
└───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘ └─────┬──────┘
    │          │          │          │          │            │
┌───▼────┐ ┌───▼────┐ ┌───▼────┐ ┌───▼────┐ ┌───▼────┐      │
│auth_db │ │users_db│ │event_db│ │order_db│ │payment │   (Stateless)
│:5433  │ │ :5434  │ │ :5435  │ │ :5436  │ │ :5437  │      │
│PG 15  │ │ PG 15  │ │ PG 15  │ │ PG 15  │ │ PG 15  │      │
└───────┘ └───────┘ └───────┘ └───────┘ └────────┘      │
                                                          │
                    Comunicación entre Servicios:          │
                    WebClient (Spring WebFlux)            │
```

### Microservicios

| Servicio | Puerto | Base de Datos | Responsabilidad |
|----------|--------|----------------|-----------------|
| **API Gateway** | 8080 | N/A | Enrutamiento, JWT, CORS |
| **auth-service** | 8081 | auth_db (:5433) | Autenticación y usuarios |
| **users-service** | 8082 | users_db (:5434) | Perfiles y empleados |
| **event-service** | 8086 | event_db (:5435) | Eventos y tickets |
| **order-service** | 8084 | order_db (:5436) | Carritos y órdenes |
| **payment-service** | 8085 | payment_db (:5437) | Pagos con Stripe |
| **analytics-service** | 8087 | Stateless | Dashboard y estadísticas |

### Principios de Arquitectura

1. **Separación de Responsabilidades**: Cada microservicio tiene una responsabilidad única
2. **Independencia de Datos**: Cada servicio tiene su propia base de datos
3. **Comunicación Reactiva**: Uso de WebClient para comunicación entre servicios
4. **Autenticación Centralizada**: API Gateway valida JWT e inyecta headers
5. **CORS Centralizado**: Solo el API Gateway maneja CORS

---

## ✨ Funcionalidades Principales

### 1. Gestión de Eventos

- ✅ Crear, editar y eliminar eventos
- ✅ Gestión de categorías de eventos
- ✅ Upload de imágenes de eventos
- ✅ Capacidad máxima y disponibilidad de passes
- ✅ Fechas y horarios configurables

### 2. Sistema de Tickets y Passes

- ✅ Generación de passes pre-creados con QR único
- ✅ Conversión de passes a tickets tras compra
- ✅ Validación QR para entrada única (single entry)
- ✅ Control de stock en tiempo real

### 3. Consumiciones

- ✅ CRUD de consumiciones (bebidas, comidas)
- ✅ Asignación a eventos específicos
- ✅ Gestión de stock
- ✅ Registro de consumo desde tickets

### 4. Carrito de Compras

- ✅ Multi-item (eventos + consumiciones)
- ✅ Expiración automática (10 minutos)
- ✅ Validación de stock en tiempo real
- ✅ Checkout hacia Stripe

### 5. Procesamiento de Pagos

- ✅ Integración con Stripe Checkout
- ✅ Webhooks para confirmación de pagos
- ✅ Estados: PENDING → APPROVED/REJECTED
- ✅ Verificación de firma en webhooks

### 6. Dashboard y Analytics

- ✅ Métricas de eventos, ventas e ingresos
- ✅ Consolidación de datos de múltiples servicios
- ✅ Estadísticas en tiempo real
- ✅ Multi-tenant por organizador

### 7. Gestión de Empleados

- ✅ CRUD de empleados
- ✅ Asignación a eventos específicos
- ✅ Validación de tickets QR
- ✅ Registro de consumos

### 8. Flujos de Usuario

#### Registro y Login
```
1. Cliente → Registro con email, DNI, contraseña
2. Sistema → Envía email de verificación
3. Cliente → Verifica email → Cuenta activa
4. Cliente → Login con credenciales → JWT
```

#### Compra de Tickets
```
1. Cliente → Explora eventos
2. Cliente → Selecciona evento + consumiciones
3. Cliente → Agrega al carrito
4. Cliente → Checkout → Crea orden
5. Cliente → Pago con Stripe
6. Sistema → Confirma pago → Genera tickets
7. Cliente → Recibe tickets con QR
```

#### Validación de Entrada
```
1. Empleado → Login con credenciales
2. Empleado → Ve eventos asignados
3. Cliente → Presenta ticket QR
4. Empleado → Escanea QR → Valida
5. Sistema → Marca ticket como usado
```

---

## 🛠️ Stack Tecnológico

### Backend

| Componente | Tecnología | Versión |
|------------|------------|---------|
| **Framework** | Spring Boot | 3.5.6/3.5.7 |
| **Lenguaje** | Java | 17 |
| **API Gateway** | Spring Cloud Gateway | 2023.0.0 |
| **Base de Datos** | PostgreSQL | 15-alpine |
| **Autenticación** | JWT (jjwt) | 0.12.5/0.12.6 |
| **Pasarela de Pago** | Stripe SDK | 26.7.0 |
| **Cliente HTTP** | Spring WebFlux | 3.5.6 |
| **Contenedores** | Docker + Docker Compose | Latest |

### Frontend

| Componente | Tecnología | Versión |
|------------|------------|---------|
| **Framework** | Angular | 19.2.0 |
| **Lenguaje** | TypeScript | 5.7.2 |
| **UI Framework** | Bootstrap | 5.3.8 |
| **QR Scanner** | @zxing/ngx-scanner | 20.0.0 |
| **Alertas** | SweetAlert2 | 11.26.3 |
| **Estado** | RxJS + BehaviorSubject | 7.8.0 |

### Herramientas de Desarrollo

| Herramienta | Propósito |
|-------------|-----------|
| **Maven** | Build y gestión de dependencias Java |
| **Docker** | Containerización de servicios |
| **Angular CLI** | Generación y build de frontend |
| **Postman/curl** | Testing de APIs |

---

## 📁 Estructura del Proyecto

```
ps-packedgo-main/
├── packedgo/
│   ├── back/                          # Backend (Microservicios Spring Boot)
│   │   ├── api-gateway/               # API Gateway (Spring Cloud)
│   │   ├── auth-service/              # Autenticación
│   │   ├── users-service/             # Perfiles y empleados
│   │   ├── event-service/             # Eventos y tickets
│   │   ├── order-service/             # Carritos y órdenes
│   │   ├── payment-service/           # Pagos con Stripe
│   │   ├── analytics-service/        # Dashboard y estadísticas
│   │   ├── docker-compose.yml         # Orquestación Docker
│   │   ├── .env.example               # Variables de entorno
│   │   ├── README.md                  # Documentación backend
│   │   └── TECHNICAL_DOCUMENTATION.md # Documentación técnica completa
│   │
│   └── front-angular/                 # Frontend (Angular 19)
│       ├── src/
│       │   ├── app/
│       │   │   ├── core/              # Servicios, guards, interceptors
│       │   │   ├── features/          # Componentes por módulo
│       │   │   │   ├── admin/         # Dashboard, eventos, empleados
│       │   │   │   ├── auth/           # Login, register, verificación
│       │   │   │   ├── customer/       # Checkout, orders, perfil
│       │   │   │   ├── employee/      # Validación QR
│       │   │   │   ├── events-explore/ # Exploración pública
│       │   │   │   ├── landing/        # Página de inicio
│       │   │   │   └── terms/          # Términos y privacidad
│       │   │   ├── shared/            # Modelos, pipes, componentes
│       │   │   ├── app.component.ts
│       │   │   ├── app.config.ts
│       │   │   └── app.routes.ts
│       │   ├── environments/          # Configuraciones de entorno
│       │   ├── assets/                 # Imágenes y recursos
│       │   └── styles.css             # Estilos globales
│       ├── proxy.conf.json            # Proxy para API Gateway
│       ├── package.json               # Dependencias npm
│       ├── angular.json               # Configuración Angular
│       └── TECHNICAL_DOCUMENTATION.md # Documentación técnica
│
└── README.md                          # Este archivo
```

---

## 🚀 Instalación y Configuración

### Requisitos Previos

| Requisito | Versión Mínima |
|-----------|----------------|
| **Java** | 17+ |
| **Maven** | 3.9+ |
| **Docker Desktop** | Latest |
| **PostgreSQL** | 15 (incluido en Docker) |
| **Node.js** | 18+ |
| **npm** | 9+ |

### Paso 1: Clonar el Repositorio

```bash
git clone <repositorio-url>
cd ps-packedgo-main/packedgo
```

### Paso 2: Configurar Variables de Entorno

Cada servicio requiere un archivo `.env`. Copia los ejemplos:

```bash
# Backend
cd back
cp auth-service/.env.example auth-service/.env
cp users-service/.env.example users-service/.env
cp event-service/.env.example event-service/.env
cp order-service/.env.example order-service/.env
cp payment-service/.env.example payment-service/.env
cp analytics-service/.env.example analytics-service/.env
```

Edita cada archivo `.env` con tus credenciales:

```env
# Variables críticas
JWT_SECRET=your-super-secret-key-change-in-production
STRIPE_API_KEY=sk_test_your_stripe_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret
FRONTEND_URL=http://localhost:3000
```

### Paso 3: Iniciar las Bases de Datos

```bash
cd back
docker-compose up -d auth-db users-db event-db order-db payment-db
```

### Paso 4: Compilar los Microservicios

```bash
# Compilar cada servicio
cd auth-service && mvn clean package -DskipTests && cd ..
cd users-service && mvn clean package -DskipTests && cd ..
cd event-service && mvn clean package -DskipTests && cd ..
cd order-service && mvn clean package -DskipTests && cd ..
cd payment-service && mvn clean package -DskipTests && cd ..
cd analytics-service && mvn clean package -DskipTests && cd ..
cd api-gateway && mvn clean package -DskipTests && cd ..
```

### Paso 5: Iniciar los Servicios

```bash
# Iniciar todos los servicios
docker-compose up -d

# Verificar que están corriendo
docker-compose ps
```

### Paso 6: Instalar y Ejecutar el Frontend

```bash
cd front-angular

# Instalar dependencias
npm install

# Iniciar servidor de desarrollo
npm start
# o
ng serve --proxy-config proxy.conf.json
```

El frontend estará disponible en: **http://localhost:3000**

### Verificación de Instalación

```bash
# Test de endpoint público
curl http://localhost:8080/api/events

# Login de admin
curl -X POST http://localhost:8080/api/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@test.com", "password": "Admin123!"}'

# Health check de todos los servicios
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # auth-service
curl http://localhost:8082/actuator/health  # users-service
curl http://localhost:8086/actuator/health  # event-service
curl http://localhost:8084/actuator/health  # order-service
curl http://localhost:8085/actuator/health  # payment-service
curl http://localhost:8087/actuator/health  # analytics-service
```

---

## 📖 Guía de Uso

### Accesos de Prueba

| Tipo | Email/DNI | Contraseña | Rol |
|------|-----------|------------|-----|
| Admin | admin@test.com | Admin123! | ADMIN |
| Cliente | 12345678 | Customer123! | CUSTOMER |
| Empleado | employee@test.com | Employee123! | EMPLOYEE |

### Endpoints Principales

#### Autenticación (Públicos)

```bash
# Login de clientes
POST /api/auth/customer/login
Body: {"document": "12345678", "password": "Customer123!"}

# Login de administradores
POST /api/auth/admin/login
Body: {"email": "admin@test.com", "password": "Admin123!"}

# Registro de clientes
POST /api/auth/customer/register
Body: {"username": "john", "email": "john@test.com", "document": "87654321", "password": "Pass123!", "firstName": "John", "lastName": "Doe"}
```

#### Eventos (Mixto)

```bash
# Listar eventos (público)
GET /api/events

# Detalle de evento (público)
GET /api/events/{id}

# Crear evento (ADMIN)
POST /api/events
Authorization: Bearer {token}
Body: {"name": "Concierto", "description": "...", "location": "...", "startDate": "2025-12-01", "endDate": "2025-12-02", "maxCapacity": 1000, "price": 50.00, "eventCategoryId": 1}
```

#### Carrito y Órdenes

```bash
# Agregar al carrito (autenticado)
POST /api/cart/add
Authorization: Bearer {token}
Body: {"eventId": 1, "quantity": 2}

# Ver carrito
GET /api/cart
Authorization: Bearer {token}

# Checkout
POST /api/orders/checkout
Authorization: Bearer {token}
```

#### Pagos

```bash
# Crear sesión de pago Stripe
POST /api/payments/create-checkout-stripe
Authorization: Bearer {token}
Body: {"orderId": 1}

# Webhook de Stripe (Stripe → Backend)
POST /api/webhooks/stripe
```

#### Dashboard (ADMIN)

```bash
# Dashboard del organizador
GET /api/dashboard
Authorization: Bearer {token}
```

### Comandos Docker Útiles

```bash
# Ver logs de un servicio
docker-compose logs -f auth-service

# Reiniciar un servicio
docker-compose restart analytics-service

# Reconstruir sin caché
docker-compose build --no-cache analytics-service
docker-compose up -d analytics-service

# Detener todo
docker-compose down

# Limpiar todo (⚠️ elimina datos)
docker-compose down -v
```

---

## 🤝 Guía de Contribución

### Fork y Clonado

1. Haz fork del repositorio
2. Clona tu fork:
   ```bash
   git clone https://github.com/TU_USUARIO/ps-packedgo.git
   cd ps-packedgo
   ```

### Crear Rama de Feature

```bash
# Crear rama desde develop/main
git checkout -b feature/nueva-funcionalidad
# o
git checkout -b fix/corregir-bug
```

### Estándares de Código

#### Backend (Java/Spring)

- ✅ Usar Lombok para reducir boilerplate
- ✅ Anotaciones `@Slf4j` para logging
- ✅ DTOs para transferencia de datos
- ✅ Services para lógica de negocio
- ✅ Repositories para acceso a datos
- ✅ Validaciones con Bean Validation

#### Frontend (Angular)

- ✅ Componentes standalone (Angular 19+)
- ✅ Lazy loading para todas las rutas
- ✅ TypeScript strict mode
- ✅ Services para lógica de negocio
- ✅ BehaviorSubject para estado
- ✅ Pipes para transformaciones

### Commits Convencionales

Usar conventional commits:

```bash
feat: agregar validación de tickets QR
fix: corregir error de CORS duplicado
docs: actualizar documentación de API
refactor: reorganizar estructura de servicios
test: agregar tests unitarios para auth-service
```

### Pull Request

1. Push de tu rama:
   ```bash
   git push origin feature/nueva-funcionalidad
   ```

2. Crear PR con:
   - Título descriptivo
   - Descripción del cambio
   - Screenshots si es UI
   - Links a issues relacionados

3. Verificar que los tests pasen

### Testing

```bash
# Backend - Tests unitarios
cd auth-service
mvn test

# Backend - Con cobertura
mvn test jacoco:report

# Frontend - Tests unitarios
cd front-angular
ng test

# Frontend - Con cobertura
ng test --code-coverage
```

---

## ⚠️ Notas Importantes de Configuración

### CORS

⚠️ **CRÍTICO**: CORS está configurado ÚNICAMENTE en el API Gateway.

| Componente | CORS | Acción |
|------------|------|--------|
| **API Gateway** | ✅ | `allowedOrigins: http://localhost:3000` |
| auth-service | ❌ | `.cors(cors -> cors.disable())` |
| users-service | ❌ | `.cors(cors -> cors.disable())` |
| payment-service | ❌ | `.cors(cors -> cors.disable())` |
| event-service | ❌ | Sin CorsConfig.java |
| order-service | ❌ | Sin CorsConfig.java |
| analytics-service | ❌ | Sin Spring Security |

**NO agregar**:
- ❌ Archivos `CorsConfig.java` en microservicios
- ❌ Anotaciones `@CrossOrigin` en controllers
- ❌ Configuración CORS en `SecurityConfig` de microservicios

### Seguridad

El sistema implementa un modelo de seguridad en dos capas:

```
Capa 1: API Gateway
  - Valida JWT (firma + expiración)
  - Extrae userId y role
  - Inyecta headers: X-User-Id, X-User-Role

Capa 2: Microservicios
  - Leen headers inyectados
  - Aplican reglas de negocio
  - No re-validan JWT (confían en Gateway)
```

---

## 📚 Recursos Adicionales

### Documentación Técnica

- **[TECHNICAL_DOCUMENTATION.md](packedgo/back/TECHNICAL_DOCUMENTATION.md)** - Documentación completa del backend
- **[TECHNICAL_DOCUMENTATION.md](packedgo/front-angular/TECHNICAL_DOCUMENTATION.md)** - Documentación del frontend
- **[API Gateway README](packedgo/back/api-gateway/API_GATEWAY_README.md)**
- **[Auth Service README](packedgo/back/auth-service/AUTH_SERVICE_README.md)**
- **[Users Service README](packedgo/back/users-service/USERS_SERVICE_README.md)**
- **[Event Service README](packedgo/back/event-service/EVENT_SERVICE_README.md)**
- **[Order Service README](packedgo/back/order-service/ORDER_SERVICE_README.md)**
- **[Payment Service README](packedgo/back/payment-service/PAYMENT_SERVICE_README.md)**
- **[Analytics Service README](packedgo/back/analytics-service/ANALYTICS_SERVICE_README.md)**

### Enlaces Externos

- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [JWT.io](https://jwt.io/)
- [Stripe API](https://stripe.com/docs/api)
- [Angular Documentation](https://angular.io/docs)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)
- [Docker Compose](https://docs.docker.com/compose/)

---

## 🔧 Troubleshooting

### Error: Duplicate CORS headers

**Síntoma**: `Access-Control-Allow-Origin: http://localhost:3000, http://localhost:3000`

**Solución**: 
- CORS debe estar SOLO en API Gateway
- Deshabilitar CORS en TODOS los microservicios

### Error: 403 Forbidden

**Causas posibles**:
1. Spring Security bloqueando requests
2. Anotación `@CrossOrigin` con puerto incorrecto
3. JWT expirado o inválido

### Error: Cannot connect to database

```bash
# Verificar que bases de datos están corriendo
docker-compose ps | grep db

# Reiniciar bases de datos
docker-compose restart auth-db users-db event-db order-db payment-db
```

---

## 📞 Soporte

**Desarrollador**: David Delfino  
**Email**: daviddelfino97@hotmail.com  
**Proyecto**: PackedGo - Sistema de Gestión de Eventos  
**Última Actualización**: 15 de Diciembre de 2025

---

## 📄 Licencia

Propiedad de PackedGo. Todos los derechos reservados.

---

<div align="center">

⭐️ Si este proyecto te fue útil, ¡considera给它 una estrella!

</div>
