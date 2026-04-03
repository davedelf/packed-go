# 📱 DOCUMENTACIÓN TÉCNICA - PACKEDGO FRONTEND

**Versión**: 1.0  
**Fecha**: 15 de diciembre de 2025  
**Framework**: Angular 19.2.0  
**Estado**: ✅ Sistema Completamente Operativo

---

## 📋 Tabla de Contenidos

1. [Visión General del Sistema](#-visión-general-del-sistema)
2. [Arquitectura Frontend](#-arquitectura-frontend)
3. [Stack Tecnológico](#-stack-tecnológico)
4. [Estructura del Proyecto](#-estructura-del-proyecto)
5. [Core - Servicios y Guards](#-core---servicios-y-guards)
6. [Features - Módulos Funcionales](#-features---módulos-funcionales)
7. [Routing y Navegación](#-routing-y-navegación)
8. [Autenticación y Seguridad](#-autenticación-y-seguridad)
9. [Gestión de Estado](#-gestión-de-estado)
10. [Integración con Backend](#-integración-con-backend)
11. [Guía de Desarrollo](#-guía-de-desarrollo)
12. [Despliegue](#-despliegue)

---

## 🎯 Visión General del Sistema

**PackedGo Frontend** es una aplicación web desarrollada en Angular que proporciona interfaces diferenciadas para tres tipos de usuarios: **Clientes**, **Administradores** y **Empleados**.

### Roles y Funcionalidades

#### 👥 CUSTOMER (Clientes)
- ✅ Explorar eventos disponibles (público y autenticado)
- ✅ Ver detalles completos de eventos
- ✅ Agregar tickets y consumiciones al carrito
- ✅ Procesar pagos con Stripe
- ✅ Ver historial de órdenes
- ✅ Gestionar perfil personal

#### 👨‍💼 ADMIN (Organizadores)
- ✅ Dashboard con métricas de negocio
- ✅ Crear y gestionar eventos
- ✅ Gestionar consumiciones y categorías
- ✅ Generar passes (tickets de entrada)
- ✅ Ver analytics detallados en tiempo real
- ✅ Gestionar empleados asignados
- ✅ Ver historial de ventas y pagos

#### 👷 EMPLOYEE (Empleados)
- ✅ Validar tickets mediante escaneo QR
- ✅ Registrar consumo de productos
- ✅ Ver eventos asignados
- ✅ Dashboard con actividad del día

---

## 🏗️ Arquitectura Frontend

### Diagrama de Arquitectura

```
┌──────────────────────────────────────────────────────────────────┐
│                      PACKEDGO FRONTEND                            │
│                   Angular 19.2.0 (Standalone)                     │
│                   http://localhost:3000                           │
└──────────────────────────┬───────────────────────────────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        │                                     │
        ▼                                     ▼
┌──────────────────┐              ┌──────────────────┐
│   PUBLIC ROUTES  │              │ PROTECTED ROUTES │
│  (No Auth)       │              │  (Auth Required) │
└──────────────────┘              └──────────────────┘
        │                                     │
        ├─ Landing Page                      ├─ Admin Module
        ├─ Events Explore                    │  ├─ Dashboard
        ├─ Event Detail                      │  ├─ Events Management
        ├─ Terms & Privacy                   │  ├─ Consumptions
        ├─ Login (Customer/Admin/Employee)   │  ├─ Analytics
        └─ Register (Customer/Admin)         │  └─ Employee Management
                                              │
                                              ├─ Customer Module
                                              │  ├─ Dashboard
                                              │  ├─ Checkout
                                              │  └─ Order Success
                                              │
                                              └─ Employee Module
                                                 └─ Validation Dashboard

                    ┌─────────────────────────┐
                    │   CORE LAYER            │
                    ├─────────────────────────┤
                    │ • Services (9)          │
                    │ • Guards (4)            │
                    │ • Interceptors (1)      │
                    └─────────────────────────┘
                              │
                              ▼
                    ┌─────────────────────────┐
                    │   API GATEWAY           │
                    │   localhost:8080        │
                    └─────────────────────────┘
```

### Principios Arquitectónicos

1. **Standalone Components**: Angular 19 sin NgModules tradicionales
2. **Lazy Loading**: Todos los features se cargan bajo demanda
3. **Service-Oriented**: Lógica de negocio en servicios inyectables
4. **Route Guards**: Protección de rutas por rol y estado de autenticación
5. **HTTP Interceptors**: Inyección automática de JWT en requests
6. **Reactive Programming**: RxJS para manejo de streams de datos
7. **TypeScript Strict**: Type safety completo
8. **Component-Based**: UI modular y reutilizable

---

## 🛠️ Stack Tecnológico

### Framework y Core

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Angular** | 19.2.0 | Framework principal |
| **TypeScript** | 5.7.2 | Lenguaje de programación |
| **RxJS** | 7.8.0 | Programación reactiva |
| **Angular Router** | 19.2.0 | Navegación y routing |
| **Angular Forms** | 19.2.0 | Formularios reactivos |

### UI y Estilos

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Bootstrap** | 5.3.8 | Framework CSS |
| **Popper.js** | 2.11.8 | Tooltips y popovers |
| **SweetAlert2** | 11.26.3 | Modales y alertas |
| **CSS Custom** | - | Estilos personalizados |

### Librerías Especializadas

| Librería | Versión | Propósito |
|----------|---------|-----------|
| **@zxing/ngx-scanner** | 20.0.0 | Escaneo de códigos QR |
| **@zxing/library** | 0.21.3 | Decodificación de códigos de barras |

### Herramientas de Desarrollo

| Herramienta | Versión | Propósito |
|-------------|---------|-----------|
| **Angular CLI** | 19.2.15 | Generación y build |
| **Karma** | 6.4.0 | Test runner |
| **Jasmine** | 5.6.0 | Testing framework |

---

## 📁 Estructura del Proyecto

```
front-angular/
├── src/
│   ├── app/
│   │   ├── core/                          # Servicios core y utilidades
│   │   │   ├── guards/                    # Route guards
│   │   │   │   ├── admin.guard.ts         # Protege rutas de admin
│   │   │   │   ├── auth.guard.ts          # Protege rutas autenticadas
│   │   │   │   ├── email-verified.guard.ts # Verifica email confirmado
│   │   │   │   └── employee.guard.ts      # Protege rutas de empleado
│   │   │   ├── interceptors/              # HTTP interceptors
│   │   │   │   └── auth.interceptor.ts    # Inyecta JWT + maneja 401/403
│   │   │   └── services/                  # Servicios de negocio
│   │   │       ├── analytics.service.ts   # Analytics y dashboard
│   │   │       ├── auth.service.ts        # Autenticación
│   │   │       ├── cart.service.ts        # Carrito de compras
│   │   │       ├── employee.service.ts    # Gestión de empleados
│   │   │       ├── event.service.ts       # Eventos
│   │   │       ├── order.service.ts       # Órdenes
│   │   │       ├── payment.service.ts     # Pagos con Stripe
│   │   │       ├── ticket.service.ts      # Tickets y validación
│   │   │       └── user.service.ts        # Perfiles de usuario
│   │   │
│   │   ├── features/                      # Módulos funcionales
│   │   │   ├── admin/                     # Features de administrador
│   │   │   │   ├── admin-analytics/       # Analytics detallados
│   │   │   │   ├── admin-dashboard/       # Dashboard principal
│   │   │   │   ├── categories-management/ # Gestión de categorías
│   │   │   │   ├── consumptions-management/ # Gestión de consumiciones
│   │   │   │   ├── employee-management/   # Gestión de empleados
│   │   │   │   └── events-management/     # CRUD de eventos
│   │   │   │
│   │   │   ├── auth/                      # Autenticación
│   │   │   │   ├── admin-login/           # Login de admin
│   │   │   │   ├── admin-register/        # Registro de admin
│   │   │   │   ├── customer-login/        # Login de cliente
│   │   │   │   ├── customer-register/     # Registro de cliente
│   │   │   │   ├── employee-login/        # Login de empleado
│   │   │   │   ├── verify-email/          # Confirmación de email
│   │   │   │   └── verify-email-required/ # Pantalla de email pendiente
│   │   │   │
│   │   │   ├── customer/                  # Features de cliente
│   │   │   │   ├── checkout/              # Proceso de pago
│   │   │   │   ├── customer-dashboard/    # Dashboard de cliente
│   │   │   │   ├── event-detail/          # Detalle de evento
│   │   │   │   └── order-success/         # Confirmación de compra
│   │   │   │
│   │   │   ├── employee/                  # Features de empleado
│   │   │   │   └── employee-dashboard/    # Dashboard de validación
│   │   │   │
│   │   │   ├── events-explore/            # Exploración pública de eventos
│   │   │   ├── landing/                   # Página de inicio
│   │   │   └── terms/                     # Términos y privacidad
│   │   │
│   │   ├── shared/                        # Componentes compartidos
│   │   │   ├── components/                # Componentes reutilizables
│   │   │   │   └── location-picker/       # Selector de ubicación
│   │   │   ├── models/                    # Interfaces TypeScript
│   │   │   │   ├── user.model.ts          # Modelos de usuario
│   │   │   │   ├── event.model.ts         # Modelos de evento
│   │   │   │   ├── order.model.ts         # Modelos de orden
│   │   │   │   └── ...
│   │   │   └── pipes/                     # Pipes personalizados
│   │   │       └── safe.pipe.ts           # Sanitización HTML
│   │   │
│   │   ├── app.component.ts               # Componente raíz
│   │   ├── app.config.ts                  # Configuración de app
│   │   └── app.routes.ts                  # Definición de rutas
│   │
│   ├── assets/                            # Assets estáticos
│   ├── environments/                      # Configuraciones de entorno
│   │   ├── environment.ts                 # Desarrollo
│   │   └── environment.prod.ts            # Producción
│   ├── index.html                         # HTML principal
│   ├── main.ts                            # Bootstrap de la app
│   └── styles.css                         # Estilos globales
│
├── public/                                # Archivos públicos
│   └── images/                            # Imágenes
├── angular.json                           # Configuración de Angular
├── package.json                           # Dependencias
├── proxy.conf.json                        # Proxy para API Gateway
├── tsconfig.json                          # Configuración TypeScript
└── README.md                              # Documentación

```

---

## 🔧 Core - Servicios y Guards

### Servicios Principales

#### 1. AuthService (`auth.service.ts`)

**Responsabilidad**: Gestión completa de autenticación y sesión

**Métodos Clave**:
```typescript
// Login por tipo de usuario
adminLogin(credentials: AdminLoginRequest): Observable<LoginResponse>
customerLogin(credentials: CustomerLoginRequest): Observable<LoginResponse>
employeeLogin(credentials: EmployeeLoginRequest): Observable<LoginResponse>

// Registro
adminRegister(data: AdminRegistrationRequest): Observable<LoginResponse>
customerRegister(data: CustomerRegistrationRequest): Observable<LoginResponse>

// Gestión de sesión
logout(): void
isAuthenticated(): boolean
getToken(): string | null
getCurrentUser(): AuthUser | null
getUserId(): number | null

// Email verification
verifyEmail(token: string): Observable<any>
resendVerificationEmail(email: string): Observable<any>
```

**Almacenamiento**:
- `localStorage.token` → JWT token
- `localStorage.refreshToken` → Refresh token
- `localStorage.currentUser` → Datos del usuario (JSON)

**Observable**:
```typescript
currentUser$: BehaviorSubject<AuthUser | null>
```

---

#### 2. EventService (`event.service.ts`)

**Responsabilidad**: CRUD de eventos y gestión de passes

**Métodos Clave**:
```typescript
// Eventos
getAllEvents(): Observable<Event[]>
getEventById(id: number): Observable<Event>
getMyEvents(): Observable<Event[]>
createEvent(event: FormData): Observable<Event>
updateEvent(id: number, event: FormData): Observable<Event>
deleteEvent(id: number): Observable<void>

// Passes
generatePasses(eventId: number, quantity: number): Observable<Pass[]>
getEventPasses(eventId: number): Observable<Pass[]>

// Consumiciones
getConsumptionsByEvent(eventId: number): Observable<Consumption[]>
createConsumption(consumption: Consumption): Observable<Consumption>
```

**Características**:
- Manejo de FormData para upload de imágenes
- Filtrado de eventos por categoría
- Gestión de passes y tickets

---

#### 3. CartService (`cart.service.ts`)

**Responsabilidad**: Gestión del carrito de compras

**Métodos Clave**:
```typescript
addToCart(item: CartItemRequest): Observable<CartItem>
getCart(): Observable<Cart>
updateCartItem(itemId: number, quantity: number): Observable<CartItem>
removeCartItem(itemId: number): Observable<void>
clearCart(): Observable<void>
checkout(): Observable<Order>
```

**Estado Local**:
```typescript
private cartSubject = new BehaviorSubject<Cart | null>(null);
public cart$ = this.cartSubject.asObservable();
```

---

#### 4. PaymentService (`payment.service.ts`)

**Responsabilidad**: Integración con Stripe

**Métodos Clave**:
```typescript
createPaymentIntent(orderId: number): Observable<PaymentIntent>
confirmPayment(paymentIntentId: string): Observable<Payment>
getPaymentsByOrder(orderId: number): Observable<Payment[]>
```

**Integración Stripe**:
- Payment Intents API
- Client Secret para confirmación
- Manejo de webhooks desde backend

---

#### 5. AnalyticsService (`analytics.service.ts`)

**Responsabilidad**: Dashboard y métricas para administradores

**Métodos Clave**:
```typescript
getDashboard(): Observable<DashboardData>
getDashboardByOrganizer(organizerId: number): Observable<DashboardData>
```

**Datos Retornados**:
```typescript
interface DashboardData {
  totalEvents: number;
  totalTicketsSold: number;
  totalRevenue: number;
  averageTicketPrice: number;
  events: EventMetrics[];
  recentOrders: Order[];
}
```

---

#### 6. EmployeeService (`employee.service.ts`)

**Responsabilidad**: Gestión de empleados y asignaciones

**Métodos Clave**:
```typescript
// CRUD de empleados
getAllEmployees(): Observable<Employee[]>
getEmployeeById(id: number): Observable<Employee>
createEmployee(employee: EmployeeRequest): Observable<Employee>
updateEmployee(id: number, employee: EmployeeRequest): Observable<Employee>
deleteEmployee(id: number): Observable<void>

// Asignaciones a eventos
assignToEvent(employeeId: number, eventId: number): Observable<void>
unassignFromEvent(employeeId: number, eventId: number): Observable<void>
getEmployeeEvents(): Observable<Event[]>
```

---

#### 7. TicketService (`ticket.service.ts`)

**Responsabilidad**: Validación de tickets y consumiciones

**Métodos Clave**:
```typescript
validateTicket(qrCode: string): Observable<ValidationResponse>
validateConsumption(qrCode: string): Observable<ValidationResponse>
getValidationHistory(): Observable<Validation[]>
```

**Integración QR**:
- Escaneo con `@zxing/ngx-scanner`
- Validación en tiempo real
- Feedback visual (success/error)

---

#### 8. OrderService (`order.service.ts`)

**Responsabilidad**: Gestión de órdenes

**Métodos Clave**:
```typescript
getMyOrders(): Observable<Order[]>
getOrderById(id: number): Observable<Order>
getOrdersByOrganizer(organizerId: number): Observable<Order[]>
```

---

#### 9. UserService (`user.service.ts`)

**Responsabilidad**: Gestión de perfiles

**Métodos Clave**:
```typescript
getMyProfile(): Observable<UserProfile>
updateMyProfile(profile: UserProfileUpdate): Observable<UserProfile>
```

---

### Route Guards

#### 1. authGuard (`auth.guard.ts`)

**Propósito**: Proteger rutas que requieren autenticación

**Lógica**:
```typescript
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // Guardar URL para redirección post-login
  router.navigate(['/customer/login'], {
    queryParams: { returnUrl: state.url }
  });
  return false;
};
```

**Aplica a**: Todas las rutas autenticadas

---

#### 2. adminGuard (`admin.guard.ts`)

**Propósito**: Proteger rutas exclusivas de administradores

**Lógica**:
```typescript
export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const user = authService.getCurrentUser();

  if (user && (user.role === 'ADMIN' || user.role === 'SUPER_ADMIN')) {
    return true;
  }

  router.navigate(['/']);
  return false;
};
```

**Aplica a**: `/admin/**`

---

#### 3. employeeGuard (`employee.guard.ts`)

**Propósito**: Proteger rutas exclusivas de empleados

**Lógica**:
```typescript
export const employeeGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const user = authService.getCurrentUser();

  if (user && user.role === 'EMPLOYEE') {
    return true;
  }

  router.navigate(['/']);
  return false;
};
```

**Aplica a**: `/employee/**`

---

#### 4. emailVerifiedGuard (`email-verified.guard.ts`)

**Propósito**: Verificar que el usuario haya confirmado su email

**Lógica**:
```typescript
export const emailVerifiedGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const user = authService.getCurrentUser();

  if (user?.isEmailVerified) {
    return true;
  }

  router.navigate(['/auth/verify-email-required']);
  return false;
};
```

**Aplica a**: Rutas de admin y customer que requieren email verificado

---

### HTTP Interceptor

#### authInterceptor (`auth.interceptor.ts`)

**Propósito**: Inyectar JWT automáticamente en todas las requests

**Funcionalidades**:

1. **Inyección de Token**:
```typescript
const token = authService.getToken();
const clonedReq = token 
  ? req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    })
  : req;
```

2. **Manejo de Errores 401/403**:
```typescript
catchError((error: HttpErrorResponse) => {
  if (error.status === 401 || error.status === 403) {
    authService.logout();
    
    // Determinar login route basado en URL actual
    let loginRoute = '/customer/login';
    if (currentUrl.includes('/admin')) {
      loginRoute = '/admin/login';
    } else if (currentUrl.includes('/employee')) {
      loginRoute = '/employee/login';
    }
    
    router.navigate([loginRoute], {
      queryParams: { returnUrl: currentUrl }
    });
  }
  
  return throwError(() => error);
})
```

3. **Redirección Inteligente**: Redirige al login correcto según el contexto

---

## 📦 Features - Módulos Funcionales

### Admin Module

#### 1. Admin Dashboard (`admin-dashboard.component.ts`)

**Ruta**: `/admin/dashboard`  
**Guards**: `adminGuard`, `emailVerifiedGuard`

**Funcionalidades**:
- ✅ Vista general de métricas del organizador
- ✅ Total de eventos activos/completados
- ✅ Gráfico de ventas mensuales
- ✅ Eventos más vendidos
- ✅ Accesos rápidos a gestión

**Dependencias**:
- `AnalyticsService` → Métricas
- `EventService` → Listado de eventos

---

#### 2. Events Management (`events-management.component.ts`)

**Ruta**: `/admin/events`  
**Guards**: `adminGuard`, `emailVerifiedGuard`

**Funcionalidades**:
- ✅ CRUD completo de eventos
- ✅ Upload de imágenes (base64)
- ✅ Generación de passes
- ✅ Vista de tickets vendidos
- ✅ Activar/Desactivar eventos

**Formulario de Evento**:
```typescript
eventForm = new FormGroup({
  name: new FormControl('', [Validators.required]),
  description: new FormControl('', [Validators.required]),
  location: new FormControl('', [Validators.required]),
  locationName: new FormControl(''),
  startDate: new FormControl('', [Validators.required]),
  endDate: new FormControl('', [Validators.required]),
  maxCapacity: new FormControl(0, [Validators.required, Validators.min(1)]),
  price: new FormControl(0, [Validators.required, Validators.min(0)]),
  eventCategoryId: new FormControl(null, [Validators.required]),
  image: new FormControl(null)
});
```

**Modal de Generación de Passes**:
- Input: Cantidad de passes a generar
- Confirmación con SweetAlert2
- Actualización automática de disponibilidad

---

#### 3. Consumptions Management (`consumptions-management.component.ts`)

**Ruta**: `/admin/consumptions`  
**Guards**: `adminGuard`, `emailVerifiedGuard`

**Funcionalidades**:
- ✅ CRUD de consumiciones (bebidas, comidas, etc.)
- ✅ Asignación a eventos
- ✅ Gestión de stock
- ✅ Precios y descripciones
- ✅ Upload de imágenes de productos

---

#### 4. Admin Analytics (`admin-analytics.component.ts`)

**Ruta**: `/admin/analytics`  
**Guards**: `adminGuard`, `emailVerifiedGuard`

**Funcionalidades**:
- ✅ Dashboard completo de analytics
- ✅ Gráficos de tendencias
- ✅ Métricas por evento
- ✅ Ingresos totales y promedios
- ✅ Tickets vendidos vs capacidad

**Métricas Mostradas**:
- Total de eventos
- Total de tickets vendidos
- Ingresos totales
- Precio promedio de ticket
- Tasa de ocupación por evento
- Ventas mensuales

---

#### 5. Employee Management (`employee-management.component.ts`)

**Ruta**: `/admin/employees`  
**Guards**: `adminGuard`, `emailVerifiedGuard`

**Funcionalidades**:
- ✅ CRUD de empleados
- ✅ Asignación a eventos específicos
- ✅ Generación de credenciales
- ✅ Historial de actividad
- ✅ Soft delete

**Formulario de Empleado**:
```typescript
employeeForm = new FormGroup({
  firstName: new FormControl('', [Validators.required]),
  lastName: new FormControl('', [Validators.required]),
  email: new FormControl('', [Validators.required, Validators.email]),
  document: new FormControl('', [Validators.required]),
  position: new FormControl('', [Validators.required])
});
```

---

### Customer Module

#### 1. Events Explore (`events-explore.component.ts`)

**Ruta**: `/events`  
**Guards**: Ninguno (público)

**Funcionalidades**:
- ✅ Listado de todos los eventos activos
- ✅ Filtrado por categoría
- ✅ Búsqueda por nombre
- ✅ Vista de grid con imágenes
- ✅ Redirección a detalle

---

#### 2. Event Detail (`event-detail.component.ts`)

**Ruta**: `/customer/event/:id`  
**Guards**: `authGuard`

**Funcionalidades**:
- ✅ Información completa del evento
- ✅ Galería de imágenes
- ✅ Listado de consumiciones disponibles
- ✅ Agregar tickets al carrito
- ✅ Agregar consumiciones al carrito
- ✅ Ver disponibilidad en tiempo real

**Agregar al Carrito**:
```typescript
addTicketToCart() {
  const cartItem: CartItemRequest = {
    eventId: this.event.id,
    itemType: 'TICKET',
    quantity: this.ticketQuantity,
    unitPrice: this.event.price
  };
  
  this.cartService.addToCart(cartItem).subscribe({
    next: () => {
      Swal.fire('Éxito', 'Ticket agregado al carrito', 'success');
    },
    error: (err) => {
      Swal.fire('Error', err.error.message, 'error');
    }
  });
}
```

---

#### 3. Checkout (`checkout.component.ts`)

**Ruta**: `/customer/checkout`  
**Guards**: `authGuard`

**Funcionalidades**:
- ✅ Vista del carrito completo
- ✅ Modificación de cantidades
- ✅ Eliminación de items
- ✅ Resumen de orden
- ✅ Integración con Stripe
- ✅ Confirmación de pago

**Flujo de Pago**:
```typescript
1. Usuario → Click en "Procesar Pago"
2. Frontend → POST /api/cart/checkout (crea orden)
3. Backend → Retorna orderId
4. Frontend → POST /api/payments/create-payment-intent (con orderId)
5. Backend → Crea Payment Intent en Stripe
6. Frontend → Redirige a Stripe Checkout o muestra formulario
7. Stripe → Procesa pago
8. Backend → Webhook de confirmación
9. Frontend → Redirige a /customer/order-success/:orderId
```

---

#### 4. Customer Dashboard (`customer-dashboard.component.ts`)

**Ruta**: `/customer/dashboard`  
**Guards**: `authGuard`

**Funcionalidades**:
- ✅ Perfil del usuario
- ✅ Historial de órdenes
- ✅ Tickets comprados (con QR)
- ✅ Próximos eventos
- ✅ Edición de perfil

---

### Employee Module

#### 1. Employee Dashboard (`employee-dashboard.component.ts`)

**Ruta**: `/employee/dashboard`  
**Guards**: `employeeGuard`

**Funcionalidades**:
- ✅ Escaneo de códigos QR
- ✅ Validación de tickets
- ✅ Validación de consumiciones
- ✅ Historial de validaciones del día
- ✅ Eventos asignados

**Componente de Escaneo QR**:
```typescript
<zxing-scanner
  [formats]="allowedFormats"
  (scanSuccess)="onScanSuccess($event)"
  (scanError)="onScanError($event)"
  [device]="currentDevice">
</zxing-scanner>
```

**Validación de Ticket**:
```typescript
onScanSuccess(qrCode: string) {
  this.ticketService.validateTicket(qrCode).subscribe({
    next: (response) => {
      if (response.valid) {
        Swal.fire({
          title: '✅ Ticket Válido',
          text: `Bienvenido ${response.customerName}`,
          icon: 'success',
          timer: 2000
        });
        this.playSuccessSound();
      } else {
        Swal.fire({
          title: '❌ Ticket Inválido',
          text: response.message,
          icon: 'error'
        });
        this.playErrorSound();
      }
    }
  });
}
```

---

### Auth Module

#### 1. Admin Login (`admin-login.component.ts`)

**Ruta**: `/admin/login`

**Campos**:
- Email
- Password

**Lógica**:
```typescript
onSubmit() {
  if (this.loginForm.invalid) return;
  
  const credentials: AdminLoginRequest = {
    email: this.loginForm.value.email!,
    password: this.loginForm.value.password!
  };
  
  this.authService.adminLogin(credentials).subscribe({
    next: (response) => {
      const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/admin/dashboard';
      this.router.navigate([returnUrl]);
    },
    error: (err) => {
      if (err.status === 403 && err.error.message.includes('verify your email')) {
        this.router.navigate(['/auth/verify-email-required']);
      } else {
        Swal.fire('Error', err.error.message || 'Credenciales inválidas', 'error');
      }
    }
  });
}
```

---

#### 2. Customer Login (`customer-login.component.ts`)

**Ruta**: `/customer/login`

**Campos**:
- Document (DNI)
- Password

**Diferencia clave**: Login por documento en lugar de email

---

#### 3. Verify Email (`verify-email.component.ts`)

**Ruta**: `/verify-email?token=...`

**Funcionalidad**:
```typescript
ngOnInit() {
  const token = this.route.snapshot.queryParams['token'];
  
  if (token) {
    this.authService.verifyEmail(token).subscribe({
      next: () => {
        Swal.fire({
          title: '✅ Email Verificado',
          text: 'Tu cuenta ha sido activada exitosamente',
          icon: 'success'
        }).then(() => {
          this.router.navigate(['/customer/login']);
        });
      },
      error: (err) => {
        Swal.fire({
          title: '❌ Error',
          text: 'Token inválido o expirado',
          icon: 'error'
        });
      }
    });
  }
}
```

---

## 🛣️ Routing y Navegación

### Estructura de Rutas

```typescript
export const routes: Routes = [
  // Públicas
  { path: '', component: LandingComponent },
  { path: 'events', component: EventsExploreComponent },
  { path: 'terms', component: TermsComponent },
  
  // Auth - Admin
  { path: 'admin/login', component: AdminLoginComponent },
  { path: 'admin/register', component: AdminRegisterComponent },
  
  // Auth - Customer
  { path: 'customer/login', component: CustomerLoginComponent },
  { path: 'customer/register', component: CustomerRegisterComponent },
  
  // Auth - Employee
  { path: 'employee/login', component: EmployeeLoginComponent },
  
  // Email Verification
  { path: 'verify-email', component: VerifyEmailComponent },
  { path: 'auth/verify-email-required', component: VerifyEmailRequiredComponent },
  
  // Admin (Protected)
  { 
    path: 'admin/dashboard', 
    component: AdminDashboardComponent,
    canActivate: [adminGuard, emailVerifiedGuard]
  },
  { 
    path: 'admin/events', 
    component: EventsManagementComponent,
    canActivate: [adminGuard, emailVerifiedGuard]
  },
  { 
    path: 'admin/consumptions', 
    component: ConsumptionsManagementComponent,
    canActivate: [adminGuard, emailVerifiedGuard]
  },
  { 
    path: 'admin/analytics', 
    component: AdminAnalyticsComponent,
    canActivate: [adminGuard, emailVerifiedGuard]
  },
  { 
    path: 'admin/employees', 
    component: EmployeeManagementComponent,
    canActivate: [adminGuard, emailVerifiedGuard]
  },
  
  // Customer (Protected)
  { 
    path: 'customer/dashboard', 
    component: CustomerDashboardComponent,
    canActivate: [authGuard]
  },
  { 
    path: 'customer/event/:id', 
    component: EventDetailComponent,
    canActivate: [authGuard]
  },
  { 
    path: 'customer/checkout', 
    component: CheckoutComponent,
    canActivate: [authGuard]
  },
  { 
    path: 'customer/order-success/:orderId', 
    component: OrderSuccessComponent,
    canActivate: [authGuard]
  },
  
  // Employee (Protected)
  { 
    path: 'employee/dashboard', 
    component: EmployeeDashboardComponent,
    canActivate: [employeeGuard]
  },
  
  // Wildcard
  { path: '**', redirectTo: '' }
];
```

### Lazy Loading

Todos los componentes usan lazy loading con `loadComponent`:

```typescript
{
  path: 'admin/events',
  loadComponent: () => import('./features/admin/events-management/events-management.component')
    .then(m => m.EventsManagementComponent),
  canActivate: [adminGuard, emailVerifiedGuard]
}
```

**Beneficios**:
- ✅ Reducción de bundle inicial
- ✅ Carga bajo demanda
- ✅ Mejor performance

---

## 🔐 Autenticación y Seguridad

### Flujo de Autenticación Completo

```
1. LOGIN
   Usuario → Formulario de login (admin/customer/employee)
   Frontend → POST /api/auth/{userType}/login
   Backend → Valida credenciales
   Backend → Genera JWT + Refresh Token
   Frontend → Almacena en localStorage:
              - token
              - refreshToken
              - currentUser (JSON)
   Frontend → Actualiza currentUser$ (BehaviorSubject)
   Frontend → Redirige a dashboard correspondiente

2. REQUEST AUTENTICADA
   Frontend → HTTP Request a cualquier endpoint
   authInterceptor → Inyecta header: Authorization: Bearer {token}
   API Gateway → Valida JWT
   Backend → Procesa request
   Frontend → Recibe respuesta

3. TOKEN EXPIRADO (401/403)
   Backend → Retorna 401 Unauthorized
   authInterceptor → Detecta error
   authInterceptor → authService.logout()
   authInterceptor → Limpia localStorage
   authInterceptor → Redirige a login correcto con returnUrl

4. LOGOUT
   Usuario → Click en "Cerrar Sesión"
   Frontend → authService.logout()
   Frontend → Limpia localStorage
   Frontend → currentUser$.next(null)
   Frontend → Redirige a landing page
```

### Almacenamiento de Sesión

**localStorage Items**:
```typescript
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "refresh_token_value",
  "currentUser": {
    "id": 2,
    "username": "admin1",
    "email": "admin1@test.com",
    "role": "ADMIN",
    "isEmailVerified": true,
    "firstName": "Juan",
    "lastName": "Pérez"
  }
}
```

### Validación de Token

```typescript
isAuthenticated(): boolean {
  const token = this.getToken();
  if (!token) return false;

  try {
    // Decodificar payload JWT
    const payload = JSON.parse(atob(token.split('.')[1]));
    const expiration = payload.exp * 1000; // ms
    const now = Date.now();
    
    if (now >= expiration) {
      this.logout();
      return false;
    }
    
    return true;
  } catch (error) {
    this.logout();
    return false;
  }
}
```

### Protección de Rutas

**Combinación de Guards**:

```typescript
// Ruta de admin con email verificado
{
  path: 'admin/dashboard',
  component: AdminDashboardComponent,
  canActivate: [adminGuard, emailVerifiedGuard]
}

// Ruta de customer autenticado (sin verificar email necesario)
{
  path: 'customer/checkout',
  component: CheckoutComponent,
  canActivate: [authGuard]
}

// Ruta de empleado
{
  path: 'employee/dashboard',
  component: EmployeeDashboardComponent,
  canActivate: [employeeGuard]
}
```

### Seguridad Adicional

1. **CORS**: Configurado en API Gateway (backend)
2. **XSS Protection**: Sanitización con `DomSanitizer`
3. **CSRF**: No necesario (JWT stateless)
4. **HttpOnly Cookies**: No usado (JWT en localStorage)
5. **Content Security Policy**: Configurado en index.html

---

## 📊 Gestión de Estado

### Servicios con BehaviorSubject

**AuthService** - Usuario actual:
```typescript
private currentUserSubject = new BehaviorSubject<AuthUser | null>(this.getUserFromStorage());
public currentUser$ = this.currentUserSubject.asObservable();

// Componentes se suscriben:
this.authService.currentUser$.subscribe(user => {
  this.currentUser = user;
});
```

**CartService** - Carrito:
```typescript
private cartSubject = new BehaviorSubject<Cart | null>(null);
public cart$ = this.cartSubject.asObservable();

// Actualizar carrito:
this.getCart().subscribe(cart => {
  this.cartSubject.next(cart);
});
```

### Local Storage como Persistencia

**Datos Almacenados**:
- `token` → JWT
- `refreshToken` → Refresh token
- `currentUser` → Datos del usuario

**No se almacena**:
- Carrito (se obtiene del backend)
- Eventos (se obtienen del backend)
- Órdenes (se obtienen del backend)

**Razón**: Evitar inconsistencias con el backend

---

## 🌐 Integración con Backend

### Configuración de Ambiente

**environment.ts**:
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',  // API Gateway
  authServiceUrl: 'http://localhost:8080/api',
  usersServiceUrl: 'http://localhost:8080/api',
  eventServiceUrl: 'http://localhost:8080/api',
  ordersServiceUrl: 'http://localhost:8080/api',
  paymentsServiceUrl: 'http://localhost:8080/api',
  analyticsServiceUrl: 'http://localhost:8080/api',
};
```

**Nota**: Todas las URLs apuntan al API Gateway en puerto 8080

### Proxy Configuration

**proxy.conf.json**:
```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  }
}
```

**Uso**:
```bash
ng serve --proxy-config proxy.conf.json
```

### Manejo de Respuestas del Backend

**Respuesta envuelta**:
```typescript
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
```

**Extracción de data**:
```typescript
return this.http.post<ApiResponse<LoginResponse>>(`${this.apiUrl}/auth/admin/login`, credentials)
  .pipe(
    map(response => response.data),  // Extraer solo .data
    tap(loginData => {
      this.saveAuthData(loginData);
    })
  );
```

### Manejo de Errores

**Centralizado en Interceptor**:
```typescript
catchError((error: HttpErrorResponse) => {
  if (error.status === 401 || error.status === 403) {
    // Token inválido o expirado
    this.authService.logout();
    this.router.navigate(['/login']);
  }
  
  if (error.status === 404) {
    Swal.fire('Error', 'Recurso no encontrado', 'error');
  }
  
  if (error.status === 500) {
    Swal.fire('Error', 'Error interno del servidor', 'error');
  }
  
  return throwError(() => error);
})
```

**En Componentes**:
```typescript
this.eventService.createEvent(formData).subscribe({
  next: (event) => {
    Swal.fire('Éxito', 'Evento creado correctamente', 'success');
    this.router.navigate(['/admin/events']);
  },
  error: (err) => {
    Swal.fire('Error', err.error.message || 'Error al crear evento', 'error');
  }
});
```

---

## 💻 Guía de Desarrollo

### Requisitos Previos

- Node.js 18+
- npm 9+
- Angular CLI 19.2.15
- Backend corriendo en localhost:8080

### Instalación

```bash
# 1. Clonar repositorio
cd C:\Users\david\Documents\ps-packedgo\packedgo\front-angular

# 2. Instalar dependencias
npm install

# 3. Verificar versión de Angular
ng version

# 4. Iniciar servidor de desarrollo
npm start
# o
ng serve --proxy-config proxy.conf.json
```

### Desarrollo Local

**Puerto**: `http://localhost:3000` (configurado en package.json)

**Hot Reload**: ✅ Habilitado automáticamente

**Proxy**: Todas las llamadas a `/api/*` se redirigen a `http://localhost:8080`

### Generación de Componentes

```bash
# Componente standalone
ng generate component features/admin/new-feature --standalone

# Servicio
ng generate service core/services/new-service

# Guard
ng generate guard core/guards/new-guard

# Pipe
ng generate pipe shared/pipes/new-pipe

# Interface
ng generate interface shared/models/new-model
```

### Estructura de Componente Típica

```typescript
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SomeService } from '../../core/services/some.service';

@Component({
  selector: 'app-my-component',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './my-component.component.html',
  styleUrls: ['./my-component.component.css']
})
export class MyComponent implements OnInit {
  
  constructor(private someService: SomeService) {}
  
  ngOnInit(): void {
    this.loadData();
  }
  
  loadData(): void {
    this.someService.getData().subscribe({
      next: (data) => {
        console.log('Data loaded:', data);
      },
      error: (err) => {
        console.error('Error loading data:', err);
      }
    });
  }
}
```

### Testing

```bash
# Ejecutar tests unitarios
ng test

# Ejecutar con cobertura
ng test --code-coverage

# Ver reporte de cobertura
# Abre coverage/index.html en el navegador
```

### Build

```bash
# Build de desarrollo
ng build

# Build de producción
ng build --configuration production

# Output: dist/front-angular/
```

### Linting y Formateo

```bash
# Lint (si configurado)
ng lint

# Format con Prettier (si instalado)
npx prettier --write "src/**/*.{ts,html,css}"
```

---

## 🚀 Despliegue

### Build de Producción

```bash
# 1. Build optimizado
ng build --configuration production

# 2. Output generado en: dist/front-angular/

# 3. Archivos generados:
#    - index.html
#    - main.*.js (bundle principal)
#    - polyfills.*.js
#    - styles.*.css
#    - assets/
```

### Configuración de Producción

**environment.prod.ts**:
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.packedgo.com/api',  // URL de producción
  authServiceUrl: 'https://api.packedgo.com/api',
  usersServiceUrl: 'https://api.packedgo.com/api',
  eventServiceUrl: 'https://api.packedgo.com/api',
  ordersServiceUrl: 'https://api.packedgo.com/api',
  paymentsServiceUrl: 'https://api.packedgo.com/api',
  analyticsServiceUrl: 'https://api.packedgo.com/api',
};
```

### Despliegue en Servidor Web

#### Nginx

```nginx
server {
    listen 80;
    server_name packedgo.com www.packedgo.com;
    
    root /var/www/packedgo-frontend/dist/front-angular/browser;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # Cache para assets estáticos
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # API Proxy (si no se usa API Gateway externo)
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### Apache

```apache
<VirtualHost *:80>
    ServerName packedgo.com
    DocumentRoot /var/www/packedgo-frontend/dist/front-angular/browser
    
    <Directory /var/www/packedgo-frontend/dist/front-angular/browser>
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted
        
        # Habilitar mod_rewrite para SPA
        RewriteEngine On
        RewriteBase /
        RewriteRule ^index\.html$ - [L]
        RewriteCond %{REQUEST_FILENAME} !-f
        RewriteCond %{REQUEST_FILENAME} !-d
        RewriteRule . /index.html [L]
    </Directory>
    
    # Proxy para API
    ProxyPass /api http://localhost:8080/api
    ProxyPassReverse /api http://localhost:8080/api
</VirtualHost>
```

### Despliegue en Docker

**Dockerfile**:
```dockerfile
# Build stage
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

# Production stage
FROM nginx:alpine
COPY --from=build /app/dist/front-angular/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**docker-compose.yml**:
```yaml
version: '3.8'

services:
  frontend:
    build: .
    ports:
      - "3000:80"
    environment:
      - NODE_ENV=production
    depends_on:
      - api-gateway
    networks:
      - packedgo-network

networks:
  packedgo-network:
    external: true
```

### Optimizaciones de Producción

1. **AOT Compilation**: Habilitado por defecto en producción
2. **Tree Shaking**: Eliminación de código no usado
3. **Minification**: Código minificado y ofuscado
4. **Bundle Splitting**: Código dividido en chunks
5. **Lazy Loading**: Módulos cargados bajo demanda
6. **Service Worker**: Para PWA (si configurado)

---

## 📚 Características Especiales

### 1. Escaneo de Códigos QR

**Librería**: `@zxing/ngx-scanner`

**Implementación**:
```typescript
// employee-dashboard.component.ts
<zxing-scanner
  [formats]="['QR_CODE']"
  (scanSuccess)="onScanSuccess($event)"
  (scanError)="onScanError($event)"
  [device]="currentDevice">
</zxing-scanner>

onScanSuccess(qrCode: string) {
  this.ticketService.validateTicket(qrCode).subscribe({
    next: (response) => {
      if (response.valid) {
        this.showSuccessAlert(response);
      } else {
        this.showErrorAlert(response);
      }
    }
  });
}
```

**Permisos de Cámara**: Solicitados automáticamente por el navegador

---

### 2. Upload de Imágenes

**Conversión a Base64**:
```typescript
onFileSelect(event: any) {
  const file = event.target.files[0];
  if (file) {
    const reader = new FileReader();
    reader.onload = () => {
      this.imagePreview = reader.result as string;
      this.eventForm.patchValue({ image: reader.result });
    };
    reader.readAsDataURL(file);
  }
}
```

**Envío al Backend**:
```typescript
const formData = new FormData();
formData.append('name', this.eventForm.value.name);
formData.append('image', this.imagePreview);  // Base64

this.eventService.createEvent(formData).subscribe(...);
```

---

### 3. Integración con Stripe

**Payment Intent**:
```typescript
processPayment() {
  // 1. Crear orden
  this.cartService.checkout().subscribe(order => {
    // 2. Crear Payment Intent
    this.paymentService.createPaymentIntent(order.id).subscribe(paymentIntent => {
      // 3. Redirigir a Stripe Checkout
      window.location.href = paymentIntent.checkoutUrl;
    });
  });
}
```

**Confirmación Post-Pago**:
```typescript
// order-success.component.ts
ngOnInit() {
  const orderId = this.route.snapshot.params['orderId'];
  const paymentIntentId = this.route.snapshot.queryParams['payment_intent'];
  
  if (paymentIntentId) {
    this.paymentService.confirmPayment(paymentIntentId).subscribe({
      next: () => {
        this.showSuccessMessage();
      }
    });
  }
}
```

---

### 4. SweetAlert2 para Feedback

**Confirmaciones**:
```typescript
confirmDelete(eventId: number) {
  Swal.fire({
    title: '¿Estás seguro?',
    text: 'Esta acción no se puede deshacer',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText: 'Sí, eliminar',
    cancelButtonText: 'Cancelar'
  }).then((result) => {
    if (result.isConfirmed) {
      this.eventService.deleteEvent(eventId).subscribe({
        next: () => {
          Swal.fire('Eliminado', 'Evento eliminado correctamente', 'success');
          this.loadEvents();
        }
      });
    }
  });
}
```

**Notificaciones**:
```typescript
Swal.fire({
  toast: true,
  position: 'top-end',
  icon: 'success',
  title: 'Guardado exitosamente',
  showConfirmButton: false,
  timer: 3000
});
```

---

## 🔍 Troubleshooting

### 1. Error: Cannot GET /api/...

**Causa**: Proxy no configurado o API Gateway no corriendo

**Solución**:
```bash
# 1. Verificar que API Gateway está corriendo
curl http://localhost:8080/actuator/health

# 2. Iniciar con proxy
npm start
# o
ng serve --proxy-config proxy.conf.json
```

---

### 2. Error: Token expirado (401)

**Causa**: JWT expirado (1 hora por defecto)

**Solución**: El interceptor redirige automáticamente al login

**Manual**:
```typescript
// Borrar sesión manualmente
localStorage.clear();
window.location.href = '/customer/login';
```

---

### 3. Error: CORS

**Síntoma**: `Access to XMLHttpRequest has been blocked by CORS policy`

**Causa**: CORS no configurado en API Gateway

**Solución**: Verificar `application.yml` del API Gateway:
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "http://localhost:3000"
```

---

### 4. Error: Cámara no funciona para QR

**Causa**: Permisos de cámara no otorgados

**Solución**:
1. Navegador debe solicitar permisos
2. Usuario debe aceptar
3. En Chrome: Settings → Privacy → Camera → Permitir para localhost

**Alternativa**: Usar HTTPS (requerido en producción)

---

### 5. Build falla: Memory error

**Síntoma**: `JavaScript heap out of memory`

**Solución**:
```bash
# Aumentar memoria de Node.js
export NODE_OPTIONS="--max-old-space-size=4096"
ng build --configuration production
```

---

## 📞 Soporte

**Desarrollador**: David Delfino  
**Email**: daviddelfino97@hotmail.com  
**Proyecto**: PackedGo Frontend  
**Framework**: Angular 19.2.0  
**Última Actualización**: 15 de Diciembre de 2025  

---

## 📄 Licencia

Propiedad de PackedGo. Todos los derechos reservados.
