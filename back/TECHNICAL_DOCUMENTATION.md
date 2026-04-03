# 📚 DOCUMENTACIÓN TÉCNICA COMPLETA - SISTEMA PACKEDGO BACKEND

**Versión**: 2.1 (Actualización CORS y Seguridad)  
**Fecha**: 15 de diciembre de 2025  
**Arquitectura**: Microservicios con Spring Boot + API Gateway  
**Estado del Sistema**: ✅ Completamente Operativo y Documentado

---

## 🎯 Resumen Ejecutivo

PackedGo es una plataforma completa de gestión de eventos desarrollada con arquitectura de microservicios. El sistema permite a organizadores crear y gestionar eventos, procesar pagos de forma segura con Stripe, validar entradas mediante códigos QR, y visualizar analytics en tiempo real.

### Cambios Recientes Importantes

- ✅ **CORS Centralizado**: Toda la configuración CORS está ahora únicamente en el API Gateway (eliminada de microservicios)
- ✅ **Frontend en Puerto 3000**: El frontend Angular se ejecuta en `http://localhost:3000` (NO 4200)
- ✅ **Analytics sin Spring Security**: analytics-service removió Spring Security completamente, confía en headers del Gateway
- ✅ **Autenticación Gateway-Based**: API Gateway valida JWT e inyecta headers `X-User-Id` y `X-User-Role`
- ✅ **Sin Duplicación de Headers**: Eliminado el error `Access-Control-Allow-Origin` duplicado  

---

## 📋 Tabla de Contenidos

