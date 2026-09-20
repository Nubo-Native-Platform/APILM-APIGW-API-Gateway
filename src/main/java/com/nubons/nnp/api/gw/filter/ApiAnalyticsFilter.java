package com.nubons.nnp.api.gw.filter;

import static com.nubons.nnp.api.abs.constants.APIConstants.ID_PREFIX_API_TRANSACTION;
import static com.nubons.nnp.api.abs.constants.analytics.AnalyticsConstants.CREATE;
import static com.nubons.nnp.api.abs.constants.analytics.AnalyticsConstants.UPDATE;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.DATE_FORMAT;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.*;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;

import com.nubons.nnp.api.abs.to.ApiRouteTO;
import com.nubons.nnp.api.abs.to.ApiUserTO;
import com.nubons.nnp.api.abs.to.analytics.ApiAnalyticsRequestTO;
import com.nubons.nnp.api.abs.to.analytics.ApiAnalyticsResponseTO;
import com.nubons.nnp.api.abs.to.analytics.ApiErrorTO;
import com.nubons.nnp.api.abs.to.analytics.ApiTransactionTO;
import com.nubons.nnp.api.abs.util.UUIDGenerator;
import com.nubons.nnp.api.gw.cache.service.intf.IApiRegistryProviderCacheService;
import com.nubons.nnp.api.gw.cache.service.intf.IApiRouteCacheService;
import com.nubons.nnp.api.gw.cache.service.intf.IApiUserCacheService;
import com.nubons.nnp.api.gw.util.Base64Util;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 
 * @author Gourab Guha (egouguh)
 *
 * @since Nov 29, 2021
 */

/*
 * NOTE: This filter is assuming authentication filter is already present for
 * the current route
 */
@Slf4j
public class ApiAnalyticsFilter extends AbstractCustomFilter {

	//private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
	private static final String API_ANALYTICS_TRANS_ID = "API_ANALYTICS_TRANS_ID";
	private final SimpleDateFormat dtFormat = new SimpleDateFormat(DATE_FORMAT);

	@Value("${nnp.apiecosystem.analytics.service.url}")
	private String analyticsSvcBaseUrl;

	@Value("${nnp.apiecosystem.consumer.anonymous.id}")
	private String conAnoId;

	@Autowired
	private IApiRouteCacheService routeCacheSvc;

	@Autowired
	private IApiUserCacheService userCacheSvc;

	@Autowired
	private IApiRegistryProviderCacheService apiRegProvIdCacheSvc;

	@Autowired
	private RestTemplate restTemplate;

	@Override
	public GatewayFilter apply(Config config) {

		log.debug("Execution order of ApiAnalyticsFilter is {}", String.valueOf(config.getOrder()));
		return new OrderedGatewayFilter((exchange, chain) -> {
			executePrePhase(exchange);
			return chain.filter(exchange).doFinally(a -> {
				log.info("The signal received in finally of Analytics Filter is {}", a.name());
				String signal = a.name();
				if ("CANCEL".equals(signal)) {
					executePostPhase(exchange, true);
				} else if ("ON_ERROR".equals(signal)) {
					executePostPhase(exchange, false);
				}
			}).then(Mono.fromRunnable(() -> {
				executePostPhase(exchange, false);
			}));
		}, GwFilterOrderingEnum.valueOf(config.getOrder()).getOrder());
	}

	private void executePrePhase(ServerWebExchange exchange) {
		log.info("Entering Pre-Processing phase of ApiAnalyticsFilter <<<");
		ApiAnalyticsRequestTO reqTo = null;
		try {
			reqTo = createNewRequestTo(exchange);
		} catch (Exception ex) {
			log.error("Exception while creating Analytics Feed", ex);
		}

		if (reqTo != null) {
			sendFeed(reqTo, CREATE);
		}
		log.info("Exiting Pre Processing phase of ApiAnalyticsFilter >>>");
	}

	private void executePostPhase(ServerWebExchange exchange, boolean cancel) {
		log.info("Entering Post-Processing phase of ApiAnalyticsFilter <<<");
		ApiAnalyticsRequestTO reqTo = null;
		try {
			reqTo = createUpdateFeedTo(exchange, cancel);
		} catch (Exception ex) {
			log.error("Exception while Updating Analytics Feed", ex);
		}

		if (reqTo != null) {
			sendFeed(reqTo, UPDATE);
		}
		log.info("Exiting Post-Processing phase of ApiAnalyticsFilter >>>");
	}

