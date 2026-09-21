package com.nubons.nnp.api.gw.cache.service.intf;

import java.util.Map;

import com.nubons.nnp.api.abs.to.ApiRouteTO;

/**
 * 
 * @author Gourab Guha
 *
 */
public interface IApiRouteCacheService {
	
	Map<String, ApiRouteTO> routeByRouteId();
	void evictCacheRoute();
}
