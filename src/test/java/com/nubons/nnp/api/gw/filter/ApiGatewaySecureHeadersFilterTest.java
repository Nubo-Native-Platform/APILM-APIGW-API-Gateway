package com.nubons.nnp.api.gw.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("UnitTestCode-ApiGatewaySecureHeadersFilter")
class ApiGatewaySecureHeadersFilterTest {

    private GatewayFilterChain chain;

    @BeforeEach
    void setup() {
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void securityHeadersAreApplied() {
        ApiGatewaySecureHeadersFilter filter = new ApiGatewaySecureHeadersFilter();
        GatewayFilter gatewayFilter = filter.apply(config());

        MockServerHttpRequest req = MockServerHttpRequest.get("https://gw.test/x").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);

        StepVerifier.create(gatewayFilter.filter(exchange, chain)).verifyComplete();

        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
        assertEquals("DENY", headers.getFirst("X-Frame-Options"));
        assertEquals("1; mode=block", headers.getFirst("X-XSS-Protection"));
        assertEquals("no-referrer", headers.getFirst("Referrer-Policy"));
    }

    private AbstractCustomFilter.Config config() {
        AbstractCustomFilter.Config cfg = new AbstractCustomFilter.Config();
        cfg.setName("ApiGatewaySecureHeadersFilter");
        cfg.setOrder("ORDER_7");
        return cfg;
    }
}
