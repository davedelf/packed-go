export interface Event {
  id?: number;
  name: string;
  description: string;
  eventDate: string;
  startTime?: string; // 🕐 Hora de inicio
  endTime?: string;   // 🕐 Hora de finalización
  lat: number;
  lng: number;
  locationName?: string;
  maxCapacity: number;
  currentCapacity?: number;
  basePrice: number;
  imageUrl?: string;
  hasImageData?: boolean;
  imageContentType?: string;
  categoryId: number;
  category?: EventCategory; // Objeto categoría completo
  status?: string;
  active?: boolean;
  totalPasses?: number;
  availablePasses?: number;
  soldPasses?: number;
  availableConsumptions?: Consumption[];
  createdAt?: string;
  updatedAt?: string;
}

export interface EventCategory {
  id?: number;
  name: string;
  description?: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ConsumptionCategory {
  id?: number;
  name: string;
  description?: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface Consumption {
  id?: number;
  categoryId: number;
  name: string;
  description?: string;
  price: number;
  imageUrl?: string;
  imageData?: string; // Base64-encoded image
  imageContentType?: string;
  hasImageData?: boolean; // Flag para indicar si tiene imagen local
  active?: boolean;
  createdBy?: number; // 🔒 Nuevo campo multi-tenant
  createdAt?: string;
  updatedAt?: string;
}
