package com.nubons.nnp.api.gw.cache.service.intf;

import java.util.Map;

import com.nubons.nnp.api.abs.to.ApiUserTO;

/**
 * 
 * @author Gourab Guha
 *
 */
public interface IApiUserCacheService {
	
	public Map<String, ApiUserTO> userByKeyAndSecret();
	public void evictCacheUser();
}
