package com.nubons.nnp.api.gw.filter;

import com.nubons.nnp.api.gw.filter.ApiGatewayRequestSizeFilter.RequestSizeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("UnitTestCode-ApiGatewayRequestSizeFilter")
class ApiGatewayRequestSizeFilterTest {

    private GatewayFilterChain chain;

    @BeforeEach
    void setup() {
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void underMax_passes() {
        ApiGatewayRequestSizeFilter filter = new ApiGatewayRequestSizeFilter();
        GatewayFilter gatewayFilter = filter.apply(config("5 MB"));

        MockServerHttpRequest req = MockServerHttpRequest.get("https://gw.test/x")
                .header("Content-Length", "1024").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);

        StepVerifier.create(gatewayFilter.filter(exchange, chain)).verifyComplete();
        // No 413 set means the request passed through.
    }

    @Test
    void overMax_rejectedWith413() {
        ApiGatewayRequestSizeFilter filter = new ApiGatewayRequestSizeFilter();
        GatewayFilter gatewayFilter = filter.apply(config("1 MB"));

        MockServerHttpRequest req = MockServerHttpRequest.get("https://gw.test/x")
                .header("Content-Length", String.valueOf(2L * 1024 * 1024)).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .verifyComplete();
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, exchange.getResponse().getStatusCode());
    }

    @Test
    void runsAtFixedEarlyOrder_beforeRequestBodyHandler() {
        ApiGatewayRequestSizeFilter filter = new ApiGatewayRequestSizeFilter();
        GatewayFilter gatewayFilter = filter.apply(config("1 MB"));

        int order = ((OrderedGatewayFilter) gatewayFilter).getOrder();
        assertEquals(ApiGatewayRequestSizeFilter.FILTER_ORDER, order);
        if (order >= -5) {
            throw new AssertionError("FILTER_ORDER (" + order + ") must be < -5 to run before RequestBodyHandlerFilter");
        }
    }

    private RequestSizeConfig config(String maxSize) {
        RequestSizeConfig cfg = new RequestSizeConfig();
        cfg.setName("ApiGatewayRequestSizeFilter");
        cfg.setOrder("ORDER_7");
        cfg.setMaxSize(maxSize);
        return cfg;
    }
}
