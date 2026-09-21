package com.nubons.nnp.api.gw.filter;

import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.HEADER_AUTHORIZATION;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.HEADER_X_API_KEY;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.HEADER_X_ENV_CODE;
import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.USER_TO;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;

import com.nubons.nnp.api.abs.exception.BadRequestException;
import com.nubons.nnp.api.abs.to.ApiUserTO;
import com.nubons.nnp.api.gw.cache.service.intf.IApiUserCacheService;
import com.nubons.nnp.api.gw.exception.AuthenticationException;
import com.nubons.nnp.api.gw.util.Base64Util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class ApiAuthenticationFilter extends AbstractCustomFilter {

	@Autowired
	private IApiUserCacheService userCacheSvc;

	@Autowired
	protected RedisTemplate<String, ApiUserTO> redisTemplateWithUserTo;

	private ValueOperations<String, ApiUserTO> valueOp;

	//@SuppressWarnings("unchecked")
	@PostConstruct
	public void init() {
//		redisTemplate.setKeySerializer(new StringRedisSerializer());
//		redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//		redisTemplate.afterPropertiesSet();
		valueOp = redisTemplateWithUserTo.opsForValue();
	}

	@Override
	public GatewayFilter apply(Config config) {
		log.debug("Execution order of ApiAuthenticationFilter is {}", String.valueOf(config.getOrder()));
		return new OrderedGatewayFilter((exchange, chain) -> {
			log.info("Entering Pre-Processing phase of ApiAuthenticationFilter <<<");
			HttpHeaders headers = exchange.getRequest().getHeaders();
			String envCode;
			if(headers.get(HEADER_X_ENV_CODE) != null) {
				envCode = headers.get(HEADER_X_ENV_CODE).get(0);
			} else {
				throw new BadRequestException("Missing Env Code");
			}

			if (headers.containsKey(HEADER_X_API_KEY) && !headers.get(HEADER_X_API_KEY).isEmpty()) {
				String authorization = headers.get(HEADER_X_API_KEY).get(0);
				authorization = Base64Util.decode(authorization);

				if (userCacheSvc.userByKeyAndSecret().containsKey(authorization)) {
					ApiUserTO userTo = userCacheSvc.userByKeyAndSecret().get(authorization);
					log.info("user env code: "+ userTo.getEnvCode() + " request env code: envCode");
					if (!envCode.equals(userTo.getEnvCode())) {
						throw new AuthenticationException("User authentication failed");
					}
					exchange.getAttributes().put(USER_TO, userTo);
					log.info("User authentication is successfull");
				} else {
					log.warn("User authentication failed");
					throw new AuthenticationException("Authentication failure");
				}

			} else if (headers.containsKey(HEADER_AUTHORIZATION) && !headers.get(HEADER_AUTHORIZATION).isEmpty()) {
				log.info("bearer authentication >>>>>>");
				String authorization = headers.get(HEADER_AUTHORIZATION).get(0);
				if (authorization != null && authorization.startsWith("Bearer")) {
					String token = authorization.substring(7);
					ApiUserTO userTo = /* (List<ApiUserTO>) */ valueOp.get(token);
					if (Objects.nonNull(userTo)) {
						if (!envCode.equals(userTo.getEnvCode())) {
							throw new AuthenticationException("User authentication failed");
						}
						exchange.getAttributes().put(USER_TO, userTo);
						log.info("User authentication is successfull");
					} else {
						log.warn("User authentication failed");
						throw new AuthenticationException("Authentication failure");
					}

				} else {
					log.warn("User authentication failed");
					throw new AuthenticationException("Authentication failure");
				}

			} else {
				log.error("Authorization header is missing");
				throw new AuthenticationException("Authorization header is missing");
			}

			log.info("Exiting Pre Processing phase of ApiAuthenticationFilter >>>");
			return chain.filter(exchange).doFinally(a -> {
			}).then(Mono.fromRunnable(() -> {
			}));
		}, GwFilterOrderingEnum.valueOf(config.getOrder()).getOrder());
	}
}
