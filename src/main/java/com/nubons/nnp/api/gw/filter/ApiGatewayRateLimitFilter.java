package com.nubons.nnp.api.gw.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;

@Slf4j
public class ApiGatewayRateLimitFilter extends AbstractCustomFilter {

    final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private final LongSupplier nanoClock;

    public ApiGatewayRateLimitFilter() {
        this(System::nanoTime);
    }

    ApiGatewayRateLimitFilter(LongSupplier nanoClock) {
        super(RateLimitConfig.class);
        this.nanoClock = nanoClock;
    }


    private static String bucketKey(ServerWebExchange exchange) {
        return "path:" + exchange.getRequest().getPath();
    }

    private static long parsePositive(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            long value = Long.parseLong(raw.trim());
            return value > 0 ? value : 0;
        } catch (NumberFormatException ex) {
            log.warn("{} is not a positive integer ({}); treating as 0", "replenishRate", raw);
            return 0;
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        RateLimitConfig rlConfig = (RateLimitConfig) config;
        long replenishRate = parsePositive(rlConfig.getReplenishRate());
        long burstCapacity = replenishRate;
        long requestedTokens = 1;

        return new OrderedGatewayFilter((exchange, chain) -> {

            String bucketKey = bucketKey(exchange);
            TokenBucket bucket = buckets.computeIfAbsent(bucketKey,
                    k -> new TokenBucket(burstCapacity, replenishRate, nanoClock));
            bucket.resync(burstCapacity, replenishRate);

            if (bucket.tryConsume(requestedTokens)) {
                return chain.filter(exchange);
            }

            log.debug("Rate limit exceeded for key {}; returning 429", bucketKey);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();

        }, GwFilterOrderingEnum.valueOf(config.getOrder()).getOrder());
    }


    @Getter
    @Setter
    @ToString
    public static class RateLimitConfig extends Config {
        private String replenishRate;
    }


    static final class TokenBucket {
        private final LongSupplier nanoClock;
        private long capacity;
        private long refillRatePerSec;
        private double availableTokens;
        private long lastRefillNanos;

        TokenBucket(long capacity, long refillRatePerSec, LongSupplier nanoClock) {
            this.capacity = capacity;
            this.refillRatePerSec = refillRatePerSec;
            this.availableTokens = capacity;
            this.nanoClock = nanoClock;
            this.lastRefillNanos = nanoClock.getAsLong();
        }

        void resync(long capacity, long refillRatePerSec) {
            this.capacity = capacity;
            this.refillRatePerSec = refillRatePerSec;
        }

        synchronized boolean tryConsume(long tokens) {
            refill();
            if (tokens <= 0 || tokens > availableTokens) {
                return false;
            }
            availableTokens -= tokens;
            return true;
        }

        private void refill() {
            long now = nanoClock.getAsLong();
            double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
            if (elapsedSeconds > 0) {
                availableTokens = Math.min(capacity, availableTokens + elapsedSeconds * refillRatePerSec);
                lastRefillNanos = now;
            }
        }
    }

}
