package com.packed_go.event_service.dtos.qr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateEntryQRRequest {
    private String qrCode;  // El contenido del QR escaneado
    private Long eventId;   // ID del evento al que intenta ingresar
}
