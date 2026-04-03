package com.packed_go.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PublicEndpointFilter extends AbstractGatewayFilterFactory<PublicEndpointFilter.Config> {

    public PublicEndpointFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            log.info("🌐 PublicEndpointFilter - Path: {} (No authentication required)", 
                    exchange.getRequest().getPath());
            return chain.filter(exchange);
        };
    }

    public static class Config {
        // Configuration properties if needed
    }
}
