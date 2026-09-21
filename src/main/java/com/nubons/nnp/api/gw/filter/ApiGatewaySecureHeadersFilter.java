package com.nubons.nnp.api.gw.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.http.HttpHeaders;

@Slf4j
public class ApiGatewaySecureHeadersFilter extends AbstractCustomFilter {

    public ApiGatewaySecureHeadersFilter() {
        super();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("X-Frame-Options", "DENY");
            headers.set("X-XSS-Protection", "1; mode=block");
            headers.set("Referrer-Policy", "no-referrer");
            headers.set("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains");
            headers.set("X-Download-Options", "noopen");
            headers.set("X-Permitted-Cross-Domain-Policies", "none");
            return chain.filter(exchange);
        }, GwFilterOrderingEnum.valueOf(config.getOrder()).getOrder());
    }
}
