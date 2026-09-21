package com.nubons.nnp.api.gw.exception;

import static com.nubons.nnp.api.abs.constants.APIConstants.ERROR_TO;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.ERR_CD_API_GW_001;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.ERR_CD_API_GW_002;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.ERR_CD_API_GW_003;

import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import com.nubons.nnp.api.abs.to.ApiErrorTO;

/**
 * 
 * @author Gourab Guha
 *
 */
@Component
public class GatewayFluxErrorAttributes extends DefaultErrorAttributes {

	@Override
	public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
		Map<String, Object> map = super.getErrorAttributes(request, options);

		Throwable t = getError(request);

		if (t instanceof AuthenticationException) {
			map.put(ERROR_TO,
					ApiErrorTO.builder().message(t.getLocalizedMessage()).errorCode(ERR_CD_API_GW_001).build());
		} else if (t instanceof AuthorizationException) {
			map.put(ERROR_TO,
					ApiErrorTO.builder().message(t.getLocalizedMessage()).errorCode(ERR_CD_API_GW_002).build());
		} else {
			map.put(ERROR_TO, ApiErrorTO.builder().message("Internal Error has occurred in API Gateway")
					.errorCode(ERR_CD_API_GW_003).build());
		}

		return map;
	}
}
