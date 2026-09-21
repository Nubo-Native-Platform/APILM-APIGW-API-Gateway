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

import com.nubons.nnp.api.abs.to.ApiRegistryTO;
import com.nubons.nnp.api.gw.cache.service.intf.IApiRegistryProviderCacheService;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Gourab Guha
 *
 */
@Service
@Slf4j
public class ApiRegistryProviderCacheService extends AbstractCacheService implements IApiRegistryProviderCacheService {

	@Override
	@Cacheable(cacheNames = "providerIdByApiId")
	public Map<String, String> providerByApiId() {

		log.info(LOG_STATEMENT_ENTERING);
		Map<String, String> providerByApiIdMap = new HashMap<>();
		List<ApiRegistryTO> apiRigistryToList = Arrays
				.asList(restTemplate.getForObject(svcBaseUrl + "/registry", ApiRegistryTO[].class));
		
		if (Objects.nonNull(apiRigistryToList)) {
			log.debug("Number of APIs returned {}", apiRigistryToList.size());
			log.debug("Content of the API Registry list is {}", apiRigistryToList.toString());

			apiRigistryToList.forEach(to -> {
				providerByApiIdMap.put(to.getApiid(), to.getProvId());
			});
		} else {
			log.error("apiRigistryToList is returned null");
		}

		log.debug("Content of providerByApiIdMap is {}", providerByApiIdMap.toString());
		log.info(LOG_STATEMENT_EXITING);
		return providerByApiIdMap;
	}

	@Override
	@CacheEvict(cacheNames = "providerIdByApiId", allEntries = true)
	public void evictProviderIdByApiIdCache() {
		log.info("Evicting providerIdByApiId Cache");

	}

}
