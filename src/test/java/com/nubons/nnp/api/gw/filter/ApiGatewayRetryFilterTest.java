package com.nubons.nnp.api.gw.filter;

import com.nubons.nnp.api.gw.filter.ApiGatewayRetryFilter.RetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("UnitTestCode-ApiGatewayRetryFilter")
class ApiGatewayRetryFilterTest {

    private GatewayFilterChain chain;

    @BeforeEach
    void setup() {
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void appliesWithParsedArgs_returnsOrderedFilter() {
        ApiGatewayRetryFilter filter = new ApiGatewayRetryFilter();
        GatewayFilter gatewayFilter = filter.apply(config("3", "SERVER_ERROR", "GET"));

        MockServerHttpRequest req = MockServerHttpRequest.get("https://gw.test/x").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);

        StepVerifier.create(gatewayFilter.filter(exchange, chain)).verifyComplete();
        verify(chain, times(1)).filter(any(ServerWebExchange.class));
    }

    @Test
    void blankArgs_useDefaultsAndStillApply() {
        ApiGatewayRetryFilter filter = new ApiGatewayRetryFilter();
        GatewayFilter gatewayFilter = filter.apply(config(null, null, null));

        MockServerHttpRequest req = MockServerHttpRequest.get("https://gw.test/x").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);

        StepVerifier.create(gatewayFilter.filter(exchange, chain)).verifyComplete();
        verify(chain, times(1)).filter(any(ServerWebExchange.class));
    }

    private RetryConfig config(String retries, String series, String methods) {
        RetryConfig cfg = new RetryConfig();
        cfg.setName("ApiGatewayRetryFilter");
        cfg.setOrder("ORDER_7");
        cfg.setRetries(retries);
        cfg.setSeries(series);
        cfg.setMethods(methods);
        return cfg;
    }
}