	private ApiAnalyticsRequestTO createUpdateFeedTo(ServerWebExchange exchange, boolean cancel) {

		ApiAnalyticsRequestTO reqTo = (ApiAnalyticsRequestTO) exchange.getAttributes().get(API_ANALYTICS_FEED_TO);
		ApiTransactionTO transTo = reqTo.getApiTransaction();

		// Set Resp TS

		Timestamp respTs = new Timestamp(System.currentTimeMillis());
		transTo.setCallReturnTs(dtFormat.format(respTs));
		log.debug("APIAnalytics responseTimeStamp {}", respTs);

		// In case request-size could not be set during inward journey, then set it now
		if (transTo.getReqSize() < 0) {
			/*
			 * Note: During outward journey request size can be set if
			 * RequestBodyHandlerFilter is active for the current route
			 */
			Object reqSize = exchange.getAttribute(REQ_SIZE);
			if (Objects.nonNull(reqSize)) {
				transTo.setReqSize((Integer) reqSize);
			} else {
				log.debug(
						"{} is not set in Gateway's Exchange, possibly because RequestBodyHandlerFilter is not activated for the current route",
						REQ_SIZE);
				log.debug("Setting req size as 0 due to the above reason");
				transTo.setReqSize(0);
			}
		} else {
			log.debug("Req size was set during inward journey, hence not setting now");
		}

		// Set resp size
		Integer respSize = ((Long) exchange.getResponse().getHeaders().getContentLength()).intValue();
		transTo.setRespSize(respSize);
		if (respSize < 0) {
			/*
			 * Response size could not be found from content-length header, possibly because
			 * chunked encoding. It can be set if ResponseBodyHandlerFilter is active for
			 * the current route.
			 */
			Object objRespSize = exchange.getAttribute(RESP_SIZE);
			if (Objects.nonNull(objRespSize)) {
				transTo.setRespSize((Integer) objRespSize);
				log.debug(
						"Response size is being set from Exchange attribute {} as ResponseBodyHandlerFilter is active for the current route",
						RESP_SIZE);
			} else {
				log.debug(
						"Response size could not be set as content-legth response header is missing and ResponseBodyHandlerFilter is also not set");
			}
		} else {
			log.debug("Resp Size is set from response header's content-length");
		}
		log.debug("APIAnalytics responseSize{}", respSize);

		// Set apiStatus
		String apiStatus = exchange.getAttribute(API_STATUS);
		log.debug("ApiStatus : {}", apiStatus);
		if (Objects.nonNull(apiStatus)) {
			if (TRUE.equals(apiStatus)) {
				transTo.setSuccess(true);
			} else {
				transTo.setSuccess(false);

				ApiErrorTO errorTo = new ApiErrorTO();

				Timestamp errTs = new Timestamp(System.currentTimeMillis());
				errorTo.setErrorTs(dtFormat.format(errTs));

				// Determine error messageId from httpStatus
				String httpStatus = exchange.getAttribute(HTTP_STATUS);

				if (httpStatus !=null && httpStatus.startsWith(FOUR))
					errorTo.setErrorMsgId("7"); // Client Error
				else
					errorTo.setErrorMsgId("8"); // Server Error as http status must have started with 5**

				String errorMessage = exchange.getAttribute(RESP);
				errorTo.setErrorMsg(errorMessage);
				transTo.getApiErrors().add(errorTo);

			}
		}

		// Set Cache hit
		Boolean cacheHitFlag = exchange.getAttribute(CACHE_HIT_FLAG);
		if(Objects.nonNull(cacheHitFlag) && cacheHitFlag.booleanValue()) {
			transTo.setCacheHit(Boolean.TRUE);
		}else {
			transTo.setCacheHit(Boolean.FALSE);
		}

		if (cancel) {
			transTo.setSuccess(false);

			ApiErrorTO errorTo = new ApiErrorTO();

			Timestamp errTs = new Timestamp(System.currentTimeMillis());
			errorTo.setErrorTs(dtFormat.format(errTs));
			errorTo.setErrorMsgId("6"); // Unknown Reason
			errorTo.setErrorMsg("CANCEL Event is received in AnalyticsFilter");
			transTo.getApiErrors().add(errorTo);
		}

		return reqTo;
	}