1. [Visión General del Sistema](#-visión-general-del-sistema)
2. [Arquitectura de Microservicios](#-arquitectura-de-microservicios)
3. [Microservicios](#-microservicios)
   - [API Gateway](#1-api-gateway)
   - [Auth Service](#2-auth-service)
   - [Users Service](#3-users-service)
   - [Event Service](#4-event-service)
   - [Order Service](#5-order-service)
   - [Payment Service](#6-payment-service)
   - [Analytics Service](#7-analytics-service)
4. [Infraestructura y Despliegue](#-infraestructura-y-despliegue)
5. [Seguridad](#-seguridad)
6. [Flujos de Negocio](#-flujos-de-negocio)
7. [Base de Datos](#-base-de-datos)
8. [APIs y Endpoints](#-apis-y-endpoints)
9. [Configuración y Variables de Entorno](#-configuración-y-variables-de-entorno)
10. [Guía de Desarrollo](#-guía-de-desarrollo)

---

## 🎯 Visión General del Sistema

**PackedGo** es una plataforma completa de gestión de eventos que permite a organizadores crear eventos, vender tickets, gestionar consumiciones y analizar métricas en tiempo real. El sistema está diseñado con arquitectura de microservicios para garantizar escalabilidad, mantenibilidad y resiliencia.

### Características Principales

- 🎫 **Gestión de Eventos**: Creación, edición y administración completa de eventos
- 💳 **Sistema de Pagos**: Integración con Stripe para pagos seguros
- 🛒 **Carrito de Compras**: Sistema de carrito con expiración automática
- 👥 **Multi-tenant**: Soporte para múltiples organizadores independientes
- 📊 **Analytics**: Dashboard en tiempo real con métricas de negocio
- 👷 **Gestión de Empleados**: Sistema de empleados para validación de tickets y consumos
- 🔐 **Autenticación JWT**: Sistema de autenticación robusto con roles diferenciados
- 📱 **Validación QR**: Sistema de validación de tickets mediante códigos QR

### Stack Tecnológico

| Componente | Tecnología | Versión |
|------------|------------|---------|
| **Backend** | Spring Boot | 3.5.6/3.5.7 |
| **Lenguaje** | Java | 17 |
| **Base de Datos** | PostgreSQL | 15-alpine |
| **Autenticación** | JWT (jjwt) | 0.12.5/0.12.6 |
| **Pasarela de Pago** | Stripe SDK | 26.7.0 |
| **Contenedores** | Docker + Docker Compose | Latest |
| **Frontend** | Angular | Latest |
| **Cliente HTTP** | Spring WebFlux (WebClient) | 3.5.6 |

---

## 🏗 Arquitectura de Microservicios

### Diagrama de Arquitectura

```
┌──────────────────────────────────────────────────────────────────┐
│                        FRONTEND (Angular)                         │
│                      http://localhost:3000 ⚠️                     │
│            (NO 4200 - actualizado en proxy.conf.json)            │
└────────────────────────┬─────────────────────────────────────────┘
                         │ HTTP Requests + JWT Bearer Token
                         ▼
         ┌────────────────────────────────────────────┐
         │       ✅ API GATEWAY (Spring Cloud)        │
         │           Puerto: 8080                      │
         │                                             │
         │  ✅ CORS: allowedOrigins: localhost:3000   │
         │  ✅ JWT Validation (firma + expiración)    │
         │  ✅ Header Injection (X-User-Id, X-User-Role)│
         │  ✅ Public Endpoint Filter (/api/events,etc)│
         │  ✅ Retry Logic (3 intentos)                │
         │  ✅ Route Predicates a microservicios       │
         └───────────────┬────────────────────────────┘
                         │
         ┌───────────────┴──────────────────────┐
         │                                      │
┌────────▼─────┐  ┌────────▼──┐  ┌────────▼──┐  ┌───────▼───┐  ┌────────▼──┐  ┌──────────▼─┐
│  Auth        │  │  Users    │  │  Event    │  │  Order    │  │  Payment  │  │ Analytics  │
│  Service     │◄─┤  Service  │◄─┤  Service  │◄─┤  Service  │◄─┤  Service  │◄─┤  Service   │
│  :8081       │  │   :8082   │  │   :8086   │  │   :8084   │  │   :8085   │  │   :8087    │
│              │  │           │  │           │  │           │  │           │  │            │
│ ❌ CORS      │  │ ❌ CORS   │  │ ❌ CORS   │  │ ❌ CORS   │  │ ❌ CORS   │  │ ❌ CORS    │
│ disabled     │  │ disabled  │  │ disabled  │  │ disabled  │  │ disabled  │  │ disabled   │
│              │  │           │  │           │  │           │  │           │  │            │
│ ✅ Spring    │  │ ✅ Spring │  │ ❌ No     │  │ ❌ No     │  │ ✅ Spring │  │ ❌ No      │
│ Security     │  │ Security  │  │ Security  │  │ Security  │  │ Security  │  │ Security   │
│ (CORS dis.)  │  │(CORS dis.)│  │           │  │           │  │(CORS dis.)│  │ ⚠️ Confía  │
│              │  │           │  │           │  │           │  │           │  │ en Gateway │
└────┬─────────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └──────┬─────┘
     │                  │                │              │              │               │
┌────▼─────┐  ┌────────▼──────┐  ┌─────▼──────┐  ┌────▼──────┐  ┌────▼──────┐       │
│ auth_db  │  │   users_db    │  │  event_db  │  │ order_db  │  │ payment   │   (Stateless)
│  :5433   │  │     :5434     │  │   :5435    │  │   :5436   │  │   _db     │       │
│ PG 15    │  │    PG 15      │  │   PG 15    │  │   PG 15   │  │  :5437    │       │
└──────────┘  └───────────────┘  └────────────┘  └───────────┘  │  PG 15    │       │
                                                                  └───────────┘       │
                                                                                      │
                                    Consulta HTTP con WebClient:                     │
                                    - event-service (eventos, tickets, consumiciones)│
                                    - order-service (órdenes)                         │
                                    - payment-service (pagos, transacciones)          │
```

**Leyenda**:
- ✅ Habilitado/Configurado
- ❌ Deshabilitado/No configurado
- ⚠️ Atención especial
- PG 15 = PostgreSQL 15-alpine

### Principios de Arquitectura

1. **Separación de Responsabilidades**: Cada microservicio tiene una responsabilidad única y bien definida
2. **Independencia de Datos**: Cada servicio tiene su propia base de datos (excepto analytics-service)
3. **Comunicación Asíncrona**: Uso de WebClient para comunicación reactiva entre servicios
4. **Autenticación Centralizada**: Auth-service valida credenciales y genera tokens JWT
5. **Gateway Pattern (Futuro)**: API Gateway como punto de entrada único

---

## 🔧 Microservicios

### 1. API Gateway

**Estado**: ✅ ACTIVO Y FUNCIONAL  
**Puerto**: 8080  
**Propósito**: Punto de entrada único para todos los microservicios  
**Tecnología**: Spring Cloud Gateway (reactive)

#### Funcionalidades Implementadas

- ✅ **Enrutamiento Centralizado**: 7 rutas configuradas a todos los microservicios
- ✅ **Validación JWT**: AuthenticationFilter valida tokens en endpoints protegidos
- ✅ **Endpoints Públicos**: PublicEndpointFilter para login, register y webhooks
- ✅ **CORS Centralizado**: Configuración para Angular (localhost:4200)
- ✅ **Retry Logic**: 3 reintentos con backoff exponencial
- ✅ **Headers de Contexto**: Inyecta X-User-Id y X-User-Role a servicios downstream
- ✅ **Health Checks**: Actuator endpoints para monitoreo
- ✅ **Docker Ready**: Dockerfile multi-stage con imagen optimizada

#### Rutas Configuradas

| Ruta Pattern | Servicio Destino | Puerto | Filtro |
|--------------|------------------|--------|--------|
| `/api/auth/**` | auth-service | 8081 | Public |
| `/api/user-profiles/**` | users-service | 8082 | Auth |
| `/api/admin/employees/**` | users-service | 8082 | Auth |
| `/api/employee/**` | users-service | 8082 | Auth |
| `/api/events` (GET) | event-service | 8086 | Public |
| `/api/events/**` (otros) | event-service | 8086 | Auth |
| `/api/event-categories/**` | event-service | 8086 | Auth |
| `/api/consumptions/**` | event-service | 8086 | Auth |
| `/api/passes/**` | event-service | 8086 | Auth |
| `/api/tickets/**` | event-service | 8086 | Auth |
| `/api/cart/**` | order-service | 8084 | Auth |
| `/api/orders/**` | order-service | 8084 | Auth |
| `/api/payments/health` | payment-service | 8085 | Public |
| `/api/webhooks/stripe` | payment-service | 8085 | Public |
| `/api/payments/**` | payment-service | 8085 | Auth |
| `/api/dashboard/**` | analytics-service | 8087 | Auth |

#### Componentes Principales

**JwtUtil.java**: Utilidad de validación JWT
- Extracción de claims (userId, role)
- Verificación de expiración
- Validación de firma HMAC-SHA

**AuthenticationFilter.java**: Filtro de autenticación
- Valida Bearer tokens
- Agrega headers X-User-Id y X-User-Role
- Retorna 401 para tokens inválidos

**PublicEndpointFilter.java**: Filtro para endpoints públicos
- Permite acceso sin JWT
- Logging de requests

#### Tecnologías Clave

- Spring Cloud Gateway 2023.0.0 (reactive stack)
- Spring WebFlux (no MVC)
- JWT (jjwt 0.12.5)
- Spring Boot Actuator
- Lombok

#### Integración con Frontend

El frontend Angular (localhost:3000) se conecta directamente al API Gateway en puerto 8080. 

**⚠️ IMPORTANTE - Configuración CORS**:
- ✅ **CORS configurado ÚNICAMENTE en API Gateway** con `allowedOrigins: http://localhost:3000`
- ❌ **CORS deshabilitado en TODOS los microservicios** para evitar duplicación de headers
- ✅ Frontend NO necesita proxy.conf.json ya que todas las peticiones van directo a `http://localhost:8080/api/*`

**Antes (con error)**:
```
Access-Control-Allow-Origin: http://localhost:3000, http://localhost:3000
❌ Headers duplicados causaban error en frontend
```

**Ahora (correcto)**:
```
Access-Control-Allow-Origin: http://localhost:3000
✅ Un solo header desde API Gateway
```

**📁 Ubicación**: `packedgo/back/api-gateway/`  
**📖 README**: [API_GATEWAY_README.md](api-gateway/API_GATEWAY_README.md)

---

### 2. Auth Service

**Puerto**: 8081  
**Base de Datos**: auth_db (PostgreSQL :5433)  
**Propósito**: Autenticación, autorización y gestión de sesiones  

#### Responsabilidades

- ✅ Autenticación de usuarios por tipo (CUSTOMER, ADMIN, EMPLOYEE, SUPER_ADMIN)
- ✅ Generación y validación de tokens JWT
- ✅ Gestión de sesiones de usuario
- ✅ Verificación de email obligatoria para clientes
- ✅ Recuperación de contraseñas
- ✅ Protección contra fuerza bruta (5 intentos, 30 min bloqueo)
- ✅ Auditoría de intentos de login
- ✅ Integración con Mailtrap/SendGrid para emails

#### Endpoints Principales

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/auth/customer/login` | Login de clientes con DNI | No |
| POST | `/api/auth/admin/login` | Login de administradores con email | No |
| POST | `/api/auth/employee/login` | Login de empleados con email | No |
| POST | `/api/auth/customer/register` | Registro de nuevos clientes | No |
| POST | `/api/auth/admin/register` | Registro de nuevos administradores | No |
| POST | `/api/auth/verify-email/{token}` | Verificación de email | No |
| POST | `/api/auth/logout` | Cierre de sesión | JWT |
| POST | `/api/auth/refresh-token` | Renovar token de acceso | Refresh Token |

#### Tecnologías Clave

- Spring Security para autenticación
- JWT (jjwt 0.12.5) para tokens
- BCrypt (strength 12) para encriptación de contraseñas
- Spring Mail + SendGrid para emails
- WebClient para comunicación con users-service

#### Integración con Otros Servicios

- **users-service**: Validación de credenciales de empleados via `/api/internal/employees/validate`

**📁 Ubicación**: `packedgo/back/auth-service/`  
**📖 README**: [AUTH_SERVICE_README.md](auth-service/AUTH_SERVICE_README.md)

---

### 3. Users Service

**Puerto**: 8082  
**Base de Datos**: users_db (PostgreSQL :5434)  
**Propósito**: Gestión de perfiles de usuario y sistema de empleados  

#### Responsabilidades

- ✅ Gestión de perfiles de usuario (clientes, admins)
- ✅ Sistema completo de gestión de empleados
- ✅ Asignación de empleados a eventos
- ✅ Validación de credenciales de empleados (endpoint interno)
- ✅ Proxy de validación de tickets hacia event-service
- ✅ Proxy de registro de consumiciones hacia event-service
- ✅ Soft delete para preservación de datos
- ✅ Estadísticas de empleados

#### Endpoints Principales

**Gestión de Perfiles**
| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/user-profiles` | Crear perfil de usuario | JWT |
| GET | `/api/user-profiles/{authUserId}` | Obtener perfil por authUserId | JWT |
| PUT | `/api/user-profiles/{id}` | Actualizar perfil | JWT |
| DELETE | `/api/user-profiles/{id}` | Soft delete de perfil | JWT |

**Gestión de Empleados (Admin)**
| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/admin/employees` | Crear empleado | JWT (ADMIN) |
| GET | `/api/admin/employees` | Listar empleados del admin | JWT (ADMIN) |
| GET | `/api/admin/employees/{id}` | Obtener detalle de empleado | JWT (ADMIN) |
| PUT | `/api/admin/employees/{id}` | Actualizar empleado | JWT (ADMIN) |
| PATCH | `/api/admin/employees/{id}/toggle-status` | Activar/Desactivar empleado | JWT (ADMIN) |
| DELETE | `/api/admin/employees/{id}` | Eliminar empleado | JWT (ADMIN) |

**Operaciones de Empleados**
| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| GET | `/api/employee/assigned-events` | Ver eventos asignados | JWT (EMPLOYEE) |
| POST | `/api/employee/validate-ticket` | Validar ticket QR | JWT (EMPLOYEE) |
| POST | `/api/employee/register-consumption` | Registrar consumo QR | JWT (EMPLOYEE) |
| GET | `/api/employee/stats` | Estadísticas diarias | JWT (EMPLOYEE) |

**Endpoints Internos**
| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/internal/employees/validate` | Validar credenciales de empleado | Interno (auth-service) |

#### Tecnologías Clave

- Spring Data JPA para persistencia
- WebClient para integración con event-service
- BCryptPasswordEncoder para contraseñas de empleados
- ModelMapper para mapeo DTO ↔ Entidad

#### Integración con Otros Servicios

- **event-service**: Proxy para validación de tickets y registro de consumiciones
- **auth-service**: Proporciona validación de empleados

**📁 Ubicación**: `packedgo/back/users-service/`  
**📖 README**: [USERS_SERVICE_README.md](users-service/USERS_SERVICE_README.md)

---

### 4. Event Service

**Puerto**: 8086  
**Base de Datos**: event_db (PostgreSQL :5435)  
**Propósito**: Gestión completa de eventos, tickets, passes y consumiciones  

#### Responsabilidades

- ✅ CRUD completo de eventos multi-tenant
- ✅ Gestión de categorías de eventos
- ✅ Sistema de consumiciones y categorización de productos
- ✅ Generación de passes pre-generados con QR
- ✅ Conversión de passes a tickets tras compra
- ✅ Validación QR para entrada única (single entry)
- ✅ Control de stock en tiempo real
- ✅ Gestión de imágenes de eventos (almacenamiento en BD)
- ✅ Estadísticas de eventos para organizadores
- ✅ Sistema de consumiciones asociadas a tickets

#### Endpoints Principales

**Gestión de Eventos**
| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/events` | Crear evento | JWT (ADMIN) |
| GET | `/api/events` | Listar todos los eventos | No |
| GET | `/api/events/{id}` | Obtener detalle de evento | No |
| GET | `/api/events/organizer/{organizerId}` | Eventos de un organizador | JWT (ADMIN) |
| PUT | `/api/events/{id}` | Actualizar evento | JWT (ADMIN) |
| DELETE | `/api/events/{id}` | Eliminar evento | JWT (ADMIN) |

**Gestión de Passes y Tickets**
| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/passes/generate` | Generar passes pre-creados | JWT (ADMIN) |
| POST | `/api/passes/reserve` | Reservar pass para compra | Interno |
| GET | `/api/tickets/my-tickets` | Tickets del usuario | JWT |
| POST | `/api/tickets/validate` | Validar ticket QR | JWT (EMPLOYEE) |

**Gestión de Consumiciones**
| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/consumptions` | Crear consumición | JWT (ADMIN) |
| GET | `/api/consumptions/event/{eventId}` | Listar consumiciones de evento | No |
| PUT | `/api/consumptions/{id}` | Actualizar consumición | JWT (ADMIN) |
| POST | `/api/consumptions/register` | Registrar consumo de ticket | JWT (EMPLOYEE) |

#### Tecnologías Clave

- Spring Data JPA con relaciones complejas
- @Lob para almacenamiento de imágenes
- UUID para generación de QR codes únicos
- Transacciones para control de stock
- Soft delete con `isActive`

#### Integración con Otros Servicios

- **order-service**: Recibe solicitudes de reserva de passes
- **users-service**: Acepta validaciones de tickets y consumiciones desde empleados
- **analytics-service**: Proporciona datos de eventos y estadísticas

**📁 Ubicación**: `packedgo/back/event-service/`  
**📖 README**: [EVENT_SERVICE_README.md](event-service/EVENT_SERVICE_README.md)

---

### 5. Order Service

**Puerto**: 8084  
**Base de Datos**: order_db (PostgreSQL :5436)  
**Propósito**: Gestión de carrito de compras y órdenes  

#### Responsabilidades

- ✅ Carrito de compra multi-item (eventos + consumiciones)
- ✅ Expiración automática de carritos (10 minutos de inactividad)
- ✅ Validación de stock en tiempo real con event-service
- ✅ Generación de órdenes con número único (ORD-YYYYMMDD-XXX)
- ✅ Integración con payment-service para checkout
- ✅ Limpieza programada de carritos expirados (cada 5 minutos)
- ✅ Email de confirmación de orden
- ✅ Límite de 10 tickets por grupo de compra

#### Endpoints Principales

**Gestión de Carrito**
| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/cart/add` | Agregar evento con consumos al carrito | JWT |
| GET | `/api/cart` | Ver carrito actual | JWT |
| PUT | `/api/cart/item/{itemId}` | Actualizar cantidad de item | JWT |
| DELETE | `/api/cart/item/{itemId}` | Eliminar item del carrito | JWT |
| DELETE | `/api/cart/clear` | Vaciar carrito completo | JWT |

**Gestión de Órdenes**
| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/orders/checkout` | Crear orden desde carrito | JWT |
| GET | `/api/orders` | Listar órdenes del usuario | JWT |
| GET | `/api/orders/{orderNumber}` | Detalle de orden | JWT |
| PUT | `/api/orders/{orderId}/status` | Actualizar estado (interno) | Interno |

#### Tecnologías Clave

- Spring Scheduling para limpieza automática
- Spring Mail para confirmación de órdenes
- WebClient para integración con event-service y payment-service
- @Scheduled(fixedRate) para tareas periódicas

#### Estados de Carrito

- `ACTIVE`: Carrito en uso
- `EXPIRED`: Carrito expirado (> 10 min inactividad)
- `CHECKED_OUT`: Carrito convertido a orden

#### Estados de Orden

- `PENDING`: Orden creada, esperando pago
- `PAID`: Pago confirmado
- `CANCELLED`: Orden cancelada

#### Integración con Otros Servicios

- **event-service**: Validación de stock y reserva de passes
- **payment-service**: Creación de sesión de pago Stripe
- **Servicio de Email**: Confirmación de órdenes

**📁 Ubicación**: `packedgo/back/order-service/`  
**📖 README**: [ORDER_SERVICE_README.md](order-service/ORDER_SERVICE_README.md)

---

### 6. Payment Service

**Puerto**: 8085  
**Base de Datos**: payment_db (PostgreSQL :5437)  
**Propósito**: Pasarela de pagos con Stripe  

#### Responsabilidades

- ✅ Integración con Stripe Checkout para pagos seguros
- ✅ Procesamiento de webhooks de Stripe
- ✅ Persistencia de transacciones en base de datos
- ✅ Verificación manual de estado de pago
- ✅ Estadísticas de pagos por administrador
- ✅ Verificación de firma en webhooks para seguridad
- ✅ Soporte multi-moneda (principalmente ARS)

#### Endpoints Principales

**Gestión de Pagos**
| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/payments/create-checkout-stripe` | Crear sesión Stripe Checkout | JWT |
| POST | `/api/payments/verify/{orderId}` | Verificar estado de pago | No |
| GET | `/api/payments/stats` | Estadísticas de pagos | JWT (ADMIN) |
| GET | `/api/payments/health` | Health check del servicio | No |

**Webhooks (Uso Interno)**
| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/webhooks/stripe` | Recibir eventos de Stripe | Stripe Signature |

#### Flujo de Pago

```
1. Order-Service crea orden → POST /payments/create-checkout-stripe
2. Payment-Service crea Payment con status PENDING
3. Payment-Service llama Stripe API para crear Session
4. Stripe retorna checkout URL
5. Frontend redirige usuario a Stripe Checkout
6. Usuario completa pago en Stripe
7. Stripe envía webhook checkout.session.completed
8. Payment-Service verifica firma y actualiza Payment a APPROVED
9. Stripe redirige usuario a success_url
10. Frontend verifica pago con POST /payments/verify/{orderId}
```

#### Estados de Pago

- `PENDING`: Pago iniciado, esperando confirmación
- `APPROVED`: Pago aprobado por Stripe
- `REJECTED`: Pago rechazado
- `CANCELLED`: Pago cancelado por el usuario

#### Tecnologías Clave

- Stripe Java SDK 26.7.0
- Webhook signature verification para seguridad
- Gson para procesamiento JSON
- Spring Security para autenticación JWT

#### Variables de Entorno Críticas

- `STRIPE_API_KEY`: Secret key de Stripe (sk_test_... / sk_live_...)
- `STRIPE_WEBHOOK_SECRET`: Secret para verificar webhooks (whsec_...)
- `FRONTEND_URL`: URL para redirecciones tras pago

**📁 Ubicación**: `packedgo/back/payment-service/`  
**📖 README**: [PAYMENT_SERVICE_README.md](payment-service/PAYMENT_SERVICE_README.md)

---

### 7. Analytics Service

**Puerto**: 8087  
**Base de Datos**: Ninguna (stateless)  
**Propósito**: Dashboard y estadísticas en tiempo real  

#### Responsabilidades

- ✅ Dashboard unificado con métricas de eventos, ventas e ingresos
- ✅ Consolidación de datos de múltiples servicios
- ✅ Estadísticas en tiempo real sin almacenamiento persistente
- ✅ Multi-tenant por organizador
- ✅ Control de acceso por roles (ADMIN/SUPER_ADMIN)

#### ⚠️ Configuración de Seguridad Especial

**analytics-service NO tiene Spring Security**. A diferencia de otros servicios:

- ❌ Sin dependencia `spring-boot-starter-security`
- ❌ Sin archivo `SecurityConfig.java`
- ❌ Sin `@CrossOrigin` annotations
- ✅ Confía completamente en la validación JWT del API Gateway
- ✅ Lee headers `X-User-Id` y `X-User-Role` inyectados por Gateway
- ✅ Aplica validación de rol manualmente en el controller

**Código de Validación**:
```java
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    
    @GetMapping
    public ResponseEntity<DashboardDTO> getDashboard(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        String token = authorizationHeader.substring(7);
        Long userId = jwtTokenValidator.getUserIdFromToken(token);
        String role = jwtTokenValidator.getRoleFromToken(token);
        
        if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
            throw new ForbiddenException("Solo admins pueden ver el dashboard");
        }
        
        return ResponseEntity.ok(analyticsService.getDashboard(userId));
    }
}
```

**Razón de esta arquitectura**:
- API Gateway ya validó el JWT (firma + expiración)
- No hay necesidad de re-validar en analytics-service
- Simplifica el código y reduce dependencias
- Mejora performance (sin overhead de Spring Security)

#### Endpoints Principales

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| GET | `/api/dashboard` | Dashboard del organizador autenticado | JWT (ADMIN) |
| GET | `/api/dashboard/{organizerId}` | Dashboard de organizador específico | JWT (SUPER_ADMIN) |

#### Métricas Proporcionadas

**Métricas Generales**
- Total de eventos (activos, completados, cancelados)
- Total de tickets vendidos
- Ingresos totales
- Precio promedio de ticket

**Métricas Detalladas por Evento**
- Tickets vendidos vs capacidad
- Tasa de ocupación (%)
- Ingresos por evento
- Estado del evento

**Tendencias**
- Ventas mensuales
- Eventos más vendidos
- Órdenes recientes

#### Tecnologías Clave

- Spring WebFlux (WebClient) para llamadas paralelas a servicios
- No tiene base de datos (obtiene todo en tiempo real)
- JWT validation manual (no Spring Security)
- Diseño stateless para escalabilidad

#### Integración con Otros Servicios

Analytics-service consulta datos en tiempo real de:

- **event-service** (`http://event-service:8086`)
  - GET `/api/event-service/event/my-events` → Eventos del organizador
  - GET `/api/event-service/passes/event/{eventId}/count` → Passes vendidos
  - GET `/api/event-service/consumptions/event/{eventId}` → Consumiciones del evento

- **payment-service** (`http://payment-service:8085`)
  - GET `/api/payment-service/payments/organizer/{organizerId}` → Pagos del organizador

- **order-service** (`http://order-service:8084`)
  - GET `/api/order-service/orders/organizer/{organizerId}` → Órdenes del organizador

**Configuración WebClient**:
```java
@Bean
public WebClient.Builder webClientBuilder() {
    return WebClient.builder()
        .codecs(configurer -> configurer
            .defaultCodecs()
            .maxInMemorySize(5 * 1024 * 1024)); // 5MB buffer
}
```

**📁 Ubicación**: `packedgo/back/analytics-service/`  
**📖 README**: [ANALYTICS_SERVICE_README.md](analytics-service/ANALYTICS_SERVICE_README.md)

---

## 🐳 Infraestructura y Despliegue

### Docker Compose

El sistema completo se orquesta mediante Docker Compose. Archivo ubicado en `packedgo/back/docker-compose.yml`.

#### Servicios Activos

| Servicio | Puerto Host | Puerto Interno | Debug Port | Estado |
|----------|-------------|----------------|------------|--------|
| **api-gateway** | 8080 | 8080 | - | ✅ Activo |
| auth-service | 8081 | 8081 | 5005 | ✅ Activo |
| users-service | 8082 | 8082 | 5006 | ✅ Activo |
| order-service | 8084 | 8084 | 5008 | ✅ Activo |
| payment-service | 8085 | 8085 | 5010 | ✅ Activo |
| event-service | 8086 | 8086 | 5007 | ✅ Activo |
| analytics-service | 8087 | 8087 | 5009 | ✅ Activo |

#### Bases de Datos

| Base de Datos | Puerto Host | Puerto Interno | Imagen |
|---------------|-------------|----------------|--------|
| auth-db | 5433 | 5432 | postgres:15-alpine |
| users-db | 5434 | 5432 | postgres:15-alpine |
| event-db | 5435 | 5432 | postgres:15-alpine |
| order-db | 5436 | 5432 | postgres:15-alpine |
| payment-db | 5437 | 5432 | postgres:15-alpine |

### Red Docker

Todos los servicios están conectados a la red `packedgo-network` para comunicación interna.

### Health Checks

Todas las bases de datos PostgreSQL tienen health checks configurados:
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U {user} -d {database}"]
  interval: 30s
  timeout: 10s
  retries: 3
```

### Comandos de Despliegue

```bash
# Levantar todos los servicios
cd packedgo/back
docker-compose up -d

# Ver logs de un servicio específico
docker-compose logs -f auth-service

# Reconstruir y levantar un servicio
docker-compose up -d --build auth-service

# Detener todos los servicios
docker-compose down

# Eliminar volúmenes (⚠️ BORRA DATOS)
docker-compose down -v
```

---

## 🔐 Seguridad

### Arquitectura de Seguridad Completa

El sistema implementa un modelo de seguridad en dos capas:

```
┌─────────────────────────────────────────────────────────────┐
│                     CAPA 1: API GATEWAY                      │
│ ─────────────────────────────────────────────────────────── │
│ 1. Valida JWT (firma HMAC-SHA + expiración)                │
│ 2. Extrae claims (userId, role, authorities)               │
│ 3. Inyecta headers:                                         │
│    • X-User-Id: 123                                         │
│    • X-User-Role: ADMIN                                     │
│ 4. Enruta request a microservicio                           │
└──────────────────────┬──────────────────────────────────────┘
                       │ Request con headers inyectados
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                  CAPA 2: MICROSERVICIOS                      │
│ ─────────────────────────────────────────────────────────── │
│ 1. Leen headers X-User-Id y X-User-Role                    │
│ 2. Aplican reglas de negocio (ownership, permisos)         │
│ 3. No validan JWT (confían en Gateway)                     │
│ 4. CORS DESHABILITADO en todos                             │
└─────────────────────────────────────────────────────────────┘
```

**⚠️ CRÍTICO - Configuración CORS**:

| Componente | CORS Habilitado | allowedOrigins | Notas |
|------------|----------------|----------------|-------|
| **API Gateway** | ✅ SÍ | `http://localhost:3000` | Único punto con CORS |
| auth-service | ❌ NO | - | `.cors(cors -> cors.disable())` |
| users-service | ❌ NO | - | `.cors(cors -> cors.disable())` |
| payment-service | ❌ NO | - | `.cors(cors -> cors.disable())` |
| event-service | ❌ NO | - | Sin CorsConfig.java |
| order-service | ❌ NO | - | Sin CorsConfig.java |
| analytics-service | ❌ NO | - | Sin Spring Security |

**Problema Resuelto**:
```
ANTES: Access-Control-Allow-Origin: http://localhost:3000, http://localhost:3000
ERROR: Duplicate headers causaban error en frontend

AHORA: Access-Control-Allow-Origin: http://localhost:3000
✅ Un solo header desde API Gateway
```

### Autenticación JWT

Todos los servicios (excepto auth-service en endpoints de login) confían en la validación JWT del API Gateway.

#### Estructura del Token

```json
{
  "userId": 123,
  "username": "juan_perez",
  "email": "juan@example.com",
  "role": "ADMIN",
  "authorities": ["events:create", "events:read", "events:update"],
  "iat": 1702651200,
  "exp": 1702737600
}
```

#### Roles del Sistema

| Rol | Descripción | Permisos |
|-----|-------------|----------|
| **CUSTOMER** | Cliente que compra tickets | Comprar tickets, ver perfil, ver eventos |
| **ADMIN** | Organizador de eventos | CRUD eventos, ver estadísticas, gestionar empleados |
| **EMPLOYEE** | Empleado de eventos | Validar tickets, registrar consumos |
| **SUPER_ADMIN** | Administrador del sistema | Acceso completo, ver datos de todos los admins |

### Ejemplo de Validación en Microservicio

```java
// analytics-service/DashboardController.java
@GetMapping
public ResponseEntity<DashboardDTO> getDashboard(
        @RequestHeader("Authorization") String authorizationHeader
) {
    // Extraer token (ya validado por Gateway)
    String token = authorizationHeader.substring(7);
    Long userId = jwtTokenValidator.getUserIdFromToken(token);
    String role = jwtTokenValidator.getRoleFromToken(token);
    
    // O leer directamente headers inyectados por Gateway:
    // @RequestHeader("X-User-Id") Long userId
    // @RequestHeader("X-User-Role") String role
    
    if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
        throw new ForbiddenException("Solo admins pueden ver el dashboard");
    }
    
    return ResponseEntity.ok(analyticsService.getDashboard(userId));
}
```

### Protección de Endpoints

#### Endpoints Públicos (Sin JWT)
- `POST /api/auth/customer/login`
- `POST /api/auth/admin/login`
- `POST /api/auth/employee/login`
- `POST /api/auth/customer/register`
- `POST /api/auth/admin/register`
- `GET /api/events` (listado público)
- `GET /api/events/{id}` (detalle público)
- `GET /api/payments/health`
- `POST /api/webhooks/stripe` (verificación con Stripe Signature)

#### Endpoints Protegidos
- Todos los demás endpoints requieren `Authorization: Bearer {token}`

### Protección Anti-Fuerza Bruta

Auth-service implementa bloqueo de cuenta tras 5 intentos fallidos:
- **Intentos fallidos**: 5
- **Tiempo de bloqueo**: 30 minutos
- **Registro**: Tabla `login_attempts` audita todos los intentos

### Comunicación Entre Servicios

La comunicación entre servicios es interna en la red Docker y no requiere JWT en endpoints internos (ej: `/api/internal/employees/validate`). Sin embargo, estos endpoints NO están expuestos públicamente.

### Estado de Spring Security por Servicio

| Servicio | Spring Security | Configuración | Propósito |
|----------|----------------|---------------|-----------|
| auth-service | ✅ Habilitado | CORS disabled | Autenticación inicial |
| users-service | ✅ Habilitado | CORS disabled | Gestión de perfiles |
| payment-service | ✅ Habilitado | CORS disabled | Procesamiento pagos |
| event-service | ❌ No | - | Gestión de eventos |
| order-service | ❌ No | - | Gestión de carritos |
| **analytics-service** | ❌ **Removido** | - | **Confía 100% en Gateway** |

**Notas Importantes**:
- analytics-service tuvo Spring Security completamente removido (dependency comentado, SecurityConfig.java eliminado)
- Todos los servicios con Spring Security tienen `.cors(cors -> cors.disable())` para evitar duplicación

---

## 🔄 Flujos de Negocio

### 1. Flujo de Registro y Verificación de Cliente

```
1. Cliente → POST /auth/customer/register
   - Datos: username, email, document, password, firstName, lastName
2. Auth-Service:
   - Crea AuthUser con role CUSTOMER, isVerified=false
   - Genera token de verificación con 24h de validez
   - Envía email con link de verificación
3. Cliente → Abre email → Click en link
4. Frontend → GET /auth/verify-email/{token}
5. Auth-Service:
   - Valida token y expiración
   - Marca isVerified=true
   - Redirige a /customer/login según role
6. Cliente → POST /auth/customer/login con DNI
7. Auth-Service:
   - Valida credenciales
   - Verifica que isVerified=true
   - Genera JWT con 24h de validez
   - Crea UserSession
8. Cliente autenticado puede navegar la app
```

### 2. Flujo de Creación de Evento

```
1. Admin → POST /auth/admin/login
2. Auth-Service → Retorna JWT con role ADMIN
3. Admin → POST /api/events
   - Datos: name, description, location, dates, capacity, price, categoryId, image
4. Event-Service:
   - Valida JWT y extrae adminId
   - Crea Event con createdBy=adminId, isActive=true
   - Guarda imagen en campo BYTEA
   - Retorna EventResponse
5. Admin → POST /api/passes/generate
   - Datos: eventId, quantity
6. Event-Service:
   - Valida ownership del evento
   - Genera N passes con QR únicos
   - Actualiza available_passes del evento
   - Retorna lista de passes creados
```

### 3. Flujo de Compra de Tickets

```
1. Cliente → GET /api/events → Ve listado de eventos
2. Cliente → Click en evento → GET /api/events/{id}
3. Cliente → Selecciona evento + consumiciones → POST /api/cart/add
   - Datos: eventId, quantity, consumptions: [{consumptionId, quantity}]
4. Order-Service:
   - Valida JWT, extrae userId
   - Verifica stock con event-service
   - Crea/actualiza ShoppingCart con status ACTIVE
   - Crea CartItem con subtotal
   - Crea CartItemConsumptions
   - Marca expires_at = now + 10 minutos
   - Retorna CartResponse
5. Cliente → GET /api/cart → Ve su carrito
6. Cliente → POST /api/orders/checkout
7. Order-Service:
   - Verifica que cart no esté expirado
   - Valida stock nuevamente
   - Genera orderNumber (ORD-20251215-001)
   - Crea Order con status PENDING
   - Marca cart como CHECKED_OUT
   - Retorna OrderResponse con orderId
8. Frontend → POST /api/payments/create-checkout-stripe
   - Datos: adminId, orderId, amount, description
9. Payment-Service:
   - Crea Payment con status PENDING
   - Llama Stripe API para crear Session
   - Guarda stripeSessionId
   - Retorna checkoutUrl
10. Frontend → Redirige a Stripe Checkout
11. Cliente → Completa pago en Stripe
12. Stripe → POST /api/webhooks/stripe
13. Payment-Service:
    - Verifica firma de Stripe
    - Actualiza Payment a APPROVED, guarda paidAt
14. Stripe → Redirige a success_url
15. Frontend → POST /payments/verify/{orderId}
16. Payment-Service → Confirma status APPROVED
17. Frontend → Muestra confirmación y tickets
```

### 4. Flujo de Validación de Ticket por Empleado

```
1. Admin → POST /api/admin/employees (crea empleado)
   - Datos: email, username, password, document, assignedEventIds: [1, 2]
2. Users-Service → Crea Employee con isActive=true
3. Empleado → POST /auth/employee/login
4. Auth-Service:
   - Llama users-service: POST /internal/employees/validate
   - Users-Service valida email/password con BCrypt
   - Retorna employee data si válido
   - Auth-Service genera JWT con role EMPLOYEE
5. Empleado → GET /api/employee/assigned-events
6. Users-Service:
   - Extrae employeeId del JWT
   - Retorna lista de eventos asignados
7. Empleado → Escanea QR de ticket → POST /api/employee/validate-ticket
   - Datos: ticketQrCode, eventId
8. Users-Service:
   - Verifica que employee tenga acceso a ese eventId
   - Si autorizado, hace proxy a event-service
9. Event-Service:
   - Busca ticket por qrCode
   - Verifica que pertenezca al eventId
   - Verifica que no esté usado (isUsed=false)
   - Marca isUsed=true, usedAt=now
   - Retorna datos del ticket
10. Users-Service → Retorna respuesta a empleado
11. Empleado → Ve confirmación de entrada válida
```

---

## 💾 Base de Datos

### Modelo de Datos Consolidado

#### auth_db

**auth_users**
- id (PK)
- username (UNIQUE)
- email (UNIQUE)
- document (UNIQUE)
- password
- role (CUSTOMER, ADMIN, EMPLOYEE, SUPER_ADMIN)
- is_active, is_verified, is_locked
- failed_login_attempts, lock_time
- created_at, updated_at

**user_sessions**
- id (PK)
- user_id (FK → auth_users)
- token, refresh_token
- ip_address, user_agent
- created_at, expires_at, is_active

**email_verification_tokens**
- id (PK)
- user_id (FK → auth_users)
- token (UNIQUE)
- created_at, expires_at, is_used

**password_recovery_tokens**
- id (PK)
- user_id (FK → auth_users)
- token (UNIQUE)
- created_at, expires_at, is_used

**login_attempts**
- id (PK)
- user_id (FK → auth_users)
- ip_address, user_agent
- success
- attempted_at, failure_reason

#### users_db

**user_profiles**
- id (PK)
- auth_user_id (UNIQUE, FK lógico a auth_users)
- first_name, last_name, document
- phone, address, city, province, country
- birth_date, gender
- is_active
- created_at, updated_at

**employees**
- id (PK)
- email (UNIQUE), username, password_hash, document
- admin_id (FK lógico a auth_users)
- is_active
- created_at

**employee_events** (join table)
- id (PK)
- employee_id (FK → employees)
- event_id (FK lógico a events)
- assigned_at
- UNIQUE(employee_id, event_id)

#### event_db

**events**
- id (PK)
- name, description, location, location_name
- start_date, end_date, start_time, end_time
- max_capacity, available_passes, price
- event_category_id (FK → event_categories)
- created_by (FK lógico a auth_users)
- image_data (BYTEA), image_content_type
- is_active
- created_at, updated_at

**event_categories**
- id (PK)
- name, description
- created_by (FK lógico a auth_users)
- is_active, created_at

**consumptions**
- id (PK)
- name, description, price, stock
- consumption_category_id (FK → consumption_categories)
- event_id (FK → events)
- image_data (BYTEA), image_content_type
- is_active
- created_at, updated_at

**consumption_categories**
- id (PK)
- name, description
- created_by (FK lógico a auth_users)
- is_active, created_at

**passes**
- id (PK)
- event_id (FK → events)
- qr_code (UNIQUE)
- is_sold
- created_at

**tickets**
- id (PK)
- pass_id (FK → passes, UNIQUE)
- user_id (FK lógico a auth_users)
- event_id (FK → events)
- order_id
- qr_code (UNIQUE)
- is_used, used_at
- purchased_at, total_price

**ticket_consumptions**
- id (PK)
- ticket_id (FK → tickets)
- consumption_id (FK → consumptions)
- quantity
- created_at

#### order_db

**shopping_carts**
- id (PK)
- user_id (FK lógico a auth_users)
- status (ACTIVE, EXPIRED, CHECKED_OUT)
- created_at, expires_at, updated_at
- UNIQUE(user_id, status) WHERE status='ACTIVE'

**cart_items**
- id (PK)
- cart_id (FK → shopping_carts)
- event_id (FK lógico a events)
- quantity, unit_price, subtotal
- created_at

**cart_item_consumptions**
- id (PK)
- cart_item_id (FK → cart_items)
- consumption_id (FK lógico a consumptions)
- consumption_name
- quantity, unit_price, subtotal
- created_at

**orders**
- id (PK)
- order_number (UNIQUE)
- user_id (FK lógico a auth_users)
- cart_id (FK → shopping_carts)
- total_amount
- status (PENDING, PAID, CANCELLED)
- created_at, updated_at

#### payment_db

**payments**
- id (PK)
- admin_id (FK lógico a auth_users)
- order_id (UNIQUE)
- amount, currency (ARS)
- status (PENDING, APPROVED, REJECTED, CANCELLED)
- payment_method, payer_email, payer_name, description
- stripe_session_id, stripe_payment_intent_id
- payment_provider (STRIPE)
- transaction_amount, status_detail
- created_at, updated_at, paid_at

---

## 📡 APIs y Endpoints

### Resumen de Endpoints por Servicio

#### Auth Service (8081)

**Autenticación Pública**
- POST `/api/auth/customer/login` - Login de clientes
- POST `/api/auth/admin/login` - Login de administradores
- POST `/api/auth/employee/login` - Login de empleados
- POST `/api/auth/customer/register` - Registro de clientes
- POST `/api/auth/admin/register` - Registro de administradores

**Verificación de Email**
- GET `/api/auth/verify-email/{token}` - Verificar email
- POST `/api/auth/resend-verification` - Reenviar email de verificación

**Recuperación de Contraseña**
- POST `/api/auth/forgot-password` - Solicitar reset de contraseña
- POST `/api/auth/reset-password` - Resetear contraseña con token

**Gestión de Sesión**
- POST `/api/auth/logout` - Cerrar sesión
- POST `/api/auth/refresh-token` - Renovar token de acceso

#### Users Service (8082)

**Perfiles de Usuario**
- POST `/api/user-profiles` - Crear perfil
- GET `/api/user-profiles/{authUserId}` - Obtener perfil
- PUT `/api/user-profiles/{id}` - Actualizar perfil
- DELETE `/api/user-profiles/{id}` - Eliminar perfil (soft delete)

**Empleados (Admin)**
- POST `/api/admin/employees` - Crear empleado
- GET `/api/admin/employees` - Listar empleados
- GET `/api/admin/employees/{id}` - Detalle de empleado
- PUT `/api/admin/employees/{id}` - Actualizar empleado
- PATCH `/api/admin/employees/{id}/toggle-status` - Activar/Desactivar
- DELETE `/api/admin/employees/{id}` - Eliminar empleado

**Operaciones de Empleado**
- GET `/api/employee/assigned-events` - Ver eventos asignados
- POST `/api/employee/validate-ticket` - Validar ticket
- POST `/api/employee/register-consumption` - Registrar consumo
- GET `/api/employee/stats` - Estadísticas diarias

**Internos**
- POST `/api/internal/employees/validate` - Validar credenciales (para auth-service)

#### Event Service (8086)

**Eventos**
- POST `/api/events` - Crear evento
- GET `/api/events` - Listar eventos (público)
- GET `/api/events/{id}` - Detalle de evento (público)
- GET `/api/events/organizer/{organizerId}` - Eventos de organizador
- PUT `/api/events/{id}` - Actualizar evento
- DELETE `/api/events/{id}` - Eliminar evento

**Categorías de Eventos**
- POST `/api/event-categories` - Crear categoría
- GET `/api/event-categories` - Listar categorías
- PUT `/api/event-categories/{id}` - Actualizar categoría
- DELETE `/api/event-categories/{id}` - Eliminar categoría

**Consumiciones**
- POST `/api/consumptions` - Crear consumición
- GET `/api/consumptions/event/{eventId}` - Listar consumiciones de evento
- PUT `/api/consumptions/{id}` - Actualizar consumición
- DELETE `/api/consumptions/{id}` - Eliminar consumición

**Passes y Tickets**
- POST `/api/passes/generate` - Generar passes
- POST `/api/passes/reserve` - Reservar pass (interno)
- GET `/api/tickets/my-tickets` - Mis tickets
- POST `/api/tickets/validate` - Validar ticket

#### Order Service (8084)

**Carrito**
- POST `/api/cart/add` - Agregar al carrito
- GET `/api/cart` - Ver carrito
- PUT `/api/cart/item/{itemId}` - Actualizar item
- DELETE `/api/cart/item/{itemId}` - Eliminar item
- DELETE `/api/cart/clear` - Vaciar carrito

**Órdenes**
- POST `/api/orders/checkout` - Crear orden
- GET `/api/orders` - Mis órdenes
- GET `/api/orders/{orderNumber}` - Detalle de orden
- PUT `/api/orders/{orderId}/status` - Actualizar estado (interno)

#### Payment Service (8085)

**Pagos**
- POST `/api/payments/create-checkout-stripe` - Crear sesión de pago
- POST `/api/payments/verify/{orderId}` - Verificar estado de pago
- GET `/api/payments/stats` - Estadísticas de pagos
- GET `/api/payments/health` - Health check

**Webhooks**
- POST `/api/webhooks/stripe` - Webhook de Stripe (uso interno)

#### Analytics Service (8087)

**Dashboard**
- GET `/api/dashboard` - Dashboard propio (ADMIN)
- GET `/api/dashboard/{organizerId}` - Dashboard de otro organizador (SUPER_ADMIN)

---

## ⚙️ Configuración y Variables de Entorno

### Variables por Servicio

#### Auth Service
```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://auth-db:5432/auth_db
SPRING_DATASOURCE_USERNAME=auth_user
SPRING_DATASOURCE_PASSWORD=auth_password

# JWT
JWT_SECRET=your-super-secret-key-change-in-production
JWT_EXPIRATION=86400000

# Email (Mailtrap para desarrollo)
MAIL_HOST=smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=your-mailtrap-username
MAIL_PASSWORD=your-mailtrap-password

# Email (SendGrid para producción)
SENDGRID_API_KEY=your-sendgrid-api-key

# URLs
FRONTEND_URL=http://localhost:4200
USERS_SERVICE_URL=http://users-service:8082
```

#### Payment Service
```env
# Stripe
STRIPE_API_KEY=sk_test_your_stripe_secret_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret

# Frontend
FRONTEND_URL=http://localhost:4200
```

#### Analytics Service
```env
# Services URLs
EVENT_SERVICE_URL=http://event-service:8086
ORDER_SERVICE_URL=http://order-service:8084
PAYMENT_SERVICE_URL=http://payment-service:8085

# JWT
JWT_SECRET=your-super-secret-key-change-in-production
```

### Archivo .env de Ejemplo

Crear archivo `.env` en la raíz de cada servicio:

```env
# Database
POSTGRES_DB=service_db
POSTGRES_USER=service_user
POSTGRES_PASSWORD=service_password

# JWT
JWT_SECRET=change-this-secret-in-production-use-a-strong-random-string
JWT_EXPIRATION=86400000

# Email
MAIL_HOST=smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=
MAIL_PASSWORD=

# External Services
USERS_SERVICE_URL=http://users-service:8082
EVENT_SERVICE_URL=http://event-service:8086
ORDER_SERVICE_URL=http://order-service:8084
PAYMENT_SERVICE_URL=http://payment-service:8085

# Stripe
STRIPE_API_KEY=
STRIPE_WEBHOOK_SECRET=

# Frontend
FRONTEND_URL=http://localhost:4200
```

---

## 🛠 Guía de Desarrollo

### Requisitos

- **Java**: 17 o superior
- **Maven**: 3.8+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **PostgreSQL**: 15+ (para desarrollo local sin Docker)

### Setup Inicial

```bash
# 1. Clonar repositorio
git clone <repo-url>
cd packedgo/back

# 2. Copiar archivos .env de ejemplo
cp auth-service/.env.example auth-service/.env
cp payment-service/.env.example payment-service/.env
# Editar cada .env con tus credenciales

# 3. Levantar bases de datos
docker-compose up -d auth-db users-db event-db order-db payment-db

# 4. Compilar servicios
cd auth-service && ./mvnw clean package -DskipTests && cd ..
cd users-service && ./mvnw clean package -DskipTests && cd ..
# Repetir para cada servicio

# 5. Levantar todos los servicios
docker-compose up -d

# 6. Verificar que todos estén corriendo
docker-compose ps
```

### Desarrollo Local (sin Docker)

```bash
# 1. Configurar PostgreSQL local
createdb auth_db
createdb users_db
createdb event_db
createdb order_db
createdb payment_db

# 2. Actualizar application.yml de cada servicio
# Cambiar localhost en lugar de nombres de servicio Docker

# 3. Ejecutar cada servicio
cd auth-service
./mvnw spring-boot:run

# En otra terminal
cd users-service
./mvnw spring-boot:run

# Repetir para cada servicio
```

### Testing

```bash
# Ejecutar tests de un servicio
cd auth-service
./mvnw test

# Ejecutar tests con cobertura
./mvnw test jacoco:report
```

### Debugging

Cada servicio expone un puerto de debug JDWP:

| Servicio | Debug Port |
|----------|------------|
| auth-service | 5005 |
| users-service | 5006 |
| event-service | 5007 |
| order-service | 5008 |
| analytics-service | 5009 |
| payment-service | 5010 |

**IntelliJ IDEA**:
1. Run → Edit Configurations
2. Add New → Remote JVM Debug
3. Host: localhost, Port: 5005 (o el del servicio)
4. Apply → Start Debug

**VS Code**:
```json
{
  "type": "java",
  "name": "Debug Auth Service",
  "request": "attach",
  "hostName": "localhost",
  "port": 5005
}
```

### Logs

```bash
# Ver logs en tiempo real
docker-compose logs -f auth-service

# Ver logs de todos los servicios
docker-compose logs -f

# Ver últimas 100 líneas
docker-compose logs --tail=100 auth-service
```

### Rebuild de Servicios

```bash
# Rebuild de un servicio específico
docker-compose up -d --build auth-service

# Rebuild de todos los servicios
docker-compose up -d --build
```

---

## 📚 Documentación Adicional

### README de Cada Servicio

Cada microservicio tiene su propio README con información detallada:

- [API Gateway README](api-gateway/API_GATEWAY_README.md) - ✅ ACTIVO - Punto de entrada único
- [Auth Service README](auth-service/AUTH_SERVICE_README.md) - Autenticación y autorización
- [Users Service README](users-service/USERS_SERVICE_README.md) - Gestión de perfiles y empleados
- [Event Service README](event-service/EVENT_SERVICE_README.md) - Gestión de eventos y tickets
- [Order Service README](order-service/ORDER_SERVICE_README.md) - Carrito y órdenes
- [Payment Service README](payment-service/PAYMENT_SERVICE_README.md) - Pasarela de pagos Stripe
- [Analytics Service README](analytics-service/ANALYTICS_SERVICE_README.md) - Dashboard y estadísticas

### Script de Automatización

- [deploy.ps1](deploy.ps1) - Script automatizado de despliegue (si existe)

---

## 🔧 Troubleshooting - Problemas Comunes

### 1. Error: Duplicate Access-Control-Allow-Origin Headers

**Síntoma**:
```
Access-Control-Allow-Origin: http://localhost:3000, http://localhost:3000
Error: The 'Access-Control-Allow-Origin' header contains multiple values
```

**Causa**: CORS configurado en API Gateway Y en microservicios simultáneamente.

**Solución**:
1. ✅ CORS debe estar SOLO en API Gateway
2. ❌ Deshabilitar CORS en TODOS los microservicios:
   - Servicios con Spring Security: Agregar `.cors(cors -> cors.disable())` en SecurityConfig
   - Servicios sin Spring Security: Eliminar archivos CorsConfig.java
   - Eliminar todas las anotaciones `@CrossOrigin`

3. Verificar API Gateway (`application.yml`):
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: 
              - "http://localhost:3000"  # ⚠️ Debe coincidir con frontend
```

**Archivos modificados en la corrección**:
- ❌ Eliminado: `users-service/src/main/java/com/packed_go/users_service/config/CorsConfig.java`
- ❌ Eliminado: `event-service/src/main/java/com/packed_go/event_service/config/CorsConfig.java`
- ❌ Eliminado: `order-service/src/main/java/com/packed_go/order_service/config/CorsConfig.java`
- ✅ Modificado: `*-service/src/main/java/**/config/SecurityConfig.java` (agregado `.cors(cors -> cors.disable())`)
- ❌ Removidas: Anotaciones `@CrossOrigin` de todos los controllers

---

### 2. Error 403 Forbidden en Analytics Dashboard

**Síntoma**:
```
GET http://localhost:8080/api/dashboard
403 Forbidden
```

**Causas Posibles**:

#### A) Spring Security bloqueando requests

**Síntoma adicional**: Log muestra `Using generated security password: ...`

**Solución**:
```java
// Option 1: Deshabilitar Spring Security en SecurityConfig
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.disable())
        .build();
}

// Option 2: Comentar dependencia en pom.xml (preferido para analytics-service)
<!-- <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency> -->

// Option 3: Eliminar SecurityConfig.java completamente (usado en analytics-service)
```

**Rebuild necesario**:
```bash
docker-compose build --no-cache analytics-service
docker-compose up -d analytics-service
```

#### B) Anotación @CrossOrigin bloqueando puerto incorrecto

**Síntoma adicional**: Frontend en puerto 3000, pero anotación dice 4200

**Código problemático**:
```java
@RestController
@CrossOrigin(origins = "http://localhost:4200")  // ❌ INCORRECTO
public class DashboardController {
    // ...
}
```

**Solución**: Eliminar completamente la anotación @CrossOrigin
```java
@RestController  // ✅ CORRECTO - sin @CrossOrigin
public class DashboardController {
    // ...
}
```

#### C) JWT inválido o expirado

**Verificar JWT**:
```bash
# Extraer payload del token
echo "eyJhbGc..." | cut -d. -f2 | base64 -d | jq .

# Verificar expiración
{
  "userId": 2,
  "role": "ADMIN",
  "exp": 1734297600  # Unix timestamp
}
```

**Obtener nuevo token**:
```bash
curl -X POST http://localhost:8080/api/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@test.com", "password": "Admin123!"}'
```

---

### 3. Error: Frontend no puede conectar a backend

**Síntoma**:
```
Failed to fetch
net::ERR_CONNECTION_REFUSED
```

**Causas y soluciones**:

#### A) Puerto incorrecto en frontend

**Verificar `proxy.conf.json` o llamadas HTTP**:
```json
{
  "/api": {
    "target": "http://localhost:8080",  // ✅ API Gateway
    "secure": false
  }
}
```

#### B) Docker Compose no está corriendo

**Verificar**:
```bash
docker-compose ps

# Debe mostrar todos los servicios "Up"
```

**Iniciar servicios**:
```bash
docker-compose up -d
```

#### C) API Gateway no está corriendo

**Verificar logs**:
```bash
docker logs back-api-gateway-1 --tail 50
```

**Rebuild si es necesario**:
```bash
cd api-gateway
mvn clean package
cd ..
docker-compose build api-gateway
docker-compose up -d api-gateway
```

---

### 4. Error: Base de datos no conecta

**Síntoma**:
```
org.postgresql.util.PSQLException: Connection refused
```

**Soluciones**:

#### A) Verificar que PostgreSQL está corriendo

```bash
docker-compose ps | grep db

# Debe mostrar:
# back-auth-db-1     running   5433/tcp
# back-users-db-1    running   5434/tcp
# etc.
```

#### B) Verificar credenciales en application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://auth-db:5432/auth_db  # Nombre del servicio Docker
    username: auth_user
    password: auth_password
```

⚠️ **Importante**: Dentro de Docker usar nombre de servicio (`auth-db`), NO `localhost`.

#### C) Recrear bases de datos

```bash
docker-compose down -v  # ⚠️ Elimina todos los datos
docker-compose up -d auth-db users-db event-db order-db payment-db
```

---

### 5. Error: Stripe Webhook no funciona

**Síntoma**: Pagos se procesan pero órdenes no se actualizan

**Verificar**:

#### A) Webhook signature validation

**Logs**:
```bash
docker logs back-payment-service-1 | grep "webhook"
```

**Deshabilitar temporalmente** (SOLO para testing local):
```java
// PaymentController.java
@PostMapping("/webhooks/stripe")
public ResponseEntity<String> handleStripeWebhook(
        @RequestBody String payload
        // @RequestHeader("Stripe-Signature") String sigHeader  // Comentar temporalmente
) {
    // paymentService.handleWebhook(payload, sigHeader);  // Sin verificación
    paymentService.handleWebhookUnsafe(payload);  // Para testing
    return ResponseEntity.ok("OK");
}
```

#### B) Ngrok para webhooks en desarrollo

```bash
# Instalar ngrok
choco install ngrok  # Windows
brew install ngrok   # Mac

# Exponer puerto 8080
ngrok http 8080

# Copiar URL pública (ej: https://abc123.ngrok.io)
# Configurar en Stripe Dashboard:
# Webhook URL: https://abc123.ngrok.io/api/webhooks/stripe
```

---

### 6. Comandos útiles de Docker

```bash
# Ver logs de todos los servicios
docker-compose logs -f

# Ver logs de un servicio específico
docker logs back-analytics-service-1 --tail 100 -f

# Reiniciar un servicio
docker-compose restart analytics-service

# Rebuild y recrear un servicio
docker-compose up -d --build --force-recreate analytics-service

# Detener todos los servicios
docker-compose down

# Eliminar TODO (incluyendo volúmenes de BD)
docker-compose down -v

# Ver uso de recursos
docker stats

# Limpiar imágenes no usadas
docker system prune -a
```

---

### 7. Verificar que el sistema está operativo

```bash
# 1. Health check de bases de datos
curl http://localhost:5433  # auth-db (debe responder)

# 2. Health check de microservicios
curl http://localhost:8081/actuator/health  # auth-service
curl http://localhost:8082/actuator/health  # users-service
curl http://localhost:8086/actuator/health  # event-service
curl http://localhost:8084/actuator/health  # order-service
curl http://localhost:8085/actuator/health  # payment-service
curl http://localhost:8087/actuator/health  # analytics-service
curl http://localhost:8080/actuator/health  # api-gateway

# 3. Test de endpoints públicos
curl http://localhost:8080/api/events  # Debe retornar array de eventos

# 4. Test de login
curl -X POST http://localhost:8080/api/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@test.com", "password": "Admin123!"}'

# 5. Test de endpoint protegido (con token del paso anterior)
curl http://localhost:8080/api/dashboard \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**Respuesta esperada del dashboard** (200 OK):
```json
{
  "totalEvents": 5,
  "totalTicketsSold": 150,
  "totalRevenue": 1500000.00,
  "averageTicketPrice": 10000.00,
  "events": [...]
}
```

---

## 🔄 Próximas Mejoras del Sistema

### Fase 1: API Gateway + Seguridad (✅ COMPLETADA - Diciembre 2025)
- [x] ✅ Implementar Spring Cloud Gateway
- [x] ✅ Configurar rutas a todos los 6 microservicios
- [x] ✅ Implementar JWT validation filter  
- [x] ✅ Implementar Public Endpoint filter
- [x] ✅ **Configurar CORS centralizado ÚNICAMENTE en Gateway**
- [x] ✅ **Deshabilitar CORS en TODOS los microservicios**
- [x] ✅ **Eliminar anotaciones @CrossOrigin de controllers**
- [x] ✅ **Remover Spring Security de analytics-service**
- [x] ✅ Agregar retry logic (3 intentos)
- [x] ✅ Integrar con docker-compose
- [x] ✅ Actualizar frontend para puerto 3000
- [x] ✅ Inyección de headers X-User-Id y X-User-Role
- [x] ✅ Documentación completa y actualizada
- [x] ✅ Troubleshooting guide para errores comunes

**Problemas Resueltos**:
- ✅ Duplicate CORS headers (API Gateway + microservicios)
- ✅ 403 Forbidden en analytics dashboard (Spring Security + @CrossOrigin incorrecto)
- ✅ Puerto incorrecto frontend (4200 → 3000)

### Fase 1B: API Gateway - Mejoras Futuras
- [ ] Implementar rate limiting por IP/usuario (Redis)
- [ ] Configurar circuit breaker con Resilience4j
- [ ] Agregar request/response logging detallado
- [ ] Implementar API key authentication para servicios externos
- [ ] Request throttling para prevenir abuso
- [ ] Metrics collection (Micrometer)

### Fase 2: Observabilidad y Monitoreo
- [ ] Integrar Spring Actuator en todos los servicios (parcialmente completo)
- [ ] Configurar métricas con Prometheus
- [ ] Agregar dashboards en Grafana
  - Dashboard de requests por servicio
  - Dashboard de errores y latencias
  - Dashboard de health checks
- [ ] Implementar distributed tracing con Zipkin/Jaeger
- [ ] Centralizar logs con ELK Stack (Elasticsearch + Logstash + Kibana)
- [ ] Alerting con Prometheus Alertmanager
- [ ] Log correlation con request IDs

### Fase 3: Caché y Performance
- [ ] Implementar Redis para caché distribuida
  - Cachear resultados de analytics dashboard (TTL: 5 min)
  - Cachear listados de eventos públicos (TTL: 1 hora)
  - Session storage para JWT refresh tokens
- [ ] Optimizar queries PostgreSQL
  - Agregar índices en columnas frecuentemente consultadas
  - Analizar slow queries con EXPLAIN ANALYZE
- [ ] Implementar paginación en todos los listados
- [ ] Compresión de respuestas HTTP (Gzip)
- [ ] Connection pooling optimizado (HikariCP tuning)
- [ ] Database query caching

### Fase 4: Notificaciones y Comunicación
- [ ] Sistema de notificaciones push (Firebase Cloud Messaging)
- [ ] Envío de emails transaccionales
  - Confirmación de compra con PDF de tickets
  - Recordatorios de eventos (24h antes)
  - Resumen semanal para organizadores
- [ ] SMS para códigos de verificación (Twilio)
- [ ] Notificaciones en tiempo real con WebSockets
  - Notificación de nuevas ventas a organizadores
  - Alertas de validación de tickets a empleados
- [ ] In-app notifications

### Fase 5: Alta Disponibilidad y Escalabilidad
- [ ] Configurar réplicas de servicios (Kubernetes)
- [ ] Implementar service discovery (Eureka/Consul)
- [ ] Load balancing automático con Nginx/HAProxy
- [ ] Circuit breaker con Resilience4j
  - Timeout handling
  - Bulkhead pattern
  - Fallback responses
- [ ] Configurar PostgreSQL con réplicas (master-slave)
- [ ] Auto-scaling basado en métricas
- [ ] Blue-Green deployment strategy

### Fase 6: Seguridad Avanzada
- [ ] Implementar refresh token rotation
- [ ] 2FA (Two-Factor Authentication) para admins
- [ ] Audit logging completo
- [ ] Encryption at rest para datos sensibles
- [ ] HTTPS/TLS en producción
- [ ] Web Application Firewall (WAF)
- [ ] DDoS protection

### Fase 7: Testing y Calidad
- [ ] Tests de integración end-to-end
- [ ] Tests de carga con JMeter/Gatling
- [ ] Contract testing entre servicios
- [ ] Cobertura de código > 80%
- [ ] Mutation testing
- [ ] Security scanning (OWASP Dependency Check)

---

## 📞 Soporte y Contacto

**Desarrollador Principal**: David Delfino  
**Email**: daviddelfino97@hotmail.com  
**Proyecto**: PackedGo - Plataforma de Gestión de Eventos  
**Arquitectura**: Microservicios con Spring Boot + API Gateway  
**Última Actualización**: 15 de Diciembre de 2025  
**Versión**: 2.1 (CORS y Seguridad Actualizados)

---

## 📄 Licencia

Propiedad de PackedGo. Todos los derechos reservados.

---

**Fin del documento técnico**

Para más información sobre un microservicio específico, consultar su README correspondiente en las carpetas de cada servicio.
