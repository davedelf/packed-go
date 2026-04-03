import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import Swal from 'sweetalert2';
import { AuthService } from '../../../core/services/auth.service';
import { EmployeeService, EmployeeWithEvents, CreateEmployeeRequest, UpdateEmployeeRequest } from '../../../core/services/employee.service';
import { EventService } from '../../../core/services/event.service';

@Component({
  selector: 'app-employee-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './employee-management.component.html',
  styleUrl: './employee-management.component.css'
})
export class EmployeeManagementComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private employeeService = inject(EmployeeService);
  private eventService = inject(EventService);
  private router = inject(Router);

  // State
  employees: EmployeeWithEvents[] = [];
  myEvents: AdminEvent[] = [];
  showCreateModal = false;
  showEditModal = false;
  selectedEmployee: EmployeeWithEvents | null = null;
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  // Forms
  createEmployeeForm: FormGroup;
  editEmployeeForm: FormGroup;

  constructor() {
    this.createEmployeeForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      document: ['', [Validators.required, Validators.pattern(/^\d+$/)]],
      assignedEventIds: [[]] // Array of event IDs
    });

    this.editEmployeeForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      username: ['', [Validators.required, Validators.minLength(3)]],
      document: ['', [Validators.required, Validators.pattern(/^\d+$/)]],
      assignedEventIds: [[]]
    });
  }

  ngOnInit(): void {
    this.loadMyEvents();
    this.loadEmployees();
  }

  goBack(): void {
    this.router.navigate(['/admin/dashboard']);
  }

  loadMyEvents(): void {
    this.eventService.getEvents().subscribe({
      next: (events: any) => {
        // Filtrar solo eventos activos del admin actual
        this.myEvents = events
          .filter((e: any) => e.active)
          .map((e: any) => ({
            id: e.id,
            name: e.name,
            date: e.eventDate ? new Date(e.eventDate) : null,
            location: e.locationName || 'Sin ubicación',
            status: e.active ? 'ACTIVE' : 'INACTIVE'
          }));
      },
      error: (error: any) => {
        console.error('Error al cargar eventos:', error);
        this.errorMessage = 'Error al cargar eventos. Por favor, intenta nuevamente.';
      }
    });
  }

  loadEmployees(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.employeeService.getMyEmployees().subscribe({
      next: (employees) => {
        this.employees = employees;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error al cargar empleados:', error);
        this.errorMessage = error.error?.message || 'Error al cargar empleados. Por favor, intenta nuevamente.';
        this.isLoading = false;
      }
    });
  }

  openCreateModal(): void {
    this.showCreateModal = true;
    this.createEmployeeForm.reset();
    this.createEmployeeForm.patchValue({ assignedEventIds: [] });
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
    this.createEmployeeForm.reset();
  }

  openEditModal(employee: EmployeeWithEvents): void {
    this.selectedEmployee = employee;
    this.showEditModal = true;
    this.editEmployeeForm.patchValue({
      email: employee.email,
      username: employee.username,
      document: employee.document,
      assignedEventIds: employee.assignedEvents.map(e => e.id)
    });
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedEmployee = null;
    this.editEmployeeForm.reset();
  }

  onEventCheckboxChange(eventId: number, isChecked: boolean, formGroup: FormGroup): void {
    const currentIds = formGroup.get('assignedEventIds')?.value || [];
    
    if (isChecked) {
      if (!currentIds.includes(eventId)) {
        formGroup.patchValue({
          assignedEventIds: [...currentIds, eventId]
        });
      }
    } else {
      formGroup.patchValue({
        assignedEventIds: currentIds.filter((id: number) => id !== eventId)
      });
    }
  }

  isEventSelected(eventId: number, formGroup: FormGroup): boolean {
    const currentIds = formGroup.get('assignedEventIds')?.value || [];
    return currentIds.includes(eventId);
  }

  onCreateEmployee(): void {
    if (this.createEmployeeForm.invalid) {
      this.createEmployeeForm.markAllAsTouched();
      return;
    }

    const formValue = this.createEmployeeForm.value;
    
    if (formValue.assignedEventIds.length === 0) {
      this.errorMessage = 'Debe asignar al menos un evento al empleado';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const request: CreateEmployeeRequest = {
      email: formValue.email,
      username: formValue.username,
      password: formValue.password,
      document: Number(formValue.document),
      assignedEventIds: formValue.assignedEventIds
    };

    this.employeeService.createEmployee(request).subscribe({
      next: () => {
        this.successMessage = '¡Empleado creado exitosamente!';
        this.isLoading = false;
        
        setTimeout(() => {
          this.closeCreateModal();
          this.loadEmployees();
          this.successMessage = '';
        }, 1500);
      },
      error: (error) => {
        console.error('Error al crear empleado:', error);
        this.errorMessage = error.error?.message || 'Error al crear empleado. Por favor, intenta nuevamente.';
        this.isLoading = false;
      }
    });
  }

  onUpdateEmployee(): void {
    if (this.editEmployeeForm.invalid || !this.selectedEmployee) {
      this.editEmployeeForm.markAllAsTouched();
      return;
    }

    const formValue = this.editEmployeeForm.value;
    
    if (formValue.assignedEventIds.length === 0) {
      this.errorMessage = 'Debe asignar al menos un evento al empleado';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const request: UpdateEmployeeRequest = {
      email: formValue.email,
      username: formValue.username,
      document: Number(formValue.document),
      assignedEventIds: formValue.assignedEventIds
    };

    this.employeeService.updateEmployee(this.selectedEmployee.id, request).subscribe({
      next: () => {
        this.successMessage = '¡Empleado actualizado exitosamente!';
        this.isLoading = false;
        
        setTimeout(() => {
          this.closeEditModal();
          this.loadEmployees();
          this.successMessage = '';
        }, 1500);
      },
      error: (error) => {
        console.error('Error al actualizar empleado:', error);
        this.errorMessage = error.error?.message || 'Error al actualizar empleado. Por favor, intenta nuevamente.';
        this.isLoading = false;
      }
    });
  }

  toggleEmployeeStatus(employee: EmployeeWithEvents): void {
    const action = employee.isActive ? 'desactivar' : 'activar';
    Swal.fire({
      title: `¿${action.charAt(0).toUpperCase() + action.slice(1)} empleado?`,
      text: `¿Está seguro de ${action} a ${employee.username}?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: `Sí, ${action}`,
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.employeeService.toggleEmployeeStatus(employee.id).subscribe({
          next: () => {
            employee.isActive = !employee.isActive;
            Swal.fire('Actualizado', `Empleado ${employee.isActive ? 'activado' : 'desactivado'} exitosamente`, 'success');
          },
          error: (error) => {
            console.error('Error al cambiar estado del empleado:', error);
            Swal.fire('Error', error.error?.message || 'Error al cambiar estado del empleado.', 'error');
          }
        });
      }
    });
  }

  deleteEmployee(employee: EmployeeWithEvents): void {
    Swal.fire({
      title: '¿Eliminar empleado?',
      text: `¿Está seguro de eliminar al empleado ${employee.username}? Esta acción no se puede deshacer.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#d33'
    }).then((result) => {
      if (result.isConfirmed) {
        this.employeeService.deleteEmployee(employee.id).subscribe({
          next: () => {
            this.employees = this.employees.filter(e => e.id !== employee.id);
            Swal.fire('Eliminado', 'Empleado eliminado exitosamente', 'success');
          },
          error: (error) => {
            console.error('Error al eliminar empleado:', error);
            Swal.fire('Error', error.error?.message || 'Error al eliminar empleado.', 'error');
          }
        });
      }
    });
  }

  getEventNames(employee: EmployeeWithEvents): string {
    return employee.assignedEvents.map(e => e.name).join(', ');
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  isValidDate(date: any): boolean {
    if (!date) return false;
    const d = new Date(date);
    return d instanceof Date && !isNaN(d.getTime());
  }

  formatDate(date: any): string {
    if (!this.isValidDate(date)) return 'Fecha no disponible';
    return new Date(date).toLocaleDateString('es-ES');
  }
}

// Interfaces
interface AdminEvent {
  id: number;
  name: string;
  date: Date;
  location: string;
  status: string;
}
