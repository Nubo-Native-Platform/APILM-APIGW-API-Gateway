package com.nubons.nnp.api.gw.cache.service.intf;

import java.util.Map;

import com.nubons.nnp.api.abs.to.ApiRegistryPlanUserTO;

public interface IApiRegistryPlanUserCacheService {

	public Map<String, ApiRegistryPlanUserTO> registryPlanUserByUserId();

	public void evictCacheRegistryPlanUser();
}
