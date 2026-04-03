import { Component, inject, OnInit, HostListener, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { from } from 'rxjs';
import { concatMap, delay, timeout, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import Swal from 'sweetalert2';
import { EventService } from '../../../core/services/event.service';
import { AuthService } from '../../../core/services/auth.service';
import { Event, EventCategory, Consumption } from '../../../shared/models/event.model';
import { LocationPickerComponent, Location } from '../../../shared/components/location-picker/location-picker.component';
import { SafePipe } from '../../../shared/pipes/safe.pipe';

@Component({
  selector: 'app-events-management',
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule, LocationPickerComponent, SafePipe],
  templateUrl: './events-management.component.html',
  styleUrl: './events-management.component.css'
})
export class EventsManagementComponent implements OnInit {
  private fb = inject(FormBuilder);
  private eventService = inject(EventService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);

  // Tab Management
  activeTab: 'events' | 'categories' = 'events';
  
  // Events Tab
  events: Event[] = [];
  filteredEvents: Event[] = [];
  consumptionCategories: any[] = [];
  selectedConsumptions: number[] = [];
  eventForm: FormGroup;
  searchTerm = '';
  selectedCategoryFilter: number | null = null;
  showModal = false;
  isEditMode = false;
  currentEventId?: number;
  currentEvent?: Event;
  
  // Pagination
  currentPage = 1;
  itemsPerPage = 12; // 4 columnas x 3 filas
  totalPages = 1;
  paginatedEvents: Event[] = [];
  
  // Image Upload
  imageUploadOption: 'url' | 'upload' = 'url';
  selectedImageFile: File | null = null;
  imagePreviewUrl: string | null = null;
  
  // Categories Tab
  categories: EventCategory[] = [];
  filteredCategories: EventCategory[] = [];
  categorySearchTerm = '';
  showCategoryModal = false;
  isCategoryEditMode = false;
  categoryForm: any = {
    id: undefined,
    name: '',
    description: '',
    active: true
  };
  
  // Shared Data
  consumptions: Consumption[] = [];
  
  // Geocoding - Direcciones de eventos
  eventAddresses: Map<number, string> = new Map();
  
  // Mapa flotante
  showMapModal = false;
  mapEventLocation: { lat: number; lng: number; name: string; address: string } | null = null;
  
  // Loading & Messages
  isLoading = true;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';

  constructor() {
    this.eventForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      startTime: ['', [Validators.required]], // Ahora es datetime-local (fecha y hora de inicio)
      endTime: ['', [Validators.required]],   // Ahora es datetime-local (fecha y hora de finalización)
      lat: ['', [Validators.required, Validators.pattern(/^-?\d+\.?\d*$/)]],
      lng: ['', [Validators.required, Validators.pattern(/^-?\d+\.?\d*$/)]],
      locationName: [''], // Nombre del lugar (opcional)
      maxCapacity: ['', [Validators.required, Validators.min(1)]],
      basePrice: ['', [Validators.required, Validators.min(0)]],
      imageUrl: [''],
      categoryId: ['', [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.loadData();
  }

  // Cerrar modal del mapa con tecla Escape
  @HostListener('document:keydown.escape', ['$event'])
  handleEscapeKey(event: KeyboardEvent): void {
    if (this.showMapModal) {
      this.closeMapModal();
    }
  }

  loadData(): void {
    this.isLoading = true;
    
    // Cargar categorías primero
    this.eventService.getEventCategories().subscribe({
      next: (categories: any) => {
        this.categories = categories;
      },
      error: (error: any) => console.error('Error al cargar categorías:', error)
    });

    // Cargar categorías de consumición
    this.eventService.getConsumptionCategories().subscribe({
      next: (categories: any) => {
        this.consumptionCategories = categories;
      },
      error: (error: any) => console.error('Error al cargar categorías de consumición:', error)
    });

    // Cargar consumptions
    this.eventService.getConsumptions().subscribe({
      next: (consumptions: any) => {
        this.consumptions = consumptions.filter((c: Consumption) => c.active);
      },
      error: (error: any) => console.error('Error al cargar consumptions:', error)
    });

    // Cargar eventos
    this.eventService.getEvents().subscribe({
      next: (events: any) => {
        // Filtrar solo eventos activos
        const activeEvents = events.filter((event: Event) => event.active !== false);
        
        // Filtrar eventos que necesitan geocoding (sin locationName)
        const eventsNeedingGeocode = activeEvents.filter(
          (event: Event) => !event.locationName && event.id && event.lat && event.lng
        );
        
        if (eventsNeedingGeocode.length === 0) {
          // No hay eventos que necesiten geocoding, mostrar inmediatamente
          this.events = activeEvents;
          this.filteredEvents = activeEvents;
          this.updatePagination();
          this.isLoading = false;
          this.cdr.detectChanges();
          return;
        }
        
        // Hacer geocoding secuencial con delay antes de mostrar
        from(eventsNeedingGeocode).pipe(
          concatMap((event: Event, index: number) => {
            const url = `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${event.lat}&longitude=${event.lng}&localityLanguage=es`;
            
            // Para el primer elemento, no hay delay; para los siguientes, delay de 300ms
            const request$ = index === 0 
              ? this.http.get<any>(url)
              : of(null).pipe(
                  delay(300),
                  concatMap(() => this.http.get<any>(url))
                );
            
            return request$.pipe(
              timeout(5000),
              catchError((err) => {
                console.error('Error geocoding:', err);
                if (event.id) {
                  this.eventAddresses.set(event.id, 'Ubicación no disponible');
                }
                return of(null);
              })
            );
          })
        ).subscribe({
          next: (data) => {
            // Procesar cada respuesta de geocoding
            const event = eventsNeedingGeocode.find(e => 
              data && e.lat === data.latitude && e.lng === data.longitude
            );
            if (data && event && event.id) {
              const placeName = data.locality || 
                               data.city || 
                               data.principalSubdivision || 
                               data.countryName || 
                               'Ubicación';
              this.eventAddresses.set(event.id, placeName);
            }
          },
          complete: () => {
            // Una vez completado el geocoding, mostrar los eventos
            this.events = activeEvents;
            this.filteredEvents = activeEvents;
            this.updatePagination();
            this.isLoading = false;
            this.cdr.detectChanges();
          }
        });
      },
      error: (error: any) => {
        console.error('Error al cargar eventos:', error);
        this.isLoading = false;
        this.errorMessage = 'Error al cargar eventos. Por favor, intenta nuevamente.';
      }
    });
  }

  searchEvents(): void {
    let filtered = this.events;

    // Filtrar por categoría
    if (this.selectedCategoryFilter !== null) {
      filtered = filtered.filter(e => e.categoryId === this.selectedCategoryFilter);
    }

    // Filtrar por término de búsqueda
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(event => 
        event.name.toLowerCase().includes(term) ||
        event.description.toLowerCase().includes(term)
      );
    }

    this.filteredEvents = filtered;
    this.currentPage = 1; // Reset a la primera página
    this.updatePagination();
  }

  filterByCategory(categoryId: number | null): void {
    this.selectedCategoryFilter = categoryId;
    this.searchEvents();
  }

  // Paginación
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
      // Scroll suave hacia arriba
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  get pageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    
    if (this.totalPages <= maxVisible) {
      for (let i = 1; i <= this.totalPages; i++) {
        pages.push(i);
      }
    } else {
      if (this.currentPage <= 3) {
        for (let i = 1; i <= 4; i++) pages.push(i);
        pages.push(-1); // Ellipsis
        pages.push(this.totalPages);
      } else if (this.currentPage >= this.totalPages - 2) {
        pages.push(1);
        pages.push(-1);
        for (let i = this.totalPages - 3; i <= this.totalPages; i++) pages.push(i);
      } else {
        pages.push(1);
        pages.push(-1);
        pages.push(this.currentPage - 1);
        pages.push(this.currentPage);
        pages.push(this.currentPage + 1);
        pages.push(-1);
        pages.push(this.totalPages);
      }
    }
    return pages;
  }

  openCreateModal(): void {
    this.isEditMode = false;
    this.currentEventId = undefined;
    this.currentEvent = undefined;
    this.selectedConsumptions = [];
    this.eventForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
    
    // Usar setTimeout para evitar ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => {
      this.showModal = true;
      this.cdr.detectChanges();
    }, 0);
  }

  openEditModal(event: Event): void {
    this.isEditMode = true;
    this.currentEventId = event.id;
    this.currentEvent = event;
    
    // Cargar consumiciones existentes del evento
    this.selectedConsumptions = event.availableConsumptions 
      ? event.availableConsumptions.map(c => c.id).filter((id): id is number => id !== undefined) 
      : [];
    
    // Convertir startTime y endTime a formato datetime-local (YYYY-MM-DDTHH:mm)
    let startTimeFormatted = '';
    let endTimeFormatted = '';
    
    if (event.startTime) {
      const startDate = new Date(event.startTime.endsWith('Z') ? event.startTime : event.startTime + 'Z');
      startTimeFormatted = this.toDatetimeLocal(startDate);
    }
    
    if (event.endTime) {
      const endDate = new Date(event.endTime.endsWith('Z') ? event.endTime : event.endTime + 'Z');
      endTimeFormatted = this.toDatetimeLocal(endDate);
    }
    
    this.eventForm.patchValue({
      name: event.name,
      description: event.description,
      startTime: startTimeFormatted,
      endTime: endTimeFormatted,
      lat: event.lat,
      lng: event.lng,
      locationName: event.locationName || '',
      maxCapacity: event.maxCapacity,
      basePrice: event.basePrice,
      imageUrl: event.imageUrl || '',
      categoryId: event.categoryId
    });
    
    // Configurar imagen existente para preview
    this.selectedImageFile = null; // No hay nuevo archivo seleccionado
    if (event.hasImageData && event.id) {
      // Mostrar preview de la imagen existente almacenada en el servidor
      this.imagePreviewUrl = this.eventService.getEventImageUrl(event.id);
    } else if (event.imageUrl) {
      // Mostrar preview de la URL externa
      this.imagePreviewUrl = event.imageUrl;
    } else {
      this.imagePreviewUrl = null;
    }
    
    this.errorMessage = '';
    this.successMessage = '';
    
    // Usar setTimeout para evitar ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => {
      this.showModal = true;
      this.cdr.detectChanges();
    }, 0);
  }

  closeModal(): void {
    this.showModal = false;
    this.selectedConsumptions = [];
    this.eventForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
    this.isSubmitting = false;
    this.isEditMode = false;
    this.currentEventId = undefined;
    this.currentEvent = undefined;
    
    // Limpiar imagen
    this.clearImageFile();
    this.imageUploadOption = 'url';
  }

  onLocationChange(location: Location): void {
    // Actualizar los valores del formulario cuando cambia la ubicación
    this.eventForm.patchValue({
      lat: location.lat.toString(),
      lng: location.lng.toString()
    });
    
    // Marcar los campos como tocados para mostrar validación
    this.eventForm.get('lat')?.markAsTouched();
    this.eventForm.get('lng')?.markAsTouched();
  }

  onSubmit(): void {
    if (this.eventForm.invalid) {
      this.eventForm.markAllAsTouched();
      return;
    }

    // Validar que la fecha/hora de finalización sea mayor que la de inicio
    const startTime = this.eventForm.value.startTime; // datetime-local (YYYY-MM-DDTHH:mm)
    const endTime = this.eventForm.value.endTime;     // datetime-local (YYYY-MM-DDTHH:mm)
    
    if (startTime && endTime && endTime <= startTime) {
      this.errorMessage = 'La fecha y hora de finalización debe ser mayor que la de inicio';
      // Marcar los campos como tocados para mostrar el error visual
      this.eventForm.get('startTime')?.markAsTouched();
      this.eventForm.get('endTime')?.markAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    // Convertir datetime-local (YYYY-MM-DDTHH:mm) a LocalDateTime (YYYY-MM-DDTHH:mm:ss)
    const startTimeDateTime = `${startTime}:00`;
    const endTimeDateTime = `${endTime}:00`;

    const eventData: Event & { consumptionIds?: number[] } = {
      ...this.eventForm.value,
      eventDate: startTimeDateTime, // Usar startTime como eventDate por compatibilidad
      startTime: startTimeDateTime, // 🕐 Fecha y hora de inicio en formato LocalDateTime
      endTime: endTimeDateTime,     // 🕐 Fecha y hora de finalización en formato LocalDateTime
      lat: Number(this.eventForm.value.lat),
      lng: Number(this.eventForm.value.lng),
      locationName: this.eventForm.value.locationName || null,
      maxCapacity: Number(this.eventForm.value.maxCapacity),
      basePrice: Number(this.eventForm.value.basePrice),
      categoryId: Number(this.eventForm.value.categoryId),
      consumptionIds: this.selectedConsumptions,
      active: this.isEditMode && this.currentEvent ? this.currentEvent.active : true
    };

    if (this.isEditMode && this.currentEventId) {
      // Actualizar evento existente
      this.eventService.updateEvent(this.currentEventId, eventData).subscribe({
        next: () => {
          // Si hay imagen seleccionada, subirla
          if (this.selectedImageFile) {
            this.uploadEventImage(this.currentEventId!);
          } else {
            this.isSubmitting = false;
            this.successMessage = 'Evento actualizado exitosamente';
            setTimeout(() => {
              this.closeModal();
              this.loadData();
            }, 1500);
          }
        },
        error: (error: any) => {
          console.error('Error al actualizar evento:', error);
          this.errorMessage = error.error?.message || 'Error al actualizar el evento. Por favor, intenta nuevamente.';
          this.isSubmitting = false;
        }
      });
    } else {
      // Crear nuevo evento
      this.eventService.createEvent(eventData).subscribe({
        next: (response: any) => {
          const newEventId = response.id;
          
          // Si hay imagen seleccionada, subirla
          if (this.selectedImageFile && newEventId) {
            this.uploadEventImage(newEventId);
          } else {
            this.isSubmitting = false;
            this.successMessage = 'Evento creado exitosamente';
            setTimeout(() => {
              this.closeModal();
              this.loadData();
            }, 1500);
          }
        },
        error: (error: any) => {
          console.error('Error al crear evento:', error);
          this.errorMessage = error.error?.message || 'Error al crear el evento. Por favor, intenta nuevamente.';
          this.isSubmitting = false;
        }
      });
    }
  }

  uploadEventImage(eventId: number): void {
    if (!this.selectedImageFile) {
      return;
    }

    this.eventService.uploadEventImage(eventId, this.selectedImageFile).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.successMessage = `Evento ${this.isEditMode ? 'actualizado' : 'creado'} exitosamente con imagen`;
        setTimeout(() => {
          this.closeModal();
          this.loadData();
        }, 1500);
      },
      error: (error: any) => {
        console.error('Error al subir imagen:', error);
        this.isSubmitting = false;
        this.successMessage = `Evento ${this.isEditMode ? 'actualizado' : 'creado'} exitosamente`;
        this.errorMessage = 'Advertencia: Error al subir la imagen. El evento fue guardado sin imagen.';
        setTimeout(() => {
          this.closeModal();
          this.loadData();
        }, 2500);
      }
    });
  }

  deleteEvent(eventId: number, eventName: string): void {
    Swal.fire({
      title: '¿Desactivar evento?',
      text: `¿Estás seguro de que deseas desactivar el evento "${eventName}"? El evento se ocultará pero no se eliminará permanentemente.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#d33'
    }).then((result) => {
      if (result.isConfirmed) {
        this.eventService.deleteEventLogical(eventId).subscribe({
          next: () => {
            // Eliminar el evento de la lista local sin recargar
            this.events = this.events.filter(e => e.id !== eventId);
            this.filteredEvents = this.filteredEvents.filter(e => e.id !== eventId);
            
            // Eliminar la dirección del mapa si existe
            this.eventAddresses.delete(eventId);
            
            Swal.fire('Desactivado', 'Evento desactivado exitosamente', 'success');
          },
          error: (error: any) => {
            console.error('Error al desactivar evento:', error);
            
            // Mostrar mensaje específico según el error
            let errorMessage = 'Error al desactivar el evento. Por favor, intenta nuevamente.';
            
            if (error.status === 403) {
              errorMessage = 'No tienes permisos para desactivar este evento. Solo el creador puede desactivarlo.';
            } else if (error.status === 404) {
              errorMessage = 'El evento no fue encontrado.';
            } else if (error.error?.message) {
              errorMessage = error.error.message;
            }
            
            Swal.fire('Error', errorMessage, 'error');
          }
        });
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/admin/dashboard']);
  }

  logout(): void {
    this.authService.logout();
  }

  getCategoryName(categoryId: number): string {
    const category = this.categories.find(c => c.id === categoryId);
    return category ? category.name : 'Sin categoría';
  }

  // Métodos para manejo de consumptions
  toggleConsumptionSelection(consumptionId: number): void {
    const index = this.selectedConsumptions.indexOf(consumptionId);
    if (index > -1) {
      this.selectedConsumptions.splice(index, 1);
    } else {
      this.selectedConsumptions.push(consumptionId);
    }
  }

  isConsumptionSelected(consumptionId: number): boolean {
    return this.selectedConsumptions.includes(consumptionId);
  }

  getSelectedConsumptionsList(): Consumption[] {
    return this.consumptions.filter(c => this.isConsumptionSelected(c.id!));
  }

  getAvailableConsumptionsList(): Consumption[] {
    return this.consumptions.filter(c => !this.isConsumptionSelected(c.id!));
  }

  getConsumptionCategoryName(categoryId: number): string {
    const consumption = this.consumptions.find(c => c.id === categoryId);
    return consumption?.categoryId ? `Categoría ${consumption.categoryId}` : 'Sin categoría';
  }

  // ===== IMAGE UPLOAD METHODS =====
  setImageOption(option: 'url' | 'upload'): void {
    this.imageUploadOption = option;
    // ✅ AHORA PERMITE AMBAS OPCIONES: No limpiamos nada al cambiar
    // El usuario puede tener archivo Y URL simultáneamente
    // Prioridad en visualización: archivo local > URL externa
  }

  onImageFileSelected(event: any): void {
    const input = event.target as HTMLInputElement;
    
    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];
    
    // Validar tipo de archivo
    const allowedTypes = ['image/png', 'image/jpeg', 'image/jpg'];
    if (!allowedTypes.includes(file.type)) {
      this.errorMessage = 'Formato de archivo no válido. Use PNG, JPG o JPEG';
      return;
    }

    // Validar tamaño (5MB)
    const maxSize = 5 * 1024 * 1024; // 5MB en bytes
    if (file.size > maxSize) {
      this.errorMessage = `El archivo excede el tamaño máximo de 5MB (${(file.size / 1024 / 1024).toFixed(2)} MB)`;
      return;
    }

    this.selectedImageFile = file;
    this.errorMessage = '';

    // Crear preview
    const reader = new FileReader();
    reader.onload = (e: ProgressEvent<FileReader>) => {
      this.imagePreviewUrl = e.target?.result as string;
    };
    reader.readAsDataURL(file);
  }

  clearImageFile(): void {
    this.selectedImageFile = null;
    this.imagePreviewUrl = null;
    
    // Limpiar el input file
    const fileInput = document.getElementById('imageFile') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  getEventImageSrc(event: Event): string {
    // Prioridad 1: imagen subida al servidor (archivo local)
    if (event.hasImageData && event.id) {
      return this.eventService.getEventImageUrl(event.id);
    }
    // Prioridad 2: URL externa ingresada
    if (event.imageUrl) {
      return event.imageUrl;
    }
    // Prioridad 3: placeholder
    return 'https://via.placeholder.com/400x250?text=Sin+Imagen';
  }

  handleImageError(event: Event): void {
    // Si falla la imagen subida, intentar con la URL externa
    if (event.hasImageData && event.imageUrl) {
      event.hasImageData = false;
    }
  }

  // ===== TAB MANAGEMENT =====
  switchTab(tab: 'events' | 'categories'): void {
    this.activeTab = tab;
    
    // Limpiar mensajes
    this.errorMessage = '';
    this.successMessage = '';
    
    // Si cambiamos a categorías y no están cargadas, cargarlas
    if (tab === 'categories') {
      this.loadCategories();
    }
  }

  // ===== CATEGORIES TAB METHODS =====
  loadCategories(): void {
    this.eventService.getEventCategories().subscribe({
      next: (categories: any) => {
        this.categories = categories;
        this.filteredCategories = categories;
      },
      error: (error: any) => {
        console.error('Error al cargar categorías:', error);
        this.errorMessage = 'Error al cargar categorías. Por favor, intenta nuevamente.';
      }
    });
  }

  searchCategories(): void {
    if (!this.categorySearchTerm.trim()) {
      this.filteredCategories = this.categories;
      return;
    }

    const term = this.categorySearchTerm.toLowerCase();
    this.filteredCategories = this.categories.filter(category => 
      category.name.toLowerCase().includes(term) ||
      (category.description && category.description.toLowerCase().includes(term))
    );
  }

  openCreateCategoryModal(): void {
    this.isCategoryEditMode = false;
    this.categoryForm = {
      id: undefined,
      name: '',
      description: '',
      active: true
    };
    this.showCategoryModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  openEditCategoryModal(category: EventCategory): void {
    this.isCategoryEditMode = true;
    this.categoryForm = { ...category };
    this.showCategoryModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeCategoryModal(): void {
    this.showCategoryModal = false;
    this.categoryForm = {
      id: undefined,
      name: '',
      description: '',
      active: true
    };
    this.errorMessage = '';
    this.successMessage = '';
    this.isSubmitting = false;
  }

  onCategorySubmit(): void {
    if (!this.categoryForm.name || this.categoryForm.name.trim() === '') {
      this.errorMessage = 'El nombre de la categoría es obligatorio';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const categoryData = {
      name: this.categoryForm.name.trim(),
      description: this.categoryForm.description?.trim() || '',
      active: this.categoryForm.active
    };

    if (this.isCategoryEditMode && this.categoryForm.id) {
      // Actualizar categoría existente
      this.eventService.updateEventCategory(this.categoryForm.id, categoryData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.successMessage = 'Categoría actualizada exitosamente';
          setTimeout(() => {
            this.closeCategoryModal();
            this.loadCategories();
          }, 1500);
        },
        error: (error: any) => {
          this.isSubmitting = false;
          console.error('Error al actualizar categoría:', error);
          this.errorMessage = error.error?.message || 'Error al actualizar la categoría. Por favor, intenta nuevamente.';
        }
      });
    } else {
      // Crear nueva categoría
      this.eventService.createEventCategory(categoryData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.successMessage = 'Categoría creada exitosamente';
          setTimeout(() => {
            this.closeCategoryModal();
            this.loadCategories();
          }, 1500);
        },
        error: (error: any) => {
          this.isSubmitting = false;
          console.error('Error al crear categoría:', error);
          this.errorMessage = error.error?.message || 'Error al crear la categoría. Por favor, intenta nuevamente.';
        }
      });
    }
  }

  deleteCategory(categoryId: number, categoryName: string): void {
    Swal.fire({
      title: '¿Eliminar categoría?',
      text: `¿Estás seguro de que deseas eliminar la categoría "${categoryName}"?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#d33'
    }).then((result) => {
      if (result.isConfirmed) {
        this.eventService.deleteEventCategory(categoryId).subscribe({
          next: () => {
            Swal.fire('Eliminado', 'Categoría eliminada exitosamente', 'success');
            this.loadCategories();
          },
          error: (error: any) => {
            console.error('Error al eliminar categoría:', error);
            Swal.fire('Error', error.error?.message || 'Error al eliminar la categoría. Por favor, intenta nuevamente.', 'error');
          }
        });
      }
    });
  }

  toggleCategoryStatus(category: EventCategory): void {
    const updatedCategory = {
      ...category,
      active: !category.active
    };

    this.eventService.updateEventCategory(category.id!, updatedCategory).subscribe({
      next: () => {
        this.successMessage = `Categoría ${updatedCategory.active ? 'activada' : 'desactivada'} exitosamente`;
        this.loadCategories();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (error: any) => {
        console.error('Error al cambiar estado de categoría:', error);
        this.errorMessage = error.error?.message || 'Error al cambiar el estado de la categoría.';
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
  }

  // Getters para validación en template
  get name() { return this.eventForm.get('name'); }
  get description() { return this.eventForm.get('description'); }
  get startTime() { return this.eventForm.get('startTime'); }
  get endTime() { return this.eventForm.get('endTime'); }
  get lat() { return this.eventForm.get('lat'); }
  get lng() { return this.eventForm.get('lng'); }
  get locationName() { return this.eventForm.get('locationName'); }
  get maxCapacity() { return this.eventForm.get('maxCapacity'); }
  get basePrice() { return this.eventForm.get('basePrice'); }
  get categoryId() { return this.eventForm.get('categoryId'); }

  // Helper para convertir Date a formato datetime-local (YYYY-MM-DDTHH:mm)
  private toDatetimeLocal(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  // Geocodificación inversa - Convertir coordenadas a dirección (ya no se usa, se hace en loadData)
  loadEventAddress(eventId: number, lat: number, lng: number): void {
    // BigDataCloud API - gratuita, sin API key, más confiable
    const url = `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lng}&localityLanguage=es`;
    
    this.http.get<any>(url).pipe(
      timeout(5000),
      catchError((err) => {
        console.error('Error geocoding:', err);
        this.eventAddresses.set(eventId, 'Ubicación no disponible');
        return of(null);
      })
    ).subscribe({
      next: (data) => {
        if (data) {
          const placeName = data.locality || 
                           data.city || 
                           data.principalSubdivision || 
                           data.countryName || 
                           'Ubicación';
          this.eventAddresses.set(eventId, placeName);
        }
      }
    });
  }

  getEventAddress(event: Event): string {
    // Priorizar locationName si existe
    if (event.locationName) {
      return event.locationName;
    }
    
    // Luego intentar con el caché de geocoding
    if (event.id && this.eventAddresses.has(event.id)) {
      return this.eventAddresses.get(event.id)!;
    }
    
    // Fallback a coordenadas
    return `${event.lat}, ${event.lng}`;
  }

  // Abrir mapa flotante con la ubicación del evento
  openMapModal(event: Event): void {
    this.mapEventLocation = {
      lat: event.lat,
      lng: event.lng,
      name: event.name,
      address: this.getEventAddress(event)
    };
    this.showMapModal = true;
  }

  // Cerrar mapa flotante
  closeMapModal(): void {
    this.showMapModal = false;
    this.mapEventLocation = null;
  }

  // Ver descripción del evento
  viewDetails(event: Event): void {
    // Construir HTML con descripción
    let htmlContent = `
      <div style="text-align: left; max-height: 500px; overflow-y: auto; padding: 10px;">
        <div style="margin-bottom: 20px;">
          <h4 style="color: #667eea; margin-bottom: 10px; border-bottom: 2px solid #667eea; padding-bottom: 5px;">
            <i class="bi bi-file-text"></i> Descripción
          </h4>
          <p style="line-height: 1.8; color: #555; white-space: pre-wrap;">${event.description || 'Sin descripción'}</p>
        </div>
    `;

    // Agregar consumiciones si están disponibles
    if (event.availableConsumptions && event.availableConsumptions.length > 0) {
      htmlContent += `
        <div style="margin-top: 20px;">
          <h4 style="color: #667eea; margin-bottom: 10px; border-bottom: 2px solid #667eea; padding-bottom: 5px;">
            <i class="bi bi-cup-straw"></i> Consumiciones Disponibles
          </h4>
          <div style="display: grid; gap: 10px;">
      `;
      
      event.availableConsumptions.forEach(consumption => {
        const categoryName = this.consumptionCategories.find(c => c.id === consumption.categoryId)?.name || 'Sin categoría';
        htmlContent += `
          <div style="border: 1px solid #e0e0e0; border-radius: 8px; padding: 12px; background: #f9f9f9;">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div>
                <strong style="color: #333; font-size: 16px;">${consumption.name}</strong>
                <div style="margin-top: 4px;">
                  <span style="background: #667eea; color: white; padding: 2px 8px; border-radius: 12px; font-size: 12px;">
                    ${categoryName}
                  </span>
                </div>
              </div>
              <div style="text-align: right;">
                <strong style="color: #667eea; font-size: 18px;">$${consumption.price.toFixed(2)}</strong>
                <div style="color: #999; font-size: 12px; margin-top: 2px;">Precio</div>
              </div>
            </div>
          </div>
        `;
      });
      
      htmlContent += `
          </div>
        </div>
      `;
    } else {
      htmlContent += `
        <div style="margin-top: 20px;">
          <h4 style="color: #667eea; margin-bottom: 10px; border-bottom: 2px solid #667eea; padding-bottom: 5px;">
            <i class="bi bi-cup-straw"></i> Consumiciones Disponibles
          </h4>
          <p style="color: #999; font-style: italic;">No hay consumiciones disponibles para este evento</p>
        </div>
      `;
    }

    htmlContent += `</div>`;

    Swal.fire({
      title: event.name,
      html: htmlContent,
      icon: 'info',
      confirmButtonText: 'Cerrar',
      confirmButtonColor: '#667eea',
      width: '700px'
    });
  }
}
