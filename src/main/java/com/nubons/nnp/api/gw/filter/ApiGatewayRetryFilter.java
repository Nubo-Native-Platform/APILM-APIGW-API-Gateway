package com.nubons.nnp.api.gw.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ApiGatewayRetryFilter extends AbstractCustomFilter {

    public ApiGatewayRetryFilter() {
        super(RetryConfig.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        RetryConfig cfg = (RetryConfig) config;

        int retries = parseRetries(cfg.getRetries());
        HttpStatus.Series series = parseSeries(cfg.getSeries());
        List<HttpMethod> methods = parseMethods(cfg.getMethods());

        RetryGatewayFilterFactory factory = new RetryGatewayFilterFactory();
        RetryGatewayFilterFactory.RetryConfig springConfig = new RetryGatewayFilterFactory.RetryConfig()
                .setRetries(retries)
                .setSeries(series)
                .setMethods(methods.toArray(new HttpMethod[0]));

        GatewayFilter delegate = factory.apply(springConfig);

        return new OrderedGatewayFilter(delegate,
                GwFilterOrderingEnum.valueOf(config.getOrder()).getOrder());
    }

    private static int parseRetries(String raw) {
        if (raw == null || raw.isBlank()) {
            return 3;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            return (v >= 1 && v <= 10) ? v : 3;
        } catch (NumberFormatException ex) {
            return 3;
        }
    }

    private static HttpStatus.Series parseSeries(String raw) {
        if (raw == null || raw.isBlank()) {
            return HttpStatus.Series.SERVER_ERROR;
        }
        try {
            return HttpStatus.Series.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return HttpStatus.Series.SERVER_ERROR;
        }
    }

    private static List<HttpMethod> parseMethods(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of(HttpMethod.GET);
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> HttpMethod.valueOf(s.toUpperCase()))
                .collect(Collectors.toList());
    }

    @Getter
    @Setter
    @ToString
    public static class RetryConfig extends Config {
        private String retries;
        private String series;
        private String methods;
    }
}
