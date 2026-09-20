package com.nubons.nnp.api.gw.filter;

import com.nubons.nnp.api.gw.cache.provider.CacheProvider;
import com.nubons.nnp.api.gw.util.Base64Util;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.CACHE_HIT_FLAG;

@Slf4j
public class ApiGatewayCacheFilter extends AbstractCustomFilter {

    private static final String CACHE_KEY_SEPARATOR = "#";
    @Autowired
    private CacheProvider cacheProvider;
    private ObjectMapper objectMapper;

    public ApiGatewayCacheFilter() {
        super(GwCacheConfig.class);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public GatewayFilter apply(Config config) {
        log.info("config: {}", config);
        return new OrderedGatewayFilter((exchange, chain) -> {

            exchange.getAttributes().put(CACHE_HIT_FLAG, Boolean.FALSE);

            boolean isCacheCheck = isCacheCheckRequired(exchange.getRequest());

            String cacheKey = null;
            long ttl = 0;

            if (isCacheCheck) {
                cacheKey = getCacheKey(exchange.getRequest(), config);
                ttl = getCacheTtl(config);
                log.info("Cache Key : {}", cacheKey);
                log.info("Cache TTL : {}", ttl);

                Object cachedValue = cacheProvider.get(cacheKey);
                if (Objects.nonNull(cachedValue)) {
                    log.info("Return cached response for request: {}", cacheKey);

                    CachedResponse cachedResponse;
                    try {
                        cachedResponse = objectMapper.readValue((String) cachedValue, CachedResponse.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error while json de-serializing to cache response", e);
                    }

                    if (Objects.nonNull(cachedResponse)) {
                        exchange.getAttributes().put(CACHE_HIT_FLAG, Boolean.TRUE);
                        final var serverHttpResponse = exchange.getResponse();
                        serverHttpResponse.setStatusCode(cachedResponse.httpStatus);
                        serverHttpResponse.getHeaders().addAll(cachedResponse.headers);
                        final var buffer = exchange.getResponse().bufferFactory().wrap(cachedResponse.body);
                        return exchange.getResponse().writeWith(Flux.just(buffer));
                    }
                }

            }

            log.info("Either cache miss or cache check false or cache ttl expired");
            // Either cache miss or cache check false or cache ttl expired
            final var mutatedHttpResponse = getServerHttpResponse(exchange, cacheProvider, cacheKey, ttl);
            return chain.filter(exchange.mutate().response(mutatedHttpResponse).build());

        }, GwFilterOrderingEnum.valueOf(config.getOrder()).getOrder());
    }

    private ServerHttpResponse getServerHttpResponse(ServerWebExchange exchange, CacheProvider cacheProvider, String cacheKey, long ttl) {
        final var originalResponse = exchange.getResponse();
        final var dataBufferFactory = originalResponse.bufferFactory();

        return new ServerHttpResponseDecorator(originalResponse) {

            @NonNull
            @Override
            public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
                log.debug("body instanceof Flux => {}", body instanceof Flux);
                if (body instanceof Flux) {
                    final var flux = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(flux.buffer().map(dataBuffers -> {
                        final var outputStream = new ByteArrayOutputStream();
                        dataBuffers.forEach(dataBuffer -> {
                            final var responseContent = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(responseContent);
                            try {
                                outputStream.write(responseContent);
                            } catch (IOException e) {
                                throw new RuntimeException("Error while reading response stream", e);
                            }
                        });
                        if (Objects.requireNonNull(getStatusCode()).is2xxSuccessful() && isCacheCheckRequired(exchange.getRequest())) {
                            final var cachedResponse = new CachedResponse(getStatusCode(), getHeaders(), outputStream.toByteArray());
//                            log.info("Request {} Cached response {}", cacheKey, new String(cachedResponse.getBody(), UTF_8));
                            try {
                                String valueAsString = objectMapper.writeValueAsString(cachedResponse);
                                log.debug("valueAsString = {}", valueAsString);
                                cacheProvider.put(cacheKey, valueAsString, ttl);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException("Error while serializing response to json before cache put", e);
                            }
                        }
                        return dataBufferFactory.wrap(outputStream.toByteArray());
                    }));
                }
                return super.writeWith(body);
            }
        };
    }

    private String getCacheKey(ServerHttpRequest request, Config config) {
        GwCacheConfig gwCacheConfig = (GwCacheConfig) config;

        // Forming cache key
        String retEncodedCacheKey = null;
        StringBuilder cacheKey = new StringBuilder();

        // default path
        cacheKey.append(request.getPath().value());

        // If config queryParam is set
        String queryParam = gwCacheConfig.getQueryParam();
        if (Boolean.valueOf(queryParam)) {
            request.getQueryParams().forEach((name, values) -> {
                log.info("name = {}, values = {}", name, values);
                cacheKey.append(CACHE_KEY_SEPARATOR).append(name).append("=").append(values);
            });
        }

        log.info("cacheKey before base64 encoding: {}", cacheKey);
        retEncodedCacheKey = Base64Util.encode(cacheKey.toString());

        return retEncodedCacheKey;
    }

    private long getCacheTtl(Config config) {
        GwCacheConfig gwCacheConfig = (GwCacheConfig) config;
        long ttl = 0;
        try {
            ttl = Long.parseLong(gwCacheConfig.getTtl());
        } catch (NumberFormatException nfe) {
            log.error("Error while parsing TTL value from filter config");
        }
        return ttl;
    }

    private boolean isCacheCheckRequired(ServerHttpRequest request) {

        boolean isCacheCheck = true;
        if (request.getMethod() != HttpMethod.GET) {
            isCacheCheck = false;
        }

        String cacheControls = request.getHeaders().getCacheControl();
        log.info("cache-control : {}", request.getHeaders().getCacheControl());
        if (Objects.nonNull(cacheControls) && (cacheControls.contains("no-cache") || cacheControls.contains("no-store"))) {
            isCacheCheck = false;
        }

        String pragma = request.getHeaders().getPragma();
        log.info("pragma : {}", pragma);
        if (Objects.nonNull(pragma) && pragma.contains("no-cache")) {
            isCacheCheck = false;
        }

        log.info("isCacheCheck: {}", isCacheCheck);

        return isCacheCheck;
    }

    @Getter @Setter @ToString
    public static class GwCacheConfig extends Config {
//        private String keyFields;
        private String queryParam;
        private String ttl;
    }


    @Value
    @Builder
    @ToString
    private static class CachedRequest {
        RequestPath path;
        HttpMethod method;
        MultiValueMap<String, String> queryParams;

    }

//    @Value
//    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class CachedResponse {
        HttpStatusCode httpStatus;
        HttpHeaders headers;
        byte[] body;
    }

}
