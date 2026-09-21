package com.nubons.nnp.api.gw.cache.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

/**
 * 
 * @author Gourab Guha
 *
 */
public abstract class AbstractCacheService {
	
	@Value("${nnp.apiecosystem.service.url}")
	protected String svcBaseUrl;
	
	@Autowired
	protected RestTemplate restTemplate;
	
}
