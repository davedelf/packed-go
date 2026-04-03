package com.packed_go.order_service.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Utilidad para manejo de fechas y horas con zona horaria dinámica
 */
public class DateTimeUtils {
    
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    
    /**
     * Obtiene la fecha y hora actual en la zona horaria por defecto (Argentina)
     * @return LocalDateTime con la hora actual
     */
    public static LocalDateTime now() {
        return ZonedDateTime.now(DEFAULT_ZONE).toLocalDateTime();
    }
    
    /**
     * Obtiene la fecha y hora actual en una zona horaria específica
     * @param timezone Zona horaria (ej: "America/New_York", "Europe/Madrid")
     * @return LocalDateTime con la hora actual en la zona especificada
     */
    public static LocalDateTime now(String timezone) {
        if (timezone == null || timezone.isEmpty()) {
            return now();
        }
        
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            return ZonedDateTime.now(zoneId).toLocalDateTime();
        } catch (Exception e) {
            // Si el timezone no es válido, usar zona por defecto
            return now();
        }
    }
    
    /**
     * Obtiene la zona horaria por defecto (Argentina)
     * @return ZoneId por defecto
     */
    public static ZoneId getDefaultZone() {
        return DEFAULT_ZONE;
    }
    
    /**
     * Valida si un string es un timezone válido
     * @param timezone String del timezone a validar
     * @return true si es válido, false si no
     */
    public static boolean isValidTimezone(String timezone) {
        if (timezone == null || timezone.isEmpty()) {
            return false;
        }
        
        try {
            ZoneId.of(timezone);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
