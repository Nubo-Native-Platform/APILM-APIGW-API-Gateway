package com.nubons.nnp.api.gw.config;

import static com.nubons.nnp.api.gw.constants.ApiGatewayConstants.DATE_FORMAT;

import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.nubons.nnp.api.abs.to.ApiUserTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 
 * @author Gourab Guha
 * @since Mar, 28 - 2020
 *
 */
@Configuration
@Slf4j
public class ApiGatewayConfig {

	@Value("${nnp.apiecosystem.service.url}")
	private String svcBaseUrl;

	@Value("${nnp.domain:}")
	private String domain;

	@Autowired
	private LettuceConnectionFactory lettuceConnectionFactory;

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public RedisTemplate<String, ApiUserTO> redisTemplateWithUserTo() {

		final RedisTemplate<String, ApiUserTO> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(lettuceConnectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		redisTemplate.afterPropertiesSet();

		return redisTemplate;
	}

	@Bean
	public RestTemplate restTemplate() {

		RestTemplate restTemplate = new RestTemplate();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
//		requestFactory.setConnectTimeout(TIMEOUT);
//		requestFactory.setReadTimeout(TIMEOUT);

		restTemplate.setRequestFactory(requestFactory);
		return restTemplate;
	}

	@Bean
	public WebProperties.Resources resources() {
		return new WebProperties.Resources();
	}

/*	@Bean
	public JmsListenerContainerFactory<?> containerFactory(ConnectionFactory connectionFactory,
			DefaultJmsListenerContainerFactoryConfigurer configurer) {

		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		factory.setPubSubDomain(true);
		// This provides all boot's default to this factory, including the message
		// converter
		configurer.configure(factory, connectionFactory);
		// You could still override some of Boot's default if necessary.
		return factory;
	}

	@Bean // Serialize message content to json using TextMessage
	public MessageConverter jacksonJmsMessageConverter() {
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setTargetType(MessageType.TEXT);
		converter.setTypeIdPropertyName("_type");
		return converter;
	}*/

	@Bean(name = "webClientGwServ")
	public WebClient getWebClient() {
		return WebClient.builder().baseUrl(svcBaseUrl).filters(exFilerFunct -> {
			exFilerFunct.add(logReq());
			exFilerFunct.add(logResp());
		}).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();

	}

	ExchangeFilterFunction logReq() {
		return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
			if (log.isDebugEnabled()) {
				StringBuilder sb = new StringBuilder("Request: \n");
				sb.append("URL : " + clientRequest.url());
				sb.append("\n");
				sb.append("Method : " + clientRequest.method());
				sb.append("\n");
				sb.append("Headres: \n");
				clientRequest.headers()
						.forEach((name, values) -> values.forEach(value -> sb.append(name + " : " + value)));
				sb.append("Cookies: \n");
				clientRequest.cookies()
						.forEach((name, values) -> values.forEach(value -> sb.append(name + " : " + value)));
				sb.append("\n");
				log.debug(sb.toString());
			}
			return Mono.just(clientRequest);
		});
	}

	ExchangeFilterFunction logResp() {
		return ExchangeFilterFunction.ofResponseProcessor(clientResp -> {
			if (log.isDebugEnabled()) {
				StringBuilder sb = new StringBuilder("Response: \n");
				sb.append(clientResp.rawStatusCode());
				sb.append("\n");
				sb.append("Cookies: \n");
				clientResp.cookies()
						.forEach((name, values) -> values.forEach(value -> sb.append(name + " : " + value)));
				sb.append("\n");
				sb.append("Headres: \n");
				clientResp.headers().asHttpHeaders()
						.forEach((name, values) -> values.forEach(value -> sb.append(name + " : " + value)));
				log.debug(sb.toString());
			}
			return Mono.just(clientResp);
		});
	}

	@Bean(name = "singlePermitSemaphore")
	public Semaphore semaphoreWithOnePermit() {
		return new Semaphore(1);
	}

	@Bean(name = "dtFormat")
	public SimpleDateFormat dtFormat() {
		return new SimpleDateFormat(DATE_FORMAT);
	}

	@Bean(name = "scheduled-thread-pool-with-one")
	public ScheduledExecutorService scheduledExecutorService() {
		return Executors.newScheduledThreadPool(1);
	}

	@Bean
	public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory factory) {
		return new ReactiveStringRedisTemplate(factory);
	}

}
