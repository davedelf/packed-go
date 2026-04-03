package com.packedgo.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookNotification {

    private String action;
    private String apiVersion;
    private WebhookData data;
    private String dateCreated;
    private Long id;
    private Boolean liveMode;
    private String type;
    private Long userId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookData {
        private String id;
    }
}