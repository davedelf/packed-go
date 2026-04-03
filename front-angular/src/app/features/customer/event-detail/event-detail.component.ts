import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import Swal from 'sweetalert2';
import { EventService } from '../../../core/services/event.service';
import { CartService } from '../../../core/services/cart.service';
import { Event, Consumption } from '../../../shared/models/event.model';
import { AddToCartRequest, ConsumptionRequest } from '../../../shared/models/cart.model';
import { SafePipe } from '../../../shared/pipes/safe.pipe';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, SafePipe],
  templateUrl: './event-detail.component.html',
  styleUrls: ['./event-detail.component.css']
})
export class EventDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private eventService = inject(EventService);
  private cartService = inject(CartService);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);

  event: Event | null = null;
  isLoading = true;
  errorMessage = '';
  quantity = 1;
  isAddingToCart = false;
  
  // Selected consumptions with quantities
  selectedConsumptions: Map<number, number> = new Map();
  
  // Consumptions carousel pagination
  currentConsumptionIndex = 0;
  itemsPerPage = 3;
  consumptionSearchTerm = '';
  
  // Geocoding & Map modal
  private addressCache = new Map<string, string>();
  showMapModal = false;

  ngOnInit(): void {
    // Cargar el carrito primero para tener el estado actual
    this.cartService.loadCart();
    
    const eventId = Number(this.route.snapshot.paramMap.get('id'));
    if (eventId) {
      this.loadEventDetails(eventId);
    } else {
      this.router.navigate(['/customer/dashboard']);
    }
  }

  loadEventDetails(eventId: number): void {
    this.isLoading = true;
    this.eventService.getEventById(eventId).subscribe({
      next: (event) => {
        // Construir imageUrl con prioridades: archivo local > URL externa > placeholder
        if (event.hasImageData && event.id) {
          event.imageUrl = this.eventService.getEventImageUrl(event.id);
        } else if (!event.imageUrl) {
          event.imageUrl = 'https://via.placeholder.com/800x400?text=Sin+Imagen';
        }
        
        this.event = event;
        
        // Geocodificar antes de mostrar el evento
        const key = `${event.lat},${event.lng}`;
        if (!this.addressCache.has(key)) {
          this.geocodeLocationSync(event.lat, event.lng, key).then(() => {
            this.isLoading = false;
            this.cdr.detectChanges();
          });
        } else {
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      },
      error: (error) => {
        console.error('Error al cargar evento:', error);
        this.errorMessage = 'No se pudo cargar el evento';
        this.isLoading = false;
      }
    });
  }

  /**
   * Obtiene la URL de la imagen del evento con prioridades
   */
  getEventImageSrc(): string {
    if (!this.event) return 'https://via.placeholder.com/800x400?text=Sin+Imagen';
    
    // Prioridad 1: imagen subida al servidor (archivo local)
    if (this.event.hasImageData && this.event.id) {
      return this.eventService.getEventImageUrl(this.event.id);
    }
    // Prioridad 2: URL externa
    if (this.event.imageUrl) {
      return this.event.imageUrl;
    }
    // Prioridad 3: placeholder
    return 'https://via.placeholder.com/800x400?text=Sin+Imagen';
  }

  /**
   * Obtiene el nombre de la categoría del evento
   */
  getCategoryName(): string {
    if (!this.event?.category) return 'Sin categoría';
    return this.event.category.name || `Categoría ${this.event.categoryId}`;
  }

  incrementQuantity(): void {
    if (!this.event) return;
    
    // Calcular el máximo permitido considerando lo que ya está en el carrito
    const maxTickets = this.cartService.MAX_TICKETS_PER_PERSON;
    const currentInCart = this.cartService.getTicketCountForEvent(this.event.id!);
    const maxCanAdd = maxTickets - currentInCart;
    
    // También respetar el límite del evento
    const eventLimit = this.event.availablePasses || this.event.maxCapacity;
    const effectiveMax = Math.min(maxCanAdd, eventLimit);
    
    if (this.quantity < effectiveMax) {
      this.quantity++;
    } else if (this.quantity >= maxCanAdd) {
      Swal.fire('Límite alcanzado', `Ya tienes ${currentInCart} entrada(s) en tu carrito. Solo puedes agregar ${maxCanAdd} más (máximo ${maxTickets} por persona).`, 'warning');
    }
  }

  decrementQuantity(): void {
    if (this.quantity > 1) {
      this.quantity--;
    }
  }
  
  /**
   * Obtiene el máximo de entradas que se pueden agregar
   */
  getMaxQuantity(): number {
    if (!this.event) return 1;
    
    const maxTickets = this.cartService.MAX_TICKETS_PER_PERSON;
    const currentInCart = this.cartService.getTicketCountForEvent(this.event.id!);
    const maxCanAdd = maxTickets - currentInCart;
    const eventLimit = this.event.availablePasses || this.event.maxCapacity;
    
    return Math.min(maxCanAdd, eventLimit);
  }

  addToCart(): void {
    if (!this.event) return;

    // Validar que no exceda el máximo permitido
    const maxTickets = this.cartService.MAX_TICKETS_PER_PERSON;
    if (!this.cartService.canAddTickets(this.event.id!, this.quantity)) {
      const currentCount = this.cartService.getTicketCountForEvent(this.event.id!);
      const remaining = maxTickets - currentCount;
      Swal.fire('Límite alcanzado', `Ya tienes ${currentCount} entrada(s) de este evento en tu carrito. Solo puedes agregar ${remaining} más (máximo ${maxTickets} por persona).`, 'warning');
      return;
    }

    this.isAddingToCart = true;
    
    // Build consumptions array from selected items
    const consumptions: ConsumptionRequest[] = Array.from(this.selectedConsumptions.entries())
      .map(([consumptionId, quantity]) => ({
        consumptionId,
        quantity
      }));
    
    // Create request with eventId, quantity, and selected consumptions
    const request: AddToCartRequest = {
      eventId: this.event.id!,
      quantity: this.quantity, // Enviar la cantidad seleccionada
      consumptions: consumptions
    };
    
    this.cartService.addToCart(request).subscribe({
      next: (cart) => {
        const consumptionText = consumptions.length > 0 
          ? ` con ${consumptions.length} consumo(s)` 
          : '';
        
        Swal.fire({
          title: '¡Agregado!',
          text: `¡${this.quantity} entrada(s) agregada(s) al carrito${consumptionText}!`,
          icon: 'success',
          timer: 1500,
          showConfirmButton: false
        }).then(() => {
          this.isAddingToCart = false;
          // Resetear la cantidad y consumptions seleccionadas
          this.quantity = 1;
          this.selectedConsumptions.clear();
          this.router.navigate(['/customer/dashboard']);
        });
      },
      error: (error) => {
        console.error('Error al agregar al carrito:', error);
        
        // Mostrar el mensaje específico del backend si está disponible
        const errorMessage = error?.error?.message || error?.message || 'Error desconocido';
        
        if (errorMessage.includes('Cannot add more than')) {
          // Error de límite máximo desde el backend
          const maxTickets = this.cartService.MAX_TICKETS_PER_PERSON;
          const currentCount = this.cartService.getTicketCountForEvent(this.event!.id!);
          Swal.fire('Límite alcanzado', `Has alcanzado el límite máximo de ${maxTickets} entradas por persona. Actualmente tienes ${currentCount} entradas de este evento en tu carrito.`, 'error');
        } else {
          Swal.fire('Error', `Error al agregar al carrito: ${errorMessage}`, 'error');
        }
        
        this.isAddingToCart = false;
      }
    });
  }

  // Consumption selection methods
  toggleConsumption(consumptionId: number): void {
    if (this.selectedConsumptions.has(consumptionId)) {
      this.selectedConsumptions.delete(consumptionId);
    } else {
      this.selectedConsumptions.set(consumptionId, 1);
    }
  }

  isConsumptionSelected(consumptionId: number): boolean {
    return this.selectedConsumptions.has(consumptionId);
  }

  getConsumptionQuantity(consumptionId: number): number {
    return this.selectedConsumptions.get(consumptionId) || 0;
  }

  updateConsumptionQuantity(consumptionId: number, quantity: number): void {
    if (quantity > 0 && quantity <= 10) {
      this.selectedConsumptions.set(consumptionId, quantity);
    } else if (quantity <= 0) {
      this.selectedConsumptions.delete(consumptionId);
    }
  }

  incrementConsumption(consumptionId: number): void {
    const current = this.getConsumptionQuantity(consumptionId);
    if (current < 10) {
      this.updateConsumptionQuantity(consumptionId, current + 1);
    }
  }

  decrementConsumption(consumptionId: number): void {
    const current = this.getConsumptionQuantity(consumptionId);
    if (current > 1) {
      this.updateConsumptionQuantity(consumptionId, current - 1);
    } else {
      this.selectedConsumptions.delete(consumptionId);
    }
  }

  getSelectedConsumptionsTotal(): number {
    if (!this.event?.availableConsumptions) return 0;
    
    let total = 0;
    this.selectedConsumptions.forEach((quantity, consumptionId) => {
      const consumption = this.event!.availableConsumptions!.find(c => c.id === consumptionId);
      if (consumption) {
        total += consumption.price * quantity;
      }
    });
    return total;
  }

  getTotalPrice(): number {
    const ticketsTotal = this.event ? this.event.basePrice * this.quantity : 0;
    // Las consumiciones se multiplican por la cantidad de entradas porque cada entrada lleva sus consumiciones
    const consumptionsTotal = this.getSelectedConsumptionsTotal() * this.quantity;
    return ticketsTotal + consumptionsTotal;
  }

  goBack(): void {
    this.router.navigate(['/customer/dashboard']);
  }

  // ==================== GEOCODING & MAP ====================
  getEventAddress(): string {
    if (!this.event) return 'Ubicación no disponible';
    
    // Priorizar locationName si existe
    if (this.event.locationName) {
      return this.event.locationName;
    }
    
    const key = `${this.event.lat},${this.event.lng}`;
    
    if (this.addressCache.has(key)) {
      return this.addressCache.get(key)!;
    }
    
    // Si llegamos aquí, el geocoding aún no completó (no debería suceder)
    return 'Ubicación no disponible';
  }

  private geocodeLocationSync(lat: number, lng: number, key: string): Promise<void> {
    return new Promise((resolve) => {
      // BigDataCloud API - gratuita, sin API key, más confiable
      const url = `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lng}&localityLanguage=es`;
      
      this.http.get<any>(url).subscribe({
        next: (data) => {
          if (data) {
            // BigDataCloud devuelve: locality, city, principalSubdivision, countryName
            let placeName = data.locality || 
                           data.city || 
                           data.principalSubdivision || 
                           data.countryName || 
                           'Ubicación';
            
            this.addressCache.set(key, placeName);
            resolve();
          } else {
            this.addressCache.set(key, 'Ubicación no disponible');
            resolve();
          }
        },
        error: (err) => {
          console.error('Error geocoding:', err);
          this.addressCache.set(key, 'Ubicación no disponible');
          resolve();
        }
      });
    });
  }

  openMapModal(): void {
    this.showMapModal = true;
  }

  closeMapModal(): void {
    this.showMapModal = false;
  }

  onMapModalKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.closeMapModal();
    }
  }

  // Consumptions carousel methods
  getVisibleConsumptions(): Consumption[] {
    const filtered = this.getFilteredConsumptions();
    const start = this.currentConsumptionIndex;
    const end = start + this.itemsPerPage;
    return filtered.slice(start, end);
  }

  getFilteredConsumptions(): Consumption[] {
    if (!this.event?.availableConsumptions) return [];
    
    if (!this.consumptionSearchTerm.trim()) {
      return this.event.availableConsumptions;
    }
    
    const searchLower = this.consumptionSearchTerm.toLowerCase();
    return this.event.availableConsumptions.filter(c => 
      c.name.toLowerCase().includes(searchLower) ||
      (c.description && c.description.toLowerCase().includes(searchLower))
    );
  }

  onConsumptionSearch(): void {
    this.currentConsumptionIndex = 0;
  }

  prevConsumption(): void {
    if (this.currentConsumptionIndex > 0) {
      this.currentConsumptionIndex = Math.max(0, this.currentConsumptionIndex - this.itemsPerPage);
    }
  }

  nextConsumption(): void {
    const filtered = this.getFilteredConsumptions();
    const maxIndex = filtered.length - this.itemsPerPage;
    if (this.currentConsumptionIndex < maxIndex) {
      this.currentConsumptionIndex = Math.min(maxIndex, this.currentConsumptionIndex + this.itemsPerPage);
    }
  }

  getTotalPages(): number {
    if (!this.event?.availableConsumptions) return 0;
    return Math.ceil(this.event.availableConsumptions.length / this.itemsPerPage);
  }

  getCurrentPage(): number {
    return Math.floor(this.currentConsumptionIndex / this.itemsPerPage);
  }

  getPageIndicators(): number[] {
    const totalPages = this.getTotalPages();
    return Array.from({ length: totalPages }, (_, i) => i);
  }

  goToPage(page: number): void {
    this.currentConsumptionIndex = page * this.itemsPerPage;
  }

  /**
   * Obtiene la URL de la imagen de una consumición con prioridades
   */
  getConsumptionImageSrc(consumption: Consumption): string {
    // Prioridad 1: imagen subida como Base64
    if (consumption.hasImageData && consumption.imageData) {
      return `data:${consumption.imageContentType || 'image/jpeg'};base64,${consumption.imageData}`;
    }
    
    // Prioridad 2: URL externa
    if (consumption.imageUrl) {
      return consumption.imageUrl;
    }
    
    // Fallback: sin imagen
    return '';
  }

  /**
   * Verifica si una consumición tiene imagen para mostrar
   */
  hasConsumptionImage(consumption: Consumption): boolean {
    return !!(consumption.hasImageData || consumption.imageUrl);
  }
}
