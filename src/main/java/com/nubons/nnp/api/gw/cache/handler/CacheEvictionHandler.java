package com.nubons.nnp.api.gw.cache.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.nubons.nnp.api.gw.cache.service.intf.IAllowedApisForUserCacheService;
import com.nubons.nnp.api.gw.cache.service.intf.IApiRegistryPlanUserCacheService;
import com.nubons.nnp.api.gw.cache.service.intf.IApiRegistryProviderCacheService;
import com.nubons.nnp.api.gw.cache.service.intf.IApiRouteCacheService;
import com.nubons.nnp.api.gw.cache.service.intf.IApiUserCacheService;

import reactor.core.publisher.Mono;

@Component
public class CacheEvictionHandler {
	
	@Autowired
	private IApiRouteCacheService routeCacheService;
	
	@Autowired
	private IApiUserCacheService userCacheService;
	
	@Autowired
	private IApiRegistryPlanUserCacheService registryPlanUserCacheService;
	
	@Autowired
	private IAllowedApisForUserCacheService allowedApisForUserCacheService; 
	
	@Autowired
	private IApiRegistryProviderCacheService registryProviderCacheService;
	
	public Mono<ServerResponse> evictRouteCache(ServerRequest request){
		routeCacheService.evictCacheRoute();
		return Mono.empty();
	}
	public Mono<ServerResponse> evictUserCache(ServerRequest request){
		userCacheService.evictCacheUser();
		return Mono.empty();
	}
	
	public Mono<ServerResponse> evictRegistryPlanUserCache(ServerRequest request){
		registryPlanUserCacheService.evictCacheRegistryPlanUser();
		return Mono.empty();
	}
	
	public Mono<ServerResponse> evictAllowedApisForUserCahce(ServerRequest request){
		allowedApisForUserCacheService.evictCacheAllowedApisForUserMap();
		return Mono.empty();
	}
	
	public Mono<ServerResponse> evictProviderIdByApiIdCache(ServerRequest request){
		registryProviderCacheService.evictProviderIdByApiIdCache();
		return Mono.empty();
	}

}