	private ApiAnalyticsRequestTO createNewRequestTo(ServerWebExchange exchange) {

		ApiAnalyticsRequestTO reqTo = new ApiAnalyticsRequestTO();
		ApiTransactionTO transTo = new ApiTransactionTO();
		reqTo.setApiTransaction(transTo);

		// Set ID
		String id = UUIDGenerator.generateId(ID_PREFIX_API_TRANSACTION);
		transTo.setTransactionId(id);
		exchange.getAttributes().put(API_ANALYTICS_TRANS_ID, id);

		// Setting ID in header
		ServerHttpRequest request = exchange.getRequest().mutate().header(TRANSACTION_ID_HEADER, id).build();
		exchange = exchange.mutate().request(request).build();
		log.info("{} {} is created and set in request header", TRANSACTION_ID_HEADER, id);

		// Set apiid
		Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
		if (Objects.nonNull(route)) {
			String routeId = route.getId();
			log.info("Route ID is {}", routeId);
			ApiRouteTO routeTo = routeCacheSvc.routeByRouteId().get(routeId);
			if (Objects.nonNull(routeTo)){
				transTo.setEnvCode(routeTo.getEnvCode());
			}
			if (Objects.nonNull(routeTo) && Objects.nonNull(routeTo.getApiid())) {
				String apiId = routeTo.getApiid();
				transTo.setApiId(apiId);
				exchange.getAttributes().put(API_ID, apiId);
			} else {
				log.warn("ApiID could not be found against this routeId {}", routeId);
			}
		}

		// Set provId
		if (Objects.nonNull(transTo.getApiId())) {
			String provId = apiRegProvIdCacheSvc.providerByApiId().get(transTo.getApiId());
			transTo.setProvId(provId);
			log.info("ProvId = {}", provId);
		}

		// Set receive timestamp
		Timestamp receiveTs = new Timestamp(System.currentTimeMillis());
		transTo.setCallReceiveTs(dtFormat.format(receiveTs));
		log.debug("APIAnalytics receiveTimeStamp {}", receiveTs);

		// Set request size
		// Integer reqSize = ((Long)
		// exchange.getRequest().getHeaders().getContentLength()).intValue();
		Integer reqSize = (Integer) exchange.getAttribute(REQ_SIZE);
		transTo.setReqSize(reqSize);
		log.debug("APIAnalytics requestSize{}", reqSize);

		// Set conId
		String conId = null;
		ApiUserTO userTo = exchange.getAttribute(USER_TO);
		if (Objects.nonNull(userTo)) {
			log.debug("User was authenticated");
			transTo.setUserId(userTo.getUserid());
			conId = userTo.getConid();
		} else if (Objects.nonNull(exchange.getRequest().getHeaders().getFirst(AUTHORIZATION))) {
			log.debug("User was not authenticated but provides user authentication info");
			String enchodedAuth = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION);
			String decodedAuth = Base64Util.decode(enchodedAuth);
			userTo = userCacheSvc.userByKeyAndSecret().get(decodedAuth);
			if (Objects.isNull(userTo)) {
				log.warn("The given authentication info is incorrect, analytics will be captured for anonymous user");
				conId = conAnoId;
			}
		} else {
			conId = conAnoId;
		}

		transTo.setConId(conId);

		// Set RequestTo into exchange
		exchange.getAttributes().put(API_ANALYTICS_FEED_TO, reqTo);

		return reqTo;

	}

	private void sendFeed(ApiAnalyticsRequestTO reqTo, String command) {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<ApiAnalyticsRequestTO> request = new HttpEntity<ApiAnalyticsRequestTO>(reqTo, headers);
		ApiAnalyticsResponseTO responseTo = null;

		try {

			if (CREATE.equals(command)) {
				responseTo = restTemplate.postForObject(analyticsSvcBaseUrl + "/create", request,
						ApiAnalyticsResponseTO.class);
			} else {
				responseTo = restTemplate.patchForObject(analyticsSvcBaseUrl + "/update", request,
						ApiAnalyticsResponseTO.class);
			}

			log.info("Analytics Service returned {}", responseTo);

		} catch (Exception ex) {
			log.warn("Issue to publish message to Analytics Service, Analytics Feed is not getting saved", ex);
		}
	}

}
