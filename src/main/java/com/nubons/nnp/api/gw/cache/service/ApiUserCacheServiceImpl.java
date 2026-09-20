package com.nubons.nnp.api.gw.cache.service;

import static com.nubons.nnp.api.abs.constants.APIConstants.LOG_STATEMENT_ENTERING;
import static com.nubons.nnp.api.abs.constants.APIConstants.LOG_STATEMENT_EXITING;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.nubons.nnp.api.abs.to.ApiUserTO;
import com.nubons.nnp.api.gw.cache.service.intf.IApiUserCacheService;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Gourab Guha
 *
 */
@Service
@Slf4j
public class ApiUserCacheServiceImpl extends AbstractCacheService implements IApiUserCacheService{
	
	
	@Override
	@Cacheable(cacheNames = "userByKeyAndSecret")
	public Map<String, ApiUserTO> userByKeyAndSecret() {
		
		log.info(LOG_STATEMENT_ENTERING);
		Map<String, ApiUserTO> userMapByKeyAndSecret = new HashMap<>();
		ApiUserTO[] resp = restTemplate.getForObject(svcBaseUrl+"/user", ApiUserTO[].class);
		if (Objects.nonNull(resp)) {
			List<ApiUserTO> userToList = Arrays.asList(resp);
			log.debug("Number of users returned is {}", userToList.size());
			userToList.forEach(to -> {
				if (Objects.nonNull(to.getApikey()) && Objects.nonNull(to.getApisecret()))
					userMapByKeyAndSecret.put(to.getApikey() + ":" + to.getApisecret(), to);
			});
		} else {
			log.error("userToList is returned null");
		}
		
		log.info(LOG_STATEMENT_EXITING);
		return userMapByKeyAndSecret;
	}

	@Override
	@CacheEvict(cacheNames = "userByKeyAndSecret",allEntries = true )
	public void evictCacheUser() {
		log.info("Evicting userByKeyAndSecret Cache");
	}

}
