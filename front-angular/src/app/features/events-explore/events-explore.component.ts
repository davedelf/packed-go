import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

interface Event {
  id: number;
  name: string;
  description: string;
  startTime: string;
  endTime: string;
  locationName: string;
  latitude?: number;
  longitude?: number;
  imageUrl?: string;
  hasImageData?: boolean;
  category?: {
    id: number;
    name: string;
  };
  active: boolean;
}

@Component({
  selector: 'app-events-explore',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './events-explore.component.html',
  styleUrls: ['./events-explore.component.css']
})
export class EventsExploreComponent implements OnInit, OnDestroy {
  events: Event[] = [];
  currentIndex = 0;
  autoPlayInterval: any;
  isLoading = true;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadEvents();
  }

  ngOnDestroy(): void {
    this.stopAutoPlay();
  }

  loadEvents(): void {
    const apiUrl = environment.eventsServiceUrl;
    this.http.get<Event[]>(`${apiUrl}/event-service/event`)
      .subscribe({
        next: (data) => {
          // Filtrar solo eventos activos y procesar imágenes
          this.events = data.filter(event => event.active).map(event => {
            // Prioridad 1: imagen subida al servidor (archivo local)
            if (event.hasImageData && event.id) {
              return {
                ...event,
                imageUrl: `${apiUrl}/event-service/event/${event.id}/image`
              };
            }
            // Prioridad 2: URL externa
            if (event.imageUrl) {
              return event;
            }
            // Prioridad 3: placeholder
            return {
              ...event,
              imageUrl: 'https://via.placeholder.com/800x400?text=Sin+Imagen'
            };
          });
          
          this.isLoading = false;
          if (this.events.length > 0) {
            this.startAutoPlay();
          }
        },
        error: (error) => {
          console.error('Error cargando eventos:', error);
          this.isLoading = false;
        }
      });
  }

  startAutoPlay(): void {
    this.autoPlayInterval = setInterval(() => {
      this.nextEvent();
    }, 5000); // Cambiar cada 5 segundos
  }

  stopAutoPlay(): void {
    if (this.autoPlayInterval) {
      clearInterval(this.autoPlayInterval);
    }
  }

  nextEvent(): void {
    if (this.events.length > 0) {
      this.currentIndex = (this.currentIndex + 1) % this.events.length;
    }
  }

  prevEvent(): void {
    if (this.events.length > 0) {
      this.currentIndex = (this.currentIndex - 1 + this.events.length) % this.events.length;
    }
  }

  goToEvent(index: number): void {
    this.currentIndex = index;
    this.stopAutoPlay();
    this.startAutoPlay();
  }

  get currentEvent(): Event | null {
    return this.events.length > 0 ? this.events[this.currentIndex] : null;
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('es-AR', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  goBack(): void {
    this.router.navigate(['/']);
  }

  goToRegister(): void {
    this.router.navigate(['/customer/register']);
  }

  goToLogin(): void {
    this.router.navigate(['/customer/login']);
  }

  openGoogleMaps(event: Event): void {
    if (event.latitude && event.longitude) {
      const url = `https://www.google.com/maps/search/?api=1&query=${event.latitude},${event.longitude}`;
      window.open(url, '_blank');
    } else if (event.locationName) {
      const url = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(event.locationName)}`;
      window.open(url, '_blank');
    }
  }
}
