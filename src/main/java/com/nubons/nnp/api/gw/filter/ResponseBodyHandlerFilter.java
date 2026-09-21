package com.nubons.nnp.api.gw.filter;

import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.RESP_SIZE;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.API_STATUS;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.RESP;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.HTTP_STATUS;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.FOUR;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.FIVE;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 
 * @author Gourab Guha
 *
 */
@Slf4j
public class ResponseBodyHandlerFilter extends AbstractCustomFilter {

	@Autowired
	private ModifyResponseBodyGatewayFilterFactory modifyResponseBodyGatewayFilterFactory;

	@Override
	public GatewayFilter apply(Config config) {

		final ModifyResponseBodyGatewayFilterFactory.Config modifyResponseBodyFilterFactoryConfig = new ModifyResponseBodyGatewayFilterFactory.Config();
		modifyResponseBodyFilterFactoryConfig.setRewriteFunction(String.class, String.class,
				(exchange, bodyAsString) -> {
					
					// Set Resp Size
					if (Objects.nonNull(bodyAsString)) {
						log.debug("Resp size is = {}", (Integer) bodyAsString.getBytes().length);
						exchange.getAttributes().put(RESP_SIZE, (Integer) bodyAsString.getBytes().length);
					} else {
						log.warn("Response Body is comming null, hence can't set Resp Size");
					}

					// Set Resp Status and Error message (if required)
					if (Objects.nonNull(exchange.getResponse().getStatusCode())) {

						String httpStatus = String.valueOf(exchange.getResponse().getStatusCode().value());

						if (httpStatus.startsWith(FOUR) || httpStatus.startsWith(FIVE)) {
							log.info(
									"HttpStatus starts with 400 range or with 500 range is being treated as Failure, the HttpStatus came is {}",
									httpStatus);
							log.debug("HTTP Body will be trated as Error message, the HTTP body is {}", bodyAsString);
							exchange.getAttributes().put(API_STATUS, "false");
							exchange.getAttributes().put(RESP, bodyAsString);
							exchange.getAttributes().put(HTTP_STATUS, httpStatus);
						} else {
							exchange.getAttributes().put(API_STATUS, "true");
							log.debug("HttpStatus is {}, which is denoting success", httpStatus);
						}
					}
					return Mono.just(bodyAsString);
				});
		return modifyResponseBodyGatewayFilterFactory.apply(modifyResponseBodyFilterFactoryConfig);
	}

}
