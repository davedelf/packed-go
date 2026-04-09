# 🎟️ PackedGo - Sistema de Gestión de Eventos

<div align="center">

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat&logo=java&logoColor=white)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-6DB33F?style=flat&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-19-DD0031?style=flat&logo=angular&logoColor=white)](https://angular.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?style=flat&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.x-FF6600?style=flat&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**Plataforma completa de microservicios para gestión de eventos, venta de tickets y analytics en tiempo real**

</div>

---

## 📋 Índice

1. [Acerca del Proyecto](#-acerca-del-proyecto)
2. [Características](#-características)
3. [Arquitectura](#-arquitectura)
4. [Stack Tecnológico](#-stack-tecnológico)
5. [Primeros Pasos](#-primeros-pasos)
6. [Configuración](#-configuración)
7. [API Endpoints](#-api-endpoints)
8. [Contribución](#-contribución)
9. [Licencia](#-licencia)

---

## 🎯 Acerca del Proyecto

PackedGo es una plataforma de gestión de eventos desarrollada con **arquitectura de microservicios** que permite a organizadores crear eventos, vender tickets, gestionar consumiciones y analizar métricas en tiempo real.

### ¿Por qué este proyecto?

- ✅ **Escalabilidad**: Cada microservicio puede escalar independientemente
- ✅ **Mantenibilidad**: Código modular, fácil de entender y extender
- ✅ **Resiliencia**: Fallo en un servicio no afecta a los demás
- ✅ **Moderno**: Spring Boot 3.5 + Angular 19 + Java 17
- ✅ **Patterns**: Implementa Transactional Outbox Pattern para consistencia eventual

---

## ✨ Características

| Módulo | Funcionalidades |
|--------|----------------|
| **Eventos** | CRUD completo, categorías, imágenes, capacidad máxima |
| **Tickets** | Generación QR, validación de entrada única, control de stock |
| **Pagos** | Stripe integration, webhooks, estados PENDING/APPROVED/REJECTED |
| **Carrito** | Multi-item, expiración automática (10 min), validación de stock |
| **Empleados** | Asignación a eventos, validación QR, registro de consumos |
| **Analytics** | Dashboard en tiempo real, métricas por organizador |
| **Seguridad** | JWT, roles (ADMIN/CUSTOMER/EMPLOYEE), Anti-Spoofing |

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         FRONTEND (Angular 19)                           │
│                           localhost:3000                                │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │ HTTP + JWT Bearer
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     API GATEWAY (Spring Cloud)                          │
│                              :8080                                      │
│  ┌─────────────┐ ┌────────────┐ ┌─────────────┐ ┌─────────────────┐   │
│  │    CORS     │ │   JWT      │ │   Header    │ │  Anti-Spoofing  │   │
│  │             │ │ Validation │ │  Injection  │ │  (X-User-Id)   │   │
│  └─────────────┘ └────────────┘ └─────────────┘ └─────────────────┘   │
└───────────────────────────────┬───────────────────────────────────────┘
                                │
    ┌──────────┬───────┬────────┬────────┬────────┬───────┐
    │          │       │        │        │        │       │
 ┌──▼───┐ ┌──▼──┐ ┌─▼───┐ ┌──▼──┐ ┌──▼──┐ ┌──▼──┐ ┌──▼────┐
 │ Auth │ │Users│ │Event│ │Order│ │Payment│ │Analytics│
 │  :8081│ │:8082│ │:8086│ │:8084│ │:8085 │ │ :8087 │
 └──┬───┘ └──┬──┘ └──┬──┘ └──┬──┘ └──┬──┘ └───┬───┘
    │        │        │        │        │         │
    └────────┴────────┴────────┴────────┴─────────┘
                         │
        ┌────────────────┴────────────────┐
        │   POSTGRES CENTRAL (1 컨tenedor) │
        │  auth_db | users_db | event_db   │
        │  order_db | payment_db           │
        └─────────────────────────────────┘

        ┌─────────────────────────────────┐
        │        RABBITMQ                 │
        │  Outbox Pattern + DLQ           │
        └─────────────────────────────────┘
                    │
         order-service ──► RabbitMQ ──► event-service
         (Transactional Outbox Pattern)
```

### Microservicios

| Servicio | Puerto | Responsabilidad |
|----------|--------|-----------------|
| **API Gateway** | 8080 | Enrutamiento, JWT, CORS |
| **auth-service** | 8081 | Autenticación, registro |
| **users-service** | 8082 | Perfiles, empleados |
| **event-service** | 8086 | Eventos, tickets |
| **order-service** | 8084 | Carritos, órdenes |
| **payment-service** | 8085 | Pagos con Stripe |
| **analytics-service** | 8087 | Dashboard, estadísticas |

---

## 🛠️ Stack Tecnológico

### Backend
| Tecnología | Versión |
|-------------|---------|
| Java | 17 |
| Spring Boot | 3.5.x |
| Spring Cloud Gateway | 2023.0.0 |
| PostgreSQL | 15-alpine |
| RabbitMQ | 3-management |
| JWT (jjwt) | 0.12.x |
| Stripe SDK | 26.7.0 |

### Frontend
| Tecnología | Versión |
|------------|---------|
| Angular | 19.x |
| TypeScript | 5.7.x |
| Bootstrap | 5.3.x |
| RxJS | 7.8.x |

### Infraestructura
| Tecnología | Propósito |
|-------------|-----------|
| Docker | Contenedores |
| Docker Compose | Orquestación |
| Maven | Build Java |

---

## 🚀 Primeros Pasos

### Requisitos

| Requisito | Versión Mínima |
|-----------|----------------|
| Java | 17+ |
| Maven | 3.9+ |
| Docker Desktop | Latest |
| Node.js | 18+ |
| npm | 9+ |

### Instalación Rápida

```bash
# 1. Clonar el repositorio
git clone https://github.com/tu-usuario/packed-go.git
cd packed-go/app

# 2. Configurar variables de entorno
cd back
cp auth-service/.env.example auth-service/.env
# Editar .env con tus credenciales

# 3. Iniciar infraestructura (PostgreSQL + RabbitMQ)
docker-compose up -d postgres-central rabbitmq

# 4. Compilar todos los servicios
for dir in auth-service users-service event-service order-service payment-service analytics-service api-gateway; do
  cd $dir && mvn clean package -DskipTests && cd ..
done

# 5. Iniciar todos los servicios
docker-compose up -d

# 6. Instalar y ejecutar frontend
cd ../front-angular
npm install
npm start
```

### Verificar que todo funciona

```bash
# Health checks
curl http://localhost:8080/actuator/health   # API Gateway
curl http://localhost:8081/actuator/health  # Auth Service
curl http://localhost:8082/actuator/health  # Users Service
curl http://localhost:8086/actuator/health  # Event Service
curl http://localhost:8084/actuator/health  # Order Service
curl http://localhost:8085/actuator/health  # Payment Service
curl http://localhost:8087/actuator/health  # Analytics Service

# Test endpoint público
curl http://localhost:8080/api/events
```

---

## ⚙️ Configuración

### Variables de Entorno Requeridas

```env
# JWT (generar una clave segura para producción)
JWT_SECRET=your-super-secret-key-change-in-production

# Stripe (obtener de https://dashboard.stripe.com/test/apikeys)
STRIPE_API_KEY=sk_test_your_stripe_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret

# Frontend
FRONTEND_URL=http://localhost:3000
```

### Puertos de Servicios

| Servicio | Puerto | URL |
|----------|--------|-----|
| Frontend | 3000 | http://localhost:3000 |
| API Gateway | 8080 | http://localhost:8080 |
| RabbitMQ UI | 15672 | http://localhost:15672 |
| PostgreSQL | 5432 | localhost:5432 |

---

## 📡 API Endpoints

### Autenticación (Públicos)

```bash
# Login cliente
POST /api/auth/customer/login
Body: {"document": "12345678", "password": "Customer123!"}

# Login administrador
POST /api/auth/admin/login
Body: {"email": "admin@test.com", "password": "Admin123!"}

# Registro cliente
POST /api/auth/customer/register
Body: {"username": "john", "email": "john@test.com", "document": "87654321", "password": "Pass123!", "firstName": "John", "lastName": "Doe"}
```

### Eventos (Públicos)

```bash
# Listar eventos
GET /api/events

# Detalle evento
GET /api/events/{id}

# Crear evento (ADMIN)
POST /api/events
Authorization: Bearer {token}
```

### Carrito y Órdenes

```bash
# Agregar al carrito
POST /api/cart/add
Authorization: Bearer {token}
Body: {"eventId": 1, "quantity": 2}

# Checkout
POST /api/orders/checkout
Authorization: Bearer {token}
```

### Pagos

```bash
# Crear sesión Stripe
POST /api/payments/create-checkout-stripe
Authorization: Bearer {token}
Body: {"orderId": 1}
```

### Dashboard (ADMIN)

```bash
# Métricas del organizador
GET /api/dashboard
Authorization: Bearer {token}
```

---

## 🤝 Contribución

¡Las contribuciones son bienvenidas! Por favor lee nuestra guía de contribución.

### Enviar un Pull Request

1. Haz fork del proyecto
2. Crea tu rama de feature (`git checkout -b feature/amazing-feature`)
3. Haz commit de tus cambios (`git commit -m 'feat: add amazing feature'`)
4. Push a la rama (`git push origin feature/amazing-feature`)
5. Abre un Pull Request

### Estándares de Código

- **Backend**: Lombok, DTOs, Services, Repositories, Bean Validation
- **Frontend**: Componentes standalone (Angular 19), TypeScript strict, Lazy loading

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para más detalles.

---

## 📄 Licencia

Este proyecto es **Open Source**. Puedes usarlo, modificarlo y distribuirlo libremente.