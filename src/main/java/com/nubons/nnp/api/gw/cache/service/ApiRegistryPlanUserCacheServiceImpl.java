package com.nubons.nnp.api.gw.cache.service;

import static com.nubons.nnp.api.abs.constants.APIConstants.LOG_STATEMENT_ENTERING;
import static com.nubons.nnp.api.abs.constants.APIConstants.LOG_STATEMENT_EXITING;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.nubons.nnp.api.abs.to.ApiRegistryPlanUserTO;
import com.nubons.nnp.api.gw.cache.service.intf.IApiRegistryPlanUserCacheService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ApiRegistryPlanUserCacheServiceImpl extends AbstractCacheService implements IApiRegistryPlanUserCacheService {

	@Override
	@Cacheable(cacheNames = "registryPlanUserByUserId")
	public Map<String, ApiRegistryPlanUserTO> registryPlanUserByUserId() {
		log.info(LOG_STATEMENT_ENTERING);
		Map<String, ApiRegistryPlanUserTO> registryPlanUserMapByUserId = new HashMap<>();
		ApiRegistryPlanUserTO[] resp = restTemplate.getForObject(svcBaseUrl + "/regplanuser", ApiRegistryPlanUserTO[].class);
        if(resp != null) {
			List<ApiRegistryPlanUserTO> regPlanUserToList = Arrays.asList(resp);
			log.debug("Number of Registry Plan Users Returned is {}", regPlanUserToList.size());
			log.debug("Content of the Registry Plan User list is {}", regPlanUserToList.toString());
			regPlanUserToList.forEach(to -> {
				registryPlanUserMapByUserId.put(to.getUserid(), to);
			});
		} else {
			log.error("regPlanUserToList is returned null");
		}

		log.debug("Content of registryPlanUserMapByUserId is {}", registryPlanUserMapByUserId.toString());
		log.info(LOG_STATEMENT_EXITING);
		return registryPlanUserMapByUserId;
	}

	
	@Override
	@CacheEvict(cacheNames = "registryPlanUserByUserId",allEntries = true)
	public void evictCacheRegistryPlanUser() {
		log.info("Evicting registryPlanUserByUserId cache");
	}

}
