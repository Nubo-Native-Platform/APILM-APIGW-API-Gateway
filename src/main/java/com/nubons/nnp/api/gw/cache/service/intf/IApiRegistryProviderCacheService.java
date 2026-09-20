package com.nubons.nnp.api.gw.cache.service.intf;

import java.util.Map;

/**
 * 
 * @author Gourab Guha
 *
 */
public interface IApiRegistryProviderCacheService {
	
	public Map<String, String> providerByApiId();
	
	public void evictProviderIdByApiIdCache();
	
}
