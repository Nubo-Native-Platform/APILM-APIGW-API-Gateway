package com.nubons.nnp.api.gw.filter;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.REQ_SIZE;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * This class is being used to keep track of Request Body length.
 * 
 * @author Gourab Guha
 *
 */
@Service
@Slf4j
public class RequestBodyHandlerFunction implements RewriteFunction<String, String> {

	@Override
	public Publisher<String> apply(ServerWebExchange exchange, String body) {
				
		if(Objects.nonNull(body)) {
			int sizeInBytes = body.getBytes().length;
			log.debug("Request size is {}", sizeInBytes);
			exchange.getAttributes().put(REQ_SIZE, (Integer)sizeInBytes);
		}else {
			log.debug("Request Body is comming null, considering req size 0");
			exchange.getAttributes().put(REQ_SIZE, 0);
			body = ""; // Setting body as empty string
		}
		
		return Mono.just(body);
	}

}
