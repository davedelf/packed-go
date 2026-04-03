import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { Subscription, forkJoin, of, from } from 'rxjs';
import { catchError, concatMap, delay, timeout } from 'rxjs/operators';
import Swal from 'sweetalert2';
import { AuthService } from '../../../core/services/auth.service';
import { EventService } from '../../../core/services/event.service';
import { UserService } from '../../../core/services/user.service';
import { CartService } from '../../../core/services/cart.service';
import { TicketService, Ticket } from '../../../core/services/ticket.service';
import { OrderService } from '../../../core/services/order.service';
import { Event } from '../../../shared/models/event.model';
import { Cart } from '../../../shared/models/cart.model';

type TabType = 'events' | 'cart' | 'tickets' | 'profile';

@Component({
  selector: 'app-customer-dashboard',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './customer-dashboard.component.html',
  styleUrl: './customer-dashboard.component.css'
})
export class CustomerDashboardComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private eventService = inject(EventService);
  private userService = inject(UserService);
  private cartService = inject(CartService);
  private ticketService = inject(TicketService);
  private orderService = inject(OrderService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);

  // User & Navigation
  currentUser = this.authService.getCurrentUser();
  activeTab: TabType = 'events';
  
  // Events Section
  isLoadingEvents = true;
  events: Event[] = [];
  allEvents: Event[] = [];
  filteredEvents: Event[] = [];
  paginatedEvents: Event[] = [];
  searchTerm = '';
  selectedCategoryFilter: number | null = null;
  currentPage = 1;
  itemsPerPage = 12;
  totalPages = 1;
  categories: any[] = [];
  private addressCache = new Map<string, string>();

  // Cart Section
  cart: Cart | null = null;
  groupedItems: { adminId: number, items: any[], total: number }[] = [];
  private cartSubscription?: Subscription;

  // Tickets Section
  isLoadingTickets = false;
  myTickets: Ticket[] = [];
  showQrModal = false;
  selectedTicket: Ticket | null = null;

  // Profile Section
  isLoadingProfile = false;
  isEditingProfile = false;
  profileForm: FormGroup;
  changePasswordForm: FormGroup;
  originalProfileData: any = null;
  userAuthData: any = null; // Para username, email, document (readonly)

  // Modal para agregar consumiciones
  showModal = false;
  selectedItemId: number | null = null;
  availableConsumptions: any[] = [];
  isLoadingConsumptions = false;

  constructor() {
    // Formulario de perfil - DESHABILITADO por defecto
    this.profileForm = this.fb.group({
      name: [{value: '', disabled: true}, [Validators.required, Validators.minLength(2)]],
      lastName: [{value: '', disabled: true}, [Validators.required, Validators.minLength(2)]],
      telephone: [{value: '', disabled: true}, [Validators.required, Validators.pattern(/^\d{10}$/)]],
      gender: [{value: '', disabled: true}, Validators.required],
      bornDate: [{value: '', disabled: true}, Validators.required],
      profileImageUrl: [{value: '', disabled: true}]
    });

    // Formulario de cambio de contraseña
    this.changePasswordForm = this.fb.group({
      currentPassword: ['', [Validators.required, Validators.minLength(6)]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required, Validators.minLength(6)]]
    }, { validators: this.passwordMatchValidator });
  }

  // Validador personalizado para confirmar contraseñas
  private passwordMatchValidator(g: FormGroup) {
    return g.get('newPassword')?.value === g.get('confirmPassword')?.value
      ? null : { 'mismatch': true };
  }

  ngOnInit(): void {
    // Verificar si hay un queryParam 'tab' y cambiar a esa pestaña
    this.route.queryParams.subscribe(params => {
      const tabParam = params['tab'];
      if (tabParam && ['events', 'cart', 'tickets', 'profile'].includes(tabParam)) {
        this.activeTab = tabParam as TabType;
        
        // Si el tab es 'tickets', cargar los tickets automáticamente
        if (tabParam === 'tickets') {
          this.loadMyTickets();
        }
        
        // Si el tab es 'profile', cargar el perfil automáticamente
        if (tabParam === 'profile' && !this.originalProfileData) {
          this.loadProfile();
        }
      }
    });
    
    this.loadEventCategories();
    this.loadEvents();
    
    // Suscribirse a cambios del carrito
    this.cartSubscription = this.cartService.cart$.subscribe(cart => {
      this.cart = cart;
      if (cart) {
        this.groupItemsByAdmin(cart);
      } else {
        this.groupedItems = [];
      }
    });
    
    // SIEMPRE cargar el carrito del backend al iniciar el dashboard
    // Esto asegura que tengamos el estado más actualizado, especialmente después de recargar la página
    this.loadCart();
  }

  // ==================== TAB NAVIGATION ====================
  switchTab(tab: TabType): void {
    this.activeTab = tab;
    
    if (tab === 'tickets' && this.myTickets.length === 0) {
      this.loadMyTickets();
    }
    
    if (tab === 'profile' && !this.originalProfileData) {
      this.loadProfile();
    }
  }

  // ==================== EVENTS SECTION ====================
  loadEventCategories(): void {
    this.eventService.getActiveEventCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
      },
      error: (error) => {
        console.error('Error cargando categorías:', error);
      }
    });
  }

  loadEvents(): void {
    this.isLoadingEvents = true;
    this.eventService.getEvents().subscribe({
      next: (events: any) => {
        this.allEvents = events.filter((e: any) => e.active).map((event: any) => {
          // Prioridad 1: imagen subida al servidor (archivo local)
          if (event.hasImageData && event.id) {
            return {
              ...event,
              imageUrl: this.eventService.getEventImageUrl(event.id)
            };
          }
          // Prioridad 2: URL externa
          if (event.imageUrl) {
            return event;
          }
          // Prioridad 3: placeholder
          return {
            ...event,
            imageUrl: 'https://via.placeholder.com/400x250?text=Sin+Imagen'
          };
        });
        
        // Filtrar eventos que necesitan geocoding (sin locationName)
        const eventsNeedingGeocode = this.allEvents.filter(event => !event.locationName);
        
        // Si no hay eventos que necesiten geocoding, mostrar inmediatamente
        if (eventsNeedingGeocode.length === 0) {
          this.events = [...this.allEvents];
          this.filterEvents(); // IMPORTANTE: llamar a filterEvents para poblar filteredEvents y paginatedEvents
          this.isLoadingEvents = false;
          this.cdr.detectChanges();
          return;
        }
        
        // Hacer peticiones secuenciales con delay mínimo
        from(eventsNeedingGeocode).pipe(
          concatMap((event, index) => {
            const key = `${event.lat},${event.lng}`;
            
            if (this.addressCache.has(key)) {
              return of({ event, data: null });
            }
            
            // BigDataCloud API - gratuita, sin API key, más confiable
            const url = `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${event.lat}&longitude=${event.lng}&localityLanguage=es`;
            
            // Delay mínimo entre peticiones
            const request$ = index === 0 
              ? this.http.get<any>(url) 
              : of(null).pipe(delay(300), concatMap(() => this.http.get<any>(url)));
            
            return request$.pipe(
              timeout(5000), // Timeout de 5 segundos
              catchError((error) => {
                console.error('❌ Error en geocodificación:', error.name || error.message);
                this.addressCache.set(key, 'Ubicación no disponible');
                return of(null);
              })
            ).pipe(
              catchError(() => of(null)),
              concatMap(data => of({ event, data }))
            );
          })
        ).subscribe({
          next: (result: any) => {
            const { event, data } = result;
            
            if (data) {
              const key = `${event.lat},${event.lng}`;
              
              // BigDataCloud devuelve: locality, city, principalSubdivision, countryName
              let placeName = data.locality || 
                             data.city || 
                             data.principalSubdivision || 
                             data.countryName || 
                             'Ubicación';
              
              this.addressCache.set(key, placeName);
              this.cdr.detectChanges();
            }
          },
          complete: () => {
            // Ahora sí mostrar los eventos con las ubicaciones ya resueltas
            this.events = [...this.allEvents];
            this.filterEvents();
            this.isLoadingEvents = false;
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('❌ Error en proceso de geocodificación:', err);
            // Mostrar eventos aunque haya error
            this.events = [...this.allEvents];
            this.filterEvents();
            this.isLoadingEvents = false;
            this.cdr.detectChanges();
          }
        });
      },
      error: (error: any) => {
        console.error('Error al cargar eventos:', error);
        this.isLoadingEvents = false;
      }
    });
  }

  filterEvents(): void {
    let filtered = [...this.allEvents];

    // Aplicar filtro de búsqueda
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(event => 
        event.name.toLowerCase().includes(term) ||
        event.description.toLowerCase().includes(term)
      );
    }

    // Aplicar filtro de categoría
    if (this.selectedCategoryFilter !== null) {
      filtered = filtered.filter(event => event.category?.id === this.selectedCategoryFilter);
    }

    this.filteredEvents = filtered;
    this.currentPage = 1;
    this.updatePagination();
  }

  filterByCategory(): void {
    this.filterEvents();
  }

  updatePagination(): void {
    this.totalPages = Math.ceil(this.filteredEvents.length / this.itemsPerPage);
    this.updatePaginatedItems();
  }

  updatePaginatedItems(): void {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.paginatedEvents = this.filteredEvents.slice(startIndex, endIndex);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updatePaginatedItems();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  get pageNumbers(): (number | string)[] {
    const pages: (number | string)[] = [];
    const maxVisible = 5;

    if (this.totalPages <= maxVisible + 2) {
      for (let i = 1; i <= this.totalPages; i++) {
        pages.push(i);
      }
    } else {
      pages.push(1);

      if (this.currentPage > 3) {
        pages.push('...');
      }

      const start = Math.max(2, this.currentPage - 1);
      const end = Math.min(this.totalPages - 1, this.currentPage + 1);

      for (let i = start; i <= end; i++) {
        pages.push(i);
      }

      if (this.currentPage < this.totalPages - 2) {
        pages.push('...');
      }

      pages.push(this.totalPages);
    }

    return pages;
  }

  searchEvents(): void {
    this.filterEvents();
  }

  viewEventDetails(eventId: number): void {
    this.router.navigate(['/customer/events', eventId]);
  }

  // ==================== CART SECTION ====================
  loadCart(): void {
    this.cartService.loadCart();
  }

  private groupItemsByAdmin(cart: Cart): void {
    const groups = new Map<number, { adminId: number, items: any[], total: number }>();

    cart.items.forEach(item => {
      // Si por alguna razón adminId es null/undefined, usar 0 o un placeholder
      const adminId = item.adminId || 0;
      
      if (!groups.has(adminId)) {
        groups.set(adminId, { adminId: adminId, items: [], total: 0 });
      }
      const group = groups.get(adminId)!;
      group.items.push(item);
      group.total += item.subtotal;
    });

    this.groupedItems = Array.from(groups.values());
  }

  payAdminGroup(adminId: number): void {
    Swal.fire({
      title: '¿Proceder al pago?',
      text: '¿Deseas pagar los items de este administrador?',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, pagar',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        // Mostrar mensaje de procesamiento
        Swal.fire({
          title: 'Procesando tu pago...',
          html: 'En breve serás dirigido a la pasarela de Stripe',
          icon: 'info',
          allowOutsideClick: false,
          allowEscapeKey: false,
          showConfirmButton: false,
          didOpen: () => {
            Swal.showLoading();
          }
        });

        this.orderService.checkoutSingleAdmin(adminId).subscribe({
          next: (response) => {
            if (response.paymentUrl) {
              // Redirigir a Stripe
              window.location.href = response.paymentUrl;
            } else {
              Swal.fire('Error', 'No se recibió URL de pago', 'error');
            }
          },
          error: (error) => {
            console.error('Error initiating checkout:', error);
            Swal.fire('Error', 'Error al iniciar el pago: ' + (error.error?.message || error.message), 'error');
          }
        });
      }
    });
  }

  get cartCount(): number {
    return this.cart ? this.cart.itemCount : 0;
  }

  get cartTotal(): number {
    return this.cart ? this.cart.totalAmount : 0;
  }

  removeFromCart(itemId: number): void {
    Swal.fire({
      title: '¿Eliminar item?',
      text: '¿Estás seguro de que quieres eliminar este item del carrito?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#d33'
    }).then((result) => {
      if (result.isConfirmed) {
        this.cartService.removeCartItem(itemId).subscribe({
          next: (cart) => {
            Swal.fire({
              title: 'Eliminado',
              text: 'El item ha sido eliminado del carrito',
              icon: 'success',
              timer: 1500,
              showConfirmButton: false
            });
          },
          error: (error) => {
            console.error('Error removing item:', error);
            Swal.fire('Error', 'Error al eliminar el item del carrito: ' + error.message, 'error');
          }
        });
      }
    });
  }

  /**
   * Duplica un item del carrito (crea una copia idéntica)
   * Con Opción A: cada item = 1 entrada = 1 QR
   */
  duplicateItem(item: any): void {
    Swal.fire({
      title: '¿Duplicar entrada?',
      text: 'Se creará una copia idéntica con las mismas consumiciones.',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, duplicar',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        // Construir las consumiciones para el nuevo item
        const consumptions = item.consumptions?.map((c: any) => ({
          consumptionId: c.consumptionId,
          quantity: c.quantity
        })) || [];

        // Agregar al carrito (esto creará un nuevo item con quantity=1)
        this.cartService.addToCart({
          eventId: item.eventId,
          quantity: 1,
          consumptions: consumptions
        }).subscribe({
          next: () => {
            Swal.fire({
              title: 'Duplicado',
              text: 'Entrada duplicada exitosamente',
              icon: 'success',
              timer: 1500,
              showConfirmButton: false
            });
          },
          error: (error) => {
            console.error('Error al duplicar item:', error);
            Swal.fire('Error', error.error?.message || 'Error al duplicar la entrada', 'error');
          }
        });
      }
    });
  }

  /**
   * Incrementa la cantidad de una consumición específica
   */
  incrementConsumption(itemId: number, consumption: any): void {
    const newQuantity = consumption.quantity + 1;
    
    this.cartService.updateConsumptionQuantity(itemId, consumption.consumptionId, newQuantity).subscribe({
      next: () => {
      },
      error: (error) => {
        console.error('Error incrementing consumption:', error);
        Swal.fire('Error', error.error?.message || 'Error al actualizar la cantidad de consumición', 'error');
      }
    });
  }

  /**
   * Decrementa la cantidad de una consumición específica
   * Si llega a 0, elimina la consumición automáticamente
   */
  decrementConsumption(itemId: number, consumption: any): void {
    const newQuantity = consumption.quantity - 1;
    
    // Si la nueva cantidad es 0 o menos, eliminar la consumición
    if (newQuantity <= 0) {
      this.removeConsumption(itemId, consumption);
      return;
    }
    
    this.cartService.updateConsumptionQuantity(itemId, consumption.consumptionId, newQuantity).subscribe({
      next: () => {
      },
      error: (error) => {
        console.error('Error decrementing consumption:', error);
        Swal.fire('Error', error.error?.message || 'Error al actualizar la cantidad de consumición', 'error');
      }
    });
  }

  /**
   * Elimina una consumición específica del item del carrito
   */
  removeConsumption(itemId: number, consumption: any): void {
    Swal.fire({
      title: '¿Eliminar consumición?',
      text: `¿Eliminar "${consumption.consumptionName}" del carrito?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#d33'
    }).then((result) => {
      if (result.isConfirmed) {
        this.cartService.removeConsumptionFromItem(itemId, consumption.consumptionId).subscribe({
          next: () => {
            Swal.fire({
              title: 'Eliminado',
              text: 'Consumición eliminada',
              icon: 'success',
              timer: 1500,
              showConfirmButton: false
            });
          },
          error: (error) => {
            console.error('Error removing consumption:', error);
            Swal.fire('Error', error.error?.message || 'Error al eliminar la consumición', 'error');
          }
        });
      }
    });
  }

  /**
   * Muestra el modal para agregar consumiciones a un item específico
   */
  showAddConsumptionModal(item: any): void {
    this.selectedItemId = item.id;
    this.isLoadingConsumptions = true;
    this.showModal = true;
    
    // Obtener consumiciones disponibles del evento
    this.eventService.getEventById(item.eventId).subscribe({
      next: (event: any) => {
        // El backend devuelve availableConsumptions (no consumptions)
        this.availableConsumptions = event.availableConsumptions || [];
        this.isLoadingConsumptions = false;
      },
      error: (err: any) => {
        console.error('Error loading consumptions:', err);
        Swal.fire('Error', 'No se pudieron cargar las consumiciones del evento', 'error');
        this.closeModal();
      }
    });
  }

  /**
   * Agrega una consumición al item seleccionado
   */
  addConsumption(consumptionId: number): void {
    if (!this.selectedItemId) return;
    
    this.cartService.addConsumptionToItem(this.selectedItemId, consumptionId, 1).subscribe({
      next: () => {
        this.closeModal();
        Swal.fire({
          title: 'Agregado',
          text: 'Consumición agregada exitosamente',
          icon: 'success',
          timer: 1500,
          showConfirmButton: false
        });
      },
      error: (err) => {
        console.error('Error adding consumption:', err);
        Swal.fire('Error', err.error?.message || 'Error al agregar consumición', 'error');
      }
    });
  }

  /**
   * Cierra el modal de agregar consumiciones
   */
  closeModal(): void {
    this.showModal = false;
    this.selectedItemId = null;
    this.availableConsumptions = [];
    this.isLoadingConsumptions = false;
  }

  clearCart(): void {
    Swal.fire({
      title: '¿Vaciar carrito?',
      text: '¿Estás seguro de que quieres vaciar todo el carrito?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, vaciar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#d33'
    }).then((result) => {
      if (result.isConfirmed) {
        this.cartService.clearCart().subscribe({
          next: () => {
            Swal.fire({
              title: 'Vaciado',
              text: 'El carrito ha sido vaciado',
              icon: 'success',
              timer: 1500,
              showConfirmButton: false
            });
          },
          error: (error) => {
            console.error('Error clearing cart:', error);
            Swal.fire('Error', 'Error al vaciar el carrito: ' + error.message, 'error');
          }
        });
      }
    });
  }

  getConsumptionsTotal(consumptions: any[]): number {
    if (!consumptions || consumptions.length === 0) return 0;
    return consumptions.reduce((total, consumption) => total + consumption.subtotal, 0);
  }

  proceedToCheckout(): void {
    if (!this.cart || this.cart.itemCount === 0) {
      Swal.fire('Carrito vacío', 'Tu carrito está vacío', 'info');
      return;
    }
    
    // Navegar al checkout multi-admin
    this.router.navigate(['/customer/checkout']);
  }

  // ==================== TICKETS SECTION ====================
  loadMyTickets(): void {
    this.isLoadingTickets = true;
    const userId = this.currentUser?.id;
    
    if (!userId) {
      console.error('No se pudo obtener el ID del usuario');
      this.isLoadingTickets = false;
      return;
    }
    
    this.ticketService.getUserTickets(userId).subscribe({
      next: (tickets) => {
        this.myTickets = tickets;
        this.isLoadingTickets = false;
      },
      error: (error) => {
        console.error('❌ Error cargando tickets:', error);
        console.error('❌ Status:', error.status);
        console.error('❌ Mensaje:', error.error);
        this.myTickets = [];
        this.isLoadingTickets = false;
        
        // Mensaje más específico según el error
        let errorMsg = 'Error al cargar tus entradas';
        if (error.status === 401 || error.status === 403) {
          errorMsg = 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.';
        } else if (error.status === 400) {
          errorMsg = 'Error de autenticación. Por favor, cierra sesión e inicia sesión nuevamente.';
        } else if (error.error?.message) {
          errorMsg += ': ' + error.error.message;
        }
        
        Swal.fire('Error', errorMsg, 'error');
      }
    });
  }

  showTicketQR(ticket: Ticket): void {
    this.selectedTicket = ticket;
    this.showQrModal = true;
  }

  openTicketLocation(ticket: Ticket): void {
    if (ticket.eventLat && ticket.eventLng) {
      const url = `https://www.google.com/maps/search/?api=1&query=${ticket.eventLat},${ticket.eventLng}`;
      window.open(url, '_blank');
    } else if (ticket.eventLocationName) {
      const url = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(ticket.eventLocationName)}`;
      window.open(url, '_blank');
    } else {
      Swal.fire('Información', 'No hay ubicación disponible para este evento', 'info');
    }
  }

  closeQrModal(): void {
    this.showQrModal = false;
    this.selectedTicket = null;
  }

  // ==================== PROFILE SECTION ====================
  loadProfile(): void {
    this.isLoadingProfile = true;
    const userId = this.currentUser?.id;
    
    if (!userId) {
      console.error('No se encontró ID de usuario');
      this.isLoadingProfile = false;
      return;
    }

    this.userService.getUserProfile(userId).subscribe({
      next: (profile: any) => {
        this.originalProfileData = profile;
        
        // Guardar datos de autenticación (readonly)
        this.userAuthData = {
          username: this.currentUser?.username || '',
          email: this.currentUser?.email || '',
          document: profile.document
        };
        
        // Llenar formulario con datos del perfil
        this.profileForm.patchValue({
          name: profile.name,
          lastName: profile.lastName,
          telephone: profile.telephone,
          gender: profile.gender,
          bornDate: profile.bornDate,
          profileImageUrl: profile.profileImageUrl || ''
        });
        this.isLoadingProfile = false;
      },
      error: (error: any) => {
        console.error('Error al cargar perfil:', error);
        
        if (error.status === 500 || error.status === 404) {
          // Perfil no existe - mostrar mensaje informativo
          Swal.fire('Perfil no disponible', 'Tu perfil no está disponible. Por favor, contacta con el administrador o intenta registrarte nuevamente.', 'warning');
        } else {
          Swal.fire('Error', 'Error al cargar el perfil. Por favor, intenta nuevamente.', 'error');
        }
        
        this.isLoadingProfile = false;
        // Volver a la pestaña de eventos
        this.activeTab = 'events';
      }
    });
  }

  toggleEditProfile(): void {
    this.isEditingProfile = !this.isEditingProfile;
    
    if (this.isEditingProfile) {
      this.profileForm.enable();
    } else {
      this.profileForm.disable();
      // Restaurar valores originales si cancela
      if (this.originalProfileData) {
        this.profileForm.patchValue(this.originalProfileData);
      }
    }
  }

  onSubmitProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const userId = this.currentUser?.id;
    if (!userId) return;

    this.isLoadingProfile = true;
    
    // ⚠️ IMPORTANTE: Incluir TODOS los campos requeridos por el backend
    // El backend requiere: name, lastName, document, gender, bornDate, telephone, profileImageUrl
    const profileData = {
      name: this.profileForm.get('name')?.value,
      lastName: this.profileForm.get('lastName')?.value,
      document: this.userAuthData?.document || 0, // Del auth-service (readonly)
      gender: this.profileForm.get('gender')?.value,
      bornDate: this.profileForm.get('bornDate')?.value,
      telephone: this.profileForm.get('telephone')?.value,
      profileImageUrl: this.profileForm.get('profileImageUrl')?.value || ''
    };

    this.userService.updateUserProfile(userId, profileData).subscribe({
      next: (response: any) => {
        this.originalProfileData = response;
        this.isEditingProfile = false;
        this.profileForm.disable();
        this.isLoadingProfile = false;
        Swal.fire('Éxito', 'Perfil actualizado exitosamente', 'success');
      },
      error: (error: any) => {
        console.error('Error al actualizar perfil:', error);
        this.isLoadingProfile = false;
        Swal.fire('Error', 'Error al actualizar el perfil. Intenta nuevamente.', 'error');
      }
    });
  }

  onChangePassword(): void {
    if (this.changePasswordForm.invalid) {
      this.changePasswordForm.markAllAsTouched();
      return;
    }

    const user = this.authService.getCurrentUser();
    if (!user) {
      Swal.fire('Error', 'Usuario no encontrado', 'error');
      return;
    }

    const currentPassword = this.changePasswordForm.get('currentPassword')?.value;
    const newPassword = this.changePasswordForm.get('newPassword')?.value;

    this.authService.changePassword(user.id, currentPassword, newPassword).subscribe({
      next: () => {
        Swal.fire('Éxito', 'Contraseña actualizada exitosamente', 'success');
        this.changePasswordForm.reset();
      },
      error: (error) => {
        const errorMessage = error.error?.message || 'Error al cambiar la contraseña. Verifica que la contraseña actual sea correcta.';
        Swal.fire('Error', errorMessage, 'error');
        console.error('Error al cambiar contraseña:', error);
      }
    });
  }

  // ==================== GEOCODING ====================
  getEventAddress(event: Event): string {
    // Priorizar locationName si existe, sino usar geocoding
    if (event.locationName) {
      return event.locationName;
    }
    
    const key = `${event.lat},${event.lng}`;
    
    // Si ya tenemos la dirección en caché, devolverla
    if (this.addressCache.has(key)) {
      return this.addressCache.get(key)!;
    }
    
    // Mientras se geocodifica, mostrar mensaje de carga
    return 'Ubicación no disponible';
  }

  private geocodeLocation(lat: number, lng: number, key: string): void {
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&addressdetails=1&zoom=18`;
    
    this.http.get<any>(url).subscribe({
      next: (data) => {
        if (data) {
          const addr = data.address || {};
          let placeName: string | undefined;
          
          // Prioridad 1: Nombre directo del POI/establecimiento
          if (data.name) {
            const roadTypes = ['Avenida', 'Calle', 'Ruta', 'Autovía', 'Boulevard', 'Paseo', 'Camino'];
            const isRoadName = roadTypes.some(type => data.name.startsWith(type));
            
            // Solo usar data.name si NO es una calle
            if (!isRoadName && data.name !== addr.road && data.name !== addr.highway) {
              placeName = data.name;
            }
          }
          
          // Prioridad 2: Tipos de establecimientos desde address
          if (!placeName) {
            placeName = addr.amenity || addr.shop || addr.leisure || 
                       addr.tourism || addr.office || addr.club_venue;
          }
          
          // Prioridad 3: Nombre de edificio (si no es genérico)
          if (!placeName && addr.building && typeof addr.building === 'string' && 
              addr.building !== 'yes' && addr.building !== 'house') {
            placeName = addr.building;
          }
          
          // Prioridad 4: Primera parte del display_name (filtrado)
          if (!placeName && data.display_name) {
            const firstPart = data.display_name.split(',')[0].trim();
            const roadTypes = ['Avenida', 'Calle', 'Ruta', 'Autovía', 'Boulevard', 'Paseo', 'Camino'];
            const isRoadName = roadTypes.some(type => firstPart.startsWith(type));
            
            if (!isRoadName && firstPart !== addr.road) {
              placeName = firstPart;
            }
          }
          
          // Fallback: barrio + ciudad
          if (!placeName) {
            const parts = [
              addr.neighbourhood || addr.suburb || addr.quarter,
              addr.city || addr.town || addr.village
            ].filter(p => p);
            placeName = parts.join(', ') || 'Ubicación desconocida';
          }
          
          this.addressCache.set(key, placeName);
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        console.error('Error geocoding:', err);
        this.addressCache.set(key, 'Ubicación no disponible');
        this.cdr.detectChanges();
      }
    });
  }

  // ==================== GENERAL ====================
  logout(): void {
    this.authService.logout();
  }

  ngOnDestroy(): void {
    // Limpiar suscripciones para evitar memory leaks
    if (this.cartSubscription) {
      this.cartSubscription.unsubscribe();
    }
  }
}
