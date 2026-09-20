package com.nubons.nnp.api.gw.filter;

import java.util.List;
import java.util.Objects;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.nubons.nnp.api.abs.to.ApiFilterPredicateTO;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Gourab Guha
 *
 */
@Service
@Slf4j
public class ApiGatewayFilterFactory {

	@Value("${nnp.apiecosystem.service.url}")
	private String svcBaseUrl;

	// GenericApplicationContext instead of the concrete web context type: the
	// concrete class is not autowirable in AOT / native-image mode
	@Autowired
	private GenericApplicationContext context;

	@PostConstruct
	public void instantiateCustomFilters() {

		log.debug("svcBaseUrl = {}", svcBaseUrl);
		log.debug("context class is {}", context.getClass().getName());

		List<ApiFilterPredicateTO> toList = getActiveCustomFilters();

		if (Objects.nonNull(toList))
			instantiateActiveCustomFilters(toList);
	}

	private List<ApiFilterPredicateTO> getActiveCustomFilters() {

		return WebClient.builder().defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build()
				.get().uri(svcBaseUrl + "/filterPredicate?status=ACTIVE&custom=true&type=Filter").retrieve()
				.bodyToMono(new ParameterizedTypeReference<List<ApiFilterPredicateTO>>() {
				}).log().block();
	}

	private void instantiateActiveCustomFilters(List<ApiFilterPredicateTO> toList) {

		String pkgName = "com.nubons.nnp.api.gw.filter.";
		toList.forEach(to -> {

			String filterName = to.getName();
			String fullyQname = pkgName + filterName;
			Class<?> filterClazz = null;

			try {
				filterClazz = Class.forName(fullyQname);
			} catch (ClassNotFoundException e) {
				log.error("{} class is not found", fullyQname);
				throw new RuntimeException(fullyQname + " class is not found", e);
			}
			log.info("Filter {} loaded successfully", filterName);
			GenericBeanDefinition beanDefination = new GenericBeanDefinition();
			beanDefination.setBeanClass(filterClazz);

			context.registerBeanDefinition(filterName, beanDefination);
		});
	}
}
