import { Component, EventEmitter, Input, Output, OnInit, AfterViewInit, OnChanges, SimpleChanges, PLATFORM_ID, Inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

export interface Location {
  lat: number;
  lng: number;
  address?: string;
}

declare var L: any;

@Component({
  selector: 'app-location-picker',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './location-picker.component.html',
  styleUrls: ['./location-picker.component.css']
})
export class LocationPickerComponent implements OnInit, AfterViewInit, OnChanges {
  @Input() lat: number = -34.603722; // Buenos Aires por defecto
  @Input() lng: number = -58.381592;
  @Output() locationChange = new EventEmitter<Location>();

  searchQuery: string = '';
  map: any;
  marker: any;
  isSearching: boolean = false;
  private isBrowser: boolean;

  constructor(
    @Inject(PLATFORM_ID) platformId: Object,
    private http: HttpClient
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    if (this.isBrowser) {
      this.loadLeafletScript();
    }
  }

  ngAfterViewInit(): void {
    // La inicialización del mapa se hace en loadLeafletScript después de cargar el script
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Detectar cambios en lat/lng y actualizar mapa si ya está inicializado
    if ((changes['lat'] || changes['lng']) && this.map && this.marker) {
      const newLat = changes['lat']?.currentValue ?? this.lat;
      const newLng = changes['lng']?.currentValue ?? this.lng;
      
      // Solo actualizar si los valores realmente cambiaron
      if (newLat !== this.marker.getLatLng().lat || newLng !== this.marker.getLatLng().lng) {
        this.marker.setLatLng([newLat, newLng]);
        this.map.setView([newLat, newLng], this.map.getZoom());
        this.emitLocation(newLat, newLng);
      }
    }
  }

  private loadLeafletScript(): void {
    // Verificar si Leaflet ya está cargado
    if (typeof L !== 'undefined') {
      this.initializeMap();
      return;
    }

    // Cargar CSS de Leaflet
    const linkElement = document.createElement('link');
    linkElement.rel = 'stylesheet';
    linkElement.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
    document.head.appendChild(linkElement);

    // Cargar JS de Leaflet
    const script = document.createElement('script');
    script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
    script.async = true;
    script.defer = true;
    script.onload = () => {
      this.initializeMap();
    };
    document.head.appendChild(script);
  }

  private initializeMap(): void {
    if (!this.isBrowser || typeof L === 'undefined') {
      return;
    }

    const mapElement = document.getElementById('map');
    if (!mapElement) {
      console.error('Map element not found');
      return;
    }

    // Inicializar mapa de Leaflet
    this.map = L.map('map').setView([this.lat, this.lng], 15);

    // Agregar tiles de OpenStreetMap
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19
    }).addTo(this.map);

    // Crear marcador personalizado
    const customIcon = L.icon({
      iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
      shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      shadowSize: [41, 41]
    });

    // Agregar marcador arrastrable
    this.marker = L.marker([this.lat, this.lng], {
      draggable: true,
      icon: customIcon
    }).addTo(this.map);

    // Evento al arrastrar el marcador
    this.marker.on('dragend', (event: any) => {
      const position = event.target.getLatLng();
      this.updateLocation(position.lat, position.lng);
    });

    // Evento al hacer clic en el mapa
    this.map.on('click', (event: any) => {
      this.updateLocation(event.latlng.lat, event.latlng.lng);
    });

    // Emitir ubicación inicial
    this.emitLocation(this.lat, this.lng);
  }

  private updateLocation(lat: number, lng: number, address?: string): void {
    this.lat = lat;
    this.lng = lng;

    // Actualizar posición del marcador
    this.marker.setLatLng([lat, lng]);
    this.map.setView([lat, lng], this.map.getZoom());

    // Si no hay dirección, obtenerla mediante geocoding inverso con Nominatim
    if (!address) {
      this.reverseGeocode(lat, lng);
    } else {
      this.emitLocation(lat, lng, address);
    }
  }

  private reverseGeocode(lat: number, lng: number): void {
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`;
    
    this.http.get<any>(url, {
      headers: {
        'Accept-Language': 'es'
      }
    }).subscribe({
      next: (data) => {
        const address = data.display_name || `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
        this.emitLocation(lat, lng, address);
      },
      error: () => {
        this.emitLocation(lat, lng);
      }
    });
  }

  private emitLocation(lat: number, lng: number, address?: string): void {
    this.locationChange.emit({ lat, lng, address });
  }

  searchLocation(): void {
    if (!this.searchQuery.trim()) {
      return;
    }

    this.isSearching = true;
    const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(this.searchQuery)}&limit=1`;

    this.http.get<any[]>(url, {
      headers: {
        'Accept-Language': 'es'
      }
    }).subscribe({
      next: (results) => {
        this.isSearching = false;
        if (results && results.length > 0) {
          const result = results[0];
          const lat = parseFloat(result.lat);
          const lng = parseFloat(result.lon);
          this.updateLocation(lat, lng, result.display_name);
        } else {
          alert('No se pudo encontrar la ubicación. Intenta con otra búsqueda.');
        }
      },
      error: (error) => {
        this.isSearching = false;
        console.error('Error en búsqueda:', error);
        alert('Error al buscar la ubicación. Intenta de nuevo.');
      }
    });
  }

  getCurrentLocation(): void {
    if (!navigator.geolocation) {
      alert('Tu navegador no soporta geolocalización');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;
        this.updateLocation(lat, lng);
      },
      (error) => {
        console.error('Error obteniendo ubicación:', error);
        alert('No se pudo obtener tu ubicación actual');
      }
    );
  }
}
