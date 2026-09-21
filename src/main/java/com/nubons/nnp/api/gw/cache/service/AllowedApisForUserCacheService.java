package com.nubons.nnp.api.gw.cache.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.nubons.nnp.api.gw.cache.service.intf.IAllowedApisForUserCacheService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AllowedApisForUserCacheService extends AbstractCacheService implements IAllowedApisForUserCacheService {

	@Override
	@Cacheable(cacheNames = "allowedApisByUserId")
	public Map<String, Set<String>> allowedApisByUserId() {

		Map<String, Set<String>> allowedApiIdsByUserIdMap = new HashMap<>();

//		try {

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> allowedApisForUserMapList = restTemplate
				.getForObject(svcBaseUrl + "/regplanuser/allowedUsersForApis", List.class);

		log.debug("allowedApisForUserMapList {}", allowedApisForUserMapList);

		if (Objects.nonNull(allowedApisForUserMapList)) {

			for (Map<String, Object> userIdToApiId : allowedApisForUserMapList) {

				String userId = (String) userIdToApiId.get("userid");
				String apiId = (String) userIdToApiId.get("apiid");

				if (Objects.nonNull(userId) && Objects.nonNull(apiId)) {

					Set<String> apiIdSet = allowedApiIdsByUserIdMap.get(userId);
					if (Objects.nonNull(apiIdSet)) {
						apiIdSet.add(apiId);
					} else {
						apiIdSet = new HashSet<>();
						apiIdSet.add(apiId);
						allowedApiIdsByUserIdMap.put(userId, apiIdSet);
					}
				}
			}

		}

		log.debug("allowedApiIdsByUserIdMap {}", allowedApiIdsByUserIdMap);

//		} catch (Exception ex) {
//			log.error("Exception caught", ex);
//		}
		return allowedApiIdsByUserIdMap;
	}

	@Override
	@CacheEvict(cacheNames = "allowedApisByUserId", allEntries = true)
	public void evictCacheAllowedApisForUserMap() {
		log.info("Evicting allowedApisByUserId cache");
	}

}
