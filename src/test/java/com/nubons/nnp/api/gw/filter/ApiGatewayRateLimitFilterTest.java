package com.nubons.nnp.api.gw.filter;

import com.nubons.nnp.api.gw.filter.ApiGatewayRateLimitFilter.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@DisplayName("UnitTestCode-ApiGatewayRateLimitFilter")
class ApiGatewayRateLimitFilterTest {

    private FakeClock clock;
    private GatewayFilterChain chain;

    private static String keyPath(String path) {
        return "path:" + path;
    }

    private static MockServerWebExchange exchange(String path) {
        MockServerHttpRequest req = MockServerHttpRequest.get("https://gw.test" + path).build();
        return MockServerWebExchange.from(req);
    }

    @BeforeEach
    void setup() {
        clock = new FakeClock();
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void burstEqualsReplenishRate_then_429() {
        ApiGatewayRateLimitFilter filter = newFilter();
        GatewayFilter gatewayFilter = filter.apply(config("2"));

        StepVerifier.create(gatewayFilter.filter(exchange("/x"), chain)).verifyComplete();
        StepVerifier.create(gatewayFilter.filter(exchange("/x"), chain)).verifyComplete();

        MockServerWebExchange overLimit = exchange("/x");
        StepVerifier.create(gatewayFilter.filter(overLimit, chain)).verifyComplete();
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, overLimit.getResponse().getStatusCode());
    }

    @Test
    void zeroReplenishRate_blocksEverything() {
        ApiGatewayRateLimitFilter filter = newFilter();
        GatewayFilter gatewayFilter = filter.apply(config("0"));

        MockServerWebExchange ex = exchange("/x");
        StepVerifier.create(gatewayFilter.filter(ex, chain)).verifyComplete();
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getResponse().getStatusCode());
    }

    @Test
    void refillOverTimeRestoresCapacity() {
        ApiGatewayRateLimitFilter filter = newFilter();
        assertTrue(consume(filter, "/a"));
        assertFalse(consume(filter, "/a"));

        clock.advanceSeconds();
        assertTrue(consume(filter, "/a"));
    }

    @Test
    void perKeyIsolation() {
        ApiGatewayRateLimitFilter filter = newFilter();
        assertTrue(consume(filter, "/a"));
        assertTrue(consume(filter, "/b"));
        assertFalse(consume(filter, "/a"));
        assertFalse(consume(filter, "/b"));
    }

    @Test
    void editedReplenishRateTakesEffectOnResync() {
        ApiGatewayRateLimitFilter filter = newFilter();
        assertTrue(consume(filter, "/a"));
        assertFalse(consume(filter, "/a"));

        filter.buckets.get(keyPath("/a")).resync(10, 10);
        clock.advanceSeconds();
        assertTrue(consume(filter, "/a"));
    }

    private ApiGatewayRateLimitFilter newFilter() {
        return new ApiGatewayRateLimitFilter(clock);
    }

    private RateLimitConfig config(String replenishRate) {
        RateLimitConfig cfg = new RateLimitConfig();
        cfg.setName("ApiGatewayRateLimitFilter");
        cfg.setOrder("ORDER_6");
        cfg.setReplenishRate(replenishRate);
        return cfg;
    }

    private boolean consume(ApiGatewayRateLimitFilter filter, String path) {
        return filter.buckets
                .computeIfAbsent(keyPath(path), k -> new ApiGatewayRateLimitFilter.TokenBucket(1, 1, clock))
                .tryConsume(1);
    }

    private static final class FakeClock extends AtomicLong implements LongSupplier {
        FakeClock() {
            super(System.nanoTime());
        }

        @Override
        public long getAsLong() {
            return get();
        }

        void advanceSeconds() {
            addAndGet((long) ((double) 1 * 1_000_000_000L));
        }
    }
}
