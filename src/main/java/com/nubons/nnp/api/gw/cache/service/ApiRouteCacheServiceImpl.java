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

import com.nubons.nnp.api.abs.to.ApiRouteTO;
import com.nubons.nnp.api.gw.cache.service.intf.IApiRouteCacheService;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author Gourab Guha
 *
 */
@Service
@Slf4j
public class ApiRouteCacheServiceImpl extends AbstractCacheService implements IApiRouteCacheService {

	@Override
	@Cacheable(cacheNames = "routeByRouteId")
	public Map<String, ApiRouteTO> routeByRouteId() {
		
		log.info(LOG_STATEMENT_ENTERING);
		Map<String, ApiRouteTO> routeMapByRouteId = new HashMap<>();
		ApiRouteTO[] resp = restTemplate.getForObject(svcBaseUrl+"/route", ApiRouteTO[].class);
		if (Objects.nonNull(resp)) {
			List<ApiRouteTO> routeToList = Arrays.asList(resp);

			log.debug("Number of routes returned is {}", routeToList.size());
			log.debug("Content of the route list is {}", routeToList.toString());


			routeToList.forEach(to -> {
				routeMapByRouteId.put(to.getApiRouteId(), to);
			});
		} else {
			log.error("routeToList is returned null");
		}
		
		log.debug("Content of routeMapByRouteId is {}", routeMapByRouteId.toString());
		log.info(LOG_STATEMENT_EXITING);
		return routeMapByRouteId;
	}

	@Override
	@CacheEvict(cacheNames = "routeByRouteId",allEntries = true)
	public void evictCacheRoute() {
		log.info("Evicting routeByRouteId Cache");
	}
}
