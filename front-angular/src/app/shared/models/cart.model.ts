export interface Cart {
  id: number;
  userId: number;
  status: 'ACTIVE' | 'EXPIRED' | 'COMPLETED';
  expiresAt: string;
  createdAt: string;
  updatedAt: string;
  items: CartItem[];
  totalAmount: number;
  itemCount: number;
  expired: boolean;
}

export interface CartItem {
  id: number;
  eventId: number;
  eventName: string;
  adminId: number;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  consumptions: CartItemConsumption[];
}

export interface CartItemConsumption {
  id: number;
  consumptionId: number;
  consumptionName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface AddToCartRequest {
  eventId: number;
  quantity?: number; // Cantidad de entradas del evento (opcional, default 1)
  consumptions: ConsumptionRequest[];
}

export interface ConsumptionRequest {
  consumptionId: number;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}

export interface CartErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  validationErrors?: { [key: string]: string };
}
