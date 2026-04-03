# 🔒 Configuración de Seguridad - PackedGo

## ⚠️ IMPORTANTE: Configuración de Variables de Entorno

Este proyecto requiere archivos `.env` para cada microservicio con datos sensibles. **NUNCA** commits estos archivos a Git.

## 📝 Pasos para Configurar el Proyecto

### 1. Copiar Archivos de Plantilla

Cada microservicio tiene un archivo `.env.example` con la estructura necesaria. Copia y renombra cada uno:

```bash
# Auth Service
cp back/auth-service/.env.example back/auth-service/.env

# Users Service
cp back/users-service/.env.example back/users-service/.env

# Event Service
cp back/event-service/.env.example back/event-service/.env

# Order Service
cp back/order-service/.env.example back/order-service/.env

# Payment Service
cp back/payment-service/.env.example back/payment-service/.env

# Analytics Service
cp back/analytics-service/.env.example back/analytics-service/.env
```

### 2. Configurar Variables Sensibles

Edita cada archivo `.env` y completa con tus valores reales:

#### 🔑 Claves Requeridas

##### Auth Service (.env)
- `JWT_SECRET`: Genera una clave secreta (mínimo 32 caracteres)
- `MAIL_USERNAME`: Tu email de Gmail
- `MAIL_PASSWORD`: App Password de Gmail ([generar aquí](https://myaccount.google.com/apppasswords))
- `DATABASE_PASSWORD`: Contraseña segura para PostgreSQL

##### Payment Service (.env)
- `STRIPE_SECRET_KEY`: Tu clave secreta de Stripe ([obtener aquí](https://dashboard.stripe.com/apikeys))
- `STRIPE_WEBHOOK_SECRET`: Secreto del webhook de Stripe

##### Otros Servicios
- Actualiza las contraseñas de base de datos en cada servicio
- Verifica las URLs de servicios si no usas Docker

### 3. Generar Claves Seguras

```bash
# JWT Secret (PowerShell)
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | ForEach-Object {[char]$_})

# Database Password
-join ((48..57) + (65..90) + (97..122) + (33,35,37,38,42) | Get-Random -Count 16 | ForEach-Object {[char]$_})
```

## 🚀 Iniciar el Proyecto

Una vez configurados todos los `.env`:

```bash
# Con Docker
cd back
docker-compose up -d

# O servicios individuales
cd back/auth-service
mvn spring-boot:run
```

## ⚡ Variables de Entorno Críticas

| Servicio | Variable | Descripción |
|----------|----------|-------------|
| auth-service | `JWT_SECRET` | Clave para firmar tokens JWT |
| auth-service | `MAIL_PASSWORD` | App Password de Gmail |
| payment-service | `STRIPE_SECRET_KEY` | Clave secreta de Stripe |
| payment-service | `STRIPE_WEBHOOK_SECRET` | Secreto de webhook Stripe |
| Todos | `DATABASE_PASSWORD` | Contraseña de PostgreSQL |

## 🔍 Verificar Configuración

```bash
# Verificar que los .env NO estén en Git
git status

# Debe mostrar: .env files are untracked
```

## 📧 Configurar Gmail SMTP

1. Ve a tu cuenta de Google → Seguridad
2. Habilita "Verificación en 2 pasos"
3. Genera una "Contraseña de aplicación" para "Correo"
4. Usa esa contraseña en `MAIL_PASSWORD`

## 🌐 Configurar Stripe

1. Crea cuenta en [Stripe](https://dashboard.stripe.com/register)
2. Obtén las claves de API en modo "Test"
3. Configura webhook: `http://localhost:8080/api/webhooks/stripe`
4. Copia el secreto del webhook

## ⚠️ Nunca Commitear

- ❌ `.env` files
- ❌ API keys
- ❌ Passwords
- ❌ JWT secrets
- ❌ Database credentials
- ❌ Email passwords

## ✅ Sí Commitear

- ✅ `.env.example` files (sin datos reales)
- ✅ Documentación de setup
- ✅ Scripts de inicialización (sin secretos)
- ✅ Configuración de desarrollo (valores por defecto)

---

**Última actualización**: 15 de diciembre de 2025  
**Versión**: 2.1
