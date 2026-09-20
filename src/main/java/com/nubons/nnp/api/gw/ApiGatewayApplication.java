package com.nubons.nnp.api.gw;

import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.context.annotation.ImportRuntimeHints;

import com.nubons.nnp.api.gw.config.ApiGatewayNativeHints;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Gourab Guha
 * @since Mar, 28 - 2020
 *
 */
@SpringBootApplication
@EnableScheduling
@Slf4j
@ImportRuntimeHints(ApiGatewayNativeHints.class)
@ComponentScan({ "com.nubons.nnp.api.gw", "com.nubons.nnp.api.abs.redis.blocking.entities.employee",
		"com.nubons.nnp.api.abs.redis.blocking.core" })
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	@Bean
	@ConditionalOnProperty(name = "nnp.apiecosystem.apigw.show-filters-at-startup")
	public ApplicationRunner showFilters(List<GatewayFilterFactory<?>> filters) {

		log.info("No of filters loaded {}", filters.size());
		return args -> {
			filters.forEach(f -> log.info("Filter -- " + f.name()));
			log.info("Above filters loaded successfully ...");
		};

	}

}
