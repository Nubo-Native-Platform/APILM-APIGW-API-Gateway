package com.nubons.nnp.api.gw.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RequestSizeGatewayFilterFactory;
import org.springframework.util.unit.DataSize;

@Slf4j
public class ApiGatewayRequestSizeFilter extends AbstractCustomFilter {

    static final int FILTER_ORDER = -10;

    public ApiGatewayRequestSizeFilter() {
        super(RequestSizeConfig.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        RequestSizeConfig cfg = (RequestSizeConfig) config;
        DataSize maxSize = DataSize.parse(cfg.getMaxSize());

        RequestSizeGatewayFilterFactory factory = new RequestSizeGatewayFilterFactory();
        RequestSizeGatewayFilterFactory.RequestSizeConfig springConfig = new RequestSizeGatewayFilterFactory.RequestSizeConfig();
        springConfig.setMaxSize(maxSize);

        GatewayFilter delegate = factory.apply(springConfig);

        return new OrderedGatewayFilter(delegate, FILTER_ORDER);
    }

    @Getter
    @Setter
    @ToString
    public static class RequestSizeConfig extends Config {
        private String maxSize;
    }
}
