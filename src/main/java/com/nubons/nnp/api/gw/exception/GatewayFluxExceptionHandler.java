package com.nubons.nnp.api.gw.exception;

import static com.nubons.nnp.api.abs.constants.APIConstants.ERROR_TO;

import java.util.Map;

import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 
 * @author Gourab Guha
 *
 */
@Component
@Slf4j
public class GatewayFluxExceptionHandler extends AbstractErrorWebExceptionHandler {

	public GatewayFluxExceptionHandler(GatewayFluxErrorAttributes errorAttributes, Resources resources,
			ApplicationContext applicationContext, ServerCodecConfigurer serverCodecConfigurer) {
		super(errorAttributes, resources, applicationContext);
		super.setMessageWriters(serverCodecConfigurer.getWriters());
		super.setMessageReaders(serverCodecConfigurer.getReaders());
	}

	@Override
	protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
		return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
	}

	private Mono<ServerResponse> renderErrorResponse(final ServerRequest request) {
		ErrorAttributeOptions options = ErrorAttributeOptions.defaults()
				.including(ErrorAttributeOptions.Include.MESSAGE).including(ErrorAttributeOptions.Include.STACK_TRACE);
		final Map<String, Object> errorPropertiesMap = getErrorAttributes(request, options);

		Mono<ServerResponse> serverResponseMono = null;
		Throwable t = getError(request);

		if (t instanceof AuthenticationException) {
			log.error("AuthenticationException occured > {}", request.requestPath(), t);
			serverResponseMono = ServerResponse.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
					.body(BodyInserters.fromValue(errorPropertiesMap.get(ERROR_TO)));
		} else if (t instanceof AuthorizationException) {
			log.error("AuthorizationException occured > {}", request.requestPath(), t);
			serverResponseMono = ServerResponse.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
					.body(BodyInserters.fromValue(errorPropertiesMap.get(ERROR_TO)));
		} else {
			log.error("Exception occured > {}", request.requestPath(), t);
			serverResponseMono = ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.contentType(MediaType.APPLICATION_JSON)
					.body(BodyInserters.fromValue(errorPropertiesMap.get(ERROR_TO)));
		}
		return serverResponseMono;
	}
}
