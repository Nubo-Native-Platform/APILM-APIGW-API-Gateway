package com.nubons.nnp.api.gw.cache.service.intf;

import java.util.Map;
import java.util.Set;

public interface IAllowedApisForUserCacheService {
	Map<String, Set<String>> allowedApisByUserId();
	public void evictCacheAllowedApisForUserMap();
}
