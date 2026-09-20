package com.nubons.nnp.api.gw.route.refresher;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class RouteRefreshRoutes {

    @Bean
    public RouterFunction<ServerResponse> routeRefreshFunction(RouteRefreshHandler handler) {
        return RouterFunctions.route(GET("/refreshRoutes"), handler::refreshRoutes);
    }
}
