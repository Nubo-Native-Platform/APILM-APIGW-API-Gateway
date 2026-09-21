package com.nubons.nnp.api.gw.filter;

import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.API_ID;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.USER_TO;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.TRANSACTION_ID_HEADER;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Set;

import com.nubons.nnp.api.abs.to.ApiRouteTO;
import com.nubons.nnp.api.gw.cache.service.intf.IApiRouteCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;

import com.nubons.nnp.api.abs.to.ApiUserTO;
import com.nubons.nnp.api.abs.to.analytics.ApiAnalyticsRequestTO;
import com.nubons.nnp.api.abs.to.analytics.ApiAnalyticsResponseTO;
import com.nubons.nnp.api.abs.to.analytics.ApiErrorTO;
import com.nubons.nnp.api.abs.to.analytics.ApiTransactionTO;
import com.nubons.nnp.api.gw.cache.service.intf.IAllowedApisForUserCacheService;
import com.nubons.nnp.api.gw.exception.AuthenticationException;
import com.nubons.nnp.api.gw.exception.AuthorizationException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class ApiAuthorizationFilter extends AbstractCustomFilter {

	@Value("${nnp.apiecosystem.analytics.service.url}")
	private String analyticsSvcBaseUrl;

	@Autowired
	private IApiRouteCacheService routeCacheSvc;

	@Autowired
	private IAllowedApisForUserCacheService allowedApisForUserCacheSvc;

	@Autowired
	private RestTemplate restTemplate;

	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
	private final SimpleDateFormat dtFormat = new SimpleDateFormat(DATE_FORMAT);

	@Override
	public GatewayFilter apply(Config config) {
		log.debug("Execution order of ApiAuthorizationFilter is {}", String.valueOf(config.getOrder()));
		return new OrderedGatewayFilter((exchange, chain) -> {

			try {
				log.info("Entering Pre-Processing phase of ApiAuthorizationFilter <<<");
				ApiUserTO userTo = (ApiUserTO) exchange.getAttributes().get(USER_TO);
				log.debug("userId {}", userTo.getUserid());
				String apiId = exchange.getAttribute(API_ID);
				if(apiId == null) {
					log.info("api id not found in exchange");
					Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
					if (Objects.nonNull(route)) {
						String routeId = route.getId();
						log.info("Route ID is {}", routeId);
						ApiRouteTO routeTo = routeCacheSvc.routeByRouteId().get(routeId);
						if (Objects.nonNull(routeTo) && Objects.nonNull(routeTo.getApiid())) {
							apiId = routeTo.getApiid();
						} else {
							log.warn("ApiID could not be found against this routeId {}", routeId);
						}
					}
				}
				log.debug("apiId {}", apiId);
				if (Objects.nonNull(userTo) && apiId!=null) {
					if (allowedApisForUserCacheSvc.allowedApisByUserId().containsKey(userTo.getUserid())) {
						Set<String> apiIdSet = allowedApisForUserCacheSvc.allowedApisByUserId().get(userTo.getUserid());
						if (apiIdSet.contains(apiId)) {
							log.info("User authorization successfull");
						} else {
							log.warn("User authorization failed");
							throw new AuthorizationException("Authorization failure");
						}
					} else {
						log.warn("User authorization failed");
						throw new AuthorizationException("Authorization failure");
					}
				} else {
					log.warn("User authentication failed");
					throw new AuthenticationException("Authentication failure");
				}
				return chain.filter(exchange).doFinally(a -> {
				}).then(Mono.fromRunnable(() -> {
				}));
			} catch (AuthenticationException ex1) {
				// No logging needed
				throw ex1;
			} catch (AuthorizationException ex2) {
				// No logging needed
				sendAuthorizationErrorToAnalytics(exchange);
				throw ex2;
			} catch (Exception ex) {
				log.warn("User authorization failed", ex);
				sendAuthorizationErrorToAnalytics(exchange);
				throw new AuthorizationException("Authorization failure");
			} finally {
				log.info("Exiting Pre Processing phase of ApiAuthorizationFilter >>>");
			}

		}, GwFilterOrderingEnum.valueOf(config.getOrder()).getOrder());
	}

	private void sendAuthorizationErrorToAnalytics(ServerWebExchange exchange) {
		log.info("Start sending analytics feed because of AuthorizationException <<<");
		HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
		if (Objects.nonNull(httpHeaders) && Objects.nonNull(httpHeaders.getFirst(TRANSACTION_ID_HEADER))) {
			String transactionId = httpHeaders.getFirst(TRANSACTION_ID_HEADER);
			log.info("TransactionId = {}", transactionId);

			ApiAnalyticsRequestTO reqTo = new ApiAnalyticsRequestTO();
			ApiTransactionTO transTo = new ApiTransactionTO();
			transTo.setTransactionId(transactionId);
			reqTo.setApiTransaction(transTo);

			ApiErrorTO errorTo = new ApiErrorTO();
			Timestamp errTs = new Timestamp(System.currentTimeMillis());
			errorTo.setErrorTs(dtFormat.format(errTs));
			errorTo.setErrorMsgId("7");
			errorTo.setErrorMsg("Authorization Failure");
			transTo.getApiErrors().add(errorTo);

			ApiAnalyticsResponseTO responseTo = restTemplate.patchForObject(analyticsSvcBaseUrl + "/update", reqTo,
					ApiAnalyticsResponseTO.class);
			log.info("ApiAnalyticsResponseTO {}", responseTo);

			exchange.getResponse().getHeaders().add("api_status", Boolean.FALSE.toString());
			
			log.info("Analytics Feed sending is done >>>");
		} else {
			log.error("Required HttpHeader(s) is / are missing in Exchange, can't send Analytics Feed >>>");
		}
	}

}
