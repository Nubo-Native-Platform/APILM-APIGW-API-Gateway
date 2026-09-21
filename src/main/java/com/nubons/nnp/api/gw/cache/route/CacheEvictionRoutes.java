package com.nubons.nnp.api.gw.cache.route;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.nubons.nnp.api.gw.cache.handler.CacheEvictionHandler;

@Configuration(proxyBeanMethods = false)
public class CacheEvictionRoutes {
	
	@Bean
	public RouterFunction<ServerResponse> cachingRouteFunction(CacheEvictionHandler handler){
		return RouterFunctions.route(GET("/evictRouteCache"), handler::evictRouteCache)
				.andRoute(GET("/evictUserCache"), handler::evictUserCache)
				.andRoute(GET("/evictRegistryPlanUserCache"), handler::evictRegistryPlanUserCache)
				.andRoute(GET("/evictAllowedApisForUserCache"),handler::evictAllowedApisForUserCahce)
				.andRoute(GET("/evictProviderIdByApiIdCache"),handler::evictProviderIdByApiIdCache);
	}
	
}
