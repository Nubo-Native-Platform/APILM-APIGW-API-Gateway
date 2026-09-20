package com.nubons.nnp.api.gw.filter;

import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.FIVE;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.FOUR;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.REQ_TIME;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.REQ_URL;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.USER_ID;

import java.net.URI;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.nubons.nnp.api.abs.to.ApiLogTO;
import com.nubons.nnp.api.gw.logging.ApiLogBuffer;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 
 * @author Gourab Guha
 *
 */
@Component
@ConditionalOnExpression(value = "${api.logging.enable:false}==true")
@Slf4j
public class LoggingGlobalFilter implements GlobalFilter, Ordered {
	
	@Autowired
	@Qualifier("dtFormat")
	private SimpleDateFormat dtFormat = null;

	@Value("${api.logging.enable.db:false}")
	private boolean dbLoggingEnabled;

	@Value("${api.logging.enable.console:true}")
	private boolean consoleLoggingEnabled;

	@Value("${api.logging.enable.log.optional.attributes:false}")
	private boolean logOptionalAttributes;

	@Value("${api.logging.enable.skip.optional.routes:true}")
	private boolean skipOptionalRoutes;

	@Autowired(required = false)
	private ApiLogBuffer logBuffer;

	@PostConstruct
	public void validate() {

		if ((!dbLoggingEnabled) && (!consoleLoggingEnabled)) {
			log.error(
					"Incorrect configuration of LoggingGlobalFilter detected, either DB Logging or Console logging or both has to be enabled");
			throw new IllegalArgumentException(
					"Incorrect configuration of LoggingGlobalFilter detected, either DB Logging or Console logging or both has to be enabled");
		}
		log.info("LoggingGlobalFilter has been validated");
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		if (skipOptionalRoutes && skip(exchange)) {
			return chain.filter(exchange);
		}
		executePrePhase(exchange);
		return chain.filter(exchange).doFinally(a -> {
			log(exchange);
		});
	}

	@Override
	public int getOrder() {
		int orderingSeq = GwFilterOrderingEnum.ORDER_1.getOrder() - 1;
		log.info("Ordering sequence of {} filter is {}", this.getClass().getName(), orderingSeq);
		return orderingSeq;
	}

	private void executePrePhase(ServerWebExchange exchange) {

		log.debug("Entering Pre-Processing phase of LoggingGlobalFilter <<<");

		// set requestTimestamp in exchange attribute
		Timestamp requestTs = new Timestamp(System.currentTimeMillis());
		exchange.getAttributes().put(REQ_TIME, dtFormat.format(requestTs));

		log.debug("Exiting Pre Processing phase of LoggingGlobalFilter >>>");
	}

	private void log(ServerWebExchange exchange) {

		log.debug("Entering Post-Processing phase of LoggingGlobalFilter <<<");

		// fetch userId
		/*
		 * Any if these filters are required to fetch this :
		 * InternalAuthenticationFilter and ApiAuthyenticationFilter else userId will be
		 * null
		 */
		String userId = (String) exchange.getAttributes().get(USER_ID);

		// fetch requestUrl
		String reqUri = exchange.getAttribute(REQ_URL);
		if (Objects.isNull(reqUri)) {
			URI requestUri = exchange.getRequest().getURI();
			if (Objects.nonNull(requestUri)) {
				reqUri = requestUri.toString();
			}
		}

		String routeId = null;
		String routeUrl = null;

		// Fetch routeId and routeUrl if logging optional is enabled
		if (logOptionalAttributes) {
			Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
			if (Objects.nonNull(route)) {
				routeId = route.getId();
			}

			URI routeUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
			if (Objects.nonNull(routeUri)) {
				routeUrl = routeUri.toString();
			}
		}

		// Fetch requestTs
		String requestTs = exchange.getAttribute(REQ_TIME);

		// Set respTs
		Timestamp respTs = new Timestamp(System.currentTimeMillis());
		String responseTs = dtFormat.format(respTs);

		// Set response status and status code
		Boolean status = null;
		String responseStatusCode = null;
		if (Objects.nonNull(exchange.getResponse().getStatusCode())) {
			responseStatusCode = String.valueOf(exchange.getResponse().getStatusCode().value());
			status = (responseStatusCode.startsWith(FOUR) || responseStatusCode.startsWith(FIVE)) ? false : true;
		}


		ApiLogTO logTo = ApiLogTO.builder().userId(userId).requestUrl(reqUri).routeId(routeId).routeUrl(routeUrl)
				.requestTs(requestTs).responseTs(responseTs).status(status).respStatusCode(responseStatusCode).build();

		if (consoleLoggingEnabled) {
			log.info("Api Log = {}", logTo);
		}

		if (dbLoggingEnabled) {
			logBuffer.add(logTo);
		}

		log.debug("Exiting Post-Processing phase of LoggingGlobalFilter >>>");
	}

	private boolean skip(ServerWebExchange exchange) {

		URI requestUri = exchange.getRequest().getURI();

		if (Objects.nonNull(requestUri)) {
			String reqUri = requestUri.toString();
			exchange.getAttributes().put(REQ_URL, reqUri);
			String lastPart = reqUri.substring(reqUri.lastIndexOf('/') + 1);
			return (Objects.nonNull(lastPart) && lastPart.contains(".")) ? true : false;
		}

		log.warn("Could not fetch the URl, hence skipping logging");
		return true;
	}

}
