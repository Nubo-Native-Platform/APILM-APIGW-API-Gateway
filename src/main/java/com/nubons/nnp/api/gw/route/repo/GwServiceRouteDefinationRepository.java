package com.nubons.nnp.api.gw.route.repo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.nubons.nnp.api.gw.service.client.to.ApiRouteTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 
 * @author Gourab Guha
 * @since Mar, 28 - 2020
 *
 */
@Component
@Slf4j
public class GwServiceRouteDefinationRepository implements RouteDefinitionRepository {

	@Value("${nnp.apiecosystem.service.url}")
	private String svcBaseUrl;

	@Autowired
	private ObjectMapper objectMapper;

	/*
	 * The getRouteDefinations method is getting called multiple times by the
	 * underlying library. The following Semaphore is required to safeguard this
	 * Route Repository loading the same Route definitions multiple times.
	 */
	@Autowired
	@Qualifier("singlePermitSemaphore")
	private Semaphore lock = null;

	private Flux<RouteDefinition> routeDefinations = null;

	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {

		try {
			log.debug("Trying to acquire the Semaphore ...");
			lock.acquire();
			log.debug("Semaphore is acquired");

			if (Objects.isNull(routeDefinations)) {
				log.info("Starting to fetch route definations <<<");
				routeDefinations = WebClient.builder()
						.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build().get()
						.uri(svcBaseUrl + "/route").retrieve()
						.bodyToMono(new ParameterizedTypeReference<List<ApiRouteTO>>() {
						}).log().flatMapMany(this::getRdFlux).cache();
				log.info("Route Definations are fetched >>>");
			}
			return routeDefinations;
		} catch (InterruptedException e) {
			log.error("Someway the current thread {} has been interrupted, causing aborting the route loading",
					Thread.currentThread().getName(), e);
			throw new RuntimeException("Someway the current thread " + Thread.currentThread().getName()
					+ " has been interrupted, causing aborting the route loading", e);
		} finally {
			log.debug("Semaphore is getting released ...");
			lock.release();
			log.debug("Semaphore has been released");
		}
	}

	@Override
	public Mono<Void> save(Mono<RouteDefinition> route) {
		throw new UnsupportedOperationException(
				"Saving / Updating / Creating a route to backend is not available from Gateway, use the UI insted");
	}

	@Override
	public Mono<Void> delete(Mono<String> routeId) {
		throw new UnsupportedOperationException(
				"Deleting a route from backend is not available from Gateway, use the UI insted");
	}

	private RouteDefinition getRd(ApiRouteTO routeTO) {
		RouteDefinition rd = new RouteDefinition();

		// Set ID
		rd.setId(routeTO.getApiRouteId());

		// Set URI
		if (Objects.isNull(routeTO.getUrl())) {
			log.error("Route {} (id: {}) has no URL, it will be skipped", routeTO.getName(), routeTO.getApiRouteId());
			return null;
		}
		try {
			rd.setUri(new URI(routeTO.getUrl()));
		} catch (URISyntaxException e) {
			log.error("Route {} (id: {}) has a malformed URL [{}], it will be skipped", routeTO.getName(),
					routeTO.getApiRouteId(), routeTO.getUrl(), e);
			return null;
		}

		// Set predicates
		JsonNode predicateJson = routeTO.getPredicate();
		if (Objects.nonNull(predicateJson)) {
			try {

				String predicateJsonAsString = predicateJson.toString();
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> predicateList = (List<Map<String, Object>>) objectMapper
						.readValue(predicateJsonAsString, List.class);
				List<PredicateDefinition> pdList = new ArrayList<PredicateDefinition>();
				predicateList.forEach(P -> {
					PredicateDefinition pd = new PredicateDefinition();
					if (P.containsKey("name")) {
						pd.setName((String) P.get("name"));
					}

					if (P.containsKey("args")) {
						@SuppressWarnings("unchecked")
						List<Map<String, String>> argsList = (List<Map<String, String>>) P.get("args");
						argsList.forEach(X -> {
							pd.getArgs().putAll(X);
						});
					}
					pdList.add(pd);
				});

				rd.setPredicates(pdList);

				} catch (JsonMappingException e) {
					log.error("Error while parsing predicates of route {} (id: {}), it will be skipped",
							routeTO.getName(), routeTO.getApiRouteId(), e);
					return null;
				} catch (JsonProcessingException e) {
					log.error("Error while parsing predicates of route {} (id: {}), it will be skipped",
							routeTO.getName(), routeTO.getApiRouteId(), e);
					return null;
				}
		}

		// Set filters
		JsonNode filterJson = routeTO.getFilter();
		if (Objects.nonNull(filterJson)) {
			try {

				String filterJsonAsString = filterJson.toString();
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> filterList = (List<Map<String, Object>>) objectMapper
						.readValue(filterJsonAsString, List.class);
				List<FilterDefinition> fdList = new ArrayList<FilterDefinition>();
				int[] index = new int[1];
				index[0] = 0;
				filterList.forEach(F -> {

					FilterDefinition fd = new FilterDefinition();
					if (F.containsKey("name")) {
						fd.setName((String) F.get("name"));
					}
					index[0] = index[0] + 1;
					fd.addArg("order", "ORDER_" + index[0]);

					if (F.containsKey("args")) {
						@SuppressWarnings("unchecked")
						List<Map<String, String>> argsList = (List<Map<String, String>>) F.get("args");
						argsList.forEach(X -> {
							fd.getArgs().putAll(X);
						});
					}
					fdList.add(fd);
				});

				rd.setFilters(fdList);

				} catch (JsonMappingException e) {
					log.error("Error while parsing filters of route {} (id: {}), it will be skipped", routeTO.getName(),
							routeTO.getApiRouteId(), e);
					return null;
				} catch (JsonProcessingException e) {
					log.error("Error while parsing filters of route {} (id: {}), it will be skipped", routeTO.getName(),
							routeTO.getApiRouteId(), e);
					return null;
				}
		}

		log.info("route {} has been added / refreshed", routeTO.getName());
		return rd;
	}

	public Flux<RouteDefinition> getRdFlux(List<ApiRouteTO> routeList) {
		return Flux.fromStream(routeList.stream().map(this::getRdSafely).filter(Objects::nonNull));
	}

	private RouteDefinition getRdSafely(ApiRouteTO routeTO) {
		try {
			return getRd(routeTO);
		} catch (RuntimeException e) {
			log.error("Route {} (id: {}) could not be loaded, it will be skipped", routeTO.getName(),
					routeTO.getApiRouteId(), e);
			return null;
		}
	}

	@Component
	public class GwStateModifier {

		public void resetGwState() {

			try {
				log.debug("Trying to acquire the Semaphore ...");
				lock.acquire();
				log.debug("Semaphore is acquired");

				if (Objects.nonNull(routeDefinations)) {
					routeDefinations = null;
					log.info("Gw State has been reset");
				} else {
					log.warn("Gw is already in NULL state, nothing to reset");
				}

			} catch (InterruptedException e) {
				log.error("Gw State could not be reset, Internal Semaphore could not be obtained", e);
				throw new RuntimeException("Gw State could not be reset, Internal Semaphore could not be obtained", e);
			} finally {
				log.debug("Semaphore is getting released ...");
				lock.release();
				log.debug("Semaphore has been released");
			}

		}
	}

}
