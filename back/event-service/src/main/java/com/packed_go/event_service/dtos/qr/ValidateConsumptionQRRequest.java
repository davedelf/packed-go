package com.packed_go.event_service.dtos.qr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateConsumptionQRRequest {
    private String qrCode;      // El contenido del QR escaneado
    private Long eventId;       // ID del evento
    private Long detailId;      // ID del detalle de consumición seleccionado por el empleado
    private Integer quantity;   // Cantidad a canjear (opcional, default 1)
}
