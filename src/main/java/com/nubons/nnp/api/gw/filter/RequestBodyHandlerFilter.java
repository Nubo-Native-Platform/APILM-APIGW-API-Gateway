package com.nubons.nnp.api.gw.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Gourab Guha
 *
 */
@Slf4j
public class RequestBodyHandlerFilter extends AbstractCustomFilter {

	@Autowired
	private RequestBodyHandlerFunction requestBodyHandler;

	@Override
	public GatewayFilter apply(Config config) {

		log.debug("Execution order of RequestBodyHandlerFilter is {}", String.valueOf(config.getOrder()));
		return new OrderedGatewayFilter((exchange, chain) -> {
			log.info("Entering Pre-Processing phase of RequestBodyHandlerFilter <<<");
			ModifyRequestBodyGatewayFilterFactory.Config modifyRequestConfig = new ModifyRequestBodyGatewayFilterFactory.Config()
					.setInClass(String.class).setOutClass(String.class).setRewriteFunction(requestBodyHandler);
			log.info("Exiting Pre Processing phase of RequestBodyHandlerFilter >>>");
			return new ModifyRequestBodyGatewayFilterFactory().apply(modifyRequestConfig).filter(exchange, chain);
		}, GwFilterOrderingEnum.valueOf(config.getOrder()).getOrder());
	}
}
