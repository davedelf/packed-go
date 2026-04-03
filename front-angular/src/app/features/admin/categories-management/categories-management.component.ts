import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import Swal from 'sweetalert2';
import { EventService } from '../../../core/services/event.service';
import { AuthService } from '../../../core/services/auth.service';
import { EventCategory, ConsumptionCategory } from '../../../shared/models/event.model';

type CategoryType = 'event' | 'consumption';

@Component({
  selector: 'app-categories-management',
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './categories-management.component.html',
  styleUrl: './categories-management.component.css'
})
export class CategoriesManagementComponent implements OnInit {
  private fb = inject(FormBuilder);
  private eventService = inject(EventService);
  private authService = inject(AuthService);
  private router = inject(Router);

  eventCategories: EventCategory[] = [];
  consumptionCategories: ConsumptionCategory[] = [];
  
  categoryForm: FormGroup;
  isLoading = true;
  isSubmitting = false;
  showModal = false;
  isEditMode = false;
  currentCategoryId?: number;
  currentCategoryType: CategoryType = 'event';
  
  activeTab: CategoryType = 'event';
  errorMessage = '';
  successMessage = '';

  constructor() {
    this.categoryForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(500)]]
    });
  }

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.isLoading = true;
    
    // Cargar categorías de eventos
    this.eventService.getEventCategories().subscribe({
      next: (categories: any) => {
        this.eventCategories = categories;
      },
      error: (error: any) => console.error('Error al cargar categorías de eventos:', error)
    });

    // Cargar categorías de consumiciones
    this.eventService.getConsumptionCategories().subscribe({
      next: (categories: any) => {
        this.consumptionCategories = categories;
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Error al cargar categorías de consumiciones:', error);
        this.isLoading = false;
        this.errorMessage = 'Error al cargar categorías. Por favor, intenta nuevamente.';
      }
    });
  }

  switchTab(tab: CategoryType): void {
    this.activeTab = tab;
  }

  openCreateModal(type: CategoryType): void {
    this.isEditMode = false;
    this.currentCategoryId = undefined;
    this.currentCategoryType = type;
    this.categoryForm.reset();
    this.showModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  openEditModal(category: EventCategory | ConsumptionCategory, type: CategoryType): void {
    this.isEditMode = true;
    this.currentCategoryId = category.id;
    this.currentCategoryType = type;
    
    this.categoryForm.patchValue({
      name: category.name,
      description: category.description || ''
    });
    
    this.showModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeModal(): void {
    this.showModal = false;
    this.categoryForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
  }

  onSubmit(): void {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const categoryData = {
      name: this.categoryForm.value.name,
      description: this.categoryForm.value.description || null
    };

    if (this.isEditMode && this.currentCategoryId) {
      // Actualizar categoría existente
      this.updateCategory(this.currentCategoryId, categoryData);
    } else {
      // Crear nueva categoría
      this.createCategory(categoryData);
    }
  }

  createCategory(data: any): void {
    const request$ = this.currentCategoryType === 'event' 
      ? this.eventService.createEventCategory(data)
      : this.eventService.createConsumptionCategory(data);

    request$.subscribe({
      next: () => {
        this.successMessage = `Categoría ${this.currentCategoryType === 'event' ? 'de evento' : 'de consumición'} creada exitosamente`;
        setTimeout(() => {
          this.closeModal();
          this.loadCategories();
        }, 1500);
      },
      error: (error: any) => {
        console.error('Error al crear categoría:', error);
        this.errorMessage = error.error?.message || 'Error al crear la categoría. Por favor, intenta nuevamente.';
        this.isSubmitting = false;
      }
    });
  }

  updateCategory(id: number, data: any): void {
    const request$ = this.currentCategoryType === 'event'
      ? this.eventService.updateEventCategory(id, data)
      : this.eventService.updateConsumptionCategory(id, data);

    request$.subscribe({
      next: () => {
        this.successMessage = `Categoría ${this.currentCategoryType === 'event' ? 'de evento' : 'de consumición'} actualizada exitosamente`;
        setTimeout(() => {
          this.closeModal();
          this.loadCategories();
        }, 1500);
      },
      error: (error: any) => {
        console.error('Error al actualizar categoría:', error);
        this.errorMessage = error.error?.message || 'Error al actualizar la categoría. Por favor, intenta nuevamente.';
        this.isSubmitting = false;
      }
    });
  }

  deleteCategory(categoryId: number, categoryName: string, type: CategoryType): void {
    const typeText = type === 'event' ? 'de evento' : 'de consumición';
    Swal.fire({
      title: `¿Eliminar categoría ${typeText}?`,
      text: `¿Estás seguro de que deseas eliminar la categoría ${typeText} "${categoryName}"?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#d33'
    }).then((result) => {
      if (result.isConfirmed) {
        const request$ = type === 'event'
          ? this.eventService.deleteEventCategory(categoryId)
          : this.eventService.deleteConsumptionCategory(categoryId);

        request$.subscribe({
          next: () => {
            Swal.fire('Eliminado', `Categoría ${typeText} eliminada exitosamente`, 'success');
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

  goBack(): void {
    this.router.navigate(['/admin/dashboard']);
  }

  logout(): void {
    this.authService.logout();
  }

  // Getters para validación en template
  get name() { return this.categoryForm.get('name'); }
  get description() { return this.categoryForm.get('description'); }
}
