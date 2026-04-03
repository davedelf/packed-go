package com.packed_go.users_service.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class EmployeeDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateEmployeeRequest {
        private String email;
        private String username;
        private String password;
        private Long document;
        private Set<Long> assignedEventIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateEmployeeRequest {
        private String email;
        private String username;
        private Long document;
        private Set<Long> assignedEventIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeResponse {
        private Long id;
        private String email;
        private String username;
        private Long document;
        private Long adminId;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private Set<Long> assignedEventIds;
        private List<AssignedEventInfo> assignedEvents; // Con info completa
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignedEventInfo {
        private Long id;
        private String name;
        private String location;
        private LocalDateTime eventDate;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeLoginRequest {
        private String email;
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateEmployeeResponse {
        private Long id;
        private String email;
        private String username;
        private Long document;
        private Long adminId;
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeStatsResponse {
        private Long ticketsScannedToday;
        private Long consumptionsToday;
        private Long totalScannedToday;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateTicketRequest {
        private String qrCode;
        private Long eventId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateTicketResponse {
        private Boolean valid;
        private String message;
        private TicketInfo ticketInfo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketInfo {
        private Long ticketId;
        private Long userId;
        private String customerName;
        private String eventName;
        private String passType;
        private Boolean alreadyUsed;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterConsumptionRequest {
        private String qrCode;
        private Long eventId;
        private Long detailId;
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterConsumptionResponse {
        private Boolean success;
        private String message;
        private ConsumptionInfo consumptionInfo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsumptionInfo {
        private Long consumptionId;
        private String consumptionType;
        private String customerName;
        private String eventName;
        private LocalDateTime registeredAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FindTicketByCodeRequest {
        private String code;
        private Long eventId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketSearchResponse {
        private Long ticketId;
        private String qrCode;
        private String passCode;
        private Long eventId;
        private String eventName;
        private Long userId;
    }
}
