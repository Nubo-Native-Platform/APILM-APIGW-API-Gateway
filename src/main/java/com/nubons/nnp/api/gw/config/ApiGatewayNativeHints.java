package com.nubons.nnp.api.gw.config;

import java.util.List;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

import com.nubons.nnp.api.gw.filter.AbstractCustomFilter;
import com.nubons.nnp.api.gw.filter.ApiAnalyticsFilter;
import com.nubons.nnp.api.gw.filter.ApiAuthenticationFilter;
import com.nubons.nnp.api.gw.filter.ApiAuthorizationFilter;
import com.nubons.nnp.api.gw.filter.ApiGatewayCacheFilter;
import com.nubons.nnp.api.gw.filter.ApiGatewayRateLimitFilter;
import com.nubons.nnp.api.gw.filter.ApiGatewayRequestSizeFilter;
import com.nubons.nnp.api.gw.filter.ApiGatewayRetryFilter;
import com.nubons.nnp.api.gw.filter.ApiGatewaySecureHeadersFilter;

/**
 * Reflection registrations required when the gateway runs as a GraalVM native
 * image. ApiGatewayFilterFactory instantiates the custom filters at runtime via
 * Class.forName + registerBeanDefinition, and Spring Cloud Gateway binds the
 * Config subclasses reflectively per route, so none of them are visible to the
 * AOT processing. The control-plane transfer objects are decoded generically by
 * WebClient / Jackson at runtime and need Jackson hints for the same reason.
 */
public class ApiGatewayNativeHints implements RuntimeHintsRegistrar {

	private static final List<Class<?>> RUNTIME_BEAN_CLASSES = List.of(ApiAnalyticsFilter.class,
			ApiAuthenticationFilter.class, ApiAuthorizationFilter.class, ApiGatewayCacheFilter.class,
			ApiGatewayRateLimitFilter.class, ApiGatewayRequestSizeFilter.class, ApiGatewayRetryFilter.class,
			ApiGatewaySecureHeadersFilter.class, AbstractCustomFilter.Config.class,
			ApiGatewayRateLimitFilter.RateLimitConfig.class, ApiGatewayRequestSizeFilter.RequestSizeConfig.class,
			ApiGatewayCacheFilter.GwCacheConfig.class, ApiGatewayRetryFilter.RetryConfig.class);

	private static final List<TypeReference> JACKSON_TYPES = List.of(			TypeReference.of("com.nubons.nnp.api.abs.to.AbstractBaseTO"),
			TypeReference.of("com.nubons.nnp.api.abs.to.ApiFilterPredicateTO"),
			TypeReference.of("com.nubons.nnp.api.abs.to.ApiRouteTO"),
			TypeReference.of("com.nubons.nnp.api.abs.to.ApiUserTO"),
			TypeReference.of("com.nubons.nnp.api.abs.to.ApiLogTO"),
			TypeReference.of("com.nubons.nnp.api.abs.to.ApiRegistryTO"),
			TypeReference.of("com.nubons.nnp.api.abs.to.ApiRegistryPlanUserTO"),
			TypeReference.of("com.nubons.nnp.api.abs.to.ApiErrorTO"),
			TypeReference.of("com.nubons.nnp.api.abs.to.analytics.ApiAnalyticsRequestTO"),
			TypeReference.of("com.nubons.nnp.api.abs.to.analytics.ApiAnalyticsResponseTO"),
			TypeReference.of("com.nubons.nnp.api.abs.to.analytics.ApiTransactionTO"),
			TypeReference.of("com.nubons.nnp.api.gw.service.client.to.AbstractBaseTO"),
			TypeReference.of("com.nubons.nnp.api.gw.service.client.to.ApiRouteTO"),
			TypeReference.of("com.nubons.nnp.api.gw.service.client.to.HeartBeatResp"));

	// Spring Boot registers its Log4j2 plugins (converters, lookups, arbiters,
	// layouts used by its default log4j2.xml) only in its Log4j2Plugins.dat,
	// which native-image cannot resolve reflectively
	private static final List<TypeReference> LOG4J2_PLUGIN_TYPES = List.of(
			TypeReference.of("org.springframework.boot.logging.log4j2.WhitespaceThrowablePatternConverter"),
			TypeReference.of("org.springframework.boot.logging.log4j2.ExtendedWhitespaceThrowablePatternConverter"),
			TypeReference.of("org.springframework.boot.logging.log4j2.EnclosedInSquareBracketsConverter"),
			TypeReference.of("org.springframework.boot.logging.log4j2.CorrelationIdConverter"),
			TypeReference.of("org.springframework.boot.logging.log4j2.ColorConverter"),
			TypeReference.of("org.springframework.boot.logging.log4j2.SpringEnvironmentLookup"),
			TypeReference.of("org.springframework.boot.logging.log4j2.SpringBootPropertySource"),
			TypeReference.of("org.springframework.boot.logging.log4j2.SpringEnvironmentPropertySource"),
			TypeReference.of("org.springframework.boot.logging.log4j2.SpringProfileArbiter"),
			TypeReference.of("org.springframework.boot.logging.log4j2.SpringProfileArbiter$Builder"),
			TypeReference.of("org.springframework.boot.logging.log4j2.StructuredLogLayout"),
			TypeReference.of("org.springframework.boot.logging.log4j2.StructuredLogLayout$Builder"),
			TypeReference.of("org.springframework.boot.logging.log4j2.LogstashStructuredLogFormatter"),
			TypeReference.of("org.springframework.boot.logging.log4j2.ElasticCommonSchemaStructuredLogFormatter"),
			TypeReference.of("org.springframework.boot.logging.log4j2.GraylogExtendedLogFormatStructuredLogFormatter"),
			TypeReference.of("org.springframework.boot.logging.log4j2.Extractor"),
			TypeReference.of("org.springframework.boot.logging.log4j2.StructuredMessage"));

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		RUNTIME_BEAN_CLASSES.forEach(clazz -> hints.reflection().registerType(clazz, MemberCategory.values()));
		// the transfer objects are decoded generically by WebClient / Jackson at
		// runtime, so they need the same reflective registration
		JACKSON_TYPES.forEach(type -> hints.reflection().registerType(type, MemberCategory.values()));
		// the gateway ships no own log4j2 config, so Spring Boot's default
		// log4j2.xml must be embedded in the native image
		hints.resources().registerPattern("org/springframework/boot/logging/log4j2/*");
		LOG4J2_PLUGIN_TYPES.forEach(type -> hints.reflection().registerType(type, MemberCategory.values()));
	}

}
